package my.cinemax.app.free.Utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.orhanobut.hawk.Hawk;
import my.cinemax.app.free.entity.DownloadItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadProgressManager {
    private static final String TAG = "DownloadProgressManager";
    public static final String DOWNLOAD_PROGRESS_UPDATE = "download_progress_update";
    
    private Context context;
    private Handler handler;
    private Runnable progressRunnable;
    private boolean isPolling = false;
    private Map<Long, DownloadItem> activeDownloads = new HashMap<>();
    private Map<Long, Long> lastDownloadedBytes = new HashMap<>();
    private Map<Long, Long> lastUpdateTime = new HashMap<>();
    
    public DownloadProgressManager(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        setupProgressRunnable();
        registerDownloadCompleteReceiver();
    }
    
    private void setupProgressRunnable() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPolling) {
                    pollDownloadProgress();
                    handler.postDelayed(this, 500); // Poll every 500ms for more responsive updates
                }
            }
        };
    }
    
    public void startPolling() {
        if (!isPolling) {
            isPolling = true;
            loadActiveDownloads();
            handler.post(progressRunnable);
            Log.d(TAG, "Started polling for download progress");
        }
    }
    
    public void stopPolling() {
        isPolling = false;
        handler.removeCallbacks(progressRunnable);
        Log.d(TAG, "Stopped polling for download progress");
    }
    
    private void loadActiveDownloads() {
        List<DownloadItem> tempDownloads = Hawk.get("my_downloads_temp");
        if (tempDownloads != null) {
            activeDownloads.clear();
            lastDownloadedBytes.clear();
            lastUpdateTime.clear();
            for (DownloadItem item : tempDownloads) {
                if (item.getDownloadid() != null) {
                    activeDownloads.put(item.getDownloadid(), item);
                    lastDownloadedBytes.put(item.getDownloadid(), 0L);
                    lastUpdateTime.put(item.getDownloadid(), System.currentTimeMillis());
                }
            }
        }
    }
    
    private void pollDownloadProgress() {
        if (activeDownloads.isEmpty()) {
            return;
        }
        
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        
        for (Map.Entry<Long, DownloadItem> entry : activeDownloads.entrySet()) {
            long downloadId = entry.getKey();
            DownloadItem downloadItem = entry.getValue();
            
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            
            try (Cursor cursor = downloadManager.query(query)) {
                if (cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int progressIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int totalSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    
                    int status = cursor.getInt(statusIndex);
                    long downloadedBytes = cursor.getLong(progressIndex);
                    long totalBytes = cursor.getLong(totalSizeIndex);
                    
                    int progress = 0;
                    if (totalBytes > 0) {
                        progress = (int) ((downloadedBytes * 100) / totalBytes);
                    }
                    
                    // Update download item with current progress
                    downloadItem.setProgress(progress);
                    downloadItem.setDownloadedBytes(downloadedBytes);
                    downloadItem.setTotalBytes(totalBytes);
                    downloadItem.setDownloading(true);
                    
                    // Calculate and store ETA information (optional enhancement)
                    String eta = calculateETA(downloadId, downloadedBytes, totalBytes);
                    
                    // Store current values for ETA calculation
                    lastDownloadedBytes.put(downloadId, downloadedBytes);
                    lastUpdateTime.put(downloadId, System.currentTimeMillis());
                    
                    // Send progress update
                    sendProgressUpdate(downloadItem, status, progress);
                    
                    // Check if download is complete
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        Log.d(TAG, "Download completed: " + downloadItem.getTitle());
                        activeDownloads.remove(downloadId);
                        lastDownloadedBytes.remove(downloadId);
                        lastUpdateTime.remove(downloadId);
                        moveToCompletedDownloads(downloadItem);
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        Log.d(TAG, "Download failed: " + downloadItem.getTitle());
                        activeDownloads.remove(downloadId);
                        lastDownloadedBytes.remove(downloadId);
                        lastUpdateTime.remove(downloadId);
                        sendDownloadFailed(downloadItem);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error polling download progress", e);
            }
        }
        
        // Update temp downloads list
        updateTempDownloads();
    }
    
    private String calculateETA(long downloadId, long downloadedBytes, long totalBytes) {
        Long lastBytes = lastDownloadedBytes.get(downloadId);
        Long lastTime = lastUpdateTime.get(downloadId);
        
        if (lastBytes == null || lastTime == null || totalBytes <= 0) {
            return "";
        }
        
        long bytesDiff = downloadedBytes - lastBytes;
        long timeDiff = System.currentTimeMillis() - lastTime;
        
        if (timeDiff <= 0 || bytesDiff <= 0) {
            return "";
        }
        
        long downloadSpeed = (bytesDiff * 1000) / timeDiff; // bytes per second
        long remainingBytes = totalBytes - downloadedBytes;
        long remainingSeconds = remainingBytes / downloadSpeed;
        
        if (remainingSeconds < 60) {
            return remainingSeconds + "s";
        } else if (remainingSeconds < 3600) {
            return (remainingSeconds / 60) + "m " + (remainingSeconds % 60) + "s";
        } else {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
    
    private void sendProgressUpdate(DownloadItem downloadItem, int status, int progress) {
        Intent intent = new Intent(DOWNLOAD_PROGRESS_UPDATE);
        intent.putExtra("action", "progress_update");
        intent.putExtra("downloadId", downloadItem.getDownloadid());
        intent.putExtra("title", downloadItem.getTitle());
        intent.putExtra("progress", progress);
        intent.putExtra("status", status);
        intent.putExtra("downloadedBytes", downloadItem.getDownloadedBytes());
        intent.putExtra("totalBytes", downloadItem.getTotalBytes());
        intent.putExtra("type", downloadItem.getType());
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    
    private void sendDownloadFailed(DownloadItem downloadItem) {
        Intent intent = new Intent(DOWNLOAD_PROGRESS_UPDATE);
        intent.putExtra("action", "download_failed");
        intent.putExtra("downloadId", downloadItem.getDownloadid());
        intent.putExtra("title", downloadItem.getTitle());
        intent.putExtra("type", downloadItem.getType());
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    
    private void moveToCompletedDownloads(DownloadItem downloadItem) {
        // Remove from temp list
        List<DownloadItem> tempDownloads = Hawk.get("my_downloads_temp");
        if (tempDownloads != null) {
            tempDownloads.removeIf(item -> item.getDownloadid().equals(downloadItem.getDownloadid()));
            Hawk.put("my_downloads_temp", tempDownloads);
        }
        
        // Add to completed downloads list
        List<DownloadItem> completedDownloads = Hawk.get("my_downloads_list");
        if (completedDownloads == null) {
            completedDownloads = new ArrayList<>();
        }
        
        // Remove if already exists
        completedDownloads.removeIf(item -> item.getId().equals(downloadItem.getId()));
        
        // Add the completed download
        completedDownloads.add(downloadItem);
        Hawk.put("my_downloads_list", completedDownloads);
        
        // Send completion broadcast
        Intent intent = new Intent(DOWNLOAD_PROGRESS_UPDATE);
        intent.putExtra("action", "download_completed");
        intent.putExtra("downloadId", downloadItem.getDownloadid());
        intent.putExtra("title", downloadItem.getTitle());
        intent.putExtra("type", downloadItem.getType());
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    
    private void updateTempDownloads() {
        List<DownloadItem> tempDownloads = new ArrayList<>(activeDownloads.values());
        Hawk.put("my_downloads_temp", tempDownloads);
    }
    
    private void registerDownloadCompleteReceiver() {
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId != -1 && activeDownloads.containsKey(downloadId)) {
                    DownloadItem downloadItem = activeDownloads.get(downloadId);
                    activeDownloads.remove(downloadId);
                    lastDownloadedBytes.remove(downloadId);
                    lastUpdateTime.remove(downloadId);
                    moveToCompletedDownloads(downloadItem);
                }
            }
        };
        context.registerReceiver(receiver, filter);
    }
    
    public void addActiveDownload(DownloadItem downloadItem) {
        if (downloadItem.getDownloadid() != null) {
            activeDownloads.put(downloadItem.getDownloadid(), downloadItem);
            lastDownloadedBytes.put(downloadItem.getDownloadid(), 0L);
            lastUpdateTime.put(downloadItem.getDownloadid(), System.currentTimeMillis());
            updateTempDownloads();
        }
    }
    
    public void cleanup() {
        stopPolling();
        activeDownloads.clear();
        lastDownloadedBytes.clear();
        lastUpdateTime.clear();
    }
}