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
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Controller that toggles game spoof.
 */
public final class GameSpoofPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    @VisibleForTesting
    static final String GAME_SPOOF_SYSPROP = "persist.sys.pixelprops.games";
    private static final String GAME_SPOOF = "game_spoof";
    private final boolean mPropertyExists;
    private Context mContext;

    public GameSpoofPreferenceController(Context context) {
        super(context);
        mContext = context;
        mPropertyExists = SystemProperties.get(GAME_SPOOF_SYSPROP) != null;
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public String getPreferenceKey() {
        return GAME_SPOOF;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(GAME_SPOOF_SYSPROP, isEnabled ? "true" : "false");
        return true;
    }

    @Override
    public boolean isAvailable() {
        return mPropertyExists;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = Boolean.parseBoolean(
            SystemProperties.get(GAME_SPOOF_SYSPROP, "false"));
        ((SwitchPreference) preference).setChecked(isEnabled);
    }
}