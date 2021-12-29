package com.wutsi.application.cash.endpoint.pay.screen

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.application.cash.endpoint.pay.dto.ScanRequest
import com.wutsi.application.cash.service.AccountQrParser
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.dto.CreateTransferResponse
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import java.nio.charset.Charset
import java.util.UUID
import kotlin.test.Ignore

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PayProcessScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @MockBean
    private lateinit var qrParser: AccountQrParser

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/pay/process?amount=5000"
    }

    @Test
    fun success() {
        // GIVEN
        val resp = CreateTransferResponse(id = UUID.randomUUID().toString())
        doReturn(resp).whenever(paymentApi).createTransfer(any())

        // WHEN
        val request = ScanRequest(code = createQRCode(), format = "QR_CODE")
        val response = rest.postForEntity(url, request, Map::class.java)

        // THEN
        assertJsonEquals("/screens/pay/process.json", response.body)
    }

    @Test
    fun verificationFailed() {
        // GIVEN
        doThrow(RuntimeException::class).whenever(qrParser).parse(any())

        // WHEN
        val request = ScanRequest(code = createQRCode(), format = "QR_CODE")
        val response = rest.postForEntity(url, request, Map::class.java)

        // THEN
        assertJsonEquals("/screens/pay/process-bad-qr-code.json", response.body)
    }

    @Test
    @Ignore
    fun paymentFailed() {
        // GIVEN
        val ex = createFeignException("failed")
        doThrow(ex).whenever(paymentApi).createTransfer(any())

        // WHEN
        val request = ScanRequest(code = createQRCode(), format = "QR_CODE")
        val response = rest.postForEntity(url, request, Map::class.java)

        // THEN
        assertJsonEquals("/screens/pay/process-payment-failed.json", response.body)
    }

    private fun createQRCode(userId: Long = USER_ID) =
        JWT.create()
            .withSubject(userId.toString())
            .sign(Algorithm.HMAC256(UUID.randomUUID().toString()))
            .toString()

    private fun createFeignException(code: String, errorCode: ErrorCode = ErrorCode.NONE) = FeignException.Conflict(
        "failed",
        Request.create(
            Request.HttpMethod.POST,
            "https://www.google.ca",
            emptyMap(),
            "".toByteArray(),
            Charset.defaultCharset(),
            RequestTemplate()
        ),
        """
            {
                "error":{
                    "code": "$code",
                    "downstreamCode": "$errorCode"
                }
            }
        """.trimIndent().toByteArray(),
        emptyMap()
    )
}
