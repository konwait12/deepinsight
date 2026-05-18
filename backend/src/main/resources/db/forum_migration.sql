-- ============================================
-- DeepInsight 知识库 & 论坛 建表脚本
-- ============================================
USE deepinsight;

-- 知识节点表 (层级知识树)
CREATE TABLE IF NOT EXISTS knowledge_nodes (
    id          VARCHAR(50)   NOT NULL COMMENT '节点唯一标识',
    parent_id   VARCHAR(50)   DEFAULT NULL COMMENT '父节点ID',
    label       VARCHAR(100)  NOT NULL COMMENT '节点显示名称',
    category    VARCHAR(50)   DEFAULT NULL COMMENT '分类标签',
    description TEXT          DEFAULT NULL COMMENT '节点描述',
    color       VARCHAR(7)    DEFAULT '#60a5fa' COMMENT '节点颜色',
    size_val    FLOAT         DEFAULT 1.0 COMMENT '节点大小',
    sort_order  INT           DEFAULT 0 COMMENT '排序序号',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识节点表';

-- 知识文章表 (官方文章)
CREATE TABLE IF NOT EXISTS knowledge_articles (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    node_id     VARCHAR(50)   DEFAULT NULL COMMENT '关联知识节点',
    title       VARCHAR(300)  NOT NULL COMMENT '文章标题',
    content     LONGTEXT      NOT NULL COMMENT '文章内容(Markdown)',
    author_id   INT           DEFAULT NULL COMMENT '作者(管理员)',
    is_pinned   TINYINT(1)    DEFAULT 0 COMMENT '是否置顶',
    view_count  INT           DEFAULT 0 COMMENT '浏览次数',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_ka_node (node_id),
    INDEX idx_ka_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文章表(官方)';

-- 论坛帖子表
CREATE TABLE IF NOT EXISTS forum_posts (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    title       VARCHAR(300)  NOT NULL COMMENT '帖子标题',
    content     LONGTEXT      NOT NULL COMMENT '帖子内容(Markdown)',
    user_id     INT           NOT NULL COMMENT '发帖用户',
    is_official TINYINT(1)    DEFAULT 0 COMMENT '是否官方文章',
    is_pinned   TINYINT(1)    DEFAULT 0 COMMENT '是否置顶',
    is_locked   TINYINT(1)    DEFAULT 0 COMMENT '是否锁定',
    view_count  INT           DEFAULT 0 COMMENT '浏览次数',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_fp_user (user_id),
    INDEX idx_fp_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛帖子表';

-- 论坛评论表
CREATE TABLE IF NOT EXISTS forum_comments (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    post_id     BIGINT        NOT NULL COMMENT '所属帖子',
    user_id     INT           NOT NULL COMMENT '评论用户',
    content     TEXT          NOT NULL COMMENT '评论内容',
    parent_id   BIGINT        DEFAULT NULL COMMENT '父评论ID',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_fc_post (post_id),
    INDEX idx_fc_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛评论表';

-- 插入默认知识节点
INSERT IGNORE INTO knowledge_nodes (id, parent_id, label, category, description, color, size_val, sort_order) VALUES
('d', NULL, 'DeepInsight', '平台核心', '深度学习全流程可视化分析平台', '#4dc9f0', 2.5, 0),
('data', 'd', '数据管理', '功能模块', '数据集上传、预处理、增强与版本管理', '#60a5fa', 1.5, 1),
('train', 'd', '模型训练', '功能模块', '超参数配置、架构选择、训练监控', '#10b981', 1.5, 2),
('arch', 'd', '模型架构', '功能模块', 'CNN/RNN/Transformer/GAN等深度学习架构', '#8b5cf6', 1.5, 3),
('tech', 'd', '训练技巧', '功能模块', '优化器/学习率/正则化等核心技术', '#f59e0b', 1.4, 4),
('eval', 'd', '评估指标', '功能模块', '准确率/召回率/mAP/ROC等评估标准', '#ec4899', 1.3, 5),
('deploy', 'd', '部署推理', '功能模块', '模型压缩/导出/加速推理', '#14b8a6', 1.3, 6);
