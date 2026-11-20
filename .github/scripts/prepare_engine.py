#!/usr/bin/env python3
"""Memasukkan engine Xash3D ke dalam project launcher supaya jadi SATU APK.

Pemakaian: python3 prepare_engine.py <cof-src> <engine.apk> <app/src/main>

  1. Salin kode Java SDL + XashActivity (tanpa ACRA/crash reporter) ke app/src/main/java
  2. Tambah patch resolusi / aspect / refresh rate di XashActivity
  3. Ambil library native (arm64) dan assets dari APK engine hasil build
"""
import os
import re
import shutil
import sys
import zipfile

cof, apk, main = sys.argv[1], sys.argv[2], sys.argv[3]
engine = os.path.join(cof, "external", "xash3d-fwgs")
java_dst = os.path.join(main, "java")

DISPLAY_METHOD = r'''
	// ---- Cry of Fear launcher: resolusi / aspect / refresh rate ----
	private void applyCofDisplay() {
		try {
			Intent in = getIntent();
			int h = in.getIntExtra("cof_height", 0);
			String aspect = in.getStringExtra("cof_aspect");
			int hz = in.getIntExtra("cof_hz", 0);

			getWindow().getDecorView().setBackgroundColor(0xFF000000);

			if (hz > 0) {
				WindowManager.LayoutParams lp = getWindow().getAttributes();
				lp.preferredRefreshRate = hz;
				getWindow().setAttributes(lp);
			}

			if (h > 0 && mSurface != null) {
				android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
				getWindowManager().getDefaultDisplay().getRealMetrics(dm);
				int sw = Math.max(dm.widthPixels, dm.heightPixels);
				int sh = Math.min(dm.widthPixels, dm.heightPixels);
				boolean fit = "fit".equals(aspect);

				int bufH = Math.min(h, sh);
				float ratio = fit ? (16f / 9f) : ((float) sw / (float) sh);
				int bufW = Math.round(bufH * ratio) & ~1;

				if (fit && mSurface.getParent() instanceof android.widget.RelativeLayout) {
					int viewW = Math.min(sw, Math.round(sh * 16f / 9f));
					android.widget.RelativeLayout.LayoutParams lp =
						new android.widget.RelativeLayout.LayoutParams(viewW, sh);
					lp.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
					mSurface.setLayoutParams(lp);
				}
				mSurface.getHolder().setFixedSize(bufW, bufH);
				Log.i(TAG, "cof display: buffer " + bufW + "x" + bufH + " aspect=" + aspect + " hz=" + hz);
			}
		} catch (Throwable t) {
			Log.e(TAG, "cof display setup failed", t);
		}
	}
'''


def die(msg):
    print("GAGAL:", msg)
    sys.exit(1)


def remove_method(src, signature):
    """Hapus satu method Java lengkap (cocokkan kurung kurawal)."""
    i = src.find(signature)
    if i < 0:
        return src
    j = src.find("{", i)
    depth, k = 0, j
    while k < len(src):
        if src[k] == "{":
            depth += 1
        elif src[k] == "}":
            depth -= 1
            if depth == 0:
                break
        k += 1
    return src[:i] + src[k + 1:]


# 1) SDL java
sdl_src = os.path.join(engine, "3rdparty", "SDL", "android-project", "app", "src", "main", "java", "org", "libsdl", "app")
if not os.path.isdir(sdl_src):
    die("Source Java SDL tidak ditemukan: " + sdl_src)
sdl_dst = os.path.join(java_dst, "org", "libsdl", "app")
shutil.rmtree(sdl_dst, ignore_errors=True)
shutil.copytree(sdl_src, sdl_dst)
print("SDL java disalin ->", sdl_dst)

# 2) XashActivity + util
eng_java = os.path.join(engine, "android", "app", "src", "main", "java", "su", "xash", "engine")
xa = open(os.path.join(eng_java, "XashActivity.java"), encoding="utf-8").read()

xa = xa.replace("import su.xash.engine.util.CrashReports;\n", "")
xa = remove_method(xa, "private void recordLaunchInfo()")
xa = xa.replace("recordLaunchInfo();", "")
if "CrashReports" in xa:
    die("Masih ada referensi CrashReports di XashActivity")

pat = re.compile(r"(protected void onCreate\(Bundle savedInstanceState\) \{\s*super\.onCreate\(savedInstanceState\);)")
if not pat.search(xa):
    die("onCreate di XashActivity tidak ditemukan")
xa = pat.sub(r"\1\n\t\tapplyCofDisplay();", xa, count=1)

marker = "\t@Override\n\tprotected String[] getLibraries()"
if marker not in xa:
    die("getLibraries di XashActivity tidak ditemukan")
xa = xa.replace(marker, DISPLAY_METHOD + "\n" + marker, 1)

out_dir = os.path.join(java_dst, "su", "xash", "engine")
os.makedirs(os.path.join(out_dir, "util"), exist_ok=True)
open(os.path.join(out_dir, "XashActivity.java"), "w", encoding="utf-8").write(xa)
shutil.copy(os.path.join(eng_java, "util", "AndroidBug5497Workaround.java"),
            os.path.join(out_dir, "util", "AndroidBug5497Workaround.java"))
print("XashActivity ditulis ->", out_dir)

# 3) native libs + assets dari APK engine
libs_dst = os.path.join(main, "jniLibs", "arm64-v8a")
assets_dst = os.path.join(main, "assets")
shutil.rmtree(libs_dst, ignore_errors=True)
os.makedirs(libs_dst, exist_ok=True)
n_libs = 0
with zipfile.ZipFile(apk) as z:
    for name in z.namelist():
        if name.startswith("lib/arm64-v8a/") and name.endswith(".so"):
            with z.open(name) as s, open(os.path.join(libs_dst, os.path.basename(name)), "wb") as d:
                shutil.copyfileobj(s, d)
            n_libs += 1
        elif name.startswith("assets/") and not name.endswith("/"):
            target = os.path.join(main, name)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with z.open(name) as s, open(target, "wb") as d:
                shutil.copyfileobj(s, d)
have = set(os.listdir(libs_dst))
for need in ("libxash.so", "libSDL2.so"):
    if need not in have:
        die(need + " tidak ada di APK engine. Isi: " + ", ".join(sorted(have)))
print("%d library native disalin ->" % n_libs, libs_dst)
