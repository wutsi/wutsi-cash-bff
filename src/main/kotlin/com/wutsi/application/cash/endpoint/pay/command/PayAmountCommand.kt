package com.wutsi.application.cash.endpoint.pay.command

import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.endpoint.pay.dto.PayAmountRequest
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType.Prompt
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.payment.dto.CreatePaymentRequestRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/commands/pay/amount")
class PayAmountCommand(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,
) : AbstractCommand() {
    @PostMapping
    fun index(@RequestBody @Valid request: PayAmountRequest): Action {
        logger.add("amount", request.amount)

        // Validate
        val action = validate(request)
        if (action != null)
            return action

        // Create the payment request
        val tenant = tenantProvider.get()
        val paymentRequestId = paymentApi.createPaymentRequest(
            CreatePaymentRequestRequest(
                amount = request.amount,
                currency = tenant.currency,
                timeToLive = 300
            )
        ).id

        // Goto next page
        return Action(
            type = Route,
            url = urlBuilder.build("pay/qr-code?payment-request-id=$paymentRequestId&amount=${request.amount}")
        )
    }

    private fun validate(request: PayAmountRequest): Action? {
        if (request.amount == 0.0)
            return Action(
                type = Prompt,
                prompt = Dialog(
                    type = DialogType.Error,
                    message = getText("prompt.error.amount-required"),
                    title = getText("prompt.error.title")
                ).toWidget()
            )
        return null
    }
}
