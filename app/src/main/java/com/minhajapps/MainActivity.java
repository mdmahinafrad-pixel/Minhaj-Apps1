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

        // Always load the current live website.
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

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
            public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimeType,
                    long contentLength) {

                downloadFile(url, userAgent, contentDisposition, mimeType);
            }
        });

        webView.loadUrl(SITE_URL);
    }

    private boolean handleUrl(String url) {
        if (url == null || url.length() == 0) {
            return false;
        }

        String lower = url.toLowerCase();

        // Telegram links
        if (lower.startsWith("tg://")
                || lower.startsWith("telegram://")
                || lower.contains("t.me/")
                || lower.contains("telegram.me/")) {

            openExternal(url);
            return true;
        }

        // Play Store links
        if (lower.startsWith("market://")
                || lower.contains("play.google.com/store")) {

            openExternal(url);
            return true;
        }

        // File/download links
        if (isDownloadUrl(lower)) {
            downloadFile(url, null, null, null);
            return true;
        }

        // Keep the MINHAJ APPS website inside the app.
        if (lower.startsWith("https://mdmahinafrad-pixel.github.io/")
                || lower.startsWith("http://mdmahinafrad-pixel.github.io/")) {
            return false;
        }

        // Other external websites open outside the app.
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
                || url.contains("post-download");
    }

    private void downloadFile(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType) {

        try {
            DownloadManager.Request request =
                    new DownloadManager.Request(Uri.parse(url));

            request.setTitle("MINHAJ APPS Download");
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            if (mimeType != null && mimeType.length() > 0) {
                request.setMimeType(mimeType);
            }

            if (userAgent != null) {
                request.addRequestHeader("User-Agent", userAgent);
            }

            String cookies = CookieManager.getInstance()
                    .getCookie(url);

            if (cookies != null) {
                request.addRequestHeader("Cookie", cookies);
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
            // If Android DownloadManager cannot handle the URL,
            // open it in the browser instead.
            openExternal(url);
        }
    }

    private String getFileName(String url, String contentDisposition) {

        if (contentDisposition != null) {
            String lower = contentDisposition.toLowerCase();

            int index = lower.indexOf("filename=");

            if (index >= 0) {
                String name = contentDisposition
                        .substring(index + 9)
                        .replace("\"", "")
                        .trim();

                if (name.length() > 0) {
                    return name;
                }
            }
        }

        try {
            String path = Uri.parse(url).getPath();

            if (path != null) {
                int slash = path.lastIndexOf('/');

                if (slash >= 0 && slash < path.length() - 1) {
                    String name = path.substring(slash + 1);

                    if (name.length() > 0) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return "MINHAJ-APPS-Download";
    }

    private void openExternal(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No compatible external app/browser was found.
        } catch (Exception ignored) {
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
