package my.cinemax.app.free.ui.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import my.cinemax.app.free.Provider.PrefManager;
import my.cinemax.app.free.R;
import my.cinemax.app.free.api.apiClient;
import my.cinemax.app.free.api.apiRest;
import my.cinemax.app.free.entity.Genre;
import my.cinemax.app.free.entity.Poster;
import my.cinemax.app.free.entity.JsonApiResponse;
import my.cinemax.app.free.ui.Adapters.PosterAdapter;

import java.util.ArrayList;
import java.util.List;

public class GenreActivity extends AppCompatActivity {
    private SwipeRefreshLayout swipe_refresh_layout_list_genre_search;
    private Button button_try_again;
    private LinearLayout linear_layout_layout_error;
    private RecyclerView recycler_view_activity_genre;
    private ImageView image_view_empty_list;
    private GridLayoutManager gridLayoutManager;
    private PosterAdapter adapter;

    private int pastVisiblesItems, visibleItemCount, totalItemCount;
    private boolean loading = true;

    private Integer page = 0;
    private Integer position = 0;
    private Integer item = 0 ;
    ArrayList<Poster> posterArrayList = new ArrayList<>();
    private RelativeLayout relative_layout_load_more;
    private LinearLayout linear_layout_load_genre_activity;


    private String SelectedOrder = "created";
    private Genre genre;
    private String from;

    private Integer lines_beetween_ads = 2 ;
    private boolean tabletSize;
    private Boolean native_ads_enabled = false ;
    private int type_ads = 0;
    private PrefManager prefManager;

    // JSON API data cache
    private JsonApiResponse cachedJsonResponse = null;
    private List<Poster> allMovies = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre);
        prefManager= new PrefManager(getApplicationContext());

        try {
            getGenre();
            if (genre == null) {
                Log.e("GenreActivity", "Genre is null after getGenre(), finishing activity");
                finish();
                return;
            }
            initView();
            initAction();
            loadPostersFromJson();
            showAdsBanner();
        } catch (Exception e) {
            Log.e("GenreActivity", "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_order, menu);
        return true;
    }
    @Override
    public void onBackPressed(){
        if (from!=null){
            Intent intent =  new Intent(getApplicationContext(),HomeActivity.class);
            startActivity(intent);
        }else{
            super.onBackPressed();
        }
        return;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem itemMenu) {
        switch (itemMenu.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (from!=null){
                    Intent intent =  new Intent(getApplicationContext(),HomeActivity.class);
                    startActivity(intent);
                }else{
                    super.onBackPressed();
                }
                return true;
            case R.id.nav_created:
                SelectedOrder = "created";
                item = 0;
                page = 0;
                loading = true;
                posterArrayList.clear();
                adapter.notifyDataSetChanged();
                loadPostersFromJson();
                return true;
            case R.id.nav_rating:
                SelectedOrder = "rating";
                item = 0;
                page = 0;
                loading = true;
                posterArrayList.clear();
                adapter.notifyDataSetChanged();
                loadPostersFromJson();
                return true;
            case R.id.nav_views:
                SelectedOrder = "views";
                item = 0;
                page = 0;
                loading = true;
                posterArrayList.clear();
                adapter.notifyDataSetChanged();
                loadPostersFromJson();
                return true;
            case R.id.nav_year:
                SelectedOrder = "year";
                item = 0;
                page = 0;
                loading = true;
                posterArrayList.clear();
                adapter.notifyDataSetChanged();
                loadPostersFromJson();
                return true;
            case R.id.nav_title:
                SelectedOrder = "title";
                item = 0;
                page = 0;
                loading = true;
                posterArrayList.clear();
                adapter.notifyDataSetChanged();
                loadPostersFromJson();
                return true;
            case R.id.nav_imdb:
                SelectedOrder = "imdb";
                item = 0;
                page = 0;
                loading = true;
                posterArrayList.clear();
                adapter.notifyDataSetChanged();
                loadPostersFromJson();

                return true;
        }
        return super.onOptionsItemSelected(itemMenu);
    }
    private void getGenre() {
        // Get genre info from intent extras instead of Parcelable
        Integer genreId = getIntent().getIntExtra("genre_id", -1);
        String genreTitle = getIntent().getStringExtra("genre_title");
        from = getIntent().getStringExtra("from");
        
        // Create a new Genre object with the received data
        if (genreId != null && genreTitle != null) {
            genre = new Genre();
            genre.setId(genreId);
            genre.setTitle(genreTitle);
            Log.d("GenreActivity", "Received genre: " + genreTitle + " (ID: " + genreId + ")");
        } else {
            Log.e("GenreActivity", "Genre ID or title is null, finishing activity");
            finish();
            return;
        }
    }

    /**
     * Load posters from GitHub JSON API instead of old server API
     */
    private void loadPostersFromJson() {
        if (page==0){
            linear_layout_load_genre_activity.setVisibility(View.VISIBLE);
        }else{
            relative_layout_load_more.setVisibility(View.VISIBLE);
        }
        swipe_refresh_layout_list_genre_search.setRefreshing(false);
        
        // If we already have cached data, use it
        if (cachedJsonResponse != null && !allMovies.isEmpty()) {
            filterAndDisplayMovies();
            return;
        }
        
        // Load data from GitHub JSON API
        apiClient.getJsonApiData(new apiClient.JsonApiCallback() {
            @Override
            public void onSuccess(JsonApiResponse jsonResponse) {
                if (jsonResponse != null && jsonResponse.getMovies() != null) {
                    cachedJsonResponse = jsonResponse;
                    allMovies = jsonResponse.getMovies();
                    Log.d("GenreActivity", "Loaded " + allMovies.size() + " movies/series");
                    filterAndDisplayMovies();
                } else {
                    Log.e("GenreActivity", "JSON response or movies is null");
                    showError();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e("GenreActivity", "Error loading JSON data: " + error);
                showError();
            }
        });
    }
    
    /**
     * Filter movies and series by genre and order, then display them
     */
    private void filterAndDisplayMovies() {
        try {
            List<Poster> filteredMovies = new ArrayList<>();
            
            Log.d("GenreActivity", "Filtering for genre ID: " + genre.getId() + ", title: " + genre.getTitle());
            Log.d("GenreActivity", "Total movies/series to filter: " + allMovies.size());
            
            // Filter movies and series by genre
            for (Poster movie : allMovies) {
            if (genre.getId() == -1) {
                // Special case for "Top Rated" - show all movies and series
                filteredMovies.add(movie);
            } else if (genre.getId() == 0) {
                // Special case for "Most Viewed" - show all movies and series
                filteredMovies.add(movie);
            } else if (genre.getId() == -2) {
                // Special case for "My List" - show all movies and series (or implement actual my list logic)
                filteredMovies.add(movie);
            } else {
                // Filter by actual genre
                if (movie.getGenres() != null) {
                    for (Genre movieGenre : movie.getGenres()) {
                        if (movieGenre.getId() != null && movieGenre.getId().equals(genre.getId())) {
                            filteredMovies.add(movie);
                            break;
                        }
                    }
                }
            }
        }
        
        Log.d("GenreActivity", "Found " + filteredMovies.size() + " movies/series for genre: " + genre.getTitle());
        
        // Sort movies by selected order
        sortMoviesByOrder(filteredMovies, SelectedOrder);
        
        // Apply pagination
        int startIndex = page * 20; // 20 items per page
        int endIndex = Math.min(startIndex + 20, filteredMovies.size());
        
        if (startIndex < filteredMovies.size()) {
            List<Poster> pageMovies = filteredMovies.subList(startIndex, endIndex);
            
            // Add movies to the list
            for (Poster movie : pageMovies) {
                posterArrayList.add(movie);
                if (native_ads_enabled){
                    item++;
                    if (item == lines_beetween_ads ){
                        item= 0;
                        if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("FACEBOOK")) {
                            posterArrayList.add(new Poster().setTypeView(4));
                        }else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("ADMOB")){
                            posterArrayList.add(new Poster().setTypeView(5));
                        } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("BOTH")){
                            if (type_ads == 0) {
                                posterArrayList.add(new Poster().setTypeView(4));
                                type_ads = 1;
                            }else if (type_ads == 1){
                                posterArrayList.add(new Poster().setTypeView(5));
                                type_ads = 0;
                            }
                        }
                    }
                }
            }
            
            linear_layout_layout_error.setVisibility(View.GONE);
            recycler_view_activity_genre.setVisibility(View.VISIBLE);
            image_view_empty_list.setVisibility(View.GONE);

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            page++;
            loading=true;
        } else {
            if (page==0) {
                linear_layout_layout_error.setVisibility(View.GONE);
                recycler_view_activity_genre.setVisibility(View.GONE);
                image_view_empty_list.setVisibility(View.VISIBLE);
            }
        }
        
        relative_layout_load_more.setVisibility(View.GONE);
        swipe_refresh_layout_list_genre_search.setRefreshing(false);
        linear_layout_load_genre_activity.setVisibility(View.GONE);
        
        } catch (Exception e) {
            Log.e("GenreActivity", "Error in filterAndDisplayMovies: " + e.getMessage(), e);
            showError();
        }
    }
    
    /**
     * Sort movies by the selected order
     */
    private void sortMoviesByOrder(List<Poster> movies, String order) {
        switch (order) {
            case "rating":
                movies.sort((m1, m2) -> {
                    Float rating1 = m1.getRating() != null ? m1.getRating() : 0f;
                    Float rating2 = m2.getRating() != null ? m2.getRating() : 0f;
                    return rating2.compareTo(rating1); // Descending order
                });
                break;
            case "views":
                movies.sort((m1, m2) -> {
                    Integer views1 = m1.getViews() != null ? m1.getViews() : 0;
                    Integer views2 = m2.getViews() != null ? m2.getViews() : 0;
                    return views2.compareTo(views1); // Descending order
                });
                break;
            case "year":
                movies.sort((m1, m2) -> {
                    String year1 = m1.getYear() != null ? m1.getYear() : "";
                    String year2 = m2.getYear() != null ? m2.getYear() : "";
                    return year2.compareTo(year1); // Descending order
                });
                break;
            case "title":
                movies.sort((m1, m2) -> {
                    String title1 = m1.getTitle() != null ? m1.getTitle() : "";
                    String title2 = m2.getTitle() != null ? m2.getTitle() : "";
                    return title1.compareToIgnoreCase(title2); // Ascending order
                });
                break;
            case "imdb":
                movies.sort((m1, m2) -> {
                    String imdb1 = m1.getImdb() != null ? m1.getImdb() : "";
                    String imdb2 = m2.getImdb() != null ? m2.getImdb() : "";
                    return imdb2.compareTo(imdb1); // Descending order
                });
                break;
            case "created":
            default:
                // Keep original order (most recent first)
                break;
        }
    }
    
    /**
     * Show error state
     */
    private void showError() {
        linear_layout_layout_error.setVisibility(View.VISIBLE);
        recycler_view_activity_genre.setVisibility(View.GONE);
        image_view_empty_list.setVisibility(View.GONE);
        relative_layout_load_more.setVisibility(View.GONE);
        swipe_refresh_layout_list_genre_search.setVisibility(View.GONE);
        linear_layout_load_genre_activity.setVisibility(View.GONE);
    }

    /**
     * @deprecated Old API method - replaced with loadPostersFromJson()
     */
    @Deprecated
    private void loadPosters() {
        // This method is kept for backward compatibility but now redirects to JSON API
        loadPostersFromJson();
    }

    private void initAction() {

        swipe_refresh_layout_list_genre_search.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                item = 0;
                page = 0;
                loading = true;
                posterArrayList.clear();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                loadPostersFromJson();
            }
        });
        button_try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                item = 0;
                page = 0;
                loading = true;
                posterArrayList.clear();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                loadPostersFromJson();
            }
        });
        recycler_view_activity_genre.addOnScrollListener(new RecyclerView.OnScrollListener()
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
                            loading = false;
                            loadPostersFromJson();
                        }
                    }
                }else{

                }
            }
        });
    }

    private void initView() {

        boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
        if (!prefManager.getString("ADMIN_NATIVE_TYPE").equals("FALSE")){
            native_ads_enabled=true;
            if (tabletSize) {
                lines_beetween_ads=6*Integer.parseInt(prefManager.getString("ADMIN_NATIVE_LINES"));
            }else{
                lines_beetween_ads=3*Integer.parseInt(prefManager.getString("ADMIN_NATIVE_LINES"));
            }
        }
        if (checkSUBSCRIBED()) {
            native_ads_enabled=false;
        }

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle(genre.getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.linear_layout_load_genre_activity=findViewById(R.id.linear_layout_load_genre_activity);
        this.relative_layout_load_more=findViewById(R.id.relative_layout_load_more);
        this.swipe_refresh_layout_list_genre_search=findViewById(R.id.swipe_refresh_layout_list_genre_search);
        button_try_again            = findViewById(R.id.button_try_again);
        image_view_empty_list       = findViewById(R.id.image_view_empty_list);
        linear_layout_layout_error  = findViewById(R.id.linear_layout_layout_error);
        recycler_view_activity_genre          = findViewById(R.id.recycler_view_activity_genre);
        adapter = new PosterAdapter(posterArrayList, this);

        if (native_ads_enabled){
            Log.v("MYADS","ENABLED");
            if (tabletSize) {
                this.gridLayoutManager=  new GridLayoutManager(getApplicationContext(),6,RecyclerView.VERTICAL,false);
                Log.v("MYADS","tabletSize");
                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return ((position  + 1) % (lines_beetween_ads  + 1  ) == 0 && position!=0) ? 6 : 1;
                    }
                });
            } else {
                this.gridLayoutManager=  new GridLayoutManager(getApplicationContext(),3,RecyclerView.VERTICAL,false);
                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return ((position  + 1) % (lines_beetween_ads + 1 ) == 0  && position!=0)  ? 3 : 1;
                    }
                });
            }
        }else {
            if (tabletSize) {
                this.gridLayoutManager=  new GridLayoutManager(getApplicationContext(),6,RecyclerView.VERTICAL,false);
            } else {
                this.gridLayoutManager=  new GridLayoutManager(getApplicationContext(),3,RecyclerView.VERTICAL,false);
            }
        }

        recycler_view_activity_genre.setHasFixedSize(true);
        recycler_view_activity_genre.setAdapter(adapter);
        recycler_view_activity_genre.setLayoutManager(gridLayoutManager);

    }

    public boolean checkSUBSCRIBED(){
        PrefManager prefManager= new PrefManager(getApplicationContext());
        if (!prefManager.getString("SUBSCRIBED").equals("TRUE") && !prefManager.getString("NEW_SUBSCRIBE_ENABLED").equals("TRUE")) {
            return false;
        }
        return true;
    }
    public void showAdsBanner() {
        if (!checkSUBSCRIBED()) {
            PrefManager prefManager= new PrefManager(getApplicationContext());
            if (!prefManager.getString("ADMIN_BANNER_TYPE").equals("FALSE")){
                showAdmobBanner();
            }
        }
    }
    public void showAdmobBanner(){
        PrefManager prefManager= new PrefManager(getApplicationContext());
        LinearLayout linear_layout_ads =  (LinearLayout) findViewById(R.id.linear_layout_ads);
        final AdView mAdView = new AdView(this);
        mAdView.setAdSize(AdSize.SMART_BANNER);
        mAdView.setAdUnitId(prefManager.getString("ADMIN_BANNER_ADMOB_ID"));
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mAdView.loadAd(adRequest);
        linear_layout_ads.addView(mAdView);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                mAdView.setVisibility(View.VISIBLE);

            }
        });
    }

}
