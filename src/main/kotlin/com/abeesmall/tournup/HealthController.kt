package com.abeesmall.tournup

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

data class HealthResponse(
    val status: String,
    val message: String,
)

@RestController
class HealthController {
    @GetMapping("/api/health/")
    fun health(): HealthResponse =
        HealthResponse(
            status = "healthy",
            message = "TournUp API is running",
        )
}
