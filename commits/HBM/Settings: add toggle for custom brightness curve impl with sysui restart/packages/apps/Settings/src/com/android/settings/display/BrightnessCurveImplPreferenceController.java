/*
 * Copyright 2020 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.os.SystemProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Controller that toggles custom brightness curve impl.
 */
public final class BrightnessCurveImplPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    static final String LOW_GAMMA_SYSPROP = "persist.sys.brightness.low.gamma";
    private static final String CUSTOM_BRIGHTNESS_CURVE_IMPL = "custom_brightness_curve_impl";
    private static final String TAG = "BrightnessCurveImpl";
    private Context mContext;

    public BrightnessCurveImplPreferenceController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getPreferenceKey() {
        return CUSTOM_BRIGHTNESS_CURVE_IMPL;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(LOW_GAMMA_SYSPROP, isEnabled ? "true" : "false");
        org.lineageos.settings.utils.Utils.killProcess(mContext, "com.android.systemui");
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = Boolean.parseBoolean(
            SystemProperties.get(LOW_GAMMA_SYSPROP, "false"));
        ((SwitchPreference) preference).setChecked(isEnabled);
    }
}