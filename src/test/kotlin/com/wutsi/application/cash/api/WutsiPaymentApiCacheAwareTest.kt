package com.wutsi.application.cash.api

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
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
    private lateinit var api: WutsiPaymentApi

    @BeforeEach
    fun setUp() {
        delegate = mock()
        cache = mock()

        securityManager = mock()
        doReturn(USER_ID).whenever(securityManager).currentUserId()

        api = WutsiPaymentApiCacheAware(delegate, securityManager, cache)
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
    }

    @Test
    fun getBalanceFromServer() {
        // GIVEN
        doReturn(null).whenever(cache).get("balance_$USER_ID")

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
    fun runJob() {
        val result = RunJobResponse()
        doReturn(result).whenever(delegate).runJob(any())

        val response = api.runJob("any")
        assertEquals(result, response)
    }
}
