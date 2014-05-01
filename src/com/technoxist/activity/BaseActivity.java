/**
 * technoXist
 *
 * Copyright (c) 2014 Suyash Bhatt
 * 
 * Copyright (c) 2012-2013 Frederic Julian
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.technoxist.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.technoxist.Constants;

public abstract class BaseActivity extends Activity {

    public interface OnFullScreenListener {
        public void onFullScreenEnabled(boolean isImmersive);

        public void onFullScreenDisabled();
    }

    private static final String STATE_IS_FULLSCREEN = "STATE_IS_FULLSCREEN";

    private boolean mIsFullScreen;
    private View mDecorView;

    private OnFullScreenListener mOnFullScreenListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDecorView = getWindow().getDecorView();
        mDecorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) { // We are not un fullscreen mode

                            if (mIsFullScreen) { // It was fullscreen, we need to change it
                                toggleFullScreen();
                                mIsFullScreen = false;

                                if (mOnFullScreenListener != null) {
                                    mOnFullScreenListener.onFullScreenDisabled();
                                }
                            }
                        } else { // We are now in fullscreen mode
                            if (!mIsFullScreen) { // It was not-fullscreen, we need to change it
                                mIsFullScreen = true;

                                if (mOnFullScreenListener != null) {
                                    mOnFullScreenListener.onFullScreenEnabled(android.os.Build.VERSION.SDK_INT >= 19);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        if (Constants.NOTIF_MGR != null) {
            Constants.NOTIF_MGR.cancel(0);
        }

        if (mIsFullScreen && getActionBar().isShowing()) { // This is needed for the immersive mode
            mIsFullScreen = false;
            toggleFullScreen();
        }

        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_IS_FULLSCREEN, mIsFullScreen);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_IS_FULLSCREEN)) {
            toggleFullScreen();
        }

        super.onRestoreInstanceState(savedInstanceState);
    }

    public void setOnFullscreenListener(OnFullScreenListener listener) {
        mOnFullScreenListener = listener;
    }

    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    @SuppressLint("InlinedApi")
    public void toggleFullScreen() {
        if (!mIsFullScreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            } else {
                mIsFullScreen = true;

                getActionBar().hide();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

                if (mOnFullScreenListener != null) {
                    mOnFullScreenListener.onFullScreenEnabled(false);
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                mIsFullScreen = false;

                getActionBar().show();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                if (mOnFullScreenListener != null) {
                    mOnFullScreenListener.onFullScreenDisabled();
                }
            }
        }
    }
}