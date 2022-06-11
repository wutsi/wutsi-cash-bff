package com.wutsi.application.cash.endpoint.cashout.command

import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.endpoint.cashout.dto.CashoutRequest
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.payment.core.Money
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat
import javax.validation.Valid

@RestController
@RequestMapping("/commands/cashout/amount")
class CashoutAmountCommand(
    private val tenantProvider: TenantProvider,
) : AbstractCommand() {
    @PostMapping
    fun index(@RequestBody @Valid request: CashoutRequest): Action {
        logger.add("amount", request.amount)
        logger.add("payment_token", request.paymentToken)

        // Validate
        val tenant = tenantProvider.get()
        val error = validate(request, tenant)
        if (error != null)
            return error

        // Cashout
        return gotoUrl(
            url = urlBuilder.build("cashout/confirm?amount=${request.amount}&payment-token=${request.paymentToken}")
        )
    }

    private fun validate(request: CashoutRequest, tenant: Tenant): Action? {
        if (request.amount == 0.0)
            return showError(
                message = getText("prompt.error.amount-required")
            )

        if (request.amount < tenant.limits.minCashout) {
            val amountText = DecimalFormat(tenant.monetaryFormat).format(tenant.limits.minCashin)
            return showError(
                message = getText("prompt.error.min-cashout", arrayOf(amountText))
            )
        }

        val balance = getBalance()
        if (request.amount > balance.value)
            return Action(
                type = ActionType.Prompt,
                prompt = Dialog(
                    type = DialogType.Error,
                    message = getText("prompt.error.transaction-failed.NOT_ENOUGH_FUNDS"),
                    title = getText("prompt.error.title"),
                ).toWidget()
            )
        return null
    }

    private fun getBalance(): Money {
        val balance = paymentApi.getBalance(securityContext.currentAccountId()).balance
        return Money(balance.amount, balance.currency)
    }
}
