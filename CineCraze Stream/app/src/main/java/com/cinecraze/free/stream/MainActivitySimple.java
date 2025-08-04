package com.cinecraze.free.stream;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.cinecraze.free.stream.models.Category;
import com.cinecraze.free.stream.models.Entry;
import com.cinecraze.free.stream.models.Playlist;
import com.cinecraze.free.stream.net.ApiService;
import com.cinecraze.free.stream.net.RetrofitClient;
import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivitySimple extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MovieAdapter movieAdapter;
    private List<Entry> entryList = new ArrayList<>();
    public List<Entry> allEntries = new ArrayList<>();
    private ViewPager2 carouselViewPager;
    private CarouselAdapter carouselAdapter;
    private ImageView gridViewIcon;
    private ImageView listViewIcon;
    private BubbleNavigationConstraintView bottomNavigationView;

    private boolean isGridView = true;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_simple);

        try {
            recyclerView = findViewById(R.id.recycler_view);
            carouselViewPager = findViewById(R.id.carousel_view_pager);
            gridViewIcon = findViewById(R.id.grid_view_icon);
            listViewIcon = findViewById(R.id.list_view_icon);
            bottomNavigationView = (BubbleNavigationConstraintView) findViewById(R.id.bottom_navigation);
            
            setupRecyclerView();
            setupCarousel();
            setupBottomNavigation();
            setupViewSwitch();

            // Only fetch data if we don't have it already
            if (allEntries.isEmpty()) {
                fetchPlaylist();
            }
        } catch (Exception e) {
            Log.e("MainActivitySimple", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        movieAdapter = new MovieAdapter(this, entryList, isGridView);
        if (isGridView) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        recyclerView.setAdapter(movieAdapter);
    }

    private void setupCarousel() {
        carouselAdapter = new CarouselAdapter(this, new ArrayList<>());
        carouselViewPager.setAdapter(carouselAdapter);
    }

    private void setupBottomNavigation() {
        // Set up navigation change listener
        bottomNavigationView.setNavigationChangeListener((view, position) -> {
            if (position == 0) {
                filterEntries("");
            } else if (position == 1) {
                filterEntries("Movies");
            } else if (position == 2) {
                filterEntries("TV Series");
            } else if (position == 3) {
                filterEntries("Live TV");
            }
        });
    }

    private void setupViewSwitch() {
        gridViewIcon.setOnClickListener(v -> {
            if (!isGridView) {
                isGridView = true;
                gridViewIcon.setVisibility(View.GONE);
                listViewIcon.setVisibility(View.VISIBLE);
                recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
                movieAdapter.setGridView(true);
                movieAdapter.notifyDataSetChanged();
            }
        });

        listViewIcon.setOnClickListener(v -> {
            if (isGridView) {
                isGridView = false;
                gridViewIcon.setVisibility(View.VISIBLE);
                listViewIcon.setVisibility(View.GONE);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                movieAdapter.setGridView(false);
                movieAdapter.notifyDataSetChanged();
            }
        });
    }

    private void filterEntries(String category) {
        if (category.isEmpty()) {
            entryList.clear();
            entryList.addAll(allEntries);
        } else {
            entryList.clear();
            for (Entry entry : allEntries) {
                if (entry != null && entry.getSubCategory() != null && entry.getSubCategory().equals(category)) {
                    entryList.add(entry);
                }
            }
        }
        movieAdapter.notifyDataSetChanged();
    }

    private Playlist playlistCache;

    private void fetchPlaylist() {
        findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
        
        new android.os.Handler().postDelayed(() -> {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<Playlist> call = apiService.getPlaylist();
        call.enqueue(new Callback<Playlist>() {
            @Override
            public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                Log.d("MainActivitySimple", "Response received: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        playlistCache = response.body();
                        allEntries.clear();
                        
                        if (playlistCache.getCategories() != null) {
                            for (Category category : playlistCache.getCategories()) {
                                if (category != null && category.getEntries() != null) {
                                    allEntries.addAll(category.getEntries());
                                }
                            }
                        }
                        
                        if (allEntries.isEmpty()) {
                            Toast.makeText(MainActivitySimple.this, "No data available", Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        filterEntries(""); // Show all entries initially

                        // For now, just use the first 5 entries for the carousel
                        List<Entry> carouselEntries = new ArrayList<>();
                        for (int i = 0; i < 5 && i < allEntries.size(); i++) {
                            carouselEntries.add(allEntries.get(i));
                        }
                        carouselAdapter.setEntries(carouselEntries);
                        carouselAdapter.notifyDataSetChanged();

                        retryCount = 0; // Reset retry count on success
                        Log.d("MainActivitySimple", "Data loaded successfully with " + allEntries.size() + " items");
                        Toast.makeText(MainActivitySimple.this, "Data loaded successfully (" + allEntries.size() + " items)", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e("MainActivitySimple", "Error processing data: " + e.getMessage(), e);
                        Toast.makeText(MainActivitySimple.this, "Error processing data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("MainActivitySimple", "Failed to load data: " + response.code());
                    Toast.makeText(MainActivitySimple.this, "Failed to load data: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Playlist> call, Throwable t) {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                Log.e("MainActivitySimple", "Network failure: " + t.getMessage(), t);
                
                String errorMessage = "Network error";
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("timeout")) {
                        errorMessage = "Connection timeout";
                    } else if (t.getMessage().contains("Unable to resolve host")) {
                        errorMessage = "No internet connection";
                    } else if (t.getMessage().contains("SSL")) {
                        errorMessage = "SSL connection error";
                    } else {
                        errorMessage = "Network error: " + t.getMessage();
                    }
                }
                
                if (retryCount < MAX_RETRY_COUNT) {
                    retryCount++;
                    Log.d("MainActivitySimple", "Retrying... Attempt " + retryCount + "/" + MAX_RETRY_COUNT);
                    Toast.makeText(MainActivitySimple.this, errorMessage + " - Retrying... Attempt " + retryCount + "/" + MAX_RETRY_COUNT, Toast.LENGTH_SHORT).show();
                    // Retry after a short delay
                    new android.os.Handler().postDelayed(() -> fetchPlaylist(), 2000);
                } else {
                    Log.e("MainActivitySimple", "Failed to load data after " + MAX_RETRY_COUNT + " attempts");
                    Toast.makeText(MainActivitySimple.this, "Failed to load data after " + MAX_RETRY_COUNT + " attempts. " + errorMessage, Toast.LENGTH_LONG).show();
                    retryCount = 0; // Reset for next manual retry
                }
            }
        });
        }, 500); // 500ms delay
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            Log.d("MainActivitySimple", "Network available: " + isConnected);
            return isConnected;
        }
        Log.d("MainActivitySimple", "ConnectivityManager is null");
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we need to refresh data (e.g., if we came back from background)
        if (allEntries.isEmpty() && isNetworkAvailable()) {
            fetchPlaylist();
        }
    }

    // Method to manually refresh data (can be called from UI)
    public void refreshData() {
        retryCount = 0; // Reset retry count
        fetchPlaylist();
    }
}