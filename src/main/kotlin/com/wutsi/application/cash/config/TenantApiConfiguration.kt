package com.wutsi.application.cash.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.api.WutsiTenantApiCacheAware
import com.wutsi.platform.core.security.feign.FeignApiKeyRequestInterceptor
import com.wutsi.platform.core.security.feign.FeignAuthorizationRequestInterceptor
import com.wutsi.platform.core.tracing.feign.FeignTracingRequestInterceptor
import com.wutsi.platform.tenant.Environment.PRODUCTION
import com.wutsi.platform.tenant.Environment.SANDBOX
import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.WutsiTenantApiBuilder
import org.springframework.cache.Cache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles

@Configuration
class TenantApiConfiguration(
    private val authorizationRequestInterceptor: FeignAuthorizationRequestInterceptor,
    private val tracingRequestInterceptor: FeignTracingRequestInterceptor,
    private val apiKeyRequestInterceptor: FeignApiKeyRequestInterceptor,
    private val mapper: ObjectMapper,
    private val env: Environment,
    private val cache: Cache
) {
    @Bean
    fun tenantApi(): WutsiTenantApi =
        WutsiTenantApiCacheAware(
            delegate = WutsiTenantApiBuilder().build(
                env = environment(),
                mapper = mapper,
                interceptors = listOf(
                    apiKeyRequestInterceptor,
                    tracingRequestInterceptor,
                    authorizationRequestInterceptor
                )
            ),
            cache = cache,
        )

    private fun environment(): com.wutsi.platform.tenant.Environment =
        if (env.acceptsProfiles(Profiles.of("prod")))
            PRODUCTION
        else
            SANDBOX
}
