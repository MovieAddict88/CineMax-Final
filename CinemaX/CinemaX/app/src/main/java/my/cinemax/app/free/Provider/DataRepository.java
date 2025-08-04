package my.cinemax.app.free.Provider;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import my.cinemax.app.free.Utils.CacheManager;
import my.cinemax.app.free.Utils.SimpleCacheManager;
import my.cinemax.app.free.api.apiClient;
import my.cinemax.app.free.entity.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DataRepository - Centralized data management for CineMax
 * 
 * This repository provides a single source of truth for all data operations.
 * It intelligently manages cache and API calls to provide optimal performance
 * for large datasets (10,000+ entries) without using DAO.
 * 
 * Features:
 * - Cache-first strategy
 * - Automatic background refresh
 * - Memory-efficient pagination
 * - Smart data prefetching
 * - Offline support
 */
public class DataRepository {
    
    private static final String TAG = "DataRepository";
    private static DataRepository instance;
    
    private CacheManager cacheManager;
    private SimpleCacheManager simpleCacheManager;
    private Context context;
    private ExecutorService executorService;
    private boolean isLoading = false;
    
    // Callback interfaces
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
        void onLoading();
    }
    
    public interface ApiResponseCallback {
        void onSuccess(JsonApiResponse response);
        void onError(String error);
        void onFromCache(JsonApiResponse response);
        void onLoading();
    }
    
    private DataRepository() {
        this.executorService = Executors.newCachedThreadPool();
        this.cacheManager = CacheManager.getInstance();
        this.simpleCacheManager = SimpleCacheManager.getInstance();
    }
    
    public static synchronized DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }
    
    /**
     * Initialize the repository
     */
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        cacheManager.initialize(this.context);
        simpleCacheManager.initialize(this.context);
        Log.d(TAG, "DataRepository initialized with simple caching system");
    }
    
    /**
     * Load all data with enhanced cache-first strategy
     */
    public void loadAllData(ApiResponseCallback callback) {
        if (callback != null) {
            callback.onLoading();
        }
        
        // Check simple cache first (memory + disk)
        JsonApiResponse cachedResponse = simpleCacheManager.getApiResponse();
        if (cachedResponse != null && simpleCacheManager.isCacheValid("api_response")) {
            Log.d(TAG, "Returning cached data from simple cache");
            
            // Cache data in memory for faster future access
            try {
                simpleCacheManager.cacheApiResponse(cachedResponse);
            } catch (Exception e) {
                Log.e(TAG, "Error caching data in memory", e);
            }
            
            if (callback != null) {
                callback.onFromCache(cachedResponse);
            }
            
            // Still refresh in background if cache is older than 1 hour
            long lastUpdate = System.currentTimeMillis() - 3600000; // 1 hour ago
            if (shouldRefreshInBackground()) {
                refreshDataInBackground(null);
            }
            return;
        }
        
        // Load from API
        loadFromApi(callback);
    }
    
    /**
     * Force refresh data from API
     */
    public void refreshData(ApiResponseCallback callback) {
        cacheManager.invalidateCache();
        loadFromApi(callback);
    }
    
    /**
     * Load data from API and cache it
     */
    private void loadFromApi(ApiResponseCallback callback) {
        if (isLoading) {
            Log.d(TAG, "Already loading data, skipping duplicate request");
            return;
        }
        
        isLoading = true;
        Log.d(TAG, "Loading data from API");
        
        apiClient.getJsonApiData(new Callback<JsonApiResponse>() {
            @Override
            public void onResponse(Call<JsonApiResponse> call, Response<JsonApiResponse> response) {
                isLoading = false;
                
                if (response.isSuccessful() && response.body() != null) {
                    JsonApiResponse apiResponse = response.body();
                    
                    // Cache the response in all layers in background
                    executorService.execute(() -> {
                        Log.d(TAG, "Caching API response in all layers in background");
                        cacheManager.storeApiResponse(apiResponse);
                        simpleCacheManager.cacheApiResponse(apiResponse);
                        Log.d(TAG, "API response cached successfully in all layers");
                    });
                    
                    if (callback != null) {
                        callback.onSuccess(apiResponse);
                    }
                    
                } else {
                    String error = "Failed to load data from API: " + response.code();
                    Log.e(TAG, error);
                    
                    // Try to return cached data as fallback
                    JsonApiResponse cachedResponse = cacheManager.getCachedApiResponse();
                    if (cachedResponse != null) {
                        Log.d(TAG, "Returning stale cached data as fallback");
                        if (callback != null) {
                            callback.onFromCache(cachedResponse);
                        }
                    } else if (callback != null) {
                        callback.onError(error);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<JsonApiResponse> call, Throwable t) {
                isLoading = false;
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, error, t);
                
                // Try to return cached data as fallback
                JsonApiResponse cachedResponse = cacheManager.getCachedApiResponse();
                if (cachedResponse != null) {
                    Log.d(TAG, "Returning cached data due to network error");
                    if (callback != null) {
                        callback.onFromCache(cachedResponse);
                    }
                } else if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * Refresh data in background without affecting UI
     */
    private void refreshDataInBackground(Runnable onComplete) {
        executorService.execute(() -> {
            Log.d(TAG, "Background refresh started");
            
            apiClient.getJsonApiData(new Callback<JsonApiResponse>() {
                @Override
                public void onResponse(Call<JsonApiResponse> call, Response<JsonApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        cacheManager.storeApiResponse(response.body());
                        simpleCacheManager.cacheApiResponse(response.body());
                        Log.d(TAG, "Background refresh completed successfully and cached in all layers");
                    } else {
                        Log.w(TAG, "Background refresh failed: " + response.code());
                    }
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
                
                @Override
                public void onFailure(Call<JsonApiResponse> call, Throwable t) {
                    Log.w(TAG, "Background refresh failed", t);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
        });
    }
    
    // ===== MOVIES =====
    
    /**
     * Get all movies (from cache preferably)
     */
    public void getAllMovies(DataCallback<List<Poster>> callback) {
        executorService.execute(() -> {
            List<Poster> movies = cacheManager.getAllMovies();
            if (movies.isEmpty()) {
                // Load from API if cache is empty
                loadAllData(new ApiResponseCallback() {
                    @Override
                    public void onSuccess(JsonApiResponse response) {
                        List<Poster> freshMovies = cacheManager.getAllMovies();
                        callback.onSuccess(freshMovies);
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                    
                    @Override
                    public void onFromCache(JsonApiResponse response) {
                        List<Poster> cachedMovies = cacheManager.getAllMovies();
                        callback.onSuccess(cachedMovies);
                    }
                    
                    @Override
                    public void onLoading() {
                        callback.onLoading();
                    }
                });
            } else {
                callback.onSuccess(movies);
            }
        });
    }
    
    /**
     * Get paginated movies for efficient loading
     */
    public void getMoviesPaginated(int page, int pageSize, DataCallback<List<Poster>> callback) {
        executorService.execute(() -> {
            List<Poster> movies = cacheManager.getMoviesPaginated(page, pageSize);
            callback.onSuccess(movies);
        });
    }
    
    /**
     * Search movies by title
     */
    public void searchMovies(String query, DataCallback<List<Poster>> callback) {
        executorService.execute(() -> {
            List<Poster> results = cacheManager.searchMovies(query);
            callback.onSuccess(results);
        });
    }
    
    /**
     * Get movies by genre
     */
    public void getMoviesByGenre(int genreId, DataCallback<List<Poster>> callback) {
        executorService.execute(() -> {
            List<Poster> results = cacheManager.getMoviesByGenre(genreId);
            callback.onSuccess(results);
        });
    }
    
    /**
     * Get movie by ID with caching
     */
    public void getMovieById(int movieId, DataCallback<Poster> callback) {
        executorService.execute(() -> {
            Poster movie = cacheManager.getMovieById(movieId);
            if (movie != null) {
                callback.onSuccess(movie);
            } else {
                callback.onError("Movie not found");
            }
        });
    }
    
    // ===== TV SERIES =====
    
    /**
     * Get all TV series
     */
    public void getAllTvSeries(DataCallback<List<Poster>> callback) {
        executorService.execute(() -> {
            List<Poster> tvSeries = cacheManager.getAllTvSeries();
            if (tvSeries.isEmpty()) {
                // Load from API if cache is empty
                loadAllData(new ApiResponseCallback() {
                    @Override
                    public void onSuccess(JsonApiResponse response) {
                        List<Poster> freshTvSeries = cacheManager.getAllTvSeries();
                        callback.onSuccess(freshTvSeries);
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                    
                    @Override
                    public void onFromCache(JsonApiResponse response) {
                        List<Poster> cachedTvSeries = cacheManager.getAllTvSeries();
                        callback.onSuccess(cachedTvSeries);
                    }
                    
                    @Override
                    public void onLoading() {
                        callback.onLoading();
                    }
                });
            } else {
                callback.onSuccess(tvSeries);
            }
        });
    }
    
    /**
     * Get paginated TV series
     */
    public void getTvSeriesPaginated(int page, int pageSize, DataCallback<List<Poster>> callback) {
        executorService.execute(() -> {
            List<Poster> tvSeries = cacheManager.getTvSeriesPaginated(page, pageSize);
            callback.onSuccess(tvSeries);
        });
    }
    
    /**
     * Search TV series by title
     */
    public void searchTvSeries(String query, DataCallback<List<Poster>> callback) {
        executorService.execute(() -> {
            List<Poster> results = cacheManager.searchTvSeries(query);
            callback.onSuccess(results);
        });
    }
    
    /**
     * Get TV series by ID
     */
    public void getTvSeriesById(int seriesId, DataCallback<Poster> callback) {
        executorService.execute(() -> {
            Poster series = cacheManager.getTvSeriesById(seriesId);
            if (series != null) {
                callback.onSuccess(series);
            } else {
                callback.onError("TV series not found");
            }
        });
    }
    
    // ===== LIVE TV / CHANNELS =====
    
    /**
     * Get all channels
     */
    public void getAllChannels(DataCallback<List<Channel>> callback) {
        executorService.execute(() -> {
            List<Channel> channels = cacheManager.getAllChannels();
            if (channels.isEmpty()) {
                // Load from API if cache is empty
                loadAllData(new ApiResponseCallback() {
                    @Override
                    public void onSuccess(JsonApiResponse response) {
                        List<Channel> freshChannels = cacheManager.getAllChannels();
                        callback.onSuccess(freshChannels);
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                    
                    @Override
                    public void onFromCache(JsonApiResponse response) {
                        List<Channel> cachedChannels = cacheManager.getAllChannels();
                        callback.onSuccess(cachedChannels);
                    }
                    
                    @Override
                    public void onLoading() {
                        callback.onLoading();
                    }
                });
            } else {
                callback.onSuccess(channels);
            }
        });
    }
    
    /**
     * Get paginated channels
     */
    public void getChannelsPaginated(int page, int pageSize, DataCallback<List<Channel>> callback) {
        executorService.execute(() -> {
            List<Channel> channels = cacheManager.getChannelsPaginated(page, pageSize);
            callback.onSuccess(channels);
        });
    }
    
    /**
     * Search channels by title
     */
    public void searchChannels(String query, DataCallback<List<Channel>> callback) {
        executorService.execute(() -> {
            List<Channel> results = cacheManager.searchChannels(query);
            callback.onSuccess(results);
        });
    }
    
    /**
     * Get channel by ID
     */
    public void getChannelById(int channelId, DataCallback<Channel> callback) {
        executorService.execute(() -> {
            Channel channel = cacheManager.getChannelById(channelId);
            if (channel != null) {
                callback.onSuccess(channel);
            } else {
                callback.onError("Channel not found");
            }
        });
    }
    
    // ===== ACTORS =====
    
    /**
     * Get all actors
     */
    public void getAllActors(DataCallback<List<Actor>> callback) {
        executorService.execute(() -> {
            List<Actor> actors = cacheManager.getAllActors();
            callback.onSuccess(actors);
        });
    }
    
    // ===== METADATA =====
    
    /**
     * Get genres
     */
    public void getGenres(DataCallback<List<Genre>> callback) {
        executorService.execute(() -> {
            List<Genre> genres = cacheManager.getGenres();
            callback.onSuccess(genres);
        });
    }
    
    /**
     * Get categories
     */
    public void getCategories(DataCallback<List<Category>> callback) {
        executorService.execute(() -> {
            List<Category> categories = cacheManager.getCategories();
            callback.onSuccess(categories);
        });
    }
    
    /**
     * Get countries
     */
    public void getCountries(DataCallback<List<Country>> callback) {
        executorService.execute(() -> {
            List<Country> countries = cacheManager.getCountries();
            callback.onSuccess(countries);
        });
    }
    
    /**
     * Get home data
     */
    public void getHomeData(DataCallback<JsonApiResponse.HomeData> callback) {
        executorService.execute(() -> {
            JsonApiResponse.HomeData homeData = cacheManager.getHomeData();
            if (homeData != null) {
                callback.onSuccess(homeData);
            } else {
                // Load from API if not cached
                loadAllData(new ApiResponseCallback() {
                    @Override
                    public void onSuccess(JsonApiResponse response) {
                        JsonApiResponse.HomeData freshHomeData = cacheManager.getHomeData();
                        callback.onSuccess(freshHomeData);
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                    
                    @Override
                    public void onFromCache(JsonApiResponse response) {
                        JsonApiResponse.HomeData cachedHomeData = cacheManager.getHomeData();
                        callback.onSuccess(cachedHomeData);
                    }
                    
                    @Override
                    public void onLoading() {
                        callback.onLoading();
                    }
                });
            }
        });
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Get cache statistics
     */
    public CacheManager.CacheStats getCacheStats() {
        return cacheManager.getCacheStats();
    }
    
    /**
     * Get simple cache statistics
     */
    public String getSimpleCacheStats() {
        try {
            return simpleCacheManager.getCacheStats();
        } catch (Exception e) {
            Log.e(TAG, "Error getting simple cache stats", e);
            return "Error getting cache stats";
        }
    }
    
    /**
     * Get SimpleCacheManager instance
     */
    public SimpleCacheManager getSimpleCacheManager() {
        return simpleCacheManager;
    }
    
    /**
     * Get CacheManager instance
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * Check if cache is valid
     */
    public boolean isCacheValid() {
        return cacheManager.isCacheValid();
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        cacheManager.clearCache();
    }
    
    /**
     * Check if background refresh is needed
     */
    private boolean shouldRefreshInBackground() {
        // Refresh in background if cache is older than 1 hour
        return !cacheManager.isCacheValid();
    }
    
    /**
     * Preload essential data for better user experience
     */
    public void preloadEssentialData() {
        executorService.execute(() -> {
            Log.d(TAG, "Preloading essential data");
            
            // Preload first page of movies and channels
            cacheManager.getMoviesPaginated(0, 20);
            cacheManager.getChannelsPaginated(0, 20);
            cacheManager.getGenres();
            cacheManager.getCategories();
            
            Log.d(TAG, "Essential data preloaded");
        });
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}