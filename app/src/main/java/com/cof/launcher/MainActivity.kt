package com.cof.launcher

import android.content.Context
import java.io.File
import java.util.Locale

/**
 * Memasang kontrol ala PS2 ke folder game sebelum engine dijalankan:
 *  - tekstur tombol  -> folder <gamedir>/touch_ps2 (file png)
 *  - layout sentuh   -> <gamedir>/touch_ps2.cfg   (dipakai lewat +touch_config_file)
 *  - bind gamepad + keyboard -> <gamedir>/cof_input.cfg   (dipakai lewat +exec)
 *
 * File cfg hanya ditimpa kalau baris pertamanya masih penanda buatan launcher.
 * Kalau kamu geser tombol lewat editor engine, engine menulis ulang file itu tanpa
 * penanda, jadi susunan kamu tidak ditimpa lagi. Mau reset: hapus touch_ps2.cfg.
 */
object ControlsInstaller {
    private const val MARKER = "// cof-launcher-generated"
    const val TOUCH_PROFILE = "touch_ps2.cfg"
    const val INPUT_CFG = "cof_input.cfg"
    private const val TEX_DIR = "touch_ps2"

    private val textures = listOf(
        "ps2_cross" to R.raw.ps2_cross,
        "ps2_circle" to R.raw.ps2_circle,
        "ps2_square" to R.raw.ps2_square,
        "ps2_triangle" to R.raw.ps2_triangle,
        "ps2_l1" to R.raw.ps2_l1,
        "ps2_l2" to R.raw.ps2_l2,
        "ps2_r1" to R.raw.ps2_r1,
        "ps2_r2" to R.raw.ps2_r2,
        "ps2_start" to R.raw.ps2_start,
        "ps2_select" to R.raw.ps2_select,
        "ps2_dpad_up" to R.raw.ps2_dpad_up,
        "ps2_dpad_down" to R.raw.ps2_dpad_down,
        "ps2_dpad_left" to R.raw.ps2_dpad_left,
        "ps2_dpad_right" to R.raw.ps2_dpad_right,
        "ps2_stick" to R.raw.ps2_stick,
    )

    /** Mengembalikan null kalau sukses, atau pesan error. */
    fun install(c: Context, gameDir: File): String? = try {
        installTextures(c, File(gameDir, TEX_DIR))
        writeIfOurs(File(gameDir, TOUCH_PROFILE), touchConfig())
        writeIfOurs(File(gameDir, INPUT_CFG), inputConfig())
        null
    } catch (e: Exception) {
        e.message ?: e.javaClass.simpleName
    }

    private fun installTextures(c: Context, dir: File) {
        dir.mkdirs()
        for ((name, res) in textures) {
            val out = File(dir, "$name.png")
            c.resources.openRawResource(res).use { input ->
                val bytes = input.readBytes()
                if (!out.exists() || out.length() != bytes.size.toLong()) out.writeBytes(bytes)
            }
        }
    }

    private fun writeIfOurs(f: File, text: String) {
        if (f.exists()) {
            val first = f.bufferedReader().use { it.readLine() } ?: ""
            if (!first.startsWith(MARKER)) return // sudah diubah lewat editor engine
        }
        f.writeText(text)
    }

    // ---------------------------------------------------------------- touch layout

    // Tinggi tombol dihitung engine dari lebar + rasio (aspect = tinggi/lebar tekstur),
    // jadi y2 di sini hanya perkiraan untuk layar ~20:9.
    private const val SCREEN_AR = 0.45f

    private class Btn(
        val name: String, val tex: String, val cmd: String,
        val x1: Float, val y1: Float, val w: Float, val aspect: Float,
    )

    private fun f(v: Float) = String.format(Locale.US, "%.4f", v)

    private fun line(b: Btn): String {
        val x2 = b.x1 + b.w
        val y2 = b.y1 + b.w / SCREEN_AR * b.aspect
        return "touch_addbutton \"${b.name}\" \"${b.tex}\" \"${b.cmd}\" " +
            "${f(b.x1)} ${f(b.y1)} ${f(x2)} ${f(y2)} 255 255 255 255 0 ${f(b.aspect)}"
    }

    private fun touchConfig(): String {
        val sq = 1f
        val pill = 0.5f
        val small = 0.375f
        val dp = 0.055f      // lebar tombol D-pad
        val dh = dp / SCREEN_AR
        val fw = 0.075f      // lebar tombol ✕ ○ □ △
        val fh = fw / SCREEN_AR
        val dcx = 0.115f; val dcy = 0.355f
        val fcx = 0.860f; val fcy = 0.600f

        val buttons = listOf(
            // stik analog kiri (gerak)
            Btn("stick", "$TEX_DIR/ps2_stick", "_joy", 0.050f, 0.620f, 0.140f, sq),
            // D-pad
            Btn("dpad_up", "$TEX_DIR/ps2_dpad_up", "impulse 100", dcx - dp / 2, dcy - dh * 1.5f, dp, sq),
            Btn("dpad_down", "$TEX_DIR/ps2_dpad_down", "lastinv", dcx - dp / 2, dcy + dh * 0.5f, dp, sq),
            Btn("dpad_left", "$TEX_DIR/ps2_dpad_left", "invprev", dcx - dp * 1.5f, dcy - dh * 0.5f, dp, sq),
            Btn("dpad_right", "$TEX_DIR/ps2_dpad_right", "invnext", dcx + dp * 0.5f, dcy - dh * 0.5f, dp, sq),
            // tombol muka
            Btn("btn_triangle", "$TEX_DIR/ps2_triangle", "+use", fcx - fw / 2, fcy - fh, fw, sq),
            Btn("btn_square", "$TEX_DIR/ps2_square", "+reload", fcx - fw * 1.5f, fcy, fw, sq),
            Btn("btn_circle", "$TEX_DIR/ps2_circle", "+duck", fcx + fw / 2, fcy, fw, sq),
            Btn("btn_cross", "$TEX_DIR/ps2_cross", "+jump", fcx - fw / 2, fcy + fh, fw, sq),
            // bahu
            Btn("l2", "$TEX_DIR/ps2_l2", "+attack2", 0.025f, 0.020f, 0.110f, pill),
            Btn("l1", "$TEX_DIR/ps2_l1", "+speed", 0.145f, 0.020f, 0.110f, pill),
            Btn("r1", "$TEX_DIR/ps2_r1", "cof_inventory_toggle", 0.745f, 0.020f, 0.110f, pill),
            Btn("r2", "$TEX_DIR/ps2_r2", "+attack", 0.865f, 0.020f, 0.110f, pill),
            // select / start
            Btn("select", "$TEX_DIR/ps2_select", "pause", 0.330f, 0.020f, 0.100f, small),
            Btn("start", "$TEX_DIR/ps2_start", "cancelselect", 0.570f, 0.020f, 0.100f, small),
            // editor tata letak bawaan engine
            Btn("edit", "touch_default/settings", "touch_enableedit", 0.470f, 0.020f, 0.060f, sq),
        )

        return buildString {
            appendLine("$MARKER v1 (hapus baris ini kalau mau mengedit manual)")
            appendLine("touch_setclientonly 0")
            appendLine("touch_removeall")
            // area lihat ditaruh paling bawah supaya tombol di atasnya tetap menang
            appendLine("touch_addbutton \"look\" \"\" \"_look\" 0.4000 0.0000 1.0000 1.0000 255 255 255 255 0")
            buttons.forEach { appendLine(line(it)) }
        }
    }

    // ---------------------------------------------------------------- gamepad + keyboard

    private fun inputConfig(): String = """
$MARKER v1
// Gamepad: urutan ala PS2 (x = bawah, o = kanan, kotak = kiri, segitiga = atas)
joy_enable 1
bind "A_BUTTON" "+jump"
bind "B_BUTTON" "+duck"
bind "X_BUTTON" "+reload"
bind "Y_BUTTON" "+use"
bind "L1_BUTTON" "+speed"
bind "R1_BUTTON" "cof_inventory_toggle"
bind "L2_BUTTON" "+attack2"
bind "R2_BUTTON" "+attack"
bind "LTRIGGER" "+attack2"
bind "RTRIGGER" "+attack"
bind "DPAD_UP" "impulse 100"
bind "DPAD_DOWN" "lastinv"
bind "DPAD_LEFT" "invprev"
bind "DPAD_RIGHT" "invnext"
bind "START" "cancelselect"
bind "BACK" "pause"
bind "STICK1" "+speed"
bind "STICK2" "+duck"

// Keyboard + mouse
bind "w" "+forward"
bind "s" "+back"
bind "a" "+moveleft"
bind "d" "+moveright"
bind "SPACE" "+jump"
bind "CTRL" "+duck"
bind "SHIFT" "+speed"
bind "e" "+use"
bind "r" "+reload"
bind "f" "impulse 100"
bind "q" "lastinv"
bind "i" "cof_inventory_toggle"
bind "TAB" "cof_inventory_toggle"
bind "MOUSE1" "+attack"
bind "MOUSE2" "+attack2"
bind "MWHEELUP" "invprev"
bind "MWHEELDOWN" "invnext"
bind "1" "slot1"
bind "2" "slot2"
bind "3" "slot3"
bind "4" "slot4"
bind "5" "slot5"
bind "6" "slot6"
bind "7" "slot7"
bind "8" "slot8"
bind "9" "slot9"
""".trimStart()
}
