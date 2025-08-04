package my.cinemax.app.free.Utils;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.hawk.Hawk;
import my.cinemax.app.free.entity.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Advanced Cache Manager for CineMax
 * Handles persistent storage of large datasets (10,000+ entries)
 * Provides efficient data retrieval without DAO dependency
 * 
 * Features:
 * - Persistent storage using Hawk
 * - Chunk-based storage for memory efficiency
 * - Cache expiration and validation
 * - Search and filtering capabilities
 * - Memory-efficient lazy loading
 */
public class CacheManager {
    
    private static final String TAG = "CacheManager";
    private static CacheManager instance;
    private static final String CACHE_PREFIX = "cinemax_cache_";
    private static final String METADATA_PREFIX = "cache_meta_";
    
    // Cache keys
    private static final String FULL_API_RESPONSE = CACHE_PREFIX + "full_response";
    private static final String MOVIES_CACHE = CACHE_PREFIX + "movies";
    private static final String TV_SERIES_CACHE = CACHE_PREFIX + "tv_series";
    private static final String CHANNELS_CACHE = CACHE_PREFIX + "channels";
    private static final String ACTORS_CACHE = CACHE_PREFIX + "actors";
    private static final String GENRES_CACHE = CACHE_PREFIX + "genres";
    private static final String CATEGORIES_CACHE = CACHE_PREFIX + "categories";
    private static final String COUNTRIES_CACHE = CACHE_PREFIX + "countries";
    private static final String HOME_DATA_CACHE = CACHE_PREFIX + "home_data";
    
    // Metadata keys
    private static final String LAST_UPDATE_TIME = METADATA_PREFIX + "last_update";
    private static final String CACHE_VERSION = METADATA_PREFIX + "version";
    private static final String DATA_CHUNKS_COUNT = METADATA_PREFIX + "chunks_count";
    
    // Configuration
    private static final long CACHE_EXPIRY_TIME = TimeUnit.HOURS.toMillis(24); // 24 hours
    private static final int CHUNK_SIZE = 500; // Process data in chunks of 500 items
    private static final int CURRENT_CACHE_VERSION = 3;
    
    private Gson gson;
    private boolean isInitialized = false;
    
    private CacheManager() {
        this.gson = new Gson();
    }
    
    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }
    
    /**
     * Initialize cache manager with context
     */
    public void initialize(Context context) {
        if (!isInitialized) {
            if (!Hawk.isBuilt()) {
                Hawk.init(context).build();
            }
            validateCacheVersion();
            isInitialized = true;
            Log.d(TAG, "CacheManager initialized successfully");
        }
    }
    
    /**
     * Store complete API response with chunking for large datasets
     */
    public void storeApiResponse(JsonApiResponse response) {
        if (!isInitialized) {
            Log.e(TAG, "CacheManager not initialized");
            return;
        }
        
        try {
            Log.d(TAG, "Starting to cache API response");
            long startTime = System.currentTimeMillis();
            
            // Store full response (for small datasets)
            Hawk.put(FULL_API_RESPONSE, response);
            
            // Store individual components with chunking
            if (response.getMovies() != null) {
                storeMoviesInChunks(response.getMovies());
            }
            
            if (response.getChannels() != null) {
                storeChannelsInChunks(response.getChannels());
            }
            
            if (response.getActors() != null) {
                storeActorsInChunks(response.getActors());
            }
            
            // Store smaller datasets normally
            if (response.getGenres() != null) {
                Hawk.put(GENRES_CACHE, response.getGenres());
            }
            
            if (response.getCategories() != null) {
                Hawk.put(CATEGORIES_CACHE, response.getCategories());
            }
            
            if (response.getCountries() != null) {
                Hawk.put(COUNTRIES_CACHE, response.getCountries());
            }
            
            if (response.getHome() != null) {
                Hawk.put(HOME_DATA_CACHE, response.getHome());
            }
            
            // Update metadata
            Hawk.put(LAST_UPDATE_TIME, System.currentTimeMillis());
            Hawk.put(CACHE_VERSION, CURRENT_CACHE_VERSION);
            
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "API response cached successfully in " + (endTime - startTime) + "ms");
            
        } catch (Exception e) {
            Log.e(TAG, "Error storing API response", e);
        }
    }
    
    /**
     * Store movies in chunks for memory efficiency
     */
    private void storeMoviesInChunks(List<Poster> movies) {
        Log.d(TAG, "Storing " + movies.size() + " movies in chunks");
        
        // Separate movies and TV series
        List<Poster> moviesList = new ArrayList<>();
        List<Poster> tvSeriesList = new ArrayList<>();
        
        for (Poster poster : movies) {
            if ("movie".equals(poster.getType())) {
                moviesList.add(poster);
            } else if ("serie".equals(poster.getType()) || "series".equals(poster.getType())) {
                tvSeriesList.add(poster);
            }
        }
        
        // Store movies in chunks
        storeListInChunks(moviesList, MOVIES_CACHE);
        
        // Store TV series in chunks
        storeListInChunks(tvSeriesList, TV_SERIES_CACHE);
    }
    
    /**
     * Store channels in chunks
     */
    private void storeChannelsInChunks(List<Channel> channels) {
        Log.d(TAG, "Storing " + channels.size() + " channels in chunks");
        storeListInChunks(channels, CHANNELS_CACHE);
    }
    
    /**
     * Store actors in chunks
     */
    private void storeActorsInChunks(List<Actor> actors) {
        Log.d(TAG, "Storing " + actors.size() + " actors in chunks");
        storeListInChunks(actors, ACTORS_CACHE);
    }
    
    /**
     * Generic method to store any list in chunks
     */
    private <T> void storeListInChunks(List<T> items, String cacheKey) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        int totalItems = items.size();
        int chunks = (int) Math.ceil((double) totalItems / CHUNK_SIZE);
        
        // Clear existing chunks
        clearChunks(cacheKey);
        
        // Store each chunk
        for (int i = 0; i < chunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, totalItems);
            List<T> chunk = items.subList(start, end);
            
            String chunkKey = cacheKey + "_chunk_" + i;
            Hawk.put(chunkKey, chunk);
            
            Log.d(TAG, "Stored chunk " + i + " for " + cacheKey + " (" + chunk.size() + " items)");
        }
        
        // Store chunk count metadata
        Hawk.put(cacheKey + "_chunk_count", chunks);
    }
    
    /**
     * Retrieve cached API response
     */
    public JsonApiResponse getCachedApiResponse() {
        if (!isInitialized || !isCacheValid()) {
            return null;
        }
        
        try {
            return Hawk.get(FULL_API_RESPONSE, null);
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving cached API response", e);
            return null;
        }
    }
    
    /**
     * Get all movies with lazy loading
     */
    public List<Poster> getAllMovies() {
        return getItemsFromChunks(MOVIES_CACHE, new TypeToken<List<Poster>>(){}.getType());
    }
    
    /**
     * Get all TV series with lazy loading
     */
    public List<Poster> getAllTvSeries() {
        return getItemsFromChunks(TV_SERIES_CACHE, new TypeToken<List<Poster>>(){}.getType());
    }
    
    /**
     * Get all channels with lazy loading
     */
    public List<Channel> getAllChannels() {
        return getItemsFromChunks(CHANNELS_CACHE, new TypeToken<List<Channel>>(){}.getType());
    }
    
    /**
     * Get all actors with lazy loading
     */
    public List<Actor> getAllActors() {
        return getItemsFromChunks(ACTORS_CACHE, new TypeToken<List<Actor>>(){}.getType());
    }
    
    /**
     * Get paginated movies (for large datasets)
     */
    public List<Poster> getMoviesPaginated(int page, int pageSize) {
        return getPaginatedItems(MOVIES_CACHE, page, pageSize, new TypeToken<List<Poster>>(){}.getType());
    }
    
    /**
     * Get paginated TV series
     */
    public List<Poster> getTvSeriesPaginated(int page, int pageSize) {
        return getPaginatedItems(TV_SERIES_CACHE, page, pageSize, new TypeToken<List<Poster>>(){}.getType());
    }
    
    /**
     * Get paginated channels
     */
    public List<Channel> getChannelsPaginated(int page, int pageSize) {
        return getPaginatedItems(CHANNELS_CACHE, page, pageSize, new TypeToken<List<Channel>>(){}.getType());
    }
    
    /**
     * Generic method to retrieve items from chunks
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getItemsFromChunks(String cacheKey, Type type) {
        if (!isInitialized || !isCacheValid()) {
            return new ArrayList<>();
        }
        
        try {
            int chunkCount = Hawk.get(cacheKey + "_chunk_count", 0);
            if (chunkCount == 0) {
                return new ArrayList<>();
            }
            
            List<T> allItems = new ArrayList<>();
            
            for (int i = 0; i < chunkCount; i++) {
                String chunkKey = cacheKey + "_chunk_" + i;
                List<T> chunk = Hawk.get(chunkKey, new ArrayList<T>());
                allItems.addAll(chunk);
            }
            
            Log.d(TAG, "Retrieved " + allItems.size() + " items from " + chunkCount + " chunks for " + cacheKey);
            return allItems;
            
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving items from chunks for " + cacheKey, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get paginated items for memory efficiency
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getPaginatedItems(String cacheKey, int page, int pageSize, Type type) {
        if (!isInitialized || !isCacheValid()) {
            return new ArrayList<>();
        }
        
        try {
            int chunkCount = Hawk.get(cacheKey + "_chunk_count", 0);
            if (chunkCount == 0) {
                return new ArrayList<>();
            }
            
            int startIndex = page * pageSize;
            int endIndex = startIndex + pageSize;
            
            List<T> result = new ArrayList<>();
            int currentIndex = 0;
            
            for (int i = 0; i < chunkCount && result.size() < pageSize; i++) {
                String chunkKey = cacheKey + "_chunk_" + i;
                List<T> chunk = Hawk.get(chunkKey, new ArrayList<T>());
                
                for (T item : chunk) {
                    if (currentIndex >= startIndex && currentIndex < endIndex) {
                        result.add(item);
                    }
                    currentIndex++;
                    
                    if (currentIndex >= endIndex) {
                        break;
                    }
                }
            }
            
            Log.d(TAG, "Retrieved page " + page + " (" + result.size() + " items) for " + cacheKey);
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving paginated items for " + cacheKey, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Search movies by title
     */
    public List<Poster> searchMovies(String query) {
        List<Poster> allMovies = getAllMovies();
        return filterPostersByTitle(allMovies, query);
    }
    
    /**
     * Search TV series by title
     */
    public List<Poster> searchTvSeries(String query) {
        List<Poster> allTvSeries = getAllTvSeries();
        return filterPostersByTitle(allTvSeries, query);
    }
    
    /**
     * Search channels by title
     */
    public List<Channel> searchChannels(String query) {
        List<Channel> allChannels = getAllChannels();
        List<Channel> result = new ArrayList<>();
        
        String lowercaseQuery = query.toLowerCase().trim();
        for (Channel channel : allChannels) {
            if (channel.getTitle() != null && 
                channel.getTitle().toLowerCase().contains(lowercaseQuery)) {
                result.add(channel);
            }
        }
        
        return result;
    }
    
    /**
     * Filter movies by genre
     */
    public List<Poster> getMoviesByGenre(int genreId) {
        List<Poster> allMovies = getAllMovies();
        List<Poster> result = new ArrayList<>();
        
        for (Poster movie : allMovies) {
            if (movie.getGenres() != null) {
                for (Genre genre : movie.getGenres()) {
                    if (genre.getId() == genreId) {
                        result.add(movie);
                        break;
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Get movie by ID
     */
    public Poster getMovieById(int movieId) {
        List<Poster> allMovies = getAllMovies();
        for (Poster movie : allMovies) {
            if (movie.getId() == movieId) {
                return movie;
            }
        }
        return null;
    }
    
    /**
     * Get TV series by ID
     */
    public Poster getTvSeriesById(int seriesId) {
        List<Poster> allTvSeries = getAllTvSeries();
        for (Poster series : allTvSeries) {
            if (series.getId() == seriesId) {
                return series;
            }
        }
        return null;
    }
    
    /**
     * Get channel by ID
     */
    public Channel getChannelById(int channelId) {
        List<Channel> allChannels = getAllChannels();
        for (Channel channel : allChannels) {
            if (channel.getId() == channelId) {
                return channel;
            }
        }
        return null;
    }
    
    /**
     * Get cached genres
     */
    public List<Genre> getGenres() {
        if (!isInitialized || !isCacheValid()) {
            return new ArrayList<>();
        }
        return Hawk.get(GENRES_CACHE, new ArrayList<Genre>());
    }
    
    /**
     * Get cached categories
     */
    public List<Category> getCategories() {
        if (!isInitialized || !isCacheValid()) {
            return new ArrayList<>();
        }
        return Hawk.get(CATEGORIES_CACHE, new ArrayList<Category>());
    }
    
    /**
     * Get cached countries
     */
    public List<Country> getCountries() {
        if (!isInitialized || !isCacheValid()) {
            return new ArrayList<>();
        }
        return Hawk.get(COUNTRIES_CACHE, new ArrayList<Country>());
    }
    
    /**
     * Get cached home data
     */
    public JsonApiResponse.HomeData getHomeData() {
        if (!isInitialized || !isCacheValid()) {
            return null;
        }
        return Hawk.get(HOME_DATA_CACHE, null);
    }
    
    /**
     * Check if cache is valid (not expired)
     */
    public boolean isCacheValid() {
        if (!isInitialized) {
            return false;
        }
        
        long lastUpdate = Hawk.get(LAST_UPDATE_TIME, 0L);
        long currentTime = System.currentTimeMillis();
        
        boolean isValid = (currentTime - lastUpdate) < CACHE_EXPIRY_TIME;
        Log.d(TAG, "Cache valid: " + isValid + " (age: " + (currentTime - lastUpdate) + "ms)");
        
        return isValid;
    }
    
    /**
     * Force refresh cache (mark as invalid)
     */
    public void invalidateCache() {
        Log.d(TAG, "Invalidating cache");
        Hawk.put(LAST_UPDATE_TIME, 0L);
    }
    
    /**
     * Clear all cache data
     */
    public void clearCache() {
        if (!isInitialized) {
            return;
        }
        
        Log.d(TAG, "Clearing all cache data");
        
        // Clear main caches
        Hawk.delete(FULL_API_RESPONSE);
        clearChunks(MOVIES_CACHE);
        clearChunks(TV_SERIES_CACHE);
        clearChunks(CHANNELS_CACHE);
        clearChunks(ACTORS_CACHE);
        Hawk.delete(GENRES_CACHE);
        Hawk.delete(CATEGORIES_CACHE);
        Hawk.delete(COUNTRIES_CACHE);
        Hawk.delete(HOME_DATA_CACHE);
        
        // Clear metadata
        Hawk.delete(LAST_UPDATE_TIME);
        Hawk.delete(CACHE_VERSION);
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        if (!isInitialized) {
            return new CacheStats(0, 0, 0, 0, false);
        }
        
        int movieCount = getChunkItemCount(MOVIES_CACHE);
        int tvSeriesCount = getChunkItemCount(TV_SERIES_CACHE);
        int channelCount = getChunkItemCount(CHANNELS_CACHE);
        int actorCount = getChunkItemCount(ACTORS_CACHE);
        
        return new CacheStats(movieCount, tvSeriesCount, channelCount, actorCount, isCacheValid());
    }
    
    // Helper methods
    
    private void validateCacheVersion() {
        int currentVersion = Hawk.get(CACHE_VERSION, 0);
        if (currentVersion < CURRENT_CACHE_VERSION) {
            Log.d(TAG, "Cache version outdated. Clearing cache.");
            clearCache();
        }
    }
    
    private void clearChunks(String cacheKey) {
        int chunkCount = Hawk.get(cacheKey + "_chunk_count", 0);
        for (int i = 0; i < chunkCount; i++) {
            String chunkKey = cacheKey + "_chunk_" + i;
            Hawk.delete(chunkKey);
        }
        Hawk.delete(cacheKey + "_chunk_count");
    }
    
    private int getChunkItemCount(String cacheKey) {
        int chunkCount = Hawk.get(cacheKey + "_chunk_count", 0);
        int totalItems = 0;
        
        for (int i = 0; i < chunkCount; i++) {
            String chunkKey = cacheKey + "_chunk_" + i;
            List<?> chunk = Hawk.get(chunkKey, new ArrayList<>());
            totalItems += chunk.size();
        }
        
        return totalItems;
    }
    
    private List<Poster> filterPostersByTitle(List<Poster> posters, String query) {
        List<Poster> result = new ArrayList<>();
        String lowercaseQuery = query.toLowerCase().trim();
        
        for (Poster poster : posters) {
            if (poster.getTitle() != null && 
                poster.getTitle().toLowerCase().contains(lowercaseQuery)) {
                result.add(poster);
            }
        }
        
        return result;
    }
    
    /**
     * Cache statistics class
     */
    public static class CacheStats {
        public final int movieCount;
        public final int tvSeriesCount;
        public final int channelCount;
        public final int actorCount;
        public final boolean isValid;
        
        public CacheStats(int movieCount, int tvSeriesCount, int channelCount, int actorCount, boolean isValid) {
            this.movieCount = movieCount;
            this.tvSeriesCount = tvSeriesCount;
            this.channelCount = channelCount;
            this.actorCount = actorCount;
            this.isValid = isValid;
        }
        
        public int getTotalItems() {
            return movieCount + tvSeriesCount + channelCount + actorCount;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{movies=%d, tvSeries=%d, channels=%d, actors=%d, total=%d, valid=%s}", 
                movieCount, tvSeriesCount, channelCount, actorCount, getTotalItems(), isValid);
        }
    }
}