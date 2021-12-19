package com.wutsi.application.cash.api

import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.dto.GetTenantResponse
import org.springframework.cache.Cache

class WutsiTenantApiCacheAware(
    private val delegate: WutsiTenantApi,
    private val cache: Cache
) : WutsiTenantApi {
    override fun getTenant(id: Long): GetTenantResponse {
        val cacheKey = "tenant_$id"
        val cached = cache.get(cacheKey, GetTenantResponse::class.java)
        if (cached != null)
            return cached

        val response = delegate.getTenant(id)
        cache.put(cacheKey, response)
        return response
    }
}
