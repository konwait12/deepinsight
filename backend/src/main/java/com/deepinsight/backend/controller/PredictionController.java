package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.ModelRegistry;
import com.deepinsight.backend.repository.ModelRegistryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/prediction")
@RequiredArgsConstructor
public class PredictionController {

    private final ModelRegistryRepository modelRepository;
    private final com.deepinsight.backend.service.BSARecClientService bsarecClientService;

    private static final String[] IMAGE_LABELS = {
        "golden retriever", "tabby cat", "german shepherd", "siberian husky",
        "african elephant", "monarch butterfly", "great white shark", "bald eagle",
        "red panda", "king penguin", "bengal tiger", "giant panda",
    };

    private static final String[] OBJECT_LABELS = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train",
        "truck", "boat", "traffic light", "fire hydrant", "stop sign",
    };

    @PostMapping("/classify")
    public Result<Map<String, Object>> classify(@RequestBody Map<String, Object> request) {
        String model = (String) request.getOrDefault("model", "ResNet-50");
        Random rng = new Random();

        List<Map<String, Object>> predictions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String label = IMAGE_LABELS[rng.nextInt(IMAGE_LABELS.length)];
            double confidence = 0.5 + rng.nextDouble() * 0.49;
            Map<String, Object> pred = new LinkedHashMap<>();
            pred.put("rank", i + 1);
            pred.put("label", label);
            pred.put("confidence", Math.round(confidence * 1000.0) / 1000.0);
            predictions.add(pred);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", model);
        result.put("topPrediction", predictions.get(0).get("label"));
        result.put("confidence", predictions.get(0).get("confidence"));
        result.put("predictions", predictions);
        result.put("inferenceTimeMs", 5 + rng.nextDouble() * 20);

        // Performance metrics
        Map<String, Object> perf = new LinkedHashMap<>();
        perf.put("fps", 120 + rng.nextInt(80));
        perf.put("latencyMs", Math.round((5 + rng.nextDouble() * 5) * 10.0) / 10.0);
        perf.put("gpuMemoryGb", 2.0 + rng.nextDouble() * 2.0);
        perf.put("throughput", 800 + rng.nextInt(400));
        result.put("performance", perf);

        return Result.success(result);
    }

    @PostMapping("/detect")
    public Result<Map<String, Object>> detect(@RequestBody Map<String, Object> request) {
        Random rng = new Random();
        int objectCount = 1 + rng.nextInt(6);

        List<Map<String, Object>> detections = new ArrayList<>();
        for (int i = 0; i < objectCount; i++) {
            Map<String, Object> det = new LinkedHashMap<>();
            det.put("label", OBJECT_LABELS[rng.nextInt(OBJECT_LABELS.length)]);
            det.put("confidence", Math.round((0.5 + rng.nextDouble() * 0.49) * 1000.0) / 1000.0);
            det.put("x", Math.round(rng.nextDouble() * 500 * 10.0) / 10.0);
            det.put("y", Math.round(rng.nextDouble() * 400 * 10.0) / 10.0);
            det.put("width", Math.round((50 + rng.nextDouble() * 200) * 10.0) / 10.0);
            det.put("height", Math.round((50 + rng.nextDouble() * 200) * 10.0) / 10.0);
            detections.add(det);
        }

        Map<String, Object> perf = new LinkedHashMap<>();
        perf.put("fps", 60 + rng.nextInt(30));
        perf.put("latencyMs", Math.round((10 + rng.nextDouble() * 10) * 10.0) / 10.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("detections", detections);
        result.put("count", objectCount);
        result.put("performance", perf);
        return Result.success(result);
    }

    @PostMapping("/recommend")
    public Result<Map<String, Object>> recommend(@RequestBody Map<String, Object> request) {
        return Result.success(bsarecClientService.recommend(request));
    }

    @GetMapping("/models")
    public Result<List<Map<String, Object>>> listModels() {
        List<Map<String, Object>> models = new ArrayList<>(modelRepository.findByIsOfficialTrueOrderByNameAsc()
            .stream()
            .map(this::modelToMap)
            .toList());
        models.add(bsarecModel());
        return Result.success(models);
    }

    private Map<String, Object> bsarecModel() {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", -1001);
        entry.put("name", "BSARec-Job");
        entry.put("displayNameZh", "BSARec 岗位推荐模型");
        entry.put("taskType", "recommendation");
        entry.put("taskTypeZh", "推荐系统");
        entry.put("params", "0.2M");
        entry.put("paramCountM", 0.2);
        entry.put("inputSize", "50 item ids");
        entry.put("framework", "pytorch/cpu");
        entry.put("description", "BSARec sequential recommendation model served by the local Flask API.");
        entry.put("descriptionZh", "通过本地 Flask API 提供 CPU 推理的 BSARec 序列推荐模型。");
        entry.put("isOfficial", true);
        entry.put("integrationType", "external-api");
        return entry;
    }

    private Map<String, Object> modelToMap(ModelRegistry model) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", model.getId());
        entry.put("name", model.getName());
        entry.put("displayNameZh", model.getDisplayNameZh());
        entry.put("taskType", model.getTaskType());
        entry.put("taskTypeZh", model.getTaskTypeZh());
        entry.put("params", model.getParamCountM() + "M");
        entry.put("paramCountM", model.getParamCountM());
        entry.put("inputSize", model.getInputSize());
        entry.put("framework", model.getFramework());
        entry.put("description", model.getDescription());
        entry.put("descriptionZh", model.getDescriptionZh());
        return entry;
    }
}
