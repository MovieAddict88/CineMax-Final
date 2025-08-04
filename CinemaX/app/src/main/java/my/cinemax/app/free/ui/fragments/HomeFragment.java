package my.cinemax.app.free.ui.fragments;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.util.Log;
import android.os.Handler;

import my.cinemax.app.free.Provider.PrefManager;
import my.cinemax.app.free.R;
import my.cinemax.app.free.api.apiClient;
import my.cinemax.app.free.api.apiRest;
import my.cinemax.app.free.ui.activities.HomeActivity;
import my.cinemax.app.free.entity.Data;
import my.cinemax.app.free.entity.Genre;
import my.cinemax.app.free.entity.JsonApiResponse;
import my.cinemax.app.free.ui.Adapters.HomeAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.FacebookSdk.getApplicationContext;
import es.dmoral.toasty.Toasty;

/**
 * A simple {@link Fragment} subclass.
 * Enhanced with proper loading indicators and cache integration
 */
public class HomeFragment extends Fragment {


    private View view;
    private SwipeRefreshLayout swipe_refresh_layout_home_fragment;
    private LinearLayout linear_layout_load_home_fragment;
    private LinearLayout linear_layout_page_error_home_fragment;
    private RecyclerView recycler_view_home_fragment;
    private RelativeLayout relative_layout_load_more_home_fragment;
    private HomeAdapter homeAdapter;
    private ShimmerFrameLayout shimmer_layout;


    private Genre my_genre_list;
    private List<Data> dataList=new ArrayList<>();
    private GridLayoutManager gridLayoutManager;
    private Button button_try_again;


    private Integer lines_beetween_ads = 2 ;
    private boolean tabletSize;
    private Boolean native_ads_enabled = false ;
    private int type_ads = 0;
    private PrefManager prefManager;
    private Integer item = 0 ;
    
    // Loading state management
    private boolean isDataLoaded = false;
    private boolean isLoadingInProgress = false;

    public HomeFragment() {
        // Required empty public constructor
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        this.view=  inflater.inflate(R.layout.fragment_home, container, false);
        prefManager= new PrefManager(getApplicationContext());

        initViews();
        initActions();
        
        // Show loading immediately when fragment is created
        showShimmerLoading();
        
        // Add timeout to prevent infinite loading (10 seconds)
        new Handler().postDelayed(() -> {
            if (!isDataLoaded && isLoadingInProgress) {
                Log.w("HomeFragment", "Loading timeout, stopping shimmer and showing error");
                isLoadingInProgress = false;
                if (getActivity() != null && isAdded()) {
                    showErrorView();
                }
            }
        }, 10000); // 10 second timeout
        
        Log.d("HomeFragment", "Fragment created, showing shimmer loading");
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        try {
            // Ensure loading state is shown when fragment becomes visible
            if (!isDataLoaded && !isLoadingInProgress && getActivity() != null && isAdded()) {
                Log.d("HomeFragment", "Fragment resumed without data, showing shimmer");
                showShimmerLoading();
                
                // Notify HomeActivity that fragment is ready for data
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).onHomeFragmentReady();
                }
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "Error in onResume", e);
        }
    }

    private void loadData() {
        if (isLoadingInProgress) {
            Log.d("HomeFragment", "Data loading already in progress, skipping");
            return;
        }
        
        isLoadingInProgress = true;
        showShimmerLoading();
        
        Log.d("HomeFragment", "Starting data loading from API");
        
        // Use GitHub JSON API instead of old API
        apiClient.getJsonApiData(new Callback<JsonApiResponse>() {
            @Override
            public void onResponse(Call<JsonApiResponse> call, Response<JsonApiResponse> response) {
                isLoadingInProgress = false;
                
                apiClient.FormatData(getActivity(), null); // Initialize format data
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("HomeFragment", "API response successful, processing data");
                    processApiResponse(response.body());
                } else {
                    Log.e("HomeFragment", "API response unsuccessful");
                    showErrorView();
                }
            }

            @Override
            public void onFailure(Call<JsonApiResponse> call, Throwable t) {
                isLoadingInProgress = false;
                Log.e("HomeFragment", "API call failed: " + t.getMessage());
                showErrorView();
            }
        });
    }
    
    /**
     * Process API response and update UI
     */
    private void processApiResponse(JsonApiResponse apiResponse) {
        if (getActivity() == null || !isAdded()) {
            Log.w("HomeFragment", "Fragment not attached, skipping data processing");
            return;
        }
        
        try {
            dataList.clear();
            dataList.add(new Data().setViewType(0));
            
            // Load slides from GitHub JSON
            if (apiResponse.getHome() != null && apiResponse.getHome().getSlides() != null && 
                apiResponse.getHome().getSlides().size() > 0) {
                Data slideData = new Data();
                slideData.setSlides(apiResponse.getHome().getSlides());
                dataList.add(slideData);
                Log.d("HomeFragment", "Added " + apiResponse.getHome().getSlides().size() + " slides");
            }
            
            // Load channels from GitHub JSON
            if (apiResponse.getHome() != null && apiResponse.getHome().getChannels() != null && 
                apiResponse.getHome().getChannels().size() > 0) {
                Data channelData = new Data();
                channelData.setChannels(apiResponse.getHome().getChannels());
                dataList.add(channelData);
                Log.d("HomeFragment", "Added " + apiResponse.getHome().getChannels().size() + " channels");
            }
            
            // Load actors from GitHub JSON
            if (apiResponse.getHome() != null && apiResponse.getHome().getActors() != null && 
                apiResponse.getHome().getActors().size() > 0) {
                Data actorsData = new Data();
                actorsData.setActors(apiResponse.getHome().getActors());
                dataList.add(actorsData);
                Log.d("HomeFragment", "Added " + apiResponse.getHome().getActors().size() + " actors");
            }
            
            // Load genres from GitHub JSON
            if (apiResponse.getHome() != null && apiResponse.getHome().getGenres() != null && 
                apiResponse.getHome().getGenres().size() > 0) {
                if (my_genre_list != null) {
                    Data genreDataMyList = new Data();
                    genreDataMyList.setGenre(my_genre_list);
                    dataList.add(genreDataMyList);
                }
                for (int i = 0; i < apiResponse.getHome().getGenres().size(); i++) {
                    Data genreData = new Data();
                    genreData.setGenre(apiResponse.getHome().getGenres().get(i));
                    dataList.add(genreData);
                    if (native_ads_enabled){
                        item++;
                        if (item == lines_beetween_ads ){
                            item= 0;
                            if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("FACEBOOK")) {
                                dataList.add(new Data().setViewType(5));
                            }else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("ADMOB")){
                                dataList.add(new Data().setViewType(6));
                            } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("BOTH")){
                                if (type_ads == 0) {
                                    dataList.add(new Data().setViewType(5));
                                    type_ads = 1;
                                }else if (type_ads == 1){
                                    dataList.add(new Data().setViewType(6));
                                    type_ads = 0;
                                }
                            }
                        }
                    }
                }
                Log.d("HomeFragment", "Added " + apiResponse.getHome().getGenres().size() + " genres");
            }
            
            isDataLoaded = true;
            showListView();
            homeAdapter.notifyDataSetChanged();
            
            Log.d("HomeFragment", "Data processing completed successfully");
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error processing API response", e);
            showErrorView();
        }
    }
   
       /**
     * Show shimmer loading effect
     */
    private void showShimmerLoading(){
        if (getActivity() == null || !isAdded() || getView() == null) return;
        
        try {
            if (shimmer_layout != null) {
                shimmer_layout.startShimmer();
            }
            
            if (linear_layout_load_home_fragment != null) {
                linear_layout_load_home_fragment.setVisibility(View.VISIBLE);
            }
            if (linear_layout_page_error_home_fragment != null) {
                linear_layout_page_error_home_fragment.setVisibility(View.GONE);
            }
            if (recycler_view_home_fragment != null) {
                recycler_view_home_fragment.setVisibility(View.GONE);
            }
            
            Log.d("HomeFragment", "Shimmer loading displayed");
        } catch (Exception e) {
            Log.e("HomeFragment", "Error showing shimmer loading", e);
        }
    }
   
    private void showListView(){
        if (getActivity() == null || !isAdded() || getView() == null) return;
        
        try {
            if (shimmer_layout != null) {
                shimmer_layout.stopShimmer();
            }
            
            if (linear_layout_load_home_fragment != null) {
                linear_layout_load_home_fragment.setVisibility(View.GONE);
            }
            if (linear_layout_page_error_home_fragment != null) {
                linear_layout_page_error_home_fragment.setVisibility(View.GONE);
            }
            if (recycler_view_home_fragment != null) {
                recycler_view_home_fragment.setVisibility(View.VISIBLE);
            }
            
            Log.d("HomeFragment", "List view displayed");
        } catch (Exception e) {
            Log.e("HomeFragment", "Error showing list view", e);
        }
    }
    
    private void showErrorView(){
        if (getActivity() == null || !isAdded() || getView() == null) return;
        
        try {
            if (shimmer_layout != null) {
                shimmer_layout.stopShimmer();
            }
            
            if (linear_layout_load_home_fragment != null) {
                linear_layout_load_home_fragment.setVisibility(View.GONE);
            }
            if (linear_layout_page_error_home_fragment != null) {
                linear_layout_page_error_home_fragment.setVisibility(View.VISIBLE);
            }
            if (recycler_view_home_fragment != null) {
                recycler_view_home_fragment.setVisibility(View.GONE);
            }
            
            Log.d("HomeFragment", "Error view displayed");
        } catch (Exception e) {
            Log.e("HomeFragment", "Error showing error view", e);
        }
    }
    
    private void initActions() {
        swipe_refresh_layout_home_fragment.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("HomeFragment", "Pull to refresh triggered");
                // Reset loading state
                isDataLoaded = false;
                
                // Call HomeActivity to refresh data
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).refreshDataFromApi();
                }
                swipe_refresh_layout_home_fragment.setRefreshing(false);
            }
        });
        button_try_again.setOnClickListener(v->{
            Log.d("HomeFragment", "Try again button clicked");
            // Reset loading state
            isDataLoaded = false;
            
            // Call HomeActivity to refresh data
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).refreshDataFromApi();
            }
        });
    }
    
    public boolean checkSUBSCRIBED(){
        if (!prefManager.getString("SUBSCRIBED").equals("TRUE") && !prefManager.getString("NEW_SUBSCRIBE_ENABLED").equals("TRUE")) {
            return false;
        }
        return true;
    }
    
    private void initViews() {

        boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
        if (!prefManager.getString("ADMIN_NATIVE_TYPE").equals("FALSE")){
            native_ads_enabled=true;
            if (tabletSize) {
                lines_beetween_ads=Integer.parseInt(prefManager.getString("ADMIN_NATIVE_LINES"));
            }else{
                lines_beetween_ads=Integer.parseInt(prefManager.getString("ADMIN_NATIVE_LINES"));
            }
        }
        if (checkSUBSCRIBED()) {
            native_ads_enabled=false;
        }
        this.swipe_refresh_layout_home_fragment = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_home_fragment);
        this.linear_layout_load_home_fragment = (LinearLayout) view.findViewById(R.id.linear_layout_load_home_fragment);
        this.linear_layout_page_error_home_fragment = (LinearLayout) view.findViewById(R.id.linear_layout_page_error_home_fragment);
        this.recycler_view_home_fragment = (RecyclerView) view.findViewById(R.id.recycler_view_home_fragment);
        this.relative_layout_load_more_home_fragment = (RelativeLayout) view.findViewById(R.id.relative_layout_load_more_home_fragment);
        this.button_try_again = (Button) view.findViewById(R.id.button_try_again);
        
        // Initialize shimmer layout
        this.shimmer_layout = view.findViewById(R.id.shimmer_layout);

        // Safely initialize GridLayoutManager
        if (getActivity() != null && getContext() != null) {
            this.gridLayoutManager = new GridLayoutManager(getContext(), 1, RecyclerView.VERTICAL, false);
            this.homeAdapter = new HomeAdapter(dataList, getActivity());
            
            if (recycler_view_home_fragment != null) {
                recycler_view_home_fragment.setHasFixedSize(true);
                recycler_view_home_fragment.setAdapter(homeAdapter);
                recycler_view_home_fragment.setLayoutManager(gridLayoutManager);
            }
        }
        
        Log.d("HomeFragment", "Views initialized successfully");
    }
    
    // Method to update fragment with JSON data
    public void updateWithJsonData(my.cinemax.app.free.entity.JsonApiResponse jsonResponse) {
        if (jsonResponse == null) {
            Log.w("HomeFragment", "Received null JSON response");
            showErrorView();
            return;
        }
        
        // Check if fragment is still alive and attached
        if (getActivity() == null || !isAdded() || getView() == null) {
            Log.w("HomeFragment", "Fragment not attached, skipping data update");
            return;
        }
        
        Log.d("HomeFragment", "Updating fragment with JSON data");
        
        try {
            // Don't show shimmer again if we're updating with cached data
            // Only show loading if we don't have data yet
            if (!isDataLoaded) {
                isLoadingInProgress = true;
                showShimmerLoading();
            }
            
            // Process data on main thread to avoid threading issues
            processJsonData(jsonResponse);
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error updating fragment with JSON data", e);
            showErrorView();
        }
    }
    
    /**
     * Process JSON data safely on main thread
     */
    private void processJsonData(JsonApiResponse jsonResponse) {
        try {
            // Clear existing data
            dataList.clear();
            dataList.add(new Data().setViewType(0));
            
            // Get home data
            JsonApiResponse.HomeData homeData = jsonResponse.getHome();
            if (homeData != null) {
                // Add slides if available
                if (homeData.getSlides() != null && homeData.getSlides().size() > 0) {
                    Data slideData = new Data();
                    slideData.setSlides(homeData.getSlides());
                    dataList.add(slideData);
                }
                
                // Add channels if available
                if (homeData.getChannels() != null && homeData.getChannels().size() > 0) {
                    Data channelData = new Data();
                    channelData.setChannels(homeData.getChannels());
                    dataList.add(channelData);
                }
                
                // Add actors if available
                if (homeData.getActors() != null && homeData.getActors().size() > 0) {
                    Data actorsData = new Data();
                    actorsData.setActors(homeData.getActors());
                    dataList.add(actorsData);
                }
                
                // Add genres if available
                if (homeData.getGenres() != null && homeData.getGenres().size() > 0) {
                    if (my_genre_list != null) {
                        Data genreDataMyList = new Data();
                        genreDataMyList.setGenre(my_genre_list);
                        dataList.add(genreDataMyList);
                    }
                    
                    for (int i = 0; i < homeData.getGenres().size(); i++) {
                        Data genreData = new Data();
                        genreData.setGenre(homeData.getGenres().get(i));
                        dataList.add(genreData);
                        
                        if (native_ads_enabled) {
                            item++;
                            if (item == lines_beetween_ads) {
                                item = 0;
                                if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("FACEBOOK")) {
                                    dataList.add(new Data().setViewType(5));
                                } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("ADMOB")) {
                                    dataList.add(new Data().setViewType(6));
                                } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("BOTH")) {
                                    if (type_ads == 0) {
                                        dataList.add(new Data().setViewType(5));
                                        type_ads = 1;
                                    } else if (type_ads == 1) {
                                        dataList.add(new Data().setViewType(6));
                                        type_ads = 0;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Update UI immediately since we're on main thread
            isDataLoaded = true;
            isLoadingInProgress = false;
            showListView();
            
            // Safely notify adapter
            if (homeAdapter != null) {
                homeAdapter.notifyDataSetChanged();
            }
            
            Log.d("HomeFragment", "Fragment updated with JSON data successfully - Total items: " + dataList.size());
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error processing JSON data", e);
            showErrorView();
        }
    }
    
    /**
     * Check if data is loaded
     */
    public boolean isDataLoaded() {
        return isDataLoaded;
    }
    
    /**
     * Reset loading state (called when refreshing)
     */
    public void resetLoadingState() {
        isDataLoaded = false;
        isLoadingInProgress = false;
        Log.d("HomeFragment", "Loading state reset");
    }

}
