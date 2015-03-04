/**
 * technoXist
 *
 * Copyright (c) 2014 Suyash Bhatt
 * 
 * Copyright (c) 2012-2013 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.technoxist.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.technoxist.R;
import com.technoxist.provider.FeedData;
import com.technoxist.provider.FeedData.EntryColumns;
import com.technoxist.utils.UiUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class DrawerAdapter extends BaseAdapter {

    private static final int POS_ID = 0;
    private static final int POS_URL = 1;
    private static final int POS_NAME = 2;
    private static final int POS_IS_GROUP = 3;
    private static final int POS_GROUP_ID = 4;
    private static final int POS_ICON = 5;
    private static final int POS_UNREAD = 8;

    private static final int ITEM_PADDING = UiUtils.dpToPixel(20);
    private static final int NORMAL_TEXT_COLOR = Color.parseColor("#000000");
    private static final int GROUP_TEXT_COLOR = Color.parseColor("#BBBBBB");

    private static final int CACHE_MAX_ENTRIES = 100;
    private final Map<Long, String> mFormattedDateCache = new LinkedHashMap<Long, String>(CACHE_MAX_ENTRIES + 1, .75F, true) {
        @Override
        public boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
            return size() > CACHE_MAX_ENTRIES;
        }
    };

    private final Context mContext;
    private Cursor mFeedsCursor;
    private int mAllUnreadNumber, mFavoritesNumber, N;

    private static class ViewHolder {
        public ImageView iconView;
        public TextView titleTxt;
        public TextView unreadTxt;
        public View separator;
    }

    public DrawerAdapter(Context context, Cursor feedCursor) {
        mContext = context;
        mFeedsCursor = feedCursor;

        updateNumbers();
    }

    public void setCursor(Cursor feedCursor) {
        mFeedsCursor = feedCursor;

        updateNumbers();
        notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_drawer_list, parent, false);

            ViewHolder holder = new ViewHolder();
            holder.iconView = (ImageView) convertView.findViewById(R.id.icon);
            holder.titleTxt = (TextView) convertView.findViewById(android.R.id.text1); 
            holder.unreadTxt = (TextView) convertView.findViewById(R.id.unread_count);
            holder.separator = convertView.findViewById(R.id.separator);
            convertView.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();

        // default init
        holder.iconView.setImageDrawable(null);
        holder.titleTxt.setText("");
        holder.titleTxt.setTextColor(NORMAL_TEXT_COLOR);
        holder.titleTxt.setAllCaps(false);
        holder.unreadTxt.setText("");
        convertView.setPadding(0, 0, 0, 0);
        holder.separator.setVisibility(View.GONE);
        N = mFeedsCursor.getCount();
        if (position == 0) {
            holder.titleTxt.setText(R.string.all);
            holder.iconView.setImageResource(R.drawable.all);

            int unread = mAllUnreadNumber;
            if (unread != 0) {
                holder.unreadTxt.setText(String.valueOf(unread));
            }
        }else if (position == N+1){
	    holder.titleTxt.setText(R.string.favorites);
            holder.iconView.setImageResource(R.drawable.dimmed_rating_important);
            int unread = mFavoritesNumber;
            if (unread != 0) {
            holder.unreadTxt.setText(String.valueOf(unread));
            }
	} else if (position == N+2) {
            holder.titleTxt.setText(android.R.string.search_go);
            holder.iconView.setImageResource(android.R.drawable.ic_menu_search);
    } else if (mFeedsCursor.moveToPosition(position - 1)) {
        holder.titleTxt.setText((mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME)));

        if (mFeedsCursor.getInt(POS_IS_GROUP) == 1) {
            holder.titleTxt.setTextColor(GROUP_TEXT_COLOR);
            holder.titleTxt.setAllCaps(true);
            holder.separator.setVisibility(View.VISIBLE);
        } else {
            final long feedId = mFeedsCursor.getLong(POS_ID);
            Bitmap bitmap = UiUtils.getFaviconBitmap(feedId, mFeedsCursor, POS_ICON);
            String FeedName = mFeedsCursor.getString(POS_NAME);
            if (FeedName.equals("News")){
            	holder.iconView.setImageResource(R.drawable.news);
            }
            else if (FeedName.equals("Help")) {
            	holder.iconView.setImageResource(R.drawable.help);
            }
            else if (FeedName.equals("Reviews")) {
            	holder.iconView.setImageResource(R.drawable.review);
            }
            else{
            	if (bitmap != null) {
                    holder.iconView.setImageBitmap(bitmap);
                } else {
            	holder.iconView.setImageResource(R.drawable.icon);
                }
            }

            int unread = mFeedsCursor.getInt(POS_UNREAD);
            if (unread != 0) {
                holder.unreadTxt.setText(String.valueOf(unread));
            }
        }

        if (!mFeedsCursor.isNull(POS_GROUP_ID)) { // First level
            convertView.setPadding(ITEM_PADDING, 0, 0, 0);
        }
    }return convertView;
    }

    @Override
    public int getCount() {
        if (mFeedsCursor != null) {
            return mFeedsCursor.getCount() + 3;
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 1)) {
            return mFeedsCursor.getLong(POS_ID);
        }

        return -1;
    }

    public byte[] getItemIcon(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 1)) {
            return mFeedsCursor.getBlob(POS_ICON);
        }

        return null;
    }

    public String getItemName(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 1)) {
            return mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME);
        }

        return null;
    }

    public boolean isItemAGroup(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 1)) {
            return mFeedsCursor.getInt(POS_IS_GROUP) == 1;
        }

        return false;
    }

    private void updateNumbers() {
        mAllUnreadNumber = mFavoritesNumber = 0;

        // Gets the numbers of entries (should be in a thread, but it's way easier like this and it shouldn't be so slow)
        Cursor numbers = mContext.getContentResolver().query(EntryColumns.CONTENT_URI, new String[]{FeedData.ALL_UNREAD_NUMBER, FeedData.FAVORITES_NUMBER}, null, null, null);
        if (numbers != null) {
            if (numbers.moveToFirst()) {
                mAllUnreadNumber = numbers.getInt(0);
                mFavoritesNumber = numbers.getInt(1);
            }
            numbers.close();
        }
    }
}
