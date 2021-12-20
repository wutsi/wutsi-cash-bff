package com.wutsi.application.cash.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
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
import com.wutsi.platform.account.event.EventURN
import com.wutsi.platform.core.stream.Event
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache

internal class WutsiAccountApiCacheAwareTest {
    private lateinit var delegate: WutsiAccountApi
    private lateinit var cache: Cache
    private lateinit var api: WutsiAccountApiCacheAware

    @BeforeEach
    fun setUp() {
        delegate = mock()
        cache = mock()
        api = WutsiAccountApiCacheAware(delegate, cache, ObjectMapper())
    }

    @Test
    fun onAccountUpdated() {
        api.onEvent(createEvent(EventURN.ACCOUNT_UPDATED.urn, 1L))
        verify(cache).evict("account_1")
    }

    @Test
    fun onAccountDeleted() {
        api.onEvent(createEvent(EventURN.ACCOUNT_DELETED.urn, 1L))
        verify(cache).evict("account_1")
    }

    @Test
    fun getAccountFromServer() {
        doReturn(null).whenever(cache).get("account_1", GetAccountResponse::class.java)

        val result = GetAccountResponse()
        doReturn(result).whenever(delegate).getAccount(1)

        val response = api.getAccount(1L)
        assertEquals(result, response)
        verify(cache).put("account_1", response)
    }

    @Test
    fun getAccountFromCache() {
        val result = GetAccountResponse()
        doReturn(result).whenever(cache).get("account_1", GetAccountResponse::class.java)

        val response = api.getAccount(1L)

        assertEquals(result, response)
        verify(cache, never()).put("account_1", response)
        verify(delegate, never()).getAccount(1L)
    }

    @Test
    fun addPaymentMethod() {
        val request = AddPaymentMethodRequest()
        val result = AddPaymentMethodResponse()
        doReturn(result).whenever(delegate).addPaymentMethod(1, request)

        val response = api.addPaymentMethod(1, request)
        assertEquals(result, response)
    }

    @Test
    fun updatePaymentMethod() {
        val request = UpdatePaymentMethodRequest()

        api.updatePaymentMethod(1, "xxx", request)

        verify(delegate).updatePaymentMethod(1, "xxx", request)
    }

    @Test
    fun deletePaymentMethod() {
        api.deletePaymentMethod(1, "xxx")

        verify(delegate).deletePaymentMethod(1, "xxx")
    }

    @Test
    fun getPaymentMethod() {
        val result = GetPaymentMethodResponse()
        doReturn(result).whenever(delegate).getPaymentMethod(1, "xxx")

        val response = api.getPaymentMethod(1, "xxx")
        assertEquals(result, response)
    }

    @Test
    fun listPaymentMethods() {
        val result = ListPaymentMethodResponse()
        doReturn(result).whenever(delegate).listPaymentMethods(1)

        val response = api.listPaymentMethods(1)
        assertEquals(result, response)
    }

    @Test
    fun checkPassword() {
        api.checkPassword(1, "xxx")

        verify(delegate).checkPassword(1, "xxx")
        verify(cache, never()).evict("account_1")
    }

    @Test
    fun savePassword() {
        val request = SavePasswordRequest()
        api.savePassword(1, request)

        verify(delegate).savePassword(1, request)
    }

    @Test
    fun createAccount() {
        val request = CreateAccountRequest()
        val result = CreateAccountResponse()
        doReturn(result).whenever(delegate).createAccount(request)

        val response = api.createAccount(request)

        assertEquals(result, response)
        verify(cache, never()).evict("account_1")
    }

    @Test
    fun deleteAccount() {
        api.deleteAccount(1)

        verify(delegate).deleteAccount(1)
        verify(cache).evict("account_1")
    }

    @Test
    fun searchAccount() {
        val request = SearchAccountRequest()
        val result = SearchAccountResponse()
        doReturn(result).whenever(delegate).searchAccount(request)

        val response = api.searchAccount(request)
        assertEquals(result, response)
    }

    @Test
    fun updateAccount() {
        val request = UpdateAccountRequest()
        val response = UpdateAccountResponse()
        doReturn(response).whenever(delegate).updateAccount(1, request)

        api.updateAccount(1, request)
    }

    @Test
    fun updateAccountAttribute() {
        val request = UpdateAccountAttributeRequest()
        api.updateAccountAttribute(1, "xxx", request)

        verify(delegate).updateAccountAttribute(1, "xxx", request)
    }

    private fun createEvent(type: String, accountId: Long = 1): Event =
        Event(
            type = type,
            payload = """
            {
                "accountId": $accountId
            }
            """.trimIndent()
        )
}
