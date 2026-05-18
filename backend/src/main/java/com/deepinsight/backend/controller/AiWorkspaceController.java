package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.service.AiWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/ai/workspace")
@RequiredArgsConstructor
public class AiWorkspaceController {

    private final AiWorkspaceService workspaceService;

    @GetMapping("/resources")
    public Result<List<Map<String, Object>>> resources(Principal principal) {
        return Result.success(workspaceService.listResources(principal));
    }

    @GetMapping("/items")
    public Result<List<Map<String, Object>>> items(
        Principal principal,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return Result.success(workspaceService.listWorkspaceItems(principal, limit));
    }

    @GetMapping("/tree")
    public Result<Map<String, Object>> tree(Principal principal) {
        return Result.success(workspaceService.tree(principal));
    }

    @PostMapping("/folders")
    public Result<Map<String, Object>> createFolder(
        Principal principal,
        @RequestBody AiWorkspaceService.FolderRequest request
    ) {
        return Result.success("文件夹已创建", workspaceService.createFolder(principal, request));
    }

    @PutMapping("/folders/{folderId}/rename")
    public Result<Map<String, Object>> renameFolder(
        Principal principal,
        @PathVariable Long folderId,
        @RequestBody AiWorkspaceService.RenameRequest request
    ) {
        return Result.success("文件夹已重命名", workspaceService.renameFolder(principal, folderId, request));
    }

    @PutMapping("/folders/{folderId}/move")
    public Result<Map<String, Object>> moveFolder(
        Principal principal,
        @PathVariable Long folderId,
        @RequestBody AiWorkspaceService.FolderMoveRequest request
    ) {
        return Result.success("文件夹已移动", workspaceService.moveFolder(principal, folderId, request));
    }

    @DeleteMapping("/folders/{folderId}")
    public Result<Void> deleteFolder(Principal principal, @PathVariable Long folderId) {
        workspaceService.deleteFolder(principal, folderId);
        return Result.success("文件夹已删除", null);
    }

    @PostMapping("/upload")
    public Result<List<Map<String, Object>>> upload(
        Principal principal,
        @RequestParam("files") MultipartFile[] files,
        @RequestParam(required = false) Long folderId
    ) {
        return Result.success("素材已上传", workspaceService.uploadMaterials(principal, files, folderId));
    }

    @PostMapping("/items")
    public Result<Map<String, Object>> saveItem(
        Principal principal,
        @RequestBody AiWorkspaceService.SaveWorkspaceItemRequest request
    ) {
        return Result.success("素材已保存", workspaceService.saveWorkspaceItem(principal, request));
    }

    @PutMapping("/items/{itemId}/rename")
    public Result<Map<String, Object>> renameItem(
        Principal principal,
        @PathVariable Long itemId,
        @RequestBody AiWorkspaceService.RenameRequest request
    ) {
        return Result.success("素材已重命名", workspaceService.renameWorkspaceItem(principal, itemId, request));
    }

    @PutMapping("/items/{itemId}/move")
    public Result<Map<String, Object>> moveItem(
        Principal principal,
        @PathVariable Long itemId,
        @RequestBody AiWorkspaceService.ItemMoveRequest request
    ) {
        return Result.success("素材已移动", workspaceService.moveWorkspaceItem(principal, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public Result<Void> deleteItem(Principal principal, @PathVariable Long itemId) {
        workspaceService.deleteWorkspaceItem(principal, itemId);
        return Result.success("素材已删除", null);
    }

    @GetMapping("/items/{itemId}/download")
    public ResponseEntity<byte[]> downloadItem(Principal principal, @PathVariable Long itemId) {
        AiWorkspaceService.DownloadFile file = workspaceService.downloadWorkspaceItem(principal, itemId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.mimeType()))
            .contentLength(file.data().length)
            .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(file.data());
    }

    @PostMapping("/visual-views")
    public Result<Map<String, Object>> saveVisualView(
        Principal principal,
        @RequestBody AiWorkspaceService.SaveWorkspaceItemRequest request
    ) {
        return Result.success("可视化视图已保存", workspaceService.saveVisualView(principal, request));
    }
}
