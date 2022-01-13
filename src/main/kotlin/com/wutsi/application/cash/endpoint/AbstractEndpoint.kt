package com.wutsi.application.cash.endpoint

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.wutsi.application.cash.exception.PasswordInvalidException
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.application.shared.service.SecurityContext
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType.Page
import com.wutsi.flutter.sdui.enums.ActionType.Prompt
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.DialogType.Error
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.Money
import com.wutsi.platform.tenant.dto.MobileCarrier
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.web.bind.annotation.ExceptionHandler
import java.net.URLEncoder

abstract class AbstractEndpoint {
    @Autowired
    private lateinit var messages: MessageSource

    @Autowired
    protected lateinit var logger: KVLogger

    @Autowired
    private lateinit var phoneNumberUtil: PhoneNumberUtil

    @Autowired
    protected lateinit var paymentApi: WutsiPaymentApi

    @Autowired
    protected lateinit var securityContext: SecurityContext

    @ExceptionHandler(Throwable::class)
    fun onThrowable(ex: Throwable): Action =
        showError(getText("prompt.error.unexpected-error"), ex)

    @ExceptionHandler(TransactionException::class)
    fun onTransactionException(ex: TransactionException): Action {
        val message = getTransactionErrorMessage(ex.error)
        return showError(message, ex)
    }

    protected fun getTransactionErrorMessage(error: ErrorCode): String =
        getTransactionErrorMessage(error.name)

    private fun getTransactionErrorMessage(error: String?): String =
        try {
            getText("prompt.error.transaction-failed.$error")
        } catch (ex: Exception) {
            getText("prompt.error.transaction-failed")
        }

    @ExceptionHandler(PasswordInvalidException::class)
    fun onPasswordInvalid(ex: PasswordInvalidException): Action =
        showError(getText("prompt.error.password-invalid"), ex)

    protected fun showError(message: String, e: Throwable? = null): Action {
        val action = Action(
            type = Prompt,
            prompt = Dialog(
                title = getText("prompt.error.title"),
                type = Error,
                message = message
            ).toWidget()
        )

        log(action, e)
        return action
    }

    private fun log(action: Action, e: Throwable?) {
        logger.add("action_type", action.type)
        logger.add("action_url", action.url)
        logger.add("action_prompt_type", action.prompt?.type)
        logger.add("action_prompt_message", action.prompt?.attributes?.get("message"))
        if (e != null) {
            logger.setException(e)
        }
    }

    protected fun getText(key: String, args: Array<Any?> = emptyArray()) =
        messages.getMessage(key, args, LocaleContextHolder.getLocale()) ?: key

    protected fun gotoPage(page: Int) = Action(
        type = Page,
        url = "page:/$page"
    )

    protected fun gotoRoute(path: String, replacement: Boolean? = null, parameters: Map<String, String>? = null) =
        Action(
            type = Route,
            url = "route:$path",
            replacement = replacement,
            parameters = parameters
        )

    protected fun gotoUrl(url: String, replacement: Boolean? = null, parameters: Map<String, String>? = null) = Action(
        type = Route,
        url = url,
        replacement = replacement,
        parameters = parameters
    )

    protected fun sanitizePhoneNumber(phoneNumber: String): String {
        val tmp = phoneNumber.trim()
        return if (tmp.startsWith("+"))
            tmp
        else
            "+$tmp"
    }

    protected fun formattedPhoneNumber(phoneNumber: String?, country: String? = null): String? {
        if (phoneNumber == null)
            return null

        val number = phoneNumberUtil.parse(phoneNumber, country ?: "")
        return phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
    }

    protected fun encodeURLParam(text: String?): String =
        text?.let { URLEncoder.encode(it, "utf-8") } ?: ""

    protected fun getMobileCarrier(paymentMethod: PaymentMethodSummary, tenant: Tenant): MobileCarrier? =
        tenant.mobileCarriers.findLast { it.code.equals(paymentMethod.provider, true) }

    protected fun getBalance(tenant: Tenant): Money {
        try {
            val userId = securityContext.currentAccountId()
            val balance = paymentApi.getBalance(userId).balance
            return Money(
                value = balance.amount,
                currency = balance.currency
            )
        } catch (ex: Throwable) {
            return Money(currency = tenant.currency)
        }
    }
}
