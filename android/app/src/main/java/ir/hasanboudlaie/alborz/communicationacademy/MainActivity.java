package ir.hasanboudlaie.alborz.communicationacademy;

import android.app.Activity;
import android.os.Bundle;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * True Android package identity (not a browser-installed PWA).
 * Renders the single canonical academy online: no lesson content is duplicated.
 * All navigation within the academy stays in the app, and external links open outside.
 */
public class MainActivity extends Activity {
    private static final String ACADEMY_URL =
      "https://hassnboudlaie.github.io/hasan-boudlaie/alborz-communication-skills/";
    private static final String ACADEMY_HOST = "hassnboudlaie.github.io";
    private static final String ACADEMY_PATH = "/hasan-boudlaie/alborz-communication-skills/";
    private static final int FILE_PICKER_REQUEST = 4201;

    private FrameLayout root;
    private WebView web;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private android.webkit.ValueCallback<Uri[]> fileChooserCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff07593e);
        getWindow().setNavigationBarColor(0xfff3f7f4);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        CookieManager.getInstance().setAcceptCookie(true);

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                if ("https".equalsIgnoreCase(uri.getScheme())
                   && ACADEMY_HOST.equalsIgnoreCase(uri.getHost())
                   && uri.getPath() != null
                   && uri.getPath().startsWith(ACADEMY_PATH)) return false;
                openExternal(uri);
                return true;
            }
            @Override public void onReceivedError(WebView view,
                    WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this,
                       "ارتباط با آکادمی برقرار نشد؛ اتصال اینترنت را بررسی کنید.",
                       Toast.LENGTH_LONG).show();
                }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreenView != null) {callback.onCustomViewHidden();return;}
                fullscreenView=view; fullscreenCallback=callback;
                web.setVisibility(View.GONE);
                root.addView(view,new FrameLayout.LayoutParams(-1,-1));
            }
            @Override public void onHideCustomView() {
                if (fullscreenView == null) return;
                root.removeView(fullscreenView);
                fullscreenView=null;
                web.setVisibility(View.VISIBLE);
                if (fullscreenCallback != null) fullscreenCallback.onCustomViewHidden();
                fullscreenCallback=null;
            }
            @Override public boolean onShowFileChooser(WebView view,
                    android.webkit.ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileChooserCallback!=null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback=callback;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent,FILE_PICKER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException error) {
                    fileChooserCallback=null;
                    callback.onReceiveValue(null);
                    return false;
                }
            }
        });
        web.setDownloadListener((url,userAgent,contentDisposition,mimeType,contentLength)->{
            try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))); }
            catch (Exception error) {Toast.makeText(this,"بازکردن فایل ممکن نشد",Toast.LENGTH_SHORT).show();}
        });
        if (state==null) web.loadUrl(ACADEMY_URL);
        else web.restoreState(state);
    }

    private void openExternal(Uri uri) {
        try {startActivity(new Intent(Intent.ACTION_VIEW,uri));}
        catch (Exception ex) {Toast.makeText(this,"بازکردن پیوند ممکن نشد",Toast.LENGTH_SHORT).show();}
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode==FILE_PICKER_REQUEST && fileChooserCallback!=null) {
            fileChooserCallback.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode,data));
            fileChooserCallback=null;
        }
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        if(web!=null) web.saveState(outState);
        super.onSaveInstanceState(outState);
    }
    @Override public void onBackPressed() {
        if (fullscreenView!=null) {
            if (fullscreenCallback!=null) fullscreenCallback.onCustomViewHidden();
            root.removeView(fullscreenView);
            fullscreenView=null;
            web.setVisibility(View.VISIBLE);
        } else if(web!=null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (web!=null) {
            root.removeView(web);
            web.stopLoading();web.setWebChromeClient(null);web.setWebViewClient(null);
            web.destroy();web=null;
        }
        super.onDestroy();
    }
}
