package com.abeesmall.tournup

import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
	properties = ["tournup.auth.provider=stub"],
)
@Import(TestcontainersConfiguration::class)
class TournUpSpringBootApplicationTests {

	@Test
	fun contextLoads() {
	}

}
