package com.wutsi.application.cash.endpoint.transaction.widget

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.payment.entity.TransactionType
import com.wutsi.platform.payment.error.ErrorURN
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class TransactionStatusWidgetTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    @Test
    fun pending() {
        val tx = createTransaction(status = Status.PENDING)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val url = "http://localhost:$port/widgets/transaction/status?transaction-id=1111&count=1"
        assertEndpointEquals("/widgets/transaction/status-pending.json", url)
    }

    @Test
    fun `success TRANSFER`() {
        val tx = createTransaction(status = Status.SUCCESSFUL, type = TransactionType.TRANSFER)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val url = "http://localhost:$port/widgets/transaction/status?transaction-id=1111&count=1"
        assertEndpointEquals("/widgets/transaction/status-success-transfer.json", url)
    }

    @Test
    fun `success CASHIN`() {
        val tx = createTransaction(status = Status.SUCCESSFUL, type = TransactionType.CASHIN)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val url = "http://localhost:$port/widgets/transaction/status?transaction-id=1111&count=1"
        assertEndpointEquals("/widgets/transaction/status-success-cashin.json", url)
    }

    @Test
    fun `success CASHOUT`() {
        val tx = createTransaction(status = Status.SUCCESSFUL, type = TransactionType.CASHOUT)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val url = "http://localhost:$port/widgets/transaction/status?transaction-id=1111&count=1"
        assertEndpointEquals("/widgets/transaction/status-success-cashout.json", url)
    }

    @Test
    fun failed() {
        val ex = createFeignException(
            errorCode = ErrorURN.TRANSACTION_FAILED.urn,
            downstreamError = ErrorCode.UNEXPECTED_ERROR
        )
        doThrow(ex).whenever(paymentApi).getTransaction(any())

        val url = "http://localhost:$port/widgets/transaction/status?transaction-id=1111&count=1"
        assertEndpointEquals("/widgets/transaction/status-failed.json", url)
    }

    @Test
    fun tooManyTries() {
        val tx = createTransaction(status = Status.PENDING)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val url =
            "http://localhost:$port/widgets/transaction/status?transaction-id=1111&count=" + (TransactionStatusWidget.MAX_COUNT + 1)
        assertEndpointEquals("/widgets/transaction/status-too-many-tries.json", url)
    }

    private fun createTransaction(status: Status, type: TransactionType = TransactionType.TRANSFER) = Transaction(
        status = status.name,
        type = type.name
    )
}
