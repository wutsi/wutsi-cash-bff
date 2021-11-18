package com.wutsi.application.cash.endpoint.command

import com.wutsi.application.cash.dto.SendRecipientRequest
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.platform.payment.WutsiPaymentApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendRecipientCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    public val port: Int = 0

    private lateinit var url: String

    @MockBean
    private lateinit var paymentApi: WutsiPaymentApi

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/send/recipient?amount=3000.0"
    }

    @Test
    fun success() {
        // WHEN
        val request = SendRecipientRequest(
            phoneNumber = "+237666666666"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        kotlin.test.assertEquals(200, response.statusCodeValue)

        val action = response.body
        kotlin.test.assertEquals(Route, action.type)
        kotlin.test.assertEquals("http://localhost:0/send/confirm", action.url)
    }
}
