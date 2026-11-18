package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mushroom.annotation.RequireRole;
import com.mushroom.common.Result;
import com.mushroom.entity.*;
import com.mushroom.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计控制器
 * 管理员数据大屏 + 商家统计
 * 支持 time range 切换: today / month / 7days / 30days
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private static final BigDecimal PROFIT_RATE = new BigDecimal("0.3");

    @Autowired
    private OrdersService ordersService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private CartService cartService;
    @Autowired
    private AddressService addressService;

    // ======================== Helper ========================

    /** 返回 [start, end) 时间范围 */
    private LocalDateTime[] getTimeRange(String range) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        switch (range == null ? "7days" : range) {
            case "today":
                return new LocalDateTime[]{ todayStart, todayStart.plusDays(1) };
            case "month":
                return new LocalDateTime[]{ now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0),
                        now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).plusMonths(1) };
            case "30days":
                return new LocalDateTime[]{ todayStart.minusDays(29), todayStart.plusDays(1) };
            case "7days":
            default:
                return new LocalDateTime[]{ todayStart.minusDays(6), todayStart.plusDays(1) };
        }
    }

    /** 返回趋势天数 */
    private int getTrendDays(String range) {
        switch (range == null ? "7days" : range) {
            case "today": return 1;
            case "month": return 30;
            case "30days": return 30;
            default: return 7;
        }
    }

    /** 计算指定时间范围内的已支付订单列表 (start/end/merchantId 为 null 时跳过该条件) */
    private List<Orders> getPaidOrders(LocalDateTime start, LocalDateTime end, Long merchantId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Orders::getStatus, 3, 4);
        // 关键修复：MyBatis-Plus 的 wrapper.between() 在 start/end 为 null 时行为不可靠
        // 改用 ge/lt 显式判断，只有非 null 才添加条件（用于 "总销售额" 不限时间范围的场景）
        if (start != null) {
            wrapper.ge(Orders::getCreateTime, start);
        }
        if (end != null) {
            wrapper.lt(Orders::getCreateTime, end);
        }
        if (merchantId != null) {
            wrapper.eq(Orders::getMerchantId, merchantId);
        }
        return ordersService.list(wrapper);
    }

    /** 计算订单总金额 */
    private BigDecimal sumAmount(List<Orders> orders) {
        return orders.stream()
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 计算利润 (销售额 * 利润率) */
    private BigDecimal calcProfit(BigDecimal sales) {
        return sales.multiply(PROFIT_RATE).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    // ======================== 首页统计卡 ========================

    @GetMapping("/home")
    public Result<Map<String, Object>> home(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        Map<String, Object> result = new HashMap<>();

        if (role == null) return Result.error("无法获取角色信息");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        if (role == 0) {
            result.put("userCount", userService.count());
            result.put("productCount", productService.count());

            // 当日订单数 + 当日销售额
            List<Orders> todayOrders = getPaidOrders(todayStart, todayStart.plusDays(1), null);
            result.put("todayOrderCount", ordersService.count(
                    new LambdaQueryWrapper<Orders>().between(Orders::getCreateTime, todayStart, todayStart.plusDays(1))));
            result.put("todaySalesAmount", sumAmount(todayOrders));

            // 当月销售额 + 当月订单数
            List<Orders> monthOrders = getPaidOrders(monthStart, monthStart.plusMonths(1), null);
            result.put("monthSalesAmount", sumAmount(monthOrders));
            result.put("monthOrderCount", ordersService.count(
                    new LambdaQueryWrapper<Orders>().between(Orders::getCreateTime, monthStart, monthStart.plusMonths(1))));
        } else if (role == 1) {
            LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
            productWrapper.eq(Product::getMerchantId, userId);
            result.put("productCount", productService.count(productWrapper));

            LambdaQueryWrapper<Orders> pendingWrapper = new LambdaQueryWrapper<>();
            pendingWrapper.eq(Orders::getMerchantId, userId);
            pendingWrapper.eq(Orders::getStatus, 2);
            result.put("pendingShipCount", ordersService.count(pendingWrapper));

            // 当日销售额 + 当月销售额
            List<Orders> todayOrders = getPaidOrders(todayStart, todayStart.plusDays(1), userId);
            List<Orders> monthOrders = getPaidOrders(monthStart, monthStart.plusMonths(1), userId);
            result.put("todaySalesAmount", sumAmount(todayOrders));
            result.put("monthSalesAmount", sumAmount(monthOrders));

            // 待回复评价
            LambdaQueryWrapper<Product> pWrapper = new LambdaQueryWrapper<>();
            pWrapper.eq(Product::getMerchantId, userId);
            pWrapper.select(Product::getId);
            List<Product> products = productService.list(pWrapper);
            List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            long pendingReply = 0L;
            if (!productIds.isEmpty()) {
                LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
                reviewWrapper.in(Review::getProductId, productIds);
                reviewWrapper.and(w -> w.isNull(Review::getReply).or().eq(Review::getReply, ""));
                pendingReply = reviewService.count(reviewWrapper);
            }
            result.put("pendingReplyCount", pendingReply);
        } else {
            LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(Orders::getUserId, userId);
            result.put("orderCount", ordersService.count(orderWrapper));

            LambdaQueryWrapper<Cart> cartWrapper = new LambdaQueryWrapper<>();
            cartWrapper.eq(Cart::getUserId, userId);
            result.put("cartCount", cartService.count(cartWrapper));

            LambdaQueryWrapper<Address> addressWrapper = new LambdaQueryWrapper<>();
            addressWrapper.eq(Address::getUserId, userId);
            result.put("addressCount", addressService.count(addressWrapper));
            result.put("favoriteCount", 0);
        }

        return Result.success(result);
    }

    // ======================== 管理员数据大屏 ========================

    @RequireRole(0)
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(
            @RequestParam(required = false, defaultValue = "7days") String range) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime[] rangeArr = getTimeRange(range);
        int trendDays = getTrendDays(range);

        // ---- 核心指标卡 ----
        // 总销售额
        List<Orders> allPaid = getPaidOrders(null, null, null);
        BigDecimal totalSales = sumAmount(allPaid);
        result.put("totalSales", totalSales);
        result.put("totalOrders", ordersService.count());
        result.put("totalUsers", userService.count());
        result.put("totalProducts", productService.count());

        // 当日 + 当月
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        BigDecimal todaySales = sumAmount(getPaidOrders(todayStart, todayStart.plusDays(1), null));
        BigDecimal monthSales = sumAmount(getPaidOrders(monthStart, monthStart.plusMonths(1), null));
        result.put("todaySales", todaySales);
        result.put("monthSales", monthSales);
        result.put("todayProfit", calcProfit(todaySales));
        result.put("monthProfit", calcProfit(monthSales));

        // 指定时间范围销售额 + 利润
        BigDecimal rangeSales = sumAmount(getPaidOrders(rangeArr[0], rangeArr[1], null));
        result.put("rangeSales", rangeSales);
        result.put("rangeProfit", calcProfit(rangeSales));

        // ---- 销售额趋势折线图 ----
        List<Map<String, Object>> salesTrend = new ArrayList<>();
        for (int i = trendDays - 1; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            BigDecimal dayAmount = sumAmount(getPaidOrders(dayStart, dayStart.plusDays(1), null));
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dayStart.format(DateTimeFormatter.ofPattern("MM-dd")));
            dayData.put("amount", dayAmount);
            salesTrend.add(dayData);
        }
        result.put("salesTrend", salesTrend);

        // ---- 订单量趋势柱状图 ----
        List<Map<String, Object>> orderTrend = new ArrayList<>();
        for (int i = trendDays - 1; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            long count = ordersService.count(new LambdaQueryWrapper<Orders>()
                    .between(Orders::getCreateTime, dayStart, dayStart.plusDays(1)));
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dayStart.format(DateTimeFormatter.ofPattern("MM-dd")));
            dayData.put("count", count);
            orderTrend.add(dayData);
        }
        result.put("orderTrend", orderTrend);

        // ---- 分类销量饼图 ----
        List<Category> categories = categoryService.list();
        List<Map<String, Object>> categorySales = new ArrayList<>();
        for (Category category : categories) {
            LambdaQueryWrapper<Product> pw = new LambdaQueryWrapper<>();
            pw.eq(Product::getCategoryId, category.getId());
            List<Product> products = productService.list(pw);
            int totalSaleCount = products.stream()
                    .mapToInt(p -> p.getSaleCount() == null ? 0 : p.getSaleCount())
                    .sum();
            Map<String, Object> catData = new HashMap<>();
            catData.put("name", category.getName());
            catData.put("value", totalSaleCount);
            categorySales.add(catData);
        }
        categorySales.sort((a, b) -> Integer.compare((Integer) b.get("value"), (Integer) a.get("value")));
        result.put("categorySales", categorySales);

        // ---- 订单状态分布饼图 ----
        Map<String, Object> orderStatusDist = new LinkedHashMap<>();
        String[] statusNames = {"待付款", "待发货", "已发货", "已完成", "已取消", "退款中", "已退款"};
        for (int i = 0; i < statusNames.length; i++) {
            orderStatusDist.put(statusNames[i], ordersService.count(
                    new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, i + 1)));
        }
        result.put("orderStatusDist", orderStatusDist);

        // ---- 热销商品TOP5 ----
        LambdaQueryWrapper<Product> topWrapper = new LambdaQueryWrapper<>();
        topWrapper.orderByDesc(Product::getSaleCount);
        topWrapper.last("LIMIT 5");
        List<Product> topProducts = productService.list(topWrapper);
        List<Map<String, Object>> topList = new ArrayList<>();
        for (Product p : topProducts) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("image", p.getImage());
            item.put("saleCount", p.getSaleCount());
            item.put("price", p.getPrice());
            topList.add(item);
        }
        result.put("topProducts", topList);

        return Result.success(result);
    }

    // ======================== 商家统计 ========================

    @RequireRole(1)
    @GetMapping("/merchant")
    public Result<Map<String, Object>> merchant(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "7days") String range) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = new HashMap<>();
        LocalDateTime[] rangeArr = getTimeRange(range);
        int trendDays = getTrendDays(range);

        // ---- 核心指标 ----
        List<Orders> allPaid = getPaidOrders(null, null, userId);
        BigDecimal totalSales = sumAmount(allPaid);
        result.put("totalSales", totalSales);

        LambdaQueryWrapper<Orders> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Orders::getMerchantId, userId);
        result.put("totalOrders", ordersService.count(countWrapper));

        // 商品数 + 低库存商品数
        LambdaQueryWrapper<Product> pw = new LambdaQueryWrapper<>();
        pw.eq(Product::getMerchantId, userId);
        result.put("productCount", productService.count(pw));

        LambdaQueryWrapper<Product> lowStockWrapper = new LambdaQueryWrapper<>();
        lowStockWrapper.eq(Product::getMerchantId, userId);
        lowStockWrapper.le(Product::getStock, 10);
        result.put("lowStockCount", productService.count(lowStockWrapper));

        // 当日 + 当月
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        BigDecimal todaySales = sumAmount(getPaidOrders(todayStart, todayStart.plusDays(1), userId));
        BigDecimal monthSales = sumAmount(getPaidOrders(monthStart, monthStart.plusMonths(1), userId));
        result.put("todaySales", todaySales);
        result.put("monthSales", monthSales);
        result.put("todayProfit", calcProfit(todaySales));
        result.put("monthProfit", calcProfit(monthSales));

        // 指定时间范围
        BigDecimal rangeSales = sumAmount(getPaidOrders(rangeArr[0], rangeArr[1], userId));
        result.put("rangeSales", rangeSales);
        result.put("rangeProfit", calcProfit(rangeSales));

        // ---- 销售额趋势折线图 ----
        List<Map<String, Object>> salesTrend = new ArrayList<>();
        for (int i = trendDays - 1; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            BigDecimal dayAmount = sumAmount(getPaidOrders(dayStart, dayStart.plusDays(1), userId));
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dayStart.format(DateTimeFormatter.ofPattern("MM-dd")));
            dayData.put("amount", dayAmount);
            salesTrend.add(dayData);
        }
        result.put("salesTrend", salesTrend);

        // ---- 商品分类销量饼图 ----
        LambdaQueryWrapper<Product> merchantProductWrapper = new LambdaQueryWrapper<>();
        merchantProductWrapper.eq(Product::getMerchantId, userId);
        List<Product> merchantProducts = productService.list(merchantProductWrapper);
        Map<Long, Integer> catSaleMap = new HashMap<>();
        for (Product p : merchantProducts) {
            int sc = p.getSaleCount() == null ? 0 : p.getSaleCount();
            catSaleMap.merge(p.getCategoryId(), sc, Integer::sum);
        }
        List<Map<String, Object>> categorySales = new ArrayList<>();
        for (Category c : categoryService.list()) {
            int sales = catSaleMap.getOrDefault(c.getId(), 0);
            if (sales > 0) {
                Map<String, Object> m = new HashMap<>();
                m.put("name", c.getName());
                m.put("value", sales);
                categorySales.add(m);
            }
        }
        result.put("categorySales", categorySales);

        // ---- 订单状态分布饼图 ----
        Map<String, Object> orderStatusDist = new LinkedHashMap<>();
        String[] statusNames = {"待付款", "待发货", "已发货", "已完成", "已取消", "退款中", "已退款"};
        for (int i = 0; i < statusNames.length; i++) {
            orderStatusDist.put(statusNames[i], ordersService.count(
                    new LambdaQueryWrapper<Orders>()
                            .eq(Orders::getMerchantId, userId)
                            .eq(Orders::getStatus, i + 1)));
        }
        result.put("orderStatusDist", orderStatusDist);

        // ---- 热销商品TOP5 ----
        LambdaQueryWrapper<Product> topWrapper = new LambdaQueryWrapper<>();
        topWrapper.eq(Product::getMerchantId, userId);
        topWrapper.orderByDesc(Product::getSaleCount);
        topWrapper.last("LIMIT 5");
        List<Product> topProducts = productService.list(topWrapper);
        List<Map<String, Object>> topList = new ArrayList<>();
        for (Product p : topProducts) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("image", p.getImage());
            item.put("saleCount", p.getSaleCount());
            item.put("price", p.getPrice());
            topList.add(item);
        }
        result.put("topProducts", topList);

        return Result.success(result);
    }
}
