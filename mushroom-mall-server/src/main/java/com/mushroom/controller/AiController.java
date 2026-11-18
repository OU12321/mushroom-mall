package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mushroom.common.Result;
import com.mushroom.entity.Category;
import com.mushroom.entity.Product;
import com.mushroom.entity.Review;
import com.mushroom.service.CategoryService;
import com.mushroom.service.ProductService;
import com.mushroom.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * AI助手控制器
 * 对接 DeepSeek 大模型（deepseek-flash，支持图片输入），支持文字对话 + 图片识菌
 * 采用 RAG（检索增强生成）：先查项目数据库，再喂给大模型作上下文，让AI能基于项目真实数据回答
 * API调用失败时自动降级到关键词匹配，保证演示不中断
 *
 * 说明：DeepSeek 使用 OpenAI 兼容协议，请求体与响应体结构同 OpenAI Chat Completions，
 *      图片以 base64 data URL 通过 image_url 块传入（仅 user 消息支持图片）。
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Value("${mushroom.ai.api-url}")
    private String apiUrl;

    @Value("${mushroom.ai.api-key:}")
    private String apiKey;

    @Value("${mushroom.ai.model}")
    private String model;

    /** DeepSeek 默认开启思考模式（thinking），聊天助手不需要，置 true 关闭以降延迟 */
    @Value("${mushroom.ai.disable-thinking:true}")
    private boolean disableThinking;

    @Value("${mushroom.upload-path}")
    private String uploadPath;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ReviewService reviewService;

    /** 基础系统提示词：让AI扮演野生菌专家 + 商城导购 */
    private static final String BASE_PROMPT =
            "你是「高山野生菌线上销售商城」内置的AI助手，既是专业的野生菌识别专家，也是商城的智能导购。\n\n" +
            "你的职责：\n" +
            "1. 识别用户上传的菌菇照片，判断品种、是否可食用、安全等级、产地、营养价值\n" +
            "2. 回答关于野生菌的问题：烹饪方法、保存方法、采摘季节、中毒急救等\n" +
            "3. 基于下方提供的商城实时数据，回答商品、价格、销量、评价、分类等商城相关问题\n\n" +
            "重要原则：\n" +
            "- 当用户询问商城商品/价格/销量/分类/评价时，必须基于下方「商城实时数据」回答，不要编造数据\n" +
            "- 如果下方数据中没有用户提到的商品，如实告知用户目前商城没有该商品\n" +
            "- 【识别图片时必做】先判断菌种，然后必须对照下方「商城实时数据」中的在售商品清单：\n" +
            "  若清单里存在同一菌种（含俗称、同科或近似商品名，例如「牛肝菌」对应「美味牛肝菌(鲜)」「牛肝菌干片」），\n" +
            "  要主动推荐该商品并报出价格与产地；只有当清单里确实找不到对应菌种时，才可以说商城暂无销售。\n" +
            "  严禁在未对照清单的情况下直接回答「商城没有」\n" +
            "- 「商城实时数据」里列出的商品就是当前在售商品：只要清单里出现了商品，" +
            "就绝不能说「商城没有商品」「商品全部下架」之类的话\n" +
            "- 用户问的菌种清单里确实没有时，只说「暂未上架该菌种」，不要否定整个商城\n" +
            "- 回答商品相关问题时，可以顺便介绍该菌菇的知识（产地、食用方法等）\n\n" +
            "安全提示（涉及食用建议时必须包含）：\n" +
            "- AI识别结果仅供参考，不保证100%准确\n" +
            "- 不认识的野生菌请勿采食\n" +
            "- 野生菌中毒可危及生命，如有不适应立即拨打120就医\n\n" +
            "回答风格：专业简洁、有条理，适当使用emoji增加可读性（🍄可食用 ⚠️慎食 🚨剧毒 🛒商品）。";

    /** RestTemplate 实例（设超时） */
    private final RestTemplate restTemplate;

    public AiController() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);  // 15秒连接超时
        factory.setReadTimeout(60000);     // 60秒读取超时（大模型响应可能较慢）
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * AI对话（支持文字 + 图片）
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        String image = body.get("image");

        if (message == null || message.trim().isEmpty()) {
            return Result.success("请输入您的问题");
        }

        // 检查是否配置了真实 API Key
        if (apiKey == null || apiKey.isEmpty() || "your-api-key-here".equals(apiKey)) {
            log.warn("未配置AI API Key，降级到关键词匹配");
            return Result.success(generateReply(message, image));
        }

        try {
            // RAG：先查项目数据库，构建上下文
            String context = buildContext(message, image != null && !image.isEmpty());
            String reply = callLlm(message, image, context);
            return Result.success(reply);
        } catch (Exception e) {
            log.error("调用大模型失败，降级到关键词匹配: {}", e.getMessage(), e);
            return Result.success(generateReply(message, image));
        }
    }

    // ==================== RAG：检索增强生成 ====================

    /**
     * 根据用户问题意图，从项目数据库查询相关数据，构建上下文
     * 图片识别场景不查数据（识菌不需要商品库）
     */
    private String buildContext(String message, boolean hasImage) {
        String msg = message.toLowerCase();
        StringBuilder ctx = new StringBuilder();

        boolean wantProducts = msg.contains("商品") || msg.contains("有什么") || msg.contains("卖什么")
                || msg.contains("哪些") || msg.contains("商城") || msg.contains("有啥") || msg.contains("有卖");
        boolean wantCategory = msg.contains("分类") || msg.contains("种类") || msg.contains("类别") || msg.contains("类型");
        boolean wantPrice = msg.contains("价格") || msg.contains("多少钱") || msg.contains("便宜") || msg.contains("贵")
                || msg.contains("划算") || msg.contains("价位");
        boolean wantSales = msg.contains("销量") || msg.contains("热销") || msg.contains("畅销") || msg.contains("卖得最好")
                || msg.contains("排行") || msg.contains("排名") || msg.contains("爆款");
        boolean wantReview = msg.contains("评价") || msg.contains("口碑") || msg.contains("好评") || msg.contains("怎么样");
        boolean wantOrigin = msg.contains("产地") || msg.contains("哪里产") || msg.contains("哪产") || msg.contains("采摘地");
        boolean hasMushroomName = msg.contains("松茸") || msg.contains("牛肝菌") || msg.contains("鸡枞")
                || msg.contains("羊肚菌") || msg.contains("干巴菌") || msg.contains("见手青") || msg.contains("竹荪")
                || msg.contains("松露") || msg.contains("鸡油菌") || msg.contains("青头菌");
        // 通用菌菇词检测：凡是消息中带"菌"或"菇"字，就尝试提取作为商品查询词（适配商家新增的非预设菌名）
        boolean mentionMushroomWord = msg.contains("菌") || msg.contains("菇");

        // 任一意图触发，或提到具体菌名 → 查商品数据
        if (wantProducts || wantPrice || wantSales || wantReview || wantOrigin || hasMushroomName || mentionMushroomWord) {
            // 提取用户提到的"非预设菌名"作为自定义查询词
            // 例：用户输入"香的掉渣菇" → 提取"香的掉渣菇" 去 like 商品表
            String customKeyword = null;
            if (mentionMushroomWord && !hasMushroomName) {
                customKeyword = extractMushroomKeyword(msg);
            }
            // 带图提问 / "这是什么菌"式提问：用户并不知道品种名，此时不能拿句子片段去模糊查商品。
            // 否则会查到 0 条，并在上下文里写成"暂无该商品"，模型就会错误地回答"商城没卖"。
            // → 这类场景改为列出全部在售商品，让模型识别出品种后自行对照（提示词里有硬规则约束）。
            boolean listAll = hasImage || customKeyword == null || customKeyword.isEmpty()
                    || isIdentifyQuestion(msg);
            ctx.append(buildProductContext(msg, wantSales, wantPrice, hasMushroomName, customKeyword, listAll));
        }

        if (wantCategory) {
            ctx.append(buildCategoryContext());
        }

        if (wantReview) {
            ctx.append(buildReviewContext());
        }

        // 如果没有任何匹配意图，返回商城概况（让AI知道商城大致情况）
        if (ctx.length() == 0) {
            ctx.append(buildOverviewContext());
        }

        return ctx.toString();
    }

    /** 问句常见前缀：提取菌名时先剥掉，避免把"帮我看看这是什么菌菇""有卖XX吗"整句当成商品名 */
    private static final String[] QUESTION_PREFIXES = {
            "帮我看看这是什么", "麻烦看看这是什么", "请问这是什么", "想问问这是什么",
            "有没有卖", "有没有", "卖不卖", "有木有", "有卖",
            "这是什么", "这是一",
            "帮我看看", "麻烦看看", "请看看", "帮我查查", "帮我查", "帮我",
            "请问", "麻烦", "想买", "看看", "这个", "那个", "这是",
            "卖", "有", "买", "想", "查", "搜",
            "什么", "叫", "名字", "这", "那", "是", "的"
    };

    /** 泛指的菌类词：不是具体品种名，不能拿去查商品 */
    private static final java.util.Set<String> GENERIC_MUSHROOM_WORDS = new java.util.HashSet<>(Arrays.asList(
            "菌", "菇", "蘑菇", "菌菇", "菌子", "菇子", "野生菌", "野菌", "野菇",
            "食用菌", "菌类", "菇类", "真菌", "干菌", "鲜菌"
    ));

    /**
     * 判断是否为"识别/鉴赏"式提问（用户不知道品种，只是问这是什么）
     * 这类提问不能拿句子片段去 like 查商品，应改为列出全部在售商品让模型自行对照
     */
    private boolean isIdentifyQuestion(String msg) {
        return msg.contains("这是什么") || msg.contains("什么菌") || msg.contains("什么菇")
                || msg.contains("啥菌") || msg.contains("啥菇")
                || msg.contains("识别") || msg.contains("认识") || msg.contains("辨别") || msg.contains("看看")
                || msg.contains("图片") || msg.contains("照片") || msg.contains("拍")
                || msg.contains("能吃吗") || msg.contains("能不能吃") || msg.contains("能吃么")
                || msg.contains("有毒") || msg.contains("是什么");
    }

    /**
     * 从用户消息中提取含"菌/菇"的中文词组（取最长的）
     * 用于适配商家新增的非预设菌名（如"香的掉渣菇""黑虎掌菌"等）
     * 会剥掉问句前缀，并丢弃"野生菌""菌菇"这类泛指词，避免生成垃圾查询词
     */
    private String extractMushroomKeyword(String msg) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("([\\u4e00-\\u9fa5]{1,12}(?:菌|菇))");
        java.util.regex.Matcher m = p.matcher(msg);
        String longest = null;
        while (m.find()) {
            String found = m.group();
            // 反复剥掉问句前缀（"帮我看看这是什么菌菇" → "菌菇"；再被泛义词规则丢弃）
            boolean changed = true;
            while (changed) {
                changed = false;
                for (String pre : QUESTION_PREFIXES) {
                    if (found.startsWith(pre) && found.length() > pre.length()) {
                        found = found.substring(pre.length());
                        changed = true;
                    }
                }
            }
            if (found.isEmpty() || GENERIC_MUSHROOM_WORDS.contains(found)) {
                continue;
            }
            if (longest == null || found.length() > longest.length()) {
                longest = found;
            }
        }
        return longest;
    }

    /** 查询商品数据（在售：已审核通过+上架）
     *  @param listAll true=不加名称过滤，列出全部在售商品（供模型识别品种后自行对照） */
    private String buildProductContext(String msg, boolean sortBySales, boolean sortByPrice,
                                       boolean hasMushroomName, String customKeyword, boolean listAll) {
        StringBuilder sb = new StringBuilder();
        boolean byPrice = sortByPrice && !sortBySales;

        // 本次用于筛选商品名的词（预设菌名优先，其次是从问句抽出的自定义菌名）
        String searchTerm = hasMushroomName ? matchPresetMushroomName(msg) : customKeyword;
        if (searchTerm != null && searchTerm.isEmpty()) {
            searchTerm = null;
        }

        List<Product> products = queryProducts(msg, hasMushroomName, customKeyword, listAll, byPrice);

        // 兜底防线：按关键词筛选后一条都没查到，很可能是关键词抽得不准（也可能确实没上架）。
        // 此时绝不能只回一句"暂无" —— 模型会据此对用户说"商城没卖"。
        // 改为退回「列出全部在售商品」，让模型拿完整清单自行对照判断。
        String note = null;
        if (products.isEmpty() && !listAll) {
            products = queryProducts(msg, false, null, true, byPrice);
            if (!products.isEmpty()) {
                note = "（本次未在商品名中匹配到「" + (searchTerm == null ? "该菌种" : searchTerm)
                        + "」，因此下面列出的是商城全部在售商品，请对照确认是否存在该菌种或近似品种；"
                        + "注意：这些商品都是在售状态，不要回答「商城没有商品」或「全部下架」）";
                listAll = true;
            }
        }

        sb.append(listAll
                ? "\n\n【商城在售商品完整清单（实时查询，仅含已上架商品）——判断是否有用户所需菌种时必须对照此清单】\n"
                : "\n\n【商城商品数据（实时查询，仅含已上架商品）】\n");
        if (note != null) {
            sb.append(note).append("\n");
        }

        if (products.isEmpty()) {
            if (listAll) {
                sb.append("（商城当前没有任何在售商品）\n");
            } else {
                sb.append("（目前商城暂无名称含 \"")
                  .append(customKeyword == null ? "" : customKeyword)
                  .append("\" 的在售商品）\n");
                sb.append("（提示：如果是新菌种/地方俗称未上架，可基于菌菇专业知识回答；商家如需销售该商品请先上架）\n");
            }
            return sb.toString();
        }

        for (Product p : products) {
            sb.append("- 商品名:").append(p.getName())
              .append(" | 价格:").append(p.getPrice()).append("元")
              .append(" | 产地:").append(p.getOrigin() == null || p.getOrigin().isEmpty() ? "未标注" : p.getOrigin())
              .append(" | 销量:").append(p.getSaleCount() == null ? 0 : p.getSaleCount());
            if (p.getCookingMethod() != null && !p.getCookingMethod().isEmpty()) {
                sb.append(" | 食用方法:").append(p.getCookingMethod());
            }
            if (p.getHarvestTime() != null && !p.getHarvestTime().isEmpty()) {
                sb.append(" | 采摘时间:").append(p.getHarvestTime());
            }
            sb.append("\n");
        }

        // 统计概览
        long totalProducts = productService.count(new LambdaQueryWrapper<Product>()
                .eq(Product::getAuditStatus, 1).eq(Product::getStatus, 1));
        sb.append("（商城目前在售商品共 ").append(totalProducts).append(" 种）\n");
        return sb.toString();
    }

    /** 预设菌名（按顺序匹配，先命中者优先） */
    private static final String[] PRESET_MUSHROOM_NAMES = {
            "松茸", "牛肝菌", "鸡枞", "羊肚菌", "干巴菌", "见手青", "竹荪", "松露", "鸡油菌", "青头菌"
    };

    /** 从消息中识别命中的预设菌名；没有则返回 null */
    private String matchPresetMushroomName(String msg) {
        for (String n : PRESET_MUSHROOM_NAMES) {
            if (msg.contains(n)) {
                return n;
            }
        }
        return null;
    }

    /** 按条件查询在售商品（已审核通过 + 已上架），最多 15 条
     *  @param listAll true=不加名称过滤，返回全部在售商品 */
    private List<Product> queryProducts(String msg, boolean hasMushroomName, String customKeyword,
                                        boolean listAll, boolean byPrice) {
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        w.eq(Product::getAuditStatus, 1).eq(Product::getStatus, 1);

        // 按菌名筛选（预设菌名走精确 like，非预设菌名用 customKeyword 模糊查）
        if (hasMushroomName) {
            String preset = matchPresetMushroomName(msg);
            if (preset != null) {
                w.like(Product::getName, preset);
            }
        } else if (!listAll && customKeyword != null && !customKeyword.isEmpty()) {
            // 适配商家新增的非预设菌名（如"香的掉渣菇""黑虎掌菌"等地方俗称）
            w.like(Product::getName, customKeyword);
        }
        // listAll=true 时不加任何名称过滤 → 返回全部在售商品供模型对照

        if (byPrice) {
            w.orderByAsc(Product::getPrice);
        } else {
            w.orderByDesc(Product::getSaleCount);
        }
        w.last("limit 15");
        return productService.list(w);
    }

    /** 查询全部分类 */
    private String buildCategoryContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n【商城分类数据（实时查询）】\n");
        List<Category> cats = categoryService.list(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        if (cats.isEmpty()) {
            sb.append("（暂无分类）\n");
            return sb.toString();
        }
        for (Category c : cats) {
            sb.append("- 分类:").append(c.getName()).append("\n");
        }
        return sb.toString();
    }

    /** 查询最新评价 */
    private String buildReviewContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n【商品评价数据（实时查询，最近5条）】\n");
        List<Review> reviews = reviewService.list(
                new LambdaQueryWrapper<Review>().orderByDesc(Review::getId).last("limit 5"));
        if (reviews.isEmpty()) {
            sb.append("（暂无评价数据）\n");
            return sb.toString();
        }
        // 批量查商品名
        Set<Long> productIds = new HashSet<>();
        for (Review r : reviews) productIds.add(r.getProductId());
        Map<Long, String> productNameMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<Product> ps = productService.listByIds(productIds);
            for (Product p : ps) productNameMap.put(p.getId(), p.getName());
        }
        for (Review r : reviews) {
            sb.append("- 商品:").append(productNameMap.getOrDefault(r.getProductId(), "未知商品"))
              .append(" | 评分:").append(r.getScore()).append("星")
              .append(" | 用户:").append(r.getUserName() == null ? "匿名" : r.getUserName())
              .append(" | 评价:").append(r.getContent() == null ? "无文字" : r.getContent())
              .append("\n");
        }
        return sb.toString();
    }

    /** 商城概况（兜底，让AI知道商城基本信息） */
    private String buildOverviewContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n【商城概况（实时查询）】\n");
        long productCount = productService.count(new LambdaQueryWrapper<Product>()
                .eq(Product::getAuditStatus, 1).eq(Product::getStatus, 1));
        long categoryCount = categoryService.count();
        sb.append("- 商城目前在售商品: ").append(productCount).append(" 种\n");
        sb.append("- 商品分类数量: ").append(categoryCount).append(" 个\n");
        sb.append("- 商城主营: 各类高山野生菌（松茸、牛肝菌、鸡枞菌、羊肚菌、干巴菌等）\n");
        sb.append("（提示：用户若问商品/价格/销量/评价等，以上数据可供参考）\n");
        return sb.toString();
    }

    // ==================== 调用大模型（DeepSeek） ====================

    /**
     * 调用大模型 API（DeepSeek，OpenAI 兼容协议，带 RAG 上下文）
     */
    @SuppressWarnings("unchecked")
    private String callLlm(String message, String image, String context) throws Exception {
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        // 构建 messages
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. system 消息（基础提示词 + RAG上下文）
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", BASE_PROMPT + context);
        messages.add(systemMsg);

        // 2. user 消息
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");

        boolean hasImage = image != null && !image.isEmpty();
        if (hasImage) {
            // 多模态：把本地图片转成 base64 data URL
            String imageDataUrl = resolveImageToBase64(image);
            if (imageDataUrl != null) {
                // content 为数组格式（文字 + 图片）
                List<Map<String, Object>> content = new ArrayList<>();
                // 文字部分
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("type", "text");
                textPart.put("text", message);
                content.add(textPart);
                // 图片部分
                Map<String, Object> imagePart = new HashMap<>();
                imagePart.put("type", "image_url");
                Map<String, String> imageUrlMap = new HashMap<>();
                imageUrlMap.put("url", imageDataUrl);
                imagePart.put("image_url", imageUrlMap);
                content.add(imagePart);
                userMsg.put("content", content);
                log.info("调用大模型 [多模态+RAG] model={} contextLen={}", model, context.length());
            } else {
                // 图片读取失败，只发文字
                userMsg.put("content", message + "\n\n（注意：用户上传了一张菌菇图片，但系统无法读取图片内容，请告知用户重新上传）");
                log.info("调用大模型 [纯文字+RAG-图片读取失败] model={}", model);
            }
        } else {
            // 纯文字对话
            userMsg.put("content", message);
            log.info("调用大模型 [纯文字+RAG] model={} contextLen={}", model, context.length());
        }
        messages.add(userMsg);

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);
        // DeepSeek 默认 thinking=enabled（返回 reasoning_content 且明显变慢），
        // 聊天助手不需要思考链，显式关闭；若换回不支持该字段的服务商可在配置里置 false
        if (disableThinking) {
            Map<String, String> thinking = new HashMap<>();
            thinking.put("type", "disabled");
            requestBody.put("thinking", thinking);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 发送请求
        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, entity, Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("大模型API返回异常: status=" + response.getStatusCode());
        }

        // 解析响应
        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("大模型返回的choices为空");
        }

        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> messageObj = (Map<String, Object>) firstChoice.get("message");
        if (messageObj == null) {
            throw new RuntimeException("大模型返回的message为空");
        }

        String content = (String) messageObj.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("大模型返回的内容为空");
        }

        log.info("大模型响应成功，长度={}", content.length());
        return content;
    }

    /**
     * 将本地图片URL转换为 base64 data URL（DeepSeek 支持 base64 data URL / 公网URL / Files file_id）
     */
    private String resolveImageToBase64(String image) {
        try {
            if (image == null || image.isEmpty()) {
                return null;
            }

            // 已经是 base64 data URL
            if (image.startsWith("data:")) {
                return image;
            }

            // 从URL中提取文件名
            String fileName;
            if (image.startsWith("http")) {
                // http://localhost:8080/api/static/xxx.jpg → xxx.jpg
                fileName = image.substring(image.lastIndexOf("/") + 1);
            } else if (image.contains("/static/")) {
                // /api/static/xxx.jpg → xxx.jpg
                fileName = image.substring(image.lastIndexOf("/") + 1);
            } else {
                fileName = image;
            }

            // 从本地文件读取
            File imgFile = new File(uploadPath + fileName);
            if (!imgFile.exists()) {
                log.warn("图片文件不存在: {}", imgFile.getAbsolutePath());
                return null;
            }

            byte[] bytes = Files.readAllBytes(imgFile.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);

            // 根据后缀确定 mime type
            String mimeType = "image/jpeg";
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".png")) {
                mimeType = "image/png";
            } else if (lower.endsWith(".gif")) {
                mimeType = "image/gif";
            } else if (lower.endsWith(".webp")) {
                mimeType = "image/webp";
            }

            return "data:" + mimeType + ";base64," + base64;
        } catch (Exception e) {
            log.error("图片转base64失败: " + image, e);
            return null;
        }
    }

    // ==================== 降级方案：关键词匹配（API调用失败时使用） ====================

    /**
     * 基于关键词的智能回复（降级方案）
     * 注意：降级方案无法使用真实大模型，只能回答预设问题，商品数据不实时
     */
    private String generateReply(String message, String image) {
        String msg = message.toLowerCase();

        // 图片识别场景
        if (image != null && !image.isEmpty()) {
            return "⚠️ AI大模型服务暂时不可用，无法识别图片。\n\n" +
                    "通用安全提示：\n" +
                    "1. 不确定的野生菌请勿食用\n" +
                    "2. 野生菌中毒症状包括恶心、呕吐、幻觉等，严重可危及生命\n" +
                    "3. 如有不适请立即拨打120就医\n\n" +
                    "请稍后重试，或直接向我提问菌菇的文字问题。";
        }

        // 商品相关：降级方案下查询真实数据库（不依赖大模型，直接返回数据）
        if (msg.contains("商品") || msg.contains("有什么") || msg.contains("卖什么") || msg.contains("商城") || msg.contains("有啥") || msg.contains("有卖")) {
            return generateProductListReply();
        }
        if (msg.contains("价格") || msg.contains("多少钱") || msg.contains("便宜") || msg.contains("贵") || msg.contains("划算")) {
            return generateProductListReply("按价格排序");
        }
        if (msg.contains("销量") || msg.contains("热销") || msg.contains("畅销") || msg.contains("卖得最好") || msg.contains("排行")) {
            return generateProductListReply("按销量排序");
        }
        if (msg.contains("分类") || msg.contains("种类") || msg.contains("类别")) {
            return generateCategoryReply();
        }
        if (msg.contains("评价") || msg.contains("口碑") || msg.contains("好评")) {
            return generateReviewReply();
        }

        // 菌菇知识库（预设）
        if (msg.contains("松茸")) {
            return "🍄 **松茸**（Tricholoma matsutake）\n\n" +
                    "• 安全等级：可食用（名贵食用菌）\n" +
                    "• 产地：云南香格里拉、四川、西藏等高原地区\n" +
                    "• 营养价值：富含蛋白质、氨基酸、维生素，具有抗癌、抗肿瘤功效\n" +
                    "• 烹饪方法：松茸炖鸡、黄油煎松茸、松茸刺身、松茸饭\n" +
                    "• 保存方法：鲜品冷藏3-5天，可切片冷冻或晒干保存\n" +
                    "• 采摘季节：7-10月";
        }

        if (msg.contains("牛肝菌")) {
            return "🍄 **牛肝菌**（Boletus）\n\n" +
                    "• 安全等级：大部分可食用，少数品种有毒（如红网牛肝菌）\n" +
                    "• 产地：云南、四川、贵州等\n" +
                    "• 特点：菌肉肥厚，味道鲜美，是云南最常见的野生菌之一\n" +
                    "• 烹饪方法：干炒、煲汤、做菌油\n" +
                    "• 注意：见手青（小美牛肝菌）必须充分炒熟，否则会引起幻觉中毒\n" +
                    "• 采摘季节：6-9月";
        }

        if (msg.contains("鸡枞") || msg.contains("鸡纵")) {
            return "🍄 **鸡枞菌**（Termitomyces）\n\n" +
                    "• 安全等级：可食用（珍稀美味食用菌）\n" +
                    "• 产地：云南、贵州、四川等\n" +
                    "• 特点：与白蚁共生，味道极鲜，有\"菌中之王\"美誉\n" +
                    "• 烹饪方法：鸡枞汤、油鸡枞（云南名菜）、清蒸鸡枞\n" +
                    "• 保存方法：鲜品易变质，常制成油鸡枞长期保存\n" +
                    "• 采摘季节：6-9月雨季";
        }

        if (msg.contains("见手青")) {
            return "⚠️ **见手青**（小美牛肝菌）\n\n" +
                    "• 安全等级：慎食（必须充分烹饪！）\n" +
                    "• 特点：切开后接触空气会变青蓝色，由此得名\n" +
                    "• ⚠️ 中毒风险：未炒熟会引起神经精神型中毒，产生幻觉（俗称\"见小人\"）\n" +
                    "• 安全烹饪要点：\n" +
                    "  1. 必须切薄片，充分炒熟至少15分钟以上\n" +
                    "  2. 炒制时放大量大蒜和猪油\n" +
                    "  3. 不要猛吃、不要吃隔夜的\n" +
                    "  4. 第一次吃建议少量尝试\n" +
                    "• 中毒急救：立即催吐并拨打120就医";
        }

        if (msg.contains("羊肚菌")) {
            return "🍄 **羊肚菌**（Morchella）\n\n" +
                    "• 安全等级：可食用（名贵食用菌）\n" +
                    "• 产地：云南、四川、青海等\n" +
                    "• 特点：菌盖呈蜂窝状，形似羊肚，味道鲜美\n" +
                    "• 营养价值：富含多种氨基酸和维生素\n" +
                    "• 烹饪方法：炖汤、炒肉、做酱\n" +
                    "• 采摘季节：3-5月春季";
        }

        if (msg.contains("干巴菌")) {
            return "🍄 **干巴菌**（Thelephora ganbajun）\n\n" +
                    "• 安全等级：可食用（云南特有名贵食用菌）\n" +
                    "• 产地：云南昆明、玉溪、楚雄等地\n" +
                    "• 特点：形似干牛肉丝，味道浓香独特\n" +
                    "• 烹饪方法：干巴菌炒青椒（经典）、干巴菌炒饭\n" +
                    "• 价格：野生菌中价格最高的品种之一\n" +
                    "• 采摘季节：7-9月";
        }

        if (msg.contains("毒") || msg.contains("中毒") || msg.contains("急救")) {
            return "🚨 **野生菌中毒急救指南**\n\n" +
                    "1. **立即催吐**：用手指或筷子刺激咽喉催吐\n" +
                    "2. **拨打120**：立即就医，告知医生食用的菌种\n" +
                    "3. **保留样本**：保存剩余菌菇供医生鉴定\n" +
                    "4. **大量饮水**：催吐后饮用淡盐水补充水分\n" +
                    "5. **不要自行服药**：等待专业医疗救治\n\n" +
                    "常见中毒类型：\n" +
                    "• 胃肠炎型：恶心、呕吐、腹泻（6小时内发作）\n" +
                    "• 神经精神型：幻觉、谵妄（见手青等）\n" +
                    "• 溶血型：黄疸、血红蛋白尿\n" +
                    "• 多脏器损伤型：最危险，可致死（鹅膏菌属）";
        }

        if (msg.contains("辨别") || msg.contains("识别") || msg.contains("怎么分辨")) {
            return "🔍 **野生菌辨别要点**\n\n" +
                    "⚠️ 没有任何单一规则能区分毒菌和食用菌！\n\n" +
                    "常见误区（均不可靠）：\n" +
                    "× 颜色鲜艳的才有毒 — 错！剧毒鹅膏菌多为白色\n" +
                    "× 有虫咬痕的无毒 — 错！昆虫能吃的菌人不一定能吃\n" +
                    "× 银针试毒变黑 — 错！这是民间谣言\n" +
                    "× 大蒜炒菌不变黑就安全 — 错！不可靠\n\n" +
                    "正确做法：\n" +
                    "✓ 只采食自己能100%确认的品种\n" +
                    "✓ 不采不认识的菌菇\n" +
                    "✓ 参考当地经验丰富的采菌人指导\n" +
                    "✓ 使用本系统的AI助手辅助识别";
        }

        if (msg.contains("保存") || msg.contains("储藏") || msg.contains("存放")) {
            return "📦 **野生菌保存方法**\n\n" +
                    "1. **冷藏保存**（鲜品）：用厨房纸包裹后放入保鲜袋，冷藏3-5天\n" +
                    "2. **冷冻保存**：清洗切块后速冻，可保存3-6个月\n" +
                    "3. **晒干保存**：切片晒干或烘干，密封保存可达1年\n" +
                    "4. **油浸保存**：如油鸡枞，用植物油浸制可长期保存\n" +
                    "5. **盐渍保存**：用盐水浸泡保存\n\n" +
                    "注意：鲜菌保存前不要清洗，食用前再洗";
        }

        if (msg.contains("烹饪") || msg.contains("做法") || msg.contains("怎么吃") || msg.contains("怎么炒")) {
            return "🍳 **野生菌烹饪通用原则**\n\n" +
                    "1. **充分烹饪**：所有野生菌必须充分炒熟/煮熟\n" +
                    "2. **不要混炒**：不同品种的菌不要混在一起炒\n" +
                    "3. **加蒜辨毒**：炒菌时放大蒜（民间做法，非绝对可靠）\n" +
                    "4. **不要吃隔夜的**：炒好的菌当天吃完\n" +
                    "5. **第一次少量**：第一次吃某品种时少量尝试\n\n" +
                    "经典做法推荐：\n" +
                    "• 干炒：牛肝菌、干巴菌\n" +
                    "• 炖汤：松茸炖鸡、羊肚菌炖排骨\n" +
                    "• 油浸：油鸡枞、菌油\n" +
                    "• 刺身：新鲜松茸可生食";
        }

        // 默认回复
        return "您好！我是AI助手。您可以问我关于野生菌的任何问题，例如：\n\n" +
                "• \"商城里有什么商品？\"\n" +
                "• \"有什么热销的野生菌？\"\n" +
                "• \"松茸怎么吃？\"\n" +
                "• \"见手青安全吗？\"\n" +
                "• \"野生菌中毒怎么办？\"\n\n" +
                "您也可以上传菌菇照片让我帮您识别 📷";
    }

    /** 降级方案：查询商品列表（直接查数据库，不走大模型） */
    private String generateProductListReply() {
        return generateProductListReply(null);
    }

    private String generateProductListReply(String sortHint) {
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        w.eq(Product::getAuditStatus, 1).eq(Product::getStatus, 1);
        if (sortHint != null && sortHint.contains("价格")) {
            w.orderByAsc(Product::getPrice);
        } else {
            w.orderByDesc(Product::getSaleCount);
        }
        w.last("limit 10");
        List<Product> products = productService.list(w);

        StringBuilder sb = new StringBuilder();
        sb.append("🛒 **商城在售商品").append(sortHint == null ? "" : "（" + sortHint + "）").append("**\n\n");
        if (products.isEmpty()) {
            sb.append("目前商城暂无在售商品。\n");
            return sb.toString();
        }
        for (Product p : products) {
            sb.append("• ").append(p.getName())
              .append(" — ").append(p.getPrice()).append("元/kg")
              .append("，销量").append(p.getSaleCount() == null ? 0 : p.getSaleCount());
            if (p.getOrigin() != null && !p.getOrigin().isEmpty()) {
                sb.append("，产地").append(p.getOrigin());
            }
            sb.append("\n");
        }
        sb.append("\n（注：AI大模型服务暂时不可用，以上为数据库实时查询结果）");
        return sb.toString();
    }

    /** 降级方案：查询分类 */
    private String generateCategoryReply() {
        List<Category> cats = categoryService.list(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        StringBuilder sb = new StringBuilder();
        sb.append("📂 **商城商品分类**\n\n");
        if (cats.isEmpty()) {
            sb.append("暂无分类数据。\n");
            return sb.toString();
        }
        for (Category c : cats) {
            sb.append("• ").append(c.getName()).append("\n");
        }
        sb.append("\n（注：AI大模型服务暂时不可用，以上为数据库实时查询结果）");
        return sb.toString();
    }

    /** 降级方案：查询评价 */
    private String generateReviewReply() {
        List<Review> reviews = reviewService.list(
                new LambdaQueryWrapper<Review>().orderByDesc(Review::getId).last("limit 5"));
        StringBuilder sb = new StringBuilder();
        sb.append("💬 **最新商品评价**\n\n");
        if (reviews.isEmpty()) {
            sb.append("暂无评价数据。\n");
            return sb.toString();
        }
        Set<Long> productIds = new HashSet<>();
        for (Review r : reviews) productIds.add(r.getProductId());
        Map<Long, String> nameMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Product p : productService.listByIds(productIds)) {
                nameMap.put(p.getId(), p.getName());
            }
        }
        for (Review r : reviews) {
            sb.append("• ").append(nameMap.getOrDefault(r.getProductId(), "未知商品"))
              .append(" — ").append(r.getScore()).append("星 — ")
              .append(r.getContent() == null ? "无文字" : r.getContent()).append("\n");
        }
        sb.append("\n（注：AI大模型服务暂时不可用，以上为数据库实时查询结果）");
        return sb.toString();
    }
}