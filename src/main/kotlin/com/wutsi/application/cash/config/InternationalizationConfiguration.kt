package com.wutsi.application.cash.config

import com.wutsi.application.cash.service.RequestLocaleResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.web.servlet.LocaleResolver

@Configuration
class InternationalizationConfiguration {
    @Bean
    fun messageSource(): ResourceBundleMessageSource? {
        val messageSource = ResourceBundleMessageSource()
        messageSource.setBasename("messages")
        return messageSource
    }

    @Bean
    fun localeResolver(): LocaleResolver =
        RequestLocaleResolver()
}
