package com.mushroom.controller;

import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 通用控制器 (文件上传)
 *
 * 安全要点：
 * 1. 扩展名白名单 —— 上传目录通过 /static/** 直接对外暴露，若允许 .html/.js/.svg，
 *    可被同源访问执行脚本，进而窃取存在 sessionStorage 中的 token（存储型 XSS）
 * 2. 大小上限校验
 * 3. 文件名判空/判扩展名，避免 substring(-1) 抛异常
 * 4. 落盘使用随机文件名 + new File(dir, name)，避免路径穿越与覆盖
 */
@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    /** 允许上传的扩展名：仅图片与视频 */
    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "mp4", "webm");

    /** 单文件大小上限 100MB (与 application.yml 的 multipart 配置保持一致) */
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;

    @Value("${mushroom.upload-path}")
    private String uploadPath;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过 100MB");
        }

        // 先校验文件名，再取扩展名（原名可能为 null 或不含"."）
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty() || !originalName.contains(".")) {
            throw new BusinessException("文件名不合法，请选择带扩展名的文件");
        }
        String suffix = originalName.substring(originalName.lastIndexOf(".") + 1)
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(suffix)) {
            throw new BusinessException("不支持的文件类型，仅允许上传图片或视频");
        }
        // 扩展名是可以随便改的（把 .html 改成 .png 一样能过上面那关），
        // 这里再校验文件头，确保内容和扩展名对得上
        if (!matchesMagic(file, suffix)) {
            throw new BusinessException("文件内容与扩展名不匹配，请上传真实的图片或视频");
        }

        // 随机文件名，保留扩展名
        String newName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;

        // 确保目录存在
        File dir = new File(uploadPath);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("上传目录创建失败: {}", uploadPath);
            throw new BusinessException("上传目录创建失败");
        }

        // 保存文件
        try {
            file.transferTo(new File(dir, newName));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }

        Map<String, String> result = new HashMap<>();
        result.put("name", newName);
        result.put("url", "/api/static/" + newName);
        return Result.success("上传成功", result);
    }

    /**
     * 校验文件头（magic number）与扩展名是否匹配
     *
     * 只查扩展名的话，把任意文本文件改名成 .png 就能落盘；虽然本项目的静态目录
     * 不会执行脚本，但仍应在入口挡掉，避免上传目录变成任意文件寄存点。
     */
    private boolean matchesMagic(MultipartFile file, String suffix) {
        byte[] head = new byte[12];
        try (java.io.InputStream in = file.getInputStream()) {
            int total = 0;
            while (total < head.length) {
                int n = in.read(head, total, head.length - total);
                if (n < 0) break;
                total += n;
            }
            if (total < 4) return false;
        } catch (IOException e) {
            log.warn("读取上传文件头失败", e);
            return false;
        }

        switch (suffix) {
            case "jpg":
            case "jpeg":
                return (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case "png":
                return (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
            case "gif":
                return head[0] == 'G' && head[1] == 'I' && head[2] == 'F';
            case "bmp":
                return head[0] == 'B' && head[1] == 'M';
            case "webp":
                return head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                        && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
            case "mp4":
                // 前 4 字节是 box 长度，5~8 字节固定为 ftyp
                return head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p';
            case "webm":
                return (head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45
                        && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3;
            default:
                return false;
        }
    }
}
