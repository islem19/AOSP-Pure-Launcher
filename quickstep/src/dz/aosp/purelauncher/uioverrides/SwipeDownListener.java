package dz.aosp.purelauncher.uioverrides;

import static dz.aosp.purelauncher.LauncherState.NORMAL;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import dz.aosp.purelauncher.Launcher;
import dz.aosp.purelauncher.Utilities;
import dz.aosp.purelauncher.util.TouchController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SwipeDownListener implements TouchController {
    private static final String PREF_STATUSBAR_EXPAND = "pref_expand_statusbar";

    private GestureDetector mGestureDetector;
    private Launcher mLauncher;

    public SwipeDownListener(Launcher launcher) {
        SharedPreferences prefs = Utilities.getPrefs(launcher.getApplicationContext());

        mLauncher = launcher;
        mGestureDetector = new GestureDetector(launcher,
        new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vy) {
                if (prefs.getBoolean(PREF_STATUSBAR_EXPAND, true) && e1.getY() < e2.getY()) {
                    expandStatusBar(launcher);
                }
                return true;
            }
        });
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (mLauncher.isInState(NORMAL)) {
            mGestureDetector.onTouchEvent(ev);
        }
        return false;
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        return false;
    }

    private void expandStatusBar(Context context) {
        try {
            Object service = context.getSystemService("statusbar");
            Class<?> manager = Class.forName("android.app.StatusBarManager");
            Method expand = manager.getMethod("expandNotificationsPanel");
            expand.invoke(service);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                InvocationTargetException e) {
            Log.w("Reflection",
                    "Can't to invoke android.app.StatusBarManager$expandNotificationsPanel");
        }
    }
}