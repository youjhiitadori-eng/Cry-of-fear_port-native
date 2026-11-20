package com.cof.launcher

import android.content.Context

data class GameConfig(
    val gameUri: String = "",
    val resolution: String = "720p",
    val fps: Int = 30,
    val aspect: String = "16:9",
    val fxaa: Boolean = false,
    val showFps: Boolean = false,
    val refreshHz: String = "Auto",
)

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun load(c: Context): GameConfig = sp(c).let {
        GameConfig(
            it.getString("uri", "") ?: "",
            it.getString("res", "720p") ?: "720p",
            it.getInt("fps", 30),
            it.getString("aspect", "16:9") ?: "16:9",
            it.getBoolean("fxaa", false),
            it.getBoolean("showFps", false),
            it.getString("hz", "Auto") ?: "Auto",
        )
    }

    fun save(c: Context, g: GameConfig) = sp(c).edit()
        .putString("uri", g.gameUri).putString("res", g.resolution)
        .putInt("fps", g.fps).putString("aspect", g.aspect)
        .putBoolean("fxaa", g.fxaa).putBoolean("showFps", g.showFps)
        .putString("hz", g.refreshHz).apply()
}
