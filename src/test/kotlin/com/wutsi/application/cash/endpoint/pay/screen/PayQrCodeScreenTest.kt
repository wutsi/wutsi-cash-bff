package com.wutsi.application.cash.endpoint.pay.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.qr.WutsiQrApi
import com.wutsi.platform.qr.dto.EncodeQRCodeResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PayQrCodeScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @MockBean
    private lateinit var qrApi: WutsiQrApi

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/pay/qr-code?amount=5000&payment-request-id=1111"
    }

    @Test
    fun index() {
        doReturn(EncodeQRCodeResponse("xxxx")).whenever(qrApi).encode(any())

        assertEndpointEquals("/screens/pay/qr-code.json", url)
    }
}
