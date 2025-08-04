package com.cinecraze.free.stream;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.widget.ProgressBar;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class EmbedActivity extends AppCompatActivity {
    private WebView webView;
    private FrameLayout customViewContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View mCustomView;
    private myWebChromeClient mWebChromeClient;
    private myWebViewClient mWebViewClient;
    private String url;
    private static final String TAG = "EmbedActivity";
    private ProgressBar loadingProgressBar;
    private Handler timeoutHandler = new Handler();
    private static final int LOADING_TIMEOUT = 30000; // 30 seconds timeout

    public static void start(Context context, String embedUrl) {
        Intent intent = new Intent(context, EmbedActivity.class);
        intent.putExtra("url", embedUrl);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Set landscape orientation like CinemaX
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_embed);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        url = getIntent().getStringExtra("url");

        initializeViews();
        setupWebView();
        loadUrl();
    }

    private void initializeViews() {
        customViewContainer = findViewById(R.id.customViewContainer);
        webView = findViewById(R.id.webView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
    }

    private void setupWebView() {
        mWebViewClient = new myWebViewClient();
        webView.setWebViewClient(mWebViewClient);

        mWebChromeClient = new myWebChromeClient();
        webView.setWebChromeClient(mWebChromeClient);

        // Enhanced WebView settings for better video playback (CinemaX approach)
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setSaveFormData(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // Additional settings for better video compatibility (from CinemaX)
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);  // Key CinemaX setting - prevents cached ads

        // Enable hardware acceleration for better video performance
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void loadUrl() {
        // Validate URL before loading
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Invalid video URL", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        Log.d(TAG, "Loading URL: " + url);
        
        // Enhance URL with parameters for better compatibility
        String enhancedUrl = enhanceVideoUrl(url);
        
        // Show loading indicator
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
        
        webView.loadUrl(enhancedUrl);
    }

    private String enhanceVideoUrl(String originalUrl) {
        if (originalUrl == null) return originalUrl;
        
        // Add parameters for better video server compatibility
        StringBuilder enhancedUrl = new StringBuilder(originalUrl);
        
        if (!originalUrl.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        // Common parameters for embedded video players
        enhancedUrl.append("autoplay=1")
                  .append("&muted=0")
                  .append("&controls=1")
                  .append("&rel=0")
                  .append("&showinfo=0")
                  .append("&iv_load_policy=3");
        
        return enhancedUrl.toString();
    }

    public boolean inCustomView() {
        return (mCustomView != null);
    }

    public void hideCustomView() {
        mWebChromeClient.onHideCustomView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (inCustomView()) {
            hideCustomView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up timeout handler
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        
        // Clean up WebView
        if (webView != null) {
            webView.stopLoading();
            webView.clearCache(true);
            webView.clearHistory();
            webView.loadUrl("about:blank");
            webView.destroy();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (inCustomView()) {
                hideCustomView();
                return true;
            }

            if ((mCustomView == null) && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    class myWebChromeClient extends WebChromeClient {
        private Bitmap mDefaultVideoPoster;
        private View mVideoProgressView;

        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
            onShowCustomView(view, callback);
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            mCustomView = view;
            webView.setVisibility(View.GONE);
            customViewContainer.setVisibility(View.VISIBLE);
            customViewContainer.addView(view);
            customViewCallback = callback;
        }

        @Override
        public View getVideoLoadingProgressView() {
            if (mVideoProgressView == null) {
                LayoutInflater inflater = LayoutInflater.from(EmbedActivity.this);
                mVideoProgressView = inflater.inflate(R.layout.video_progress, null);
            }
            return mVideoProgressView;
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            if (mCustomView == null)
                return;

            webView.setVisibility(View.VISIBLE);
            customViewContainer.setVisibility(View.GONE);

            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            customViewContainer.removeView(mCustomView);
            customViewCallback.onCustomViewHidden();

            mCustomView = null;
        }
    }

    class myWebViewClient extends WebViewClient {
        private int loadAttempts = 0;
        private static final int MAX_ATTEMPTS = 3;

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Navigation attempt to: " + url);
            
            // Simple CinemaX approach: only allow navigation within the same domain
            if (isWithinVideoServerDomain(url, EmbedActivity.this.url)) {
                return false; // Allow navigation within video server
            }
            
            // Block all external navigation (this prevents most ads and redirects)
            Log.d(TAG, "Blocked external navigation to: " + url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "Page started loading: " + url);
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.VISIBLE);
            }

            // Set timeout for loading
            timeoutHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (loadingProgressBar != null && loadingProgressBar.getVisibility() == View.VISIBLE) {
                        Toast.makeText(EmbedActivity.this,
                            "Video loading timeout. Please try a different source.",
                            Toast.LENGTH_LONG).show();
                        if (loadingProgressBar != null) {
                            loadingProgressBar.setVisibility(View.GONE);
                        }
                    }
                }
            }, LOADING_TIMEOUT);

            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG, "Page finished loading: " + url);
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.GONE);
            }

            // Remove timeout callback
            timeoutHandler.removeCallbacksAndMessages(null);

            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.e(TAG, "WebView error: " + errorCode + " - " + description + " for URL: " + failingUrl);

            loadAttempts++;
            if (loadAttempts < MAX_ATTEMPTS) {
                // Try fallback server
                String fallbackUrl = getFallbackUrl(failingUrl);
                if (fallbackUrl != null && !fallbackUrl.equals(failingUrl)) {
                    Log.d(TAG, "Trying fallback URL: " + fallbackUrl);
                    view.loadUrl(fallbackUrl);
                    return;
                }
            }

            // Show error message to user
            Toast.makeText(EmbedActivity.this,
                "Video server unavailable. Please try a different source.",
                Toast.LENGTH_LONG).show();

            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            Log.e(TAG, "HTTP Error: " + errorResponse.getStatusCode() + " for URL: " + request.getUrl());

            loadAttempts++;
            if (loadAttempts < MAX_ATTEMPTS) {
                // Try fallback server
                String fallbackUrl = getFallbackUrl(request.getUrl().toString());
                if (fallbackUrl != null && !fallbackUrl.equals(request.getUrl().toString())) {
                    Log.d(TAG, "Trying fallback URL: " + fallbackUrl);
                    view.loadUrl(fallbackUrl);
                    return;
                }
            }

            // Show error message to user
            Toast.makeText(EmbedActivity.this,
                "Video server unavailable. Please try a different source.",
                Toast.LENGTH_LONG).show();

            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.e(TAG, "SSL Error: " + error.toString());
            // Continue loading despite SSL errors for video servers (CinemaX approach)
            handler.proceed();
        }

        /**
         * Get fallback URL when primary server fails
         */
        private String getFallbackUrl(String failingUrl) {
            // Since VidJoy and VidSrc are separate sources, we don't need fallback logic
            return null;
        }
        
        /**
         * Helper method for domain validation (CinemaX approach)
         */
        private boolean isWithinVideoServerDomain(String url, String currentVideoUrl) {
            try {
                java.net.URI uri = new java.net.URI(url);
                java.net.URI currentUri = new java.net.URI(currentVideoUrl);
                return uri.getHost().equals(currentUri.getHost());
            } catch (Exception e) {
                return false;
            }
        }
    }
}