package com.wutsi.application.cash.endpoint.history.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.SearchAccountResponse
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.SearchTransactionResponse
import com.wutsi.platform.payment.dto.TransactionSummary
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class HistoryScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    public val port: Int = 0

    private lateinit var url: String

    @MockBean
    private lateinit var paymentApi: WutsiPaymentApi

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/history"
    }

    @Test
    fun index() {
        val txs = listOf(
            createCashInOutTransactionSummary(true, "FAILED"),
            createCashInOutTransactionSummary(true),
            createTransferTransactionSummary(USER_ID, 100),
            createTransferTransactionSummary(101, USER_ID, "PENDING"),
            createTransferTransactionSummary(101, USER_ID),
            createCashInOutTransactionSummary(false)
        )
        doReturn(SearchTransactionResponse(txs)).whenever(paymentApi).searchTransaction(any())

        val accounts = listOf(
            createAccount(USER_ID),
            createAccount(100),
            createAccount(101)
        )
        doReturn(SearchAccountResponse(accounts)).whenever(accountApi).searchAccount(any())

        assertEndpointEquals("/screens/history/history.json", url)
    }

    private fun createTransferTransactionSummary(userId: Long, recipientId: Long, status: String = "SUCCESSFUL") =
        TransactionSummary(
            userId = userId,
            recipientId = recipientId,
            type = "TRANSFER",
            tenantId = TENANT_ID.toLong(),
            status = status,
            net = 10000.0,
            amount = 10000.0,
            description = "Sample description",
            created = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)
        )

    private fun createCashInOutTransactionSummary(cashin: Boolean, status: String = "SUCCESSFUL") =
        TransactionSummary(
            userId = USER_ID,
            type = if (cashin) "CASHIN" else "CASHOUT",
            tenantId = TENANT_ID.toLong(),
            status = status,
            net = 10000.0,
            amount = 10000.0,
            description = "Sample description",
            created = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)
        )

    private fun createAccount(id: Long) = AccountSummary(
        id = id,
        displayName = "Name $id"
    )
}
