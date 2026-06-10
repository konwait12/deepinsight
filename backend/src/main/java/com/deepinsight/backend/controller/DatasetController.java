package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.Dataset;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.DatasetRepository;
import com.deepinsight.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private static final Set<String> ALLOWED_UPLOAD_EXTENSIONS = Set.of("csv", "zip");
    private static final int PREVIEW_LIMIT = 100;

    private final DatasetRepository repository;
    private final UserRepository userRepository;

    private Long getUserId(Principal p) {
        if (p == null) return null;
        return userRepository.findByUsername(p.getName()).map(User::getId).orElse(null);
    }

    @GetMapping
    public Result<List<Dataset>> list(@RequestParam(required = false) String taskType,
                                      @RequestParam(required = false) String status,
                                      Principal principal) {
        List<Dataset> all = taskType != null ? repository.findByTaskType(taskType) :
                            status != null ? repository.findByStatusOrderByCreatedAtDesc(status) :
                            repository.findAll();
        Long uid = getUserId(principal);
        if (uid != null) {
            final Long id = uid;
            all = all.stream().filter(d -> id.equals(d.getUploadedBy())).toList();
        }
        return Result.success(all);
    }

    @GetMapping("/{id}")
    public Result<Dataset> get(@PathVariable Long id, Principal principal) {
        Dataset ds = repository.findById(id).orElse(null);
        if (ds == null) return Result.error(404, "Not found");
        Long uid = getUserId(principal);
        if (uid != null && ds.getUploadedBy() != null && !ds.getUploadedBy().equals(uid)) {
            return Result.error(403, "无权查看");
        }
        return Result.success(ds);
    }

    @PostMapping
    public Result<Dataset> create(@RequestBody Dataset dataset, Principal principal) {
        Long uid = getUserId(principal);
        if (uid != null) dataset.setUploadedBy(uid);
        return Result.success("数据集注册成功", repository.save(dataset));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Dataset> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam(required = false) String name,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false) String taskType,
                                  Principal principal) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要上传的数据集文件");
        }

        String originalName = safeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_UPLOAD_EXTENSIONS.contains(extension)) {
            return Result.error(400, "仅支持 CSV 或 ZIP 数据集文件");
        }

        try {
            Path dir = uploadRoot().resolve(LocalDate.now().toString());
            Files.createDirectories(dir);

            String storedName = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "_" + originalName;
            Path dest = dir.resolve(storedName).normalize();
            if (!dest.startsWith(uploadRoot())) {
                return Result.error(400, "非法文件名");
            }
            file.transferTo(dest);

            PreviewSummary summary = "zip".equals(extension) ? summarizeZip(dest) : summarizeCsv(dest);
            Dataset dataset = new Dataset();
            dataset.setName(hasText(name) ? name.trim() : stripExtension(originalName));
            dataset.setDescription(hasText(description) ? description.trim() : defaultDescription(extension));
            dataset.setTaskType(hasText(taskType) ? taskType.trim() : "other");
            dataset.setFormat(extension);
            dataset.setFilePath(projectRoot().relativize(dest).toString());
            dataset.setFileSizeMb(roundMb(file.getSize()));
            dataset.setSampleCount(summary.totalRows());
            dataset.setClassCount(summary.featureCount());
            dataset.setSplitRatio("-");
            dataset.setStatus("ready");

            Long uid = getUserId(principal);
            if (uid != null) dataset.setUploadedBy(uid);
            return Result.success("数据集文件已保存", repository.save(dataset));
        } catch (Exception ex) {
            return Result.error(500, "上传失败: " + ex.getMessage());
        }
    }

    @GetMapping("/{id}/preview")
    public Result<Map<String, Object>> preview(@PathVariable Long id, Principal principal) {
        Dataset ds = repository.findById(id).orElse(null);
        if (ds == null) return Result.error(404, "Not found");
        Long uid = getUserId(principal);
        if (uid != null && ds.getUploadedBy() != null && !ds.getUploadedBy().equals(uid)) {
            return Result.error(403, "无权查看");
        }

        Path stored = resolveStoredDataset(ds.getFilePath());
        if (stored == null || !Files.isRegularFile(stored)) {
            return Result.success(metadataPreview(ds, "未找到可解析的已保存文件，仅返回元数据。"));
        }

        try {
            String extension = extensionOf(stored.getFileName().toString());
            if ("zip".equals(extension)) return Result.success(zipPreview(ds, stored));
            if ("csv".equals(extension)) return Result.success(csvPreview(ds, stored));
            return Result.success(metadataPreview(ds, "文件格式不支持在线预览，仅返回元数据。"));
        } catch (Exception ex) {
            return Result.error(500, "预览读取失败: " + ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Dataset> update(@PathVariable Long id, @RequestBody Dataset dataset, Principal principal) {
        Dataset existing = repository.findById(id).orElseThrow();
        Long uid = getUserId(principal);
        if (uid != null && existing.getUploadedBy() != null && !existing.getUploadedBy().equals(uid)) {
            return Result.error(403, "无权操作");
        }
        dataset.setId(id);
        dataset.setUploadedBy(existing.getUploadedBy());
        return Result.success("更新成功", repository.save(dataset));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, Principal principal) {
        Dataset existing = repository.findById(id).orElseThrow();
        Long uid = getUserId(principal);
        if (uid != null && existing.getUploadedBy() != null && !existing.getUploadedBy().equals(uid)) {
            return Result.error(403, "无权操作");
        }
        repository.deleteById(id);
        deleteStoredFile(existing.getFilePath());
        return Result.success("已删除");
    }

    private Map<String, Object> csvPreview(Dataset dataset, Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return previewPayload(dataset, "csv", List.of("字段 1"), List.of(List.of("空文件")), 0, 1, List.of(1), "CSV 文件为空。");
            }

            List<String> first = splitCsvLine(firstLine);
            boolean firstLooksHeader = first.stream().anyMatch(cell -> cell.matches(".*[A-Za-z_\\u4e00-\\u9fa5].*"))
                && first.stream().anyMatch(cell -> !isNumeric(cell));
            List<String> columns = firstLooksHeader
                ? first
                : java.util.stream.IntStream.range(0, first.size()).mapToObj(String::valueOf).toList();
            List<List<String>> rows = new ArrayList<>();
            List<Number> seed = new ArrayList<>();
            int totalRows = 0;

            if (!firstLooksHeader) {
                rows.add(first);
                addNumericSeed(seed, first);
                totalRows = 1;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> parsed = splitCsvLine(line);
                if (rows.size() < PREVIEW_LIMIT) {
                    rows.add(parsed);
                }
                addNumericSeed(seed, parsed);
                totalRows++;
            }

            return previewPayload(
                dataset,
                "csv",
                columns,
                rows.isEmpty() ? List.of(List.of("无样例数据")) : rows,
                totalRows,
                columns.size(),
                seed.isEmpty() ? List.of(totalRows, columns.size()) : seed,
                "已从服务器保存的 CSV 文件解析预览。"
            );
        }
    }

    private Map<String, Object> zipPreview(Dataset dataset, Path path) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        Set<String> folders = new LinkedHashSet<>();
        int total = 0;

        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                total++;
                String entryName = entry.getName();
                String folder = folderName(entryName);
                if (!folder.isBlank()) {
                    folders.add(folder);
                }
                if (rows.size() < PREVIEW_LIMIT) {
                    rows.add(List.of(entryName, folder.isBlank() ? "-" : folder, formatBytes(entry.getSize()), "stored"));
                }
            }
        }

        int storedClassCount = dataset.getClassCount() == null ? 0 : dataset.getClassCount();
        int featureCount = folders.isEmpty() ? Math.max(storedClassCount, 1) : folders.size();
        return previewPayload(
            dataset,
            "zip",
            List.of("文件名", "目录/类别", "大小", "状态"),
            rows.isEmpty() ? List.of(List.of(path.getFileName().toString(), "-", "-", "empty zip")) : rows,
            total,
            featureCount,
            List.of(total, featureCount, rows.size()),
            "已从服务器保存的 ZIP 文件读取条目预览。"
        );
    }

    private PreviewSummary summarizeCsv(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return new PreviewSummary(0, 1);
            }
            List<String> first = splitCsvLine(firstLine);
            boolean firstLooksHeader = first.stream().anyMatch(cell -> cell.matches(".*[A-Za-z_\\u4e00-\\u9fa5].*"))
                && first.stream().anyMatch(cell -> !isNumeric(cell));
            int totalRows = firstLooksHeader ? 0 : 1;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    totalRows++;
                }
            }
            return new PreviewSummary(totalRows, first.size());
        }
    }

    private PreviewSummary summarizeZip(Path path) throws IOException {
        int total = 0;
        Set<String> folders = new LinkedHashSet<>();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                total++;
                String folder = folderName(entry.getName());
                if (!folder.isBlank()) {
                    folders.add(folder);
                }
            }
        }
        return new PreviewSummary(total, Math.max(folders.size(), 1));
    }

    private Map<String, Object> metadataPreview(Dataset dataset, String message) {
        int totalRows = dataset.getSampleCount() == null ? 0 : dataset.getSampleCount();
        int featureCount = dataset.getClassCount() == null ? 0 : dataset.getClassCount();
        return previewPayload(
            dataset,
            dataset.getFormat() == null ? "metadata" : dataset.getFormat(),
            List.of("字段", "值"),
            List.of(
                List.of("任务类型", valueOr(dataset.getTaskType(), "-")),
                List.of("格式", valueOr(dataset.getFormat(), "-")),
                List.of("状态", valueOr(dataset.getStatus(), "-")),
                List.of("样本数", String.valueOf(totalRows)),
                List.of("类别/字段数", String.valueOf(featureCount)),
                List.of("文件路径", valueOr(dataset.getFilePath(), "-"))
            ),
            totalRows,
            featureCount,
            List.of(totalRows, featureCount),
            message
        );
    }

    private Map<String, Object> previewPayload(Dataset dataset,
                                               String kind,
                                               List<String> columns,
                                               List<List<String>> rows,
                                               int totalRows,
                                               int featureCount,
                                               List<? extends Number> seed,
                                               String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("datasetId", dataset.getId());
        payload.put("kind", kind);
        payload.put("columns", columns);
        payload.put("rows", rows);
        payload.put("totalRows", totalRows);
        payload.put("statsRows", Math.min(totalRows, 5000));
        payload.put("featureCount", featureCount);
        payload.put("seed", seed);
        payload.put("message", message);
        return payload;
    }

    private List<String> splitCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            char next = i + 1 < line.length() ? line.charAt(i + 1) : 0;
            if (ch == '"' && quoted && next == '"') {
                current.append('"');
                i++;
            } else if (ch == '"') {
                quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cells.add(current.toString().trim());
        return cells;
    }

    private void addNumericSeed(List<Number> seed, List<String> cells) {
        if (seed.size() >= 60) return;
        for (String cell : cells) {
            if (seed.size() >= 60) return;
            try {
                seed.add(Double.parseDouble(cell.replace(",", "")));
            } catch (NumberFormatException ignored) {
                // Non-numeric preview cells are intentionally ignored for chart seeding.
            }
        }
    }

    private Path resolveStoredDataset(String filePath) {
        if (!hasText(filePath)) {
            return null;
        }
        Path root = projectRoot();
        Path resolved = root.resolve(filePath).normalize();
        Path uploads = uploadRoot();
        if (!resolved.startsWith(uploads)) {
            return null;
        }
        return resolved;
    }

    private void deleteStoredFile(String filePath) {
        Path stored = resolveStoredDataset(filePath);
        if (stored == null) {
            return;
        }
        try {
            Files.deleteIfExists(stored);
        } catch (IOException ignored) {
            // Metadata deletion should not fail because a local file was already removed or locked.
        }
    }

    private Path uploadRoot() {
        return projectRoot().resolve("uploads").resolve("datasets").normalize();
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("model")) && Files.isDirectory(current.resolve("backend"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.isDirectory(parent.resolve("model")) && Files.isDirectory(parent.resolve("backend"))) {
            return parent;
        }
        return current;
    }

    private String safeFileName(String original) {
        String name = hasText(original) ? Paths.get(original).getFileName().toString() : "dataset";
        return name.replaceAll("[^A-Za-z0-9._\\-\\u4e00-\\u9fa5]", "_");
    }

    private String extensionOf(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot >= 0 && dot + 1 < fileName.length() ? fileName.substring(dot + 1).toLowerCase() : "";
    }

    private String stripExtension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : valueOr(fileName, "uploaded_dataset");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String valueOr(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private String defaultDescription(String extension) {
        return "zip".equals(extension) ? "用户上传的 ZIP 数据集文件" : "用户上传的 CSV 数据集文件";
    }

    private Double roundMb(long bytes) {
        return Math.round((bytes / 1024d / 1024d) * 1000d) / 1000d;
    }

    private boolean isNumeric(String value) {
        try {
            Double.parseDouble(value.replace(",", ""));
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String folderName(String entryName) {
        int slash = entryName.lastIndexOf('/');
        if (slash <= 0) {
            return "";
        }
        return entryName.substring(0, slash);
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "-";
        }
        if (bytes >= 1024 * 1024) {
            return String.format("%.1f MB", bytes / 1024d / 1024d);
        }
        if (bytes >= 1024) {
            return String.format("%.1f KB", bytes / 1024d);
        }
        return bytes + " B";
    }

    private record PreviewSummary(int totalRows, int featureCount) {}
}
