package com.wutsi.application.cash.api

import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.dto.GetTenantResponse
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache

class WutsiTenantApiCacheAware(
    private val delegate: WutsiTenantApi,
    private val cache: Cache
) : WutsiTenantApi {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(WutsiTenantApiCacheAware::class.java)
    }

    override fun getTenant(id: Long): GetTenantResponse {
        val key = "tenant_$id"
        val cached = getFromCache(key)
        if (cached != null)
            return cached

        return addToCache(key, delegate.getTenant(id))
    }

    private fun getFromCache(key: String): GetTenantResponse? =
        try {
            cache.get(key, GetTenantResponse::class.java)
        } catch (ex: Exception) {
            LOGGER.warn("Unable to resolve from cache $key", ex)
            null
        }

    private fun addToCache(key: String, value: GetTenantResponse): GetTenantResponse {
        try {
            cache.put(key, value)
        } catch (ex: Exception) {
            LOGGER.warn("Unable to store into the cache: $key", ex)
        }
        return value
    }
}
