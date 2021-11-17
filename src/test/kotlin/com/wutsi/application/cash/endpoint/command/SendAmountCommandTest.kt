package com.wutsi.application.cash.endpoint.command

import com.wutsi.application.cash.dto.SendAmountRequest
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.platform.payment.WutsiPaymentApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendAmountCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    public val port: Int = 0

    private lateinit var url: String

    @MockBean
    private lateinit var paymentApi: WutsiPaymentApi

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/send/amount"
    }

    @Test
    fun success() {
        // WHEN
        val request = SendAmountRequest(
            amount = 1000.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(Route, action.type)
        assertEquals("http://localhost:0/send/recipient?amount=1000.0", action.url)
    }
}
