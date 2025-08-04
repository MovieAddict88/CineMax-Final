package my.cinemax.app.free.api;

import android.app.Activity;
import android.util.Log;

import com.jakewharton.picasso.OkHttp3Downloader;
import my.cinemax.app.free.BuildConfig;
import my.cinemax.app.free.MyApplication;
import my.cinemax.app.free.Provider.PrefManager;
import my.cinemax.app.free.config.Global;
import my.cinemax.app.free.entity.ApiResponse;
import my.cinemax.app.free.entity.JsonApiResponse;
import my.cinemax.app.free.entity.Poster;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

import static okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS;
import static okhttp3.logging.HttpLoggingInterceptor.Level.NONE;

/**
 * Created by Tamim on 28/09/2019.
 * Updated to use GitHub JSON API exclusively
 */

public class apiClient {
    private static Retrofit retrofit = null;
    private static Retrofit githubRetrofit = null;
    private static boolean isPicassoInitialized = false;
    private static final String CACHE_CONTROL = "Cache-Control";
    
    // GitHub API base URL - all data comes from GitHub now
    private static final String GITHUB_API_BASE_URL = "https://raw.githubusercontent.com/MovieAddict88/movie-api/main/";
    
    // Fallback URL in case the main one doesn't work
    private static final String FALLBACK_API_BASE_URL = "https://api.github.com/repos/your-username/movie-api/contents/";

    /**
     * Get the main GitHub API client for all movie data
     * This replaces the old API system completely
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                    .addInterceptor(provideHttpLoggingInterceptor())
                    .addInterceptor(provideOfflineCacheInterceptor())
                    .addNetworkInterceptor(provideCacheInterceptor())
                    .cache(provideCache())
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            // Only set Picasso singleton if not already set to prevent crash
            if (!isPicassoInitialized) {
                try {
                    OkHttp3Downloader okHttp3Downloader = new OkHttp3Downloader(okHttpClient);
                    Picasso picasso = new Picasso.Builder(MyApplication.getInstance())
                            .downloader(okHttp3Downloader)
                            .build();
                    
                    Picasso.setSingletonInstance(picasso);
                    isPicassoInitialized = true;
                    Log.d("apiClient", "Picasso singleton initialized successfully");
                } catch (IllegalStateException e) {
                    // Singleton already exists, ignore
                    isPicassoInitialized = true;
                    Log.d("apiClient", "Picasso singleton already initialized, reusing existing instance");
                } catch (Exception e) {
                    Log.e("apiClient", "Error initializing Picasso", e);
                }
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(GITHUB_API_BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Get GitHub JSON API client (same as getClient() for consistency)
     */
    public static Retrofit getJsonApiClient() {
        return getClient();
    }

    private static Cache provideCache() {
        Cache cache = null;
        try {
            cache = new Cache(new File(MyApplication.getInstance().getCacheDir(), "movie-api-cache"),
                    20 * 1024 * 1024); // 20 MB cache for better performance
        } catch (Exception e) {
            Timber.e(e, "Could not create Cache!");
        }
        return cache;
    }

    private static HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor =
                new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        Timber.d(message);
                        Log.v("GITHUB_API", message);
                    }
                });
        httpLoggingInterceptor.setLevel(BuildConfig.DEBUG ? HEADERS : NONE);
        return httpLoggingInterceptor;
    }

    public static Interceptor provideCacheInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response response = chain.proceed(chain.request());
                // Cache for 5 minutes for better performance
                CacheControl cacheControl = new CacheControl.Builder()
                        .maxAge(5, TimeUnit.MINUTES)
                        .build();
                return response.newBuilder()
                        .header(CACHE_CONTROL, cacheControl.toString())
                        .build();
            }
        };
    }

    public static Interceptor provideOfflineCacheInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                if (!MyApplication.hasNetwork()) {
                    CacheControl cacheControl = new CacheControl.Builder()
                            .maxStale(7, TimeUnit.DAYS) // Cache for 7 days when offline
                            .build();
                    request = request.newBuilder()
                            .cacheControl(cacheControl)
                            .build();
                }
                return chain.proceed(request);
            }
        };
    }

    // ===== GITHUB JSON API METHODS =====
    
    /**
     * Fetch all data from the GitHub JSON API
     */
    public static void getJsonApiData(Callback<JsonApiResponse> callback) {
        Retrofit retrofit = getClient();
        apiRest service = retrofit.create(apiRest.class);
        Call<JsonApiResponse> call = service.getJsonApiData();
        call.enqueue(callback);
    }
    
    /**
     * Fetch home data from the GitHub JSON API
     */
    public static void getHomeDataFromJson(Callback<JsonApiResponse> callback) {
        getJsonApiData(callback);
    }
    
    /**
     * Fetch movies from the GitHub JSON API
     */
    public static void getMoviesFromJson(Callback<JsonApiResponse> callback) {
        getJsonApiData(callback);
    }
    
    /**
     * Fetch channels from the GitHub JSON API
     */
    public static void getChannelsFromJson(Callback<JsonApiResponse> callback) {
        getJsonApiData(callback);
    }
    
    /**
     * Fetch actors from the GitHub JSON API
     */
    public static void getActorsFromJson(Callback<JsonApiResponse> callback) {
        getJsonApiData(callback);
    }
    
    /**
     * Fetch genres from the GitHub JSON API
     */
    public static void getGenresFromJson(Callback<JsonApiResponse> callback) {
        getJsonApiData(callback);
    }
    
    /**
     * Get a specific movie by ID from the GitHub JSON API
     */
    public static void getMovieByIdFromJson(int movieId, Callback<JsonApiResponse> callback) {
        getMoviesFromJson(new Callback<JsonApiResponse>() {
            @Override
            public void onResponse(Call<JsonApiResponse> call, retrofit2.Response<JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonApiResponse apiResponse = response.body();
                    if (apiResponse.getMovies() != null) {
                        // Find the movie by ID
                        for (Poster movie : apiResponse.getMovies()) {
                            if (movie.getId() == movieId) {
                                // Create a new response with just this movie
                                JsonApiResponse singleMovieResponse = new JsonApiResponse();
                                java.util.List<Poster> singleMovieList = new java.util.ArrayList<>();
                                singleMovieList.add(movie);
                                singleMovieResponse.setMovies(singleMovieList);
                                callback.onResponse(call, retrofit2.Response.success(singleMovieResponse));
                                return;
                            }
                        }
                    }
                    callback.onFailure(call, new Exception("Movie not found"));
                } else {
                    callback.onFailure(call, new Exception("Failed to load movies"));
                }
            }
            
            @Override
            public void onFailure(Call<JsonApiResponse> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }
    
    /**
     * Get video sources for a movie from the GitHub JSON API
     */
    public static void getMovieVideoSources(int movieId, Callback<JsonApiResponse> callback) {
        getMovieByIdFromJson(movieId, callback);
    }
    
    /**
     * Get ads configuration from GitHub JSON API
     */
    public static void getAdsConfigFromJson(Callback<JsonApiResponse> callback) {
        Retrofit adsRetrofit = new Retrofit.Builder()
                .baseUrl(GITHUB_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        apiRest adsService = adsRetrofit.create(apiRest.class);
        Call<JsonApiResponse> call = adsService.getAdsConfig();
        
        call.enqueue(new Callback<JsonApiResponse>() {
            @Override
            public void onResponse(Call<JsonApiResponse> call, retrofit2.Response<JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResponse(call, response);
                } else {
                    callback.onFailure(call, new Exception("Failed to load ads configuration"));
                }
            }
            
            @Override
            public void onFailure(Call<JsonApiResponse> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }
    
    /**
     * Get GitHub JSON API data with custom callback
     */
    public static void getJsonApiData(JsonApiCallback callback) {
        Log.d("API_CLIENT", "Attempting to load data from: " + GITHUB_API_BASE_URL + "free_movie_api.json");
        
        getJsonApiData(new Callback<JsonApiResponse>() {
            @Override
            public void onResponse(Call<JsonApiResponse> call, retrofit2.Response<JsonApiResponse> response) {
                Log.d("API_CLIENT", "Response received - Success: " + response.isSuccessful() + ", Code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("API_CLIENT", "Successfully loaded JSON data");
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "Failed to load data from GitHub JSON API. Response code: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += ", Error: " + response.errorBody().string();
                        } catch (IOException e) {
                            errorMsg += ", Error body could not be read";
                        }
                    }
                    Log.e("API_CLIENT", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<JsonApiResponse> call, Throwable t) {
                String errorMsg = "Network error: " + t.getMessage();
                Log.e("API_CLIENT", errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    /**
     * Load ads configuration and update PrefManager
     */
    public static void loadAdsConfigAndUpdatePrefs(Activity activity, AdsConfigCallback callback) {
        getAdsConfigFromJson(new Callback<JsonApiResponse>() {
            @Override
            public void onResponse(Call<JsonApiResponse> call, retrofit2.Response<JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonApiResponse.AdsConfig adsConfig = response.body().getAdsConfig();
                    if (adsConfig != null) {
                        PrefManager prefManager = new PrefManager(activity.getApplicationContext());
                        
                        // Update AdMob IDs
                        if (adsConfig.getAdmob() != null) {
                            prefManager.setString("ADMIN_BANNER_ADMOB_ID", adsConfig.getAdmob().getBannerId());
                            prefManager.setString("ADMIN_INTERSTITIAL_ADMOB_ID", adsConfig.getAdmob().getInterstitialId());
                            prefManager.setString("ADMIN_REWARDED_ADMOB_ID", adsConfig.getAdmob().getRewardedId());
                            prefManager.setString("ADMIN_NATIVE_ADMOB_ID", adsConfig.getAdmob().getNativeId());
                        }
                        
                        // Update Facebook IDs
                        if (adsConfig.getFacebook() != null) {
                            prefManager.setString("ADMIN_BANNER_FACEBOOK_ID", adsConfig.getFacebook().getBannerId());
                            prefManager.setString("ADMIN_INTERSTITIAL_FACEBOOK_ID", adsConfig.getFacebook().getInterstitialId());
                            prefManager.setString("ADMIN_REWARDED_FACEBOOK_ID", adsConfig.getFacebook().getRewardedId());
                            prefManager.setString("ADMIN_NATIVE_FACEBOOK_ID", adsConfig.getFacebook().getNativeId());
                        }
                        
                        // Update settings
                        if (adsConfig.getSettings() != null) {
                            JsonApiResponse.AdsSettings settings = adsConfig.getSettings();
                            
                            prefManager.setString("ADMIN_BANNER_TYPE", settings.getBannerType());
                            prefManager.setString("ADMIN_INTERSTITIAL_TYPE", settings.getInterstitialType());
                            prefManager.setString("ADMIN_NATIVE_TYPE", settings.getNativeType());
                            prefManager.setInt("ADMIN_INTERSTITIAL_CLICKS", settings.getInterstitialClicks());
                            prefManager.setString("ADMIN_NATIVE_LINES", String.valueOf(settings.getNativeLines()));
                            
                            // Enable/disable ads
                            prefManager.setString("ADMIN_BANNER_TYPE", settings.isBannerEnabled() ? settings.getBannerType() : "FALSE");
                            prefManager.setString("ADMIN_INTERSTITIAL_TYPE", settings.isInterstitialEnabled() ? settings.getInterstitialType() : "FALSE");
                            prefManager.setString("ADMIN_NATIVE_TYPE", settings.isNativeEnabled() ? settings.getNativeType() : "FALSE");
                        }
                        
                        callback.onSuccess("Ads configuration updated successfully from GitHub");
                    } else {
                        callback.onError("No ads configuration found in GitHub JSON");
                    }
                } else {
                    callback.onError("Failed to load ads configuration from GitHub");
                }
            }
            
            @Override
            public void onFailure(Call<JsonApiResponse> call, Throwable t) {
                callback.onError("Failed to load ads config from GitHub: " + t.getMessage());
            }
        });
    }

    // ===== DEPRECATED OLD API METHODS (Kept for backward compatibility but redirect to GitHub) =====
    
    /**
     * @deprecated Use getJsonApiData() instead. This now redirects to GitHub API.
     */
    @Deprecated
    public static Retrofit initClient() {
        return getClient();
    }
    
    /**
     * @deprecated Old API system replaced with GitHub JSON API
     */
    @Deprecated
    public static void setClient(retrofit2.Response<ApiResponse> response, Activity activity, PrefManager prf) {
        // Load GitHub JSON data instead
        loadAdsConfigAndUpdatePrefs(activity, new AdsConfigCallback() {
            @Override
            public void onSuccess(String message) {
                prf.setString("formatted", "true");
                Log.d("API_CLIENT", "GitHub API loaded successfully: " + message);
            }
            
            @Override
            public void onError(String error) {
                prf.setString("formatted", "false");
                Log.e("API_CLIENT", "GitHub API load failed: " + error);
            }
        });
    }
    
    /**
     * @deprecated Old API system replaced with GitHub JSON API
     */
    @Deprecated
    public static String LoadClientData(Activity activity) {
        return activity.getApplicationContext().getPackageName();
    }
    
    /**
     * @deprecated Old API system replaced with GitHub JSON API. This now loads GitHub data.
     */
    @Deprecated
    public static void FormatData(final Activity activity, Object o) {
        try {
            final PrefManager prf = new PrefManager(activity.getApplication());
            
            if (!prf.getString("formatted").equals("true")) {
                if (check(activity)) {
                    // Load GitHub JSON data instead of old API
                    loadAdsConfigAndUpdatePrefs(activity, new AdsConfigCallback() {
                        @Override
                        public void onSuccess(String message) {
                            prf.setString("formatted", "true");
                            Log.d("API_CLIENT", "GitHub JSON API data loaded successfully");
                        }
                        
                        @Override
                        public void onError(String error) {
                            prf.setString("formatted", "false");
                            Log.e("API_CLIENT", "Failed to load GitHub JSON API data: " + error);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e("API_CLIENT", "Error in FormatData: " + e.getMessage());
        }
    }
    
    public static boolean check(Activity activity) {
        final PrefManager prf = new PrefManager(activity.getApplication());
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = sdf.format(c.getTime());

        if (prf.getString("LAST_DATA_LOAD").equals("")) {
            prf.setString("LAST_DATA_LOAD", strDate);
            return true;
        } else {
            String toyBornTime = prf.getString("LAST_DATA_LOAD");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            try {
                Date oldDate = dateFormat.parse(toyBornTime);
                Date currentDate = new Date();
                long diff = currentDate.getTime() - oldDate.getTime();
                long seconds = diff / 1000;

                if (seconds > 300) { // Check every 5 minutes instead of 15 seconds
                    prf.setString("LAST_DATA_LOAD", strDate);
                    return true;
                }
            } catch (ParseException e) {
                e.printStackTrace();
                return true;
            }
        }
        return false;
    }
    
    // Callback interface for ads configuration
    public interface AdsConfigCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // Callback interface for JSON API data
    public interface JsonApiCallback {
        void onSuccess(JsonApiResponse jsonResponse);
        void onError(String error);
    }
}
