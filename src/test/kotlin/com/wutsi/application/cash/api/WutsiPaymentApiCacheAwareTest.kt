package com.wutsi.application.cash.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.service.SecurityManager
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.CreateCashinRequest
import com.wutsi.platform.payment.dto.CreateCashinResponse
import com.wutsi.platform.payment.dto.CreateCashoutRequest
import com.wutsi.platform.payment.dto.CreateCashoutResponse
import com.wutsi.platform.payment.dto.CreatePaymentRequest
import com.wutsi.platform.payment.dto.CreatePaymentResponse
import com.wutsi.platform.payment.dto.CreateTransferRequest
import com.wutsi.platform.payment.dto.CreateTransferResponse
import com.wutsi.platform.payment.dto.GetBalanceResponse
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.RequestPaymentRequest
import com.wutsi.platform.payment.dto.RequestPaymentResponse
import com.wutsi.platform.payment.dto.SearchTransactionRequest
import com.wutsi.platform.payment.dto.SearchTransactionResponse
import com.wutsi.platform.payment.event.EventURN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache

internal class WutsiPaymentApiCacheAwareTest {
    companion object {
        const val USER_ID = 1L
    }

    private lateinit var delegate: WutsiPaymentApi
    private lateinit var cache: Cache
    private lateinit var securityManager: SecurityManager
    private lateinit var api: WutsiPaymentApiCacheAware

    @BeforeEach
    fun setUp() {
        delegate = mock()
        cache = mock()

        securityManager = mock()
        doReturn(USER_ID).whenever(securityManager).currentUserId()

        api = WutsiPaymentApiCacheAware(delegate, securityManager, cache, ObjectMapper())
    }

    @Test
    fun getBalanceFromCache() {
        // GIVEN
        val result = GetBalanceResponse()
        doReturn(result).whenever(cache).get("balance_$USER_ID", GetBalanceResponse::class.java)

        // WHEN
        val response = api.getBalance(USER_ID)

        // THEN
        assertEquals(result, response)
        verify(delegate, never()).getBalance(1L)
        verify(cache, never()).put("balance_$USER_ID", response)
    }

    @Test
    fun getBalanceFromServer() {
        // GIVEN
        doReturn(null).whenever(cache).get("balance_$USER_ID", GetBalanceResponse::class.java)

        val result = GetBalanceResponse()
        doReturn(result).whenever(delegate).getBalance(USER_ID)

        // WHEN
        val response = api.getBalance(USER_ID)

        // THEN
        assertEquals(result, response)
        verify(cache).put("balance_$USER_ID", response)
    }

    @Test
    fun getBalanceFromServerOnCacheReadError() {
        // GIVEN
        doReturn(null).whenever(cache).get("balance_$USER_ID", GetBalanceResponse::class.java)
        doThrow(RuntimeException::class).whenever(cache).get("balance_$USER_ID", GetBalanceResponse::class.java)

        val result = GetBalanceResponse()
        doReturn(result).whenever(delegate).getBalance(USER_ID)

        // WHEN
        val response = api.getBalance(USER_ID)

        // THEN
        assertEquals(result, response)
    }

    @Test
    fun getBalanceFromServerOnCacheWriteError() {
        // GIVEN
        doThrow(RuntimeException::class).whenever(cache).put("balance_$USER_ID", GetBalanceResponse::class.java)

        val result = GetBalanceResponse()
        doReturn(result).whenever(delegate).getBalance(USER_ID)

        // WHEN
        val response = api.getBalance(USER_ID)

        // THEN
        assertEquals(result, response)
    }

    @Test
    fun createCashin() {
        val result = CreateCashinResponse()
        doReturn(result).whenever(delegate).createCashin(any())

        val response = api.createCashin(CreateCashinRequest())
        assertEquals(result, response)

        verify(cache).evict("balance_$USER_ID")
    }

    @Test
    fun createCashout() {
        val result = CreateCashoutResponse()
        doReturn(result).whenever(delegate).createCashout(any())

        val response = api.createCashout(CreateCashoutRequest())
        assertEquals(result, response)

        verify(cache).evict("balance_$USER_ID")
    }

    @Test
    fun createTransfer() {
        val result = CreateTransferResponse()
        doReturn(result).whenever(delegate).createTransfer(any())

        val response = api.createTransfer(CreateTransferRequest())
        assertEquals(result, response)

        verify(cache).evict("balance_$USER_ID")
    }

    @Test
    fun getTransaction() {
        val result = GetTransactionResponse()
        doReturn(result).whenever(delegate).getTransaction(any())

        val response = api.getTransaction("32032039")
        assertEquals(result, response)
    }

    @Test
    fun searchTransaction() {
        val result = SearchTransactionResponse()
        doReturn(result).whenever(delegate).searchTransaction(any())

        val response = api.searchTransaction(SearchTransactionRequest())
        assertEquals(result, response)
    }

    @Test
    fun createPayment() {
        val result = CreatePaymentResponse()
        doReturn(result).whenever(delegate).createPayment(any())

        val response = api.createPayment(CreatePaymentRequest())
        assertEquals(result, response)
    }

    @Test
    fun requestPayment() {
        val result = RequestPaymentResponse()
        doReturn(result).whenever(delegate).requestPayment(any())

        val response = api.requestPayment(RequestPaymentRequest())
        assertEquals(result, response)
    }

    @Test
    fun transactionFailed() {
        val event = createEvent(EventURN.TRANSACTION_FAILED.urn)
        api.onEvent(event)

        verify(cache).evict("balance_$USER_ID")
    }

    @Test
    fun transactionSuccessfull() {
        val event = createEvent(EventURN.TRANSACTION_SUCCESSFULL.urn)
        api.onEvent(event)

        verify(cache).evict("balance_$USER_ID")
    }

    @Test
    fun transactionPending() {
        val event = createEvent(EventURN.TRANSACTION_PENDING.urn)
        api.onEvent(event)

        verify(cache).evict("balance_$USER_ID")
    }

    private fun createEvent(type: String): Event =
        Event(
            type = type,
            payload = """
            {
                "accountId": $USER_ID
            }
            """.trimIndent()
        )
}
