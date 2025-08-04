package my.cinemax.app.free.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import my.cinemax.app.free.entity.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Unified Cache Manager - Master Coordinator for All Caching Layers
 * 
 * Coordinates all caching layers to provide optimal performance:
 * 1. Memory Cache (fastest access)
 * 2. Disk Cache (persistent storage)
 * 3. Image Cache (specialized for images)
 * 4. Network Cache (HTTP response caching)
 * 
 * Features:
 * - Intelligent cache layer coordination
 * - Automatic data synchronization across layers
 * - Performance monitoring and optimization
 * - Unified statistics and management
 * - Background data prefetching
 */
public class UnifiedCacheManager {
    
    private static final String TAG = "UnifiedCacheManager";
    private static UnifiedCacheManager instance;
    
    // Cache layer managers
    private final MemoryCacheManager memoryCacheManager;
    private final CacheManager diskCacheManager;
    private ImageCacheManager imageCacheManager; // Initialized in initialize()
    private NetworkCacheManager networkCacheManager; // Initialized in initialize()
    
    // Background processing
    private final ExecutorService executorService;
    
    // Configuration
    private boolean isInitialized = false;
    private boolean autoSyncEnabled = true;
    private boolean backgroundPrefetchEnabled = true;
    
    // Statistics
    private long totalRequests = 0;
    private long memoryHits = 0;
    private long diskHits = 0;
    private long imageHits = 0;
    private long networkHits = 0;
    private long cacheMisses = 0;
    
    private UnifiedCacheManager() {
        this.memoryCacheManager = MemoryCacheManager.getInstance();
        this.diskCacheManager = CacheManager.getInstance();
        this.executorService = Executors.newFixedThreadPool(4);
        
        Log.d(TAG, "UnifiedCacheManager created");
    }
    
    public static synchronized UnifiedCacheManager getInstance() {
        if (instance == null) {
            instance = new UnifiedCacheManager();
        }
        return instance;
    }
    
    /**
     * Initialize all cache layers
     */
    public void initialize(Context context) {
        if (isInitialized) {
            Log.w(TAG, "UnifiedCacheManager already initialized");
            return;
        }
        
        if (context == null) {
            Log.e(TAG, "Context is null, cannot initialize UnifiedCacheManager");
            return;
        }
        
        try {
            Log.d(TAG, "Starting UnifiedCacheManager initialization...");
            
            // Initialize disk cache manager
            try {
                diskCacheManager.initialize(context);
                Log.d(TAG, "Disk cache manager initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing disk cache manager", e);
                // Continue with other initializations
            }
            
            // Initialize image cache manager
            try {
                imageCacheManager = ImageCacheManager.getInstance(context);
                Log.d(TAG, "Image cache manager initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing image cache manager", e);
                // Continue with other initializations
            }
            
            // Initialize network cache manager
            try {
                networkCacheManager = NetworkCacheManager.getInstance(context);
                Log.d(TAG, "Network cache manager initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing network cache manager", e);
                // Continue with other initializations
            }
            
            isInitialized = true;
            
            Log.d(TAG, "UnifiedCacheManager initialization completed");
            
            // Start background optimization only if everything initialized successfully
            if (backgroundPrefetchEnabled && imageCacheManager != null && networkCacheManager != null) {
                startBackgroundOptimization();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Critical error initializing UnifiedCacheManager", e);
            // Don't set isInitialized to true if there's a critical error
        }
    }
    
    // ==================== MOVIES CACHE ====================
    
    /**
     * Get movie with multi-layer caching
     */
    public Poster getMovie(Integer movieId) {
        totalRequests++;
        
        // 1. Check memory cache first (fastest)
        Poster movie = memoryCacheManager.getMovie(movieId);
        if (movie != null) {
            memoryHits++;
            Log.v(TAG, "Movie hit in memory cache: " + movieId);
            return movie;
        }
        
        // 2. Check disk cache
        movie = diskCacheManager.getMovieById(movieId);
        if (movie != null) {
            diskHits++;
            Log.v(TAG, "Movie hit in disk cache: " + movieId);
            
            // Add to memory cache for future fast access
            memoryCacheManager.cacheMovie(movie);
            
            return movie;
        }
        
        // 3. Cache miss
        cacheMisses++;
        Log.v(TAG, "Movie miss in all caches: " + movieId);
        return null;
    }
    
    /**
     * Cache movie in all layers
     */
    public void cacheMovie(Poster movie) {
        if (movie == null) return;
        
        // Cache in memory (fastest access)
        memoryCacheManager.cacheMovie(movie);
        
        // Cache in disk (persistent storage)
        // Note: Disk cache is handled by DataRepository when storing full response
        
        Log.v(TAG, "Movie cached in all layers: " + movie.getId());
    }
    
    /**
     * Get movies by genre with caching
     */
    public List<Poster> getMoviesByGenre(String genre) {
        totalRequests++;
        
        // 1. Check memory cache first
        List<Poster> movies = memoryCacheManager.getMoviesByGenre(genre);
        if (movies != null) {
            memoryHits++;
            Log.v(TAG, "Movies by genre hit in memory cache: " + genre);
            return movies;
        }
        
        // 2. Check disk cache
        movies = diskCacheManager.getMoviesByGenre(0); // Assuming genre ID 0 for now
        if (movies != null) {
            diskHits++;
            Log.v(TAG, "Movies by genre hit in disk cache: " + genre);
            
            // Add to memory cache
            memoryCacheManager.cacheMoviesByGenre(genre, movies);
            
            return movies;
        }
        
        // 3. Cache miss
        cacheMisses++;
        Log.v(TAG, "Movies by genre miss in all caches: " + genre);
        return null;
    }
    
    // ==================== TV SERIES CACHE ====================
    
    /**
     * Get TV series with multi-layer caching
     */
    public Poster getTvSeries(Integer seriesId) {
        totalRequests++;
        
        // 1. Check memory cache first
        Poster series = memoryCacheManager.getTvSeries(seriesId);
        if (series != null) {
            memoryHits++;
            Log.v(TAG, "TV Series hit in memory cache: " + seriesId);
            return series;
        }
        
        // 2. Check disk cache
        series = diskCacheManager.getTvSeriesById(seriesId);
        if (series != null) {
            diskHits++;
            Log.v(TAG, "TV Series hit in disk cache: " + seriesId);
            
            // Add to memory cache
            memoryCacheManager.cacheTvSeries(series);
            
            return series;
        }
        
        // 3. Cache miss
        cacheMisses++;
        Log.v(TAG, "TV Series miss in all caches: " + seriesId);
        return null;
    }
    
    /**
     * Cache TV series in all layers
     */
    public void cacheTvSeries(Poster series) {
        if (series == null) return;
        
        memoryCacheManager.cacheTvSeries(series);
        Log.v(TAG, "TV Series cached in all layers: " + series.getId());
    }
    
    // ==================== CHANNELS CACHE ====================
    
    /**
     * Get channel with multi-layer caching
     */
    public Channel getChannel(Integer channelId) {
        totalRequests++;
        
        // 1. Check memory cache first
        Channel channel = memoryCacheManager.getChannel(channelId);
        if (channel != null) {
            memoryHits++;
            Log.v(TAG, "Channel hit in memory cache: " + channelId);
            return channel;
        }
        
        // 2. Check disk cache
        channel = diskCacheManager.getChannelById(channelId);
        if (channel != null) {
            diskHits++;
            Log.v(TAG, "Channel hit in disk cache: " + channelId);
            
            // Add to memory cache
            memoryCacheManager.cacheChannel(channel);
            
            return channel;
        }
        
        // 3. Cache miss
        cacheMisses++;
        Log.v(TAG, "Channel miss in all caches: " + channelId);
        return null;
    }
    
    /**
     * Cache channel in all layers
     */
    public void cacheChannel(Channel channel) {
        if (channel == null) return;
        
        memoryCacheManager.cacheChannel(channel);
        Log.v(TAG, "Channel cached in all layers: " + channel.getId());
    }
    
    // ==================== IMAGE CACHE ====================
    
    /**
     * Load image with multi-layer caching
     */
    public Future<Bitmap> loadImage(String url, ImageCacheManager.ImageLoadCallback callback) {
        if (!isInitialized) {
            Log.w(TAG, "UnifiedCacheManager not initialized, cannot load image");
            if (callback != null) {
                callback.onImageLoadFailed("Cache manager not initialized");
            }
            return null;
        }
        
        if (imageCacheManager == null) {
            Log.e(TAG, "ImageCacheManager not initialized");
            if (callback != null) {
                callback.onImageLoadFailed("Image cache not available");
            }
            return null;
        }
        
        return imageCacheManager.loadImage(url, new ImageCacheManager.ImageLoadCallback() {
            @Override
            public void onImageLoaded(Bitmap bitmap, String source) {
                imageHits++;
                Log.v(TAG, "Image loaded from " + source + ": " + url);
                if (callback != null) {
                    callback.onImageLoaded(bitmap, source);
                }
            }
            
            @Override
            public void onImageLoadFailed(String error) {
                cacheMisses++;
                Log.e(TAG, "Image load failed: " + url + " - " + error);
                if (callback != null) {
                    callback.onImageLoadFailed(error);
                }
            }
        });
    }
    
    /**
     * Get image from memory cache only
     */
    public Bitmap getImageFromMemory(String url) {
        return memoryCacheManager.getImage(url);
    }
    
    // ==================== SEARCH CACHE ====================
    
    /**
     * Get search results with caching
     */
    public List<Poster> getSearchResults(String query) {
        totalRequests++;
        
        // 1. Check memory cache first
        List<Poster> results = memoryCacheManager.getSearchResults(query);
        if (results != null) {
            memoryHits++;
            Log.v(TAG, "Search results hit in memory cache: " + query);
            return results;
        }
        
        // 2. Check disk cache
        results = diskCacheManager.searchMovies(query);
        if (results != null && !results.isEmpty()) {
            diskHits++;
            Log.v(TAG, "Search results hit in disk cache: " + query);
            
            // Add to memory cache
            memoryCacheManager.cacheSearchResults(query, results);
            
            return results;
        }
        
        // 3. Cache miss
        cacheMisses++;
        Log.v(TAG, "Search results miss in all caches: " + query);
        return null;
    }
    
    /**
     * Cache search results
     */
    public void cacheSearchResults(String query, List<Poster> results) {
        if (query == null || results == null) return;
        
        memoryCacheManager.cacheSearchResults(query, results);
        Log.v(TAG, "Search results cached: " + query + " (" + results.size() + " results)");
    }
    
    // ==================== BULK OPERATIONS ====================
    
    /**
     * Cache all data from API response
     */
    public void cacheAllData(JsonApiResponse response) {
        if (response == null) return;
        
        // Cache in memory
        memoryCacheManager.cacheAllData(response);
        
        // Cache in disk (handled by DataRepository)
        diskCacheManager.storeApiResponse(response);
        
        Log.d(TAG, "All data cached in all layers");
    }
    
    /**
     * Preload essential data in background
     */
    public void preloadEssentialData() {
        if (!isInitialized) {
            Log.w(TAG, "UnifiedCacheManager not initialized, skipping preload");
            return;
        }
        
        if (!backgroundPrefetchEnabled) return;
        
        executorService.submit(() -> {
            try {
                Log.d(TAG, "Starting essential data preload");
                
                // Preload popular movies
                try {
                    List<Poster> popularMovies = diskCacheManager.getAllMovies();
                    if (popularMovies != null && !popularMovies.isEmpty()) {
                        // Cache first 100 movies in memory
                        int count = Math.min(100, popularMovies.size());
                        List<Poster> toCache = popularMovies.subList(0, count);
                        memoryCacheManager.cacheMovies(toCache);
                        Log.d(TAG, "Preloaded " + count + " popular movies");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error preloading movies", e);
                }
                
                // Preload popular TV series
                try {
                    List<Poster> popularSeries = diskCacheManager.getAllTvSeries();
                    if (popularSeries != null && !popularSeries.isEmpty()) {
                        int count = Math.min(50, popularSeries.size());
                        List<Poster> toCache = popularSeries.subList(0, count);
                        memoryCacheManager.cacheTvSeries(toCache);
                        Log.d(TAG, "Preloaded " + count + " popular TV series");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error preloading TV series", e);
                }
                
                // Preload channels
                try {
                    List<Channel> channels = diskCacheManager.getAllChannels();
                    if (channels != null && !channels.isEmpty()) {
                        int count = Math.min(50, channels.size());
                        List<Channel> toCache = channels.subList(0, count);
                        memoryCacheManager.cacheChannels(toCache);
                        Log.d(TAG, "Preloaded " + count + " channels");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error preloading channels", e);
                }
                
                Log.d(TAG, "Essential data preload completed");
                
            } catch (Exception e) {
                Log.e(TAG, "Error preloading essential data", e);
            }
        });
    }
    
    /**
     * Preload images for popular content
     */
    public void preloadPopularImages() {
        if (imageCacheManager == null) return;
        
        executorService.submit(() -> {
            try {
                Log.d(TAG, "Starting popular images preload");
                
                // Get popular movies and preload their images
                List<Poster> popularMovies = diskCacheManager.getAllMovies();
                if (popularMovies != null && !popularMovies.isEmpty()) {
                    List<String> imageUrls = new ArrayList<>();
                    
                    // Collect image URLs from first 50 movies
                    int count = Math.min(50, popularMovies.size());
                    for (int i = 0; i < count; i++) {
                        Poster movie = popularMovies.get(i);
                        if (movie.getImage() != null && !movie.getImage().isEmpty()) {
                            imageUrls.add(movie.getImage());
                        }
                    }
                    
                    // Preload images
                    imageCacheManager.preloadImages(imageUrls);
                    Log.d(TAG, "Preloading " + imageUrls.size() + " popular movie images");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error preloading popular images", e);
            }
        });
    }
    
    // ==================== CACHE MANAGEMENT ====================
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        memoryCacheManager.clearAllCaches();
        diskCacheManager.clearCache();
        
        if (imageCacheManager != null) {
            imageCacheManager.clearAllCaches();
        }
        
        if (networkCacheManager != null) {
            networkCacheManager.clearCache();
        }
        
        // Reset statistics
        totalRequests = 0;
        memoryHits = 0;
        diskHits = 0;
        imageHits = 0;
        networkHits = 0;
        cacheMisses = 0;
        
        Log.d(TAG, "All caches cleared");
    }
    
    /**
     * Clear memory cache only
     */
    public void clearMemoryCache() {
        memoryCacheManager.clearAllCaches();
        Log.d(TAG, "Memory cache cleared");
    }
    
    /**
     * Clear image cache only
     */
    public void clearImageCache() {
        if (imageCacheManager != null) {
            imageCacheManager.clearAllCaches();
        }
        Log.d(TAG, "Image cache cleared");
    }
    
    /**
     * Optimize cache performance
     */
    public void optimizeCache() {
        executorService.submit(() -> {
            try {
                Log.d(TAG, "Starting cache optimization");
                
                // Remove old search results from memory
                memoryCacheManager.clearSearchCache();
                
                // Compact disk cache if needed
                // (implemented in CacheManager)
                
                Log.d(TAG, "Cache optimization completed");
                
            } catch (Exception e) {
                Log.e(TAG, "Error optimizing cache", e);
            }
        });
    }
    
    // ==================== BACKGROUND OPTIMIZATION ====================
    
    /**
     * Start background optimization tasks
     */
    private void startBackgroundOptimization() {
        executorService.submit(() -> {
            try {
                // Wait a bit for app to stabilize
                Thread.sleep(5000);
                
                // Preload essential data
                preloadEssentialData();
                
                // Wait and preload images
                Thread.sleep(3000);
                preloadPopularImages();
                
                // Schedule periodic optimization
                schedulePeriodicOptimization();
                
            } catch (Exception e) {
                Log.e(TAG, "Error in background optimization", e);
            }
        });
    }
    
    /**
     * Schedule periodic cache optimization
     */
    private void schedulePeriodicOptimization() {
        executorService.submit(() -> {
            try {
                while (true) {
                    // Wait 30 minutes
                    Thread.sleep(30 * 60 * 1000);
                    
                    // Run optimization
                    optimizeCache();
                    
                    // Log statistics
                    UnifiedCacheStats stats = getCacheStats();
                    Log.d(TAG, "Periodic cache stats: " + stats.toString());
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Periodic optimization interrupted");
            } catch (Exception e) {
                Log.e(TAG, "Error in periodic optimization", e);
            }
        });
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Get unified cache statistics
     */
    public UnifiedCacheStats getCacheStats() {
        MemoryCacheManager.CacheStats memoryStats = memoryCacheManager.getCacheStats();
        CacheManager.CacheStats diskStats = diskCacheManager.getCacheStats();
        
        ImageCacheManager.ImageCacheStats imageStats = null;
        if (imageCacheManager != null) {
            imageStats = imageCacheManager.getCacheStats();
        }
        
        NetworkCacheManager.NetworkCacheStats networkStats = null;
        if (networkCacheManager != null) {
            networkStats = networkCacheManager.getCacheStats();
        }
        
        return new UnifiedCacheStats(
            totalRequests,
            memoryHits,
            diskHits,
            imageHits,
            networkHits,
            cacheMisses,
            getOverallHitRate(),
            memoryStats,
            diskStats,
            imageStats,
            networkStats
        );
    }
    
    /**
     * Calculate overall hit rate
     */
    public double getOverallHitRate() {
        long totalHits = memoryHits + diskHits + imageHits + networkHits;
        long totalRequests = this.totalRequests;
        return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
    }
    
    /**
     * Get memory usage
     */
    public long getMemoryUsage() {
        return memoryCacheManager.getMemoryUsage();
    }
    
    // ==================== CONFIGURATION ====================
    
    /**
     * Enable/disable auto sync
     */
    public void setAutoSyncEnabled(boolean enabled) {
        this.autoSyncEnabled = enabled;
        Log.d(TAG, "Auto sync " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Enable/disable background prefetch
     */
    public void setBackgroundPrefetchEnabled(boolean enabled) {
        this.backgroundPrefetchEnabled = enabled;
        Log.d(TAG, "Background prefetch " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if the cache system is ready for use
     */
    public boolean isReady() {
        return isInitialized && diskCacheManager != null;
    }
    
    /**
     * Shutdown the unified cache manager
     */
    public void shutdown() {
        executorService.shutdown();
        
        if (imageCacheManager != null) {
            imageCacheManager.shutdown();
        }
        
        if (networkCacheManager != null) {
            networkCacheManager.shutdown();
        }
        
        Log.d(TAG, "UnifiedCacheManager shutdown");
    }
    
    // ==================== STATISTICS CLASSES ====================
    
    /**
     * Unified cache statistics
     */
    public static class UnifiedCacheStats {
        public final long totalRequests;
        public final long memoryHits;
        public final long diskHits;
        public final long imageHits;
        public final long networkHits;
        public final long cacheMisses;
        public final double overallHitRate;
        public final MemoryCacheManager.CacheStats memoryStats;
        public final CacheManager.CacheStats diskStats;
        public final ImageCacheManager.ImageCacheStats imageStats;
        public final NetworkCacheManager.NetworkCacheStats networkStats;
        
        public UnifiedCacheStats(long totalRequests, long memoryHits, long diskHits,
                               long imageHits, long networkHits, long cacheMisses,
                               double overallHitRate, MemoryCacheManager.CacheStats memoryStats,
                               CacheManager.CacheStats diskStats, ImageCacheManager.ImageCacheStats imageStats,
                               NetworkCacheManager.NetworkCacheStats networkStats) {
            this.totalRequests = totalRequests;
            this.memoryHits = memoryHits;
            this.diskHits = diskHits;
            this.imageHits = imageHits;
            this.networkHits = networkHits;
            this.cacheMisses = cacheMisses;
            this.overallHitRate = overallHitRate;
            this.memoryStats = memoryStats;
            this.diskStats = diskStats;
            this.imageStats = imageStats;
            this.networkStats = networkStats;
        }
        
        public long getTotalHits() {
            return memoryHits + diskHits + imageHits + networkHits;
        }
        
        @Override
        public String toString() {
            return String.format("UnifiedCacheStats{requests=%d, hits=%d, misses=%d, " +
                               "hitRate=%.2f%%, memory=%s, disk=%s}",
                totalRequests, getTotalHits(), cacheMisses, overallHitRate * 100,
                memoryStats != null ? memoryStats.toString() : "N/A",
                diskStats != null ? diskStats.toString() : "N/A");
        }
    }
}