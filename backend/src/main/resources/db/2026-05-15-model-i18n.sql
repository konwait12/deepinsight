-- ============================================
-- DeepInsight model registry i18n metadata
-- Adds Chinese display fields while preserving canonical English model names.
-- ============================================

USE deepinsight;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'deepinsight' AND TABLE_NAME = 'model_registry' AND COLUMN_NAME = 'display_name_zh');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE model_registry ADD COLUMN display_name_zh VARCHAR(200) NULL AFTER name',
    'SELECT ''display_name_zh already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'deepinsight' AND TABLE_NAME = 'model_registry' AND COLUMN_NAME = 'task_type_zh');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE model_registry ADD COLUMN task_type_zh VARCHAR(80) NULL AFTER task_type',
    'SELECT ''task_type_zh already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'deepinsight' AND TABLE_NAME = 'model_registry' AND COLUMN_NAME = 'description_zh');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE model_registry ADD COLUMN description_zh TEXT NULL AFTER description',
    'SELECT ''description_zh already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE model_registry SET display_name_zh = '残差网络 50 层', task_type_zh = '图像分类' WHERE name = 'ResNet-50';
UPDATE model_registry SET display_name_zh = '残差网络 101 层', task_type_zh = '图像分类' WHERE name = 'ResNet-101';
UPDATE model_registry SET display_name_zh = 'VGG 19 层卷积网络', task_type_zh = '图像分类' WHERE name = 'VGG-19';
UPDATE model_registry SET display_name_zh = '高效卷积网络 B4', task_type_zh = '图像分类' WHERE name = 'EfficientNet-B4';
UPDATE model_registry SET display_name_zh = '视觉 Transformer 基础版', task_type_zh = '图像分类' WHERE name = 'ViT-B/16';
UPDATE model_registry SET display_name_zh = 'Swin Transformer 轻量版', task_type_zh = '图像分类' WHERE name = 'Swin-T';
UPDATE model_registry SET display_name_zh = '现代化卷积网络轻量版', task_type_zh = '图像分类' WHERE name = 'ConvNeXt-T';
UPDATE model_registry SET display_name_zh = '移动端高效网络大版', task_type_zh = '图像分类' WHERE name = 'MobileNetV3-L';
UPDATE model_registry SET display_name_zh = '密集连接卷积网络 201 层', task_type_zh = '图像分类' WHERE name = 'DenseNet-201';
UPDATE model_registry SET display_name_zh = 'YOLOv8 Nano 实时检测', task_type_zh = '目标检测' WHERE name = 'YOLOv8n';
UPDATE model_registry SET display_name_zh = 'YOLOv8 Small 目标检测', task_type_zh = '目标检测' WHERE name = 'YOLOv8s';
UPDATE model_registry SET display_name_zh = 'DeepLabV3 语义分割', task_type_zh = '语义分割' WHERE name = 'DeepLabV3-RN50';
UPDATE model_registry SET display_name_zh = '深度因子分解推荐模型', task_type_zh = '推荐系统' WHERE name = 'DeepFM';
UPDATE model_registry SET display_name_zh = '宽深联合推荐模型', task_type_zh = '推荐系统' WHERE name = 'Wide&Deep';
UPDATE model_registry SET display_name_zh = '神经协同过滤模型', task_type_zh = '推荐系统' WHERE name = 'NCF';
UPDATE model_registry SET display_name_zh = '深度兴趣网络', task_type_zh = '推荐系统' WHERE name = 'DIN';
UPDATE model_registry SET display_name_zh = 'BERT 文本理解基础版', task_type_zh = '自然语言处理' WHERE name = 'BERT-Base';
UPDATE model_registry SET display_name_zh = 'GPT-2 文本生成模型', task_type_zh = '自然语言处理' WHERE name = 'GPT-2';
UPDATE model_registry SET display_name_zh = 'T5 文本到文本小型版', task_type_zh = '自然语言处理' WHERE name = 'T5-Small';
UPDATE model_registry SET display_name_zh = 'LLaMA 7B 大语言模型', task_type_zh = '自然语言处理' WHERE name = 'LLaMA-7B';

UPDATE model_registry
SET task_type_zh = CASE task_type
    WHEN 'classification' THEN '图像分类'
    WHEN 'detection' THEN '目标检测'
    WHEN 'segmentation' THEN '语义分割'
    WHEN 'recommendation' THEN '推荐系统'
    WHEN 'nlp' THEN '自然语言处理'
    ELSE '其他任务'
END
WHERE task_type_zh IS NULL OR task_type_zh = '';
