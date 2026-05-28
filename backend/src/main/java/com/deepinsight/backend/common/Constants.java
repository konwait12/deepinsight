package com.deepinsight.backend.common;

/**
 * 系统常量类
 * <p>
 * 集中管理JWT配置键、API路径、用户角色、响应状态码等全局常量，
 * 避免硬编码分散在各处，便于统一维护和修改。
 *
 * @author DeepInsight Team
 */
public final class Constants {

    private Constants() {
        // 工具类，禁止实例化
    }

    // ==================== JWT相关 ====================

    /** JWT密钥配置键（对应application.yml中的security.jwt.secret） */
    public static final String JWT_SECRET_KEY = "${security.jwt.secret}";

    /** JWT过期时间配置键（对应application.yml中的security.jwt.expiration） */
    public static final String JWT_EXPIRATION_KEY = "${security.jwt.expiration}";

    /** JWT Token前缀（Bearer认证方案，符合RFC 6750规范） */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** HTTP Authorization请求头名称 */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Token前缀字符数（"Bearer " = 7个字符），用于截取Token值 */
    public static final int TOKEN_PREFIX_LENGTH = 7;

    // ==================== API路径 ====================

    /** 认证模块API根路径 */
    public static final String AUTH_API_ROOT = "/api/v1/auth";

    /** 公开接口（无需JWT认证即可访问） */
    public static final String[] PUBLIC_PATHS = {
        "/api/v1/auth/**",
        "/api/v1/forum/**",
        "/api/v1/models/articles/**",
        "/api/v1/prediction/recommend",
        "/uploads/forum/**",
        "/uploads/runs/**"
    };

    /** 公开只读接口（仅 GET 请求无需 JWT 认证） */
    public static final String[] PUBLIC_GET_PATHS = {
        "/api/v1/models",
        "/api/v1/models/official",
        "/api/v1/prediction/models"
    };

    // ==================== 用户角色 ====================

    /** 普通用户（平台基础用户，可查看和操作自己的实验） */
    public static final String ROLE_USER = "USER";

    /** 研究人员（深度学习平台核心角色，可创建训练任务和管理数据集） */
    public static final String ROLE_RESEARCHER = "RESEARCHER";

    /** 管理员（拥有平台全部权限，可管理用户和系统配置） */
    public static final String ROLE_ADMIN = "ADMIN";

    /** Spring Security角色前缀（GrantedAuthority格式要求） */
    public static final String ROLE_PREFIX = "ROLE_";

    /** 新注册用户的默认角色 */
    public static final String DEFAULT_ROLE = ROLE_USER;

    // ==================== 响应状态码 ====================

    /** 请求成功 */
    public static final int CODE_SUCCESS = 200;

    /** 请求参数错误 */
    public static final int CODE_BAD_REQUEST = 400;

    /** 未认证（未登录或Token无效） */
    public static final int CODE_UNAUTHORIZED = 401;

    /** 无权限（角色不匹配） */
    public static final int CODE_FORBIDDEN = 403;

    /** 资源不存在 */
    public static final int CODE_NOT_FOUND = 404;

    /** 服务器内部错误 */
    public static final int CODE_INTERNAL_ERROR = 500;

    // ==================== 业务常量 ====================

    /** BCrypt密码编码器加密强度（数值越高越安全但越慢，10是推荐值） */
    public static final int BCRYPT_STRENGTH = 10;

    /** 密码最小长度 */
    public static final int PASSWORD_MIN_LENGTH = 6;

    /** 密码最大长度 */
    public static final int PASSWORD_MAX_LENGTH = 100;

    /** 用户名最小长度 */
    public static final int USERNAME_MIN_LENGTH = 3;

    /** 用户名最大长度 */
    public static final int USERNAME_MAX_LENGTH = 50;
}
