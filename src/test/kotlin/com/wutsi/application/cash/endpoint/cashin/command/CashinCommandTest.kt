package com.wutsi.application.cash.endpoint.cashin.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateCashinResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CashinCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/cashin?amount=10000&payment-token=xxxx"
    }

    @Test
    fun success() {
        // GIVEN
        val resp = CreateCashinResponse(id = "111", status = Status.SUCCESSFUL.name)
        doReturn(resp).whenever(paymentApi).createCashin(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/cashin/success?amount=10000.0", action.url)
    }

    @Test
    fun pending() {
        // GIVEN
        val resp = CreateCashinResponse(id = "111", status = Status.PENDING.name)
        doReturn(resp).whenever(paymentApi).createCashin(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/cashin/pending", action.url)
    }

    @Test
    fun failure() {
        // GIVEN
        val ex = createFeignException("transaction-failed", ErrorCode.NONE)
        doThrow(ex).whenever(paymentApi).createCashin(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error.name, action.prompt?.attributes?.get("type"))
        assertEquals(getText("prompt.error.transaction-failed"), action.prompt?.attributes?.get("message"))
    }

    @Test
    fun failureNotEnoughFunds() {
        // GIVEN
        val ex = createFeignException("transaction-failed", ErrorCode.NOT_ENOUGH_FUNDS)
        doThrow(ex).whenever(paymentApi).createCashin(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error.name, action.prompt?.attributes?.get("type"))
        assertEquals(
            getText("prompt.error.transaction-failed.NOT_ENOUGH_FUNDS"),
            action.prompt?.attributes?.get("message")
        )
    }
}
