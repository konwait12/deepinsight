USE deepinsight;

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forum_posts ADD COLUMN cover_url VARCHAR(500) NULL AFTER content',
        'SELECT ''forum_posts.cover_url already exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'forum_posts'
      AND COLUMN_NAME = 'cover_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forum_posts ADD COLUMN source_type VARCHAR(40) NULL AFTER cover_url',
        'SELECT ''forum_posts.source_type already exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'forum_posts'
      AND COLUMN_NAME = 'source_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forum_posts ADD COLUMN source_id BIGINT NULL AFTER source_type',
        'SELECT ''forum_posts.source_id already exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'forum_posts'
      AND COLUMN_NAME = 'source_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE UNIQUE INDEX uk_forum_posts_source ON forum_posts (source_type, source_id)',
        'SELECT ''uk_forum_posts_source already exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'forum_posts'
      AND INDEX_NAME = 'uk_forum_posts_source'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
