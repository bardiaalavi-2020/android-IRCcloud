/*
 * Copyright (c) 2015 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irccloud.android;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.drawerlayout.widget.DrawerLayout;

// An EditText that lets you use actions ("Done", "Go", etc.) on multi-line edits.
// From: http://stackoverflow.com/a/12570003/1406639
public class ActionEditText extends IRCEditText {
    private DrawerLayout mDrawerLayout = null;

    public ActionEditText(Context context) {
        super(context);
        init();
    }

    public ActionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ActionEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection ic = super.onCreateInputConnection(outAttrs);
        if(ic == null)
            return null;

        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS;
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NAVIGATE_NEXT;
        if (IRCCloudApplication.getInstance().getApplicationContext().getResources().getBoolean(R.bool.isTablet) || PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("kb_send", false)) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
            outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT;
        } else {
            outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT | EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
            outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }
        if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("kb_caps", true)) {
            outAttrs.inputType |= EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES;
        } else {
            outAttrs.inputType &= ~EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES;
        }

        EditorInfoCompat.setContentMimeTypes(outAttrs, new String[]{"image/*"});

        final InputConnectionCompat.OnCommitContentListener callback =
                new InputConnectionCompat.OnCommitContentListener() {
                    @Override
                    public boolean onCommitContent(InputContentInfoCompat inputContentInfo,
                                                   int flags, Bundle opts) {
                        // read and display inputContentInfo asynchronously
                        if ((flags &
                                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                            try {
                                inputContentInfo.requestPermission();
                            }
                            catch (Exception e) {
                                return false; // return false if failed
                            }
                        }

                        if(imageListener != null) {
                            boolean result = imageListener.onIMEImageReceived(inputContentInfo);
                            inputContentInfo.releasePermission();
                            return result;
                        }
                        return false;
                    }
                };
        return InputConnectionCompat.createWrapper(ic, outAttrs, callback);
    }

    private void init() {
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (mDrawerLayout != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            mDrawerLayout.closeDrawers();
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setDrawerLayout(DrawerLayout view) {
        mDrawerLayout = view;
    }

    private OnIMEImageReceivedListener imageListener = null;

    public void setOnIMEImageReceivedListener(OnIMEImageReceivedListener listener) {
        imageListener = listener;
    }

    public interface OnIMEImageReceivedListener {
        boolean onIMEImageReceived(InputContentInfoCompat info);
    }
}