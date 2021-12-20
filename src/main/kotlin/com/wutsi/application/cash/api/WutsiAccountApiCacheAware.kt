package com.wutsi.application.cash.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.AddPaymentMethodRequest
import com.wutsi.platform.account.dto.AddPaymentMethodResponse
import com.wutsi.platform.account.dto.CreateAccountRequest
import com.wutsi.platform.account.dto.CreateAccountResponse
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.account.dto.GetPaymentMethodResponse
import com.wutsi.platform.account.dto.ListPaymentMethodResponse
import com.wutsi.platform.account.dto.SavePasswordRequest
import com.wutsi.platform.account.dto.SearchAccountRequest
import com.wutsi.platform.account.dto.SearchAccountResponse
import com.wutsi.platform.account.dto.UpdateAccountAttributeRequest
import com.wutsi.platform.account.dto.UpdateAccountRequest
import com.wutsi.platform.account.dto.UpdateAccountResponse
import com.wutsi.platform.account.dto.UpdatePaymentMethodRequest
import com.wutsi.platform.account.event.AccountDeletedPayload
import com.wutsi.platform.account.event.AccountUpdatedPayload
import com.wutsi.platform.account.event.EventURN
import com.wutsi.platform.core.stream.Event
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.context.event.EventListener

class WutsiAccountApiCacheAware(
    private val delegate: WutsiAccountApi,
    private val cache: Cache,
    private val mapper: ObjectMapper,
) : WutsiAccountApi {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(WutsiAccountApiCacheAware::class.java)
    }

    // EventListener override
    @EventListener
    fun onEvent(event: Event) {
        LOGGER.info("onEvent(${event.type}, ...)")

        if (EventURN.ACCOUNT_DELETED.urn == event.type) {
            val payload = mapper.readValue(event.payload, AccountDeletedPayload::class.java)
            LOGGER.info("Evicting Account#${payload.accountId}")
            evictAccount(payload.accountId)
        } else if (EventURN.ACCOUNT_UPDATED.urn == event.type) {
            val payload = mapper.readValue(event.payload, AccountUpdatedPayload::class.java)
            LOGGER.info("Evicting Account#${payload.accountId}")
            evictAccount(payload.accountId)
        }
    }

    // WutsiAccountAPI override
    override fun addPaymentMethod(id: Long, request: AddPaymentMethodRequest): AddPaymentMethodResponse =
        delegate.addPaymentMethod(id, request)

    override fun checkPassword(id: Long, password: String) =
        delegate.checkPassword(id, password)

    override fun createAccount(request: CreateAccountRequest): CreateAccountResponse =
        delegate.createAccount(request)

    override fun deleteAccount(id: Long) {
        delegate.deleteAccount(id)
        evictAccount(id)
    }

    override fun deletePaymentMethod(id: Long, token: String) =
        delegate.deletePaymentMethod(id, token)

    override fun getAccount(id: Long): GetAccountResponse {
        val key = getAccountCacheKey(id)
        val cached = getFromCache(key)
        if (cached != null)
            return cached

        return addAccountToCache(key, delegate.getAccount(id))
    }

    override fun getPaymentMethod(id: Long, token: String): GetPaymentMethodResponse =
        delegate.getPaymentMethod(id, token)

    override fun listPaymentMethods(id: Long): ListPaymentMethodResponse =
        delegate.listPaymentMethods(id)

    override fun savePassword(id: Long, request: SavePasswordRequest) =
        delegate.savePassword(id, request)

    override fun searchAccount(request: SearchAccountRequest): SearchAccountResponse =
        delegate.searchAccount(request)

    override fun updateAccount(id: Long, request: UpdateAccountRequest): UpdateAccountResponse {
        evictAccount(id)
        return delegate.updateAccount(id, request)
    }

    override fun updateAccountAttribute(id: Long, name: String, request: UpdateAccountAttributeRequest) {
        evictAccount(id)
        return delegate.updateAccountAttribute(id, name, request)
    }

    override fun updatePaymentMethod(id: Long, token: String, request: UpdatePaymentMethodRequest) =
        delegate.updatePaymentMethod(id, token, request)

    private fun getFromCache(key: String): GetAccountResponse? =
        try {
            cache.get(key, GetAccountResponse::class.java)
        } catch (ex: Exception) {
            LOGGER.warn("Unable to resolve from cache $key", ex)
            null
        }

    private fun addAccountToCache(key: String, value: GetAccountResponse): GetAccountResponse {
        try {
            cache.put(key, value)
        } catch (ex: Exception) {
            LOGGER.warn("Unable to store into the cache: $key", ex)
        }
        return value
    }

    private fun evictAccount(accountId: Long) {
        val key = getAccountCacheKey(accountId)
        try {
            cache.evict(key)
        } catch (ex: Exception) {
            LOGGER.warn("Unable to evict from cache $key", ex)
        }
    }

    private fun getAccountCacheKey(id: Long): String =
        "account_$id"
}
