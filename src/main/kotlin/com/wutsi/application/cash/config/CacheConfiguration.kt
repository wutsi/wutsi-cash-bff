package com.wutsi.application.cash.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CacheConfiguration(
    private val cacheManager: CacheManager,

    @Value("\${wutsi.platform.cache.name}") private val cacheName: String
) {
    @Bean
    fun getCache(): Cache =
        cacheManager.getCache(cacheName)
}
