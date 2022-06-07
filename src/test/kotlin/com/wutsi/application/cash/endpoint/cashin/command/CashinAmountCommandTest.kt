package com.wutsi.application.cash.endpoint.cashin.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.application.cash.endpoint.cashin.dto.CashinRequest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateCashinResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CashinAmountCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String
    private lateinit var request: CashinRequest

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/cashin/amount"
        request = CashinRequest(paymentToken = "xxx", amount = 10000.0)
    }

    @Test
    fun success() {
        // GIVEN
        val resp = CreateCashinResponse(id = "111", status = Status.SUCCESSFUL.name)
        doReturn(resp).whenever(paymentApi).createCashin(any())

        // WHEN
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/cashin/confirm?amount=10000.0&payment-token=xxx", action.url)
    }

    @Test
    fun minCashin() {
        // WHEN
        request = CashinRequest(paymentToken = "xxx", amount = 1.0)
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error.name, action.prompt?.attributes?.get("type"))
        assertEquals(
            getText("prompt.error.min-cashin", arrayOf("5,000 XAF")),
            action.prompt?.attributes?.get("message")
        )
    }

    @Test
    fun noValue() {
        // WHEN
        request = CashinRequest(paymentToken = "xxx", amount = 0.0)
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error.name, action.prompt?.attributes?.get("type"))
        assertEquals(getText("prompt.error.amount-required"), action.prompt?.attributes?.get("message"))
    }
}
