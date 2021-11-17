package com.wutsi.application.cash.endpoint.command

import com.wutsi.application.cash.dto.SendRecipientRequest
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.platform.core.logging.KVLogger
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/commands/send/recipient")
class SendRecipientCommand(
    private val logger: KVLogger,
    private val urlBuilder: URLBuilder,
) : AbstractCommand() {
    @PostMapping
    fun index(
        @RequestParam amount: Double,
        @RequestBody @Valid request: SendRecipientRequest
    ): Action {
        logger.add("phone_number", request.phoneNumber)
        return Action(
            type = Route,
            url = urlBuilder.build("send/pin?amount=$amount&phone-number=${request.phoneNumber}")
        )
    }
}
