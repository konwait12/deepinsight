package com.deepinsight.backend.config;

import com.deepinsight.backend.service.KnowledgeTrainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeTrainingBootstrap implements ApplicationRunner {

    private final KnowledgeTrainingService knowledgeTrainingService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!knowledgeTrainingService.requiresTraining()) {
                log.info("AI knowledge training index is already up to date");
                return;
            }
            KnowledgeTrainingService.TrainingReport report = knowledgeTrainingService.rebuildIndex();
            log.info(
                "AI knowledge training index rebuilt: generated={}, created={}, updated={}, embedded={}, totalEmbedded={}, models={}",
                report.generatedTrainingDocs(),
                report.createdDocs(),
                report.updatedDocs(),
                report.embeddedExistingDocs(),
                report.totalEmbeddedDocs(),
                report.modelCount()
            );
        } catch (Exception ex) {
            log.warn("AI knowledge training bootstrap skipped: {}", ex.getMessage());
        }
    }
}
