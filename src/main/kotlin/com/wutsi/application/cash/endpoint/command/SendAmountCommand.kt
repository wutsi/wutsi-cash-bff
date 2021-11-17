package com.wutsi.application.cash.endpoint.command

import com.wutsi.application.cash.dto.SendAmountRequest
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.platform.core.logging.KVLogger
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/commands/send/amount")
class SendAmountCommand(
    private val logger: KVLogger,
    private val urlBuilder: URLBuilder,
) : AbstractCommand() {
    @PostMapping
    fun index(@RequestBody @Valid request: SendAmountRequest): Action {
        logger.add("amount", request.amount)
        return Action(
            type = Route,
            url = urlBuilder.build("send/recipient?amount=${request.amount}")
        )
    }
}
