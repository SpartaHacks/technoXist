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

package com.technoxist.view;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.technoxist.Constants;
import com.technoxist.R;
import com.technoxist.utils.FileUtils;
import com.technoxist.utils.HtmlUtils;
import com.technoxist.utils.PrefUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class EntryView extends WebView {

    private static final String TEXT_HTML = "text/html";
    private static final String HTML_IMG_REGEX = "(?i)<[/]?[ ]?img(.|\n)*?>";

    private static final String BACKGROUND_COLOR = "#f6f6f6";
    private static final String TEXT_COLOR = "#000000";
    private static final String BUTTON_COLOR = "#2b8bf0";
    private static final String SUBTITLE_COLOR = "#666666";
    private static final String SUBTBORDER_COLOR = "solid #ddd";

    private static final String CSS = "<head><style type='text/css'> "
            + "body {max-width: 100%; margin: 1.2em 0.3cm 0.3cm 0.2cm; font-family: sans-serif-light; color: " + TEXT_COLOR + "; background-color:" + BACKGROUND_COLOR + "; line-height: 140%; font-weight:800} "
            + "* {max-width: 100%; word-break: break-word}"
            + "h1, h2 {font-weight: normal; line-height: 130%} "
            + "h1 {font-size: 170%; margin-bottom: 0.1em} "
            + "h2 {font-size: 140%} "
            + "a {color: #0099CC}"
            + "h1 a {color: inherit; text-decoration: none}"
            + "img {height: auto} "
            + "pre {white-space: pre-wrap;} "
            + "blockquote {margin: 0.8em 0 0.8em 1.2em; padding: 0} "
            + "p {margin: 0.8em 0 0.8em 0; text-align: justify} "
            + "input[type=button], .buttons {font-weight:800} "
            + ".buttons {margin: 0.8em 0 0.8em 0; text-align: center} "
            + "p.subtitle {color: " + SUBTITLE_COLOR + "; border-top:1px " + SUBTBORDER_COLOR + "; border-bottom:1px " + SUBTBORDER_COLOR + "; padding-top:2px; padding-bottom:2px; font-weight:800 } "
            + "ul, ol {margin: 0 0 0.8em 0.6em; padding: 0 0 0 1em} "
            + "ul li, ol li {margin: 0 0 0.8em 0; padding: 0} "
            + "div.button-section {padding: 0.4cm 0; margin: 0; text-align: center} "
            + ".button-section p {margin: 0.1cm 0 0.2cm 0}"
            + ".button-section p.marginfix {margin: 0.5cm 0 0.5cm 0}"
            + ".button-section input, .button-section a {font-family: sans-serif-light; font-size: 100%; color: #FFFFFF; background-color: " + BUTTON_COLOR + "; text-decoration: none; border: none; border-radius:0.2cm; padding: 0.3cm} "
            + "</style><meta name='viewport' content='width=device-width'/></head>";
    private static final String BODY_START = "<body>";
    private static final String BODY_END = "</body>";
    private static final String TITLE_START = "<h1><a href='";
    private static final String TITLE_MIDDLE = "'>";
    private static final String TITLE_END = "</a></h1>";
    private static final String SUBTITLE_START = "<p class='subtitle'>";
    private static final String SUBTITLE_END = "</p>";
    private static final String BUTTON_SECTION_START = "<div class='button-section'>";
    private static final String BUTTON_SECTION_END = "</div>";
    private static final String BUTTON_START = "<p class='buttons'><input type='button' value='";
    private static final String BUTTON_MIDDLE = "' onclick='";
    private static final String BUTTON_END = "'/></p>";
    // the separate 'marginfix' selector in the following is only needed because the CSS box model treats <input> and <a> elements differently
    private static final String LINK_BUTTON_START = "<p class='marginfix buttons'><a href='";
    private static final String LINK_BUTTON_MIDDLE = "'>";
    private static final String LINK_BUTTON_END = "</a></p>";
    private static final String IMAGE_ENCLOSURE = "[@]image/";

    private final JavaScriptObject mInjectedJSObject = new JavaScriptObject();
    private EntryViewManager mEntryViewMgr;

    //For Fullscreen Video view
    public View mCustomView;
    private FrameLayout videoLayout;

    public EntryView(Context context) {
        super(context);

        init(context, null, 0);
    }

    public EntryView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs, 0);
    }

    public EntryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context, attrs, defStyle);
        }
        
    public void setListener(EntryViewManager manager) {
        mEntryViewMgr = manager;
    }

    public void setHtml(long entryId, String title, String link, String contentText, String enclosure, String author, long timestamp, boolean preferFullText) {
    	if (PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true)) {
        	contentText = HtmlUtils.replaceImageURLs(contentText, entryId);
            if (getSettings().getBlockNetworkImage()) {
                // setBlockNetwortImage(false) calls postSync, which takes time, so we clean up the html first and change the value afterwards
            	loadData("", TEXT_HTML, Constants.UTF8);
            	getSettings().setBlockNetworkImage(false);
            }
    	} else {
            contentText = contentText.replaceAll(HTML_IMG_REGEX, "");
            getSettings().setBlockNetworkImage(true);
        }

        // Iframe videos to get fullscreen
        contentText = contentText.replace("frameborder=\"0\"", "frameborder=\"0\" allowfullscreen");
        contentText = contentText.replace("?rel=0&amp;autoplay=0&amp;wmode=opaque&amp;controls=2&amp;autohide=1&amp;showinfo=0", "");

        // String baseUrl = "";
        // try {
        // URL url = new URL(mLink);
        // baseUrl = url.getProtocol() + "://" + url.getHost();
        // } catch (MalformedURLException ignored) {
        // }

        // do not put 'null' to the base url...
    	loadDataWithBaseURL("", generateHtmlContent(title, link, contentText, enclosure, author, timestamp), TEXT_HTML, Constants.UTF8, null);
    }

    private String generateHtmlContent(String title, String link, String contentText, String enclosure, String author, long timestamp) {
        StringBuilder content = new StringBuilder(CSS).append(BODY_START);

        if (link == null) {
            link = "";
        }
        content.append(TITLE_START).append(link).append(TITLE_MIDDLE).append(title).append(TITLE_END).append(SUBTITLE_START);
        Date date = new Date(timestamp);
        Context context = getContext();
        StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getLongDateFormat(context).format(date)).append(' ').append(
                DateFormat.getTimeFormat(context).format(date));

        if (author != null && !author.isEmpty()) {
            dateStringBuilder.append(" | ").append(author);
        }

        content.append(dateStringBuilder).append(SUBTITLE_END).append(contentText).append(BUTTON_SECTION_START);


        if (enclosure != null && enclosure.length() > 6 && !enclosure.contains(IMAGE_ENCLOSURE)) {
            content.append(BUTTON_START).append(context.getString(R.string.see_enclosure)).append(BUTTON_MIDDLE)
                    .append("injectedJSObject.onClickEnclosure();").append(BUTTON_END);
        }

        if (link.length() > 0) {
            content.append(LINK_BUTTON_START).append(link).append(LINK_BUTTON_MIDDLE).append(context.getString(R.string.see_link)).append(LINK_BUTTON_END);
        }

        content.append(BUTTON_SECTION_END).append(BODY_END);

        return content.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init(Context context, AttributeSet attrs, int defStyle) {

        // For scrolling
        setHorizontalScrollBarEnabled(false);
        getSettings().setUseWideViewPort(false);

        // For color
        setBackgroundColor(Color.parseColor(BACKGROUND_COLOR));

        // Text zoom level from preferences
        int fontSize = Integer.parseInt(PrefUtils.getString(PrefUtils.FONT_SIZE, "0"));
        if (fontSize != 0) {
        	getSettings().setTextZoom(100 + (fontSize * 20));
        }

        // For javascript
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mInjectedJSObject, mInjectedJSObject.toString());

        // For HTML5 Video
        setWebChromeClient(new VideoWebView());


        setWebViewClient(new MyWebViewClient());

    }
    // For HTML5 video
    public class VideoWebView extends WebChromeClient {
        private CustomViewCallback mCustomViewCallback;

        @Override
        public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }


            videoLayout = mEntryViewMgr.getVideoLayout();
            videoLayout.addView(view);
            mCustomView = view;
            mCustomViewCallback = callback;
            videoLayout.setVisibility(View.VISIBLE);
            setVisibility(View.GONE);
            mEntryViewMgr.onStartVideoFullScreen();

        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();

            if (mCustomView == null) {
                return;
            }

            videoLayout = mEntryViewMgr.getVideoLayout();

            setVisibility(View.VISIBLE);
            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);
            // Remove the custom view from its container.
            videoLayout.removeView(mCustomView);
            mCustomView = null;
            videoLayout.setVisibility(View.GONE);
            mCustomViewCallback.onCustomViewHidden();
            mEntryViewMgr.onEndVideoFullScreen();
        }
    }
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Context context = getContext();
            try {
                if (url.startsWith(Constants.FILE_SCHEME)) {
                    File file = new File(url.replace(Constants.FILE_SCHEME, ""));
                    File extTmpFile = new File(context.getExternalCacheDir(), "tmp_img.jpg");
                    FileUtils.copy(file, extTmpFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(extTmpFile), "image/jpeg");
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                }
            } catch (ActivityNotFoundException e) {
                Toast.makeText(context, R.string.cant_open_link, Toast.LENGTH_SHORT).show();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    public interface EntryViewManager {
        public void onClickEnclosure();

        public void onStartVideoFullScreen();

        public void onEndVideoFullScreen();

        public FrameLayout getVideoLayout();
    }

    private class JavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "injectedJSObject";
        }


        @JavascriptInterface
        public void onClickEnclosure() {
        	mEntryViewMgr.onClickEnclosure();
        }
    }
}
