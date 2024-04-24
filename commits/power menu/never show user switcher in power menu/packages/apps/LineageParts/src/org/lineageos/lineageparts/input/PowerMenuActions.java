/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
 *               2017-2022 The LineageOS Project
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

package org.lineageos.lineageparts.input;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;

import org.lineageos.internal.util.PowerMenuConstants;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.utils.TelephonyUtils;

import java.util.ArrayList;
import java.util.List;

import lineageos.app.LineageGlobalActions;
import lineageos.providers.LineageSettings;

import static org.lineageos.internal.util.PowerMenuConstants.*;

public class PowerMenuActions extends SettingsPreferenceFragment {
    final static String TAG = "PowerMenuActions";

    private CheckBoxPreference mAirplanePref;
    private CheckBoxPreference mBugReportPref;
    private CheckBoxPreference mLockDownPref;
    private CheckBoxPreference mEmergencyPref;

    Context mContext;
    private LockPatternUtils mLockPatternUtils;
    private UserManager mUserManager;
    private List<String> mLocalUserConfig = new ArrayList<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.power_menu_settings);
        mContext = getActivity().getApplicationContext();
        mLockPatternUtils = new LockPatternUtils(mContext);
        mUserManager = UserManager.get(mContext);

        for (String action : PowerMenuConstants.getAllActions()) {
            if (action.equals(GLOBAL_ACTION_KEY_AIRPLANE)) {
                mAirplanePref = findPreference(GLOBAL_ACTION_KEY_AIRPLANE);
            } else if (action.equals(GLOBAL_ACTION_KEY_BUGREPORT)) {
                mBugReportPref = findPreference(GLOBAL_ACTION_KEY_BUGREPORT);
            } else if (action.equals(GLOBAL_ACTION_KEY_LOCKDOWN)) {
                mLockDownPref = findPreference(GLOBAL_ACTION_KEY_LOCKDOWN);
            } else if (action.equals(GLOBAL_ACTION_KEY_EMERGENCY)) {
                mEmergencyPref = findPreference(GLOBAL_ACTION_KEY_EMERGENCY);
            }
        }

        if (!TelephonyUtils.isVoiceCapable(getActivity())) {
            getPreferenceScreen().removePreference(mEmergencyPref);
            mEmergencyPref = null;
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        if (mAirplanePref != null) {
            boolean airplaneModeEnabled = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.AIRPLANE_MODE_IN_POWER_MENU, 0, UserHandle.USER_CURRENT) == 1;
            mAirplanePref.setChecked(airplaneModeEnabled);
        }

        if (mBugReportPref != null) {
            boolean bugReportEnabled = Settings.Global.getInt(
                    getContentResolver(), Settings.Global.BUGREPORT_IN_POWER_MENU, 0) == 1;
            mBugReportPref.setChecked(bugReportEnabled);
        }

        if (mEmergencyPref != null) {
            boolean emergencyEnabled = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.EMERGENCY_IN_POWER_MENU, 0, UserHandle.USER_CURRENT) == 1;
            mEmergencyPref.setChecked(emergencyEnabled);
        }

        updatePreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        boolean value;

        if (preference == mAirplanePref) {
            value = mAirplanePref.isChecked();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.AIRPLANE_MODE_IN_POWER_MENU, value ? 1 : 0, UserHandle.USER_CURRENT);

        } else if (preference == mBugReportPref) {
            value = mBugReportPref.isChecked();
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.BUGREPORT_IN_POWER_MENU, value ? 1 : 0);

        } else if (preference == mLockDownPref) {
            value = mLockDownPref.isChecked();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.LOCKDOWN_IN_POWER_MENU, value ? 1 : 0, UserHandle.USER_CURRENT);

        } else if (preference == mEmergencyPref) {
            value = mEmergencyPref.isChecked();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.EMERGENCY_IN_POWER_MENU, value ? 1 : 0, UserHandle.USER_CURRENT);
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private void updatePreferences() {
        UserInfo currentUser = mUserManager.getUserInfo(UserHandle.myUserId());
        boolean developmentSettings = Settings.Global.getInt(
                getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
        boolean bugReport = Settings.Global.getInt(
                getContentResolver(), Settings.Global.BUGREPORT_IN_POWER_MENU, 0) == 1;
        boolean isPrimaryUser = currentUser == null || currentUser.isPrimary();
        if (mBugReportPref != null) {
            mBugReportPref.setEnabled(developmentSettings && isPrimaryUser);
            if (!developmentSettings) {
                mBugReportPref.setChecked(false);
                mBugReportPref.setSummary(R.string.power_menu_bug_report_devoptions_unavailable);
            } else if (!isPrimaryUser) {
                mBugReportPref.setChecked(false);
                mBugReportPref.setSummary(R.string.power_menu_bug_report_unavailable_for_user);
            } else {
                mBugReportPref.setChecked(bugReport);
                mBugReportPref.setSummary(null);
            }
        }

        boolean isKeyguardSecure = mLockPatternUtils.isSecure(UserHandle.myUserId());
        boolean lockdown = Settings.Secure.getIntForUser(
                getContentResolver(), Settings.Secure.LOCKDOWN_IN_POWER_MENU, 0,
                UserHandle.USER_CURRENT) == 1;
        if (mLockDownPref != null) {
            mLockDownPref.setEnabled(isKeyguardSecure);
            if (isKeyguardSecure) {
                mLockDownPref.setChecked(lockdown);
                mLockDownPref.setSummary(null);
            } else {
                mLockDownPref.setChecked(false);
                mLockDownPref.setSummary(R.string.power_menu_lockdown_unavailable);
            }
        }
    }
}