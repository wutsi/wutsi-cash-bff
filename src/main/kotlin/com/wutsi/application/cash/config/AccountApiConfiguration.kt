package com.wutsi.application.cash.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.api.WutsiAccountApiCacheAware
import com.wutsi.platform.account.Environment.PRODUCTION
import com.wutsi.platform.account.Environment.SANDBOX
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.WutsiAccountApiBuilder
import com.wutsi.platform.core.security.feign.FeignApiKeyRequestInterceptor
import com.wutsi.platform.core.security.feign.FeignAuthorizationRequestInterceptor
import com.wutsi.platform.core.tracing.feign.FeignTracingRequestInterceptor
import org.springframework.cache.Cache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles

@Configuration
class AccountApiConfiguration(
    private val authorizationRequestInterceptor: FeignAuthorizationRequestInterceptor,
    private val tracingRequestInterceptor: FeignTracingRequestInterceptor,
    private val apiKeyRequestInterceptor: FeignApiKeyRequestInterceptor,
    private val mapper: ObjectMapper,
    private val env: Environment,
    private val cache: Cache
) {
    @Bean
    fun accountApi(): WutsiAccountApi =
        WutsiAccountApiCacheAware(
            cache = cache,
            mapper = mapper,
            delegate = WutsiAccountApiBuilder().build(
                env = environment(),
                mapper = mapper,
                interceptors = listOf(
                    apiKeyRequestInterceptor,
                    tracingRequestInterceptor,
                    authorizationRequestInterceptor
                )
            )
        )

    private fun environment(): com.wutsi.platform.account.Environment =
        if (env.acceptsProfiles(Profiles.of("prod")))
            PRODUCTION
        else
            SANDBOX
}
