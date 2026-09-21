package com.minhajapps;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private static final String SITE_URL =
            "https://mdmahinafrad-pixel.github.io/Minhaj-Apps/";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {
                return handleUrl(url);
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition,
                                        String mimeType, long contentLength) {
                startDownload(url, userAgent, contentDisposition, mimeType);
            }
        });

        webView.addJavascriptInterface(
                new DownloadBridge(this), "MINHAJ_APP");

        webView.loadUrl(SITE_URL);
    }

    private boolean handleUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;

        String lower = url.toLowerCase();

        if (lower.startsWith("tg://")
                || lower.startsWith("telegram://")
                || lower.contains("t.me/")
                || lower.contains("telegram.me/")) {
            openExternal(url);
            return true;
        }

        if (lower.startsWith("market://")
                || lower.contains("play.google.com/store")) {
            openExternal(url);
            return true;
        }

        if (isDownloadUrl(lower)) {
            startDownload(url, null, null, null);
            return true;
        }

        if (lower.startsWith("https://mdmahinafrad-pixel.github.io/")
                || lower.startsWith("http://mdmahinafrad-pixel.github.io/")) {
            return false;
        }

        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            openExternal(url);
            return true;
        }

        return false;
    }

    private boolean isDownloadUrl(String url) {
        return url.contains(".apk")
                || url.contains(".xapk")
                || url.contains(".apks")
                || url.contains(".zip")
                || url.contains(".rar")
                || url.contains("download.php")
                || url.contains("/download/")
                || url.contains("post-download")
                || url.contains("download");
    }

    private void startDownload(String url, String userAgent,
                               String contentDisposition, String mimeType) {
        try {
            Uri uri = Uri.parse(url);

            if (uri.getScheme() == null
                    || (!uri.getScheme().equalsIgnoreCase("http")
                    && !uri.getScheme().equalsIgnoreCase("https"))) {
                openExternal(url);
                return;
            }

            DownloadManager.Request request =
                    new DownloadManager.Request(uri);

            request.setTitle(getFileName(url, contentDisposition));
            request.setDescription("MINHAJ APPS Download");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            if (mimeType != null && !mimeType.isEmpty()) {
                request.setMimeType(mimeType);
            }

            String ua = userAgent;
            if (ua == null || ua.isEmpty()) {
                ua = webView.getSettings().getUserAgentString();
            }
            if (ua != null && !ua.isEmpty()) {
                request.addRequestHeader("User-Agent", ua);
            }

            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null && !cookie.isEmpty()) {
                request.addRequestHeader("Cookie", cookie);
            }

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    getFileName(url, contentDisposition));

            DownloadManager manager =
                    (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            if (manager != null) {
                manager.enqueue(request);
            } else {
                openExternal(url);
            }
        } catch (Exception e) {
            openExternal(url);
        }
    }

    private String getFileName(String url, String contentDisposition) {
        String fileName = URLUtil.guessFileName(
                url, contentDisposition, "application/octet-stream");

        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "MINHAJ-APPS-Download";
        }

        return fileName;
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) {
        } catch (Exception ignored) {
        }
    }

    public static class DownloadBridge {
        private final MainActivity activity;

        DownloadBridge(MainActivity activity) {
            this.activity = activity;
        }

        @android.webkit.JavascriptInterface
        public void download(final String url) {
            if (url == null || url.trim().isEmpty()) return;

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.startDownload(url, null, null, null);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
            }
