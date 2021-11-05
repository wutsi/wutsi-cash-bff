package com.wutsi.application.cash.endpoint

import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType.Page
import com.wutsi.flutter.sdui.enums.ActionType.Prompt
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.DialogType.Error
import com.wutsi.platform.core.logging.KVLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

abstract class AbstractEndpoint {
    @Autowired
    private lateinit var messages: MessageSource

    @Autowired
    private lateinit var logger: KVLogger

    private fun createErrorAction(e: Throwable, messageKey: String): Action {
        val action = Action(
            type = Prompt,
            prompt = Dialog(
                title = getText("prompt.error.title"),
                type = Error,
                message = getText(messageKey)
            )
        )
        log(action, e)
        return action
    }

    private fun log(action: Action, e: Throwable) {
        logger.add("action_type", action.type)
        logger.add("action_url", action.url)
        logger.add("action_prompt_type", action.prompt?.type)
        logger.add("action_prompt_message", action.prompt?.message)
        logger.add("exception", e::class.java)
        logger.add("exception_message", e.message)
    }

    protected fun getText(key: String, args: Array<Any?> = emptyArray()) =
        try {
            messages.getMessage(key, args, LocaleContextHolder.getLocale()) ?: key
        } catch (ex: Exception) {
            key
        }

    protected fun gotoPage(page: Int) = Action(
        type = Page,
        url = "page:/$page"
    )

    protected fun gotoRoute(path: String, replacement: Boolean? = null) = Action(
        type = Route,
        url = "route:$path",
        replacement = replacement
    )
}
