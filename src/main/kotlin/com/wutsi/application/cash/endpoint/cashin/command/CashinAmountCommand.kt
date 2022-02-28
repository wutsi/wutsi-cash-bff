package com.wutsi.application.cash.endpoint.cashin.command

import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.endpoint.cashin.dto.CashinRequest
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat
import javax.validation.Valid

@RestController
@RequestMapping("/commands/cashin/amount")
class CashinAmountCommand(
    private val tenantProvider: TenantProvider,
) : AbstractCommand() {
    @PostMapping
    fun index(@RequestBody @Valid request: CashinRequest): Action {
        logger.add("amount", request.amount)
        logger.add("payment_token", request.paymentToken)

        // Validate
        val tenant = tenantProvider.get()
        val error = validate(request, tenant)
        if (error != null)
            return error

        // Cashin
        return gotoUrl(
            url = urlBuilder.build("cashin/confirm?amount=${request.amount}&payment-token=${request.paymentToken}")
        )
    }

    private fun validate(request: CashinRequest, tenant: Tenant): Action? {
        if (request.amount == 0.0)
            return showError(
                message = getText("prompt.error.amount-required")
            )

        if (request.amount < tenant.limits.minCashin) {
            val amountText = DecimalFormat(tenant.monetaryFormat).format(tenant.limits.minCashin)
            return showError(
                message = getText("prompt.error.min-cashin", arrayOf(amountText))
            )
        }
        return null
    }
}
