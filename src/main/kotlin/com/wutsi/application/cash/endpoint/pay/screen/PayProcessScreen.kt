package com.wutsi.application.cash.endpoint.pay.screen

import com.auth0.jwt.JWT
import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.cash.endpoint.Theme
import com.wutsi.application.cash.endpoint.pay.dto.ScanRequest
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.application.cash.service.QrKeyVerifier
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.ButtonType
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.payment.dto.CreateTransferRequest
import com.wutsi.platform.tenant.dto.Tenant
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pay/process")
class PayProcessScreen(
    private val verifier: QrKeyVerifier,
    private val tenantProvider: TenantProvider,
    private val objectMapper: ObjectMapper,
) : AbstractQuery() {
    @PostMapping
    fun index(@RequestParam amount: Double, @RequestBody request: ScanRequest): Widget {
        logger.add("amount", amount)
        logger.add("scan_format", request.format)
        logger.add("scan_code", "***")

        val tenant = tenantProvider.get()

        // Verify the QR code
        try {
            verifier.verify(request.code)
        } catch (ex: Exception) {
            logger.setException(ex)
            return error(getText("page.pay.error.qr-code-invalid"), amount, tenant)
        }

        // Process
        try {
            process(amount, request, tenant)
        } catch (ex: TransactionException) {
            logger.setException(ex)
            return error(getErrorMessage(ex), amount, tenant)
        }

        return success(amount, tenant)
    }

    private fun error(error: String, amount: Double, tenant: Tenant): Widget =
        Screen(
            id = Page.PAY_ERROR,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.WHITE_COLOR,
                foregroundColor = Theme.BLACK_COLOR,
                title = getText("page.pay-process.app-bar.title"),
            ),
            child = Column(
                children = listOf(
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = MoneyText(
                            value = amount,
                            currency = tenant.currency,
                            numberFormat = tenant.numberFormat
                        )
                    ),
                    Container(
                        padding = 20.0,
                        child = Icon(
                            code = Theme.ICON_ERROR,
                            size = 80.0,
                            color = Theme.DANGER_COLOR
                        )
                    ),
                    Container(
                        alignment = Alignment.Center,
                        padding = 10.0,
                        child = Text(
                            caption = error,
                            alignment = TextAlignment.Center,
                            size = Theme.X_LARGE_TEXT_SIZE,
                            color = Theme.DANGER_COLOR
                        )
                    ),
                    Container(
                        padding = 10.0,
                        child = Button(
                            type = ButtonType.Elevated,
                            caption = getText("page.pay-process.button.submit"),
                            action = Action(
                                type = ActionType.Route,
                                url = "route:/~"
                            )
                        )
                    )
                ),
            )
        ).toWidget()

    private fun success(amount: Double, tenant: Tenant): Widget =
        Screen(
            id = Page.PAY_SUCCESS,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.WHITE_COLOR,
                foregroundColor = Theme.BLACK_COLOR,
                title = getText("page.pay-process.app-bar.title"),
            ),
            child = Column(
                children = listOf(
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = MoneyText(
                            value = amount,
                            currency = tenant.currency,
                            numberFormat = tenant.numberFormat
                        )
                    ),
                    Container(
                        padding = 20.0,
                        child = Icon(
                            code = Theme.ICON_CHECK,
                            size = 80.0,
                            color = Theme.SUCCESS_COLOR
                        )
                    ),
                    Container(
                        alignment = Alignment.Center,
                        padding = 10.0,
                        child = Text(
                            caption = getText("page.pay-process.success"),
                            alignment = TextAlignment.Center,
                            size = Theme.X_LARGE_TEXT_SIZE,
                        )
                    ),
                    Container(
                        padding = 10.0,
                        child = Button(
                            type = ButtonType.Elevated,
                            caption = getText("page.pay-process.button.submit"),
                            action = Action(
                                type = ActionType.Route,
                                url = "route:/~"
                            )
                        )
                    )
                ),
            )
        ).toWidget()

    private fun process(amount: Double, request: ScanRequest, tenant: Tenant) {
        try {
            val jwt = JWT.decode(request.code)
            val merchantId = jwt.subject
            logger.add("merchant_id", merchantId)

            val response = paymentApi.createTransfer(
                CreateTransferRequest(
                    recipientId = merchantId.toLong(),
                    amount = amount,
                    currency = tenant.currency
                )
            )

            logger.add("transaction_id", response.id)
            logger.add("transaction_status", response.status)
        } catch (ex: FeignException) {
            throw TransactionException.of(objectMapper, ex)
        }
    }
}
