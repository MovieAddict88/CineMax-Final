package my.cinemax.app.free.Utils;

import android.content.Context;
import android.util.Log;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Network Cache Manager - HTTP Response Caching Layer
 * 
 * Provides HTTP response caching at the network level using OkHttp interceptors.
 * Caches API responses, images, and other network resources efficiently.
 * 
 * Features:
 * - HTTP response caching with proper headers
 * - Automatic cache validation
 * - Network request/response logging
 * - Configurable cache sizes and policies
 * - Offline support with stale cache
 */
public class NetworkCacheManager {
    
    private static final String TAG = "NetworkCacheManager";
    private static NetworkCacheManager instance;
    
    // Cache configuration
    private static final long CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_AGE_SECONDS = 60 * 60 * 24; // 24 hours
    private static final int MAX_STALE_SECONDS = 60 * 60 * 24 * 7; // 7 days
    
    private final Context context;
    private final OkHttpClient httpClient;
    private final Cache cache;
    
    // Statistics
    private long totalRequests = 0;
    private long cacheHits = 0;
    private long networkRequests = 0;
    private long totalBytesTransferred = 0;
    
    private NetworkCacheManager(Context context) {
        this.context = context.getApplicationContext();
        
        // Initialize cache
        File cacheDir = new File(context.getCacheDir(), "http_cache");
        cache = new Cache(cacheDir, CACHE_SIZE);
        
        // Build HTTP client with caching
        httpClient = createHttpClient();
        
        Log.d(TAG, "NetworkCacheManager initialized - Cache: " + (CACHE_SIZE / 1024 / 1024) + "MB");
    }
    
    public static synchronized NetworkCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkCacheManager(context);
        }
        return instance;
    }
    
    /**
     * Create HTTP client with caching configuration
     */
    private OkHttpClient createHttpClient() {
        // Create logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
            Log.d(TAG, "HTTP: " + message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        
        // Create cache interceptor
        Interceptor cacheInterceptor = chain -> {
            Request request = chain.request();
            
            // Add cache headers for GET requests
            if (request.method().equals("GET")) {
                request = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build();
            }
            
            Response response = chain.proceed(request);
            
            // Add cache headers to response
            return response.newBuilder()
                .header("Cache-Control", "public, max-age=" + MAX_AGE_SECONDS)
                .header("Cache-Control", "public, max-stale=" + MAX_STALE_SECONDS)
                .build();
        };
        
        // Create offline interceptor
        Interceptor offlineInterceptor = chain -> {
            Request request = chain.request();
            
            if (!isNetworkAvailable()) {
                // Use cache only when offline
                request = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build();
            }
            
            return chain.proceed(request);
        };
        
        return new OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(offlineInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * Check if network is available
     */
    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get HTTP client with caching
     */
    public OkHttpClient getHttpClient() {
        return httpClient;
    }
    
    /**
     * Make HTTP request with caching
     */
    public void makeRequest(String url, NetworkCallback callback) {
        totalRequests++;
        
        Request request = new Request.Builder()
            .url(url)
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        totalBytesTransferred += responseBody.length();
                        
                        // Check if response came from cache
                        Response cacheResponse = response.cacheResponse();
                        Response networkResponse = response.networkResponse();
                        
                        if (cacheResponse != null && networkResponse == null) {
                            cacheHits++;
                            Log.d(TAG, "Response from cache: " + url);
                            if (callback != null) {
                                callback.onSuccess(responseBody, "cache");
                            }
                        } else {
                            networkRequests++;
                            Log.d(TAG, "Response from network: " + url);
                            if (callback != null) {
                                callback.onSuccess(responseBody, "network");
                            }
                        }
                    } else {
                        Log.e(TAG, "HTTP error: " + response.code() + " for " + url);
                        if (callback != null) {
                            callback.onError("HTTP " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response for " + url, e);
                    if (callback != null) {
                        callback.onError("Error: " + e.getMessage());
                    }
                }
            }
            
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network failure for " + url, e);
                if (callback != null) {
                    callback.onError("Network error: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Make synchronous HTTP request with caching
     */
    public String makeSyncRequest(String url) throws IOException {
        totalRequests++;
        
        Request request = new Request.Builder()
            .url(url)
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                totalBytesTransferred += responseBody.length();
                
                // Check if response came from cache
                Response cacheResponse = response.cacheResponse();
                Response networkResponse = response.networkResponse();
                
                if (cacheResponse != null && networkResponse == null) {
                    cacheHits++;
                    Log.d(TAG, "Sync response from cache: " + url);
                } else {
                    networkRequests++;
                    Log.d(TAG, "Sync response from network: " + url);
                }
                
                return responseBody;
            } else {
                throw new IOException("HTTP " + response.code());
            }
        }
    }
    
    /**
     * Download file with caching
     */
    public void downloadFile(String url, File destination, DownloadCallback callback) {
        totalRequests++;
        
        Request request = new Request.Builder()
            .url(url)
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        // Create parent directories
                        File parent = destination.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        
                        // Download file
                        try (InputStream inputStream = response.body().byteStream();
                             FileOutputStream outputStream = new FileOutputStream(destination)) {
                            
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            long totalBytes = 0;
                            
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                                totalBytes += bytesRead;
                                
                                if (callback != null) {
                                    callback.onProgress(totalBytes, -1); // Unknown total size
                                }
                            }
                            
                            totalBytesTransferred += totalBytes;
                            
                            // Check if response came from cache
                            Response cacheResponse = response.cacheResponse();
                            Response networkResponse = response.networkResponse();
                            
                            if (cacheResponse != null && networkResponse == null) {
                                cacheHits++;
                                Log.d(TAG, "File downloaded from cache: " + url);
                                if (callback != null) {
                                    callback.onSuccess(destination, "cache");
                                }
                            } else {
                                networkRequests++;
                                Log.d(TAG, "File downloaded from network: " + url);
                                if (callback != null) {
                                    callback.onSuccess(destination, "network");
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "HTTP error: " + response.code() + " for " + url);
                        if (callback != null) {
                            callback.onError("HTTP " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error downloading file: " + url, e);
                    if (callback != null) {
                        callback.onError("Error: " + e.getMessage());
                    }
                }
            }
            
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network failure for file download: " + url, e);
                if (callback != null) {
                    callback.onError("Network error: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Preload URLs in background
     */
    public void preloadUrls(java.util.List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        
        Log.d(TAG, "Preloading " + urls.size() + " URLs");
        
        for (String url : urls) {
            makeRequest(url, new NetworkCallback() {
                @Override
                public void onSuccess(String response, String source) {
                    Log.v(TAG, "Preloaded: " + url + " from " + source);
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "Failed to preload: " + url + " - " + error);
                }
            });
        }
    }
    
    /**
     * Clear HTTP cache
     */
    public void clearCache() {
        try {
            cache.evictAll();
            Log.d(TAG, "HTTP cache cleared");
        } catch (IOException e) {
            Log.e(TAG, "Error clearing HTTP cache", e);
        }
    }
    
    /**
     * Get cache statistics
     */
    public NetworkCacheStats getCacheStats() {
        long cacheSize = 0;
        try {
            cacheSize = cache.size();
        } catch (IOException e) {
            Log.e(TAG, "Error getting cache size", e);
        }
        
        return new NetworkCacheStats(
            totalRequests,
            cacheHits,
            networkRequests,
            totalBytesTransferred,
            getHitRate(),
            cacheSize,
            cache.hitCount(),
            0, // OkHttp Cache doesn't have missCount()
            cache.requestCount()
        );
    }
    
    /**
     * Calculate hit rate
     */
    public double getHitRate() {
        return totalRequests > 0 ? (double) cacheHits / totalRequests : 0.0;
    }
    
    /**
     * Get cache size in bytes
     */
    public long getCacheSize() {
        try {
            return cache.size();
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * Get cache directory size
     */
    public long getCacheDirectorySize() {
        File cacheDir = new File(context.getCacheDir(), "http_cache");
        return getDirectorySize(cacheDir);
    }
    
    /**
     * Calculate directory size recursively
     */
    private long getDirectorySize(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    }
                }
            }
        }
        return size;
    }
    
    /**
     * Shutdown the network cache manager
     */
    public void shutdown() {
        try {
            cache.close();
            Log.d(TAG, "NetworkCacheManager shutdown");
        } catch (IOException e) {
            Log.e(TAG, "Error shutting down NetworkCacheManager", e);
        }
    }
    
    /**
     * Callback interface for network requests
     */
    public interface NetworkCallback {
        void onSuccess(String response, String source);
        void onError(String error);
    }
    
    /**
     * Callback interface for file downloads
     */
    public interface DownloadCallback {
        void onSuccess(File file, String source);
        void onProgress(long bytesDownloaded, long totalBytes);
        void onError(String error);
    }
    
    /**
     * Network cache statistics
     */
    public static class NetworkCacheStats {
        public final long totalRequests;
        public final long cacheHits;
        public final long networkRequests;
        public final long totalBytesTransferred;
        public final double hitRate;
        public final long cacheSize;
        public final long cacheHitCount;
        public final long cacheMissCount;
        public final long cacheRequestCount;
        
        public NetworkCacheStats(long totalRequests, long cacheHits, long networkRequests,
                               long totalBytesTransferred, double hitRate, long cacheSize,
                               long cacheHitCount, long cacheMissCount, long cacheRequestCount) {
            this.totalRequests = totalRequests;
            this.cacheHits = cacheHits;
            this.networkRequests = networkRequests;
            this.totalBytesTransferred = totalBytesTransferred;
            this.hitRate = hitRate;
            this.cacheSize = cacheSize;
            this.cacheHitCount = cacheHitCount;
            this.cacheMissCount = cacheMissCount;
            this.cacheRequestCount = cacheRequestCount;
        }
        
        @Override
        public String toString() {
            return String.format("NetworkCacheStats{requests=%d, hits=%d, network=%d, " +
                               "bytes=%dKB, hitRate=%.2f%%, cacheSize=%dKB}",
                totalRequests, cacheHits, networkRequests, totalBytesTransferred / 1024,
                hitRate * 100, cacheSize / 1024);
        }
    }
}