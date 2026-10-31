package com.abeesmall.tournup

import com.abeesmall.tournup.catalog.TenantCatalog
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(
    properties = ["tournup.auth.provider=stub"],
)
@Import(TestcontainersConfiguration::class)
@MockitoBean(types = [TenantCatalog::class])
class TournUpSpringBootApplicationTests {

    @Test
    fun contextLoads() {
    }
}
