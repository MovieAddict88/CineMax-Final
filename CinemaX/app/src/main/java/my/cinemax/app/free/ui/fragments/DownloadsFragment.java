package my.cinemax.app.free.ui.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.orhanobut.hawk.Hawk;
import my.cinemax.app.free.Provider.AndroidWebServer;
import my.cinemax.app.free.R;
import my.cinemax.app.free.Utils.DownloadProgressManager;
import my.cinemax.app.free.entity.DownloadItem;
import my.cinemax.app.free.ui.Adapters.DownloadedAdapter;
import my.cinemax.app.free.ui.activities.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.WIFI_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 */
public class DownloadsFragment extends Fragment  implements DownloadedAdapter.DownloadListener {

    private static final int DEFAULT_PORT = 8589;

    // INSTANCE OF ANDROID WEB SERVER
    private AndroidWebServer androidWebServer;
    private BroadcastReceiver broadcastReceiverNetworkState;
    private BroadcastReceiver downloadProgressReceiver;
    private static boolean isStarted = false;

    private View view;
    private SwipeRefreshLayout swipe_refresh_layout_downloads_fragment;
    private LinearLayout linear_layout_load_downloads_fragment;
    private ImageView image_view_empty_list;
    private RecyclerView recycler_view_downloads_fragment;
    private List<DownloadItem> downloadItemArrayList = new ArrayList<>();
    private GridLayoutManager gridLayoutManager;
    private DownloadedAdapter downloadedAdapter;
    private DownloadItem downloadItem;
    
    // Download progress manager
    private DownloadProgressManager downloadProgressManager;

    public DownloadsFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        this.view =  inflater.inflate(R.layout.fragment_downloads, container, false);
        initView();
        initAction();
        initDownloadProgressManager();
        loadDownloadsList();
        initBroadcastReceiverNetworkStateChanged();
        initDownloadProgressReceiver();

        // Register for reload_downloads_list broadcast
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadDownloadsList();
            }
        }, new IntentFilter("reload_downloads_list"));

        return view;
    }

    private void initDownloadProgressManager() {
        downloadProgressManager = new DownloadProgressManager(getActivity());
        downloadProgressManager.startPolling();
    }

    private void initDownloadProgressReceiver() {
        downloadProgressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getStringExtra("action");
                if (action != null) {
                    switch (action) {
                        case "progress_update":
                            handleProgressUpdate(intent);
                            break;
                        case "download_completed":
                            handleDownloadCompleted(intent);
                            break;
                        case "download_failed":
                            handleDownloadFailed(intent);
                            break;
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(DownloadProgressManager.DOWNLOAD_PROGRESS_UPDATE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(downloadProgressReceiver, filter);
    }

    private void handleProgressUpdate(Intent intent) {
        Long downloadId = intent.getLongExtra("downloadId", -1);
        String title = intent.getStringExtra("title");
        int progress = intent.getIntExtra("progress", 0);
        long downloadedBytes = intent.getLongExtra("downloadedBytes", 0);
        long totalBytes = intent.getLongExtra("totalBytes", 0);
        String type = intent.getStringExtra("type");

        // Update the download item in the list
        updateDownloadProgress(downloadId, progress, downloadedBytes, totalBytes);
    }

    private void handleDownloadCompleted(Intent intent) {
        Long downloadId = intent.getLongExtra("downloadId", -1);
        String title = intent.getStringExtra("title");
        String type = intent.getStringExtra("type");

        // Refresh the downloads list
        loadDownloadsList();
    }

    private void handleDownloadFailed(Intent intent) {
        Long downloadId = intent.getLongExtra("downloadId", -1);
        String title = intent.getStringExtra("title");
        String type = intent.getStringExtra("type");

        // Remove failed download from temp list and refresh
        removeFailedDownload(downloadId);
        loadDownloadsList();
    }

    private void updateDownloadProgress(Long downloadId, int progress, long downloadedBytes, long totalBytes) {
        // Find the download item in the list and update its progress
        for (int i = 0; i < downloadItemArrayList.size(); i++) {
            DownloadItem item = downloadItemArrayList.get(i);
            if (item.getDownloadid() != null && item.getDownloadid().equals(downloadId)) {
                item.setProgress(progress);
                item.setDownloadedBytes(downloadedBytes);
                item.setTotalBytes(totalBytes);
                item.setDownloading(true);
                
                // Update the adapter
                if (downloadedAdapter != null) {
                    downloadedAdapter.notifyItemChanged(i);
                }
                break;
            }
        }
    }

    private void removeFailedDownload(Long downloadId) {
        List<DownloadItem> tempDownloads = Hawk.get("my_downloads_temp");
        if (tempDownloads != null) {
            tempDownloads.removeIf(item -> item.getDownloadid() != null && item.getDownloadid().equals(downloadId));
            Hawk.put("my_downloads_temp", tempDownloads);
        }
    }

    private void loadDownloadsList() {
        downloadItemArrayList.clear();
        linear_layout_load_downloads_fragment.setVisibility(View.VISIBLE);
        recycler_view_downloads_fragment.setVisibility(View.GONE);
        image_view_empty_list.setVisibility(View.GONE);
        
        // Load completed downloads
        List<DownloadItem> my_downloads_list = Hawk.get("my_downloads_list");
        if (my_downloads_list == null) {
            my_downloads_list = new ArrayList<>();
        }
        
        // Load active downloads (temp list)
        List<DownloadItem> my_downloads_temp = Hawk.get("my_downloads_temp");
        if (my_downloads_temp == null) {
            my_downloads_temp = new ArrayList<>();
        }
        
        // Add header
        downloadItemArrayList.add(new DownloadItem().setTypeView(2));
        
        // Add active downloads first (with progress)
        for (DownloadItem item : my_downloads_temp) {
            if (item.getDownloadid() != null) {
                item.setDownloading(true);
                downloadItemArrayList.add(item);
            }
        }
        
        // Add completed downloads
        for (DownloadItem item : my_downloads_list) {
            item.setDownloading(false);
            downloadItemArrayList.add(item);
        }
        
        if (downloadItemArrayList.size() <= 1) { // Only header
            linear_layout_load_downloads_fragment.setVisibility(View.GONE);
            recycler_view_downloads_fragment.setVisibility(View.GONE);
            image_view_empty_list.setVisibility(View.VISIBLE);
        } else {
            linear_layout_load_downloads_fragment.setVisibility(View.GONE);
            recycler_view_downloads_fragment.setVisibility(View.VISIBLE);
            image_view_empty_list.setVisibility(View.GONE);
        }
        
        if (downloadedAdapter != null) {
            downloadedAdapter.notifyDataSetChanged();
        }
    }

    private void initAction() {
            swipe_refresh_layout_downloads_fragment.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    loadDownloadsList();
                    swipe_refresh_layout_downloads_fragment.setRefreshing(false);
                }
            });
    }

    private void initView() {
        this.swipe_refresh_layout_downloads_fragment = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_downloads_fragment);
        this.linear_layout_load_downloads_fragment = (LinearLayout) view.findViewById(R.id.linear_layout_load_downloads_fragment);
        this.image_view_empty_list = (ImageView) view.findViewById(R.id.image_view_empty_list);
        this.recycler_view_downloads_fragment = (RecyclerView) view.findViewById(R.id.recycler_view_downloads_fragment);

        this.gridLayoutManager=  new GridLayoutManager(getActivity().getApplicationContext(),1,RecyclerView.VERTICAL,false);


        this.downloadedAdapter =new DownloadedAdapter(downloadItemArrayList,getActivity(),this);
        recycler_view_downloads_fragment.setHasFixedSize(true);
        recycler_view_downloads_fragment.setAdapter(downloadedAdapter);
        recycler_view_downloads_fragment.setLayoutManager(gridLayoutManager);
    }

    @Override
    public void OnUpdated() {
        loadDownloadsList();
    }
    private String getIpAccess() {
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return "http://" + formatedIpAddress + ":";
    }

    private int getPortFromEditText() {
        return  DEFAULT_PORT;
    }

    public boolean isConnectedInWifi() {
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
        NetworkInfo networkInfo = ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()
                && wifiManager.isWifiEnabled() && networkInfo.getTypeName().equals("WIFI")) {
            return true;
        }
        return false;
    }

    private boolean startAndroidWebServer(String url) {
        if (!isStarted) {
            int port = getPortFromEditText();
            try {
                if (port == 0) {
                    throw new Exception();
                }
                androidWebServer = new AndroidWebServer(port,url);
                androidWebServer.start();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean stopAndroidWebServer() {
        if (isStarted && androidWebServer != null) {
            androidWebServer.stop();
            return true;
        }
        return false;
    }
    private void initBroadcastReceiverNetworkStateChanged() {
        final IntentFilter filters = new IntentFilter();
        filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filters.addAction("android.net.wifi.STATE_CHANGE");
        broadcastReceiverNetworkState = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            }
        };
        getActivity().registerReceiver(broadcastReceiverNetworkState, filters);
    }
    @Override
    public void OnPlay(DownloadItem downloadItem) {
        this.downloadItem =downloadItem;

        if (isConnectedInWifi()) {
            if (!isStarted && startAndroidWebServer(downloadItem.getPath())) {
                String type = "video/mp4";
                if (downloadItem.getType().equals("mov"))
                    type = "video/quicktime";
                Intent intent1 = new Intent(getActivity(),PlayerActivity.class);
                intent1.putExtra("id",downloadItem.getElement());
                intent1.putExtra("url",getIpAccess()+getPortFromEditText());
                intent1.putExtra("type",type);
                intent1.putExtra("kind",downloadItem.getType());
                intent1.putExtra("image",downloadItem.getImage());
                intent1.putExtra("title",downloadItem.getTitle());
                intent1.putExtra("subtitle",downloadItem.getTitle());
                getActivity().startActivity(intent1);
                isStarted = true;
            } else if (stopAndroidWebServer()) {
                isStarted = false;
            }
        } else {
            String type = "video/mp4";
            if (downloadItem.getType().equals("mov"))
                type = "video/quicktime";
            Intent intent = new Intent(getActivity(),PlayerActivity.class);
            intent.putExtra("id",downloadItem.getElement());
            intent.putExtra("url",downloadItem.getPath());
            intent.putExtra("type",type);
            intent.putExtra("kind",downloadItem.getType());
            intent.putExtra("image",downloadItem.getImage());
            intent.putExtra("title",downloadItem.getTitle());
            intent.putExtra("subtitle",downloadItem.getTitle());
            getActivity().startActivity(intent);
        }
    }

    @Override
    public void onResume() {
        stopAndroidWebServer();
        isStarted = false;
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (downloadProgressManager != null) {
            downloadProgressManager.cleanup();
        }
        if (downloadProgressReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(downloadProgressReceiver);
        }
        getActivity().unregisterReceiver(broadcastReceiverNetworkState);
        super.onDestroy();
    }
}
