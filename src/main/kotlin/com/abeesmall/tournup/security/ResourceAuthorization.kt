package com.abeesmall.tournup.security

import com.abeesmall.tournup.ui.resource.ResourceAction
import com.abeesmall.tournup.ui.resource.UiResourceRegistry
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.method.HandlerMethod
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ResourceAccess(val action: ResourceAction, val resource: String = "")

/** Runs after route selection and before argument conversion, including JSON binding. */
@Configuration(proxyBeanMethods = false)
class ResourceAuthorization(private val registry: ObjectProvider<UiResourceRegistry>) : HandlerInterceptor, WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) { registry.addInterceptor(this) }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val access = (handler as? HandlerMethod)?.getMethodAnnotation(ResourceAccess::class.java) ?: return true
        val variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *>
        val name = access.resource.ifEmpty { variables?.get("resource") as? String ?: "" }
        val resource = registry.getObject().resources.values.firstOrNull { it.pluralName == name }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if (access.action !in resource.actions) throw ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED)
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val allowed = if (access.action.write) resource.canWrite(authentication) else resource.canRead(authentication)
        if (!allowed) throw ResponseStatusException(HttpStatus.FORBIDDEN)
        return true
    }
}
