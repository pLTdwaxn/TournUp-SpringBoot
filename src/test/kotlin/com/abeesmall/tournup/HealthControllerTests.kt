package com.abeesmall.tournup

import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest
class HealthControllerTests(
    @Autowired private val mockMvc: MockMvc,
) {

    @Test
    fun `health endpoint returns expected response`() {
        mockMvc.get("/api/health/")
            .andExpect {
                status { isOk() }
                content { contentType("application/json") }
                jsonPath("$.status", equalTo("healthy"))
                jsonPath("$.message", equalTo("TournUp API is running"))
            }
    }
}
