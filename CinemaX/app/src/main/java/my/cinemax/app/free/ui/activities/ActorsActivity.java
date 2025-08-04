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

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import my.cinemax.app.free.entity.Actor;
import my.cinemax.app.free.entity.JsonApiResponse;
import my.cinemax.app.free.ui.Adapters.ActorAdapter;

import java.util.ArrayList;
import java.util.List;

public class ActorsActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipe_refresh_layout_list_actors_search;
    private Button button_try_again;
    private LinearLayout linear_layout_layout_error;
    private RecyclerView recycler_view_activity_actors;
    private ImageView image_view_empty_list;
    private GridLayoutManager gridLayoutManager;
    private ActorAdapter adapter;

    private int pastVisiblesItems, visibleItemCount, totalItemCount;
    private boolean loading = true;

    private Integer page = 0;
    private Integer position = 0;
    private Integer item = 0 ;
    ArrayList<Actor> actorArrayList = new ArrayList<>();
    private RelativeLayout relative_layout_load_more;
    private LinearLayout linear_layout_load_actors_activity;
    private ImageView image_view_activity_actors_search;
    private ImageView image_view_activity_actors_close_search;
    private EditText edit_text_actors_activity_actors;
    private String searchtext = "null";

    // JSON API data cache
    private JsonApiResponse cachedJsonResponse = null;
    private List<Actor> allActors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actors);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initView();
        initAction();
        loadActorsFromJson();
        showAdsBanner();
    }

    /**
     * Load actors from GitHub JSON API instead of old server API
     */
    private void loadActorsFromJson() {
        if (page==0){
            linear_layout_load_actors_activity.setVisibility(View.VISIBLE);
        }else{
            relative_layout_load_more.setVisibility(View.VISIBLE);
        }
        swipe_refresh_layout_list_actors_search.setRefreshing(false);
        
        // If we already have cached data, use it
        if (cachedJsonResponse != null && !allActors.isEmpty()) {
            filterAndDisplayActors();
            return;
        }
        
        // Load data from GitHub JSON API
        apiClient.getJsonApiData(new apiClient.JsonApiCallback() {
            @Override
            public void onSuccess(JsonApiResponse jsonResponse) {
                if (jsonResponse != null && jsonResponse.getActors() != null) {
                    cachedJsonResponse = jsonResponse;
                    allActors = jsonResponse.getActors();
                    filterAndDisplayActors();
                } else {
                    showError();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e("ActorsActivity", "Error loading JSON data: " + error);
                showError();
            }
        });
    }
    
    /**
     * Filter actors by search text and display them
     */
    private void filterAndDisplayActors() {
        List<Actor> filteredActors = new ArrayList<>();
        
        // Filter actors by search text
        if ("null".equals(searchtext) || searchtext.isEmpty()) {
            filteredActors = new ArrayList<>(allActors);
        } else {
            for (Actor actor : allActors) {
                if (actor.getName() != null && 
                    actor.getName().toLowerCase().contains(searchtext.toLowerCase())) {
                    filteredActors.add(actor);
                }
            }
        }
        
        // Apply pagination
        int startIndex = page * 20; // 20 items per page
        int endIndex = Math.min(startIndex + 20, filteredActors.size());
        
        if (startIndex < filteredActors.size()) {
            List<Actor> pageActors = filteredActors.subList(startIndex, endIndex);
            
            // Add actors to the list
            for (Actor actor : pageActors) {
                actorArrayList.add(actor);
            }
            
            linear_layout_layout_error.setVisibility(View.GONE);
            recycler_view_activity_actors.setVisibility(View.VISIBLE);
            image_view_empty_list.setVisibility(View.GONE);

            adapter.notifyDataSetChanged();
            page++;
            loading=true;
        } else {
            if (page==0) {
                linear_layout_layout_error.setVisibility(View.GONE);
                recycler_view_activity_actors.setVisibility(View.GONE);
                image_view_empty_list.setVisibility(View.VISIBLE);
            }
        }
        
        relative_layout_load_more.setVisibility(View.GONE);
        swipe_refresh_layout_list_actors_search.setRefreshing(false);
        linear_layout_load_actors_activity.setVisibility(View.GONE);
    }
    
    /**
     * Show error state
     */
    private void showError() {
        linear_layout_layout_error.setVisibility(View.VISIBLE);
        recycler_view_activity_actors.setVisibility(View.GONE);
        image_view_empty_list.setVisibility(View.GONE);
        relative_layout_load_more.setVisibility(View.GONE);
        swipe_refresh_layout_list_actors_search.setVisibility(View.GONE);
        linear_layout_load_actors_activity.setVisibility(View.GONE);
    }

    /**
     * @deprecated Old API method - replaced with loadActorsFromJson()
     */
    @Deprecated
    private void loadActors() {
        // This method is kept for backward compatibility but now redirects to JSON API
        loadActorsFromJson();
    }

    private void initAction() {

        image_view_activity_actors_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchtext = edit_text_actors_activity_actors.getText().toString();
                page = 0;
                loading = true;
                actorArrayList.clear();
                adapter.notifyDataSetChanged();
                loadActorsFromJson();
            }
        });
        image_view_activity_actors_close_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchtext = "null";
                edit_text_actors_activity_actors.setText("");
                page = 0;
                loading = true;
                actorArrayList.clear();
                adapter.notifyDataSetChanged();
                loadActorsFromJson();
            }
        });

        swipe_refresh_layout_list_actors_search.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                page = 0;
                loading = true;
                actorArrayList.clear();
                adapter.notifyDataSetChanged();
                loadActorsFromJson();
            }
        });
        button_try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                page = 0;
                loading = true;
                actorArrayList.clear();
                adapter.notifyDataSetChanged();
                loadActorsFromJson();
            }
        });
        recycler_view_activity_actors.addOnScrollListener(new RecyclerView.OnScrollListener()
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
                            loadActorsFromJson();
                        }
                    }
                }else{

                }
            }
        });
    }

    private void initView() {

        this.linear_layout_load_actors_activity=findViewById(R.id.linear_layout_load_actors_activity);
        this.relative_layout_load_more=findViewById(R.id.relative_layout_load_more);
        this.swipe_refresh_layout_list_actors_search=findViewById(R.id.swipe_refresh_layout_list_actors_search);
        button_try_again            = findViewById(R.id.button_try_again);
        image_view_empty_list       = findViewById(R.id.image_view_empty_list);
        linear_layout_layout_error  = findViewById(R.id.linear_layout_layout_error);
        recycler_view_activity_actors          = findViewById(R.id.recycler_view_activity_actors);
        this.image_view_activity_actors_search=findViewById(R.id.image_view_activity_actors_search);
        this.image_view_activity_actors_close_search=findViewById(R.id.image_view_activity_actors_close_search);
        this.edit_text_actors_activity_actors=findViewById(R.id.edit_text_actors_activity_actors);
        adapter = new ActorAdapter(actorArrayList, this);
        this.gridLayoutManager=  new GridLayoutManager(getApplicationContext(),3,RecyclerView.VERTICAL,false);
        recycler_view_activity_actors.setHasFixedSize(true);
        recycler_view_activity_actors.setAdapter(adapter);
        recycler_view_activity_actors.setLayoutManager(gridLayoutManager);

    }
    public boolean checkSUBSCRIBED(){
        PrefManager prefManager= new PrefManager(getApplicationContext());
        if (!prefManager.getString("SUBSCRIBED").equals("TRUE") && !prefManager.getString("NEW_SUBSCRIBE_ENABLED").equals("TRUE")) {
            return false;
        }
        return true;
    }
    @Override
    public void onBackPressed(){
        super.onBackPressed();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
