package com.wutsi.application.cash.endpoint

import com.wutsi.application.shared.Theme
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.WidgetAware
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.Transaction

abstract class AbstractQuery : AbstractEndpoint() {
    protected fun toTransactionStatusWidget(tx: Transaction?, error: String? = null): WidgetAware {
        return if (error != null)
            toTransactionStatusWidget(
                Icon(code = Theme.ICON_ERROR, size = 40.0, color = Theme.COLOR_DANGER),
                Text(
                    error,
                    color = Theme.COLOR_DANGER,
                    alignment = TextAlignment.Center
                )
            )
        else if (tx?.status == Status.SUCCESSFUL.name)
            toTransactionStatusWidget(
                Icon(code = Theme.ICON_CHECK, size = 40.0, color = Theme.COLOR_SUCCESS),
                Text(
                    getText("widget.transaction.processing.success.${tx.type}"),
                    color = Theme.COLOR_SUCCESS,
                    alignment = TextAlignment.Center
                )
            )
        else if (tx?.status == Status.FAILED.name)
            toTransactionStatusWidget(
                Icon(code = Theme.ICON_ERROR, size = 40.0, color = Theme.COLOR_DANGER),
                Text(
                    getTransactionErrorMessage(tx.errorCode),
                    color = Theme.COLOR_DANGER,
                    alignment = TextAlignment.Center
                )
            )
        else
            toTransactionStatusWidget(
                Icon(code = Theme.ICON_PENDING, size = 40.0, color = Theme.COLOR_PRIMARY),
                Text(
                    getText("widget.transaction.processing.pending"),
                    alignment = TextAlignment.Center
                )
            )
    }

    private fun toTransactionStatusWidget(icon: Icon, text: Text): WidgetAware {
        return Column(
            children = listOf(
                icon,
                Container(
                    padding = 10.0,
                    alignment = Alignment.Center,
                    child = text
                ),
                Container(
                    padding = 10.0,
                    child = Button(
                        caption = getText("widget.transaction.processing.button.OK"),
                        action = gotoRoute("/~")
                    )
                )
            )
        )
    }
}
