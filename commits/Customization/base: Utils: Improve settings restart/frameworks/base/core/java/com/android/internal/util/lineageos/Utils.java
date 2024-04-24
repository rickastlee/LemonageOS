/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.lineageos;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.R;

import java.util.List;

public class Utils {
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.sys.debug", false);
    private static final String TAG = "LineageUtils";

    private static boolean isSystemUi;
    private static boolean isSettings;
    private static Intent mAppLaunchIntent;

    private static OverlayManager mOverlayService;

    // Method to detect whether an overlay is enabled or not
    public static boolean isThemeEnabled(String packageName) {
        mOverlayService = new OverlayManager();
        try {
            List<OverlayInfo> infos = mOverlayService.getOverlayInfosForTarget("android",
                    UserHandle.myUserId());
            for (int i = 0, size = infos.size(); i < size; i++) {
                if (infos.get(i).packageName.equals(packageName)) {
                    return infos.get(i).isEnabled();
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void killProcess(Context context, String process) {
        isSystemUi = "com.android.systemui".equals(process);
        isSettings = "com.android.settings".equals(process);
        dlog("killProcess: isSystemUi = " + (isSystemUi ? "true" : "false"));
        if (isSystemUi) {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SYSUI_RESTART_COMPLETE, 0);
        }
        new KillProcessTask(context, process).execute();
    }

    public static class OverlayManager {
        private final IOverlayManager mService;

        public OverlayManager() {
            mService = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
        }

        public void setEnabled(String pkg, boolean enabled, int userId)
                throws RemoteException {
            mService.setEnabled(pkg, enabled, userId);
        }

        public List<OverlayInfo> getOverlayInfosForTarget(String target, int userId)
                throws RemoteException {
            return mService.getOverlayInfosForTarget(target, userId);
        }
    }

    private static class KillProcessTask extends AsyncTask<Void, Void, Boolean> {
        private Context mContext;
        private String mProcess;

        public KillProcessTask(Context context, String process) {
            super();
            mContext = context;
            mProcess = process;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                ActivityManager am =
                        (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                IActivityManager ams = ActivityManager.getService();
                for (ActivityManager.RunningAppProcessInfo app: am.getRunningAppProcesses()) {
                    if (mProcess.equals(app.processName)) {
                        ams.killApplicationProcess(app.processName, app.uid);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            dlog("KillProcessTask: onPostExecute: isSystemUi = "
                    + (isSystemUi ? "true" : "false"));
            if (isSystemUi) {
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.SYSUI_RESTART_COMPLETE, 1);
            } else if (isSettings) {
                PackageManager mPm = mContext.getPackageManager();
                mAppLaunchIntent = mPm.getLaunchIntentForPackage("com.android.settings");
                int mUserId = UserHandle.myUserId();
                if (mAppLaunchIntent != null) {
                    mContext.startActivityAsUser(mAppLaunchIntent, new UserHandle(mUserId));
                }
            }
        }
    }

    private static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}