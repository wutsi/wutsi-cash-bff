package com.wutsi.application.cash.endpoint.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.dto.CashinRequest
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.application.cash.service.UserProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType.Prompt
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.DialogType.Error
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateCashinRequest
import com.wutsi.platform.tenant.dto.Tenant
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat
import javax.validation.Valid

@RestController
@RequestMapping("/commands/cashin")
class CashinCommand(
    private val paymentApi: WutsiPaymentApi,
    private val userApi: WutsiAccountApi,
    private val tenantProvider: TenantProvider,
    private val userProvider: UserProvider,
    private val logger: KVLogger,
    private val urlBuilder: URLBuilder,
    private val objectMapper: ObjectMapper,
) : AbstractCommand() {
    @PostMapping
    fun index(@RequestBody @Valid request: CashinRequest): Action {
        logger.add("amount", request.amount)
        logger.add("payment_token", request.paymentToken)

        // Validate
        val tenant = tenantProvider.get()
        val user = userApi.getAccount(userProvider.id()).account
        val error = validate(request, tenant, user)
        if (error != null)
            return error

        // Cashin
        try {
            val response = paymentApi.createCashin(
                CreateCashinRequest(
                    paymentMethodToken = request.paymentToken,
                    amount = request.amount,
                    currency = tenantProvider.get().currency
                )
            )
            logger.add("transaction_id", response.id)
            logger.add("transaction_status", response.status)

            return Action(
                type = Route,
                url = if (response.status == Status.SUCCESSFUL.name)
                    urlBuilder.build("cashin/success?amount=${request.amount}")
                else
                    urlBuilder.build("cashin/pending")
            )
        } catch (ex: FeignException) {
            throw TransactionException.of(objectMapper, ex)
        }
    }

    private fun validate(request: CashinRequest, tenant: Tenant, user: Account): Action? {
        if (request.amount == 0.0)
            return Action(
                type = Prompt,
                prompt = Dialog(
                    type = Error,
                    message = getText("prompt.error.amount-required")
                )
            )

        val limits = tenant.limits.find { it.country == user.country }!!
        if (request.amount < limits.minCashin) {
            val amountText = DecimalFormat(tenant.monetaryFormat).format(limits.minCashin)
            return Action(
                type = Prompt,
                prompt = Dialog(
                    type = Error,
                    message = getText("prompt.error.min-cashin", arrayOf(amountText))
                )
            )
        }
        return null
    }
}
