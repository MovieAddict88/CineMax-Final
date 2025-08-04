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
import my.cinemax.app.free.entity.Poster;
import my.cinemax.app.free.entity.JsonApiResponse;
import my.cinemax.app.free.ui.Adapters.PosterAdapter;

import java.util.ArrayList;
import java.util.List;

public class TopActivity extends AppCompatActivity {
    private SwipeRefreshLayout swipe_refresh_layout_list_top_search;
    private Button button_try_again;
    private LinearLayout linear_layout_layout_error;
    private RecyclerView recycler_view_activity_top;
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
    private LinearLayout linear_layout_load_top_activity;

    private String order;

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
        setContentView(R.layout.activity_top);
        prefManager= new PrefManager(getApplicationContext());

        getOrder();
        initView();
        initAction();
        loadPostersFromJson();
        showAdsBanner();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem itemMenu) {
        switch (itemMenu.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(itemMenu);
    }

    private void getOrder() {
        order = getIntent().getStringExtra("order");
    }

    /**
     * Load posters from GitHub JSON API instead of old server API
     */
    private void loadPostersFromJson() {
        if (page==0){
            linear_layout_load_top_activity.setVisibility(View.VISIBLE);
        }else{
            relative_layout_load_more.setVisibility(View.VISIBLE);
        }
        swipe_refresh_layout_list_top_search.setRefreshing(false);
        
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
                    filterAndDisplayMovies();
                } else {
                    showError();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e("TopActivity", "Error loading JSON data: " + error);
                showError();
            }
        });
    }
    
    /**
     * Filter movies by order and display them
     */
    private void filterAndDisplayMovies() {
        List<Poster> filteredMovies = new ArrayList<>(allMovies);
        
        // Sort movies by selected order
        sortMoviesByOrder(filteredMovies, order);
        
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
            recycler_view_activity_top.setVisibility(View.VISIBLE);
            image_view_empty_list.setVisibility(View.GONE);

            adapter.notifyDataSetChanged();
            page++;
            loading=true;
        } else {
            if (page==0) {
                linear_layout_layout_error.setVisibility(View.GONE);
                recycler_view_activity_top.setVisibility(View.GONE);
                image_view_empty_list.setVisibility(View.VISIBLE);
            }
        }
        
        relative_layout_load_more.setVisibility(View.GONE);
        swipe_refresh_layout_list_top_search.setRefreshing(false);
        linear_layout_load_top_activity.setVisibility(View.GONE);
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
                    Float rating1 = m1.getRating() != null ? m1.getRating() : 0f;
                    Float rating2 = m2.getRating() != null ? m2.getRating() : 0f;
                    return rating2.compareTo(rating1); // Descending order
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
        recycler_view_activity_top.setVisibility(View.GONE);
        image_view_empty_list.setVisibility(View.GONE);
        relative_layout_load_more.setVisibility(View.GONE);
        swipe_refresh_layout_list_top_search.setVisibility(View.GONE);
        linear_layout_load_top_activity.setVisibility(View.GONE);
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

        swipe_refresh_layout_list_top_search.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                item = 0;
                page = 0;
                loading = true;
                posterArrayList.clear();
                adapter.notifyDataSetChanged();
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
                adapter.notifyDataSetChanged();
                loadPostersFromJson();
            }
        });
        recycler_view_activity_top.addOnScrollListener(new RecyclerView.OnScrollListener()
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
        toolbar.setTitle(order);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.linear_layout_load_top_activity=findViewById(R.id.linear_layout_load_top_activity);
        this.relative_layout_load_more=findViewById(R.id.relative_layout_load_more);
        this.swipe_refresh_layout_list_top_search=findViewById(R.id.swipe_refresh_layout_list_top_search);
        button_try_again            = findViewById(R.id.button_try_again);
        image_view_empty_list       = findViewById(R.id.image_view_empty_list);
        linear_layout_layout_error  = findViewById(R.id.linear_layout_layout_error);
        recycler_view_activity_top          = findViewById(R.id.recycler_view_activity_top);
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

        recycler_view_activity_top.setHasFixedSize(true);
        recycler_view_activity_top.setAdapter(adapter);
        recycler_view_activity_top.setLayoutManager(gridLayoutManager);

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
