package com.wutsi.application.cash.endpoint.history.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.ecommerce.order.WutsiOrderApi
import com.wutsi.ecommerce.order.dto.GetOrderResponse
import com.wutsi.ecommerce.order.dto.Order
import com.wutsi.ecommerce.order.entity.OrderStatus
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.ListPaymentMethodResponse
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.account.dto.SearchAccountResponse
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.payment.entity.TransactionType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class TransactionScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @MockBean
    private lateinit var orderApi: WutsiOrderApi

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/transaction?id=111"

        val paymentMethods = listOf(
            createPaymentMethodSummary("A", "11111"),
            createPaymentMethodSummary("B", "22222"),
            createPaymentMethodSummary("C", "33333"),
        )
        doReturn(ListPaymentMethodResponse(paymentMethods)).whenever(accountApi).listPaymentMethods(any())

        val accounts = listOf(
            createAccount(USER_ID),
            createAccount(100),
            createAccount(101)
        )
        doReturn(SearchAccountResponse(accounts)).whenever(accountApi).searchAccount(any())

        val order = createOrder()
        doReturn(GetOrderResponse(order)).whenever(orderApi).getOrder(any())
    }

    @Test
    fun transfer() {
        val tx = createTransferTransaction(USER_ID, 100, type = TransactionType.TRANSFER, status = Status.SUCCESSFUL)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        assertEndpointEquals("/screens/history/transaction-transfer.json", url)
    }

    @Test
    fun cashin() {
        val tx = createTransferTransaction(USER_ID, 100, type = TransactionType.CASHIN, status = Status.FAILED)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        assertEndpointEquals("/screens/history/transaction-cashin.json", url)
    }

    @Test
    fun cashout() {
        val tx = createTransferTransaction(USER_ID, 100, type = TransactionType.CASHOUT, status = Status.PENDING)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        assertEndpointEquals("/screens/history/transaction-cashout.json", url)
    }

    @Test
    fun charge() {
        val tx = createTransferTransaction(USER_ID, 100, type = TransactionType.CASHOUT, status = Status.PENDING)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        assertEndpointEquals("/screens/history/transaction-charge.json", url)
    }

    private fun createOrder() = Order(
        id = "111",
        status = OrderStatus.CANCELLED.name,
        created = OffsetDateTime.now()
    )

    private fun createTransferTransaction(
        accountId: Long,
        recipientId: Long,
        status: Status = Status.SUCCESSFUL,
        type: TransactionType = TransactionType.TRANSFER
    ) =
        Transaction(
            accountId = accountId,
            recipientId = recipientId,
            type = type.name,
            status = status.name,
            net = 9000.0,
            amount = 10100.0,
            fees = 1000.0,
            gatewayFees = 100.0,
            description = "Sample description",
            orderId = "203920-3209-3209-3209ref",
            created = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC),
        )

    private fun createPaymentMethodSummary(token: String, maskedNumber: String) = PaymentMethodSummary(
        token = token,
        maskedNumber = maskedNumber
    )

    private fun createAccount(id: Long) = AccountSummary(
        id = id,
        displayName = "Name $id"
    )
}
