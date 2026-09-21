package com.minhajapps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
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
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String SITE_URL =
            "https://mdmahinafrad-pixel.github.io/Minhaj-Apps/";

    private static final String UPDATE_JSON_URL =
            "https://raw.githubusercontent.com/mdmahinafrad-pixel/Minhaj-Apps1/main/update.json";

    private WebView webView;
    private long updateDownloadId = -1L;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(
                    DownloadManager.EXTRA_DOWNLOAD_ID, -1L);

            if (id == updateDownloadId) {
                installDownloadedApk(id);
            }
        }
    };

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
            public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimeType,
                    long contentLength) {

                startFileDownload(
                        url, userAgent, contentDisposition, mimeType);
            }
        });

        webView.addJavascriptInterface(
                new DownloadBridge(this), "MINHAJ_APP");

        webView.loadUrl(SITE_URL);

        try {
            registerReceiver(
                    downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } catch (Exception ignored) {
        }

        // Check for a newer APK shortly after the website opens.
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForUpdate();
            }
        }, 1200);
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
            startFileDownload(url, null, null, null);
            return true;
        }

        if (lower.startsWith(
                "https://mdmahinafrad-pixel.github.io/")
                || lower.startsWith(
                "http://mdmahinafrad-pixel.github.io/")) {
            return false;
        }

        if (lower.startsWith("http://")
                || lower.startsWith("https://")) {
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

    private void startFileDownload(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType) {

        try {
            Uri uri = Uri.parse(url);

            if (uri.getScheme() == null
                    || (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme()))) {
                openExternal(url);
                return;
            }

            DownloadManager.Request request =
                    new DownloadManager.Request(uri);

            String fileName = URLUtil.guessFileName(
                    url, contentDisposition, "application/octet-stream");

            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "MINHAJ-APPS-Download";
            }

            request.setTitle(fileName);
            request.setDescription("MINHAJ APPS Download");
            request.setNotificationVisibility(
                    DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
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

            String cookie =
                    CookieManager.getInstance().getCookie(url);

            if (cookie != null && !cookie.isEmpty()) {
                request.addRequestHeader("Cookie", cookie);
            }

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager =
                    (DownloadManager) getSystemService(
                            Context.DOWNLOAD_SERVICE);

            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(
                        this,
                        "Download শুরু হয়েছে — Downloads ফোল্ডার দেখুন",
                        Toast.LENGTH_SHORT).show();
            } else {
                openExternal(url);
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Download শুরু করা যায়নি",
                    Toast.LENGTH_SHORT).show();
            openExternal(url);
        }
    }

    private void checkForUpdate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(UPDATE_JSON_URL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    connection.setUseCaches(false);
                    connection.setRequestProperty(
                            "Cache-Control", "no-cache");

                    InputStream input =
                            connection.getInputStream();

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(input));

                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    reader.close();

                    JSONObject json =
                            new JSONObject(result.toString());

                    final int latestCode =
                            json.optInt("versionCode", 0);

                    final String latestName =
                            json.optString("versionName", "");

                    final String apkUrl =
                            json.optString("apkUrl", "");

                    int currentCode = getCurrentVersionCode();

                    if (latestCode > currentCode
                            && !apkUrl.trim().isEmpty()) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showUpdateDialog(
                                        latestName, apkUrl);
                            }
                        });
                    }

                } catch (Exception ignored) {
                    // Update check must never stop the website.
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo info =
                    getPackageManager().getPackageInfo(
                            getPackageName(), 0);

            if (android.os.Build.VERSION.SDK_INT
                    >= android.os.Build.VERSION_CODES.P) {
                return (int) info.getLongVersionCode();
            }

            return info.versionCode;

        } catch (Exception e) {
            return 0;
        }
    }

    private void showUpdateDialog(
            final String versionName,
            final String apkUrl) {

        new AlertDialog.Builder(this)
                .setTitle("MINHAJ APPS Update")
                .setMessage(
                        "নতুন Version " + versionName
                                + " পাওয়া গেছে।\n\n"
                                + "Update করলে নতুন APK ডাউনলোড হবে।")
                .setNegativeButton("পরে", null)
                .setPositiveButton("Update",
                        (dialog, which) -> downloadUpdateApk(apkUrl))
                .setCancelable(true)
                .show();
    }

    private void downloadUpdateApk(String apkUrl) {
        try {
            DownloadManager.Request request =
                    new DownloadManager.Request(
                            Uri.parse(apkUrl));

            request.setTitle("MINHAJ APPS Update");
            request.setDescription("নতুন APK ডাউনলোড হচ্ছে...");
            request.setMimeType(
                    "application/vnd.android.package-archive");
            request.setNotificationVisibility(
                    DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "MINHAJ-APPS-Update.apk");

            DownloadManager manager =
                    (DownloadManager) getSystemService(
                            Context.DOWNLOAD_SERVICE);

            if (manager != null) {
                updateDownloadId = manager.enqueue(request);

                Toast.makeText(
                        this,
                        "Update APK ডাউনলোড হচ্ছে...",
                        Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Update download শুরু করা যায়নি",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void installDownloadedApk(long downloadId) {
        try {
            DownloadManager manager =
                    (DownloadManager) getSystemService(
                            Context.DOWNLOAD_SERVICE);

            if (manager == null) return;

            android.database.Cursor cursor =
                    manager.query(
                            new DownloadManager.Query()
                                    .setFilterById(downloadId));

            if (cursor == null) return;

            if (cursor.moveToFirst()) {
                int status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_STATUS));

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    Uri uri = manager.getUriForDownloadedFile(
                            downloadId);

                    if (uri != null) {
                        Intent installIntent =
                                new Intent(
                                        Intent.ACTION_VIEW);

                        installIntent.setDataAndType(
                                uri,
                                "application/vnd.android.package-archive");

                        installIntent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        installIntent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK);

                        try {
                            startActivity(installIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(
                                    this,
                                    "APK install করার App পাওয়া যায়নি",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                } else {
                    Toast.makeText(
                            this,
                            "Update APK download ব্যর্থ হয়েছে",
                            Toast.LENGTH_LONG).show();
                }
            }

            cursor.close();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "APK install শুরু করা যায়নি",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openExternal(String url) {
        try {
            startActivity(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)));
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
                    activity.startFileDownload(
                            url, null, null, null);
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
        try {
            unregisterReceiver(downloadReceiver);
        } catch (Exception ignored) {
        }

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
