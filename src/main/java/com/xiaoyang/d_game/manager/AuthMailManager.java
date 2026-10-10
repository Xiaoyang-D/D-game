package com.xiaoyang.d_game.manager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
/** SMTP 适配层，测试替换此组件；生产绝不输出验证码。 */
@Component @RequiredArgsConstructor
public class AuthMailManager {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}") private String from;
    public void send(String email, String code, int ttlSeconds) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email);
        message.setSubject("D-Game 邮箱验证码");
        message.setText("你的验证码为：" + code + "，有效期 " + ttlSeconds / 60 + " 分钟。请勿分享验证码。如非本人操作，请忽略。 ");
        mailSender.send(message);
    }
}
