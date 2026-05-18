CREATE TABLE IF NOT EXISTS visual_analysis_batches (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  title VARCHAR(200) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'completed',
  target_count INT NOT NULL DEFAULT 0,
  module_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_visual_analysis_batches_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS visual_analysis_results (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  run_id BIGINT NOT NULL,
  run_type VARCHAR(30) NOT NULL,
  run_name VARCHAR(200) NOT NULL,
  model_name VARCHAR(200) NULL,
  module_key VARCHAR(80) NOT NULL,
  status VARCHAR(30) NOT NULL,
  score DOUBLE NULL,
  record_count BIGINT NOT NULL DEFAULT 0,
  latest_step BIGINT NULL,
  summary TEXT NULL,
  metrics_json MEDIUMTEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_visual_analysis_results_batch (batch_id),
  KEY idx_visual_analysis_results_user_module (user_id, module_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS visual_ai_panels (
  id BIGINT NOT NULL AUTO_INCREMENT,
  result_id BIGINT NOT NULL,
  module_key VARCHAR(80) NOT NULL,
  title VARCHAR(200) NOT NULL,
  insight_text TEXT NULL,
  recommendations_json MEDIUMTEXT NULL,
  ai_model_name VARCHAR(120) NOT NULL DEFAULT 'DeepInsight 规则分析器',
  status VARCHAR(30) NOT NULL DEFAULT 'ready',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_visual_ai_panels_result (result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO model_registry
  (name, display_name_zh, task_type, task_type_zh, description, description_zh, param_count_m, input_size, framework, is_official, created_at, updated_at)
SELECT 'Whisper-Tiny', 'Whisper-Tiny', 'audio', 'audio',
       'Speech recognition baseline for audio visualization.',
       'Speech recognition baseline for audio visualization.',
       39.0, 'audio', 'pytorch', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM model_registry WHERE name = 'Whisper-Tiny');

INSERT INTO model_registry
  (name, display_name_zh, task_type, task_type_zh, description, description_zh, param_count_m, input_size, framework, is_official, created_at, updated_at)
SELECT 'Wav2Vec2-Base', 'Wav2Vec2-Base', 'audio', 'audio',
       'Self-supervised speech representation model.',
       'Self-supervised speech representation model.',
       95.0, '16kHz audio', 'pytorch', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM model_registry WHERE name = 'Wav2Vec2-Base');

INSERT INTO model_registry
  (name, display_name_zh, task_type, task_type_zh, description, description_zh, param_count_m, input_size, framework, is_official, created_at, updated_at)
SELECT 'AST-Base', 'AST-Base', 'audio', 'audio',
       'Audio Spectrogram Transformer for audio classification.',
       'Audio Spectrogram Transformer for audio classification.',
       86.0, 'spectrogram', 'pytorch', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM model_registry WHERE name = 'AST-Base');

INSERT INTO model_registry
  (name, display_name_zh, task_type, task_type_zh, description, description_zh, param_count_m, input_size, framework, is_official, created_at, updated_at)
SELECT 'CLIP-ViT-B/32', 'CLIP-ViT-B/32', 'multimodal', 'multimodal',
       'Vision-language embedding model for cross-modal projections.',
       'Vision-language embedding model for cross-modal projections.',
       151.0, '224x224x3 + text', 'pytorch', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM model_registry WHERE name = 'CLIP-ViT-B/32');
