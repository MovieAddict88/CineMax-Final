package com.cinecraze.free.stream;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cinecraze.free.stream.models.Episode;

import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private Context context;
    private List<Episode> episodes;
    private OnEpisodeClickListener listener;
    private int selectedEpisodePosition = 0;

    public interface OnEpisodeClickListener {
        void onEpisodeClick(Episode episode, int position);
    }

    public EpisodeAdapter(Context context, List<Episode> episodes, OnEpisodeClickListener listener) {
        this.context = context;
        this.episodes = episodes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_episode, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Episode episode = episodes.get(position);
        
        holder.episodeNumber.setText("Episode " + episode.getEpisode());
        holder.episodeTitle.setText(episode.getTitle());
        holder.episodeDuration.setText(episode.getDuration());
        
        if (episode.getThumbnail() != null && !episode.getThumbnail().isEmpty()) {
            Glide.with(context).load(episode.getThumbnail()).into(holder.episodeThumbnail);
        }
        
        // Highlight selected episode
        holder.itemView.setSelected(position == selectedEpisodePosition);
        
        holder.itemView.setOnClickListener(v -> {
            selectedEpisodePosition = position;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onEpisodeClick(episode, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return episodes != null ? episodes.size() : 0;
    }

    public void setSelectedEpisode(int position) {
        selectedEpisodePosition = position;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView episodeThumbnail;
        TextView episodeNumber;
        TextView episodeTitle;
        TextView episodeDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            episodeThumbnail = itemView.findViewById(R.id.episode_thumbnail);
            episodeNumber = itemView.findViewById(R.id.episode_number);
            episodeTitle = itemView.findViewById(R.id.episode_title);
            episodeDuration = itemView.findViewById(R.id.episode_duration);
        }
    }
}