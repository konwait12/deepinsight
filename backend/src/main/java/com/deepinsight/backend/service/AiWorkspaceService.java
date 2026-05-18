package com.deepinsight.backend.service;

import com.deepinsight.backend.entity.Dataset;
import com.deepinsight.backend.entity.ExperimentRun;
import com.deepinsight.backend.entity.ModelRegistry;
import com.deepinsight.backend.entity.TrainingJob;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.DatasetRepository;
import com.deepinsight.backend.repository.ExperimentRunRepository;
import com.deepinsight.backend.repository.ModelRegistryRepository;
import com.deepinsight.backend.repository.TrainingJobRepository;
import com.deepinsight.backend.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.Principal;
import java.sql.Blob;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiWorkspaceService {

    public record SaveWorkspaceItemRequest(
        String title,
        String itemType,
        String sourceType,
        String format,
        String content,
        String imageDataUrl,
        Long folderId,
        Integer sortOrder,
        Map<String, Object> payload
    ) {}

    public record FolderRequest(String name, Long parentId) {}
    public record FolderMoveRequest(Long parentId, Integer sortOrder) {}
    public record ItemMoveRequest(Long folderId, Integer sortOrder) {}
    public record RenameRequest(String name) {}
    public record DownloadFile(String fileName, String mimeType, byte[] data) {}

    private static final Set<String> INLINE_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> TEXT_EXT = Set.of("md", "txt", "csv", "json", "svg", "log", "xml", "yaml", "yml");
    private static final Set<String> BLOCKED_EXT = Set.of(
        "exe", "bat", "cmd", "com", "msi", "scr", "ps1", "vbs", "js", "jar", "sh", "dll", "sys", "reg"
    );
    private static final Set<String> SAFE_MIME_PREFIXES = Set.of("image/", "text/", "application/pdf");
    private static final Set<String> SAFE_MIME_TYPES = Set.of(
        "application/json",
        "application/xml",
        "application/zip",
        "application/x-zip-compressed",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/msword",
        "application/vnd.ms-excel",
        "application/octet-stream"
    );
    private static final long MAX_TEXT_CONTEXT_BYTES = 80_000L;
    private static final long MAX_IMAGE_INLINE_BYTES = 6_000_000L;
    private static final long MAX_UPLOAD_BYTES = 50L * 1024L * 1024L;

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final TrainingJobRepository trainingJobRepository;
    private final DatasetRepository datasetRepository;
    private final ModelRegistryRepository modelRegistryRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initializeStorage() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS ai_workspace_folders (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL,
              parent_id BIGINT NULL,
              name VARCHAR(160) NOT NULL,
              sort_order INT NOT NULL DEFAULT 0,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_ai_workspace_folders_user_parent (user_id, parent_id),
              CONSTRAINT fk_ai_workspace_folder_parent
                FOREIGN KEY (parent_id) REFERENCES ai_workspace_folders(id)
                ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS ai_workspace_items (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL,
              folder_id BIGINT NULL,
              title VARCHAR(220) NOT NULL,
              item_type VARCHAR(40) NOT NULL,
              source_type VARCHAR(60) NOT NULL,
              format VARCHAR(40) NULL,
              content MEDIUMTEXT NULL,
              image_data_url LONGTEXT NULL,
              file_name VARCHAR(260) NULL,
              file_url VARCHAR(600) NULL,
              mime_type VARCHAR(160) NULL,
              file_size BIGINT NULL,
              file_blob LONGBLOB NULL,
              content_sha256 VARCHAR(64) NULL,
              sort_order INT NOT NULL DEFAULT 0,
              payload_json MEDIUMTEXT NULL,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_ai_workspace_items_user_folder (user_id, folder_id),
              KEY idx_ai_workspace_items_user_created (user_id, created_at),
              KEY idx_ai_workspace_items_user_type (user_id, item_type),
              CONSTRAINT fk_ai_workspace_item_folder
                FOREIGN KEY (folder_id) REFERENCES ai_workspace_folders(id)
                ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        ensureColumn("ai_workspace_items", "folder_id", "BIGINT NULL");
        ensureColumn("ai_workspace_items", "file_blob", "LONGBLOB NULL");
        ensureColumn("ai_workspace_items", "content_sha256", "VARCHAR(64) NULL");
        ensureColumn("ai_workspace_items", "sort_order", "INT NOT NULL DEFAULT 0");
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS visual_saved_views (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL,
              title VARCHAR(220) NOT NULL,
              view_type VARCHAR(60) NOT NULL,
              format VARCHAR(40) NULL,
              batch_id BIGINT NULL,
              content MEDIUMTEXT NULL,
              image_data_url LONGTEXT NULL,
              payload_json MEDIUMTEXT NULL,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_visual_saved_views_user_created (user_id, created_at),
              KEY idx_visual_saved_views_batch (batch_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        ensureIndex("ai_workspace_items", "idx_ai_workspace_items_user_folder", "CREATE INDEX idx_ai_workspace_items_user_folder ON ai_workspace_items (user_id, folder_id)");
        ensureIndex("ai_workspace_items", "idx_ai_workspace_items_user_created", "CREATE INDEX idx_ai_workspace_items_user_created ON ai_workspace_items (user_id, created_at)");
        ensureIndex("ai_workspace_items", "idx_ai_workspace_items_user_type", "CREATE INDEX idx_ai_workspace_items_user_type ON ai_workspace_items (user_id, item_type)");
        ensureIndex("ai_workspace_folders", "idx_ai_workspace_folders_user_parent", "CREATE INDEX idx_ai_workspace_folders_user_parent ON ai_workspace_folders (user_id, parent_id)");
    }

    public Long requireUserId(Principal principal) {
        if (principal == null) throw new IllegalArgumentException("请先登录后再使用工作素材");
        return userRepository.findByUsername(principal.getName())
            .map(User::getId)
            .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
    }

    public List<Map<String, Object>> listResources(Principal principal) {
        Long userId = requireUserId(principal);
        List<Map<String, Object>> resources = new ArrayList<>();

        trainingJobRepository.findByCreatedByOrderByCreatedAtDesc(userId).stream().limit(24).forEach(job -> {
            Map<String, Object> item = baseResource("training", job.getId(), job.getName(), job.getModelArchitecture());
            item.put("status", job.getStatus());
            item.put("summary", "训练任务 · epoch " + nullSafe(job.getCurrentEpoch()) + " · loss " + nullSafe(job.getCurrentLoss()));
            resources.add(item);
        });

        datasetRepository.findByUploadedByOrderByCreatedAtDesc(userId).stream().limit(24).forEach(dataset -> {
            Map<String, Object> item = baseResource("dataset", dataset.getId(), dataset.getName(), dataset.getTaskType());
            item.put("status", dataset.getStatus());
            item.put("summary", "数据集 · " + nullSafe(dataset.getFormat()) + " · " + nullSafe(dataset.getSampleCount()) + " samples");
            resources.add(item);
        });

        List<ModelRegistry> models = new ArrayList<>();
        models.addAll(modelRegistryRepository.findByIsOfficialTrueOrderByNameAsc().stream().limit(18).toList());
        models.addAll(modelRegistryRepository.findByCreatedByOrderByNameAsc(userId).stream().limit(18).toList());
        models.forEach(model -> {
            Map<String, Object> item = baseResource("model", model.getId(), model.getDisplayNameZh() != null ? model.getDisplayNameZh() : model.getName(), model.getTaskType());
            boolean official = Boolean.TRUE.equals(model.getIsOfficial());
            item.put("status", official ? "official" : "custom");
            item.put("summary", "模型 · " + nullSafe(model.getFramework()) + " · " + nullSafe(model.getParamCountM()) + "M 参数");
            item.put("official", official);
            item.put("readOnly", official);
            item.put("canManage", !official);
            item.put("canSync", !official);
            resources.add(item);
        });

        experimentRunRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().limit(24).forEach(run -> {
            Map<String, Object> item = baseResource("run", run.getId(), run.getName(), run.getStatus());
            item.put("status", run.getStatus());
            item.put("summary", "上传运行 · " + nullSafe(run.getLogDir()));
            resources.add(item);
        });

        jdbcTemplate.queryForList(
            """
            SELECT id, title, module_key, run_name, model_name, updated_at
            FROM visual_saved_analysis_records
            WHERE user_id = ?
            ORDER BY updated_at DESC
            LIMIT 36
            """,
            userId
        ).forEach(row -> {
            Map<String, Object> item = baseResource("analysis_result", numberToLong(row.get("id")), text(row.get("title")), text(row.get("module_key")));
            item.put("status", "saved");
            item.put("summary", text(row.get("run_name")) + " · " + text(row.get("model_name")));
            resources.add(item);
        });

        jdbcTemplate.queryForList(
            """
            SELECT id, title, view_type, format, updated_at
            FROM visual_saved_views
            WHERE user_id = ?
            ORDER BY updated_at DESC
            LIMIT 24
            """,
            userId
        ).forEach(row -> {
            Map<String, Object> item = baseResource("saved_view", numberToLong(row.get("id")), text(row.get("title")), text(row.get("view_type")));
            item.put("status", text(row.get("format")));
            item.put("summary", "已保存视图 · " + text(row.get("format")));
            resources.add(item);
        });

        return resources;
    }

    public Map<String, Object> tree(Principal principal) {
        Long userId = requireUserId(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("folders", jdbcTemplate.queryForList(
            """
            SELECT * FROM ai_workspace_folders
            WHERE user_id = ?
            ORDER BY parent_id IS NOT NULL, parent_id ASC, sort_order ASC, updated_at DESC
            """,
            userId
        ).stream().map(this::normalizeRow).toList());
        result.put("items", listWorkspaceItems(principal, 500));
        return result;
    }

    public Map<String, Object> createFolder(Principal principal, FolderRequest request) {
        Long userId = requireUserId(principal);
        String name = sanitizeDisplayName(request == null ? "" : request.name(), "新建文件夹", 120);
        Long parentId = request == null ? null : request.parentId();
        validateFolderOwner(userId, parentId);
        jdbcTemplate.update(
            """
            INSERT INTO ai_workspace_folders (user_id, parent_id, name, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, 0, ?, ?)
            """,
            userId,
            parentId,
            name,
            Timestamp.valueOf(LocalDateTime.now()),
            Timestamp.valueOf(LocalDateTime.now())
        );
        Number id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        if (id == null) throw new IllegalStateException("创建文件夹失败");
        return getFolder(userId, id.longValue());
    }

    public Map<String, Object> renameFolder(Principal principal, Long folderId, RenameRequest request) {
        Long userId = requireUserId(principal);
        validateFolderOwner(userId, folderId);
        String name = sanitizeDisplayName(request == null ? "" : request.name(), "未命名文件夹", 120);
        jdbcTemplate.update(
            "UPDATE ai_workspace_folders SET name = ?, updated_at = ? WHERE id = ? AND user_id = ?",
            name,
            Timestamp.valueOf(LocalDateTime.now()),
            folderId,
            userId
        );
        return getFolder(userId, folderId);
    }

    public Map<String, Object> moveFolder(Principal principal, Long folderId, FolderMoveRequest request) {
        Long userId = requireUserId(principal);
        validateFolderOwner(userId, folderId);
        Long parentId = request == null ? null : request.parentId();
        validateFolderOwner(userId, parentId);
        if (folderId.equals(parentId) || isFolderDescendant(userId, parentId, folderId)) {
            throw new IllegalArgumentException("不能把文件夹移动到自身或其子文件夹内");
        }
        jdbcTemplate.update(
            "UPDATE ai_workspace_folders SET parent_id = ?, sort_order = ?, updated_at = ? WHERE id = ? AND user_id = ?",
            parentId,
            request == null || request.sortOrder() == null ? 0 : request.sortOrder(),
            Timestamp.valueOf(LocalDateTime.now()),
            folderId,
            userId
        );
        return getFolder(userId, folderId);
    }

    public void deleteFolder(Principal principal, Long folderId) {
        Long userId = requireUserId(principal);
        validateFolderOwner(userId, folderId);
        jdbcTemplate.update("UPDATE ai_workspace_items SET folder_id = NULL WHERE user_id = ? AND folder_id = ?", userId, folderId);
        jdbcTemplate.update("UPDATE ai_workspace_folders SET parent_id = NULL WHERE user_id = ? AND parent_id = ?", userId, folderId);
        jdbcTemplate.update("DELETE FROM ai_workspace_folders WHERE id = ? AND user_id = ?", folderId, userId);
    }

    public List<Map<String, Object>> uploadMaterials(Principal principal, MultipartFile[] files) {
        return uploadMaterials(principal, files, null);
    }

    public List<Map<String, Object>> uploadMaterials(Principal principal, MultipartFile[] files, Long folderId) {
        Long userId = requireUserId(principal);
        validateFolderOwner(userId, folderId);
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            Path dir = Paths.get("storage", "private", "ai", String.valueOf(userId)).normalize();
            Files.createDirectories(dir);
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
                String ext = extension(original);
                String mime = safeMime(file.getContentType(), ext);
                validateUpload(original, ext, mime, file.getSize());
                byte[] bytes = file.getBytes();
                String stored = UUID.randomUUID().toString().substring(0, 12) + "_" + sanitizeFileName(original);
                Path dest = dir.resolve(stored);
                if (!dest.normalize().startsWith(dir)) throw new IllegalArgumentException("非法文件路径");
                Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                String url = "/api/v1/ai/workspace/items/" + "__PENDING__" + "/download";
                Long id = insertWorkspaceItem(
                    userId,
                    folderId,
                    original,
                    isImage(ext) ? "image" : "file",
                    "upload",
                    ext,
                    null,
                    null,
                    original,
                    url,
                    mime,
                    file.getSize(),
                    bytes,
                    sha256(bytes),
                    0,
                    Map.of("extension", ext, "storedFile", stored)
                );
                jdbcTemplate.update(
                    "UPDATE ai_workspace_items SET file_url = ? WHERE id = ? AND user_id = ?",
                    "/api/v1/ai/workspace/items/" + id + "/download",
                    id,
                    userId
                );
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", id);
                item.put("title", original);
                item.put("folderId", folderId);
                item.put("itemType", isImage(ext) ? "image" : "file");
                item.put("sourceType", "upload");
                item.put("format", ext);
                item.put("fileName", original);
                item.put("fileUrl", "/api/v1/ai/workspace/items/" + id + "/download");
                item.put("mimeType", mime);
                item.put("fileSize", file.getSize());
                results.add(item);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("上传失败：" + e.getMessage());
        }
        return results;
    }

    public Map<String, Object> saveWorkspaceItem(Principal principal, SaveWorkspaceItemRequest request) {
        Long userId = requireUserId(principal);
        if (request == null) throw new IllegalArgumentException("保存内容不能为空");
        Long id = createWorkspaceItem(
            userId,
            blankOrDefault(request.title(), "AI 工作素材"),
            blankOrDefault(request.itemType(), "text"),
            blankOrDefault(request.sourceType(), "manual"),
            request.format(),
            request.content(),
            request.imageDataUrl(),
            request.folderId(),
            request.sortOrder(),
            sanitizeUserWorkspacePayload(request.payload())
        );
        return getWorkspaceItem(userId, id);
    }

    public Long createWorkspaceItem(
        Long userId,
        String title,
        String itemType,
        String sourceType,
        String format,
        String content,
        String imageDataUrl,
        Long folderId,
        Integer sortOrder,
        Map<String, Object> payload
    ) {
        validateFolderOwner(userId, folderId);
        return insertWorkspaceItem(
            userId,
            folderId,
            blankOrDefault(title, "AI 工作素材"),
            blankOrDefault(itemType, "text"),
            blankOrDefault(sourceType, "manual"),
            format,
            content,
            imageDataUrl,
            null,
            null,
            null,
            null,
            null,
            content == null ? null : sha256(content.getBytes(StandardCharsets.UTF_8)),
            sortOrder == null ? 0 : sortOrder,
            payload == null ? Map.of() : payload
        );
    }

    public Long createWorkspaceItem(
        Long userId,
        String title,
        String itemType,
        String sourceType,
        String format,
        String content,
        String imageDataUrl,
        Map<String, Object> payload
    ) {
        return createWorkspaceItem(userId, title, itemType, sourceType, format, content, imageDataUrl, null, 0, payload);
    }

    public Map<String, Object> moveWorkspaceItem(Principal principal, Long itemId, ItemMoveRequest request) {
        Long userId = requireUserId(principal);
        validateMutableItemOwner(userId, itemId);
        Long folderId = request == null ? null : request.folderId();
        validateFolderOwner(userId, folderId);
        jdbcTemplate.update(
            "UPDATE ai_workspace_items SET folder_id = ?, sort_order = ?, updated_at = ? WHERE id = ? AND user_id = ?",
            folderId,
            request == null || request.sortOrder() == null ? 0 : request.sortOrder(),
            Timestamp.valueOf(LocalDateTime.now()),
            itemId,
            userId
        );
        return getWorkspaceItem(userId, itemId);
    }

    public Map<String, Object> renameWorkspaceItem(Principal principal, Long itemId, RenameRequest request) {
        Long userId = requireUserId(principal);
        validateMutableItemOwner(userId, itemId);
        String title = sanitizeDisplayName(request == null ? "" : request.name(), "未命名素材", 180);
        jdbcTemplate.update(
            "UPDATE ai_workspace_items SET title = ?, updated_at = ? WHERE id = ? AND user_id = ?",
            title,
            Timestamp.valueOf(LocalDateTime.now()),
            itemId,
            userId
        );
        return getWorkspaceItem(userId, itemId);
    }

    public void deleteWorkspaceItem(Principal principal, Long itemId) {
        Long userId = requireUserId(principal);
        validateMutableItemOwner(userId, itemId);
        jdbcTemplate.update("DELETE FROM ai_workspace_items WHERE id = ? AND user_id = ?", itemId, userId);
    }

    public DownloadFile downloadWorkspaceItem(Principal principal, Long itemId) {
        Long userId = requireUserId(principal);
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT file_name, title, mime_type, file_blob, content, format FROM ai_workspace_items WHERE id = ? AND user_id = ?",
            itemId,
            userId
        );
        byte[] data = bytes(row.get("file_blob"));
        if (data == null || data.length == 0) {
            String content = text(row.get("content"));
            if (content.isBlank()) throw new IllegalArgumentException("该素材没有可下载的文件内容");
            data = content.getBytes(StandardCharsets.UTF_8);
        }
        String fileName = sanitizeFileName(blankOrDefault(text(firstNonNull(row.get("file_name"), row.get("title"))), "workspace-item"));
        String mime = safeMime(text(row.get("mime_type")), text(row.get("format")));
        return new DownloadFile(fileName, mime, data);
    }

    public Map<String, Object> saveVisualView(Principal principal, SaveWorkspaceItemRequest request) {
        Long userId = requireUserId(principal);
        if (request == null) throw new IllegalArgumentException("保存视图不能为空");
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
        Long batchId = numberToLong(payload.get("batchId"));
        jdbcTemplate.update(
            """
            INSERT INTO visual_saved_views
              (user_id, title, view_type, format, batch_id, content, image_data_url, payload_json, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            userId,
            blankOrDefault(request.title(), "可视化视图"),
            blankOrDefault(request.sourceType(), "visual_analysis"),
            request.format(),
            batchId,
            request.content(),
            request.imageDataUrl(),
            toJson(payload),
            Timestamp.valueOf(LocalDateTime.now()),
            Timestamp.valueOf(LocalDateTime.now())
        );
        Map<String, Object> latest = jdbcTemplate.queryForMap(
            "SELECT * FROM visual_saved_views WHERE user_id = ? ORDER BY id DESC LIMIT 1",
            userId
        );
        return normalizeRow(latest);
    }

    public List<Map<String, Object>> listWorkspaceItems(Principal principal, int limit) {
        Long userId = requireUserId(principal);
        int bounded = Math.max(1, Math.min(limit, 500));
        return jdbcTemplate.queryForList(
            """
            SELECT *
            FROM ai_workspace_items
            WHERE user_id = ?
            ORDER BY folder_id IS NOT NULL, folder_id ASC, sort_order ASC, updated_at DESC
            LIMIT ?
            """,
            userId,
            bounded
        ).stream().map(this::withParsedPayload).toList();
    }

    public WorkspaceContext buildWorkspaceContext(Principal principal, List<?> attachments, List<?> resources) {
        Long userId = requireUserId(principal);
        StringBuilder context = new StringBuilder();
        List<Map<String, Object>> visionInputs = new ArrayList<>();

        if (attachments != null && !attachments.isEmpty()) {
            context.append("[用户上传素材]\n");
            attachments.stream().limit(12).forEach(item -> {
                if (!(item instanceof Map<?, ?> map)) {
                    context.append("- ").append(item).append("\n");
                    return;
                }
                Long id = numberToLong(map.get("id"));
                Map<String, Object> stored = id == null ? Map.of() : findWorkspaceItem(userId, id).orElse(Map.of());
                Map<?, ?> source = stored.isEmpty() ? map : stored;
                String title = text(firstNonNull(source.get("title"), source.get("fileName")));
                String itemType = text(firstNonNull(source.get("itemType"), source.get("item_type"), source.get("sourceType"), source.get("source_type")));
                String format = text(source.get("format"));
                String fileUrl = text(firstNonNull(source.get("fileUrl"), source.get("file_url")));
                String mimeType = text(firstNonNull(source.get("mimeType"), source.get("mime_type")));
                context
                    .append("- ")
                    .append(title.isBlank() ? "未命名素材" : title)
                    .append(" | 类型：").append(itemType.isBlank() ? "file" : itemType)
                    .append(" | 格式：").append(format.isBlank() ? mimeType : format)
                    .append(fileUrl.isBlank() ? "" : " | 地址：" + fileUrl)
                    .append("\n");

                appendStoredContent(context, stored);
                collectVisionInput(userId, stored, visionInputs);
            });
        }

        if (resources != null && !resources.isEmpty()) {
            context.append("[站内资源上下文]\n");
            resources.stream().limit(18).forEach(item -> {
                if (!(item instanceof Map<?, ?> map)) {
                    context.append("- ").append(item).append("\n");
                    return;
                }
                appendOwnedResourceContext(context, userId, map);
            });
        }
        return new WorkspaceContext(context.toString(), visionInputs);
    }

    public record WorkspaceContext(String text, List<Map<String, Object>> visionInputs) {}

    private Long insertWorkspaceItem(
        Long userId,
        Long folderId,
        String title,
        String itemType,
        String sourceType,
        String format,
        String content,
        String imageDataUrl,
        String fileName,
        String fileUrl,
        String mimeType,
        Long fileSize,
        byte[] fileBlob,
        String contentSha256,
        Integer sortOrder,
        Map<String, Object> payload
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO ai_workspace_items
              (user_id, folder_id, title, item_type, source_type, format, content, image_data_url, file_name, file_url, mime_type, file_size, file_blob, content_sha256, sort_order, payload_json, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            userId,
            folderId,
            title,
            itemType,
            sourceType,
            format,
            content,
            imageDataUrl,
            fileName,
            fileUrl,
            mimeType,
            fileSize,
            fileBlob,
            contentSha256,
            sortOrder == null ? 0 : sortOrder,
            toJson(payload == null ? Map.of() : payload),
            Timestamp.valueOf(LocalDateTime.now()),
            Timestamp.valueOf(LocalDateTime.now())
        );
        Number id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        if (id == null) throw new IllegalStateException("保存素材失败");
        return id.longValue();
    }

    private Map<String, Object> sanitizeUserWorkspacePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return Map.of();
        Map<String, Object> safe = new LinkedHashMap<>(payload);
        safe.remove("official");
        safe.remove("isOfficial");
        safe.remove("readOnly");
        safe.remove("canManage");
        safe.remove("canSync");
        return safe;
    }

    private Map<String, Object> getWorkspaceItem(Long userId, Long id) {
        return withParsedPayload(jdbcTemplate.queryForMap(
            "SELECT * FROM ai_workspace_items WHERE id = ? AND user_id = ?",
            id,
            userId
        ), true);
    }

    private Optional<Map<String, Object>> findWorkspaceItem(Long userId, Long id) {
        try {
            return Optional.of(getWorkspaceItem(userId, id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void appendStoredContent(StringBuilder context, Map<String, Object> stored) {
        if (stored == null || stored.isEmpty()) return;
        String content = text(stored.get("content"));
        if (!content.isBlank()) {
            context.append("  内容：").append(trimContext(content, 1800)).append("\n");
            return;
        }
        String fileUrl = text(stored.get("fileUrl"));
        String format = text(stored.get("format")).toLowerCase();
        if (!TEXT_EXT.contains(format)) return;
        byte[] blob = bytes(firstNonNull(stored.get("fileBlob"), stored.get("file_blob")));
        if (blob != null && blob.length <= MAX_TEXT_CONTEXT_BYTES) {
            context.append("  文件摘录：").append(trimContext(new String(blob, StandardCharsets.UTF_8), 2200)).append("\n");
            return;
        }
        if (fileUrl.isBlank()) return;
        readUploadedText(fileUrl).ifPresent(text -> context.append("  文件摘录：").append(trimContext(text, 2200)).append("\n"));
    }

    private void appendOwnedResourceContext(StringBuilder context, Long userId, Map<?, ?> map) {
        String type = text(firstNonNull(map.get("type"), map.get("itemType"), map.get("sourceType")));
        Long id = numberToLong(map.get("id"));
        if (id == null) {
            context.append("- ").append(type.isBlank() ? "resource" : type).append(" 未提供资源 ID\n");
            return;
        }
        switch (type) {
            case "training" -> trainingJobRepository.findById(id)
                .filter(job -> userId.equals(job.getCreatedBy()))
                .ifPresentOrElse(job -> context.append("- 训练任务#").append(job.getId())
                    .append(" ").append(job.getName())
                    .append(" | 架构：").append(nullSafe(job.getModelArchitecture()))
                    .append(" | 状态：").append(nullSafe(job.getStatus()))
                    .append(" | epoch：").append(nullSafe(job.getCurrentEpoch()))
                    .append(" | loss：").append(nullSafe(job.getCurrentLoss()))
                    .append(" | accuracy：").append(nullSafe(job.getCurrentAccuracy()))
                    .append(" | 配置：").append(trimContext(nullSafe(job.getConfigJson()), 900))
                    .append("\n"), () -> context.append("- 训练任务#").append(id).append(" 无权访问或不存在\n"));
            case "dataset" -> datasetRepository.findById(id)
                .filter(dataset -> userId.equals(dataset.getUploadedBy()))
                .ifPresentOrElse(dataset -> context.append("- 数据集#").append(dataset.getId())
                    .append(" ").append(dataset.getName())
                    .append(" | 任务：").append(nullSafe(dataset.getTaskType()))
                    .append(" | 格式：").append(nullSafe(dataset.getFormat()))
                    .append(" | 样本：").append(nullSafe(dataset.getSampleCount()))
                    .append(" | 类别：").append(nullSafe(dataset.getClassCount()))
                    .append(" | 描述：").append(trimContext(nullSafe(dataset.getDescription()), 900))
                    .append("\n"), () -> context.append("- 数据集#").append(id).append(" 无权访问或不存在\n"));
            case "model" -> modelRegistryRepository.findById(id)
                .filter(model -> Boolean.TRUE.equals(model.getIsOfficial()) || userId.equals(model.getCreatedBy()))
                .ifPresentOrElse(model -> context.append("- 模型#").append(model.getId())
                    .append(" ").append(model.getDisplayNameZh() == null ? model.getName() : model.getDisplayNameZh())
                    .append(" | 任务：").append(nullSafe(model.getTaskType()))
                    .append(" | 框架：").append(nullSafe(model.getFramework()))
                    .append(" | 参数：").append(nullSafe(model.getParamCountM())).append("M")
                    .append(" | 输入：").append(nullSafe(model.getInputSize()))
                    .append(" | 说明：").append(trimContext(nullSafe(model.getDescriptionZh() == null ? model.getDescription() : model.getDescriptionZh()), 1100))
                    .append("\n"), () -> context.append("- 模型#").append(id).append(" 无权访问或不存在\n"));
            case "run" -> experimentRunRepository.findById(id)
                .filter(run -> userId.equals(run.getUserId()))
                .ifPresentOrElse(run -> context.append("- 上传运行#").append(run.getId())
                    .append(" ").append(run.getName())
                    .append(" | 状态：").append(nullSafe(run.getStatus()))
                    .append(" | 日志目录：").append(nullSafe(run.getLogDir()))
                    .append("\n"), () -> context.append("- 上传运行#").append(id).append(" 无权访问或不存在\n"));
            case "analysis_result" -> appendSavedAnalysisRecord(context, userId, id);
            case "saved_view" -> appendSavedVisualView(context, userId, id);
            case "context_bundle", "file", "image", "text", "upload" -> findWorkspaceItem(userId, id)
                .ifPresentOrElse(stored -> {
                    context.append("- 工作素材#").append(id).append(" ").append(text(stored.get("title")))
                        .append(" | 类型：").append(text(stored.get("itemType")))
                        .append(" | 格式：").append(text(stored.get("format"))).append("\n");
                    appendStoredContent(context, stored);
                }, () -> context.append("- 工作素材#").append(id).append(" 无权访问或不存在\n"));
            default -> context.append("- ").append(type.isBlank() ? "resource" : type)
                .append("#").append(id).append(" ")
                .append(text(map.get("title")))
                .append(" | ").append(text(firstNonNull(map.get("summary"), map.get("meta"))))
                .append("\n");
        }
    }

    private void appendSavedAnalysisRecord(StringBuilder context, Long userId, Long id) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM visual_saved_analysis_records WHERE id = ? AND user_id = ?",
                id,
                userId
            );
            Map<String, Object> snapshot = parseMap(row.get("snapshot_json"));
            context.append("- 矩阵分析#").append(id)
                .append(" ").append(text(row.get("title")))
                .append(" | 模块：").append(text(row.get("module_key")))
                .append(" | 运行：").append(text(row.get("run_name")))
                .append(" | 模型：").append(text(row.get("model_name")))
                .append("\n  摘要：").append(trimContext(text(row.get("summary")), 1200))
                .append("\n  快照：").append(trimContext(toJson(snapshot), 1800))
                .append("\n");
        } catch (Exception e) {
            context.append("- 矩阵分析#").append(id).append(" 无权访问或不存在\n");
        }
    }

    private void appendSavedVisualView(StringBuilder context, Long userId, Long id) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM visual_saved_views WHERE id = ? AND user_id = ?",
                id,
                userId
            );
            context.append("- 可视化视图#").append(id)
                .append(" ").append(text(row.get("title")))
                .append(" | 类型：").append(text(row.get("view_type")))
                .append(" | 格式：").append(text(row.get("format")))
                .append("\n  内容：").append(trimContext(text(row.get("content")), 2600))
                .append("\n");
        } catch (Exception e) {
            context.append("- 可视化视图#").append(id).append(" 无权访问或不存在\n");
        }
    }

    private void collectVisionInput(Long userId, Map<String, Object> stored, List<Map<String, Object>> visionInputs) {
        if (stored == null || stored.isEmpty() || visionInputs.size() >= 8) return;
        String itemType = text(stored.get("itemType"));
        String format = text(stored.get("format")).toLowerCase();
        if (!"image".equals(itemType) && !isImage(format)) return;
        byte[] blob = bytes(firstNonNull(stored.get("fileBlob"), stored.get("file_blob")));
        if (blob != null && blob.length > 0 && blob.length <= MAX_IMAGE_INLINE_BYTES) {
            String mime = text(firstNonNull(stored.get("mimeType"), stored.get("mime_type")));
            if (mime.isBlank()) mime = mimeForImage(format);
            String data = Base64.getEncoder().encodeToString(blob);
            visionInputs.add(Map.of(
                "title", text(stored.get("title")),
                "mimeType", mime,
                "data", data,
                "dataUrl", "data:" + mime + ";base64," + data
            ));
            return;
        }
    }

    private Optional<String> readUploadedText(String fileUrl) {
        return Optional.empty();
    }

    private Map<String, Object> baseResource(String type, Long id, String title, String meta) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("id", id);
        item.put("title", title == null || title.isBlank() ? type + " #" + id : title);
        item.put("meta", meta == null ? "" : meta);
        item.put("official", false);
        item.put("readOnly", false);
        item.put("canManage", true);
        item.put("canSync", true);
        return item;
    }

    private Map<String, Object> withParsedPayload(Map<String, Object> row) {
        return withParsedPayload(row, false);
    }

    private Map<String, Object> withParsedPayload(Map<String, Object> row, boolean includeBlob) {
        Map<String, Object> normalized = normalizeRow(row);
        Map<String, Object> payload = parseMap(row.get("payload_json"));
        normalized.put("payload", payload);
        normalized.remove("payloadJson");
        boolean official = Boolean.TRUE.equals(payload.get("official"));
        boolean readOnly = official || Boolean.TRUE.equals(payload.get("readOnly"));
        normalized.put("official", official);
        normalized.put("readOnly", readOnly);
        normalized.put("canManage", !readOnly);
        normalized.put("canSync", false);
        if (!includeBlob) normalized.remove("fileBlob");
        return normalized;
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        return index >= 0 ? filename.substring(index + 1).toLowerCase() : "";
    }

    private boolean isImage(String ext) {
        return Set.of("jpg", "jpeg", "png", "gif", "webp", "svg", "bmp").contains(ext);
    }

    private String mimeForImage(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "bmp" -> "image/bmp";
            default -> "image/png";
        };
    }

    private void ensureColumn(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
            """,
            Integer.class,
            table,
            column
        );
        if (count == null || count > 0) return;
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    private void ensureIndex(String table, String index, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
            """,
            Integer.class,
            table,
            index
        );
        if (count == null || count > 0) return;
        jdbcTemplate.execute(ddl);
    }

    private Map<String, Object> getFolder(Long userId, Long folderId) {
        return normalizeRow(jdbcTemplate.queryForMap(
            "SELECT * FROM ai_workspace_folders WHERE id = ? AND user_id = ?",
            folderId,
            userId
        ));
    }

    private void validateFolderOwner(Long userId, Long folderId) {
        if (folderId == null) return;
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ai_workspace_folders WHERE id = ? AND user_id = ?",
            Integer.class,
            folderId,
            userId
        );
        if (count == null || count == 0) throw new IllegalArgumentException("目标文件夹不存在或无权访问");
    }

    private void validateItemOwner(Long userId, Long itemId) {
        if (itemId == null) throw new IllegalArgumentException("素材 ID 不能为空");
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ai_workspace_items WHERE id = ? AND user_id = ?",
            Integer.class,
            itemId,
            userId
        );
        if (count == null || count == 0) throw new IllegalArgumentException("素材不存在或无权访问");
    }

    private void validateMutableItemOwner(Long userId, Long itemId) {
        if (itemId == null) throw new IllegalArgumentException("素材 ID 不能为空");
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT id, payload_json FROM ai_workspace_items WHERE id = ? AND user_id = ?",
            itemId,
            userId
        );
        Map<String, Object> payload = parseMap(row.get("payload_json"));
        if (Boolean.TRUE.equals(payload.get("official")) || Boolean.TRUE.equals(payload.get("readOnly"))) {
            throw new IllegalArgumentException("官方或只读素材不能在普通云端工作区移动、重命名或删除，请进入管理后台处理。");
        }
    }

    private boolean isFolderDescendant(Long userId, Long candidateParentId, Long folderId) {
        if (candidateParentId == null || folderId == null) return false;
        Long current = candidateParentId;
        Set<Long> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            if (current.equals(folderId)) return true;
            try {
                current = jdbcTemplate.queryForObject(
                    "SELECT parent_id FROM ai_workspace_folders WHERE id = ? AND user_id = ?",
                    Long.class,
                    current,
                    userId
                );
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private String sanitizeDisplayName(String name, String fallback, int maxLength) {
        String value = blankOrDefault(name, fallback)
            .replaceAll("[\\p{Cntrl}&&[^\n\t]]", "")
            .replaceAll("[\\\\/:*?\"<>|]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (value.isBlank()) value = fallback;
        if (value.length() > maxLength) value = value.substring(0, maxLength).trim();
        return value;
    }

    private void validateUpload(String originalName, String ext, String mime, long size) {
        if (size <= 0) throw new IllegalArgumentException("文件为空");
        if (size > MAX_UPLOAD_BYTES) throw new IllegalArgumentException("单个文件不能超过 50MB");
        String safeName = sanitizeFileName(originalName);
        if (safeName.isBlank() || safeName.equals(".") || safeName.equals("..")) throw new IllegalArgumentException("文件名无效");
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            throw new IllegalArgumentException("文件名不能包含路径");
        }
        if (!ext.isBlank() && BLOCKED_EXT.contains(ext)) throw new IllegalArgumentException("该文件类型不允许上传");
        boolean safeMime = SAFE_MIME_PREFIXES.stream().anyMatch(mime::startsWith) || SAFE_MIME_TYPES.contains(mime);
        if (!safeMime) throw new IllegalArgumentException("该 MIME 类型不允许上传");
    }

    private String safeMime(String contentType, String ext) {
        String mime = contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase();
        if (mime.isBlank() || "application/octet-stream".equals(mime)) {
            if (isImage(ext)) return mimeForImage(ext);
            if (TEXT_EXT.contains(ext)) return "text/plain";
            if ("pdf".equals(ext)) return "application/pdf";
            if ("zip".equals(ext)) return "application/zip";
            if ("docx".equals(ext)) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            if ("xlsx".equals(ext)) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            if ("pptx".equals(ext)) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            return "application/octet-stream";
        }
        return mime;
    }

    private String sha256(byte[] data) {
        if (data == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("文件校验失败");
        }
    }

    private byte[] bytes(Object value) {
        if (value == null) return null;
        if (value instanceof byte[] data) return data;
        if (value instanceof Blob blob) {
            try {
                return blob.getBytes(1, Math.toIntExact(blob.length()));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String sanitizeFileName(String name) {
        String cleaned = name == null ? "file" : name
            .replace("\\", "_")
            .replace("/", "_")
            .replaceAll("[\\p{Cntrl}]", "")
            .replaceAll("[:*?\"<>|]", "_")
            .replaceAll("\\s+", " ")
            .trim();
        if (cleaned.isBlank() || cleaned.equals(".") || cleaned.equals("..")) return "file";
        return cleaned.length() > 180 ? cleaned.substring(0, 180).trim() : cleaned;
    }

    private String blankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Object firstNonNull(Object... values) {
        if (values == null) return null;
        for (Object value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private String trimContext(String value, int maxChars) {
        if (value == null) return "";
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxChars) return normalized;
        return normalized.substring(0, maxChars) + "...";
    }

    private Long numberToLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> parseMap(Object value) {
        if (value == null) return Map.of();
        try {
            return objectMapper.readValue(String.valueOf(value), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(toCamel(key), value));
        return normalized;
    }

    private String toCamel(String input) {
        StringBuilder out = new StringBuilder();
        boolean upperNext = false;
        for (char c : input.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                out.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
