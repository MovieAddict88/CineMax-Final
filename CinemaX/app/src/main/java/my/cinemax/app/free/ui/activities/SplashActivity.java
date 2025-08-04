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


import com.congle7997.google_iap.BillingSubs;
import com.congle7997.google_iap.CallBackCheck;
import com.greenfrvr.rubberloader.RubberLoaderView;
import my.cinemax.app.free.Provider.PrefManager;
import my.cinemax.app.free.R;
import my.cinemax.app.free.api.apiClient;
import my.cinemax.app.free.api.apiRest;
import my.cinemax.app.free.config.Global;
import my.cinemax.app.free.entity.ApiResponse;
import my.cinemax.app.free.entity.JsonApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import my.cinemax.app.free.*;

public class SplashActivity extends AppCompatActivity {

    private PrefManager prf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        check();
        prf= new PrefManager(getApplicationContext());
        ( (RubberLoaderView) findViewById(R.id.loader1)).startLoading();
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // If you want to modify a view in your Activity
                SplashActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        checkAccount();


                    }
                });
            }
        }, 3000);

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
        prf.setString("APP_PAYPAL_CLIENT_ID","");
        prf.setString("APP_CASH_ENABLED","FALSE");
        prf.setString("APP_LOGIN_REQUIRED","FALSE");

    }

    public void check(){
        List<String> listSkuStoreSubs = new ArrayList<>();
        listSkuStoreSubs.add(Global.SUBSCRIPTION_ID);
        new BillingSubs(SplashActivity.this, listSkuStoreSubs, new CallBackCheck() {
            @Override
            public void onPurchase() {
                PrefManager prefManager= new PrefManager(getApplicationContext());
                prefManager.setString("SUBSCRIBED","TRUE");
            }

            @Override
            public void onNotPurchase() {
                PrefManager prefManager= new PrefManager(getApplicationContext());
                prefManager.setString("SUBSCRIBED","FALSE");
            }
        });
    }
    private void checkAccount() {

        // ===== SKIP OLD API CALL AND LOAD FROM JSON INSTEAD =====
        // The old API server is no longer working, so we'll skip the check
        // and optionally load ad configurations from JSON
        
        // Load ad configurations from JSON (optional)
        loadAdsConfigFromJson();
        
        // Go directly to main screen
        redirect();
    }
    
    private void loadAdsConfigFromJson() {
        // Load ad configurations from your GitHub JSON file
        apiClient.loadAdsConfigAndUpdatePrefs(this, new apiClient.AdsConfigCallback() {
            @Override
            public void onSuccess(String message) {
                // Successfully loaded ad config from JSON
                // The PrefManager has been updated with new ad IDs
            }
            
            @Override
            public void onError(String error) {
                // Failed to load ad config, use default values
                // Default ad IDs are already set in onCreate()
            }
        });
    }



    public void redirect(){
        if (!prf.getString("first").equals("true")) {
            Intent intent = new Intent(SplashActivity.this,IntroActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.enter, R.anim.exit);
            finish();
            prf.setString("first","true");
        }else{
            if (prf.getString("APP_LOGIN_REQUIRED").toString().equals("TRUE")){
                if (prf.getString("LOGGED").toString().equals("TRUE")){
                    Intent intent = new Intent(SplashActivity.this,HomeActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter, R.anim.exit);
                    finish();
                }else{
                    Intent intent= new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
                    finish();
                }
            }else{
                Intent intent = new Intent(SplashActivity.this,HomeActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter, R.anim.exit);
                finish();
            }
        }

    }



}
