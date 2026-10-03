package com.abeesmall.tournup

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration

@SpringBootApplication(
	exclude = [UserDetailsServiceAutoConfiguration::class],
)
class TournUpSpringBootApplication

fun main(args: Array<String>) {
	runApplication<TournUpSpringBootApplication>(*args)
}
