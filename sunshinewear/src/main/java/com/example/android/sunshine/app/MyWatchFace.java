/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {
    private static final String TAG = MyWatchFace.class.getSimpleName();

    private static final String FONT_PATH_RUBIK_BOLD = "fonts/Rubik-Bold.ttf";
    private static final String FONT_PATH_RUBIK_MEDIUM = "fonts/Rubik-Medium.ttf";
    private static final String FONT_PATH_RUBIK_LIGHT = "fonts/Rubik-Light.ttf";

    private static final String DATE_FORMAT = "MMM d";
    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;
    Bitmap mBitmapWeatherIcon;
    String mHighTemp, mLowTemp;

    private Typeface getTypeface(TYPEFACE typeface) {
        switch (typeface) {
            case RUBIK_BOLD:
                return Typeface.createFromAsset(getAssets(), FONT_PATH_RUBIK_BOLD);
            case RUBIK_MEDIUM:
                return Typeface.createFromAsset(getAssets(), FONT_PATH_RUBIK_MEDIUM);
            case RUBIK_LIGHT:
                return Typeface.createFromAsset(getAssets(), FONT_PATH_RUBIK_LIGHT);
        }
        return Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onDataChanged(WeatherData weatherData) {
        mBitmapWeatherIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), weatherData.getDrawableRes()),
                48, 48, true);
        mHighTemp = weatherData.getHighTemp() + "\u00B0";
        mLowTemp = weatherData.getLowTemp() + "\u00B0";
    }

    private enum TYPEFACE {
        RUBIK_BOLD,
        RUBIK_MEDIUM,
        RUBIK_LIGHT
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Engine> mWeakReference;

        public EngineHandler(Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaintHours;
        Paint mTextPaintMinutes;
        Paint mTextPaintSeconds;
        Paint mTextPaintDate;
        Paint mTextPaintHighTemp;
        Paint mTextPaintLowTemp;
        Paint mPaintWeatherIcon;
        boolean mAmbient;
        Calendar mCalendar;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        float mXOffsetHours1, mXOffsetHours2,
                mXOffsetMinutes1, mXOffsetMinutes2,
                mXOffsetSeconds, mXOffsetDate,
                mXOffsetWeatherIcon, mXOffsetHighTemp,
                mXOffsetLowTemp;
        float mYOffsetHours1, mYOffsetHours2,
                mYOffsetMinutes1, mYOffsetMinutes2,
                mYOffsetSeconds, mYOffsetDate,
                mYOffsetWeatherIcon, mYOffsetHighTemp,
                mYOffsetLowTemp;
        float mYGap, mXGap;
        Bitmap mBitmapBackground;
        Bitmap mBackgroundScaledBitmap;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = MyWatchFace.this.getResources();
            mYOffsetHours1 = resources.getDimension(R.dimen.digital_y_offset);
            mYGap = resources.getDimension(R.dimen.digital_y_gap);
            mXGap = resources.getDimension(R.dimen.digital_x_gap);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaintHours = new Paint();
            mTextPaintHours = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_BOLD);
            mTextPaintHours.setTextSize(resources.getDimension(R.dimen.text_size_hours));

            mTextPaintMinutes = new Paint();
            mTextPaintMinutes = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_BOLD);
            mTextPaintMinutes.setTextSize(resources.getDimension(R.dimen.text_size_minutes));

            mTextPaintSeconds = new Paint();
            mTextPaintSeconds = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_LIGHT);
            mTextPaintSeconds.setTextSize(resources.getDimension(R.dimen.text_size_seconds));

            mTextPaintDate = new Paint();
            mTextPaintDate = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_LIGHT);
            mTextPaintDate.setTextSize(resources.getDimension(R.dimen.text_size_seconds));

            mTextPaintHighTemp = new Paint();
            mTextPaintHighTemp = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_MEDIUM);
            mTextPaintHighTemp.setTextSize(resources.getDimension(R.dimen.text_size_seconds));

            mTextPaintLowTemp = new Paint();
            mTextPaintLowTemp = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_LIGHT);
            mTextPaintLowTemp.setColor(resources.getColor(R.color.card_grey_text_color));
            mTextPaintLowTemp.setTextSize(resources.getDimension(R.dimen.text_size_seconds));

            mPaintWeatherIcon = new Paint();
            mPaintWeatherIcon.setAntiAlias(true);
            mPaintWeatherIcon.setFilterBitmap(true);
            mPaintWeatherIcon.setDither(true);

            mCalendar = Calendar.getInstance();

            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg, null);
            mBitmapBackground = ((BitmapDrawable) backgroundDrawable).getBitmap();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor, TYPEFACE typeface) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(getTypeface(typeface));
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffsetHours1 = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            mXOffsetMinutes1 = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaintHours.setTextSize(textSize);

            mTextPaintMinutes.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaintHours.setAntiAlias(!inAmbientMode);
                    mTextPaintMinutes.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    /*Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
                            .show();*/
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                //canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            String hours = String.format(Locale.getDefault(), "%02d", mCalendar.get(Calendar.HOUR));
            String minutes = String.format(Locale.getDefault(), "%02d", mCalendar.get(Calendar.MINUTE));

            char hour1 = hours.charAt(0);
            char hour2 = hours.charAt(1);

            char minutes1 = minutes.charAt(0);
            char minutes2 = minutes.charAt(1);

            String seconds = null;
            String date = null;
            if (!mAmbient) {
                seconds = String.format(Locale.getDefault(), "%02d", mCalendar.get(Calendar.SECOND));
                date = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(mCalendar.getTime());
            }

            // draw hour1 and hour2
            canvas.drawText(String.valueOf(hour1), mXOffsetHours1, mYOffsetHours1, mTextPaintHours);

            Rect rectBoundsHour1 = new Rect();
            mTextPaintHours.getTextBounds(String.valueOf(hour1), 0, 1, rectBoundsHour1);

            mXOffsetHours2 = mXOffsetHours1 + rectBoundsHour1.width() + mXGap;
            mYOffsetHours2 = mYOffsetHours1;

            canvas.drawText(String.valueOf(hour2), mXOffsetHours2, mYOffsetHours2, mTextPaintHours);

            Rect rectBoundsHours = new Rect();
            mTextPaintHours.getTextBounds(hours, 0, hours.length() - 1, rectBoundsHours);
            //Log.e(TAG, "onDraw: "+ rectBoundsHours.toString());

            mYOffsetMinutes1 = mYOffsetHours1 + rectBoundsHours.height() + mYGap;

            // draw minute1 and minute2
            canvas.drawText(String.valueOf(minutes1), mXOffsetMinutes1, mYOffsetMinutes1, mTextPaintMinutes);

            Rect rectBoundsMinute1 = new Rect();
            mTextPaintMinutes.getTextBounds(String.valueOf(minutes1), 0, 1, rectBoundsMinute1);

            mXOffsetMinutes2 = mXOffsetMinutes1 + rectBoundsHour1.width() + mXGap;
            mYOffsetMinutes2 = mYOffsetMinutes1;

            canvas.drawText(String.valueOf(minutes2), mXOffsetMinutes2, mYOffsetMinutes2, mTextPaintMinutes);

            Rect rectBoundsMinutes = new Rect();
            mTextPaintMinutes.getTextBounds(minutes, 0, minutes.length() - 1, rectBoundsMinutes);

            if (!TextUtils.isEmpty(seconds) && !TextUtils.isEmpty(date)) {
                Rect rectBoundsSeconds = new Rect();
                mTextPaintSeconds.getTextBounds(seconds, 0, seconds.length() - 1, rectBoundsSeconds);

                mXOffsetSeconds = mXOffsetHours1;
                mYOffsetSeconds = mYOffsetMinutes1 + rectBoundsSeconds.height() + mYGap;

                //mXOffsetSeconds+=rectBoundsMinutes.width()+rectBoundsSeconds.right;

                canvas.drawText(seconds, mXOffsetSeconds, mYOffsetSeconds, mTextPaintSeconds);

                // draw date
                Rect rectBoundsDate = new Rect();
                mTextPaintDate.getTextBounds(date, 0, date.length() - 1, rectBoundsDate);

                mXOffsetDate = bounds.width() - rectBoundsDate.width() - mXOffsetSeconds * 2;
                mYOffsetDate = mYOffsetSeconds;

                canvas.drawText(date, mXOffsetDate, mYOffsetDate, mTextPaintDate);

                // draw weather icon
                mXOffsetWeatherIcon = mXOffsetDate;
                mYOffsetWeatherIcon = mYOffsetHours1 - rectBoundsHours.height();

                if (mBitmapWeatherIcon != null) {
                    canvas.drawBitmap(mBitmapWeatherIcon, mXOffsetWeatherIcon, mYOffsetWeatherIcon, mPaintWeatherIcon);
                }

                if (!TextUtils.isEmpty(mHighTemp) && !TextUtils.isEmpty(mLowTemp)) {
                    // draw weather temp
                    mXOffsetHighTemp = mXOffsetDate;
                    mYOffsetHighTemp = mYOffsetMinutes1;

                    canvas.drawText(mHighTemp, mXOffsetHighTemp, mYOffsetHighTemp, mTextPaintHighTemp);

                    Rect rectBoundsHighTemp = new Rect();
                    mTextPaintHighTemp.getTextBounds(mHighTemp, 0, mHighTemp.length() - 1, rectBoundsHighTemp);

                    mXOffsetLowTemp = mXOffsetHighTemp + rectBoundsHighTemp.width() + mXGap;
                    mYOffsetLowTemp = mYOffsetHighTemp;

                    canvas.drawText(mLowTemp, mXOffsetLowTemp, mYOffsetLowTemp, mTextPaintLowTemp);
                }
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBitmapBackground,
                        width, height, true /* filter */);
            }
        }
    }
}
