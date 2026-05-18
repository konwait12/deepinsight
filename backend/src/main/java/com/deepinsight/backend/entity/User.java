package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 用户实体类
 * <p>
 * JPA实体，映射到数据库 {@code users} 表。
 * 同时实现Spring Security的 {@link UserDetails} 接口，
 * 作为认证流程中的用户主体对象。
 *
 * <p>字段映射关系：
 * <ul>
 *   <li>{@code password} → 数据库列 {@code password_hash}（存储 BCrypt 哈希值）</li>
 *   <li>{@code createdAt} → 数据库列 {@code created_at}（由JPA审计自动填充）</li>
 * </ul>
 *
 * <p>角色说明：
 * <ul>
 *   <li>{@code USER} — 普通用户，可查看和操作自己的实验</li>
 *   <li>{@code RESEARCHER} — 研究人员，可创建训练任务和管理数据集</li>
 *   <li>{@code ADMIN} — 管理员，拥有平台全部权限</li>
 * </ul>
 *
 * @author DeepInsight Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails {

    /** 用户唯一标识（自增主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名（唯一，不可为空） */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /**
     * 密码哈希值
     * <p>
     * 映射到数据库password_hash列，存储BCrypt编码后的密码。
     * BCrypt 输出长度固定 60 字符，并且盐值已嵌入哈希结果。
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    /** 用户邮箱（唯一，用于密码找回和通知） */
    @Column(unique = true, length = 100)
    private String email;

    /** 用户角色（USER / RESEARCHER / ADMIN），数据库存储为字符串 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    /** 账户创建时间（由JPA审计自动填充，不可更新） */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 用户角色枚举
     * <p>
     * 角色层级：USER < RESEARCHER < ADMIN
     */
    public enum Role {
        /** 普通用户 — 平台基础角色 */
        USER,
        /** 研究人员 — 深度学习平台核心角色，可创建和管理实验 */
        RESEARCHER,
        /** 管理员 — 拥有平台全部管理权限 */
        ADMIN
    }

    // ==================== UserDetails接口实现 ====================

    /**
     * 获取用户的权限集合
     * <p>
     * 将角色转换为Spring Security的GrantedAuthority格式。
     * 格式：ROLE_USER / ROLE_RESEARCHER / ROLE_ADMIN。
     *
     * @return 权限列表
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** @return 账户是否未过期（始终为true，平台不设置账户过期） */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** @return 账户是否未锁定（始终为true，管理员可手动锁定） */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** @return 凭证是否未过期（始终为true，JWT自身管理过期） */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** @return 账户是否启用（始终为true） */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
