/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.systemui.screenrecord;

import static com.android.systemui.screenrecord.ScreenRecordingAudioSource.INTERNAL;
import static com.android.systemui.screenrecord.ScreenRecordingAudioSource.MIC;
import static com.android.systemui.screenrecord.ScreenRecordingAudioSource.MIC_AND_INTERNAL;
import static com.android.systemui.screenrecord.ScreenRecordingAudioSource.NONE;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;

import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.settings.CurrentUserContextTracker;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Activity to select screen recording options
 */
public class ScreenRecordDialog extends Activity {
    private static final long DELAY_MS = 3000;
    private static final long INTERVAL_MS = 1000;
    private static final String TAG = "ScreenRecordDialog";
    private static final String PREFS = "screenrecord_";
    private static final String PREF_TAPS = "show_taps";
    private static final String PREF_DOT = "show_dot";
    private static final String PREF_LOW = "use_low_quality";
    private static final String PREF_LONGER = "use_longer_timeout";
    private static final String PREF_AUDIO = "use_audio";
    private static final String PREF_AUDIO_SOURCE = "audio_source";

    private final RecordingController mController;
    private final CurrentUserContextTracker mCurrentUserContextTracker;
    private Switch mTapsSwitch;
    private Switch mStopDotSwitch;
    private Switch mLowQualitySwitch;
    private Switch mLongerSwitch;
    private Switch mAudioSwitch;
    private Spinner mOptions;
    private List<ScreenRecordingAudioSource> mModes;

    @Inject
    public ScreenRecordDialog(RecordingController controller,
            CurrentUserContextTracker currentUserContextTracker) {
        mController = controller;
        mCurrentUserContextTracker = currentUserContextTracker;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        // Inflate the decor view, so the attributes below are not overwritten by the theme.
        window.getDecorView();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.addPrivateFlags(WindowManager.LayoutParams.SYSTEM_FLAG_SHOW_FOR_ALL_USERS);
        window.setGravity(Gravity.BOTTOM);
        setTitle(R.string.screenrecord_name);

        setContentView(R.layout.screen_record_dialog);

        Button cancelBtn = findViewById(R.id.button_cancel);
        cancelBtn.setOnClickListener(v -> {
            finish();
        });

        Button startBtn = findViewById(R.id.button_start);
        startBtn.setOnClickListener(v -> {
            requestScreenCapture();
            finish();
        });

        mModes = new ArrayList<>();
        mModes.add(MIC);
        mModes.add(INTERNAL);
        mModes.add(MIC_AND_INTERNAL);

        Context userContext = mCurrentUserContextTracker.getCurrentUserContext();

        mAudioSwitch = findViewById(R.id.screenrecord_audio_switch);
        mAudioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putInt(userContext, PREFS + PREF_AUDIO, isChecked ? 1 : 0);
        });

        mTapsSwitch = findViewById(R.id.screenrecord_taps_switch);
        mTapsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putInt(userContext, PREFS + PREF_TAPS, isChecked ? 1 : 0);
        });

        mStopDotSwitch = findViewById(R.id.screenrecord_stopdot_switch);
        mStopDotSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putInt(userContext, PREFS + PREF_DOT, isChecked ? 1 : 0);
        });

        mLowQualitySwitch = findViewById(R.id.screenrecord_lowquality_switch);
        mLowQualitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putInt(userContext, PREFS + PREF_LOW, isChecked ? 1 : 0);
        });

        mLongerSwitch = findViewById(R.id.screenrecord_longer_timeout_switch);
        mLongerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putInt(userContext, PREFS + PREF_LONGER, isChecked ? 1 : 0);
        });

        mOptions = findViewById(R.id.screen_recording_options);
        mOptions.setOnItemClickListenerInt((parent, view, position, id) -> {
            mAudioSwitch.setChecked(true);
            Prefs.putInt(userContext, PREFS + PREF_AUDIO_SOURCE, position);
            Prefs.putInt(userContext, PREFS + PREF_AUDIO, 1);
        });

        ArrayAdapter a = new ScreenRecordingAdapter(getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,
                mModes);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mOptions.setAdapter(a);
        loadPrefs();
    }

    private void requestScreenCapture() {
        Context userContext = mCurrentUserContextTracker.getCurrentUserContext();
        boolean showTaps = mTapsSwitch.isChecked();
        boolean showStopDot = mStopDotSwitch.isChecked();
        boolean lowQuality = mLowQualitySwitch.isChecked();
        boolean longerDuration = mLongerSwitch.isChecked();
        ScreenRecordingAudioSource audioMode = mAudioSwitch.isChecked()
                ? (ScreenRecordingAudioSource) mOptions.getSelectedItem()
                : NONE;
        PendingIntent startIntent = PendingIntent.getForegroundService(userContext,
                RecordingService.REQUEST_CODE,
                RecordingService.getStartIntent(
                        userContext, RESULT_OK,
                        audioMode.ordinal(), showTaps, showStopDot, lowQuality,
                        longerDuration),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stopIntent = PendingIntent.getService(userContext,
                RecordingService.REQUEST_CODE,
                RecordingService.getStopIntent(userContext),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mController.startCountdown(DELAY_MS, INTERVAL_MS, startIntent, stopIntent);
    }

    private void loadPrefs() {
        Context userContext = mCurrentUserContextTracker.getCurrentUserContext();
        mTapsSwitch.setChecked(Prefs.getInt(userContext, PREFS + PREF_TAPS, 0) == 1);
        mStopDotSwitch.setChecked(Prefs.getInt(userContext, PREFS + PREF_DOT, 0) == 1);
        mLowQualitySwitch.setChecked(Prefs.getInt(userContext, PREFS + PREF_LOW, 0) == 1);
        mLongerSwitch.setChecked(Prefs.getInt(userContext, PREFS + PREF_LONGER, 0) == 1);
        mAudioSwitch.setChecked(Prefs.getInt(userContext, PREFS + PREF_AUDIO, 0) == 1);
        mOptions.setSelection(Prefs.getInt(userContext, PREFS + PREF_AUDIO_SOURCE, 0));
    }
}