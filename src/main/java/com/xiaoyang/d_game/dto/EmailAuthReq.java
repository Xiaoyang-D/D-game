package com.xiaoyang.d_game.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
/** 邮箱认证请求类型；各入口分别应用校验。 */
public final class EmailAuthReq {
    private EmailAuthReq() { }
    public enum Purpose { REGISTER, RESET_PASSWORD, MIGRATION }
    @Data
    public static class SendCode {
        @NotBlank @Email @Size(max = 128) private String email;
        public void setEmail(String email) {
            this.email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        }
        @NotNull private Purpose purpose;
    }
    @Data
    public static class ResetPassword {
        @NotBlank @Email @Size(max = 128) private String email;
        public void setEmail(String email) {
            this.email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        }
        @NotBlank @Pattern(regexp = "[0-9]{6}") @ToString.Exclude private String code;
        @NotBlank @Size(min = 6, max = 64) @ToString.Exclude private String newPassword;
    }
    @Data
    public static class VerifyMigration {
        @NotBlank @Size(max = 64) private String username;
        @NotBlank @Size(max = 64) @ToString.Exclude private String password;
    }
    @Data
    public static class MigrationEmail {
        @NotBlank @Size(max = 64) @ToString.Exclude private String migrationToken;
        @NotBlank @Email @Size(max = 128) private String email;
        public void setEmail(String email) {
            this.email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }
    @Data
    public static class BindMigration extends MigrationEmail {
        @NotBlank @Pattern(regexp = "[0-9]{6}") @ToString.Exclude private String code;
    }
    public record MigrationToken(String migrationToken, Integer expiresIn) { }
}
