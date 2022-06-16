package com.wutsi.application.cash.endpoint.transaction.widget

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.flutter.sdui.Noop
import com.wutsi.flutter.sdui.Widget
import com.wutsi.platform.payment.core.Status
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/widgets/transaction/status")
class TransactionStatusWidget : AbstractQuery() {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionStatusWidget::class.java)
        const val MAX_COUNT = 3
    }

    @PostMapping
    fun index(
        @RequestParam(name = "transaction-id") transactionId: String,
        @RequestParam count: Int
    ): Widget {
        logger.add("transaction_id", transactionId)
        logger.add("count", count)
        try {
            val tx = paymentApi.getTransaction(transactionId).transaction
            logger.add("transaction_status", tx.status)

            return if (tx.status == Status.SUCCESSFUL.name)
                toTransactionStatusWidget(tx).toWidget()
            else if (tx.status == Status.PENDING.name && count > MAX_COUNT)
                toTransactionStatusWidget(tx).toWidget()
            else
                Noop().toWidget()
        } catch (ex: FeignException.Conflict) {
            return toTransactionStatusWidget(null, getErrorText(ex)).toWidget()
        } catch (ex: Throwable) {
            LOGGER.warn("Unexpected error", ex)
            return Noop().toWidget()
        }
    }
}
