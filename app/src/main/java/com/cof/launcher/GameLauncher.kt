package com.cof.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import java.io.File

/**
 * Menjalankan engine Xash3D yang ada DI DALAM APK ini.
 * Setting dari launcher dikirim lewat intent: FPS dan FPS counter lewat argumen engine,
 * resolusi / aspect / refresh rate lewat patch di XashActivity.
 * Kontrol ala PS2 (sentuh, gamepad, keyboard) dipasang oleh ControlsInstaller.
 */
object GameLauncher {
    private const val ENGINE_ACTIVITY = "su.xash.engine.XashActivity"
    private const val GAME_DIR = "cryoffear"

    fun launch(c: Context, cfg: GameConfig) {
        if (cfg.gameUri.isEmpty()) {
            toast(c, "Pilih folder game dulu")
            return
        }
        val path = uriToPath(cfg.gameUri)
        if (path == null) {
            toast(c, "Folder tidak didukung. Pilih dari penyimpanan internal atau SD card.")
            return
        }

        // Engine perlu izin "kelola semua file" untuk membaca folder game.
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            try {
                c.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${c.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                toast(c, "Aktifkan izin akses semua file, lalu tekan START lagi")
            } catch (e: Exception) {
                toast(c, "Buka Pengaturan > Aplikasi > Cry of Fear > Izin, lalu aktifkan akses semua file")
            }
            return
        }

        // Folder yang dipilih boleh 'cryoffear' itu sendiri, atau folder induknya.
        val picked = File(path)
        val baseDir = if (picked.name.equals(GAME_DIR, ignoreCase = true)) (picked.parent ?: path) else path
        if (!File(baseDir, GAME_DIR).exists()) {
            toast(c, "Folder '$GAME_DIR' tidak ditemukan di $baseDir")
            return
        }

        // Pasang kontrol ala PS2 (tombol sentuh, gamepad, keyboard) ke folder game.
        ControlsInstaller.install(c, File(baseDir, GAME_DIR))?.let {
            toast(c, "Kontrol PS2 gagal dipasang: $it")
        }

        val height = cfg.resolution.removeSuffix("p").toIntOrNull() ?: 720
        val aspect = if (cfg.aspect == "Esticar") "fill" else "fit"
        val hz = cfg.refreshHz.toIntOrNull() ?: 0

        val argv = buildString {
            append("-log +fps_override 1 +fps_max ").append(cfg.fps)
            append(" +gl_vsync 0")
            append(" +cl_showfps ").append(if (cfg.showFps) 1 else 0)
            if (cfg.fxaa) append(" +gl_msaa_samples 4")
            append(" +touch_config_file ").append(ControlsInstaller.TOUCH_PROFILE)
            append(" +exec ").append(ControlsInstaller.INPUT_CFG)
        }

        val intent = Intent()
            .setClassName(c.packageName, ENGINE_ACTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra("gamedir", GAME_DIR)
            .putExtra("gamelibdir", c.applicationInfo.nativeLibraryDir)
            .putExtra("package", c.packageName)
            .putExtra("basedir", baseDir)
            .putExtra("argv", argv)
            .putExtra("cof_height", height)
            .putExtra("cof_aspect", aspect)
            .putExtra("cof_hz", hz)

        try {
            c.startActivity(intent)
        } catch (e: Exception) {
            toast(c, "Gagal menjalankan engine: ${e.message}")
        }
    }

    /** content://...externalstorage.../tree/primary:Folder  ->  /storage/emulated/0/Folder */
    private fun uriToPath(uriStr: String): String? {
        val docId = try {
            DocumentsContract.getTreeDocumentId(Uri.parse(uriStr))
        } catch (_: Exception) { return null }
        val parts = docId.split(":", limit = 2)
        val vol = parts[0]
        val rel = parts.getOrElse(1) { "" }
        val root = if (vol == "primary") Environment.getExternalStorageDirectory().absolutePath else "/storage/$vol"
        return if (rel.isEmpty()) root else "$root/$rel"
    }

    private fun toast(c: Context, msg: String) = Toast.makeText(c, msg, Toast.LENGTH_LONG).show()
}
