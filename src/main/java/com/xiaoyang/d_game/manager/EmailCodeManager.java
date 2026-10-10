package com.xiaoyang.d_game.manager;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.config.EmailAuthProperties;
import com.xiaoyang.d_game.dto.EmailAuthReq.Purpose;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
/** 验证码与限流适配；Redis 不可用时失败关闭。 */
@lombok.extern.slf4j.Slf4j
@Component @RequiredArgsConstructor
public class EmailCodeManager {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> RESERVE = script("reserve");
    private static final DefaultRedisScript<Long> PUBLISH = script("publish");
    private static final DefaultRedisScript<Long> RELEASE = script("release");
    private static final DefaultRedisScript<Long> CONSUME = script("consume");
    private final StringRedisTemplate redis;
    private final AuthMailManager mail;
    private final EmailAuthProperties properties;
    @Value("${jwt.secret}") private String secret;
    public static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static DefaultRedisScript<Long> script(String name) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("auth/" + name + ".lua"));
        script.setResultType(Long.class); return script;
    }
    private String key(String email) { return DigestUtils.md5DigestAsHex(email.getBytes(StandardCharsets.UTF_8)); }
    private String codeKey(String email, Purpose purpose, String scope) {
        return "auth:code:" + purpose + ":" + key(email) + ":" + scope;
    }
    private String digest(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("无法计算验证码摘要", exception);
        }
    }
    public void send(String email, Purpose purpose, String scope, String ip, boolean eligible) {
        String codeKey = codeKey(email, purpose, scope);
        String reservation = UUID.randomUUID().toString();
        List<String> keys = List.of("auth:cooldown:" + key(email), "auth:email-limit:" + key(email),
                "auth:ip-limit:" + key(ip), codeKey);
        Long result = redis.execute(RESERVE, keys, reservation, String.valueOf(properties.getResendSeconds()),
                String.valueOf(properties.getEmailHourlyLimit()), String.valueOf(properties.getIpHourlyLimit()));
        if (!Long.valueOf(1).equals(result)) {
            throw new BizException(ResultCode.BAD_REQUEST, "发送过于频繁，请稍后再试");
        }
        if (!eligible) { return; }
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        try {
            mail.send(email, code, properties.getCodeTtlSeconds());
            Long published = redis.execute(PUBLISH, List.of(keys.get(0), codeKey), reservation,
                    digest(codeKey + ":" + code), String.valueOf(properties.getCodeTtlSeconds()));
            if (!Long.valueOf(1).equals(published)) { throw new IllegalStateException("发信占用已过期"); }
        } catch (RuntimeException exception) {
            log.warn("认证邮件发送失败，异常类型={}", exception.getClass().getSimpleName());
            redis.execute(RELEASE, keys, reservation);
            throw new BizException(ResultCode.INTERNAL_ERROR, "邮件发送失败，请稍后重试");
        }
    }
    public void consume(String email, Purpose purpose, String scope, String code) {
        String key = codeKey(email, purpose, scope);
        Long result = redis.execute(CONSUME, List.of(key), digest(key + ":" + code),
                String.valueOf(properties.getMaxAttempts()));
        if (!Long.valueOf(1).equals(result)) {
            throw new BizException(ResultCode.BAD_REQUEST, "验证码无效或已过期");
        }
    }
}
