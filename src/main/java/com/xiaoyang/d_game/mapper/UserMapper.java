package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
/**
 * 用户 Mapper。
 */
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT COUNT(*) FROM `user` WHERE email = #{email}")
    long countEmailIncludingDeleted(@Param("email") String email);

    @Update("UPDATE `user` SET password_hash=#{hash}, auth_version=auth_version+1, gmt_modified=NOW() WHERE id=#{id} AND auth_version=#{version} AND email_verified_at IS NOT NULL AND status=1 AND is_deleted=0")
    int resetPassword(@Param("id") Long id,
                      @Param("version") Integer version,
                      @Param("hash") String hash);

    @Update("UPDATE `user` SET email=#{email}, email_verified_at=NOW(), auth_version=auth_version+1, gmt_modified=NOW() WHERE id=#{id} AND auth_version=#{version} AND email_verified_at IS NULL AND status=1 AND is_deleted=0")
    int bindEmail(@Param("id") Long id,
                  @Param("version") Integer version,
                  @Param("email") String email);
}

