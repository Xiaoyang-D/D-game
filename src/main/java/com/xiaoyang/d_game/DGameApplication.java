package com.xiaoyang.d_game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
/**
 * 后端应用启动入口。
 *
 * <p>{@link SpringBootApplication} 会启用自动配置、组件扫描和配置属性绑定。
 * 默认扫描当前包 {@code com.xiaoyang.d_game} 及其子包，所以新增 Controller、Service、Mapper
 * 时应放在该包路径下，避免 Bean 无法被 Spring 管理。</p>
 */
public class DGameApplication {

	/**
	 * 本地开发、容器启动和测试环境都会从这里启动 Spring Boot 应用。
	 *
	 * @param args 命令行参数，通常由 Spring Boot 解析 profile、端口等启动配置
	 */
	public static void main(String[] args) {
		SpringApplication.run(DGameApplication.class, args);
	}

}
