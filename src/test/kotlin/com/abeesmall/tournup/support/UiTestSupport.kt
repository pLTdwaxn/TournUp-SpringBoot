package com.abeesmall.tournup.support

import com.abeesmall.tournup.ui.ResourceResponseComposer
import com.abeesmall.tournup.ui.UiConstraintProjector
import com.abeesmall.tournup.ui.UiManifestResolver
import com.abeesmall.tournup.validation.WriteContractMetadata
import jakarta.validation.Validation
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import

/** Pure tests own and close their validator; production uses Spring's managed validator. */
class UiTestSupport : AfterEachCallback {
    private val factory = Validation.buildDefaultValidatorFactory()
    val validator = factory.validator
    val metadata = WriteContractMetadata(validator)
    val projector = UiConstraintProjector(metadata)
    val manifests = UiManifestResolver(projector)
    override fun afterEach(context: ExtensionContext) = factory.close()
}

@TestConfiguration(proxyBeanMethods = false)
@Import(ResourceResponseComposer::class, UiManifestResolver::class, UiConstraintProjector::class, WriteContractMetadata::class)
class ResourceWebTestConfiguration
