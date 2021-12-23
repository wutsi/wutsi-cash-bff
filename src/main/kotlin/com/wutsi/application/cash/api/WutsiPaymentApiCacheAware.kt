package com.wutsi.application.cash.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.service.SecurityManager
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.CreateCashinRequest
import com.wutsi.platform.payment.dto.CreateCashinResponse
import com.wutsi.platform.payment.dto.CreateCashoutRequest
import com.wutsi.platform.payment.dto.CreateCashoutResponse
import com.wutsi.platform.payment.dto.CreateTransferRequest
import com.wutsi.platform.payment.dto.CreateTransferResponse
import com.wutsi.platform.payment.dto.GetBalanceResponse
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.SearchTransactionRequest
import com.wutsi.platform.payment.dto.SearchTransactionResponse
import com.wutsi.platform.payment.event.EventURN
import com.wutsi.platform.payment.event.TransactionEventPayload
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.context.event.EventListener

class WutsiPaymentApiCacheAware(
    private val delegate: WutsiPaymentApi,
    private val securityManager: SecurityManager,
    private val cache: Cache,
    private val mapper: ObjectMapper
) : WutsiPaymentApi {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(WutsiPaymentApiCacheAware::class.java)
        private val PAYMENT_EVENTS = listOf(
            EventURN.TRANSACTION_PENDING.urn,
            EventURN.TRANSACTION_FAILED.urn,
            EventURN.TRANSACTION_SUCCESSFULL.urn
        )
    }

    // EventListener override
    @EventListener
    fun onEvent(event: Event) {
        if (isPaymentEvent(event)) {
            val payload = mapper.readValue(event.payload, TransactionEventPayload::class.java)
            LOGGER.info("Evicting Balance#${payload.accountId}")
            evictBalance(payload.accountId)
        }
    }

    private fun isPaymentEvent(event: Event): Boolean =
        PAYMENT_EVENTS.contains(event.type)

    // WutsiPaymentApi overrides
    override fun createCashin(request: CreateCashinRequest): CreateCashinResponse {
        try {
            return delegate.createCashin(request)
        } finally {
            evictBalance()
        }
    }

    override fun createCashout(request: CreateCashoutRequest): CreateCashoutResponse {
        try {
            return delegate.createCashout(request)
        } finally {
            evictBalance()
        }
    }

    override fun createTransfer(request: CreateTransferRequest): CreateTransferResponse {
        try {
            return delegate.createTransfer(request)
        } finally {
            evictBalance()
        }
    }

    override fun getBalance(accountId: Long): GetBalanceResponse {
        // Fetch the balance from cache
        val key = getBalanceCacheKey()
        val cached = getBalanceFromCache(key)
        if (cached != null)
            return cached

        // Fetch the balance from backend
        return addToCache(key, delegate.getBalance(accountId))
    }

    private fun getBalanceFromCache(key: String): GetBalanceResponse? =
        try {
            cache.get(key, GetBalanceResponse::class.java)
        } catch (ex: Exception) {
            LOGGER.warn("Unable to resolve from cache $key", ex)
            null
        }

    private fun addToCache(key: String, value: GetBalanceResponse): GetBalanceResponse {
        try {
            cache.put(key, value)
        } catch (ex: Exception) {
            LOGGER.warn("Unable to store into the cache: $key", ex)
        }

        return value
    }

    override fun getTransaction(id: String): GetTransactionResponse =
        delegate.getTransaction(id)

    override fun searchTransaction(request: SearchTransactionRequest): SearchTransactionResponse =
        delegate.searchTransaction(request)

    private fun evictBalance() =
        evictBalance(securityManager.currentUserId())

    private fun evictBalance(accountId: Long) {
        val key = getBalanceCacheKey(accountId)
        try {
            cache.evict(key)
        } catch (ex: Exception) {
            LOGGER.warn("Unable to evict from cache $key", ex)
        }
    }

    private fun getBalanceCacheKey(): String =
        getBalanceCacheKey(securityManager.currentUserId())

    private fun getBalanceCacheKey(accountId: Long): String =
        "balance_$accountId"
}
