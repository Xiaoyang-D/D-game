package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 用户 Mapper。
 */
public interface UserMapper extends BaseMapper<User> {
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM `user` WHERE email = #{email}")
    long countEmailIncludingDeleted(@org.apache.ibatis.annotations.Param("email") String email);

    @org.apache.ibatis.annotations.Update("UPDATE `user` SET password_hash=#{hash}, auth_version=auth_version+1, gmt_modified=NOW() WHERE id=#{id} AND auth_version=#{version} AND email_verified_at IS NOT NULL AND status=1 AND is_deleted=0")
    int resetPassword(@org.apache.ibatis.annotations.Param("id") Long id,
                      @org.apache.ibatis.annotations.Param("version") Integer version,
                      @org.apache.ibatis.annotations.Param("hash") String hash);

    @org.apache.ibatis.annotations.Update("UPDATE `user` SET email=#{email}, email_verified_at=NOW(), auth_version=auth_version+1, gmt_modified=NOW() WHERE id=#{id} AND auth_version=#{version} AND email_verified_at IS NULL AND status=1 AND is_deleted=0")
    int bindEmail(@org.apache.ibatis.annotations.Param("id") Long id,
                  @org.apache.ibatis.annotations.Param("version") Integer version,
                  @org.apache.ibatis.annotations.Param("email") String email);
}

