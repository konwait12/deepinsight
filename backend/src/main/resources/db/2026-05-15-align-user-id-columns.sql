-- ============================================
-- DeepInsight migration: align user id references with JPA Long fields
-- Safe to run more than once on MySQL 8.
-- ============================================

USE deepinsight;

SET @db_name = DATABASE();

-- Drop legacy foreign key before changing model_registry.created_by.
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = @db_name
      AND TABLE_NAME = 'model_registry'
      AND CONSTRAINT_NAME = 'fk_model_user'
      AND REFERENCED_TABLE_NAME IS NOT NULL
);
SET @sql = IF(@fk_exists > 0,
    'ALTER TABLE model_registry DROP FOREIGN KEY fk_model_user',
    'SELECT ''fk_model_user already absent''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE users MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE model_registry MODIFY COLUMN created_by BIGINT NULL;
ALTER TABLE training_jobs MODIFY COLUMN created_by BIGINT NULL;
ALTER TABLE datasets MODIFY COLUMN uploaded_by BIGINT NULL;
ALTER TABLE experiment_runs MODIFY COLUMN user_id BIGINT NULL;
ALTER TABLE knowledge_articles MODIFY COLUMN author_id BIGINT NULL;
ALTER TABLE forum_posts MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE forum_comments MODIFY COLUMN user_id BIGINT NOT NULL;

-- Restore the model_registry -> users relationship after type alignment.
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = @db_name
      AND TABLE_NAME = 'model_registry'
      AND CONSTRAINT_NAME = 'fk_model_user'
      AND REFERENCED_TABLE_NAME IS NOT NULL
);
SET @sql = IF(@fk_exists = 0,
    'ALTER TABLE model_registry ADD CONSTRAINT fk_model_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL',
    'SELECT ''fk_model_user already present''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
