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

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.technoxist.Constants;
import com.technoxist.R;
import com.technoxist.fragment.EntryFragment;
import com.technoxist.utils.PrefUtils;
import com.technoxist.utils.UiUtils;

public class EntryActivity extends BaseActivity {

    private EntryFragment mEntryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);
        if (PrefUtils.getBoolean(PrefUtils.DISPLAY_ENTRIES_FULLSCREEN, false))
        	toggleFullScreen();
        
        setContentView(R.layout.activity_entry);

        mEntryFragment = (EntryFragment) getFragmentManager().findFragmentById(R.id.entry_fragment);
        if (savedInstanceState == null) { // Put the data only the first time (the fragment will save its state)
            mEntryFragment.setData(getIntent().getData());
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Bundle b = getIntent().getExtras();
            if (b != null && b.getBoolean(Constants.INTENT_FROM_WIDGET, false)) {
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
            }
            finish();
            return true;
        }

        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mEntryFragment.setData(intent.getData());
    }
}