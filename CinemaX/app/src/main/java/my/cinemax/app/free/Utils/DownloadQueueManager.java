package my.cinemax.app.free.Utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.orhanobut.hawk.Hawk;
import my.cinemax.app.free.entity.DownloadItem;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class DownloadQueueManager {
    private static final String TAG = "DownloadQueueManager";
    private static final int MAX_CONCURRENT_DOWNLOADS = 3;
    
    private Context context;
    private DownloadManager downloadManager;
    private Queue<DownloadRequest> downloadQueue = new LinkedList<>();
    private List<Long> activeDownloadIds = new ArrayList<>();
    private DownloadProgressManager progressManager;
    
    public DownloadQueueManager(Context context) {
        this.context = context;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.progressManager = new DownloadProgressManager(context);
    }
    
    public void addDownload(DownloadItem downloadItem, String url, String fileName) {
        DownloadRequest request = new DownloadRequest(downloadItem, url, fileName);
        downloadQueue.offer(request);
        processQueue();
    }
    
    private void processQueue() {
        while (!downloadQueue.isEmpty() && activeDownloadIds.size() < MAX_CONCURRENT_DOWNLOADS) {
            DownloadRequest request = downloadQueue.poll();
            if (request != null) {
                startDownload(request);
            }
        }
    }
    
    private void startDownload(DownloadRequest request) {
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadDir, request.fileName);
            
            DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(request.url))
                    .setTitle(request.downloadItem.getTitle())
                    .setDescription("Downloading " + request.downloadItem.getTitle())
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setVisibleInDownloadsUi(true)
                    .setDestinationUri(Uri.fromFile(file));
            
            long downloadId = downloadManager.enqueue(downloadRequest);
            request.downloadItem.setDownloadid(downloadId);
            activeDownloadIds.add(downloadId);
            
            // Add to progress manager
            progressManager.addActiveDownload(request.downloadItem);
            
            // Save to temp downloads
            saveToTempDownloads(request.downloadItem);
            
            Log.d(TAG, "Started download: " + request.downloadItem.getTitle() + " (ID: " + downloadId + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting download: " + request.downloadItem.getTitle(), e);
        }
    }
    
    private void saveToTempDownloads(DownloadItem downloadItem) {
        List<DownloadItem> tempDownloads = Hawk.get("my_downloads_temp");
        if (tempDownloads == null) {
            tempDownloads = new ArrayList<>();
        }
        
        // Remove if already exists
        tempDownloads.removeIf(item -> item.getId().equals(downloadItem.getId()));
        
        // Add new download
        tempDownloads.add(downloadItem);
        Hawk.put("my_downloads_temp", tempDownloads);
    }
    
    public void onDownloadCompleted(long downloadId) {
        activeDownloadIds.remove(downloadId);
        processQueue(); // Start next download in queue
    }
    
    public void onDownloadFailed(long downloadId) {
        activeDownloadIds.remove(downloadId);
        processQueue(); // Start next download in queue
    }
    
    public int getQueueSize() {
        return downloadQueue.size();
    }
    
    public int getActiveDownloadsCount() {
        return activeDownloadIds.size();
    }
    
    public void pauseAllDownloads() {
        for (Long downloadId : activeDownloadIds) {
            downloadManager.remove(downloadId);
        }
        activeDownloadIds.clear();
    }
    
    public void resumeAllDownloads() {
        processQueue();
    }
    
    public void clearQueue() {
        downloadQueue.clear();
    }
    
    private static class DownloadRequest {
        DownloadItem downloadItem;
        String url;
        String fileName;
        
        DownloadRequest(DownloadItem downloadItem, String url, String fileName) {
            this.downloadItem = downloadItem;
            this.url = url;
            this.fileName = fileName;
        }
    }
}