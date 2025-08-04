package my.cinemax.app.free.ui.Adapters;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.orhanobut.hawk.Hawk;
import my.cinemax.app.free.R;
import my.cinemax.app.free.Utils.Log;
import my.cinemax.app.free.entity.DownloadItem;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

public class DownloadedAdapter extends   RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final DownloadListener downloadListener;
    private List<DownloadItem> downloadItemList;
    private Activity activity;
    
    public DownloadedAdapter(List<DownloadItem> downloadItemList, Activity activity,DownloadListener downloadListener) {
        this.downloadItemList = downloadItemList;
        this.activity = activity;
        this.downloadListener = downloadListener;
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case 1: {
                View v1 = inflater.inflate(R.layout.item_downloaded,null);
                viewHolder = new DownloadedAdapter.DownloadedHolder(v1);
                break;
            }
            case 2: {
                View v2 = inflater.inflate(R.layout.item_empty, parent, false);
                viewHolder = new DownloadedAdapter.EmptyHolder(v2);
                break;
            }

        }
        return viewHolder;
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)){
            case 1:
                DownloadedHolder downloadedHolder =  (DownloadedHolder) holder;
                DownloadItem downloadItem = downloadItemList.get(position);
                
                downloadedHolder.text_view_item_download_title.setText(downloadItem.getTitle());
                Picasso.with(activity).load(downloadItem.getImage()).into(downloadedHolder.image_view_item_download_image);
                downloadedHolder.text_view_item_download_duration.setText(downloadItem.getDuration());
                downloadedHolder.text_view_item_download_size.setText(downloadItem.getSize());
                // Always show file size, but for active downloads, show real-time progress
                if (downloadItem.isDownloading() && downloadItem.getDownloadid() != null) {
                    downloadedHolder.progress_bar_download.setVisibility(View.VISIBLE);
                    downloadedHolder.text_view_download_status.setVisibility(View.VISIBLE);
                    downloadedHolder.image_view_item_download_play.setVisibility(View.GONE);

                    // Set progress
                    downloadedHolder.progress_bar_download.setProgress(downloadItem.getProgress());

                    // Set status text with formatted file size
                    String statusText = downloadItem.getProgress() + "%";
                    if (downloadItem.getTotalBytes() > 0) {
                        statusText += " (" + formatFileSize(downloadItem.getDownloadedBytes()) +
                                    " / " + formatFileSize(downloadItem.getTotalBytes()) + ")";
                        // Also update the main file size text to show real-time progress
                        downloadedHolder.text_view_item_download_size.setText(
                            formatFileSize(downloadItem.getDownloadedBytes()) + " / " + formatFileSize(downloadItem.getTotalBytes())
                        );
                    } else {
                        // Fallback if total size is unknown
                        statusText += " (" + formatFileSize(downloadItem.getDownloadedBytes()) + ")";
                        downloadedHolder.text_view_item_download_size.setText(formatFileSize(downloadItem.getDownloadedBytes()));
                    }
                    downloadedHolder.text_view_download_status.setText(statusText);

                    // Disable click actions during download
                    downloadedHolder.relative_layout_item_download.setClickable(false);
                    downloadedHolder.image_view_item_download_play.setClickable(false);
                    downloadedHolder.image_view_item_download_delete.setClickable(false);
                    downloadedHolder.image_view_item_download_finder.setClickable(false);
                } else {
                    // Show normal completed download UI
                    downloadedHolder.progress_bar_download.setVisibility(View.GONE);
                    downloadedHolder.text_view_download_status.setVisibility(View.GONE);
                    downloadedHolder.image_view_item_download_play.setVisibility(View.VISIBLE);

                    // Show static file size for completed downloads
                    downloadedHolder.text_view_item_download_size.setText(downloadItem.getSize());

                    // Enable click actions for completed downloads
                    downloadedHolder.relative_layout_item_download.setClickable(true);
                    downloadedHolder.image_view_item_download_play.setClickable(true);
                    downloadedHolder.image_view_item_download_delete.setClickable(true);
                    downloadedHolder.image_view_item_download_finder.setClickable(true);
                }
                
                downloadedHolder.image_view_item_download_delete.setOnClickListener(v->{
                    if (!downloadItem.isDownloading()) {
                        List<DownloadItem> my_downloads_list =Hawk.get("my_downloads_list");
                        if (my_downloads_list == null) {
                            my_downloads_list = new ArrayList<>();
                        }
                        for (int i = 0; i < my_downloads_list.size(); i++) {
                            if (my_downloads_list.get(i).getId().equals(downloadItem.getId()) ) {
                                String path = downloadItem.getPath();

                                my_downloads_list.remove(my_downloads_list.get(i));
                                Hawk.put("my_downloads_list",my_downloads_list);
                                File file = new File(path);
                                if (file.exists()){
                                    my.cinemax.app.free.Utils.Log.log( "EXISTR");
                                    try {
                                        Uri imageUri = FileProvider.getUriForFile(activity,
                                                activity.getApplicationContext()
                                                        .getPackageName() + ".provider", file);
                                        ContentResolver contentResolver = activity.getContentResolver();
                                        int deletefile = contentResolver.delete(imageUri, null, null);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    file.delete();
                                }
                            }
                        }
                        downloadListener.OnUpdated();
                    }
                });
                downloadedHolder.image_view_item_download_finder.setOnClickListener(v->{
                    if (!downloadItem.isDownloading()) {
                        String path = downloadItem.getPath();

                        Uri imageUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", new File(path));

                        Intent intent = new Intent();
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("file/*");
                        intent.setData(imageUri);
                        activity.startActivity(intent);
                    }
                });
                downloadedHolder.relative_layout_item_download.setOnClickListener(v->{
                    if (!downloadItem.isDownloading()) {
                        downloadListener.OnPlay(downloadItem);
                    }
                });
                downloadedHolder.image_view_item_download_play.setOnClickListener(v->{
                    if (!downloadItem.isDownloading()) {
                        downloadListener.OnPlay(downloadItem);
                    }
                });
                break;
            case 2:

                break;
        }
    }

    @Override
    public int getItemCount() {

        return downloadItemList.size();
    }

    public class DownloadedHolder extends RecyclerView.ViewHolder {
        private final RelativeLayout relative_layout_item_download;
        private final ImageView image_view_item_download_delete;
        private final ImageView image_view_item_download_play;
        private final ImageView image_view_item_download_image;
        private final ImageView image_view_item_download_finder;
        private final TextView text_view_item_download_title;
        private final TextView text_view_item_download_size;
        private final TextView text_view_item_download_duration;
        private final ProgressBar progress_bar_download;
        private final TextView text_view_download_status;

        public DownloadedHolder(@NonNull View itemView) {
            super(itemView);
            this.relative_layout_item_download=(RelativeLayout) itemView.findViewById(R.id.relative_layout_item_download);
            this.image_view_item_download_delete=(ImageView) itemView.findViewById(R.id.image_view_item_download_delete);
            this.image_view_item_download_image=(ImageView) itemView.findViewById(R.id.image_view_item_download_image);
            this.image_view_item_download_play=(ImageView) itemView.findViewById(R.id.image_view_item_download_play);
            this.image_view_item_download_finder=(ImageView) itemView.findViewById(R.id.image_view_item_download_finder);
            this.text_view_item_download_duration=(TextView) itemView.findViewById(R.id.text_view_item_download_duration);
            this.text_view_item_download_size=(TextView) itemView.findViewById(R.id.text_view_item_download_size);
            this.text_view_item_download_title=(TextView) itemView.findViewById(R.id.text_view_item_download_title);
            this.progress_bar_download=(ProgressBar) itemView.findViewById(R.id.progress_bar_download);
            this.text_view_download_status=(TextView) itemView.findViewById(R.id.text_view_download_status);
        }
    }
    public class EmptyHolder extends RecyclerView.ViewHolder {
        public EmptyHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return downloadItemList.get(position).getTypeView();
    }
    
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    private String fileExt(String url) {
        if (url.indexOf("?") > -1) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf(".") + 1);
            if (ext.indexOf("%") > -1) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.indexOf("/") > -1) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
    }
    public interface DownloadListener{
        void OnUpdated();
        void OnPlay(DownloadItem downloadItem);
    }
}
