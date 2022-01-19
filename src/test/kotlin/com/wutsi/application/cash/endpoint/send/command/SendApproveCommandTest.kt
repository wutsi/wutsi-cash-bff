package com.wutsi.application.cash.endpoint.send.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.platform.payment.core.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.MessageSource
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendApproveCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    @Autowired
    private lateinit var messageSource: MessageSource

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/send/approve?transaction-id=xxx"
    }

    @Test
    fun success() {
        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        verify(paymentApi).approveTransaction("xxx")

        val action = response.body
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/send/success?transaction-id=xxx", action.url)
    }

    @Test
    fun error() {
        // GIVEN
        val ex = createFeignException("failed", ErrorCode.EXPIRED, "xxx")
        doThrow(ex).whenever(paymentApi).approveTransaction(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        verify(paymentApi).approveTransaction("xxx")

        val action = response.body
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/send/success?error=EXPIRED&transaction-id=xxx", action.url)
    }
}
