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

package com.android.settings.customization.controllers;

import android.content.Context;
import android.os.SystemProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Controller that toggles GMS spoof.
 */
public final class SpoofGmsPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    @VisibleForTesting
    static final String SPOOF_GMS_SYSPROP = "persist.sys.pixelprops.spoof_gms";
    private static final String SPOOF_GMS = "spoof_gms";
    private final boolean mPropertyExists;

    public SpoofGmsPreferenceController(Context context) {
        super(context);
        mPropertyExists = SystemProperties.get(SPOOF_GMS_SYSPROP) != null;
    }

    @Override
    public String getPreferenceKey() {
        return SPOOF_GMS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(SPOOF_GMS_SYSPROP, isEnabled ? "true" : "false");
        return true;
    }

    @Override
    public boolean isAvailable() {
        return mPropertyExists;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = SystemProperties.getBoolean(SPOOF_GMS_SYSPROP, true);
        ((SwitchPreference) preference).setChecked(isEnabled);
    }
}
