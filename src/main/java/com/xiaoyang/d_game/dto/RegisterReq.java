package com.xiaoyang.d_game.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
/** 验证邮箱后注册。 */
@Data
public class RegisterReq {
    @NotBlank @Email @Size(max = 128)
    private String email;
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
    @NotBlank @Pattern(regexp = "[0-9]{6}") @ToString.Exclude
    private String code;
    @NotBlank @Size(min = 6, max = 64) @ToString.Exclude
    private String password;
    @NotBlank @Size(max = 64)
    private String nickname;
}
