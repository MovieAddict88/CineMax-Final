package my.cinemax.app.free.ui.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;



import com.applovin.sdk.AppLovinPrivacySettings;
import com.congle7997.google_iap.BillingSubs;
import com.congle7997.google_iap.CallBackBilling;
import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.gauravk.bubblenavigation.listener.BubbleNavigationChangeListener;
import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jackandphantom.blurimage.BlurImage;
import my.cinemax.app.free.Provider.PrefManager;
import my.cinemax.app.free.R;
import my.cinemax.app.free.api.apiClient;
import my.cinemax.app.free.api.apiRest;
import my.cinemax.app.free.config.Global;
import my.cinemax.app.free.entity.ApiResponse;
import my.cinemax.app.free.entity.Genre;
import my.cinemax.app.free.entity.JsonApiResponse;
import my.cinemax.app.free.entity.Poster;
import my.cinemax.app.free.entity.Channel;
import my.cinemax.app.free.ui.fragments.DownloadsFragment;
import my.cinemax.app.free.ui.fragments.HomeFragment;
import my.cinemax.app.free.ui.fragments.MoviesFragment;
import my.cinemax.app.free.ui.fragments.SeriesFragment;
import my.cinemax.app.free.ui.fragments.TvFragment;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import android.os.Handler;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.content.ComponentCallbacks2;

import androidx.appcompat.widget.Toolbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import de.hdodenhof.circleimageview.CircleImageView;
import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import my.cinemax.app.free.entity.Actress;
import my.cinemax.app.free.Provider.DataRepository;
import my.cinemax.app.free.BuildConfig;
import my.cinemax.app.free.Utils.CacheManager;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private final List<Fragment> mFragmentList = new ArrayList<>();
    private ViewPager viewPager;
    private ViewPagerAdapter adapter;
    private JsonApiResponse cachedJsonResponse = null;
    private boolean dataLoaded = false;
    private DataRepository dataRepository;
    private NavigationView navigationView;
    private TextView text_view_name_nave_header;
    private CircleImageView circle_image_view_profile_nav_header;
    private ImageView image_view_profile_nav_header_bg;
    private Dialog rateDialog;
    private boolean FromLogin;
    private RelativeLayout relative_layout_home_activity_search_section;
    private EditText edit_text_home_activity_search;
    private ImageView image_view_activity_home_close_search;
    private ImageView image_view_activity_home_search;
    private ImageView image_view_activity_actors_back;
    private Dialog dialog;
    ConsentForm form;


    public static final String PREF_FILE= "MyPref";
    public static final String SUBSCRIBE_KEY= "subscribe";
    public static final String ITEM_SKU_SUBSCRIBE= "sub_example";




    private String payment_methode_id = "null";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Add crash protection
        try {
            setContentView(R.layout.activity_home);
            
            // Initialize DataRepository first
            dataRepository = DataRepository.getInstance();
            dataRepository.initialize(this);
            
            // Initialize views and actions
            initViews();
            initActions();
            firebaseSubscribe();
            initGDPR();
            initBuy();
            
            // Enhanced loading strategy
            initializeDataLoading();
            
            Log.d("HomeActivity", "Activity created successfully");
            
        } catch (Exception e) {
            Log.e("HomeActivity", "Error in onCreate", e);
            // Prevent crash by showing error and finishing activity gracefully
            finish();
        }
    }
    
    /**
     * Initialize data loading with proper cache handling
     */
    private void initializeDataLoading() {
        try {
            Log.d("HomeActivity", "Initializing data loading...");
            
            // Always show loading state first to prevent infinite loading
            showLoadingOnAllFragments();
            
            // Add delay to ensure fragments are properly initialized
            new Handler().postDelayed(() -> {
                try {
                    // Check if we have valid cached data
                    JsonApiResponse cachedResponse = dataRepository.getSimpleCacheManager().getApiResponse();
                    if (cachedResponse != null && dataRepository.getSimpleCacheManager().isCacheValid("api_response")) {
                        Log.d("HomeActivity", "Found valid cache, displaying immediately");
                        handleJsonResponse(cachedResponse, "Cache");
                        
                        // Schedule background refresh for later
                        scheduleBackgroundRefresh();
                    } else {
                        Log.d("HomeActivity", "No valid cache found, loading from API");
                        // Load fresh data from API
                        loadAllDataWithCaching();
                    }
                } catch (Exception e) {
                    Log.e("HomeActivity", "Error in delayed data loading", e);
                    // Fallback to legacy loading
                    loadAllDataFromJson();
                }
            }, 100); // 100ms delay to ensure fragments are ready
            
        } catch (Exception e) {
            Log.e("HomeActivity", "Error initializing data loading", e);
            // Fallback to legacy loading
            loadAllDataFromJson();
        }
    }
    
    /**
     * Show loading state on all fragments to prevent black screens
     */
    private void showLoadingOnAllFragments() {
        try {
            // Ensure all fragments show loading state
            for (Fragment fragment : mFragmentList) {
                if (fragment instanceof HomeFragment) {
                    ((HomeFragment) fragment).resetLoadingState();
                }
            }
            Log.d("HomeActivity", "Loading state applied to all fragments");
        } catch (Exception e) {
            Log.e("HomeActivity", "Error showing loading on fragments", e);
        }
    }
    
    /**
     * Schedule background refresh for better performance
     */
    private void scheduleBackgroundRefresh() {
        new Handler().postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                Log.d("HomeActivity", "Performing scheduled background refresh");
                dataRepository.refreshData(new DataRepository.ApiResponseCallback() {
                    @Override
                    public void onSuccess(JsonApiResponse response) {
                        Log.d("HomeActivity", "Background refresh successful");
                        // Optionally update UI with fresh data
                    }
                    
                    @Override
                    public void onFromCache(JsonApiResponse response) {
                        Log.d("HomeActivity", "Background refresh returned cache");
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e("HomeActivity", "Background refresh failed: " + error);
                    }
                    
                    @Override
                    public void onLoading() {
                        Log.d("HomeActivity", "Background refresh loading");
                    }
                });
            }
        }, 2000); // 2 second delay
    }

    BillingSubs billingSubs;
    public void initBuy(){
        List<String> listSkuStoreSubs = new ArrayList<>();
        listSkuStoreSubs.add(Global.SUBSCRIPTION_ID);
        billingSubs = new BillingSubs(this, listSkuStoreSubs, new CallBackBilling() {
            @Override
            public void onPurchase() {
                PrefManager prefManager= new PrefManager(getApplicationContext());
                prefManager.setString("SUBSCRIBED","TRUE");
                Toasty.success(HomeActivity.this, "you have successfully subscribed ", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNotPurchase() {
                Toasty.warning(HomeActivity.this, "Operation has been cancelled  ", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNotLogin() {
            }
        });
    }

    public void subscribe(){
        billingSubs.purchase(Global.SUBSCRIPTION_ID);
    }



    private void initActions() {
        image_view_activity_actors_back.setOnClickListener(v->{
            relative_layout_home_activity_search_section.setVisibility(View.GONE);
            edit_text_home_activity_search.setText("");
        });
        edit_text_home_activity_search.setOnEditorActionListener((v,actionId,event) -> {
            try {
                if (edit_text_home_activity_search != null && 
                    edit_text_home_activity_search.getText() != null && 
                    edit_text_home_activity_search.getText().toString().trim().length() > 0) {
                    
                    String searchQuery = edit_text_home_activity_search.getText().toString().trim();
                    Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
                    intent.putExtra("query", searchQuery);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter, R.anim.exit);

                    if (relative_layout_home_activity_search_section != null) {
                        relative_layout_home_activity_search_section.setVisibility(View.GONE);
                    }
                    edit_text_home_activity_search.setText("");
                } else {
                    // Show message for empty search
                    Toast.makeText(HomeActivity.this, "Please enter a search term", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("HomeActivity", "Error in search action: " + e.getMessage(), e);
                Toast.makeText(HomeActivity.this, "Search error occurred", Toast.LENGTH_SHORT).show();
            }
            return false;
        });
        image_view_activity_home_close_search.setOnClickListener(v->{
            edit_text_home_activity_search.setText("");
        });
        image_view_activity_home_search.setOnClickListener(v->{
            try {
                if (edit_text_home_activity_search != null && 
                    edit_text_home_activity_search.getText() != null && 
                    edit_text_home_activity_search.getText().toString().trim().length() > 0) {
                    
                    String searchQuery = edit_text_home_activity_search.getText().toString().trim();
                    Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
                    intent.putExtra("query", searchQuery);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter, R.anim.exit);
                    
                    if (relative_layout_home_activity_search_section != null) {
                        relative_layout_home_activity_search_section.setVisibility(View.GONE);
                    }
                    edit_text_home_activity_search.setText("");
                } else {
                    // Show message for empty search
                    Toast.makeText(HomeActivity.this, "Please enter a search term", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("HomeActivity", "Error in search click: " + e.getMessage(), e);
                Toast.makeText(HomeActivity.this, "Search error occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        this.navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        View headerview = navigationView.getHeaderView(0);
        this.text_view_name_nave_header=(TextView) headerview.findViewById(R.id.text_view_name_nave_header);
        this.circle_image_view_profile_nav_header=(CircleImageView) headerview.findViewById(R.id.circle_image_view_profile_nav_header);
        this.image_view_profile_nav_header_bg=(ImageView) headerview.findViewById(R.id.image_view_profile_nav_header_bg);
        // init pager view

        viewPager = (ViewPager) findViewById(R.id.vp_horizontal_ntb);
        viewPager.setOffscreenPageLimit(100);
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new HomeFragment());
        adapter.addFragment(new MoviesFragment());
        adapter.addFragment(new SeriesFragment());
        adapter.addFragment(new TvFragment());
        adapter.addFragment(new DownloadsFragment());
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);
        final BubbleNavigationConstraintView bubbleNavigationLinearView = findViewById(R.id.top_navigation_constraint);

        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                bubbleNavigationLinearView.setCurrentActiveItem(i);
                
                // Ensure data is loaded for this page
                ensureDataLoadedForPage(i);
                
                // Update fragments with cached data when switching pages
                if (dataLoaded && cachedJsonResponse != null) {
                    switch (i) {
                        case 0: // Home
                            updateHomeFragmentWithCachedData();
                            break;
                        case 1: // Movies
                            updateMoviesFragmentWithCachedData();
                            break;
                        case 3: // TV/Live
                            updateTvFragmentWithCachedData();
                            break;
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        bubbleNavigationLinearView.setNavigationChangeListener(new BubbleNavigationChangeListener() {
            @Override
            public void onNavigationChanged(View view, int position) {
                viewPager.setCurrentItem(position, true);
            }
        });

        this.relative_layout_home_activity_search_section =  (RelativeLayout) findViewById(R.id.relative_layout_home_activity_search_section);
        this.edit_text_home_activity_search =  (EditText) findViewById(R.id.edit_text_home_activity_search);
        this.image_view_activity_home_close_search =  (ImageView) findViewById(R.id.image_view_activity_home_close_search);
        this.image_view_activity_actors_back =  (ImageView) findViewById(R.id.image_view_activity_actors_back);
        this.image_view_activity_home_search =  (ImageView) findViewById(R.id.image_view_activity_home_search);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);



        MenuItem item = menu.findItem(R.id.action_search);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),
                menu,
                R.id.media_route_menu_item);
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            viewPager.setCurrentItem(0);
        }else if(id == R.id.login){
            Intent intent= new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up, R.anim.slide_down);

            FromLogin=true;

        }else if (id == R.id.nav_exit) {
            final PrefManager prf = new PrefManager(getApplicationContext());
            if (prf.getString("NOT_RATE_APP").equals("TRUE")) {
                super.onBackPressed();
            } else {
                rateDialog(true);
            }
        }
        else if (id == R.id.my_password) {
            PrefManager prf= new PrefManager(getApplicationContext());
            if (prf.getString("LOGGED").toString().equals("TRUE")){
                Intent intent  =  new Intent(getApplicationContext(), PasswordActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
            }else{
                Intent intent= new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
                FromLogin=true;
            }
        }else if (id == R.id.nav_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.enter, R.anim.exit);
        }else if (id==R.id.my_profile){
            PrefManager prf= new PrefManager(getApplicationContext());
            if (prf.getString("LOGGED").toString().equals("TRUE")){
                Intent intent  =  new Intent(getApplicationContext(), EditActivity.class);
                intent.putExtra("id", Integer.parseInt(prf.getString("ID_USER")));
                intent.putExtra("image",prf.getString("IMAGE_USER").toString());
                intent.putExtra("name",prf.getString("NAME_USER").toString());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up, R.anim.slide_down);

            }else{
                Intent intent= new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
                FromLogin=true;
            }
        }else if (id==R.id.logout){
            logout();
        }else if (id ==  R.id.my_list){
            Intent intent= new Intent(HomeActivity.this, MyListActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
        }
        else if (id==R.id.nav_share){
			//startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MyApi.API_URL)));
            //final String appPackageName=getApplication().getPackageName();
            String shareBody = "https://raw.githubusercontent.com/MovieAddict88/movie-api/main/free_movie_api.json";
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT,  getString(R.string.app_name));
            startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.app_name)));
        }else if (id == R.id.nav_rate) {
            rateDialog(false);
        }else if (id == R.id.nav_help){
            Intent intent= new Intent(HomeActivity.this, SupportActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up, R.anim.slide_down);

        } else if (id == R.id.nav_policy  ){
            Intent intent = new Intent(getApplicationContext(), PolicyActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.enter, R.anim.exit);
        }else if (id == R.id.buy_now){
            showDialog();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public      void logout(){
        PrefManager prf= new PrefManager(getApplicationContext());
        prf.remove("ID_USER");
        prf.remove("SALT_USER");
        prf.remove("TOKEN_USER");
        prf.remove("NAME_USER");
        prf.remove("TYPE_USER");
        prf.remove("USERN_USER");
        prf.remove("IMAGE_USER");
        prf.remove("LOGGED");
        prf.remove("NEW_SUBSCRIBE_ENABLED");
        if (prf.getString("LOGGED").toString().equals("TRUE")){
            text_view_name_nave_header.setText(prf.getString("NAME_USER").toString());
            Picasso.with(getApplicationContext()).load(prf.getString("IMAGE_USER").toString()).placeholder(R.drawable.placeholder_profile).error(R.drawable.placeholder_profile).resize(200,200).centerCrop().into(circle_image_view_profile_nav_header);
            if (prf.getString("TYPE_USER").toString().equals("google")){
            }else {
            }
        }else{
            Menu nav_Menu = navigationView.getMenu();
            nav_Menu.findItem(R.id.my_profile).setVisible(false);
            nav_Menu.findItem(R.id.my_password).setVisible(false);
            nav_Menu.findItem(R.id.logout).setVisible(false);
            nav_Menu.findItem(R.id.my_list).setVisible(false);
            nav_Menu.findItem(R.id.login).setVisible(true);
            text_view_name_nave_header.setText(getResources().getString(R.string.please_login));
            Picasso.with(getApplicationContext()).load(R.drawable.placeholder_profile).placeholder(R.drawable.placeholder_profile).error(R.drawable.placeholder_profile).resize(200,200).centerCrop().into(circle_image_view_profile_nav_header);
        }

        if (prf.getString("APP_LOGIN_REQUIRED").toString().equals("TRUE")) {
            Intent intent= new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
            finish();
        }
        Menu nav_Menu = navigationView.getMenu();

        if (checkSUBSCRIBED()){
            nav_Menu.findItem(R.id.buy_now).setVisible(false);
        }else{
            nav_Menu.findItem(R.id.buy_now).setVisible(true);

        }
        image_view_profile_nav_header_bg.setVisibility(View.GONE);
        Toasty.info(getApplicationContext(),getString(R.string.message_logout),Toast.LENGTH_LONG).show();
    }
    class ViewPagerAdapter extends FragmentPagerAdapter {

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment) {
            mFragmentList.add(fragment);
        }

    }
    private void firebaseSubscribe() {
        FirebaseMessaging.getInstance().subscribeToTopic("CineMax")
                .addOnCompleteListener(new OnCompleteListener<Void>() {

                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Don't call old API for device registration
                        // Retrofit retrofit = apiClient.getClient();
                        // apiRest service = retrofit.create(apiRest.class);
                        // String unique_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),Settings.Secure.ANDROID_ID);

                        // Call<ApiResponse> call = service.addDevice(unique_id);
                        // call.enqueue(new Callback<ApiResponse>() {
                        //     @Override
                        //     public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        //         if (response.isSuccessful())
                        //             Log.v("HomeActivity","Added : "+response.body().getMessage());
                        //     }

                        //     @Override
                        //     public void onFailure(Call<ApiResponse> call, Throwable t) {
                        //         Log.v("HomeActivity","onFailure : "+ t.getMessage().toString());
                        //     }
                        // });
                    }
                });

    }
    private static final String TAG ="MainActivity ----- : " ;

    private void initGDPR() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        ConsentInformation consentInformation =
                ConsentInformation.getInstance(HomeActivity.this);
//// test
/////
        String[] publisherIds = {getResources().getString(R.string.publisher_id)};
        consentInformation.requestConsentInfoUpdate(publisherIds, new
                ConsentInfoUpdateListener() {
                    @Override
                    public void onConsentInfoUpdated(ConsentStatus consentStatus) {
// User's consent status successfully updated.
                        Log.d(TAG,"onConsentInfoUpdated");
                        switch (consentStatus){
                            case PERSONALIZED:
                                Log.d(TAG,"PERSONALIZED");
                                ConsentInformation.getInstance(HomeActivity.this)
                                        .setConsentStatus(ConsentStatus.PERSONALIZED);
                                break;
                            case NON_PERSONALIZED:
                                Log.d(TAG,"NON_PERSONALIZED");
                                ConsentInformation.getInstance(HomeActivity.this)
                                        .setConsentStatus(ConsentStatus.NON_PERSONALIZED);
                                break;


                            case UNKNOWN:
                                Log.d(TAG,"UNKNOWN");
                                if
                                (ConsentInformation.getInstance(HomeActivity.this).isRequestLocationInEeaOrUnknown
                                        ()){
                                    URL privacyUrl = null;
                                    try {
// TODO: Replace with your app's privacy policy URL.
                                        privacyUrl = new URL(Actress.actress.replace("/api/","/privacy_policy.html"));

                                    } catch (MalformedURLException e) {
                                        e.printStackTrace();
// Handle error.

                                    }
                                    form = new ConsentForm.Builder(HomeActivity.this,
                                            privacyUrl)
                                            .withListener(new ConsentFormListener() {
                                                @Override
                                                public void onConsentFormLoaded() {
                                                    Log.d(TAG,"onConsentFormLoaded");
                                                    showform();
                                                }
                                                @Override
                                                public void onConsentFormOpened() {
                                                    Log.d(TAG,"onConsentFormOpened");
                                                }
                                                @Override
                                                public void onConsentFormClosed( ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                                                    Log.d(TAG,"onConsentFormClosed");
                                                }
                                                @Override
                                                public void onConsentFormError(String errorDescription) {
                                                    Log.d(TAG,"onConsentFormError");
                                                    Log.d(TAG,errorDescription);
                                                }
                                            })
                                            .withPersonalizedAdsOption()
                                            .withNonPersonalizedAdsOption()
                                            .build();
                                    form.load();
                                } else {
                                    Log.d(TAG,"PERSONALIZED else");
                                    ConsentInformation.getInstance(HomeActivity.this).setConsentStatus(ConsentStatus.PERSONALIZED);
                                    AppLovinPrivacySettings.setHasUserConsent(true, getApplicationContext());

                                }
                                break;
                            default:
                                break;
                        }
                    }
                    @Override
                    public void onFailedToUpdateConsentInfo(String errorDescription) {
// User's consent status failed to update.
                        Log.d(TAG,"onFailedToUpdateConsentInfo");
                        Log.d(TAG,errorDescription);
                    }
                });
    }
    private void showform(){
        if (form!=null){
            Log.d(TAG,"show ok");
            form.show();
        }
    }
    public void rateDialog(final boolean close){
        this.rateDialog = new Dialog(this,R.style.Theme_Dialog);

        rateDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        rateDialog.setCancelable(true);
        rateDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Window window = rateDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        wlp.gravity = Gravity.BOTTOM;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        final   PrefManager prf= new PrefManager(getApplicationContext());
        rateDialog.setCancelable(false);
        rateDialog.setContentView(R.layout.dialog_rating_app);
        final AppCompatRatingBar AppCompatRatingBar_dialog_rating_app=(AppCompatRatingBar) rateDialog.findViewById(R.id.AppCompatRatingBar_dialog_rating_app);
        final LinearLayout linear_layout_feedback=(LinearLayout) rateDialog.findViewById(R.id.linear_layout_feedback);
        final LinearLayout linear_layout_rate=(LinearLayout) rateDialog.findViewById(R.id.linear_layout_rate);
        final Button buttun_send_feedback=(Button) rateDialog.findViewById(R.id.buttun_send_feedback);
        final Button button_later=(Button) rateDialog.findViewById(R.id.button_later);
        final Button button_never=(Button) rateDialog.findViewById(R.id.button_never);
        final Button button_cancel=(Button) rateDialog.findViewById(R.id.button_cancel);
        button_never.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prf.setString("NOT_RATE_APP", "TRUE");
                rateDialog.dismiss();
                if (close)
                    finish();
            }
        });
        button_later.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rateDialog.dismiss();
                if (close)
                    finish();
            }
        });
        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rateDialog.dismiss();
                if (close)
                    finish();
            }
        });
        final EditText edit_text_feed_back=(EditText) rateDialog.findViewById(R.id.edit_text_feed_back);
        buttun_send_feedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prf.setString("NOT_RATE_APP", "TRUE");
                // Don't call old API for rating feedback
                // Retrofit retrofit = apiClient.getClient();
                // apiRest service = retrofit.create(apiRest.class);
                // Call<ApiResponse> call = service.addSupport("Application rating feedback",AppCompatRatingBar_dialog_rating_app.getRating()+" star(s) Rating".toString(),edit_text_feed_back.getText().toString());
                // call.enqueue(new Callback<ApiResponse>() {
                //     @Override
                //     public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                //         if(response.isSuccessful()){
                //             Toasty.success(getApplicationContext(), getResources().getString(R.string.rating_done), Toast.LENGTH_SHORT).show();
                //         }else{
                //             Toasty.error(getApplicationContext(), getString(R.string.error_server), Toast.LENGTH_SHORT).show();
                //         }
                //         rateDialog.dismiss();

                //         if (close)
                //             finish();

                //     }
                //     @Override
                //     public void onFailure(Call<ApiResponse> call, Throwable t) {
                //         Toasty.error(getApplicationContext(), getString(R.string.error_server), Toast.LENGTH_SHORT).show();
                //         rateDialog.dismiss();

                //         if (close)
                //             finish();
                //     }
                // });
                
                // Just dismiss the dialog without API call
                Toasty.success(getApplicationContext(), getResources().getString(R.string.rating_done), Toast.LENGTH_SHORT).show();
                rateDialog.dismiss();
                if (close)
                    finish();
            }
        });
        AppCompatRatingBar_dialog_rating_app.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (fromUser){
                    if (rating>3){
                        final String appPackageName = getApplication().getPackageName();
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                        } catch (android.content.ActivityNotFoundException anfe) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
                        }
                        prf.setString("NOT_RATE_APP", "TRUE");
                        rateDialog.dismiss();
                    }else{
                        linear_layout_feedback.setVisibility(View.VISIBLE);
                        linear_layout_rate.setVisibility(View.GONE);
                    }
                }else{

                }
            }
        });
        rateDialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {

                    rateDialog.dismiss();
                    if (close)
                        finish();
                }
                return true;

            }
        });
        rateDialog.show();

    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.action_search :
                edit_text_home_activity_search.requestFocus();
                relative_layout_home_activity_search_section.setVisibility(View.VISIBLE);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onResume() {
        super.onResume();

        // Enhanced resume logic with crash protection
        try {
            Log.d("HomeActivity", "Activity resumed, checking data state");
            
            // Check if data is already loaded, if not load from cache immediately
            if (!dataLoaded) {
                Log.d("HomeActivity", "Data not loaded, initializing data loading in onResume");
                initializeDataLoading();
            } else {
                Log.d("HomeActivity", "Data already loaded, checking cache validity");
                // Check if cache is still valid, refresh if needed
                if (!dataRepository.getSimpleCacheManager().isCacheValid("api_response")) {
                    Log.d("HomeActivity", "Cache expired, refreshing data");
                    scheduleBackgroundRefresh();
                }
            }

            PrefManager prf= new PrefManager(getApplicationContext());
        Menu nav_Menu = navigationView.getMenu();


        if(checkSUBSCRIBED()){
            nav_Menu.findItem(R.id.buy_now).setVisible(false);
        }else{
            nav_Menu.findItem(R.id.buy_now).setVisible(true);
        }
        if (prf.getString("LOGGED").toString().equals("TRUE")){
            nav_Menu.findItem(R.id.my_profile).setVisible(true);
            if (prf.getString("TYPE_USER").toString().equals("email")){
                nav_Menu.findItem(R.id.my_password).setVisible(true);
            }
            nav_Menu.findItem(R.id.logout).setVisible(true);
            nav_Menu.findItem(R.id.my_list).setVisible(true);
            nav_Menu.findItem(R.id.login).setVisible(false);
            text_view_name_nave_header.setText(prf.getString("NAME_USER").toString());
            Picasso.with(getApplicationContext()).load(prf.getString("IMAGE_USER").toString()).placeholder(R.drawable.placeholder_profile).error(R.drawable.placeholder_profile).resize(200,200).centerCrop().into(circle_image_view_profile_nav_header);

            final com.squareup.picasso.Target target = new com.squareup.picasso.Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    BlurImage.with(getApplicationContext()).load(bitmap).intensity(25).Async(true).into(image_view_profile_nav_header_bg);
                }
                @Override
                public void onBitmapFailed(Drawable errorDrawable) { }
                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) { }
            };
            Picasso.with(getApplicationContext()).load(prf.getString("IMAGE_USER").toString()).into(target);
            image_view_profile_nav_header_bg.setTag(target);
            image_view_profile_nav_header_bg.setVisibility(View.VISIBLE);

        }else{
            nav_Menu.findItem(R.id.my_profile).setVisible(false);
            nav_Menu.findItem(R.id.my_password).setVisible(false);
            nav_Menu.findItem(R.id.logout).setVisible(false);
            nav_Menu.findItem(R.id.my_list).setVisible(false);
            nav_Menu.findItem(R.id.login).setVisible(true);
            image_view_profile_nav_header_bg.setVisibility(View.GONE);

            text_view_name_nave_header.setText(getResources().getString(R.string.please_login));
            Picasso.with(getApplicationContext()).load(R.drawable.placeholder_profile).placeholder(R.drawable.placeholder_profile).error(R.drawable.placeholder_profile).resize(200,200).centerCrop().into(circle_image_view_profile_nav_header);
        }
            if (FromLogin){
                FromLogin = false;
            }
            
        } catch (Exception e) {
            Log.e("HomeActivity", "Error in onResume", e);
            // Prevent crash by handling gracefully
            if (!dataLoaded) {
                // Fallback to basic loading
                try {
                    loadAllDataFromJson();
                } catch (Exception ex) {
                    Log.e("HomeActivity", "Fallback loading also failed", ex);
                }
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("HomeActivity", "Activity paused");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("HomeActivity", "Activity stopped");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("HomeActivity", "Activity destroyed, cleaning up resources");
        
        try {
            // Clean up resources to prevent memory leaks
            if (cachedJsonResponse != null) {
                cachedJsonResponse = null;
            }
            
            // Cancel any pending operations
            if (dataRepository != null) {
                // Stop any background operations
            }
            
        } catch (Exception e) {
            Log.e("HomeActivity", "Error in onDestroy cleanup", e);
        }
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w("HomeActivity", "Low memory warning, clearing caches");
        
        try {
            // Clear memory caches when system is low on memory
            if (dataRepository != null && dataRepository.getSimpleCacheManager() != null) {
                dataRepository.getSimpleCacheManager().clearMemoryCache();
            }
            
            // Force garbage collection
            System.gc();
            
        } catch (Exception e) {
            Log.e("HomeActivity", "Error handling low memory", e);
        }
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.d("HomeActivity", "Memory trim requested, level: " + level);
        
        try {
            switch (level) {
                case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
                    // App is in background, trim some memory
                    if (dataRepository != null && dataRepository.getSimpleCacheManager() != null) {
                        dataRepository.getSimpleCacheManager().trimMemory();
                    }
                    break;
                    
                case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                    // Critical memory situation, clear as much as possible
                    if (dataRepository != null && dataRepository.getSimpleCacheManager() != null) {
                        dataRepository.getSimpleCacheManager().clearMemoryCache();
                    }
                    System.gc();
                    break;
            }
        } catch (Exception e) {
            Log.e("HomeActivity", "Error trimming memory", e);
        }
    }

    /**
     * Called when HomeFragment is ready to receive data
     */
    public void onHomeFragmentReady() {
        Log.d("HomeActivity", "HomeFragment is ready, checking for cached data");
        
        // If we have cached data but fragment wasn't ready before, update it now
        if (dataLoaded && cachedJsonResponse != null) {
            new Handler().postDelayed(() -> {
                updateHomeFragmentWithJsonData(cachedJsonResponse);
            }, 100);
        }
    }

    public void goToTV() {
        viewPager.setCurrentItem(3);
    }
    private void getGenreList() {
        // Don't load genres from old API - they will be loaded from JSON data
        // Retrofit retrofit = apiClient.getClient();
        // apiRest service = retrofit.create(apiRest.class);
        // Call<List<Genre>> call = service.getGenreList();
        // call.enqueue(new Callback<List<Genre>>() {
        //     @Override
        //     public void onResponse(Call<List<Genre>> call, Response<List<Genre>> response) {

        //     }
        //     @Override
        //     public void onFailure(Call<List<Genre>> call, Throwable t) {
        //     }
        // });
    }
    public void showDialog(){
        this.dialog = new Dialog(this,
                R.style.Theme_Dialog);




        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        wlp.gravity = Gravity.BOTTOM;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        final   PrefManager prf= new PrefManager(getApplicationContext());
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_subscribe);

        CardView card_view_gpay=(CardView) dialog.findViewById(R.id.card_view_gpay);
        CardView card_view_paypal=(CardView) dialog.findViewById(R.id.card_view_paypal);
        CardView card_view_cash=(CardView) dialog.findViewById(R.id.card_view_cash);
        CardView card_view_credit_card=(CardView) dialog.findViewById(R.id.card_view_credit_card);
        LinearLayout payment_methode=(LinearLayout) dialog.findViewById(R.id.payment_methode);
        LinearLayout dialog_content=(LinearLayout) dialog.findViewById(R.id.dialog_content);
        RelativeLayout relative_layout_subscibe_back=(RelativeLayout) dialog.findViewById(R.id.relative_layout_subscibe_back);

        if (prf.getString("APP_STRIPE_ENABLED").toString().equals("FALSE")){
            card_view_credit_card.setVisibility(View.GONE);
        }
        if (prf.getString("APP_PAYPAL_ENABLED").toString().equals("FALSE")){
            card_view_paypal.setVisibility(View.GONE);
        }
        if (prf.getString("APP_CASH_ENABLED").toString().equals("FALSE")){
            card_view_cash.setVisibility(View.GONE);
        }
        if (prf.getString("APP_GPLAY_ENABLED").toString().equals("FALSE")){
            card_view_gpay.setVisibility(View.GONE);
        }
        TextView text_view_go_pro=(TextView) dialog.findViewById(R.id.text_view_go_pro);


        TextView text_view_policy_2=(TextView) dialog.findViewById(R.id.text_view_policy_2);
        TextView text_view_policy=(TextView) dialog.findViewById(R.id.text_view_policy);
        SpannableString content = new SpannableString(getResources().getString(R.string.subscription_policy));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        text_view_policy.setText(content);
        text_view_policy_2.setText(content);


        text_view_policy.setOnClickListener(view -> {
            startActivity(new Intent(HomeActivity.this,RefundActivity.class));
        });
        text_view_policy_2.setOnClickListener(view -> {
            startActivity(new Intent(HomeActivity.this,RefundActivity.class));
        });

        RelativeLayout relative_layout_select_method=(RelativeLayout) dialog.findViewById(R.id.relative_layout_select_method);

        relative_layout_select_method.setOnClickListener(v->{
            if(payment_methode_id.equals("null")) {
                Toasty.error(getApplicationContext(), getResources().getString(R.string.select_payment_method), Toast.LENGTH_LONG).show();
                return;
            }
            switch (payment_methode_id){
                case "gp" :
                    subscribe();
                    dialog.dismiss();
                    break;
                default:
                    PrefManager prf1= new PrefManager(getApplicationContext());
                    if (prf1.getString("LOGGED").toString().equals("TRUE")){
                        Intent intent  =  new Intent(getApplicationContext(), PlansActivity.class);
                        intent.putExtra("method",payment_methode_id);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
                        dialog.dismiss();

                    }else{
                        Intent intent= new Intent(HomeActivity.this, LoginActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
                        FromLogin=true;
                    }
                    dialog.dismiss();
                    break;
            }
        });
        text_view_go_pro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payment_methode.setVisibility(View.VISIBLE);
                dialog_content.setVisibility(View.GONE);
                relative_layout_subscibe_back.setVisibility(View.VISIBLE);
            }
        });

        relative_layout_subscibe_back.setOnClickListener(v->{
            payment_methode.setVisibility(View.GONE);
            dialog_content.setVisibility(View.VISIBLE);
            relative_layout_subscibe_back.setVisibility(View.GONE);
        });
        card_view_gpay.setOnClickListener(v->{
            payment_methode_id="gp";
            card_view_gpay.setCardBackgroundColor(getResources().getColor(R.color.colorAccent));
            card_view_paypal.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
            card_view_cash.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
            card_view_credit_card.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
        });
        card_view_paypal.setOnClickListener(v->{
            payment_methode_id="pp";
            card_view_gpay.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
            card_view_paypal.setCardBackgroundColor(getResources().getColor(R.color.colorAccent));
            card_view_cash.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
            card_view_credit_card.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
        });
        card_view_credit_card.setOnClickListener(v->{
            payment_methode_id="cc";
            card_view_gpay.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
            card_view_paypal.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
            card_view_cash.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
            card_view_credit_card.setCardBackgroundColor(getResources().getColor(R.color.colorAccent));
        });
        card_view_cash.setOnClickListener(v->{
            payment_methode_id="cash";
            card_view_gpay.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
            card_view_paypal.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
            card_view_cash.setCardBackgroundColor(getResources().getColor(R.color.colorAccent));
            card_view_credit_card.setCardBackgroundColor(getResources().getColor(R.color.dark_gray));
        });
        dialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {

                    dialog.dismiss();
                }
                return true;
            }
        });
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            final PrefManager prf = new PrefManager(getApplicationContext());
            if (prf.getString("NOT_RATE_APP").equals("TRUE")) {
                super.onBackPressed();
            } else {
                rateDialog(true);
                return;
            }
        }

    }
    public boolean checkSUBSCRIBED(){
        PrefManager prefManager= new PrefManager(getApplicationContext());
        if (!prefManager.getString("SUBSCRIBED").equals("TRUE") && !prefManager.getString("NEW_SUBSCRIBE_ENABLED").equals("TRUE")) {
            return false;
        }
        return true;
    }
    
    // ===== JSON API INTEGRATION =====
    // These methods will load data from your GitHub JSON file
    
    // Individual loading methods removed to prevent conflicts and instability
    
    /**
     * Get video sources for a movie from JSON API
     */
    public void getMovieVideoSourcesFromJson(int movieId, VideoSourcesCallback callback) {
        apiClient.getMovieVideoSources(movieId, new Callback<JsonApiResponse>() {
            @Override
            public void onResponse(Call<JsonApiResponse> call, Response<JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Get video sources from the JSON response
                    JsonApiResponse.VideoSources videoSources = response.body().getVideoSources();
                    
                    if (videoSources != null) {
                        // Return Big Buck Bunny or Elephants Dream URLs
                        if (videoSources.getBigBuckBunny() != null) {
                            callback.onSuccess(videoSources.getBigBuckBunny().getUrls().getP1080());
                        } else if (videoSources.getElephantsDream() != null) {
                            callback.onSuccess(videoSources.getElephantsDream().getUrls().getP1080());
                        } else {
                            callback.onError("No video sources available");
                        }
                    } else {
                        callback.onError("No video sources found");
                    }
                } else {
                    callback.onError("Failed to load video sources");
                }
            }
            
            @Override
            public void onFailure(Call<JsonApiResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Get live stream URL from JSON API
     */
    public void getLiveStreamFromJson(LiveStreamCallback callback) {
        apiClient.getJsonApiData(new Callback<JsonApiResponse>() {
            @Override
            public void onResponse(Call<JsonApiResponse> call, Response<JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonApiResponse.VideoSources videoSources = response.body().getVideoSources();
                    
                    if (videoSources != null && videoSources.getLiveStreams() != null) {
                        JsonApiResponse.LiveStream liveStream = videoSources.getLiveStreams().getTestHls();
                        if (liveStream != null) {
                            callback.onSuccess(liveStream.getUrl());
                        } else {
                            callback.onError("No live stream available");
                        }
                    } else {
                        callback.onError("No live streams found");
                    }
                } else {
                    callback.onError("Failed to load live streams");
                }
            }
            
            @Override
            public void onFailure(Call<JsonApiResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // Helper methods to update fragments with JSON data
    private void updateHomeFragmentWithJsonData(JsonApiResponse jsonResponse) {
        // Update HomeFragment with the JSON data
        Log.d("JSON_API", "Updating HomeFragment with JSON data");
        
        try {
            // First check if fragments are initialized
            if (mFragmentList == null || mFragmentList.isEmpty()) {
                Log.w("JSON_API", "Fragment list not initialized, waiting...");
                // Retry after a short delay
                new Handler().postDelayed(() -> updateHomeFragmentWithJsonData(jsonResponse), 200);
                return;
            }
            
            // Get the HomeFragment and update it
            if (mFragmentList.size() > 0 && mFragmentList.get(0) instanceof HomeFragment) {
                HomeFragment homeFragment = (HomeFragment) mFragmentList.get(0);
                
                // Check if fragment is added and has a view
                if (homeFragment.isAdded() && homeFragment.getView() != null) {
                    Log.d("JSON_API", "HomeFragment is ready, updating with data");
                    homeFragment.updateWithJsonData(jsonResponse);
                } else {
                    Log.w("JSON_API", "HomeFragment not ready, retrying...");
                    // Retry after fragment is ready
                    new Handler().postDelayed(() -> updateHomeFragmentWithJsonData(jsonResponse), 300);
                }
            } else {
                Log.e("JSON_API", "HomeFragment not found in fragment list");
            }
        } catch (Exception e) {
            Log.e("JSON_API", "Error updating HomeFragment", e);
        }
    }
    
    private void updateHomeFragmentWithCachedData() {
        if (cachedJsonResponse != null && dataLoaded) {
            Log.d("JSON_API", "Updating HomeFragment with cached data");
            updateHomeFragmentWithJsonData(cachedJsonResponse);
        }
    }
    
    private void updateMoviesFragmentWithJsonData(List<Poster> movies) {
        // Update MoviesFragment with the JSON data
        Log.d("JSON_API", "Updating MoviesFragment with " + movies.size() + " movies");
        
        // Get the MoviesFragment and update it
        if (mFragmentList.size() > 1 && mFragmentList.get(1) instanceof MoviesFragment) {
            MoviesFragment moviesFragment = (MoviesFragment) mFragmentList.get(1);
            // Pass the movies data to the fragment
            moviesFragment.updateWithJsonData(movies);
        }
    }
    
    private void updateMoviesFragmentWithCachedData() {
        if (cachedJsonResponse != null && dataLoaded && cachedJsonResponse.getMovies() != null) {
            Log.d("JSON_API", "Updating MoviesFragment with cached data");
            updateMoviesFragmentWithJsonData(cachedJsonResponse.getMovies());
        }
    }
    
    private void updateTvFragmentWithJsonData(List<Channel> channels) {
        // Update TvFragment with the JSON data
        Log.d("JSON_API", "Updating TvFragment with " + channels.size() + " channels");
        
        // Get the TvFragment and update it
        if (mFragmentList.size() > 3 && mFragmentList.get(3) instanceof TvFragment) {
            TvFragment tvFragment = (TvFragment) mFragmentList.get(3);
            // Pass the channels data to the fragment
            tvFragment.updateWithJsonData(channels);
        }
    }
    
    private void updateTvFragmentWithCachedData() {
        if (cachedJsonResponse != null && dataLoaded) {
            Log.d("JSON_API", "Updating TvFragment with cached data");
            // Use home channels for Live TV category (same as Home category)
            if (cachedJsonResponse.getHome() != null && cachedJsonResponse.getHome().getChannels() != null) {
                updateTvFragmentWithJsonData(cachedJsonResponse.getHome().getChannels());
            } else if (cachedJsonResponse.getChannels() != null) {
                // Fallback to main channels if home channels not available
                updateTvFragmentWithJsonData(cachedJsonResponse.getChannels());
            }
        }
    }
    
    private void updateSeriesFragmentWithJsonData(List<Poster> series) {
        // Update SeriesFragment with the JSON data
        Log.d("JSON_API", "Updating SeriesFragment with " + series.size() + " series");
        
        // Get the SeriesFragment and update it
        if (mFragmentList.size() > 2 && mFragmentList.get(2) instanceof SeriesFragment) {
            SeriesFragment seriesFragment = (SeriesFragment) mFragmentList.get(2);
            // Pass the series data to the fragment
            seriesFragment.updateWithJsonData(series);
        }
    }
    
    private void updateSeriesFragmentWithCachedData() {
        if (cachedJsonResponse != null && dataLoaded && cachedJsonResponse.getMovies() != null) {
            // Filter series from movies array
            List<Poster> series = new ArrayList<>();
            for (Poster poster : cachedJsonResponse.getMovies()) {
                if ("series".equals(poster.getType())) {
                    series.add(poster);
                }
            }
            if (!series.isEmpty()) {
                Log.d("JSON_API", "Updating SeriesFragment with cached data");
                updateSeriesFragmentWithJsonData(series);
            }
        }
    }
    
    private void updateAllFragmentsWithCachedData() {
        if (cachedJsonResponse != null && dataLoaded) {
            Log.d("JSON_API", "Updating all fragments with cached data");
            updateHomeFragmentWithCachedData();
            updateMoviesFragmentWithCachedData();
            updateSeriesFragmentWithCachedData();
            updateTvFragmentWithCachedData();
        }
    }
    
    // Callback interfaces for video sources
    public interface VideoSourcesCallback {
        void onSuccess(String videoUrl);
        void onError(String error);
    }
    
    public interface LiveStreamCallback {
        void onSuccess(String streamUrl);
        void onError(String error);
    }
    
    /**
     * Load cached data immediately without waiting for API
     */
    private void loadCachedDataImmediately() {
        Log.d("CACHE_API", "Loading cached data immediately...");
        
        // Try to get data from simple cache first
        JsonApiResponse cachedResponse = dataRepository.getSimpleCacheManager().getApiResponse();
        if (cachedResponse != null && dataRepository.getSimpleCacheManager().isCacheValid("api_response")) {
            Log.d("CACHE_API", "Found valid cached data, displaying immediately");
            handleJsonResponse(cachedResponse, "Cache");
            return;
        }
        
        // Try to get data from legacy cache
        JsonApiResponse legacyCachedResponse = dataRepository.getCacheManager().getCachedApiResponse();
        if (legacyCachedResponse != null && dataRepository.getCacheManager().isCacheValid()) {
            Log.d("CACHE_API", "Found valid legacy cached data, displaying immediately");
            handleJsonResponse(legacyCachedResponse, "Cache");
            return;
        }
        
        // If no cached data, load from API
        Log.d("CACHE_API", "No cached data found, loading from API");
        loadAllDataWithCaching();
    }
    
    /**
     * Preload data in background for future launches
     */
    private void preloadDataInBackground() {
        new Thread(() -> {
            try {
                Log.d("CACHE_API", "Preloading data in background...");
                
                // Check if we need to refresh data
                if (!dataRepository.getSimpleCacheManager().isCacheValid("api_response")) {
                    Log.d("CACHE_API", "Cache expired, refreshing in background");
                    dataRepository.refreshData(new DataRepository.ApiResponseCallback() {
                        @Override
                        public void onSuccess(JsonApiResponse response) {
                            Log.d("CACHE_API", "Background refresh completed successfully");
                        }
                        
                        @Override
                        public void onFromCache(JsonApiResponse response) {
                            Log.d("CACHE_API", "Background refresh returned cached data");
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("CACHE_API", "Background refresh failed: " + error);
                        }
                        
                        @Override
                        public void onLoading() {
                            Log.d("CACHE_API", "Background refresh loading...");
                        }
                    });
                } else {
                    Log.d("CACHE_API", "Cache is still valid, no background refresh needed");
                }
                
            } catch (Exception e) {
                Log.e("CACHE_API", "Error in background preload", e);
            }
        }).start();
    }
    
    /**
     * Ensure data is loaded for a specific page
     */
    private void ensureDataLoadedForPage(int pageIndex) {
        if (!dataLoaded) {
            Log.d("CACHE_API", "Data not loaded for page " + pageIndex + ", loading cached data");
            loadCachedDataImmediately();
        } else {
            Log.d("CACHE_API", "Data already loaded for page " + pageIndex);
        }
    }
    
    /**
     * Refresh data from API (called by fragments)
     */
    public void refreshDataFromApi() {
        Log.d("CACHE_API", "Refreshing data from API...");
        dataLoaded = false; // Reset data loaded flag
        loadAllDataWithCaching();
    }
    
    /**
     * Load all data using new caching system for optimal performance
     */
    private void loadAllDataWithCaching() {
        Log.d("CACHE_API", "Loading data with advanced caching system...");
        
        // Add timeout mechanism to prevent hanging
        new Handler().postDelayed(() -> {
            if (!dataLoaded) {
                Log.w("CACHE_API", "Cache system timeout, falling back to legacy method");
                loadAllDataFromJson();
            }
        }, 10000); // 10 second timeout
        
        // Show cache statistics
        String stats = dataRepository.getSimpleCacheStats();
        Log.d("CACHE_API", "Cache stats: " + stats);
        
        dataRepository.loadAllData(new DataRepository.ApiResponseCallback() {
            @Override
            public void onSuccess(JsonApiResponse response) {
                Log.d("CACHE_API", "Successfully loaded fresh data from API");
                handleJsonResponse(response, "API");
            }
            
            @Override
            public void onFromCache(JsonApiResponse response) {
                Log.d("CACHE_API", "Successfully loaded data from cache");
                handleJsonResponse(response, "Cache");
            }
            
            @Override
            public void onError(String error) {
                Log.e("CACHE_API", "Error loading data: " + error);
                runOnUiThread(() -> {
                    Toasty.error(HomeActivity.this, "Failed to load content: " + error, Toast.LENGTH_SHORT).show();
                    
                    // Fallback to legacy method if cache system fails
                    Log.d("CACHE_API", "Falling back to legacy data loading method");
                    loadAllDataFromJson();
                });
            }
            
            @Override
            public void onLoading() {
                Log.d("CACHE_API", "Loading data...");
            }
        });
    }
    
    /**
     * Handle JSON response from either API or cache
     */
    private void handleJsonResponse(JsonApiResponse jsonResponse, String source) {
        if (jsonResponse == null) {
            Log.e("CACHE_API", "Received null response from " + source);
            return;
        }
        
        runOnUiThread(() -> {
            try {
                // Cache the response for backward compatibility
                cachedJsonResponse = jsonResponse;
                dataLoaded = true;
                
                Log.d("CACHE_API", "Processing " + source + " data...");
                
                // Update all fragments with error handling
                if (jsonResponse.getHome() != null) {
                    updateHomeFragmentWithJsonData(jsonResponse);
                }
                
                if (jsonResponse.getMovies() != null && !jsonResponse.getMovies().isEmpty()) {
                    updateMoviesFragmentWithJsonData(jsonResponse.getMovies());
                    
                    // Filter series from movies array
                    List<Poster> series = new ArrayList<>();
                    for (Poster poster : jsonResponse.getMovies()) {
                        if ("series".equals(poster.getType()) || "serie".equals(poster.getType())) {
                            series.add(poster);
                        }
                    }
                    if (!series.isEmpty()) {
                        updateSeriesFragmentWithJsonData(series);
                    }
                }
                
                // Use channels data for Live TV
                if (jsonResponse.getChannels() != null && !jsonResponse.getChannels().isEmpty()) {
                    updateTvFragmentWithJsonData(jsonResponse.getChannels());
                } else if (jsonResponse.getHome() != null && jsonResponse.getHome().getChannels() != null) {
                    updateTvFragmentWithJsonData(jsonResponse.getHome().getChannels());
                }
                
                Log.d("CACHE_API", "All fragments updated successfully from " + source);
                
                // Show cache info to user (optional)
                if ("Cache".equals(source)) {
                    String stats = dataRepository.getSimpleCacheStats();
                    if (BuildConfig.DEBUG) {
                        Toasty.info(HomeActivity.this, "Loaded from cache: " + stats, Toast.LENGTH_SHORT).show();
                    }
                }
                
            } catch (Exception e) {
                Log.e("CACHE_API", "Error processing " + source + " data", e);
            }
        });
    }
    
    /**
     * Load all data from JSON API when app starts (Legacy method)
     */
    private void loadAllDataFromJson() {
        Log.d("JSON_API", "Loading all data from JSON API...");
        
        // Check if data is already loaded
        if (dataLoaded && cachedJsonResponse != null) {
            Log.d("JSON_API", "Data already loaded, using cached data");
            updateAllFragmentsWithCachedData();
            return;
        }
        
        // Check if we have internet connection
        if (!isNetworkAvailable()) {
            Log.e("JSON_API", "No network connection");
            Toasty.error(HomeActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Load everything in one call to prevent conflicts
        apiClient.getJsonApiData(new apiClient.JsonApiCallback() {
            @Override
            public void onSuccess(JsonApiResponse jsonResponse) {
                if (jsonResponse != null) {
                    Log.d("JSON_API", "Successfully loaded all data");
                    
                    // Cache the response
                    cachedJsonResponse = jsonResponse;
                    dataLoaded = true;
                    
                    try {
                        // Update all fragments at once with error handling
                        if (jsonResponse.getHome() != null) {
                            updateHomeFragmentWithJsonData(jsonResponse);
                        }
                        
                        if (jsonResponse.getMovies() != null && !jsonResponse.getMovies().isEmpty()) {
                            updateMoviesFragmentWithJsonData(jsonResponse.getMovies());
                            
                            // Filter series from movies array
                            List<Poster> series = new ArrayList<>();
                            for (Poster poster : jsonResponse.getMovies()) {
                                if ("series".equals(poster.getType())) {
                                    series.add(poster);
                                }
                            }
                            if (!series.isEmpty()) {
                                updateSeriesFragmentWithJsonData(series);
                            }
                        }
                        
                        // Use home channels for Live TV category (same as Home category)
                        if (jsonResponse.getHome() != null && jsonResponse.getHome().getChannels() != null && !jsonResponse.getHome().getChannels().isEmpty()) {
                            updateTvFragmentWithJsonData(jsonResponse.getHome().getChannels());
                        } else if (jsonResponse.getChannels() != null && !jsonResponse.getChannels().isEmpty()) {
                            // Fallback to main channels if home channels not available
                            updateTvFragmentWithJsonData(jsonResponse.getChannels());
                        }
                        
                        // Show success message
                        Toasty.success(HomeActivity.this, "Content loaded successfully", Toast.LENGTH_SHORT).show();
                        
                    } catch (Exception e) {
                        Log.e("JSON_API", "Error updating fragments: " + e.getMessage());
                        Toasty.error(HomeActivity.this, "Error displaying content", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("JSON_API", "JSON response is null");
                    Toasty.error(HomeActivity.this, "Failed to load content", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e("JSON_API", "Error loading data: " + error);
                Toasty.error(HomeActivity.this, "Network error: " + error, Toast.LENGTH_SHORT).show();
                
                // Retry after 3 seconds
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("JSON_API", "Retrying data load...");
                        loadAllDataFromJson();
                    }
                }, 3000);
            }
        });
    }
    
    // Ads loading method removed to prevent interference

    // Subscription loading method removed to fix compilation issues
    
    /**
     * Check if network is available
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}
