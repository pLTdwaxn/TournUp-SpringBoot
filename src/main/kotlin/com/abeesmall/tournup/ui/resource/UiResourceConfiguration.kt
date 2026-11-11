package com.abeesmall.tournup.ui.resource

import com.abeesmall.tournup.manifest.season.seasonResourceDefinition
import com.abeesmall.tournup.manifest.tournament.tournamentResourceDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class UiResourceConfiguration {
    @Bean
    fun seasonUiResource(): UiResourceRegistration = seasonResourceDefinition()

    @Bean
    fun tournamentUiResource(): UiResourceRegistration = tournamentResourceDefinition()
}
