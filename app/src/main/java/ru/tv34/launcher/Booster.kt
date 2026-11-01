package ru.tv34.launcher

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object Booster {

    /** (свободно, всего) в байтах. */
    fun ramInfo(ctx: Context): Pair<Long, Long> {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.availMem to mi.totalMem
    }

    /**
     * Чистит свой кэш и завершает фоновые процессы остальных приложений.
     * Кэш чужих приложений без root/ADB очистить нельзя — это ограничение Android.
     * Возвращает, сколько байт оперативки освободилось.
     */
    suspend fun boost(ctx: Context): Long = withContext(Dispatchers.IO) {
        val before = ramInfo(ctx).first

        ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        ctx.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }

        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val me = ctx.packageName
        ctx.packageManager.getInstalledApplications(0).forEach { info ->
            if (info.packageName != me) {
                try { am.killBackgroundProcesses(info.packageName) } catch (_: Exception) { }
            }
        }
        Runtime.getRuntime().gc()

        delay(1500)
        (ramInfo(ctx).first - before).coerceAtLeast(0L)
    }
}
