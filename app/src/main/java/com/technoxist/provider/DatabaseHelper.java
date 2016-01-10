/**
 * technoXist
 *
 * Copyright (c) 2014-2015 Suyash Bhatt
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

package com.technoxist.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.Handler;

import com.technoxist.MainApplication;
import com.technoxist.R;
import com.technoxist.parser.OPML;
import com.technoxist.provider.FeedData.EntryColumns;
import com.technoxist.provider.FeedData.FeedColumns;
import com.technoxist.provider.FeedData.FilterColumns;
import com.technoxist.provider.FeedData.TaskColumns;

import java.io.File;

class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "technoXist.db";
    private static final int DATABASE_VERSION = 8;

    private static final String ALTER_TABLE = "ALTER TABLE ";
    private static final String ADD = " ADD ";

    private final Handler mHandler;

    public DatabaseHelper(Handler handler, Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mHandler = handler;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(createTable(FeedColumns.TABLE_NAME, FeedColumns.COLUMNS));
        database.execSQL(createTable(FilterColumns.TABLE_NAME, FilterColumns.COLUMNS));
        database.execSQL(createTable(EntryColumns.TABLE_NAME, EntryColumns.COLUMNS));
        database.execSQL(createTable(TaskColumns.TABLE_NAME, TaskColumns.COLUMNS));

        // Check if we need to import the backup
        File backupFile = new File(OPML.BACKUP_OPML);
        final boolean hasBackup = backupFile.exists();
        mHandler.post(new Runnable() { // In order to it after the database is created
            @Override
            public void run() {
                new Thread(new Runnable() { // To not block the UI
                    @Override
                    public void run() {
                        try {
                            if (hasBackup) {
                                // Perform an automated import of the backup
                                OPML.importFromFile(OPML.BACKUP_OPML);
                            } else {
                                // No database and no backup, automatically add the default feeds
                                OPML.importFromFile(MainApplication.getContext().getResources().openRawResource(R.raw.default_feeds));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }).start();
            }
        });
    }

    public void exportToOPML() {
        try {
            OPML.exportToFile(OPML.BACKUP_OPML);
        } catch (Exception ignored) {
        }
    }

    private String createTable(String tableName, String[][] columns) {
        if (tableName == null || columns == null || columns.length == 0) {
            throw new IllegalArgumentException("Invalid parameters for creating table " + tableName);
        } else {
            StringBuilder stringBuilder = new StringBuilder("CREATE TABLE ");

            stringBuilder.append(tableName);
            stringBuilder.append(" (");
            for (int n = 0, i = columns.length; n < i; n++) {
                if (n > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(columns[n][0]).append(' ').append(columns[n][1]);
            }
            return stringBuilder.append(");").toString();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 8) {
            executeCatchedSQL(database, ALTER_TABLE + EntryColumns.TABLE_NAME + ADD + EntryColumns.IMAGE_URL + ' ' + FeedData.TYPE_TEXT);
        }
    }

    private void executeCatchedSQL(SQLiteDatabase database, String query) {
        try {
            database.execSQL(query);
        } catch (Exception ignored) {
        }
    }

}
