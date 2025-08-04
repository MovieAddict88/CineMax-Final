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
import my.cinemax.app.free.entity.Genre;
import my.cinemax.app.free.entity.Poster;
import my.cinemax.app.free.ui.Adapters.PosterAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * A simple {@link Fragment} subclass.
 */
public class SeriesFragment extends Fragment {



    private View view;
    private RelativeLayout relative_layout_series_fragement_filtres_button;
    private CardView card_view_series_fragement_filtres_layout;
    private ImageView image_view_series_fragement_close_filtres;
    private AppCompatSpinner spinner_fragement_series_orders_list;
    private List<Genre> genreList =  new ArrayList<>();
    private AppCompatSpinner spinner_fragement_series_genre_list;
    private RelativeLayout relative_layout_frament_series_genres;
    private RecyclerView recycler_view_series_fragment;
    private LinearLayout linear_layout_page_error_series_fragment;
    private LinearLayout linear_layout_load_series_fragment;
    private SwipeRefreshLayout swipe_refresh_layout_series_fragment;
    private RelativeLayout relative_layout_load_more_series_fragment;
    private ImageView image_view_empty_list;


    private GridLayoutManager gridLayoutManager;
    private PosterAdapter adapter;
    private List<Poster> movieList =  new ArrayList<>();

    private int pastVisiblesItems, visibleItemCount, totalItemCount;
    private boolean loading = true;

    private Integer page = 0;
    private Integer position = 0;
    private Integer item = 0 ;
    private Button button_try_again;
    private int genreSelected = 0;
    private String orderSelected = "created";

    private boolean firstLoadGenre = true;
    private boolean firstLoadOrder = true;
    private boolean loaded = false;


    private Integer lines_beetween_ads = 2 ;
    private boolean tabletSize;
    private Boolean native_ads_enabled = false ;
    private int type_ads = 0;
    private PrefManager prefManager;


    public SeriesFragment() {
        // Required empty public constructor
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser){
            if (!loaded) {
                loaded=true;
                page = 0;
                loading = true;
                // Load genres and series from GitHub JSON
                getGenreList();
                loadSeries();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_series, container, false);
        movieList.add(new Poster().setTypeView(2));
        prefManager= new PrefManager(getApplicationContext());

        initView();
        initActon();

        return view;
    }

    private void getGenreList() {
        // Load genres from GitHub JSON API
        apiClient.getJsonApiData(new retrofit2.Callback<my.cinemax.app.free.entity.JsonApiResponse>() {
            @Override
            public void onResponse(Call<my.cinemax.app.free.entity.JsonApiResponse> call, retrofit2.Response<my.cinemax.app.free.entity.JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    my.cinemax.app.free.entity.JsonApiResponse apiResponse = response.body();
                    
                    if (apiResponse.getGenres() != null && apiResponse.getGenres().size() > 0) {
                        genreList.clear();
                        // Add "All genres" option with proper ID using setter methods
                        Genre allGenres = new Genre();
                        allGenres.setId(0);
                        allGenres.setTitle("All genres");
                        genreList.add(allGenres);
                        
                        for (Genre genre : apiResponse.getGenres()) {
                            genreList.add(genre);
                        }
                        
                        // Use Genre object adapter for proper ID mapping
                        ArrayAdapter<Genre> filtresAdapter = new ArrayAdapter<Genre>(getActivity(),
                                android.R.layout.simple_spinner_item, genreList);
                        filtresAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner_fragement_series_genre_list.setAdapter(filtresAdapter);
                        relative_layout_frament_series_genres.setVisibility(View.VISIBLE);
                        
                        Log.d("SeriesFragment", "Loaded " + genreList.size() + " genres");

                    } else {
                        relative_layout_frament_series_genres.setVisibility(View.GONE);
                        Log.w("SeriesFragment", "No genres found in API response");
                    }
                } else {
                    relative_layout_frament_series_genres.setVisibility(View.GONE);
                    Log.e("SeriesFragment", "Failed to load genres: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<my.cinemax.app.free.entity.JsonApiResponse> call, Throwable t) {
                // Hide genre filter if loading fails
                relative_layout_frament_series_genres.setVisibility(View.GONE);
                Log.e("SeriesFragment", "Failed to load genres", t);
            }
        });
    }

    private void initActon() {
        this.relative_layout_series_fragement_filtres_button.setOnClickListener(v->{
            card_view_series_fragement_filtres_layout.setVisibility(View.VISIBLE);
            relative_layout_series_fragement_filtres_button.setVisibility(View.INVISIBLE);
        });
        this.image_view_series_fragement_close_filtres.setOnClickListener(v->{
            card_view_series_fragement_filtres_layout.setVisibility(View.INVISIBLE);
            relative_layout_series_fragement_filtres_button.setVisibility(View.VISIBLE);
        });
        spinner_fragement_series_genre_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!firstLoadGenre) {
                    if (position == 0) {
                        genreSelected = 0;
                    } else {
                        // Fix: Use position for proper indexing with Genre object adapter
                        if (position >= 0 && position < genreList.size()) {
                            Genre selectedGenre = genreList.get(position);
                            if (selectedGenre != null && selectedGenre.getId() != null) {
                                genreSelected = selectedGenre.getId().intValue();
                            } else {
                                genreSelected = 0;
                            }
                        } else {
                            genreSelected = 0;
                        }
                    }
                    Log.d("SeriesFragment", "Genre selected: " + genreSelected);
                    item = 0;
                    page = 0;
                    loading = true;
                    movieList.clear();
                    movieList.add(new Poster().setTypeView(2));
                    adapter.notifyDataSetChanged();
                    loadSeries();
                } else {
                    firstLoadGenre = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner_fragement_series_orders_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!firstLoadOrder) {

                    switch ((int) id) {
                        case 0:
                            orderSelected = "created";
                            break;
                        case 1:
                            orderSelected = "rating";
                            break;
                        case 2:
                            orderSelected = "imdb";
                            break;
                        case 3:
                            orderSelected = "title";
                            break;
                        case 4:
                            orderSelected = "year";
                            break;
                        case 5:
                            orderSelected = "views";
                            break;
                    }
                    Log.d("SeriesFragment", "Order selected: " + orderSelected);
                    item = 0;
                    page = 0;
                    loading = true;
                    movieList.clear();
                    movieList.add(new Poster().setTypeView(2));
                    adapter.notifyDataSetChanged();
                    loadSeries();
                }else{
                    firstLoadOrder = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        swipe_refresh_layout_series_fragment.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                item = 0;
                page = 0;
                loading = true;
                movieList.clear();
                movieList.add(new Poster().setTypeView(2));
                adapter.notifyDataSetChanged();
                loadSeries();
            }
        });
        button_try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                item = 0;
                page = 0;
                loading = true;
                movieList.clear();
                movieList.add(new Poster().setTypeView(2));
                adapter.notifyDataSetChanged();
                loadSeries();
            }
        });
        recycler_view_series_fragment.addOnScrollListener(new RecyclerView.OnScrollListener()
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
                            // Don't load more since we're loading all series at once
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
        this.relative_layout_load_more_series_fragment = (RelativeLayout) view.findViewById(R.id.relative_layout_load_more_series_fragment);
        this.swipe_refresh_layout_series_fragment = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_series_fragment);
        this.linear_layout_load_series_fragment = (LinearLayout) view.findViewById(R.id.linear_layout_load_series_fragment);
        this.linear_layout_page_error_series_fragment = (LinearLayout) view.findViewById(R.id.linear_layout_page_error_series_fragment);
        this.recycler_view_series_fragment = (RecyclerView) view.findViewById(R.id.recycler_view_series_fragment);
        this.relative_layout_series_fragement_filtres_button = (RelativeLayout) view.findViewById(R.id.relative_layout_series_fragement_filtres_button);
        this.card_view_series_fragement_filtres_layout = (CardView) view.findViewById(R.id.card_view_series_fragement_filtres_layout);
        this.image_view_series_fragement_close_filtres = (ImageView) view.findViewById(R.id.image_view_series_fragement_close_filtres);
        this.spinner_fragement_series_genre_list = (AppCompatSpinner) view.findViewById(R.id.spinner_fragement_series_genre_list);
        this.spinner_fragement_series_orders_list = (AppCompatSpinner) view.findViewById(R.id.spinner_fragement_series_orders_list);
        this.relative_layout_frament_series_genres = (RelativeLayout) view.findViewById(R.id.relative_layout_frament_series_genres);

        this.gridLayoutManager=  new GridLayoutManager(getActivity().getApplicationContext(),2,RecyclerView.VERTICAL,false);

        adapter = new PosterAdapter(movieList,getActivity());
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

        recycler_view_series_fragment.setHasFixedSize(true);
        recycler_view_series_fragment.setAdapter(adapter);
        recycler_view_series_fragment.setLayoutManager(gridLayoutManager);

        // Initialize order spinner
        String[] orders = {"Last Added", "Rating", "IMDb", "Title", "Year", "Views"};
        ArrayAdapter<String> ordersAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, orders);
        ordersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_fragement_series_orders_list.setAdapter(ordersAdapter);
    }
    private void loadSeries() {
        // Load series from GitHub JSON API with filtering
        if (page == 0) {
            linear_layout_load_series_fragment.setVisibility(View.VISIBLE);
        } else {
            relative_layout_load_more_series_fragment.setVisibility(View.VISIBLE);
        }
        swipe_refresh_layout_series_fragment.setRefreshing(false);
        
        apiClient.getJsonApiData(new retrofit2.Callback<my.cinemax.app.free.entity.JsonApiResponse>() {
            @Override
            public void onResponse(Call<my.cinemax.app.free.entity.JsonApiResponse> call, retrofit2.Response<my.cinemax.app.free.entity.JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    my.cinemax.app.free.entity.JsonApiResponse apiResponse = response.body();
                    
                    if (apiResponse.getMovies() != null && apiResponse.getMovies().size() > 0) {
                        List<Poster> filteredSeries = new ArrayList<>();
                        
                        Log.d("SeriesFragment", "Total movies in API: " + apiResponse.getMovies().size());
                        int seriesCount = 0;
                        for (Poster poster : apiResponse.getMovies()) {
                            // Filter by type (series/serie)
                            String type = poster.getType();
                            Log.d("SeriesFragment", "Checking poster: " + poster.getTitle() + " with type: " + type);
                            if ("series".equals(type) || "serie".equals(type)) {
                                seriesCount++;
                                Log.d("SeriesFragment", "Found series: " + poster.getTitle());
                                // Apply genre filtering
                                boolean matchesGenre = false;
                                if (genreSelected == 0) {
                                    // Show all genres
                                    matchesGenre = true;
                                    Log.d("SeriesFragment", "All genres selected, including: " + poster.getTitle());
                                } else if (poster.getGenres() != null && !poster.getGenres().isEmpty()) {
                                    // Check if poster has the selected genre
                                    for (Genre genre : poster.getGenres()) {
                                        Log.d("SeriesFragment", "Checking genre: " + genre.getTitle() + " (ID: " + genre.getId() + ") against selected: " + genreSelected);
                                        if (genre.getId() != null && genre.getId().intValue() == genreSelected) {
                                            matchesGenre = true;
                                            Log.d("SeriesFragment", "Genre match found for: " + poster.getTitle());
                                            break;
                                        }
                                    }
                                } else {
                                    Log.d("SeriesFragment", "No genres found for: " + poster.getTitle());
                                }
                                
                                if (matchesGenre) {
                                    filteredSeries.add(poster);
                                    Log.d("SeriesFragment", "Added to filtered list: " + poster.getTitle());
                                } else {
                                    Log.d("SeriesFragment", "Excluded from filtered list: " + poster.getTitle() + " (genre mismatch)");
                                }
                            }
                        }
                        Log.d("SeriesFragment", "Total series found: " + seriesCount);
                        
                        Log.d("SeriesFragment", "Total series found: " + filteredSeries.size() + 
                              " (Genre: " + genreSelected + ", Order: " + orderSelected + ")");
                        
                        // Apply ordering
                        if (orderSelected != null) {
                            switch (orderSelected) {
                                case "created":
                                    // Keep original order (newest first)
                                    break;
                                case "rating":
                                    Collections.sort(filteredSeries, new Comparator<Poster>() {
                                        @Override
                                        public int compare(Poster p1, Poster p2) {
                                            Float rating1 = p1.getRating();
                                            Float rating2 = p2.getRating();
                                            if (rating1 == null) rating1 = 0f;
                                            if (rating2 == null) rating2 = 0f;
                                            return rating2.compareTo(rating1); // Descending
                                        }
                                    });
                                    break;
                                case "imdb":
                                    Collections.sort(filteredSeries, new Comparator<Poster>() {
                                        @Override
                                        public int compare(Poster p1, Poster p2) {
                                            String imdb1 = p1.getImdb();
                                            String imdb2 = p2.getImdb();
                                            if (imdb1 == null) imdb1 = "0";
                                            if (imdb2 == null) imdb2 = "0";
                                            try {
                                                float rating1 = Float.parseFloat(imdb1);
                                                float rating2 = Float.parseFloat(imdb2);
                                                return Float.compare(rating2, rating1); // Descending
                                            } catch (NumberFormatException e) {
                                                return 0;
                                            }
                                        }
                                    });
                                    break;
                                case "title":
                                    Collections.sort(filteredSeries, new Comparator<Poster>() {
                                        @Override
                                        public int compare(Poster p1, Poster p2) {
                                            String title1 = p1.getTitle();
                                            String title2 = p2.getTitle();
                                            if (title1 == null) title1 = "";
                                            if (title2 == null) title2 = "";
                                            return title1.compareToIgnoreCase(title2); // Ascending
                                        }
                                    });
                                    break;
                                case "year":
                                    Collections.sort(filteredSeries, new Comparator<Poster>() {
                                        @Override
                                        public int compare(Poster p1, Poster p2) {
                                            String year1 = p1.getYear();
                                            String year2 = p2.getYear();
                                            if (year1 == null) year1 = "0";
                                            if (year2 == null) year2 = "0";
                                            try {
                                                int year1Int = Integer.parseInt(year1);
                                                int year2Int = Integer.parseInt(year2);
                                                return Integer.compare(year2Int, year1Int); // Descending
                                            } catch (NumberFormatException e) {
                                                return 0;
                                            }
                                        }
                                    });
                                    break;
                                case "views":
                                    Collections.sort(filteredSeries, new Comparator<Poster>() {
                                        @Override
                                        public int compare(Poster p1, Poster p2) {
                                            Integer views1 = p1.getViews();
                                            Integer views2 = p2.getViews();
                                            if (views1 == null) views1 = 0;
                                            if (views2 == null) views2 = 0;
                                            return views2.compareTo(views1); // Descending
                                        }
                                    });
                                    break;
                            }
                        }
                        
                        if (!filteredSeries.isEmpty()) {
                            // Only add series if this is the first page or if we're loading more
                            if (page == 0) {
                                // Clear the list for first page
                                movieList.clear();
                                movieList.add(new Poster().setTypeView(2));
                            }
                            
                            for (Poster poster : filteredSeries) {
                                movieList.add(poster);
                                
                                if (native_ads_enabled) {
                                    item++;
                                    if (item == lines_beetween_ads) {
                                        item = 0;
                                        if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("FACEBOOK")) {
                                            movieList.add(new Poster().setTypeView(3));
                                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("ADMOB")) {
                                            movieList.add(new Poster().setTypeView(4));
                                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("BOTH")) {
                                            if (type_ads == 0) {
                                                movieList.add(new Poster().setTypeView(3));
                                                type_ads = 1;
                                            } else if (type_ads == 1) {
                                                movieList.add(new Poster().setTypeView(4));
                                                type_ads = 0;
                                            }
                                        }
                                    }
                                }
                            }
                            linear_layout_page_error_series_fragment.setVisibility(View.GONE);
                            recycler_view_series_fragment.setVisibility(View.VISIBLE);
                            image_view_empty_list.setVisibility(View.GONE);
                        } else {
                            if (page == 0) {
                                linear_layout_page_error_series_fragment.setVisibility(View.GONE);
                                recycler_view_series_fragment.setVisibility(View.GONE);
                                image_view_empty_list.setVisibility(View.VISIBLE);
                            }
                        }
                        
                        adapter.notifyDataSetChanged();
                        page++;
                        loading = false; // Set to false to prevent infinite loading
                    } else {
                        if (page == 0) {
                            linear_layout_page_error_series_fragment.setVisibility(View.GONE);
                            recycler_view_series_fragment.setVisibility(View.GONE);
                            image_view_empty_list.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    linear_layout_page_error_series_fragment.setVisibility(View.VISIBLE);
                    recycler_view_series_fragment.setVisibility(View.GONE);
                    image_view_empty_list.setVisibility(View.GONE);
                }
                relative_layout_load_more_series_fragment.setVisibility(View.GONE);
                swipe_refresh_layout_series_fragment.setRefreshing(false);
                linear_layout_load_series_fragment.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<my.cinemax.app.free.entity.JsonApiResponse> call, Throwable t) {
                linear_layout_page_error_series_fragment.setVisibility(View.VISIBLE);
                recycler_view_series_fragment.setVisibility(View.GONE);
                image_view_empty_list.setVisibility(View.GONE);
                relative_layout_load_more_series_fragment.setVisibility(View.GONE);
                swipe_refresh_layout_series_fragment.setRefreshing(false);
                linear_layout_load_series_fragment.setVisibility(View.GONE);
            }
        });
    }
    
    // Method to update fragment with JSON data
    public void updateWithJsonData(List<Poster> series) {
        if (series != null && series.size() > 0) {
            // Clear existing data
            movieList.clear();
            page = 0;
            item = 0;
            
            // Add series from JSON
            for (int i = 0; i < series.size(); i++) {
                movieList.add(series.get(i));
                
                if (native_ads_enabled) {
                    item++;
                    if (item == lines_beetween_ads) {
                        item = 0;
                        if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("FACEBOOK")) {
                            movieList.add(new Poster().setTypeView(3));
                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("ADMOB")) {
                            movieList.add(new Poster().setTypeView(4));
                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("BOTH")) {
                            if (type_ads == 0) {
                                movieList.add(new Poster().setTypeView(3));
                                type_ads = 1;
                            } else if (type_ads == 1) {
                                movieList.add(new Poster().setTypeView(4));
                                type_ads = 0;
                            }
                        }
                    }
                }
            }
            
            // Show the data
            linear_layout_page_error_series_fragment.setVisibility(View.GONE);
            recycler_view_series_fragment.setVisibility(View.VISIBLE);
            image_view_empty_list.setVisibility(View.GONE);
            linear_layout_load_series_fragment.setVisibility(View.GONE);
            
            adapter.notifyDataSetChanged();
        }
    }
}
