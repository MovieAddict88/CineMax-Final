package my.cinemax.app.free.entity;


import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DownloadItem implements Parcelable {

    @SerializedName("id")
    @Expose
    private Integer id;

    @SerializedName("title")
    @Expose
    private String title;

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("path")
    @Expose
    private String path;

    @SerializedName("image")
    @Expose
    private String image;


    @SerializedName("size")
    @Expose
    private String size;

    @SerializedName("duration")
    @Expose
    private String duration;

    @SerializedName("element")
    @Expose
    private Integer element;

    @SerializedName("downloadid")
    @Expose
    private Long downloadid;

    // Progress tracking fields
    private int progress = 0;
    private long downloadedBytes = 0;
    private long totalBytes = 0;
    private boolean isDownloading = false;

    private int typeView = 1;

    public DownloadItem(Integer id, String title, String type, String path, String image, String size, String duration, Integer element, long downloadid) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.path = path;
        this.image = image;
        this.size = size;
        this.duration = duration;
        this.element = element;
        this.downloadid = downloadid;
    }

    public DownloadItem() {
    }

    protected DownloadItem(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readInt();
        }
        title = in.readString();
        type = in.readString();
        path = in.readString();
        image = in.readString();
        size = in.readString();
        duration = in.readString();
        if (in.readByte() == 0) {
            element = null;
        } else {
            element = in.readInt();
        }
        if (in.readByte() == 0) {
            downloadid = null;
        } else {
            downloadid = in.readLong();
        }
        progress = in.readInt();
        downloadedBytes = in.readLong();
        totalBytes = in.readLong();
        isDownloading = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(id);
        }
        dest.writeString(title);
        dest.writeString(type);
        dest.writeString(path);
        dest.writeString(image);
        dest.writeString(size);
        dest.writeString(duration);
        if (element == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(element);
        }
        if (downloadid == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(downloadid);
        }
        dest.writeInt(progress);
        dest.writeLong(downloadedBytes);
        dest.writeLong(totalBytes);
        dest.writeByte((byte) (isDownloading ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DownloadItem> CREATOR = new Creator<DownloadItem>() {
        @Override
        public DownloadItem createFromParcel(Parcel in) {
            return new DownloadItem(in);
        }

        @Override
        public DownloadItem[] newArray(int size) {
            return new DownloadItem[size];
        }
    };

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Integer getElement() {
        return element;
    }

    public void setElement(Integer element) {
        this.element = element;
    }

    public Long getDownloadid() {
        return downloadid;
    }

    public void setDownloadid(Long downloadid) {
        this.downloadid = downloadid;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void setDownloading(boolean downloading) {
        isDownloading = downloading;
    }

    public int getTypeView() {
        return typeView;
    }

    public DownloadItem setTypeView(int typeView) {
        this.typeView = typeView;
        return this;
    }
}

