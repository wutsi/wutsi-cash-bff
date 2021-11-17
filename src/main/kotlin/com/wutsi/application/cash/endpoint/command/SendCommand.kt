package com.wutsi.application.cash.endpoint.command

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.wutsi.application.cash.dto.SendRequest
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.exception.PasswordMismatchException
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
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.CreateTransferRequest
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import javax.validation.Valid

@RestController
@RequestMapping("/commands/send")
class SendCommand(
    private val logger: KVLogger,
    private val paymentApi: WutsiPaymentApi,
    private val accountApi: WutsiAccountApi,
    private val userProvider: UserProvider,
    private val tenantProvider: TenantProvider,
    private val phoneNumberUtil: PhoneNumberUtil,
    private val urlBuilder: URLBuilder,
) : AbstractCommand() {
    @PostMapping
    fun index(
        @RequestParam amount: Double,
        @RequestParam(name = "phone-number") phoneNumber: String,
        @RequestBody @Valid request: SendRequest,
    ): Action {
        val xphoneNumber = sanitizePhoneNumber(phoneNumber)
        logger.add("pin", "******")
        logger.add("phone_number", xphoneNumber)

        // Recipient
        val recipientId = findRecipientId(xphoneNumber)
            ?: return Action(
                type = Prompt,
                prompt = Dialog(
                    type = Error,
                    title = getText("prompt.error.title"),
                    message = getText("prompt.error.recipient-not-found")
                )
            )

        logger.add("recipient_id", recipientId)

        // Verify the password
        checkPassword(request)
        val tenant = tenantProvider.get()
        try {
            // Perform the transfer
            val response = paymentApi.createTransfer(
                CreateTransferRequest(
                    recipientId = recipientId,
                    amount = amount,
                    currency = tenant.currency
                )
            )
            logger.add("transaction_id", response.id)
            logger.add("transaction_status", response.status)

            val recipient = URLEncoder.encode(formattedPhoneNumber(xphoneNumber), "utf-8")
            return Action(
                type = Route,
                url = urlBuilder.build("send/success?amount=$amount&recipient=$recipient")
            )
        } catch (ex: FeignException) {
            throw TransactionException(ex)
        }
    }

    private fun checkPassword(request: SendRequest) {
        try {
            val userId = userProvider.id()
            accountApi.checkPassword(userId, request.pin)
        } catch (ex: FeignException) {
            throw PasswordMismatchException(ex)
        }
    }

    private fun findRecipientId(phoneNumber: String): Long? {
        val accounts = accountApi.searchAccount(phoneNumber).accounts
        return if (accounts.isEmpty())
            null
        else
            accounts[0].id
    }

    private fun sanitizePhoneNumber(phoneNumber: String): String {
        val tmp = phoneNumber.trim()
        return if (tmp.startsWith("+"))
            tmp
        else
            "+$tmp"
    }

    private fun formattedPhoneNumber(phoneNumber: String): String {
        val phoneNumber = phoneNumberUtil.parse(phoneNumber, "")
        return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
    }
}
