package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层接口
 * <p>
 * 继承JpaRepository，自动获得CRUD基础操作。
 * 通过Spring Data JPA的方法命名约定自动生成查询SQL。
 *
 * <p>命名约定说明：
 * <ul>
 *   <li>{@code findByXxx} — 根据字段精确查询</li>
 *   <li>{@code existsByXxx} — 判断对应字段的记录是否存在</li>
 * </ul>
 *
 * @author DeepInsight Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     * <p>
     * 用于Spring Security的UserDetailsService加载用户，
     * 以及登录时获取用户信息。
     *
     * @param username 用户名
     * @return 包含用户的Optional，不存在时为空
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查用户名是否已被注册
     *
     * @param username 用户名
     * @return true表示用户名已存在
     */
    Boolean existsByUsername(String username);

    /**
     * 根据邮箱查找用户
     * <p>
     * 用于密码找回流程、邮箱唯一性校验。
     *
     * @param email 邮箱地址
     * @return 包含用户的Optional
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查邮箱是否已被注册
     *
     * @param email 邮箱地址
     * @return true表示邮箱已存在
     */
    Boolean existsByEmail(String email);

    /**
     * 根据角色查找用户列表
     * <p>
     * 用于管理员查看和管理特定角色的用户。
     *
     * @param role 用户角色
     * @return 该角色的用户列表
     */
    List<User> findByRole(User.Role role);
}
