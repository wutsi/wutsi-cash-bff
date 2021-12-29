package com.wutsi.application.cash.endpoint.pay.command

import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.application.cash.endpoint.pay.dto.PayAmountRequest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.DialogType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.MessageSource
import java.util.Locale

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PayAmountCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @Autowired
    private lateinit var messages: MessageSource

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/pay/amount"
    }

    @Test
    fun success() {
        // WHEN
        val request = PayAmountRequest(
            amount = 1000.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        kotlin.test.assertEquals(200, response.statusCodeValue)

        val action = response.body
        kotlin.test.assertEquals(ActionType.Route, action.type)
        kotlin.test.assertEquals("http://localhost:0/pay/scan?amount=1000.0", action.url)
    }

    @Test
    fun zero() {
        // WHEN
        val request = PayAmountRequest(
            amount = 0.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        kotlin.test.assertEquals(200, response.statusCodeValue)

        val action = response.body
        kotlin.test.assertEquals(ActionType.Prompt, action.type)
        kotlin.test.assertEquals(DialogType.Error.name, action.prompt?.attributes?.get("type"))
        kotlin.test.assertEquals(
            messages.getMessage("prompt.error.amount-required", emptyArray(), Locale.ENGLISH),
            action.prompt?.attributes?.get("message")
        )
    }
}
