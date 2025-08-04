package com.cinecraze.free.stream.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Episode {

    @SerializedName("Episode")
    private int episode;

    @SerializedName("Title")
    private String title;

    @SerializedName("Duration")
    private String duration;

    @SerializedName("Description")
    private String description;

    @SerializedName("Thumbnail")
    private String thumbnail;

    @SerializedName("Servers")
    private List<Server> servers;

    public int getEpisode() {
        return episode;
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }
}
