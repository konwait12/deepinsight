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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
