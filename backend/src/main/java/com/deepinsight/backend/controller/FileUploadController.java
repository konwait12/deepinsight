package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.*;
import java.security.Principal;
import java.util.*;

/** 论坛文件上传控制器 */
@RestController
@RequestMapping("/api/v1/forum")
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads/forum/";
    private static final Set<String> ALLOWED_EXT = Set.of("jpg","jpeg","png","gif","webp","svg","bmp","ico","md","txt","csv","json","pdf");

    @PostMapping("/upload")
    public Result<List<Map<String, String>>> upload(@RequestParam("files") MultipartFile[] files, Principal principal) {
        if (principal == null) return Result.error(401, "请先登录");
        List<Map<String, String>> results = new ArrayList<>();
        try {
            Path dir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String origName = file.getOriginalFilename();
                String ext = origName != null ? origName.substring(origName.lastIndexOf('.') + 1).toLowerCase() : "";
                if (!ALLOWED_EXT.contains(ext)) continue;
                String newName = UUID.randomUUID().toString().substring(0, 8) + "_" + origName;
                Path dest = dir.resolve(newName);
                Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                String url = "/uploads/forum/" + newName;
                Map<String, String> info = new HashMap<>();
                info.put("name", origName);
                info.put("url", url);
                info.put("size", String.valueOf(file.getSize()));
                results.add(info);
            }
        } catch (Exception e) {
            return Result.error(500, "上传失败: " + e.getMessage());
        }
        return Result.success(results);
    }
}
