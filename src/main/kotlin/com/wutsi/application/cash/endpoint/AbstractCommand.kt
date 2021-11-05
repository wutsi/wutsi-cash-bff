package com.wutsi.application.cash.endpoint

import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType.Prompt
import com.wutsi.flutter.sdui.enums.DialogType

abstract class AbstractCommand : AbstractEndpoint() {
    protected fun promptError(errorKey: String) = Action(
        type = Prompt,
        prompt = Dialog(
            title = getText("prompt.error.title"),
            type = DialogType.Error,
            message = getText(errorKey)
        )
    )

    protected fun promptInformation(errorKey: String) = Action(
        type = Prompt,
        prompt = Dialog(
            title = getText("prompt.information.title"),
            type = DialogType.Information,
            message = getText(errorKey)
        )
    )
}
