package com.cinecraze.free.stream;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.cinecraze.free.stream.models.Category;
import com.cinecraze.free.stream.models.Entry;
import com.cinecraze.free.stream.models.Playlist;
import com.cinecraze.free.stream.net.ApiService;
import com.cinecraze.free.stream.net.RetrofitClient;
import com.cinecraze.free.stream.repository.DataRepository;
import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivityOld extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MovieAdapter movieAdapter;
    private List<Entry> entryList = new ArrayList<>();
    public List<Entry> allEntries = new ArrayList<>();
    private ViewPager2 carouselViewPager;
    private CarouselAdapter carouselAdapter;
    private ImageView gridViewIcon;
    private ImageView listViewIcon;
    private BubbleNavigationConstraintView bottomNavigationView;
    private ImageView searchIcon;
    private ImageView closeSearchIcon;
    private LinearLayout titleLayout;
    private LinearLayout searchLayout;
    private AutoCompleteTextView searchBar;

    private boolean isGridView = true;
    private boolean isSearchVisible = false;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private DataRepository dataRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Set up our custom toolbar (no default ActionBar since we use NoActionBar theme)
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            // Hide the default title since we have our custom title layout
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        }

        recyclerView = findViewById(R.id.recycler_view);
        carouselViewPager = findViewById(R.id.carousel_view_pager);
        gridViewIcon = findViewById(R.id.grid_view_icon);
        listViewIcon = findViewById(R.id.list_view_icon);
        bottomNavigationView = (BubbleNavigationConstraintView) findViewById(R.id.bottom_navigation);
        searchIcon = findViewById(R.id.search_icon);
        closeSearchIcon = findViewById(R.id.close_search_icon);
        titleLayout = findViewById(R.id.title_layout);
        searchLayout = findViewById(R.id.search_layout);
        searchBar = findViewById(R.id.search_bar);

        setupRecyclerView();
        setupCarousel();
        setupBottomNavigation();
        setupViewSwitch();
        setupSearchToggle();

        // Initialize repository
        dataRepository = new DataRepository(this);

        // Load data from cache or API
        loadData();
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
            try {
                // Hide search bar when navigating
                if (isSearchVisible) {
                    hideSearchBar();
                }
                
                if (position == 0) {
                    filterEntries("");
                } else if (position == 1) {
                    filterEntries("Movies");
                } else if (position == 2) {
                    filterEntries("TV Series");
                } else if (position == 3) {
                    filterEntries("Live TV");
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error handling navigation: " + e.getMessage(), e);
            }
        });
    }

    private void setupViewSwitch() {
        gridViewIcon.setOnClickListener(v -> {
            isGridView = false;
            setupRecyclerView();
            gridViewIcon.setVisibility(View.GONE);
            listViewIcon.setVisibility(View.VISIBLE);
        });

        listViewIcon.setOnClickListener(v -> {
            isGridView = true;
            setupRecyclerView();
            listViewIcon.setVisibility(View.GONE);
            gridViewIcon.setVisibility(View.VISIBLE);
        });
    }

    private void setupSearchToggle() {
        try {
            // Show search bar when search icon is clicked
            searchIcon.setOnClickListener(v -> {
                showSearchBar();
            });

            // Hide search bar when close icon is clicked
            closeSearchIcon.setOnClickListener(v -> {
                hideSearchBar();
            });

            // Handle search text changes and item selection
            if (searchBar != null) {
                searchBar.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                        performSearch(searchBar.getText().toString());
                        return true;
                    }
                    return false;
                });
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error setting up search toggle: " + e.getMessage(), e);
        }
    }

    private void showSearchBar() {
        try {
            if (!isSearchVisible && titleLayout != null && searchLayout != null) {
                titleLayout.setVisibility(View.GONE);
                searchLayout.setVisibility(View.VISIBLE);
                isSearchVisible = true;
                
                // Focus on search bar and show keyboard
                if (searchBar != null) {
                    searchBar.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = 
                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(searchBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error showing search bar: " + e.getMessage(), e);
        }
    }

    private void hideSearchBar() {
        try {
            if (isSearchVisible && titleLayout != null && searchLayout != null) {
                searchLayout.setVisibility(View.GONE);
                titleLayout.setVisibility(View.VISIBLE);
                isSearchVisible = false;
                
                // Clear search text and hide keyboard
                if (searchBar != null) {
                    searchBar.setText("");
                    searchBar.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = 
                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                    }
                }
                
                // Reset to show all entries
                filterEntries("");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error hiding search bar: " + e.getMessage(), e);
        }
    }

    private void performSearch(String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                filterEntries("");
                return;
            }

            String searchQuery = query.trim().toLowerCase();
            List<Entry> searchResults = new ArrayList<>();
            
            if (allEntries != null) {
                for (Entry entry : allEntries) {
                    if (entry != null && entry.getTitle() != null && 
                        entry.getTitle().toLowerCase().contains(searchQuery)) {
                        searchResults.add(entry);
                    }
                }
            }
            
            entryList.clear();
            entryList.addAll(searchResults);
            if (movieAdapter != null) {
                movieAdapter.notifyDataSetChanged();
            }
            
            if (searchResults.isEmpty()) {
                Toast.makeText(this, "No results found for: " + query, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error performing search: " + e.getMessage(), e);
            Toast.makeText(this, "Error performing search", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterEntries(String category) {
        List<Entry> filteredEntries = new ArrayList<>();
        if (category.isEmpty()) {
            filteredEntries.addAll(allEntries);
        } else {
            // Use repository to get entries by category
            if (dataRepository != null) {
                filteredEntries = dataRepository.getEntriesByCategory(category);
            }
        }
        entryList.clear();
        entryList.addAll(filteredEntries);
        movieAdapter.notifyDataSetChanged();
    }

    private Playlist playlistCache;

    private void loadData() {
        Log.d("MainActivity", "Loading data using repository");
        findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
        
        dataRepository.getPlaylistData(new DataRepository.DataCallback() {
            @Override
            public void onSuccess(List<Entry> entries) {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                
                allEntries.clear();
                allEntries.addAll(entries);
                
                if (allEntries.isEmpty()) {
                    Toast.makeText(MainActivityOld.this, "No data available", Toast.LENGTH_LONG).show();
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

                setupSearch();
                retryCount = 0; // Reset retry count on success
                Log.d("MainActivity", "Data loaded successfully with " + allEntries.size() + " items");
                Toast.makeText(MainActivityOld.this, "Data loaded (" + allEntries.size() + " items)", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String error) {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                Log.e("MainActivity", "Error loading data: " + error);
                
                if (retryCount < MAX_RETRY_COUNT) {
                    retryCount++;
                    Log.d("MainActivity", "Retrying... Attempt " + retryCount + "/" + MAX_RETRY_COUNT);
                    Toast.makeText(MainActivityOld.this, error + " - Retrying... Attempt " + retryCount + "/" + MAX_RETRY_COUNT, Toast.LENGTH_SHORT).show();
                    // Retry after a short delay
                    new android.os.Handler().postDelayed(() -> loadData(), 2000);
                } else {
                    Log.e("MainActivity", "Failed to load data after " + MAX_RETRY_COUNT + " attempts");
                    Toast.makeText(MainActivityOld.this, "Failed to load data after " + MAX_RETRY_COUNT + " attempts. " + error, Toast.LENGTH_LONG).show();
                    retryCount = 0; // Reset for next manual retry
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            Log.d("MainActivity", "Network available: " + isConnected);
            return isConnected;
        }
        Log.d("MainActivity", "ConnectivityManager is null");
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we need to refresh data (e.g., if we came back from background)
        if (allEntries.isEmpty()) {
            loadData();
        }
    }

    // Method to manually refresh data (can be called from UI)
    public void refreshData() {
        retryCount = 0; // Reset retry count
        if (dataRepository != null) {
            findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
            dataRepository.refreshData(new DataRepository.DataCallback() {
                @Override
                public void onSuccess(List<Entry> entries) {
                    findViewById(R.id.progress_bar).setVisibility(View.GONE);
                    allEntries.clear();
                    allEntries.addAll(entries);
                    filterEntries(""); // Refresh current view
                    setupSearch();
                    Toast.makeText(MainActivityOld.this, "Data refreshed (" + allEntries.size() + " items)", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onError(String error) {
                    findViewById(R.id.progress_bar).setVisibility(View.GONE);
                    Toast.makeText(MainActivityOld.this, "Failed to refresh: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        try {
            // If search is visible, hide it instead of closing the app
            if (isSearchVisible) {
                hideSearchBar();
            } else {
                super.onBackPressed();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error handling back press: " + e.getMessage(), e);
            super.onBackPressed();
        }
    }

    private void setupSearch() {
        try {
            if (searchBar == null) {
                Log.e("MainActivity", "SearchBar is null, cannot setup search");
                return;
            }

            List<String> titles = new ArrayList<>();
            if (allEntries != null) {
                for (Entry entry : allEntries) {
                    if (entry != null && entry.getTitle() != null && !entry.getTitle().trim().isEmpty()) {
                        titles.add(entry.getTitle());
                    }
                }
            }
            
            if (!titles.isEmpty()) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, titles);
                searchBar.setAdapter(adapter);

                searchBar.setOnItemClickListener((parent, view, position, id) -> {
                    try {
                        String selectedTitle = (String) parent.getItemAtPosition(position);
                        if (selectedTitle != null && allEntries != null) {
                            for (Entry entry : allEntries) {
                                if (entry != null && entry.getTitle() != null && entry.getTitle().equals(selectedTitle)) {
                                    Intent intent = new Intent(MainActivityOld.this, DetailsActivity.class);
                                    intent.putExtra("entry", new Gson().toJson(entry));
                                    startActivity(intent);
                                    hideSearchBar(); // Hide search bar after selection
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error handling search item click: " + e.getMessage(), e);
                        Toast.makeText(MainActivityOld.this, "Error opening selected item", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.w("MainActivity", "No titles available for search autocomplete");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error setting up search: " + e.getMessage(), e);
        }
    }
}
