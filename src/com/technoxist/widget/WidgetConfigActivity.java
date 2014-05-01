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
 *
 *
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 *
 *     The above copyright notice and this permission notice shall be included in
 *     all copies or substantial portions of the Software.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *     THE SOFTWARE.
 */

package com.technoxist.widget;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.View;
import android.view.View.OnClickListener;

import com.technoxist.R;
import com.technoxist.provider.FeedData.FeedColumns;
import com.technoxist.utils.PrefUtils;

public class WidgetConfigActivity extends PreferenceActivity {
    private int widgetId;

    private static final String NAME_COLUMN = "ifnull(" + FeedColumns.NAME + ',' + FeedColumns.URL + ") as title";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setResult(RESULT_CANCELED);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
        addPreferencesFromResource(R.layout.widget_preferences);
        setContentView(R.layout.widget_config);

        final PreferenceCategory feedsPreferenceCategory = (PreferenceCategory) findPreference("widget.visiblefeeds");

        Cursor cursor = getContentResolver().query(FeedColumns.CONTENT_URI, new String[]{FeedColumns._ID, NAME_COLUMN}, null, null, null);

        if (cursor.moveToFirst()) {
            int[] ids = new int[cursor.getCount() + 1];

            CheckBoxPreference checkboxPreference = new CheckBoxPreference(this);

            checkboxPreference.setTitle(R.string.all_feeds);
            feedsPreferenceCategory.addPreference(checkboxPreference);
            checkboxPreference.setKey("0");
            checkboxPreference.setDisableDependentsState(true);
            ids[0] = 0;
            for (int n = 1; !cursor.isAfterLast(); cursor.moveToNext(), n++) {
                checkboxPreference = new CheckBoxPreference(this);
                checkboxPreference.setTitle(cursor.getString(1));
                ids[n] = cursor.getInt(0);
                checkboxPreference.setKey(Integer.toString(ids[n]));
                feedsPreferenceCategory.addPreference(checkboxPreference);
                checkboxPreference.setDependency("0");
            }
            cursor.close();

            findViewById(R.id.save_button).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    StringBuilder builder = new StringBuilder();

                    for (int n = 0, i = feedsPreferenceCategory.getPreferenceCount(); n < i; n++) {
                        CheckBoxPreference preference = (CheckBoxPreference) feedsPreferenceCategory.getPreference(n);

                        if (preference.isChecked()) {
                            if (n == 0) {
                                break;
                            } else {
                                if (builder.length() > 0) {
                                    builder.append(',');
                                }
                                builder.append(preference.getKey());
                            }
                        }
                    }

                    String feedIds = builder.toString();
                    PrefUtils.putString(widgetId + ".feeds", feedIds);

                    int color = PrefUtils.getInt("widget.background", WidgetProvider.STANDARD_BACKGROUND);
                    PrefUtils.putInt(widgetId + ".background", color);

                    int fontSize = Integer.parseInt(PrefUtils.getString("widget.fontsize", "0"));
                    if (fontSize != 0) {
                        PrefUtils.putInt(widgetId + ".fontsize", fontSize);
                    } else {
                        PrefUtils.remove(widgetId + ".fontsize");
                    }

                    // Now we need to update the widget
                    Intent intent = new Intent(WidgetConfigActivity.this, WidgetProvider.class);
                    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetId});
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(WidgetConfigActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    try {
                        pendingIntent.send();
                    } catch (CanceledException ignored) {
                    }

                    setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
                    finish();
                }
            });
        } else {
            // no feeds found --> use all feeds, no dialog needed
            cursor.close();
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
        }
    }

}
