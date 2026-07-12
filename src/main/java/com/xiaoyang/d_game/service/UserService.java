package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyang.d_game.dto.UpdateProfileReq;
import com.xiaoyang.d_game.dto.UserResp;
import com.xiaoyang.d_game.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    User getByUsername(String username);

    User getRequiredUser(Long userId);

    List<String> listRoleCodes(Long userId);

    UserResp toUserResp(User user);

    UserResp getCurrentUserProfile();

    UserResp updateProfile(UpdateProfileReq req);

    void checkUserAvailable(User user);
}
