package com.minhajapps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
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

    /*
     * Keep this file in the Minhaj-Apps1 repository.
     * The GitHub Action updates it automatically when the website logo changes.
     */
    private static final String UPDATE_JSON_URL =
            "https://raw.githubusercontent.com/mdmahinafrad-pixel/Minhaj-Apps1/main/update.json";

    private WebView web;
    private long updateDownloadId = -1L;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (id == updateDownloadId) {
                    installDownloadedApk(id);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
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
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(web, true);

        web.setBackgroundColor(Color.TRANSPARENT);
        web.setWebChromeClient(new WebChromeClient());

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

        web.setDownloadListener(new DownloadListener() {
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

        registerUpdateReceiver();
        web.loadUrl(SITE_URL);

        // Check the online update file after the website starts.
        checkForUpdate();
    }

    private void registerUpdateReceiver() {
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, filter);
        }
    }

    private void checkForUpdate() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(UPDATE_JSON_URL + "?t=" + System.currentTimeMillis());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setUseCaches(false);

                InputStream input = connection.getInputStream();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(input, "UTF-8"));

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONObject data = new JSONObject(result.toString());

                int onlineCode = data.optInt("versionCode", 1);
                String onlineName = data.optString("versionName", "");
                String apkUrl = data.optString("apkUrl", "");
                String notes = data.optString("notes", "নতুন আপডেট পাওয়া গেছে।");

                int currentCode = BuildConfig.VERSION_CODE;

                if (onlineCode > currentCode && !apkUrl.isEmpty()) {
                    runOnUiThread(() ->
                            showUpdateDialog(onlineCode, onlineName, apkUrl, notes)
                    );
                }

            } catch (Exception ignored) {
                // No internet/update file: continue opening the website normally.
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void showUpdateDialog(
            int onlineCode,
            String onlineName,
            String apkUrl,
            String notes) {

        new AlertDialog.Builder(this)
                .setTitle("MINHAJ APPS Update")
                .setMessage(
                        "নতুন Version " + onlineName + " পাওয়া গেছে।\n\n" +
                        notes + "\n\n" +
                        "Update দিলে নতুন APK ইনস্টল হবে এবং নতুন Logo/Icon আসবে।"
                )
                .setNegativeButton("পরে", null)
                .setPositiveButton("Update", (dialog, which) ->
                        downloadUpdate(apkUrl, onlineName)
                )
                .setCancelable(true)
                .show();
    }

    private void downloadUpdate(String apkUrl, String versionName) {
        try {
            DownloadManager.Request request =
                    new DownloadManager.Request(Uri.parse(apkUrl));

            request.setTitle("MINHAJ APPS " + versionName);
            request.setDescription("নতুন MINHAJ APPS আপডেট ডাউনলোড হচ্ছে");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );
            request.setMimeType("application/vnd.android.package-archive");
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "MINHAJ-APPS-" + versionName.replace(".", "_") + ".apk"
            );

            DownloadManager manager =
                    (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            updateDownloadId = manager.enqueue(request);

            Toast.makeText(
                    this,
                    "Update download শুরু হয়েছে",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Update download শুরু করা যায়নি",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void installDownloadedApk(long downloadId) {
        try {
            DownloadManager manager =
                    (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            Uri apkUri = manager.getUriForDownloadedFile(downloadId);

            if (apkUri == null) {
                Toast.makeText(this, "APK পাওয়া যায়নি", Toast.LENGTH_LONG).show();
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && !getPackageManager().canRequestPackageInstalls()) {

                Intent settings = new Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())
                );
                startActivity(settings);

                Toast.makeText(
                        this,
                        "এই অ্যাপের জন্য Install unknown apps অনুমতি দিন, তারপর APK ইনস্টল করুন।",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(
                    apkUri,
                    "application/vnd.android.package-archive"
            );
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(install);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "APK installer পাওয়া যায়নি", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "APK install করা যায়নি", Toast.LENGTH_LONG).show();
        }
    }

    private boolean handleUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;

        String lower = url.toLowerCase();

        if (lower.startsWith("tg://")
                || lower.startsWith("telegram://")
                || lower.startsWith("intent://")
                || lower.startsWith("market://")
                || lower.contains("t.me/")
                || lower.contains("telegram.me/")
                || lower.contains("play.google.com/store")) {
            openExternal(url);
            return true;
        }

        if (looksLikeDownload(url)) {
            downloadFile(url, null, null, null);
            return true;
        }

        if (lower.startsWith("https://mdmahinafrad-pixel.github.io/minhaj-apps/")
                || lower.startsWith("http://mdmahinafrad-pixel.github.io/minhaj-apps/")) {
            return false;
        }

        if (lower.startsWith("http://") || lower.startsWith("https://")) {
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
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
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
            DownloadManager.Request request =
                    new DownloadManager.Request(Uri.parse(url));

            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setTitle(
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
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

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
            );

            DownloadManager manager =
                    (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            manager.enqueue(request);

            Toast.makeText(
                    this,
                    "ডাউনলোড শুরু হয়েছে",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            openExternal(url);
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
        try {
            unregisterReceiver(downloadReceiver);
        } catch (Exception ignored) {}

        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }

        super.onDestroy();
    }
                    }
