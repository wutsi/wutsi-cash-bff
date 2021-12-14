package com.wutsi.application.cash.endpoint.send.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.application.cash.endpoint.send.dto.SendRecipientRequest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.SearchAccountResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.MessageSource
import java.util.Locale
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendRecipientCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @Autowired
    private lateinit var messages: MessageSource

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/send/recipient?amount=3000.0"
    }

    @Test
    fun success() {
        // GIVEN
        val account = AccountSummary(id = 666)
        doReturn(SearchAccountResponse(listOf(account))).whenever(accountApi).searchAccount(any())

        // WHEN
        val request = SendRecipientRequest(
            phoneNumber = "+237666666666"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        kotlin.test.assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(Route, action.type)
        assertEquals("http://localhost:0/send/confirm", action.url)
    }

    @Test
    fun invalidPhoneNumber() {
        // GIVEN
        doReturn(SearchAccountResponse(listOf())).whenever(accountApi).searchAccount(any())

        // WHEN
        val request = SendRecipientRequest(
            phoneNumber = "+237666666666"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        kotlin.test.assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(Route, action.type)
        assertEquals("http://localhost:0/send/confirm", action.url)
    }

    @Test
    fun selfTransfer() {
        // GIVEN
        val account = AccountSummary(id = USER_ID)
        doReturn(SearchAccountResponse(listOf(account))).whenever(accountApi).searchAccount(any())

        // WHEN
        val request = SendRecipientRequest(
            phoneNumber = "+237666666666"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error.name, action.prompt?.attributes?.get("type"))
        assertEquals(
            messages.getMessage("prompt.error.self-transfer", emptyArray(), Locale.ENGLISH),
            action.prompt?.attributes?.get("message")
        )
    }
}
