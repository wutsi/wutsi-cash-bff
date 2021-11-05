package com.wutsi.application.cash.endpoint.home

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/screens/home")
class HomeScreen(
    private val urlBuilder: URLBuilder
) : AbstractQuery() {
    @PostMapping
    fun index() = Screen(
        safe = true,
        child = Container(
            child = Text(caption = "HOME"),
        )
    ).toWidget()
}
