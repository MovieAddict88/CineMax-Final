package my.cinemax.app.free.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import my.cinemax.app.free.entity.JsonApiResponse;
import my.cinemax.app.free.entity.Poster;
import my.cinemax.app.free.entity.Channel;
import my.cinemax.app.free.entity.Actor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Simplified Cache Manager using LruCache, disk caching with GZIP, and OkHttp network caching
 * Based on proven caching patterns for Android apps
 */
public class SimpleCacheManager {
    private static final String TAG = "SimpleCacheManager";
    private static SimpleCacheManager instance;
    
    // Memory cache (50MB)
    private final LruCache<String, Object> memoryCache;
    
    // Image cache using WeakReference
    private final Map<String, WeakReference<Bitmap>> imageCache;
    
    // Thread safety
    private final Object cacheLock = new Object();
    
    // OkHttp client with network caching
    private OkHttpClient okHttpClient;
    
    // Gson for JSON serialization
    private final Gson gson;
    
    // Context
    private Context context;
    
    private SimpleCacheManager() {
        // Initialize memory cache (50MB)
        memoryCache = new LruCache<String, Object>(50 * 1024 * 1024) {
            @Override
            protected int sizeOf(String key, Object value) {
                if (value instanceof List) {
                    return ((List<?>) value).size() * 500; // Approx bytes per item
                }
                return 1;
            }
        };
        
        // Initialize image cache
        imageCache = new ConcurrentHashMap<>();
        
        // Initialize Gson
        gson = new Gson();
        
        Log.d(TAG, "SimpleCacheManager initialized");
    }
    
    public static synchronized SimpleCacheManager getInstance() {
        if (instance == null) {
            instance = new SimpleCacheManager();
        }
        return instance;
    }
    
    /**
     * Initialize the cache manager
     */
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        
        // Setup OkHttp with network caching (100MB)
        File cacheDir = new File(context.getCacheDir(), "http_cache");
        Cache cache = new Cache(cacheDir, 100 * 1024 * 1024);
        
        okHttpClient = new OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(chain -> {
                Response response = chain.proceed(chain.request());
                return response.newBuilder()
                    .header("Cache-Control", "public, max-age=3600") // 1 hour cache
                    .build();
            })
            .build();
        
        Log.d(TAG, "SimpleCacheManager initialized with context");
    }
    
    // ==================== MEMORY CACHE ====================
    
    /**
     * Get from memory cache
     */
    public Object cacheGet(String key) {
        synchronized (cacheLock) {
            return memoryCache.get(key);
        }
    }
    
    /**
     * Put in memory cache
     */
    public void cachePut(String key, Object value) {
        synchronized (cacheLock) {
            memoryCache.put(key, value);
        }
    }
    
    /**
     * Remove from memory cache
     */
    public void cacheRemove(String key) {
        synchronized (cacheLock) {
            memoryCache.remove(key);
        }
    }
    
    // ==================== DISK CACHE ====================
    
    /**
     * Save compressed JSON to disk
     */
    public void saveToDisk(String key, String json) {
        try {
            File file = new File(context.getFilesDir(), key + ".json.gz");
            try (FileOutputStream fos = new FileOutputStream(file);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos);
                 OutputStreamWriter writer = new OutputStreamWriter(gzos)) {
                writer.write(json);
            }
            Log.d(TAG, "Saved to disk: " + key);
        } catch (IOException e) {
            Log.e(TAG, "Error saving to disk: " + key, e);
        }
    }
    
    /**
     * Load from disk with 24h expiry
     */
    public String loadFromDisk(String key) {
        try {
            File file = new File(context.getFilesDir(), key + ".json.gz");
            if (file.exists() && (System.currentTimeMillis() - file.lastModified() < 86_400_000)) {
                try (FileInputStream fis = new FileInputStream(file);
                     GZIPInputStream gzis = new GZIPInputStream(fis);
                     InputStreamReader reader = new InputStreamReader(gzis);
                     BufferedReader br = new BufferedReader(reader)) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    Log.d(TAG, "Loaded from disk: " + key);
                    return sb.toString();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading from disk: " + key, e);
        }
        return null;
    }
    
    // ==================== MOVIES CACHE ====================
    
    /**
     * Get movies from cache
     */
    public List<Poster> getMovies() {
        // Try memory first
        Object cached = cacheGet("movies");
        if (cached != null) {
            Log.d(TAG, "Movies hit in memory cache");
            return (List<Poster>) cached;
        }
        
        // Try disk
        String json = loadFromDisk("movies");
        if (json != null) {
            try {
                Type type = new TypeToken<List<Poster>>(){}.getType();
                List<Poster> movies = gson.fromJson(json, type);
                cachePut("movies", movies); // Cache in memory
                Log.d(TAG, "Movies loaded from disk cache");
                return movies;
            } catch (Exception e) {
                Log.e(TAG, "Error parsing movies from disk", e);
            }
        }
        
        return null;
    }
    
    /**
     * Cache movies
     */
    public void cacheMovies(List<Poster> movies) {
        if (movies == null) return;
        
        // Cache in memory
        cachePut("movies", movies);
        
        // Cache in disk
        try {
            String json = gson.toJson(movies);
            saveToDisk("movies", json);
            Log.d(TAG, "Movies cached in memory and disk");
        } catch (Exception e) {
            Log.e(TAG, "Error caching movies", e);
        }
    }
    
    // ==================== TV SERIES CACHE ====================
    
    /**
     * Get TV series from cache
     */
    public List<Poster> getTvSeries() {
        // Try memory first
        Object cached = cacheGet("tv_series");
        if (cached != null) {
            Log.d(TAG, "TV Series hit in memory cache");
            return (List<Poster>) cached;
        }
        
        // Try disk
        String json = loadFromDisk("tv_series");
        if (json != null) {
            try {
                Type type = new TypeToken<List<Poster>>(){}.getType();
                List<Poster> series = gson.fromJson(json, type);
                cachePut("tv_series", series); // Cache in memory
                Log.d(TAG, "TV Series loaded from disk cache");
                return series;
            } catch (Exception e) {
                Log.e(TAG, "Error parsing TV series from disk", e);
            }
        }
        
        return null;
    }
    
    /**
     * Cache TV series
     */
    public void cacheTvSeries(List<Poster> series) {
        if (series == null) return;
        
        // Cache in memory
        cachePut("tv_series", series);
        
        // Cache in disk
        try {
            String json = gson.toJson(series);
            saveToDisk("tv_series", json);
            Log.d(TAG, "TV Series cached in memory and disk");
        } catch (Exception e) {
            Log.e(TAG, "Error caching TV series", e);
        }
    }
    
    // ==================== CHANNELS CACHE ====================
    
    /**
     * Get channels from cache
     */
    public List<Channel> getChannels() {
        // Try memory first
        Object cached = cacheGet("channels");
        if (cached != null) {
            Log.d(TAG, "Channels hit in memory cache");
            return (List<Channel>) cached;
        }
        
        // Try disk
        String json = loadFromDisk("channels");
        if (json != null) {
            try {
                Type type = new TypeToken<List<Channel>>(){}.getType();
                List<Channel> channels = gson.fromJson(json, type);
                cachePut("channels", channels); // Cache in memory
                Log.d(TAG, "Channels loaded from disk cache");
                return channels;
            } catch (Exception e) {
                Log.e(TAG, "Error parsing channels from disk", e);
            }
        }
        
        return null;
    }
    
    /**
     * Cache channels
     */
    public void cacheChannels(List<Channel> channels) {
        if (channels == null) return;
        
        // Cache in memory
        cachePut("channels", channels);
        
        // Cache in disk
        try {
            String json = gson.toJson(channels);
            saveToDisk("channels", json);
            Log.d(TAG, "Channels cached in memory and disk");
        } catch (Exception e) {
            Log.e(TAG, "Error caching channels", e);
        }
    }
    
    // ==================== FULL API RESPONSE CACHE ====================
    
    /**
     * Get full API response from cache
     */
    public JsonApiResponse getApiResponse() {
        // Try memory first
        Object cached = cacheGet("api_response");
        if (cached != null) {
            Log.d(TAG, "API Response hit in memory cache");
            return (JsonApiResponse) cached;
        }
        
        // Try disk
        String json = loadFromDisk("api_response");
        if (json != null) {
            try {
                JsonApiResponse response = gson.fromJson(json, JsonApiResponse.class);
                cachePut("api_response", response); // Cache in memory
                Log.d(TAG, "API Response loaded from disk cache");
                return response;
            } catch (Exception e) {
                Log.e(TAG, "Error parsing API response from disk", e);
            }
        }
        
        return null;
    }
    
    /**
     * Cache full API response
     */
    public void cacheApiResponse(JsonApiResponse response) {
        if (response == null) return;
        
        // Cache in memory
        cachePut("api_response", response);
        
        // Cache in disk
        try {
            String json = gson.toJson(response);
            saveToDisk("api_response", json);
            Log.d(TAG, "API Response cached in memory and disk");
        } catch (Exception e) {
            Log.e(TAG, "Error caching API response", e);
        }
    }
    
    // ==================== IMAGE CACHE ====================
    
    /**
     * Get image from cache
     */
    public Bitmap getImage(String url) {
        WeakReference<Bitmap> ref = imageCache.get(url);
        if (ref != null) {
            Bitmap bitmap = ref.get();
            if (bitmap != null) {
                Log.d(TAG, "Image hit in cache: " + url);
                return bitmap;
            } else {
                imageCache.remove(url); // Clean up null reference
            }
        }
        return null;
    }
    
    /**
     * Cache image
     */
    public void cacheImage(String url, Bitmap bitmap) {
        if (bitmap != null) {
            imageCache.put(url, new WeakReference<>(bitmap));
            Log.d(TAG, "Image cached: " + url);
        }
    }
    
    // ==================== CACHE MANAGEMENT ====================
    
    /**
     * Clear cache for specific type
     */
    public void clearCacheForType(String type) {
        String key = type.toLowerCase();
        
        // Memory
        cacheRemove(key);
        
        // Disk
        File file = new File(context.getFilesDir(), key + ".json.gz");
        if (file.exists()) {
            file.delete();
        }
        
        Log.d(TAG, "Cleared cache for: " + type);
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        // Clear memory cache
        memoryCache.evictAll();
        
        // Clear image cache
        imageCache.clear();
        
        // Clear disk cache
        File filesDir = context.getFilesDir();
        File[] files = filesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".json.gz")) {
                    file.delete();
                }
            }
        }
        
        // Clear network cache
        try {
            okHttpClient.cache().evictAll();
        } catch (IOException e) {
            Log.e(TAG, "Error clearing network cache", e);
        }
        
        Log.d(TAG, "All caches cleared");
    }
    
    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        int memorySize = memoryCache.size();
        int imageCount = imageCache.size();
        long diskSize = 0;
        
        // Calculate disk size
        File filesDir = context.getFilesDir();
        File[] files = filesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".json.gz")) {
                    diskSize += file.length();
                }
            }
        }
        
        return String.format("Memory: %d items, Images: %d, Disk: %.2f MB", 
            memorySize, imageCount, diskSize / (1024.0 * 1024.0));
    }
    
    /**
     * Get OkHttp client for network requests
     */
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }
    
    /**
     * Check if cache is valid (less than 24 hours old)
     */
    public boolean isCacheValid(String key) {
        File file = new File(context.getFilesDir(), key + ".json.gz");
        if (file.exists()) {
            long age = System.currentTimeMillis() - file.lastModified();
            return age < 86_400_000; // 24 hours
        }
        return false;
    }
    
    /**
     * Clear memory cache to free up memory
     */
    public void clearMemoryCache() {
        synchronized (cacheLock) {
            Log.d(TAG, "Clearing memory cache due to memory pressure");
            memoryCache.evictAll();
            
            // Clear weak references in image cache
            imageCache.clear();
            
            Log.d(TAG, "Memory cache cleared");
        }
    }
    
    /**
     * Trim memory cache
     */
    public void trimMemory() {
        synchronized (cacheLock) {
            Log.d(TAG, "Trimming memory cache");
            
            // Remove half of the items from memory cache
            int currentSize = memoryCache.size();
            int targetSize = currentSize / 2;
            
            // LruCache doesn't have a direct way to trim to specific size
            // So we'll clear some items by temporarily reducing max size
            int originalMaxSize = memoryCache.maxSize();
            memoryCache.resize(targetSize);
            memoryCache.resize(originalMaxSize);
            
            // Clear expired image references
            imageCache.entrySet().removeIf(entry -> 
                entry.getValue().get() == null);
                
            Log.d(TAG, "Memory cache trimmed from " + currentSize + " to " + memoryCache.size() + " items");
        }
    }
}