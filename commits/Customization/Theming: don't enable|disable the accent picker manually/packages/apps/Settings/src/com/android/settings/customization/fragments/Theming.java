/*
 * Copyright (C) 2021 ShapeShiftOS
 * Copyright (C) 2024 LineageOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.lineageos.support.colorpicker.ColorPickerPreference;
import org.lineageos.support.preferences.SecureSettingListPreference;
import org.lineageos.support.preferences.SystemSettingSwitchPreference;

@SearchIndexable
public class Theming extends DashboardFragment implements Indexable, OnPreferenceChangeListener{
    private static final boolean DEBUG = true;

    private static final String TAG = "Theming";
    private static final String DASHBOARD_ICONS_PICKER = "settings_dashboard_icons";
    private static final String CLOCK_CUSTOM_ACCENT = "lock_screen_clock_use_custom_accent_color";
    private static final String CLOCK_ACCENT_PICKER = "lock_screen_clock_custom_accent_color";
    private static final String DEFAULT_COLOR = "#ffffffff";

    public static final String[] DASHBOARD_ICONS = {
        "com.android.theme.settings_dashboard.p404"
    };

    public static final String[] SUPPORTED_CLOCKS = {
        "AndroidTwelveClock",
        "SamsungHighlightClock",
        "ShapeShiftClock"
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
        super.onCreate(savedInstanceState);
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

        updateIsCustomAccentEnabled();
        dlog("onCreate: updateIsCustomAccentEnabled");

        mCustomSettingsObserver.observe();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateIsCustomAccentEnabled();
        dlog("onResume: updateIsCustomAccentEnabled");
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        dlog("onPreferenceChange: preference = " + key);
        switch (key) {
            case DASHBOARD_ICONS_PICKER:
                int value = Integer.parseInt((String) newValue);
                dlog("case DASHBOARD_ICONS_PICKER, value = " + value);
                updateSettingsIcons(value);
                break;
            case CLOCK_CUSTOM_ACCENT:
                boolean isEnabled = (Boolean) newValue;
                dlog("case CLOCK_CUSTOM_ACCENT, isEnabled = " + (isEnabled ? "true" : "false"));
                updateAccentPickerSummary(isEnabled, false);
                break;
            case CLOCK_ACCENT_PICKER:
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                dlog("case CLOCK_ACCENT_PICKER, hex = " + hex);
                if (hex.equals(DEFAULT_COLOR)) {
                    preference.setSummary(R.string.default_string);
                } else {
                    preference.setSummary(hex);
                }
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.LOCK_SCREEN_CLOCK_CUSTOM_ACCENT_COLOR, intHex);
                break;
        }
        return true;
    }

    private void updateSettingsIcons(int selection) {
        if (selection == 0) {
            setDefaultSettingsDashboardIcons(mOverlayManager);
        } else {
            enableSettingsDashboardIcons(mOverlayManager, DASHBOARD_ICONS[selection - 1]);
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

    private void updateIsCustomAccentEnabled() {
        if (mClockCustomAccent != null) {
            dlog("updateIsCustomAccentEnabled: mClockCustomAccent != null");
            boolean supported = isCurrentClockSupported();
            dlog("updateIsCustomAccentEnabled: supported = " + (supported ? "true" : "false"));
            mClockCustomAccent.setChecked(supported);
            mClockCustomAccent.setEnabled(supported);
            mClockCustomAccent.setSummary(supported
                    ? R.string.lock_screen_clock_use_custom_accent_color_summary
                    : R.string.lock_screen_clock_use_custom_accent_color_unavailable_summary);
        }
    }

    private boolean isCurrentClockSupported() {
        String currentClock = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE);
        dlog("isCurrentClockSupported: currentClock = " + currentClock);
        if (currentClock != null) {
            for (int i = 0; i < SUPPORTED_CLOCKS.length; i++) {
                if (currentClock.contains(SUPPORTED_CLOCKS[i])) {
                    dlog("isCurrentClockSupported: currentClock contains " + SUPPORTED_CLOCKS[i]);
                    return true;
                }
            }
        }
        dlog("isCurrentClockSupported: currentClock = null, return false");
        return false;
    }

    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE))) {
                updateIsCustomAccentEnabled();
                dlog("mCustomSettingsObserver: onChange");
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CUSTOM_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.customization_theming;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.customization_theming;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
    };

    private static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}