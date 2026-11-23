package com.dynamicpill.module;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class PillController {
    static final String MODULE_PKG = "com.dynamicpill.module";
    static final String PREF_FILE = "pill_prefs";
    static final String K_ENABLED = "enabled";
    static final String K_WIDTH = "width_mm10";
    static final String K_HEIGHT = "height_mm10";
    static final String K_YOFF = "yoff_mm10";
    static final int D_WIDTH = 135;
    static final int D_HEIGHT = 40;
    static final int D_YOFF = 0;

    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final WindowManager wm;
    private XSharedPreferences xsp;
    private PillView view;
    private boolean added = false;
    private int lastW = -1, lastH = -1, lastY = Integer.MIN_VALUE;
    private boolean lastVisible = true;

    public PillController(Context ctx) {
        this.ctx = ctx;
        this.wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
    }

    public void start() {
        try {
            xsp = new XSharedPreferences(MODULE_PKG, PREF_FILE);
            xsp.makeWorldReadable();
        } catch (Throwable t) {
            XposedBridge.log("DynamicPill: prefs init: " + t);
        }
        handler.postDelayed(tick, 4000);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                update();
            } catch (Throwable t) {
                XposedBridge.log("DynamicPill: update failed: " + t);
            }
            handler.postDelayed(this, 1500);
        }
    };

    private int pref(String key, int def) {
        try {
            if (xsp != null) return xsp.getInt(key, def);
        } catch (Throwable ignored) { }
        return def;
    }

    private boolean prefBool(String key, boolean def) {
        try {
            if (xsp != null) return xsp.getBoolean(key, def);
        } catch (Throwable ignored) { }
        return def;
    }

    private int mm(float mm10) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm10 / 10f, dm));
    }

    private int cutoutCenterY() {
        try {
            DisplayCutout dc = wm.getCurrentWindowMetrics().getWindowInsets().getDisplayCutout();
            if (dc != null) {
                Rect r = dc.getBoundingRectTop();
                if (r != null && !r.isEmpty()) return (r.top + r.bottom) / 2;
            }
        } catch (Throwable ignored) { }
        try {
            int id = ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (id > 0) return ctx.getResources().getDimensionPixelSize(id) / 2;
        } catch (Throwable ignored) { }
        return mm(30);
    }

    private void update() {
        if (xsp != null && xsp.hasFileChanged()) xsp.reload();

        boolean enabled = prefBool(K_ENABLED, true);
        boolean portrait = ctx.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
        boolean visible = enabled && portrait;

        int w = mm(pref(K_WIDTH, D_WIDTH));
        int h = mm(pref(K_HEIGHT, D_HEIGHT));
        int y = cutoutCenterY() - h / 2 + mm(pref(K_YOFF, D_YOFF));

        if (!added) {
            view = new PillView(ctx);
            WindowManager.LayoutParams lp = params(w, h, y);
            wm.addView(view, lp);
            added = true;
            lastW = w; lastH = h; lastY = y;
        } else if (w != lastW || h != lastH || y != lastY) {
            wm.updateViewLayout(view, params(w, h, y));
            lastW = w; lastH = h; lastY = y;
        }

        if (visible != lastVisible) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
            lastVisible = visible;
        }
    }

    private WindowManager.LayoutParams params(int w, int h, int y) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                w, h,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.x = 0;
        lp.y = y;
        lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        lp.setTitle("DynamicPill");
        return lp;
    }
}
