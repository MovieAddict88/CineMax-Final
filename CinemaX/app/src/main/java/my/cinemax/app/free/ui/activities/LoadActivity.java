package my.cinemax.app.free.ui.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.greenfrvr.rubberloader.RubberLoaderView;
import my.cinemax.app.free.Provider.PrefManager;
import my.cinemax.app.free.R;
import my.cinemax.app.free.api.apiClient;
import my.cinemax.app.free.api.apiRest;
import my.cinemax.app.free.entity.ApiResponse;
import my.cinemax.app.free.entity.Channel;
import my.cinemax.app.free.entity.Poster;

import java.util.Timer;
import java.util.TimerTask;
import my.cinemax.app.free.MyApi;

public class LoadActivity extends AppCompatActivity {

    private PrefManager prf;



    private  Integer id;
    private String type;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        int time = 3000;
        Uri data = this.getIntent().getData();
        if (data==null){
            Bundle bundle = getIntent().getExtras() ;
            if (bundle!=null) {
                this.id = bundle.getInt("id");
                this.type = bundle.getString("type");
                time = 2000;
            }
        }else{
            if (data.getPath().contains("/c/share/")){
                this.id=Integer.parseInt(data.getPath().replace("/c/share/","").replace(".html",""));
                this.type = "channel";
                time = 2000;
            }else{
                this.id=Integer.parseInt(data.getPath().replace("/share/","").replace(".html",""));
                this.type = "poster";
                time = 2000;
            }
        }



        prf= new PrefManager(getApplicationContext());
        ( (RubberLoaderView) findViewById(R.id.loader1)).startLoading();
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // If you want to modify a view in your Activity
                LoadActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        checkAccount();

                    }
                });
            }
        }, time);

        prf.setString("ADMIN_REWARDED_ADMOB_ID","");

        prf.setString("ADMIN_INTERSTITIAL_ADMOB_ID","");
        prf.setString("ADMIN_INTERSTITIAL_FACEBOOK_ID","");
        prf.setString("ADMIN_INTERSTITIAL_TYPE","FALSE");
        prf.setInt("ADMIN_INTERSTITIAL_CLICKS",3);

        prf.setString("ADMIN_BANNER_ADMOB_ID","");
        prf.setString("ADMIN_BANNER_FACEBOOK_ID","");
        prf.setString("ADMIN_BANNER_TYPE","FALSE");

        prf.setString("ADMIN_NATIVE_FACEBOOK_ID","");
        prf.setString("ADMIN_NATIVE_ADMOB_ID","");
        prf.setString("ADMIN_NATIVE_LINES","6");
        prf.setString("ADMIN_NATIVE_TYPE","FALSE");
        prf.setString("APP_STRIPE_ENABLED","FALSE");
        prf.setString("APP_PAYPAL_ENABLED","FALSE");
        prf.setString("APP_CASH_ENABLED","FALSE");
        prf.setString("APP_LOGIN_REQUIRED","FALSE");
    }


    private void checkAccount() {

        Integer version = -1;
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (version!=-1){
            // Skip old API call - use JSON configuration instead
            // Integer id_user = 0;
            // if (prf.getString("LOGGED").toString().equals("TRUE")) {
            //     id_user = Integer.parseInt(prf.getString("ID_USER"));
            // }
            // Retrofit retrofit = apiClient.getClient();
            // apiRest service = retrofit.create(apiRest.class);
            // Call<ApiResponse> call = service.check(version,id_user);
            
            // Set default values for ads configuration
            prf.setString("ADMIN_REWARDED_ADMOB_ID", "ca-app-pub-3940256099942544/5224354917");
            prf.setString("ADMIN_INTERSTITIAL_ADMOB_ID", "ca-app-pub-3940256099942544/1033173712");
            prf.setString("ADMIN_INTERSTITIAL_FACEBOOK_ID", "IMG_16_9_APP_INSTALL#YOUR_PLACEMENT_ID");
            prf.setString("ADMIN_INTERSTITIAL_TYPE", "ADMOB");
            prf.setInt("ADMIN_INTERSTITIAL_CLICKS", 3);
            prf.setString("ADMIN_BANNER_ADMOB_ID", "ca-app-pub-3940256099942544/6300978111");
            prf.setString("ADMIN_BANNER_FACEBOOK_ID", "IMG_16_9_APP_INSTALL#YOUR_PLACEMENT_ID");
            prf.setString("ADMIN_BANNER_TYPE", "ADMOB");
            prf.setString("ADMIN_NATIVE_FACEBOOK_ID", "IMG_16_9_APP_INSTALL#YOUR_PLACEMENT_ID");
            prf.setString("ADMIN_NATIVE_ADMOB_ID", "ca-app-pub-3940256099942544/2247696110");
            prf.setString("ADMIN_NATIVE_LINES", "6");
            prf.setString("ADMIN_NATIVE_TYPE", "ADMOB");
            prf.setString("APP_CURRENCY", "USD");
            prf.setString("APP_CASH_ACCOUNT", "test@example.com");
            prf.setString("APP_STRIPE_PUBLIC_KEY", "pk_test_51H1234567890");
            prf.setString("APP_CASH_ENABLED", "FALSE");
            prf.setString("APP_PAYPAL_ENABLED", "FALSE");
            prf.setString("APP_STRIPE_ENABLED", "FALSE");
            prf.setString("APP_LOGIN_REQUIRED", "FALSE");
            prf.setString("NEW_SUBSCRIBE_ENABLED", "FALSE");
            
            // Load ads configuration from GitHub API instead of old API
            apiClient.loadAdsConfigAndUpdatePrefs(this, new apiClient.AdsConfigCallback() {
                @Override
                public void onSuccess(String message) {
                    // Ads configuration has been loaded and applied automatically from ads_config.json
                    // Configuration is now stored in PrefManager
                    
                    // Navigate to appropriate activity
                    if (id != null && type != null) {
                        if (type.equals("poster"))
                            getPoster();
                        else if (type.equals("channel"))
                            getChannel();
                    } else {
                        if (!prf.getString("first").equals("true")) {
                            Intent intent = new Intent(LoadActivity.this, IntroActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.enter, R.anim.exit);
                            finish();
                            prf.setString("first", "true");
                        } else {
                            Intent intent = new Intent(LoadActivity.this, HomeActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.enter, R.anim.exit);
                            finish();
                        }
                    }
                }
                
                @Override
                public void onError(String error) {
                    // Even if ads config fails, continue with app flow
                    if (id != null && type != null) {
                        if (type.equals("poster"))
                            getPoster();
                        else if (type.equals("channel"))
                            getChannel();
                    } else {
                        if (!prf.getString("first").equals("true")) {
                            Intent intent = new Intent(LoadActivity.this, IntroActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.enter, R.anim.exit);
                            finish();
                            prf.setString("first", "true");
                        } else {
                            Intent intent = new Intent(LoadActivity.this, HomeActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.enter, R.anim.exit);
                            finish();
                        }
                    }
                }
            });

        }else{

            if (id!=null && type !=null){
                if (type.equals("poster"))
                    getPoster();
                if (type.equals("channel"))
                    getChannel();
            }else{
                if (!prf.getString("first").equals("true")){
                    Intent intent = new Intent(LoadActivity.this,IntroActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter, R.anim.exit);
                    finish();
                    prf.setString("first","true");
                }else{
                    Intent intent = new Intent(LoadActivity.this,HomeActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter, R.anim.exit);
                    finish();
                }
            }
        }

    }




    public void getPoster(){
        // Use GitHub JSON API to get movie/series by ID
        apiClient.getJsonApiData(new retrofit2.Callback<my.cinemax.app.free.entity.JsonApiResponse>() {
            @Override
            public void onResponse(Call<my.cinemax.app.free.entity.JsonApiResponse> call, Response<my.cinemax.app.free.entity.JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    my.cinemax.app.free.entity.JsonApiResponse apiResponse = response.body();
                    
                    // Search for the poster/movie by ID
                    if (apiResponse.getMovies() != null) {
                        for (Poster poster : apiResponse.getMovies()) {
                            if (poster.getId() == id) {
                                if (poster.getType().equals("serie") || poster.getType().equals("series")) {
                                    Intent in = new Intent(LoadActivity.this, SerieActivity.class);
                                    in.putExtra("poster", poster);
                                    in.putExtra("from", "true");
                                    startActivity(in);
                                    finish();
                                    return;
                                } else if (poster.getType().equals("movie")) {
                                    Intent in = new Intent(LoadActivity.this, MovieActivity.class);
                                    in.putExtra("poster", poster);
                                    in.putExtra("from", "true");
                                    startActivity(in);
                                    finish();
                                    return;
                                }
                            }
                        }
                    }
                    
                    // If not found in movies, try featured movies in home section
                    if (apiResponse.getHome() != null && apiResponse.getHome().getFeaturedMovies() != null) {
                        for (Poster poster : apiResponse.getHome().getFeaturedMovies()) {
                            if (poster.getId() == id) {
                                if (poster.getType().equals("serie") || poster.getType().equals("series")) {
                                    Intent in = new Intent(LoadActivity.this, SerieActivity.class);
                                    in.putExtra("poster", poster);
                                    in.putExtra("from", "true");
                                    startActivity(in);
                                    finish();
                                    return;
                                } else if (poster.getType().equals("movie")) {
                                    Intent in = new Intent(LoadActivity.this, MovieActivity.class);
                                    in.putExtra("poster", poster);
                                    in.putExtra("from", "true");
                                    startActivity(in);
                                    finish();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<my.cinemax.app.free.entity.JsonApiResponse> call, Throwable t) {
                // Handle error
            }
        });
    }
    public void getChannel(){
        // Use GitHub JSON API to get channel by ID
        apiClient.getJsonApiData(new retrofit2.Callback<my.cinemax.app.free.entity.JsonApiResponse>() {
            @Override
            public void onResponse(Call<my.cinemax.app.free.entity.JsonApiResponse> call, Response<my.cinemax.app.free.entity.JsonApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    my.cinemax.app.free.entity.JsonApiResponse apiResponse = response.body();
                    
                    // Search for the channel by ID
                    if (apiResponse.getChannels() != null) {
                        for (Channel channel : apiResponse.getChannels()) {
                            if (channel.getId() == id) {
                                Intent in = new Intent(LoadActivity.this, ChannelActivity.class);
                                in.putExtra("channel", channel);
                                in.putExtra("from", "true");
                                startActivity(in);
                                finish();
                                return;
                            }
                        }
                    }
                    
                    // If not found in main channels, try channels in home section
                    if (apiResponse.getHome() != null && apiResponse.getHome().getChannels() != null) {
                        for (Channel channel : apiResponse.getHome().getChannels()) {
                            if (channel.getId() == id) {
                                Intent in = new Intent(LoadActivity.this, ChannelActivity.class);
                                in.putExtra("channel", channel);
                                in.putExtra("from", "true");
                                startActivity(in);
                                finish();
                                return;
                            }
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<my.cinemax.app.free.entity.JsonApiResponse> call, Throwable t) {
                // Handle error
            }
        });
    }

}
