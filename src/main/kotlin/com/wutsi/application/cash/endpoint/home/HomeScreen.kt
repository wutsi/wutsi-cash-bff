package com.wutsi.application.cash.endpoint.home

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.core.security.WutsiPrincipal
import com.wutsi.platform.core.security.spring.jwt.JWTAuthentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/screens/home")
class HomeScreen(
    private val urlBuilder: URLBuilder,
    private val accountApi: WutsiAccountApi,
) : AbstractQuery() {
    @PostMapping
    fun index(): Widget {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth is JWTAuthentication) {
            val principal = auth.principal
            if (principal is WutsiPrincipal) {
                val userId = principal.id
                val paymentMethods = accountApi.listPaymentMethods(userId.toLong());
            }
        }
        return Screen(
            safe = true,
            child = Container(
                child = Text(caption = "HOME"),
            )
        ).toWidget()
    }
}
