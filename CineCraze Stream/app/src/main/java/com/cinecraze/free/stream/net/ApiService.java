package com.cinecraze.free.stream.net;

import com.cinecraze.free.stream.models.Playlist;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface ApiService {
    @Headers({
        "User-Agent: Mozilla/5.0 (Android) CineCraze/1.0",
        "Accept: application/json"
    })
    @GET("playlist.json")
    Call<Playlist> getPlaylist();
}
