package com.minhajapps;

import android.app.*;import android.os.*;import android.webkit.*;import android.net.*;import android.content.*;import android.view.*;import android.graphics.Color;

public class MainActivity extends Activity {
  WebView web;
  @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(246,248,252)); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); 
    web=new WebView(this); setContentView(web);
    WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setAllowContentAccess(true); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false); s.setSupportZoom(false);
    web.setWebViewClient(new WebViewClient(){ @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r){ return false; }});
    web.setDownloadListener((url,userAgent,contentDisposition,mimeType,contentLength)->{ try{ DownloadManager.Request req=new DownloadManager.Request(Uri.parse(url)); req.setMimeType(mimeType); req.addRequestHeader("User-Agent",userAgent); req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); req.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url,contentDisposition,mimeType)); ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(req); }catch(Exception e){} });
    web.loadUrl("https://mdmahinafrad-pixel.github.io/Minhaj-Apps/");
  }
  @Override public void onBackPressed(){ if(web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
