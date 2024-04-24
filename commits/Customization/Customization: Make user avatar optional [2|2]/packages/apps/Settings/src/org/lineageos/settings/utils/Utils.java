/**
 * Copyright (C) 2016 The Pure Nexus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.lineageos.settings.utils;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

public final class Utils {
    private static final boolean DEBUG = true;
    private static final String TAG = "SettingsUtils";

    private static boolean isSystemUi;
    private static boolean isSettings;
    private static Intent mAppLaunchIntent;
    

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

    public static boolean deviceSupportsFlashLight(Context context) {
        CameraManager cameraManager = context.getSystemService(CameraManager.class);
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null
                        && flashAvailable
                        && lensFacing != null
                        && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return true;
                }
            }
        } catch (CameraAccessException | AssertionError e) {
            // Ignore
        }
        return false;
    }

    private static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
