/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.keyguard.clock;

import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextClock;

import com.android.internal.colorextraction.ColorExtractor;
import com.android.systemui.R;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.plugins.ClockPlugin;

import java.util.TimeZone;

import static com.android.systemui.statusbar.phone
        .KeyguardClockPositionAlgorithm.CLOCK_USE_DEFAULT_Y;

/**
 * Plugin for the default clock face used only to provide a preview.
 */
public class SamsungHighlightClockController implements ClockPlugin {

    /**
     * Resources used to get title and thumbnail.
     */
    private final Resources mResources;

    /**
     * LayoutInflater used to inflate custom clock views.
     */
    private final LayoutInflater mLayoutInflater;

    /**
     * Extracts accent color from wallpaper.
     */
    private final SysuiColorExtractor mColorExtractor;

    /**
     * Renders preview from clock view.
     */
    private final ViewPreviewer mRenderer = new ViewPreviewer();

    /**
     * Root view of clock.
     */
    private ClockLayout mView;

    /**
     * Text clock in preview view hierarchy.
     */
    private TextClock mClock;

    /**
     * Accent color for the hour section
     */
    private int mAccentColor;

    private Context mContext;
    private Handler mHandler;

    /**
     * Create a DefaultClockController instance.
     *
     * @param res Resources contains title and thumbnail.
     * @param inflater Inflater used to inflate custom clock views.
     * @param colorExtractor Extracts accent color from wallpaper.
     */
    public SamsungHighlightClockController(Resources res, LayoutInflater inflater,
            SysuiColorExtractor colorExtractor) {
        mResources = res;
        mLayoutInflater = inflater;
        mContext = mLayoutInflater.getContext();
        mColorExtractor = colorExtractor;
    }

    private void createViews() {
        mView = (ClockLayout) mLayoutInflater
                .inflate(R.layout.digital_clock_custom, null);
        mClock = mView.findViewById(R.id.clock);
        mClock.setLineSpacing(0, 0.8f);
        updateTextColors();
        onTimeTick();
        mCustomSettingsObserver.startObserving();
    }

    @Override
    public void onDestroyView() {
        mView = null;
        mClock = null;
        mCustomSettingsObserver.stopObserving();
    }

    @Override
    public String getName() {
        return "samsung_highlight";
    }

    @Override
    public String getTitle() {
        return "Samsung";
    }

    @Override
    public Bitmap getThumbnail() {
        return BitmapFactory.decodeResource(mResources, R.drawable.samsung_thumbnail);
    }

    @Override
    public Bitmap getPreview(int width, int height) {

        View previewView = mLayoutInflater.inflate(R.layout.default_clock_preview, null);
        TextClock previewTime = previewView.findViewById(R.id.time);
        TextClock previewDate = previewView.findViewById(R.id.date);

        // Initialize state of plugin before generating preview.
        previewTime.setTextColor(Color.WHITE);
        previewDate.setTextColor(Color.WHITE);
        ColorExtractor.GradientColors colors = mColorExtractor.getColors(
                WallpaperManager.FLAG_LOCK);
        setColorPalette(colors.supportsDarkText(), colors.getColorPalette());
        previewTime.setLineSpacing(0, 0.8f);
        previewTime.setFormat12Hour(Html.fromHtml("hh<br><font color=" + mAccentColor + ">mm</font>"));
        previewTime.setFormat24Hour(Html.fromHtml("kk<br><font color=" + mAccentColor + ">mm</font>"));
        onTimeTick();

        return mRenderer.createPreview(previewView, width, height);
    }

    @Override
    public View getView() {
        if (mView == null) {
            createViews();
        }
        return mView;
    }

    @Override
    public View getBigClockView() {
        return null;
    }

    @Override
    public int getPreferredY(int totalHeight) {
        return CLOCK_USE_DEFAULT_Y;
    }

    @Override
    public void setStyle(Style style) {}

    @Override
    public void setTextColor(int color) {
        mClock.setTextColor(color);
    }

    @Override
    public void setColorPalette(boolean supportsDarkText, int[] colorPalette) {
        boolean mUseCustomAccent = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.LOCK_SCREEN_CLOCK_USE_CUSTOM_ACCENT_COLOR, 0,
                UserHandle.USER_CURRENT) != 0;
        if (mUseCustomAccent) {
            int mCustomAccent = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.LOCK_SCREEN_CLOCK_CUSTOM_ACCENT_COLOR, 0xFFFFFFFF);
            mAccentColor = mCustomAccent;
        } else {
            if (colorPalette == null || colorPalette.length == 0) {
                return;
            }
            final int color = colorPalette[Math.max(0, colorPalette.length - 5)];
            mAccentColor = color;
        }
    }

    @Override
    public void onTimeTick() {
        if (mView != null && mClock != null) {
            mView.onTimeChanged();
            mClock.refreshTime();
        }
    }

    @Override
    public void setDarkAmount(float darkAmount) {
        mView.setDarkAmount(darkAmount);
    }

    @Override
    public void onTimeZoneChanged(TimeZone timeZone) {}

    @Override
    public boolean shouldShowStatusArea() {
        return true;
    }

    private void updateTextColors() {
        ColorExtractor.GradientColors colors = mColorExtractor.getColors(
                WallpaperManager.FLAG_LOCK);
        setColorPalette(false, colors.getColorPalette());
        mClock.setFormat12Hour(Html.fromHtml("hh<br><font color=" + mAccentColor + ">mm</font>"));
        mClock.setFormat24Hour(Html.fromHtml("kk<br><font color=" + mAccentColor + ">mm</font>"));
    }

    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void startObserving() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_CLOCK_USE_CUSTOM_ACCENT_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_CLOCK_CUSTOM_ACCENT_COLOR),
                    false, this, UserHandle.USER_ALL);
        }

        void stopObserving() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(Settings.System.LOCK_SCREEN_CLOCK_USE_CUSTOM_ACCENT_COLOR)) ||
                    uri.equals(Settings.System.getUriFor(Settings.System.LOCK_SCREEN_CLOCK_CUSTOM_ACCENT_COLOR))) {
                updateTextColors();
            }
        }
    }
}