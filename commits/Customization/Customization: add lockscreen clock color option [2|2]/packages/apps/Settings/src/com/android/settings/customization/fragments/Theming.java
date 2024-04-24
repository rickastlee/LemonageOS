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
import android.util.Log;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.lineageos.support.colorpicker.ColorPickerPreference;
import org.lineageos.support.preferences.SecureSettingListPreference;
import org.lineageos.support.preferences.SystemSettingSwitchPreference;

public class Theming extends SettingsPreferenceFragment implements 
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Theming";
    private static final boolean DEBUG = true;

    private static final String DASHBOARD_ICONS_PICKER = "settings_dashboard_icons";
    private static final String CLOCK_CUSTOM_ACCENT = "lock_screen_clock_use_custom_accent_color";
    private static final String CLOCK_ACCENT_PICKER = "lock_screen_clock_custom_accent_color";

    private static final String DEFAULT_COLOR = "#ffffffff";

    public static final String[] DASHBOARD_ICONS = {
        "com.android.theme.settings_dashboard.p404"
    };

    private Context mContext;
    private Handler mHandler;
    private IOverlayManager mOverlayManager;
    private PackageManager mPackageManager;

    private SecureSettingListPreference mDashboardIcons;
    private SystemSettingSwitchPreference mClockCustomAccent;
    private ColorPickerPreference mClockAccentPicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        dlog("onCreate");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.customization_theming);

        mContext = getContext();
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

        mDashboardIcons = (SecureSettingListPreference) findPreference(DASHBOARD_ICONS_PICKER);
        mClockCustomAccent = (SystemSettingSwitchPreference) findPreference(CLOCK_CUSTOM_ACCENT);
        mClockAccentPicker = (ColorPickerPreference) findPreference(CLOCK_ACCENT_PICKER);

        mDashboardIcons.setOnPreferenceChangeListener(this);
        mClockCustomAccent.setOnPreferenceChangeListener(this);

        boolean mUseCustomAccent = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_SCREEN_CLOCK_USE_CUSTOM_ACCENT_COLOR, 0) != 0;
        updateAccentPickerSummary(mUseCustomAccent, true);
        mClockAccentPicker.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        dlog("onPreferenceChange: preference = " + key);
        switch (key) {
            case DASHBOARD_ICONS_PICKER:
                updateSettingsIcons();
                break;
            case CLOCK_CUSTOM_ACCENT:
                boolean isEnabled = (Boolean) newValue;
                updateAccentPickerSummary(isEnabled, false);
                break;
            case CLOCK_ACCENT_PICKER:
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                if (hex.equals(DEFAULT_COLOR)) {
                    preference.setSummary(R.string.default_string);
                } else {
                    preference.setSummary(hex);
                }
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.LOCK_SCREEN_CLOCK_CUSTOM_ACCENT_COLOR, intHex);
                break;
            default:
                return false;
        }
        return true;
    }

    private void updateAccentPickerSummary(boolean isEnabled, boolean init) {
        dlog("updateAccentPickerSummary: isEnabled = " + (isEnabled ? "true" : "false")
                + ", init = " + (init ? "true" : "false"));
        int mClockAccent = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_SCREEN_CLOCK_CUSTOM_ACCENT_COLOR, 0xFFFFFFFF);
        if (init) mClockAccentPicker.setNewPreviewColor(mClockAccent);

        if (isEnabled) {
            String clockAccentHex = String.format("#%08x", (0xFFFFFFFF & mClockAccent));
            if (clockAccentHex.equals(DEFAULT_COLOR)) {
                mClockAccentPicker.setSummary(R.string.default_string);
            } else {
                mClockAccentPicker.setSummary(clockAccentHex);
            }
        } else {
            mClockAccentPicker.setSummary(R.string.disabled_string);
        }
    }

    private void updateSettingsIcons() {
        int mSelection = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SETTINGS_DASHBOARD_ICONS, 0, UserHandle.USER_CURRENT);
        dlog("updateSettingsIcons: mSelection = " + mSelection);

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
        dlog("setDefaultSettingsDashboardIcons");
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
        dlog("enableSettingsDashboardIcons, overlayName = " + overlayName);
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

    private static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}