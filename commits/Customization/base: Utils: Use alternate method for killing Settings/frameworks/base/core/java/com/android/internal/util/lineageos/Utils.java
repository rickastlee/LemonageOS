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

    public static void restartPackage(Context context, String packageName) {
        new RestartPackageTask(context, packageName).execute();
    }

    private static class RestartPackageTask extends AsyncTask<Void, Void, Boolean> {
        private Context mContext;
        private String mPackage;

        public RestartPackageTask(Context context, String packageName) {
            super();
            mContext = context;
            mPackage = packageName;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                ActivityManager am =
                        (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                IActivityManager ams = ActivityManager.getService();
                dlog("Force stopping package " + mPackage);
                ams.forceStopPackage(mPackage, UserHandle.USER_CURRENT);
            } catch (RemoteException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            PackageManager mPm = mContext.getPackageManager();
            mAppLaunchIntent = mPm.getLaunchIntentForPackage(mPackage);
            int mUserId = UserHandle.myUserId();
            if (mAppLaunchIntent != null) {
                dlog("Launching package " + mPackage);
                mContext.startActivityAsUser(mAppLaunchIntent, new UserHandle(mUserId));
            }
        }
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

    private static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
