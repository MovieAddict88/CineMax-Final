package my.cinemax.app.free.ui.fragments;


import android.os.Bundle;

import androidx.appcompat.widget.AppCompatSpinner;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import my.cinemax.app.free.Provider.PrefManager;
import my.cinemax.app.free.R;
import my.cinemax.app.free.api.apiClient;
import my.cinemax.app.free.api.apiRest;
import my.cinemax.app.free.entity.Category;
import my.cinemax.app.free.entity.Channel;
import my.cinemax.app.free.entity.Country;
import my.cinemax.app.free.entity.JsonApiResponse;
import my.cinemax.app.free.ui.Adapters.ChannelAdapter;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * A simple {@link Fragment} subclass.
 */
public class TvFragment extends Fragment {


    private View view;
    private RelativeLayout relative_layout_channel_fragement_filtres_button;
    private CardView card_view_channel_fragement_filtres_layout;
    private ImageView image_view_channel_fragement_close_filtres;
    private AppCompatSpinner spinner_fragement_channel_categories_list;
    private AppCompatSpinner spinner_fragement_channel_countries_list;
    private RecyclerView recycler_view_channel_fragment;
    private LinearLayout linear_layout_page_error_channel_fragment;
    private LinearLayout linear_layout_load_channel_fragment;
    private SwipeRefreshLayout swipe_refresh_layout_channel_fragment;
    private RelativeLayout relative_layout_load_more_channel_fragment;
    private ImageView image_view_empty_list;
    private RelativeLayout relative_layout_frament_channel_categories;
    private RelativeLayout relative_layout_frament_channel_countries;

    private GridLayoutManager gridLayoutManager;
    private ChannelAdapter adapter;
    private List<Channel> channelList =  new ArrayList<>();
    private List<Country> countriesList =  new ArrayList<>();
    private List<Category> categoryList =  new ArrayList<>();

    private int pastVisiblesItems, visibleItemCount, totalItemCount;
    private boolean loading = true;

    private Integer page = 0;
    private Integer position = 0;
    private Integer item = 0 ;
    private Button button_try_again;
    private int countrySelected = 0;
    private int categorySelected = 0;

    private boolean firstLoadCountries = true;
    private boolean firstLoadCategories = true;
    private boolean loaded = false;

    private Integer lines_beetween_ads = 2 ;
    private boolean tabletSize;
    private Boolean native_ads_enabled = false ;
    private int type_ads = 0;
    private PrefManager prefManager;


    public TvFragment() {
        // Required empty public constructor
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser){
            if (!loaded) {
                Log.d("TvFragment", "Fragment became visible, loading data");
                loaded=true;
                page = 0;
                loading = true;
                
                // Show loading state immediately
                showLoadingView();
                
                // Load data with caching integration
                getCountiesList();
                getCategoriesList();
                loadChannelsWithCaching();
            } else {
                Log.d("TvFragment", "Fragment visible but data already loaded");
            }
        }
    }
    
    /**
     * Load channels with caching integration
     */
    private void loadChannelsWithCaching() {
        try {
            Log.d("TvFragment", "Loading channels with caching integration");
            
            // Try to get cached data first
            my.cinemax.app.free.Provider.DataRepository dataRepository = 
                my.cinemax.app.free.Provider.DataRepository.getInstance();
            
            if (dataRepository != null) {
                JsonApiResponse cachedResponse = dataRepository.getSimpleCacheManager().getApiResponse();
                if (cachedResponse != null && cachedResponse.getChannels() != null && 
                    !cachedResponse.getChannels().isEmpty() && 
                    dataRepository.getSimpleCacheManager().isCacheValid("api_response")) {
                    
                    Log.d("TvFragment", "Found cached channels, displaying immediately");
                    updateChannelsFromCache(cachedResponse.getChannels());
                    return;
                }
            }
            
            // If no cached data, load from API
            Log.d("TvFragment", "No cached channels found, loading from API");
            loadChannels();
            
        } catch (Exception e) {
            Log.e("TvFragment", "Error in loadChannelsWithCaching", e);
            // Fallback to regular loading
            loadChannels();
        }
    }
    
    /**
     * Update channels from cached data
     */
    private void updateChannelsFromCache(List<my.cinemax.app.free.entity.Channel> cachedChannels) {
        try {
            if (getActivity() == null || !isAdded()) {
                Log.w("TvFragment", "Fragment not attached, skipping cache update");
                return;
            }
            
            channelList.clear();
            channelList.add(new Channel().setTypeView(2)); // Header
            
            // Add cached channels
            for (my.cinemax.app.free.entity.Channel channel : cachedChannels) {
                channelList.add(channel);
                
                // Add ads if enabled
                if (native_ads_enabled) {
                    item++;
                    if (item == lines_beetween_ads) {
                        item = 0;
                        if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("FACEBOOK")) {
                            channelList.add(new Channel().setTypeView(3));
                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("ADMOB")) {
                            channelList.add(new Channel().setTypeView(4));
                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("BOTH")) {
                            if (type_ads == 0) {
                                channelList.add(new Channel().setTypeView(3));
                                type_ads = 1;
                            } else if (type_ads == 1) {
                                channelList.add(new Channel().setTypeView(4));
                                type_ads = 0;
                            }
                        }
                    }
                }
            }
            
            showListView();
            adapter.notifyDataSetChanged();
            
            Log.d("TvFragment", "Updated UI with " + cachedChannels.size() + " cached channels");
            
        } catch (Exception e) {
            Log.e("TvFragment", "Error updating channels from cache", e);
            showErrorView();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_tv, container, false);
        channelList.add(new Channel().setTypeView(2));
        prefManager= new PrefManager(getApplicationContext());

        initView();
        initActon();

        return view;
    }
    private void getCountiesList() {
        // Load countries from GitHub JSON API
        apiClient.getJsonApiData(new retrofit2.Callback<my.cinemax.app.free.entity.JsonApiResponse>() {
            @Override
            public void onResponse(Call<my.cinemax.app.free.entity.JsonApiResponse> call, retrofit2.Response<my.cinemax.app.free.entity.JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    my.cinemax.app.free.entity.JsonApiResponse apiResponse = response.body();
                    
                    if (apiResponse.getCountries() != null && apiResponse.getCountries().size() > 0) {
                        countriesList.clear();
                        // Add "All countries" option with proper ID using setter methods
                        Country allCountries = new Country();
                        allCountries.setId(0);
                        allCountries.setTitle("All countries");
                        countriesList.add(allCountries);
                        
                        for (Country country : apiResponse.getCountries()) {
                            countriesList.add(country);
                        }
                        
                        // Use Country object adapter for proper ID mapping
                        ArrayAdapter<Country> filtresAdapter = new ArrayAdapter<Country>(getActivity(),
                                android.R.layout.simple_spinner_item, countriesList);
                        filtresAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner_fragement_channel_countries_list.setAdapter(filtresAdapter);
                        relative_layout_frament_channel_countries.setVisibility(View.VISIBLE);
                        
                        Log.d("TvFragment", "Loaded " + countriesList.size() + " countries");

                    } else {
                        relative_layout_frament_channel_countries.setVisibility(View.GONE);
                        Log.d("TvFragment", "No countries found in API response");
                    }
                }
            }
            
            @Override
            public void onFailure(Call<my.cinemax.app.free.entity.JsonApiResponse> call, Throwable t) {
                // Hide country filter if loading fails
                relative_layout_frament_channel_countries.setVisibility(View.GONE);
                Log.e("TvFragment", "Failed to load countries: " + t.getMessage());
            }
        });
    }
    private void getCategoriesList() {
        // Load categories from GitHub JSON API
        apiClient.getJsonApiData(new retrofit2.Callback<my.cinemax.app.free.entity.JsonApiResponse>() {
            @Override
            public void onResponse(Call<my.cinemax.app.free.entity.JsonApiResponse> call, retrofit2.Response<my.cinemax.app.free.entity.JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    my.cinemax.app.free.entity.JsonApiResponse apiResponse = response.body();
                    
                    if (apiResponse.getCategories() != null && apiResponse.getCategories().size() > 0) {
                        categoryList.clear();
                        // Add "All categories" option with proper ID using setter methods
                        Category allCategories = new Category();
                        allCategories.setId(0);
                        allCategories.setTitle("All categories");
                        categoryList.add(allCategories);
                        
                        for (Category category : apiResponse.getCategories()) {
                            categoryList.add(category);
                        }
                        
                        // Use Category object adapter for proper ID mapping
                        ArrayAdapter<Category> filtresAdapter = new ArrayAdapter<Category>(getActivity(),
                                android.R.layout.simple_spinner_item, categoryList);
                        filtresAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner_fragement_channel_categories_list.setAdapter(filtresAdapter);
                        relative_layout_frament_channel_categories.setVisibility(View.VISIBLE);
                        
                        Log.d("TvFragment", "Loaded " + categoryList.size() + " categories");

                    } else {
                        relative_layout_frament_channel_categories.setVisibility(View.GONE);
                        Log.d("TvFragment", "No categories found in API response");
                    }
                }
            }
            
            @Override
            public void onFailure(Call<my.cinemax.app.free.entity.JsonApiResponse> call, Throwable t) {
                // Hide category filter if loading fails
                relative_layout_frament_channel_categories.setVisibility(View.GONE);
                Log.e("TvFragment", "Failed to load categories: " + t.getMessage());
            }
        });
    }

    private void initActon() {
        this.relative_layout_channel_fragement_filtres_button.setOnClickListener(v->{
            card_view_channel_fragement_filtres_layout.setVisibility(View.VISIBLE);
            relative_layout_channel_fragement_filtres_button.setVisibility(View.INVISIBLE);
        });
        this.image_view_channel_fragement_close_filtres.setOnClickListener(v->{
            card_view_channel_fragement_filtres_layout.setVisibility(View.INVISIBLE);
            relative_layout_channel_fragement_filtres_button.setVisibility(View.VISIBLE);
        });
        spinner_fragement_channel_countries_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               if (!firstLoadCountries) {
                   if (position == 0) {
                       countrySelected = 0;
                   } else {
                       // Fix: Use position for proper indexing with Country object adapter
                       if (position >= 0 && position < countriesList.size()) {
                           Country selectedCountry = countriesList.get(position);
                           if (selectedCountry != null && selectedCountry.getId() != null) {
                               countrySelected = selectedCountry.getId().intValue();
                           } else {
                               countrySelected = 0;
                           }
                       } else {
                           countrySelected = 0;
                       }
                   }
                   Log.d("TvFragment", "Country selected: " + countrySelected);
                   item = 0;
                   page = 0;
                   loading = true;
                   channelList.clear();
                   channelList.add(new Channel().setTypeView(2));
                   adapter.notifyDataSetChanged();
                   loadChannels();
               } else {
                   firstLoadCountries = false;
               }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner_fragement_channel_categories_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!firstLoadCategories) {
                    if (position == 0) {
                        categorySelected = 0;
                    } else {
                        // Fix: Use position for proper indexing with Category object adapter
                        if (position >= 0 && position < categoryList.size()) {
                            Category selectedCategory = categoryList.get(position);
                            if (selectedCategory != null && selectedCategory.getId() != null) {
                                categorySelected = selectedCategory.getId().intValue();
                            } else {
                                categorySelected = 0;
                            }
                        } else {
                            categorySelected = 0;
                        }
                    }
                    Log.d("TvFragment", "Category selected: " + categorySelected);
                    item = 0;
                    page = 0;
                    loading = true;
                    channelList.clear();
                    channelList.add(new Channel().setTypeView(2));
                    adapter.notifyDataSetChanged();
                    loadChannels();
                } else {
                    firstLoadCategories = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        swipe_refresh_layout_channel_fragment.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                item = 0;
                page = 0;
                loading = true;
                channelList.clear();
                channelList.add(new Channel().setTypeView(2));
                adapter.notifyDataSetChanged();
                loadChannels();
            }
        });
        button_try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                item = 0;
                page = 0;
                loading = true;
                channelList.clear();
                channelList.add(new Channel().setTypeView(2));
                adapter.notifyDataSetChanged();
                loadChannels();
            }
        });
        recycler_view_channel_fragment.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                if(dy > 0) //check for scroll down
                {

                    visibleItemCount    = gridLayoutManager.getChildCount();
                    totalItemCount      = gridLayoutManager.getItemCount();
                    pastVisiblesItems   = gridLayoutManager.findFirstVisibleItemPosition();

                    if (loading)
                    {
                        if ( (visibleItemCount + pastVisiblesItems) >= totalItemCount)
                        {
                            // Don't load more since we're loading all channels at once
                            loading = false;
                        }
                    }
                }else{

                }
            }
        });
    }
    public boolean checkSUBSCRIBED(){
        PrefManager prefManager= new PrefManager(getApplicationContext());
        if (!prefManager.getString("SUBSCRIBED").equals("TRUE") && !prefManager.getString("NEW_SUBSCRIBE_ENABLED").equals("TRUE")) {
            return false;
        }
        return true;
    }
    private void initView() {
        boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
        if (!prefManager.getString("ADMIN_NATIVE_TYPE").equals("FALSE")){
            native_ads_enabled=true;
            if (tabletSize) {
                lines_beetween_ads=8*Integer.parseInt(prefManager.getString("ADMIN_NATIVE_LINES"));
            }else{
                lines_beetween_ads=4*Integer.parseInt(prefManager.getString("ADMIN_NATIVE_LINES"));
            }
        }
        if (checkSUBSCRIBED()) {
            native_ads_enabled=false;
        }
        // prod

        this.button_try_again = (Button) view.findViewById(R.id.button_try_again);
        this.image_view_empty_list = (ImageView) view.findViewById(R.id.image_view_empty_list);
        this.relative_layout_load_more_channel_fragment = (RelativeLayout) view.findViewById(R.id.relative_layout_load_more_channel_fragment);
        this.swipe_refresh_layout_channel_fragment = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_channel_fragment);
        this.linear_layout_load_channel_fragment = (LinearLayout) view.findViewById(R.id.linear_layout_load_channel_fragment);
        this.linear_layout_page_error_channel_fragment = (LinearLayout) view.findViewById(R.id.linear_layout_page_error_channel_fragment);
        this.recycler_view_channel_fragment = (RecyclerView) view.findViewById(R.id.recycler_view_channel_fragment);
        this.relative_layout_channel_fragement_filtres_button = (RelativeLayout) view.findViewById(R.id.relative_layout_channel_fragement_filtres_button);
        this.card_view_channel_fragement_filtres_layout = (CardView) view.findViewById(R.id.card_view_channel_fragement_filtres_layout);
        this.image_view_channel_fragement_close_filtres = (ImageView) view.findViewById(R.id.image_view_channel_fragement_close_filtres);
        this.spinner_fragement_channel_categories_list = (AppCompatSpinner) view.findViewById(R.id.spinner_fragement_channel_categories_list);
        this.spinner_fragement_channel_countries_list = (AppCompatSpinner) view.findViewById(R.id.spinner_fragement_channel_countries_list);
        this.relative_layout_frament_channel_countries = (RelativeLayout) view.findViewById(R.id.relative_layout_frament_channel_countries);
        this.relative_layout_frament_channel_categories = (RelativeLayout) view.findViewById(R.id.relative_layout_frament_channel_categories);

        this.gridLayoutManager=  new GridLayoutManager(getActivity().getApplicationContext(),2,RecyclerView.VERTICAL,false);

        adapter = new ChannelAdapter(channelList,getActivity());
        if (native_ads_enabled){
            Log.v("MYADS","ENABLED");
            if (tabletSize) {
                this.gridLayoutManager=  new GridLayoutManager(getActivity().getApplicationContext(),4,RecyclerView.VERTICAL,false);
                Log.v("MYADS","tabletSize");
                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return ((position ) % (lines_beetween_ads + 1 ) == 0 || position == 0) ? 4 : 1;
                    }
                });
            } else {
                this.gridLayoutManager=  new GridLayoutManager(getActivity().getApplicationContext(),2,RecyclerView.VERTICAL,false);
                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return ((position ) % (lines_beetween_ads + 1 ) == 0 || position == 0) ? 2 : 1;
                    }
                });
            }
        }else {
            if (tabletSize) {
                this.gridLayoutManager=  new GridLayoutManager(getActivity().getApplicationContext(),4,RecyclerView.VERTICAL,false);
                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return ( position == 0) ? 4 : 1;
                    }
                });
            } else {
                this.gridLayoutManager=  new GridLayoutManager(getActivity().getApplicationContext(),2,RecyclerView.VERTICAL,false);
                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return ( position == 0) ? 2 : 1;
                    }
                });
            }
        }

        recycler_view_channel_fragment.setHasFixedSize(true);
        recycler_view_channel_fragment.setAdapter(adapter);
        recycler_view_channel_fragment.setLayoutManager(gridLayoutManager);
        // test


    }
    private void loadChannels() {
        // Load channels from GitHub JSON API with filtering
        if (page == 0) {
            linear_layout_load_channel_fragment.setVisibility(View.VISIBLE);
        } else {
            relative_layout_load_more_channel_fragment.setVisibility(View.VISIBLE);
        }
        swipe_refresh_layout_channel_fragment.setRefreshing(false);
        
        apiClient.getJsonApiData(new retrofit2.Callback<my.cinemax.app.free.entity.JsonApiResponse>() {
            @Override
            public void onResponse(Call<my.cinemax.app.free.entity.JsonApiResponse> call, retrofit2.Response<my.cinemax.app.free.entity.JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    my.cinemax.app.free.entity.JsonApiResponse apiResponse = response.body();
                    
                    if (apiResponse.getChannels() != null && apiResponse.getChannels().size() > 0) {
                        List<Channel> filteredChannels = new ArrayList<>();
                        
                        for (Channel channel : apiResponse.getChannels()) {
                            // Apply category filtering
                            boolean matchesCategory = false;
                            if (categorySelected == 0) {
                                // Show all categories
                                matchesCategory = true;
                            } else if (channel.getCategories() != null && !channel.getCategories().isEmpty()) {
                                // Check if channel has the selected category
                                for (Category category : channel.getCategories()) {
                                    if (category.getId() != null && category.getId().intValue() == categorySelected) {
                                        matchesCategory = true;
                                        break;
                                    }
                                }
                            }
                            
                            // Apply country filtering
                            boolean matchesCountry = false;
                            if (countrySelected == 0) {
                                // Show all countries
                                matchesCountry = true;
                            } else if (channel.getCountries() != null && !channel.getCountries().isEmpty()) {
                                // Check if channel has the selected country
                                for (Country country : channel.getCountries()) {
                                    if (country.getId() != null && country.getId().intValue() == countrySelected) {
                                        matchesCountry = true;
                                        break;
                                    }
                                }
                            } else {
                                // Fallback: use sublabel for country matching if countries list is empty
                                String sublabel = channel.getSublabel();
                                if (sublabel != null && !sublabel.isEmpty()) {
                                    String sublabelLower = sublabel.toLowerCase();
                                    // Enhanced country matching based on sublabel
                                    if (countrySelected == 1 && (sublabelLower.contains("usa") || sublabelLower.contains("united states"))) {
                                        matchesCountry = true;
                                    } else if (countrySelected == 2 && (sublabelLower.contains("uk") || sublabelLower.contains("united kingdom"))) {
                                        matchesCountry = true;
                                    } else if (countrySelected == 3 && sublabelLower.contains("france")) {
                                        matchesCountry = true;
                                    } else if (countrySelected == 4 && sublabelLower.contains("germany")) {
                                        matchesCountry = true;
                                    } else if (countrySelected == 5 && (sublabelLower.contains("ph") || sublabelLower.contains("philippines"))) {
                                        // Handle Philippines channels
                                        matchesCountry = true;
                                    }
                                }
                            }
                            
                            if (matchesCategory && matchesCountry) {
                                filteredChannels.add(channel);
                            }
                        }
                        
                        Log.d("TvFragment", "Total channels found: " + filteredChannels.size() + 
                              " (Category: " + categorySelected + ", Country: " + countrySelected + ")");
                        
                        if (!filteredChannels.isEmpty()) {
                            // Only add channels if this is the first page or if we're loading more
                            if (page == 0) {
                                // Clear the list for first page
                                channelList.clear();
                                channelList.add(new Channel().setTypeView(2));
                            }
                            
                            for (Channel channel : filteredChannels) {
                                channelList.add(channel);
                                
                                if (native_ads_enabled) {
                                    item++;
                                    if (item == lines_beetween_ads) {
                                        item = 0;
                                        if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("FACEBOOK")) {
                                            channelList.add(new Channel().setTypeView(3));
                                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("ADMOB")) {
                                            channelList.add(new Channel().setTypeView(4));
                                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("BOTH")) {
                                            if (type_ads == 0) {
                                                channelList.add(new Channel().setTypeView(3));
                                                type_ads = 1;
                                            } else if (type_ads == 1) {
                                                channelList.add(new Channel().setTypeView(4));
                                                type_ads = 0;
                                            }
                                        }
                                    }
                                }
                            }
                            linear_layout_page_error_channel_fragment.setVisibility(View.GONE);
                            recycler_view_channel_fragment.setVisibility(View.VISIBLE);
                            image_view_empty_list.setVisibility(View.GONE);
                        } else {
                            if (page == 0) {
                                linear_layout_page_error_channel_fragment.setVisibility(View.GONE);
                                recycler_view_channel_fragment.setVisibility(View.GONE);
                                image_view_empty_list.setVisibility(View.VISIBLE);
                            }
                        }
                        
                        adapter.notifyDataSetChanged();
                        page++;
                        loading = false; // Set to false to prevent infinite loading
                    } else {
                        if (page == 0) {
                            linear_layout_page_error_channel_fragment.setVisibility(View.GONE);
                            recycler_view_channel_fragment.setVisibility(View.GONE);
                            image_view_empty_list.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    linear_layout_page_error_channel_fragment.setVisibility(View.VISIBLE);
                    recycler_view_channel_fragment.setVisibility(View.GONE);
                    image_view_empty_list.setVisibility(View.GONE);
                }
                relative_layout_load_more_channel_fragment.setVisibility(View.GONE);
                swipe_refresh_layout_channel_fragment.setRefreshing(false);
                linear_layout_load_channel_fragment.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<my.cinemax.app.free.entity.JsonApiResponse> call, Throwable t) {
                linear_layout_page_error_channel_fragment.setVisibility(View.VISIBLE);
                recycler_view_channel_fragment.setVisibility(View.GONE);
                image_view_empty_list.setVisibility(View.GONE);
                relative_layout_load_more_channel_fragment.setVisibility(View.GONE);
                swipe_refresh_layout_channel_fragment.setRefreshing(false);
                linear_layout_load_channel_fragment.setVisibility(View.GONE);
            }
        });
    }
    
    // Method to update fragment with JSON data
    public void updateWithJsonData(List<Channel> channels) {
        if (channels != null && channels.size() > 0) {
            // Clear existing data
            channelList.clear();
            page = 0;
            item = 0;
            
            // Add channels from JSON
            for (int i = 0; i < channels.size(); i++) {
                channelList.add(channels.get(i));
                
                if (native_ads_enabled) {
                    item++;
                    if (item == lines_beetween_ads) {
                        item = 0;
                        if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("FACEBOOK")) {
                            channelList.add(new Channel().setTypeView(3));
                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("ADMOB")) {
                            channelList.add(new Channel().setTypeView(4));
                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("BOTH")) {
                            if (type_ads == 0) {
                                channelList.add(new Channel().setTypeView(3));
                                type_ads = 1;
                            } else if (type_ads == 1) {
                                channelList.add(new Channel().setTypeView(4));
                                type_ads = 0;
                            }
                        }
                    }
                }
            }
            
            // Show the data
            linear_layout_page_error_channel_fragment.setVisibility(View.GONE);
            recycler_view_channel_fragment.setVisibility(View.VISIBLE);
            image_view_empty_list.setVisibility(View.GONE);
            linear_layout_load_channel_fragment.setVisibility(View.GONE);
            
            adapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Show loading view
     */
    private void showLoadingView() {
        if (getActivity() == null || !isAdded()) return;
        
        try {
            linear_layout_load_channel_fragment.setVisibility(View.VISIBLE);
            linear_layout_page_error_channel_fragment.setVisibility(View.GONE);
            recycler_view_channel_fragment.setVisibility(View.GONE);
            image_view_empty_list.setVisibility(View.GONE);
            
            Log.d("TvFragment", "Loading view displayed");
        } catch (Exception e) {
            Log.e("TvFragment", "Error showing loading view", e);
        }
    }
    
    /**
     * Show list view
     */
    private void showListView() {
        if (getActivity() == null || !isAdded()) return;
        
        try {
            linear_layout_load_channel_fragment.setVisibility(View.GONE);
            linear_layout_page_error_channel_fragment.setVisibility(View.GONE);
            recycler_view_channel_fragment.setVisibility(View.VISIBLE);
            image_view_empty_list.setVisibility(View.GONE);
            
            Log.d("TvFragment", "List view displayed");
        } catch (Exception e) {
            Log.e("TvFragment", "Error showing list view", e);
        }
    }
    
    /**
     * Show error view
     */
    private void showErrorView() {
        if (getActivity() == null || !isAdded()) return;
        
        try {
            linear_layout_load_channel_fragment.setVisibility(View.GONE);
            linear_layout_page_error_channel_fragment.setVisibility(View.VISIBLE);
            recycler_view_channel_fragment.setVisibility(View.GONE);
            image_view_empty_list.setVisibility(View.GONE);
            
            Log.d("TvFragment", "Error view displayed");
        } catch (Exception e) {
            Log.e("TvFragment", "Error showing error view", e);
        }
    }
}
