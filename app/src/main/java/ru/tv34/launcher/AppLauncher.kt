package ru.tv34.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object AppLauncher {

    fun isInstalled(ctx: Context, pkg: String): Boolean =
        try { ctx.packageManager.getPackageInfo(pkg, 0); true } catch (e: Exception) { false }

    /** Первый установленный пакет из списка или null. */
    fun resolve(ctx: Context, app: TvApp): String? = app.packages.firstOrNull { isInstalled(ctx, it) }

    /** Открытие по имени пакета; если приложения нет — предлагаем скачать. */
    fun open(ctx: Context, app: TvApp) {
        val pkg = resolve(ctx, app)
        if (pkg != null) {
            val pm = ctx.packageManager
            val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { ctx.startActivity(intent); return } catch (_: Exception) { }
            }
        }
        offerDownload(ctx, app)
    }

    private fun offerDownload(ctx: Context, app: TvApp) {
        val pkg = app.packages.first()
        val links = if (app.installUrl != null) {
            listOf(app.installUrl)
        } else {
            listOf("market://details?id=$pkg", "https://play.google.com/store/apps/details?id=$pkg")
        }
        for (link in links) {
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) { }
        }
        Toast.makeText(ctx, "Не удалось открыть магазин для «${app.title}»", Toast.LENGTH_LONG).show()
    }
}
