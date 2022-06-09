package com.wutsi.application.cash.endpoint.cashout.screen

import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CashoutSuccessScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()
        url = "http://localhost:$port/cashout/success?amount=10000"
    }

    @Test
    fun success() {
        assertEndpointEquals("/screens/cashout/success.json", url)
    }

    @Test
    fun error() {
        assertEndpointEquals("/screens/cashout/error.json", url)
    }
}
