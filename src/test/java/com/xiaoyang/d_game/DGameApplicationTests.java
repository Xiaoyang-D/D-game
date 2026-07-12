package com.xiaoyang.d_game;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=test-secret-that-is-long-enough-for-hs256")
class DGameApplicationTests {

	@Test
	void contextLoads() {
	}

}
