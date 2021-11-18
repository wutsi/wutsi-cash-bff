package com.wutsi.application.cash.endpoint.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.dto.SendRequest
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.SearchAccountResponse
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.ErrorCode.NONE
import com.wutsi.platform.payment.dto.CreateTransferResponse
import feign.FeignException
import feign.Request
import feign.Request.HttpMethod.POST
import feign.RequestTemplate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import java.nio.charset.Charset
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    public val port: Int = 0

    private lateinit var url: String

    @MockBean
    private lateinit var paymentApi: WutsiPaymentApi

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/send?amount=3000.0&phone-number=+237666666666"

        val account = AccountSummary(id = 111)
        doReturn(SearchAccountResponse(listOf(account))).whenever(accountApi).searchAccount(any(), any(), any())
    }

    @Test
    fun success() {
        // Given
        doReturn(CreateTransferResponse("xxx", "SUCCESSFUL")).whenever(paymentApi).createTransfer(any())

        // WHEN
        val request = SendRequest(
            pin = "123456"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(Route, action.type)
        assertEquals("http://localhost:0/send/success?amount=3000.0&recipient=%2B237666666666", action.url)
    }

    @Test
    fun transferFailed() {
        // Given
        val ex = createFeignException("failed")
        doThrow(ex).whenever(paymentApi).createTransfer(any())

        // WHEN
        val request = SendRequest(
            pin = "123456"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error, action.prompt?.type)
        assertEquals(getText("prompt.error.transaction-failed"), action.prompt?.message)
    }

    @Test
    fun notEnoughFunds() {
        // Given
        val ex = createFeignException("failed", ErrorCode.NOT_ENOUGH_FUNDS)
        doThrow(ex).whenever(paymentApi).createTransfer(any())

        // WHEN
        val request = SendRequest(
            pin = "123456"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error, action.prompt?.type)
        assertEquals(getText("prompt.error.transaction-failed.NOT_ENOUGH_FUNDS"), action.prompt?.message)
    }

    @Test
    fun payeeNotAllowedToReceive() {
        // Given
        val ex = createFeignException("failed", ErrorCode.PAYEE_NOT_ALLOWED_TO_RECEIVE)
        doThrow(ex).whenever(paymentApi).createTransfer(any())

        // WHEN
        val request = SendRequest(
            pin = "123456"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error, action.prompt?.type)
        assertEquals(getText("prompt.error.transaction-failed.PAYEE_NOT_ALLOWED_TO_RECEIVE"), action.prompt?.message)
    }

    @Test
    fun recipientNotFound() {
        // Given
        doReturn(SearchAccountResponse()).whenever(accountApi).searchAccount(any(), any(), any())

        // WHEN
        val request = SendRequest(
            pin = "123456"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error, action.prompt?.type)
        assertEquals(getText("prompt.error.recipient-not-found"), action.prompt?.message)
    }

    @Test
    fun noValue() {
        // WHEN
        url = "http://localhost:$port/commands/send?amount=0&phone-number=+237666666666"
        val request = SendRequest(
            pin = "123456"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error, action.prompt?.type)
        assertEquals(getText("prompt.error.amount-required"), action.prompt?.message)
    }

    private fun createFeignException(code: String, errorCode: ErrorCode = NONE) = FeignException.Conflict(
        "failed",
        Request.create(POST, "https://www.google.ca", emptyMap(), "".toByteArray(), Charset.defaultCharset(), RequestTemplate()),
        """
            {
                "error":{
                    "code": "$code",
                    "downstreamCode": "$errorCode"
                }
            }
        """.trimIndent().toByteArray(),
        emptyMap()
    )
}
