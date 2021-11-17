package com.wutsi.application.cash.endpoint.screen.send

import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendPINScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    public val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/send/pin?amount=3000&phone-number=237666666666"
    }

    @Test
    fun index() {
        assertEndpointEquals("/screens/send/pin.json", url)
    }
}
