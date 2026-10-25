package com.rhythm.basket;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int PICK_IMAGE = 42;
    private static final String LOCAL_URL = "file:///android_asset/index.html";
    private WebView webView;
    private ValueCallback<Uri[]> pendingImage;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(247, 249, 248));
        getWindow().setNavigationBarColor(Color.rgb(247, 249, 248));
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(247, 249, 248));
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setDefaultTextEncodingName("UTF-8");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri url = r.getUrl();
                if ("file".equals(url.getScheme()) && LOCAL_URL.equals(url.toString())) return false;
                openExternal(url);
                return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (LOCAL_URL.equals(url)) return false;
                openExternal(Uri.parse(url));
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(
                    WebView v, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingImage != null) pendingImage.onReceiveValue(null);
                pendingImage = callback;
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent, "Choose product photo"), PICK_IMAGE);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    pendingImage.onReceiveValue(null);
                    pendingImage = null;
                    Toast.makeText(MainActivity.this, "No photo picker is available", Toast.LENGTH_LONG).show();
                    return true;
                }
            }
        });
        setContentView(webView);
        if (state == null) webView.loadUrl(LOCAL_URL); else webView.restoreState(state);
    }

    private void openExternal(Uri url) {
        if (!("http".equals(url.getScheme()) || "https".equals(url.getScheme()))) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, url)); }
        catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && pendingImage != null) {
            Uri[] selected = resultCode == RESULT_OK && data != null && data.getData() != null
                ? new Uri[]{data.getData()} : null;
            pendingImage.onReceiveValue(selected);
            pendingImage = null;
        }
    }

    @Override public void onBackPressed() {
        webView.evaluateJavascript("window.dismissEditor && window.dismissEditor()", result -> {
            if (!"true".equals(result)) {
                if (webView.canGoBack()) webView.goBack(); else MainActivity.super.onBackPressed();
            }
        });
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        webView.saveState(state);
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() {
        if (pendingImage != null) { pendingImage.onReceiveValue(null); pendingImage = null; }
        webView.destroy();
        super.onDestroy();
    }
}
