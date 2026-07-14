package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyang.d_game.dto.UpdateProfileReq;
import com.xiaoyang.d_game.dto.UserResp;
import com.xiaoyang.d_game.entity.User;

import java.util.List;

/**
 * 用户业务接口。
 *
 * <p>继承基础 CRUD，并提供用户查询、角色查询、当前用户资料、资料更新和账号状态校验能力。</p>
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户；不存在时返回 null。
     */
    User getByUsername(String username);

    /**
     * 根据 ID 查询用户；不存在时抛出业务异常。
     */
    User getRequiredUser(Long userId);

    /**
     * 查询用户拥有的角色编码列表。
     */
    List<String> listRoleCodes(Long userId);

    /**
     * 将用户实体转换为对外响应 DTO，避免泄露密码哈希等敏感字段。
     */
    UserResp toUserResp(User user);

    /**
     * 获取当前登录用户资料。
     */
    UserResp getCurrentUserProfile();

    /**
     * 更新当前登录用户资料。
     */
    UserResp updateProfile(UpdateProfileReq req);

    /**
     * 校验用户是否可用。
     *
     * <p>用户不存在、被封禁或其他不可用状态时抛出业务异常。</p>
     */
    void checkUserAvailable(User user);
}
