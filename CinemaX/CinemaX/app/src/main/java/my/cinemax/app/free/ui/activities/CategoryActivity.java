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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import my.cinemax.app.free.R;
import my.cinemax.app.free.api.apiClient;
import my.cinemax.app.free.api.apiRest;
import my.cinemax.app.free.entity.Category;
import my.cinemax.app.free.entity.Channel;
import my.cinemax.app.free.entity.JsonApiResponse;
import my.cinemax.app.free.ui.Adapters.ChannelAdapter;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipe_refresh_layout_list_category_search;
    private Button button_try_again;
    private LinearLayout linear_layout_layout_error;
    private RecyclerView recycler_view_activity_category;
    private ImageView image_view_empty_list;
    private GridLayoutManager gridLayoutManager;
    private ChannelAdapter adapter;

    private int pastVisiblesItems, visibleItemCount, totalItemCount;
    private boolean loading = true;

    private Integer page = 0;
    private Integer position = 0;
    private Integer item = 0 ;
    ArrayList<Channel> posterArrayList = new ArrayList<>();
    private RelativeLayout relative_layout_load_more;
    private LinearLayout linear_layout_load_category_activity;

    private String SelectedOrder = "created";
    private Category category;
    private String from;

    // JSON API data cache
    private JsonApiResponse cachedJsonResponse = null;
    private List<Channel> allChannels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        getCategories();
        initView();
        initAction();
        loadChannelsFromJson();
    }

    private void getCategories() {
        category = getIntent().getParcelableExtra("category");
        from = getIntent().getStringExtra("from");
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (from!=null){
                    Intent intent =  new Intent(getApplicationContext(),HomeActivity.class);
                    startActivity(intent);
                }else{
                    super.onBackPressed();
                }
                return true;

            }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Load channels from GitHub JSON API instead of old server API
     */
    private void loadChannelsFromJson() {
        if (page==0){
            linear_layout_load_category_activity.setVisibility(View.VISIBLE);
        }else{
            relative_layout_load_more.setVisibility(View.VISIBLE);
        }
        swipe_refresh_layout_list_category_search.setRefreshing(false);
        
        // If we already have cached data, use it
        if (cachedJsonResponse != null && !allChannels.isEmpty()) {
            filterAndDisplayChannels();
            return;
        }
        
        // Load data from GitHub JSON API
        apiClient.getJsonApiData(new apiClient.JsonApiCallback() {
            @Override
            public void onSuccess(JsonApiResponse jsonResponse) {
                if (jsonResponse != null && jsonResponse.getChannels() != null) {
                    cachedJsonResponse = jsonResponse;
                    allChannels = jsonResponse.getChannels();
                    filterAndDisplayChannels();
                } else {
                    showError();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e("CategoryActivity", "Error loading JSON data: " + error);
                showError();
            }
        });
    }
    
    /**
     * Filter channels by category and display them
     */
    private void filterAndDisplayChannels() {
        List<Channel> filteredChannels = new ArrayList<>();
        
        // Filter channels by category
        for (Channel channel : allChannels) {
            if (channel.getCategories() != null) {
                for (Category channelCategory : channel.getCategories()) {
                    if (channelCategory.getId() != null && 
                        channelCategory.getId().equals(category.getId())) {
                        filteredChannels.add(channel);
                        break;
                    }
                }
            }
        }
        
        // Apply pagination
        int startIndex = page * 20; // 20 items per page
        int endIndex = Math.min(startIndex + 20, filteredChannels.size());
        
        if (startIndex < filteredChannels.size()) {
            List<Channel> pageChannels = filteredChannels.subList(startIndex, endIndex);
            
            // Add channels to the list
            for (Channel channel : pageChannels) {
                posterArrayList.add(channel);
            }
            
            linear_layout_layout_error.setVisibility(View.GONE);
            recycler_view_activity_category.setVisibility(View.VISIBLE);
            image_view_empty_list.setVisibility(View.GONE);

            adapter.notifyDataSetChanged();
            page++;
            loading=true;
        } else {
            if (page==0) {
                linear_layout_layout_error.setVisibility(View.GONE);
                recycler_view_activity_category.setVisibility(View.GONE);
                image_view_empty_list.setVisibility(View.VISIBLE);
            }
        }
        
        relative_layout_load_more.setVisibility(View.GONE);
        swipe_refresh_layout_list_category_search.setRefreshing(false);
        linear_layout_load_category_activity.setVisibility(View.GONE);
    }
    
    /**
     * Show error state
     */
    private void showError() {
        linear_layout_layout_error.setVisibility(View.VISIBLE);
        recycler_view_activity_category.setVisibility(View.GONE);
        image_view_empty_list.setVisibility(View.GONE);
        relative_layout_load_more.setVisibility(View.GONE);
        swipe_refresh_layout_list_category_search.setVisibility(View.GONE);
        linear_layout_load_category_activity.setVisibility(View.GONE);
    }

    /**
     * @deprecated Old API method - replaced with loadChannelsFromJson()
     */
    @Deprecated
    private void loadChannels() {
        // This method is kept for backward compatibility but now redirects to JSON API
        loadChannelsFromJson();
    }

    private void initAction() {

        swipe_refresh_layout_list_category_search.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                page = 0;
                loading = true;
                posterArrayList.clear();
                adapter.notifyDataSetChanged();
                loadChannelsFromJson();
            }
        });
        button_try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                page = 0;
                loading = true;
                posterArrayList.clear();
                adapter.notifyDataSetChanged();
                loadChannelsFromJson();
            }
        });
        recycler_view_activity_category.addOnScrollListener(new RecyclerView.OnScrollListener()
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
                            loadChannelsFromJson();
                        }
                    }
                }else{

                }
            }
        });
    }

    private void initView() {

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle(category.getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.linear_layout_load_category_activity=findViewById(R.id.linear_layout_load_category_activity);
        this.relative_layout_load_more=findViewById(R.id.relative_layout_load_more);
        this.swipe_refresh_layout_list_category_search=findViewById(R.id.swipe_refresh_layout_list_category_search);
        button_try_again            = findViewById(R.id.button_try_again);
        image_view_empty_list       = findViewById(R.id.image_view_empty_list);
        linear_layout_layout_error  = findViewById(R.id.linear_layout_layout_error);
        recycler_view_activity_category          = findViewById(R.id.recycler_view_activity_category);
        adapter = new ChannelAdapter(posterArrayList, this);
        this.gridLayoutManager=  new GridLayoutManager(getApplicationContext(),2,RecyclerView.VERTICAL,false);
        recycler_view_activity_category.setHasFixedSize(true);
        recycler_view_activity_category.setAdapter(adapter);
        recycler_view_activity_category.setLayoutManager(gridLayoutManager);

    }
}
