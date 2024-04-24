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

import android.app.AlertDialog;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Controller that toggles window blurs on SurfaceFlinger on devices that support it.
 */
public final class EnableBlursPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    @VisibleForTesting
    static final String DISABLE_BLURS_SYSPROP = "persist.sys.sf.disable_blurs";
    private static final String ENABLE_BLURS_ON_WINDOWS = "enable_blurs_on_windows";
    private final boolean mBlurSupported;
    private Context mContext;

    public EnableBlursPreferenceController(Context context) {
        this(context, SystemProperties
                .getBoolean("ro.surface_flinger.supports_background_blur", false));
    }

    @VisibleForTesting
    public EnableBlursPreferenceController(Context context, boolean blurSupported) {
        super(context);
        mContext = context;
        mBlurSupported = blurSupported;
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_BLURS_ON_WINDOWS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isDisabled = !(Boolean) newValue;
        showRebootDialog(isDisabled);
        return false;
    }

    private void showRebootDialog(boolean isDisabled) {
        String message = mContext.getString(R.string.reboot_dialog_message,
                (isDisabled ? "disable" : "enable") + " blurs");

        new AlertDialog.Builder(mContext)
                .setTitle(R.string.reboot_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.reboot_dialog_confirm, (dialog, which) -> {
                    SystemProperties.set(DISABLE_BLURS_SYSPROP, isDisabled ? "1" : "0");
                    PowerManager pm = mContext.getSystemService(PowerManager.class);
                    pm.reboot(null);
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public boolean isAvailable() {
        return mBlurSupported;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = !SystemProperties.getBoolean(
                DISABLE_BLURS_SYSPROP, false /* default */);
        ((SwitchPreference) preference).setChecked(isEnabled);
    }
}