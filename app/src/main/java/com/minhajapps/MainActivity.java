package com.minhajapps;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String SITE_URL =
            "https://mdmahinafrad-pixel.github.io/Minhaj-Apps/";

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();

        // Website needs JavaScript, storage and modern WebView features.
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);

        // Always revalidate the live GitHub Pages website so website updates
        // are picked up without rebuilding the APK.
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(web, true);

        web.setBackgroundColor(Color.TRANSPARENT);
        web.setWebChromeClient(new WebChromeClient());

        // Helps Android WebView behave correctly with links opened by JS,
        // target="_blank", Telegram links and download links.
        web.setWebViewClient(new WebViewClient() {

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

        // HTML/JS downloads go through Android's Download Manager.
        web.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimeType,
                    long contentLength) {

                downloadFile(
                        url,
                        userAgent,
                        contentDisposition,
                        mimeType
                );
            }
        });

        // Optional JS bridge for future website buttons.
        web.addJavascriptInterface(new AppBridge(), "MINHAJ_APP");

        web.loadUrl(SITE_URL);
    }

    private boolean handleUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lower = url.toLowerCase();

        // Telegram / external application schemes.
        if (lower.startsWith("tg://")
                || lower.startsWith("telegram://")
                || lower.startsWith("intent://")
                || lower.startsWith("market://")) {
            openExternal(url);
            return true;
        }

        // Telegram HTTPS links should open outside the WebView.
        if (lower.contains("t.me/")
                || lower.contains("telegram.me/")) {
            openExternal(url);
            return true;
        }

        // Google Play links should open in Play Store/browser.
        if (lower.contains("play.google.com/store")) {
            openExternal(url);
            return true;
        }

        // Known direct-download URLs.
        if (looksLikeDownload(url)) {
            downloadFile(url, null, null, null);
            return true;
        }

        // Keep the MINHAJ APPS website inside the app.
        if (lower.startsWith("https://mdmahinafrad-pixel.github.io/minhaj-apps/")
                || lower.startsWith("http://mdmahinafrad-pixel.github.io/minhaj-apps/")) {
            return false;
        }

        // Other external websites open in Chrome/browser.
        if (lower.startsWith("http://")
                || lower.startsWith("https://")) {
            openExternal(url);
            return true;
        }

        return false;
    }

    private boolean looksLikeDownload(String url) {
        String lower = url.toLowerCase();

        return lower.contains(".apk")
                || lower.contains(".xapk")
                || lower.contains(".apks")
                || lower.contains(".zip")
                || lower.contains(".rar")
                || lower.contains("download.php")
                || lower.contains("/download/")
                || lower.contains("post-download");
    }

    private void openExternal(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    this,
                    "এই লিংকটি খোলার জন্য কোনো অ্যাপ পাওয়া যায়নি",
                    Toast.LENGTH_SHORT
            ).show();
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "লিংকটি খোলা যাচ্ছে না",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void downloadFile(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType) {

        try {
            Uri uri = Uri.parse(url);
            DownloadManager.Request request =
                    new DownloadManager.Request(uri);

            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setTitle(
                    URLUtil.guessFileName(
                            url,
                            contentDisposition,
                            mimeType
                    )
            );

            request.setDescription("MINHAJ APPS থেকে ডাউনলোড হচ্ছে");

            if (mimeType != null && !mimeType.isEmpty()) {
                request.setMimeType(mimeType);
            }

            if (userAgent != null && !userAgent.isEmpty()) {
                request.addRequestHeader("User-Agent", userAgent);
            }

            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null && !cookie.isEmpty()) {
                request.addRequestHeader("Cookie", cookie);
            }

            String fileName = URLUtil.guessFileName(
                    url,
                    contentDisposition,
                    mimeType
            );

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
            );

            DownloadManager manager =
                    (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            if (manager == null) {
                throw new IllegalStateException("DownloadManager unavailable");
            }

            manager.enqueue(request);

            Toast.makeText(
                    this,
                    "ডাউনলোড শুরু হয়েছে",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            // If a site requires its own download page, open that page
            // in the browser instead of silently failing.
            try {
                Intent browser =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browser);
            } catch (Exception ignored) {
                Toast.makeText(
                        this,
                        "ডাউনলোড শুরু করা যায়নি",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    // Small bridge reserved for future website-specific Android actions.
    private class AppBridge {

        @JavascriptInterface
        public void openExternal(String url) {
            MainActivity.this.openExternal(url);
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
                }
