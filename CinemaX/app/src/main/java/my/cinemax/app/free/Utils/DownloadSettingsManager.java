package my.cinemax.app.free.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.orhanobut.hawk.Hawk;

public class DownloadSettingsManager {
    private static final String PREF_NAME = "download_settings";
    private static final String KEY_DOWNLOAD_PATH = "download_path";
    private static final String KEY_MAX_CONCURRENT_DOWNLOADS = "max_concurrent_downloads";
    private static final String KEY_WIFI_ONLY = "wifi_only";
    private static final String KEY_AUTO_DELETE_FAILED = "auto_delete_failed";
    private static final String KEY_SHOW_SPEED = "show_speed";
    private static final String KEY_SHOW_ETA = "show_eta";
    
    private Context context;
    private SharedPreferences preferences;
    
    public DownloadSettingsManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public String getDownloadPath() {
        return preferences.getString(KEY_DOWNLOAD_PATH, 
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
    }
    
    public void setDownloadPath(String path) {
        preferences.edit().putString(KEY_DOWNLOAD_PATH, path).apply();
    }
    
    public int getMaxConcurrentDownloads() {
        return preferences.getInt(KEY_MAX_CONCURRENT_DOWNLOADS, 3);
    }
    
    public void setMaxConcurrentDownloads(int max) {
        preferences.edit().putInt(KEY_MAX_CONCURRENT_DOWNLOADS, max).apply();
    }
    
    public boolean isWifiOnly() {
        return preferences.getBoolean(KEY_WIFI_ONLY, false);
    }
    
    public void setWifiOnly(boolean wifiOnly) {
        preferences.edit().putBoolean(KEY_WIFI_ONLY, wifiOnly).apply();
    }
    
    public boolean isAutoDeleteFailed() {
        return preferences.getBoolean(KEY_AUTO_DELETE_FAILED, true);
    }
    
    public void setAutoDeleteFailed(boolean autoDelete) {
        preferences.edit().putBoolean(KEY_AUTO_DELETE_FAILED, autoDelete).apply();
    }
    
    public boolean isShowSpeed() {
        return preferences.getBoolean(KEY_SHOW_SPEED, true);
    }
    
    public void setShowSpeed(boolean showSpeed) {
        preferences.edit().putBoolean(KEY_SHOW_SPEED, showSpeed).apply();
    }
    
    public boolean isShowETA() {
        return preferences.getBoolean(KEY_SHOW_ETA, true);
    }
    
    public void setShowETA(boolean showETA) {
        preferences.edit().putBoolean(KEY_SHOW_ETA, showETA).apply();
    }
    
    public void resetToDefaults() {
        preferences.edit().clear().apply();
    }
    
    public long getTotalDownloadedBytes() {
        return Hawk.get("total_downloaded_bytes", 0L);
    }
    
    public void addDownloadedBytes(long bytes) {
        long total = getTotalDownloadedBytes() + bytes;
        Hawk.put("total_downloaded_bytes", total);
    }
    
    public void resetDownloadStats() {
        Hawk.put("total_downloaded_bytes", 0L);
    }
}