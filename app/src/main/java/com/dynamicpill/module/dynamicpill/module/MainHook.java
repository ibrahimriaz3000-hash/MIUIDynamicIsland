package com.dynamicpill.module;

import android.app.Application;
import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainHook implements IXposedHookLoadPackage {
    private static boolean started = false;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) return;
        try {
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Context ctx = (Context) param.thisObject;
                        if (!"com.android.systemui".equals(ctx.getPackageName())) return;
                        if (started) return;
                        started = true;
                        new PillController(ctx).start();
                    } catch (Throwable t) {
                        XposedBridge.log("DynamicPill: start failed: " + t);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("DynamicPill: hook failed: " + t);
        }
    }
}
