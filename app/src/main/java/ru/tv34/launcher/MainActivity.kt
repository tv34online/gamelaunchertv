package ru.tv34.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LauncherScreen(resumeTick.intValue) }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++ // обновить состояние «установлено / не установлено»
    }
}
