package ru.tv34.launcher

import androidx.compose.ui.graphics.Color

/** Фиксированный список приложений на главном экране. Менять их из интерфейса нельзя. */
data class TvApp(
    val title: String,
    /** Пакеты-кандидаты: запускается первый установленный. Скачивание предлагается по первому. */
    val packages: List<String>,
    val top: Color,
    val bottom: Color,
    /** Если задано — вместо Google Play открывается эта ссылка (для приложений, которых нет в Play). */
    val installUrl: String? = null,
)

object Apps {
    val list = listOf(
        TvApp("Minecraft", listOf("com.mojang.minecraftpe"), Color(0xFF6BBF45), Color(0xFF2F6B1F)),
        TvApp("Geometry Dash Lite", listOf("com.robtopx.geometryjumplite"), Color(0xFF3D7BFF), Color(0xFF0B2A8C)),
        // BB Racing = Beach Buggy Racing (Vector Unit). Для BB Racing 2 замени на com.vectorunit.cobalt.googleplay
        TvApp("BB Racing", listOf("com.vectorunit.purple.googleplay"), Color(0xFFFF9A3D), Color(0xFFB8400A)),
        TvApp("ТВ 34", listOf("ru.tv34.channels"), Color(0xFF8F5CFF), Color(0xFF2E1A8C)),
        // Точный пакет RuStore для ТВ не подтверждён — проверяются оба варианта.
        // Узнать свой: adb shell pm list packages | grep -i rustore
        TvApp(
            "RuStore", listOf("ru.vk.store.tv", "ru.vk.store"),
            Color(0xFF2E94FF), Color(0xFF0E4A9E),
            installUrl = "https://www.rustore.ru/tv",
        ),
    )
}
