package com.wutsi.application.cash.api

import com.wutsi.application.cash.service.SecurityManager
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.CreateCashinRequest
import com.wutsi.platform.payment.dto.CreateCashinResponse
import com.wutsi.platform.payment.dto.CreateCashoutRequest
import com.wutsi.platform.payment.dto.CreateCashoutResponse
import com.wutsi.platform.payment.dto.CreateTransferRequest
import com.wutsi.platform.payment.dto.CreateTransferResponse
import com.wutsi.platform.payment.dto.GetBalanceResponse
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.RunJobResponse
import com.wutsi.platform.payment.dto.SearchTransactionRequest
import com.wutsi.platform.payment.dto.SearchTransactionResponse
import org.springframework.cache.Cache

class WutsiPaymentApiCacheAware(
    private val delegate: WutsiPaymentApi,
    private val securityManager: SecurityManager,
    private val cache: Cache,
) : WutsiPaymentApi {
    override fun createCashin(request: CreateCashinRequest): CreateCashinResponse {
        try {
            return delegate.createCashin(request)
        } finally {
            cache.evict(getBalanceCacheKey())
        }
    }

    override fun createCashout(request: CreateCashoutRequest): CreateCashoutResponse {
        try {
            return delegate.createCashout(request)
        } finally {
            cache.evict(getBalanceCacheKey())
        }
    }

    override fun createTransfer(request: CreateTransferRequest): CreateTransferResponse {
        try {
            return delegate.createTransfer(request)
        } finally {
            cache.evict(getBalanceCacheKey())
        }
    }

    override fun getBalance(accountId: Long): GetBalanceResponse {
        // Fetch the balance from cache
        val cached = cache.get(getBalanceCacheKey(), GetBalanceResponse::class.java)
        if (cached != null)
            return cached

        // Fetch the balance from backend
        val response = delegate.getBalance(accountId)
        cache.put(getBalanceCacheKey(), response)
        return response
    }

    override fun getTransaction(id: String): GetTransactionResponse =
        delegate.getTransaction(id)

    override fun runJob(name: String): RunJobResponse =
        delegate.runJob(name)

    override fun searchTransaction(request: SearchTransactionRequest): SearchTransactionResponse =
        delegate.searchTransaction(request)

    private fun getBalanceCacheKey(): String =
        "balance_" + securityManager.currentUserId()
}
