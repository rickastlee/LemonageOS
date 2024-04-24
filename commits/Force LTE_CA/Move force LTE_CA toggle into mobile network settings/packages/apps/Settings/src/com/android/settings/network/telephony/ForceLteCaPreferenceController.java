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

package com.android.settings.network.telephony;

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
 * Controller that toggles force enable LTE_CA.
 */
public final class ForceLteCaPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    @VisibleForTesting
    static final String FORCE_LTE_CA_SYSPROP = "persist.sys.radio.force_lte_ca";
    private static final String FORCE_LTE_CA = "force_lte_ca";
    private final boolean mPropertyExists;
    private Context mContext;

    public ForceLteCaPreferenceController(Context context) {
        super(context);
        mContext = context;
        mPropertyExists = SystemProperties.get(FORCE_LTE_CA_SYSPROP) != null;
    }

    @Override
    public String getPreferenceKey() {
        return FORCE_LTE_CA;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        showRebootDialog(isEnabled);
        return false;
    }

    private void showRebootDialog(boolean isEnabled) {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.reboot_dialog_title)
                .setMessage(R.string.reboot_dialog_message)
                .setPositiveButton(R.string.reboot_dialog_confirm, (dialog, which) -> {
                    SystemProperties.set(FORCE_LTE_CA_SYSPROP, isEnabled ? "true" : "false");
                    PowerManager pm = mContext.getSystemService(PowerManager.class);
                    pm.reboot(null);
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public boolean isAvailable() {
        return mPropertyExists;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = Boolean.parseBoolean(
                SystemProperties.get(FORCE_LTE_CA_SYSPROP, "true"));
        ((SwitchPreference) preference).setChecked(isEnabled);
    }
}