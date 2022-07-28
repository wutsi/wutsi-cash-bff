package com.wutsi.application.cash.endpoint.cashout.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateCashoutResponse
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URLEncoder
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CashoutCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/cashout?amount=10000&payment-token=xxx&idempotency-key=123"
    }

    @Test
    fun success() {
        // GIVEN
        val resp = CreateCashoutResponse(id = "111", status = Status.SUCCESSFUL.name)
        doReturn(resp).whenever(paymentApi).createCashout(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/transaction/success?transaction-id=111", action.url)
    }

    @Test
    fun pending() {
        // GIVEN
        val resp = CreateCashoutResponse(id = "111", status = Status.PENDING.name)
        doReturn(resp).whenever(paymentApi).createCashout(any())

        val tx = Transaction(status = Status.PENDING.name)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/transaction/processing?transaction-id=111", action.url)
    }

    @Test
    fun failure() {
        // GIVEN
        val ex = createFeignException("transaction-failed", ErrorCode.NONE)
        doThrow(ex).whenever(paymentApi).createCashout(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        val error = getText("prompt.error.unexpected-error")
        assertEquals(ActionType.Route, action.type)
        assertEquals(
            "http://localhost:0/transaction/error?type=CASHOUT&payment-token=xxx&error=" + URLEncoder.encode(
                error,
                "utf-8"
            ),
            action.url
        )
    }
}
