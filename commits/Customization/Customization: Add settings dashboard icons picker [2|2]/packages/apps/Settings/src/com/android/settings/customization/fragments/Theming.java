/*
 * Copyright (C) 2024 Lineage OS
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

package com.android.settings.customization.fragments;

import static android.os.UserHandle.USER_SYSTEM;

import android.content.om.IOverlayManager;
import android.content.pm.PackageManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.lineageos.support.preferences.SecureSettingListPreference;

public class StatusBar extends SettingsPreferenceFragment implements 
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Theming";
    private static final String DASHBOARD_ICONS_PICKER = "settings_dashboard_icons";

    public static final String[] DASHBOARD_ICONS = {
        "com.android.theme.settings_dashboard.p404"
    };

    private Handler mHandler;
    private IOverlayManager mOverlayManager;
    private PackageManager mPackageManager;

    private SecureSettingListPreference mDashboardIcons;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.customization_theming);

        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

        mDashboardIcons = (SecureSettingListPreference) findPreference(DASHBOARD_ICONS_PICKER);
        mDashboardIcons.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDashboardIcons) {
            updateSettingsIcons();
            return true;
        }
        return false;
    }

    private void updateSettingsIcons() {
        Context mContext = getContext();
        int mSelection = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SETTINGS_DASHBOARD_ICONS, 0, UserHandle.USER_CURRENT);

        if (mSelection == 0) {
            setDefaultSettingsDashboardIcons(mOverlayManager);
        } else {
            try {
                enableSettingsDashboardIcons(mOverlayManager, DASHBOARD_ICONS[mSelection - 1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }

        }
    }

    public static void setDefaultSettingsDashboardIcons(IOverlayManager overlayManager) {
        for (int i = 0; i < DASHBOARD_ICONS.length; i++) {
            String overlayName = DASHBOARD_ICONS[i];
            try {
                overlayManager.setEnabled(overlayName, false, USER_SYSTEM);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void enableSettingsDashboardIcons(IOverlayManager overlayManager, String overlayName) {
        try {
            setDefaultSettingsDashboardIcons(overlayManager);
            overlayManager.setEnabled(overlayName, true, USER_SYSTEM);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }
}