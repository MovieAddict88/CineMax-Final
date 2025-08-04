package my.cinemax.app.free.Utils;

import android.graphics.Bitmap;
import android.util.LruCache;
import android.util.Log;
import my.cinemax.app.free.entity.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory Cache Manager - First Layer of Caching System
 * 
 * Provides ultra-fast in-memory caching for frequently accessed data.
 * Uses LRU (Least Recently Used) eviction policy to manage memory efficiently.
 * 
 * Features:
 * - Instant data access (sub-millisecond response times)
 * - Automatic memory management with LRU eviction
 * - Thread-safe operations
 * - Configurable cache sizes
 * - Memory usage monitoring
 */
public class MemoryCacheManager {
    
    private static final String TAG = "MemoryCacheManager";
    private static MemoryCacheManager instance;
    
    // Cache sizes (in entries)
    private static final int MOVIES_CACHE_SIZE = 1000;
    private static final int TV_SERIES_CACHE_SIZE = 500;
    private static final int CHANNELS_CACHE_SIZE = 200;
    private static final int ACTORS_CACHE_SIZE = 300;
    private static final int IMAGE_CACHE_SIZE = 100;
    private static final int SEARCH_RESULTS_CACHE_SIZE = 200;
    
    // LRU Caches
    private final LruCache<Integer, Poster> moviesCache;
    private final LruCache<Integer, Poster> tvSeriesCache;
    private final LruCache<Integer, Channel> channelsCache;
    private final LruCache<Integer, Actor> actorsCache;
    private final LruCache<String, Bitmap> imageCache;
    private final LruCache<String, List<Poster>> searchResultsCache;
    
    // Fast access maps for bulk operations
    private final Map<String, List<Poster>> moviesByGenre;
    private final Map<String, List<Poster>> tvSeriesByGenre;
    private final Map<String, List<Channel>> channelsByCategory;
    
    // Metadata cache
    private final Map<String, Object> metadataCache;
    
    // Statistics
    private long totalHits = 0;
    private long totalMisses = 0;
    private long totalEvictions = 0;
    
    private MemoryCacheManager() {
        // Initialize LRU caches
        moviesCache = new LruCache<Integer, Poster>(MOVIES_CACHE_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, Integer key, Poster oldValue, Poster newValue) {
                if (evicted) {
                    totalEvictions++;
                    Log.d(TAG, "Movie evicted from memory cache: " + key);
                }
            }
        };
        
        tvSeriesCache = new LruCache<Integer, Poster>(TV_SERIES_CACHE_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, Integer key, Poster oldValue, Poster newValue) {
                if (evicted) {
                    totalEvictions++;
                    Log.d(TAG, "TV Series evicted from memory cache: " + key);
                }
            }
        };
        
        channelsCache = new LruCache<Integer, Channel>(CHANNELS_CACHE_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, Integer key, Channel oldValue, Channel newValue) {
                if (evicted) {
                    totalEvictions++;
                    Log.d(TAG, "Channel evicted from memory cache: " + key);
                }
            }
        };
        
        actorsCache = new LruCache<Integer, Actor>(ACTORS_CACHE_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, Integer key, Actor oldValue, Actor newValue) {
                if (evicted) {
                    totalEvictions++;
                    Log.d(TAG, "Actor evicted from memory cache: " + key);
                }
            }
        };
        
        imageCache = new LruCache<String, Bitmap>(IMAGE_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // Calculate memory usage in bytes
                return bitmap.getByteCount();
            }
            
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (evicted) {
                    totalEvictions++;
                    Log.d(TAG, "Image evicted from memory cache: " + key);
                }
            }
        };
        
        searchResultsCache = new LruCache<String, List<Poster>>(SEARCH_RESULTS_CACHE_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, String key, List<Poster> oldValue, List<Poster> newValue) {
                if (evicted) {
                    totalEvictions++;
                    Log.d(TAG, "Search results evicted from memory cache: " + key);
                }
            }
        };
        
        // Initialize fast access maps
        moviesByGenre = new ConcurrentHashMap<>();
        tvSeriesByGenre = new ConcurrentHashMap<>();
        channelsByCategory = new ConcurrentHashMap<>();
        metadataCache = new ConcurrentHashMap<>();
        
        Log.d(TAG, "MemoryCacheManager initialized with sizes - Movies: " + MOVIES_CACHE_SIZE + 
              ", TV Series: " + TV_SERIES_CACHE_SIZE + ", Channels: " + CHANNELS_CACHE_SIZE);
    }
    
    public static synchronized MemoryCacheManager getInstance() {
        if (instance == null) {
            instance = new MemoryCacheManager();
        }
        return instance;
    }
    
    // ==================== MOVIES CACHE ====================
    
    public void cacheMovie(Poster movie) {
        if (movie != null && movie.getId() != null) {
            moviesCache.put(movie.getId(), movie);
            Log.v(TAG, "Movie cached in memory: " + movie.getId());
        }
    }
    
    public void cacheMovies(List<Poster> movies) {
        if (movies != null) {
            for (Poster movie : movies) {
                cacheMovie(movie);
            }
            Log.d(TAG, "Cached " + movies.size() + " movies in memory");
        }
    }
    
    public Poster getMovie(Integer movieId) {
        Poster movie = moviesCache.get(movieId);
        if (movie != null) {
            totalHits++;
            Log.v(TAG, "Movie hit in memory cache: " + movieId);
        } else {
            totalMisses++;
            Log.v(TAG, "Movie miss in memory cache: " + movieId);
        }
        return movie;
    }
    
    public List<Poster> getMoviesByGenre(String genre) {
        List<Poster> movies = moviesByGenre.get(genre);
        if (movies != null) {
            totalHits++;
            Log.v(TAG, "Movies by genre hit in memory cache: " + genre);
        } else {
            totalMisses++;
            Log.v(TAG, "Movies by genre miss in memory cache: " + genre);
        }
        return movies;
    }
    
    public void cacheMoviesByGenre(String genre, List<Poster> movies) {
        if (genre != null && movies != null) {
            moviesByGenre.put(genre, new ArrayList<>(movies));
            Log.d(TAG, "Cached " + movies.size() + " movies for genre: " + genre);
        }
    }
    
    // ==================== TV SERIES CACHE ====================
    
    public void cacheTvSeries(Poster series) {
        if (series != null && series.getId() != null) {
            tvSeriesCache.put(series.getId(), series);
            Log.v(TAG, "TV Series cached in memory: " + series.getId());
        }
    }
    
    public void cacheTvSeries(List<Poster> series) {
        if (series != null) {
            for (Poster s : series) {
                cacheTvSeries(s);
            }
            Log.d(TAG, "Cached " + series.size() + " TV series in memory");
        }
    }
    
    public Poster getTvSeries(Integer seriesId) {
        Poster series = tvSeriesCache.get(seriesId);
        if (series != null) {
            totalHits++;
            Log.v(TAG, "TV Series hit in memory cache: " + seriesId);
        } else {
            totalMisses++;
            Log.v(TAG, "TV Series miss in memory cache: " + seriesId);
        }
        return series;
    }
    
    public List<Poster> getTvSeriesByGenre(String genre) {
        List<Poster> series = tvSeriesByGenre.get(genre);
        if (series != null) {
            totalHits++;
            Log.v(TAG, "TV Series by genre hit in memory cache: " + genre);
        } else {
            totalMisses++;
            Log.v(TAG, "TV Series by genre miss in memory cache: " + genre);
        }
        return series;
    }
    
    public void cacheTvSeriesByGenre(String genre, List<Poster> series) {
        if (genre != null && series != null) {
            tvSeriesByGenre.put(genre, new ArrayList<>(series));
            Log.d(TAG, "Cached " + series.size() + " TV series for genre: " + genre);
        }
    }
    
    // ==================== CHANNELS CACHE ====================
    
    public void cacheChannel(Channel channel) {
        if (channel != null && channel.getId() != null) {
            channelsCache.put(channel.getId(), channel);
            Log.v(TAG, "Channel cached in memory: " + channel.getId());
        }
    }
    
    public void cacheChannels(List<Channel> channels) {
        if (channels != null) {
            for (Channel channel : channels) {
                cacheChannel(channel);
            }
            Log.d(TAG, "Cached " + channels.size() + " channels in memory");
        }
    }
    
    public Channel getChannel(Integer channelId) {
        Channel channel = channelsCache.get(channelId);
        if (channel != null) {
            totalHits++;
            Log.v(TAG, "Channel hit in memory cache: " + channelId);
        } else {
            totalMisses++;
            Log.v(TAG, "Channel miss in memory cache: " + channelId);
        }
        return channel;
    }
    
    public List<Channel> getChannelsByCategory(String category) {
        List<Channel> channels = channelsByCategory.get(category);
        if (channels != null) {
            totalHits++;
            Log.v(TAG, "Channels by category hit in memory cache: " + category);
        } else {
            totalMisses++;
            Log.v(TAG, "Channels by category miss in memory cache: " + category);
        }
        return channels;
    }
    
    public void cacheChannelsByCategory(String category, List<Channel> channels) {
        if (category != null && channels != null) {
            channelsByCategory.put(category, new ArrayList<>(channels));
            Log.d(TAG, "Cached " + channels.size() + " channels for category: " + category);
        }
    }
    
    // ==================== ACTORS CACHE ====================
    
    public void cacheActor(Actor actor) {
        if (actor != null && actor.getId() != null) {
            actorsCache.put(actor.getId(), actor);
            Log.v(TAG, "Actor cached in memory: " + actor.getId());
        }
    }
    
    public void cacheActors(List<Actor> actors) {
        if (actors != null) {
            for (Actor actor : actors) {
                cacheActor(actor);
            }
            Log.d(TAG, "Cached " + actors.size() + " actors in memory");
        }
    }
    
    public Actor getActor(Integer actorId) {
        Actor actor = actorsCache.get(actorId);
        if (actor != null) {
            totalHits++;
            Log.v(TAG, "Actor hit in memory cache: " + actorId);
        } else {
            totalMisses++;
            Log.v(TAG, "Actor miss in memory cache: " + actorId);
        }
        return actor;
    }
    
    // ==================== IMAGE CACHE ====================
    
    public void cacheImage(String url, Bitmap bitmap) {
        if (url != null && bitmap != null) {
            imageCache.put(url, bitmap);
            Log.v(TAG, "Image cached in memory: " + url);
        }
    }
    
    public Bitmap getImage(String url) {
        Bitmap bitmap = imageCache.get(url);
        if (bitmap != null) {
            totalHits++;
            Log.v(TAG, "Image hit in memory cache: " + url);
        } else {
            totalMisses++;
            Log.v(TAG, "Image miss in memory cache: " + url);
        }
        return bitmap;
    }
    
    // ==================== SEARCH RESULTS CACHE ====================
    
    public void cacheSearchResults(String query, List<Poster> results) {
        if (query != null && results != null) {
            searchResultsCache.put(query.toLowerCase(), new ArrayList<>(results));
            Log.d(TAG, "Search results cached for query: " + query + " (" + results.size() + " results)");
        }
    }
    
    public List<Poster> getSearchResults(String query) {
        List<Poster> results = searchResultsCache.get(query.toLowerCase());
        if (results != null) {
            totalHits++;
            Log.v(TAG, "Search results hit in memory cache: " + query);
        } else {
            totalMisses++;
            Log.v(TAG, "Search results miss in memory cache: " + query);
        }
        return results;
    }
    
    // ==================== METADATA CACHE ====================
    
    public void cacheMetadata(String key, Object value) {
        if (key != null && value != null) {
            metadataCache.put(key, value);
            Log.v(TAG, "Metadata cached: " + key);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadataCache.get(key);
        if (value != null && type.isInstance(value)) {
            totalHits++;
            Log.v(TAG, "Metadata hit in memory cache: " + key);
            return (T) value;
        } else {
            totalMisses++;
            Log.v(TAG, "Metadata miss in memory cache: " + key);
            return null;
        }
    }
    
    // ==================== BULK OPERATIONS ====================
    
    public void cacheAllData(JsonApiResponse response) {
        if (response != null) {
            // Cache movies
            if (response.getMovies() != null) {
                cacheMovies(response.getMovies());
            }
            
            // Cache TV series
            if (response.getMovies() != null) {
                List<Poster> tvSeries = new ArrayList<>();
                for (Poster poster : response.getMovies()) {
                    if ("series".equals(poster.getType())) {
                        tvSeries.add(poster);
                    }
                }
                cacheTvSeries(tvSeries);
            }
            
            // Cache channels
            if (response.getChannels() != null) {
                cacheChannels(response.getChannels());
            }
            
            // Cache actors
            if (response.getActors() != null) {
                cacheActors(response.getActors());
            }
            
            Log.d(TAG, "All data cached in memory successfully");
        }
    }
    
    // ==================== CACHE MANAGEMENT ====================
    
    public void clearAllCaches() {
        moviesCache.evictAll();
        tvSeriesCache.evictAll();
        channelsCache.evictAll();
        actorsCache.evictAll();
        imageCache.evictAll();
        searchResultsCache.evictAll();
        
        moviesByGenre.clear();
        tvSeriesByGenre.clear();
        channelsByCategory.clear();
        metadataCache.clear();
        
        totalHits = 0;
        totalMisses = 0;
        totalEvictions = 0;
        
        Log.d(TAG, "All memory caches cleared");
    }
    
    public void clearImageCache() {
        imageCache.evictAll();
        Log.d(TAG, "Image cache cleared");
    }
    
    public void clearSearchCache() {
        searchResultsCache.evictAll();
        Log.d(TAG, "Search cache cleared");
    }
    
    // ==================== STATISTICS ====================
    
    public CacheStats getCacheStats() {
        return new CacheStats(
            moviesCache.size(),
            tvSeriesCache.size(),
            channelsCache.size(),
            actorsCache.size(),
            imageCache.size(),
            searchResultsCache.size(),
            totalHits,
            totalMisses,
            totalEvictions,
            getHitRate()
        );
    }
    
    public double getHitRate() {
        long totalRequests = totalHits + totalMisses;
        return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
    }
    
    public long getMemoryUsage() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
    
    public static class CacheStats {
        public final int movieCount;
        public final int tvSeriesCount;
        public final int channelCount;
        public final int actorCount;
        public final int imageCount;
        public final int searchResultsCount;
        public final long totalHits;
        public final long totalMisses;
        public final long totalEvictions;
        public final double hitRate;
        
        public CacheStats(int movieCount, int tvSeriesCount, int channelCount, int actorCount,
                         int imageCount, int searchResultsCount, long totalHits, long totalMisses,
                         long totalEvictions, double hitRate) {
            this.movieCount = movieCount;
            this.tvSeriesCount = tvSeriesCount;
            this.channelCount = channelCount;
            this.actorCount = actorCount;
            this.imageCount = imageCount;
            this.searchResultsCount = searchResultsCount;
            this.totalHits = totalHits;
            this.totalMisses = totalMisses;
            this.totalEvictions = totalEvictions;
            this.hitRate = hitRate;
        }
        
        public int getTotalItems() {
            return movieCount + tvSeriesCount + channelCount + actorCount + imageCount + searchResultsCount;
        }
        
        @Override
        public String toString() {
            return String.format("MemoryCacheStats{items=%d, hits=%d, misses=%d, hitRate=%.2f%%}",
                getTotalItems(), totalHits, totalMisses, hitRate * 100);
        }
    }
}