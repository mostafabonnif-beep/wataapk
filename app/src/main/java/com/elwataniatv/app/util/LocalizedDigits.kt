package com.elwataniatv.app.util

fun normalizeDigits(value: String): String = buildString(value.length) {
    value.forEach { character ->
        append(
            when (character) {
                in '٠'..'٩' -> '0' + (character - '٠')
                in '۰'..'۹' -> '0' + (character - '۰')
                else -> character
            }
        )
    }
}
