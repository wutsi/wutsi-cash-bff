package com.wutsi.application.cash.endpoint.cashin.screen

import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CashinSuccessScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    @Test
    fun success() {
        val url = "http://localhost:$port/cashin/success?amount=10000"
        assertEndpointEquals("/screens/cashin/success.json", url)
    }

    @Test
    fun error() {
        val url = "http://localhost:$port/cashin/success?amount=10000&error=Unexpected+error"
        assertEndpointEquals("/screens/cashin/error.json", url)
    }
}
