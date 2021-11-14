package com.wutsi.application.cash.service

import com.wutsi.platform.core.tracing.TracingContext
import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.dto.MobileCarrier
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.stereotype.Service

@Service
class TenantProvider(
    private val tenantApi: WutsiTenantApi,
    private val tracingContext: TracingContext,
) {
    fun get(): Tenant =
        tenantApi.getTenant(tenantId()).tenant

    private fun tenantId(): Long =
        tracingContext.tenantId()!!.toLong()

    fun logo(carrier: MobileCarrier): String? =
        carrier.logos.find { it.type == "PICTORIAL" }?.url
}
