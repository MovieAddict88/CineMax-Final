package com.cinecraze.free.stream;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.cinecraze.free.stream.models.Entry;
import com.cinecraze.free.stream.models.Season;
import com.cinecraze.free.stream.models.Episode;
import com.cinecraze.free.stream.models.Server;
import com.cinecraze.free.stream.utils.VideoServerUtils;
import com.cinecraze.free.stream.player.CustomPlayerFragment;
import com.google.gson.Gson;

import java.util.List;
import java.util.ArrayList;

public class DetailsActivity extends AppCompatActivity {

    private static final String TAG = "DetailsActivity";

    // UI Components
    private TextView title;
    private TextView description;
    private RecyclerView relatedContentRecyclerView;
    
    // CinemaX-style UI components
    private ImageView imageViewMovieBackground;
    private ImageView imageViewMovieCover;
    private TextView textViewMovieTitle;
    private TextView textViewMovieDescription;
    private TextView textViewMovieYear;
    private TextView textViewMovieDuration;
    private RatingBar ratingBarMovieRating;
    private RecyclerView recyclerViewMovieGenres;
    // Remove My List and Share button references
    // private LinearLayout linearLayoutMovieMyList;
    // private LinearLayout linearLayoutMovieShare;
    // private ImageView imageViewMovieMyList;
    private FloatingActionButton floatingActionButtonPlay;
    
    // Server selector components
    private LinearLayout serverSelectorContainer;
    private Button serverSpinnerButton;
    private TextView serverInfoText;
    
    // Server dialog
    private Dialog serverSelectionDialog;
    private LinearLayoutManager linearLayoutManagerServers;
    
    // TV Series components
    private LinearLayout seasonSelectorContainer;
    private Button seasonSpinnerButton;
    private TextView seasonInfoText;
    private LinearLayout episodeSelectorContainer;
    private RecyclerView episodeRecyclerView;
    
    // Enhanced video source selection
    private int currentServerIndex = 0;
    private int currentSeasonIndex = 0;
    private SmartServerSpinner smartServerSpinner;
    private boolean isInFullscreen = false;
    
    // CinemaX Player Fragment
    private CustomPlayerFragment customPlayerFragment;
    
    // Data
    private Entry currentEntry;
    private Season currentSeason;
    private Episode currentEpisode;
    private List<Server> currentServers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        
        try {
            initializeViews();
            setupData();
            setupVideoPlayer(); // Restored - needed for proper initialization
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading content: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        // Old components for compatibility
        title = findViewById(R.id.title);
        description = findViewById(R.id.description);
        relatedContentRecyclerView = findViewById(R.id.related_content_recycler_view);
        
        // Initialize CinemaX-style UI components
        imageViewMovieBackground = findViewById(R.id.image_view_activity_movie_background);
        imageViewMovieCover = findViewById(R.id.image_view_activity_movie_cover);
        textViewMovieTitle = findViewById(R.id.text_view_activity_movie_title);
        textViewMovieDescription = findViewById(R.id.text_view_activity_movie_description);
        textViewMovieYear = findViewById(R.id.text_view_activity_movie_year);
        textViewMovieDuration = findViewById(R.id.text_view_activity_movie_duration);
        ratingBarMovieRating = findViewById(R.id.rating_bar_activity_movie_rating);
        recyclerViewMovieGenres = findViewById(R.id.recycle_view_activity_movie_genres);
        // Remove My List and Share button references
        // linearLayoutMovieMyList = findViewById(R.id.linear_layout_activity_movie_my_list);
        // linearLayoutMovieShare = findViewById(R.id.linear_layout_movie_activity_share);
        // imageViewMovieMyList = findViewById(R.id.image_view_activity_movie_my_list);
        floatingActionButtonPlay = findViewById(R.id.floating_action_button_activity_movie_play);
        
        // Initialize server selector components (will be hidden for movies)
        serverSelectorContainer = findViewById(R.id.server_selector_container);
        serverSpinnerButton = findViewById(R.id.server_spinner_button);
        serverInfoText = findViewById(R.id.server_info_text);
        
        // Initialize TV Series components
        seasonSelectorContainer = findViewById(R.id.season_selector_container);
        seasonSpinnerButton = findViewById(R.id.season_spinner_button);
        seasonInfoText = findViewById(R.id.season_info_text);
        episodeSelectorContainer = findViewById(R.id.episode_selector_container);
        episodeRecyclerView = findViewById(R.id.episode_recycler_view);
        
        // Setup click listeners
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        // Floating action button click listener (main play button)
        floatingActionButtonPlay.setOnClickListener(v -> showServerSelectionDialog());
        
        // Remove My List and Share button click listeners
        // linearLayoutMovieMyList.setOnClickListener(v -> toggleMyList());
        // linearLayoutMovieShare.setOnClickListener(v -> shareMovie());
        
        // Legacy server/season selectors (for compatibility)
        if (serverSpinnerButton != null) {
            serverSpinnerButton.setOnClickListener(v -> showServerSpinner());
        }
        if (seasonSpinnerButton != null) {
            seasonSpinnerButton.setOnClickListener(v -> showSeasonSpinner());
        }
    }

    private void setupData() {
        currentEntry = getEntryFromIntent();
        if (currentEntry != null) {
            // Update legacy components for compatibility
            if (title != null) title.setText(currentEntry.getTitle());
            if (description != null) description.setText(currentEntry.getDescription());
            
            // Update CinemaX-style UI components
            if (textViewMovieTitle != null) {
                textViewMovieTitle.setText(currentEntry.getTitle());
            }
            if (textViewMovieDescription != null) {
                textViewMovieDescription.setText(currentEntry.getDescription());
            }
            
            // Set movie details if available
            if (textViewMovieYear != null && currentEntry.getYear() > 0) {
                textViewMovieYear.setText(String.valueOf(currentEntry.getYear()));
            }
            if (textViewMovieDuration != null && currentEntry.getDuration() != null) {
                textViewMovieDuration.setText(currentEntry.getDuration() + " min");
            }
            if (ratingBarMovieRating != null && currentEntry.getRating() > 0) {
                try {
                    float rating = currentEntry.getRating();
                    ratingBarMovieRating.setRating(rating / 2.0f); // Convert to 5-star scale
                } catch (Exception e) {
                    ratingBarMovieRating.setVisibility(View.GONE);
                }
            }
            
            // Load movie images
            loadMovieImages();
            
            // Setup server selector
            setupServerSelector();
            
            // Setup TV Series components if it's a TV series
            if ("TV Series".equalsIgnoreCase(currentEntry.getMainCategory()) || 
                "TV".equalsIgnoreCase(currentEntry.getMainCategory())) {
                setupTVSeriesComponents();
            } else {
                // Hide season selector for movies
                if (seasonSelectorContainer != null) {
                    seasonSelectorContainer.setVisibility(View.GONE);
                }
                if (episodeSelectorContainer != null) {
                    episodeSelectorContainer.setVisibility(View.GONE);
                }
            }
            
            // Setup Related Content
            setupRelatedContent();
        } else {
            Log.e(TAG, "No entry data received");
            Toast.makeText(this, "No content data available", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void hideActionButtons() {
        // Hide My List and Share buttons
        LinearLayout myListButton = findViewById(R.id.linear_layout_activity_movie_my_list);
        LinearLayout shareButton = findViewById(R.id.linear_layout_movie_activity_share);
        
        if (myListButton != null) {
            myListButton.setVisibility(View.GONE);
        }
        if (shareButton != null) {
            shareButton.setVisibility(View.GONE);
        }
    }
    
    private void setupRelatedContent() {
        if (currentEntry != null && currentEntry.getRelated() != null && !currentEntry.getRelated().isEmpty()) {
            LinearLayout relatedContentContainer = findViewById(R.id.linear_layout_activity_movie_more_movies);
            RecyclerView relatedContentRecyclerView = findViewById(R.id.recycle_view_activity_activity_movie_more_movies);
            
            if (relatedContentContainer != null) {
                relatedContentContainer.setVisibility(View.VISIBLE);
            }
            
            if (relatedContentRecyclerView != null) {
                // Setup RecyclerView for related content
                relatedContentRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                MovieAdapter relatedAdapter = new MovieAdapter(this, currentEntry.getRelated(), false);
                relatedContentRecyclerView.setAdapter(relatedAdapter);
            }
        }
    }
    
    private void loadMovieImages() {
        // TODO: Implement image loading using Picasso or Glide
        // For now, we'll use placeholder images
        if (imageViewMovieBackground != null && currentEntry.getThumbnail() != null) {
            // Load backdrop image from thumbnail
            // Picasso.get().load(currentEntry.getThumbnail()).into(imageViewMovieBackground);
        }
        if (imageViewMovieCover != null && currentEntry.getPoster() != null) {
            // Load poster image
            // Picasso.get().load(currentEntry.getPoster()).into(imageViewMovieCover);
        }
    }

    private void setupServerSelector() {
        currentServers = getCurrentServers();
        if (currentServers != null && !currentServers.isEmpty()) {
            // Filter out servers with null or empty URLs
            currentServers.removeIf(server -> server.getUrl() == null || server.getUrl().trim().isEmpty());
            
            if (!currentServers.isEmpty()) {
                serverSelectorContainer.setVisibility(View.VISIBLE);
                updateServerButtonText();
                updateServerInfo();
            } else {
                serverSelectorContainer.setVisibility(View.GONE);
                // Create fallback servers if none available
                createFallbackServers();
            }
        } else {
            serverSelectorContainer.setVisibility(View.GONE);
            // Create fallback servers if none available
            createFallbackServers();
        }
    }
    
    private void createFallbackServers() {
        if (currentEntry != null) {
            currentServers = new ArrayList<>();
            
            // Create fallback servers based on entry data
            if (currentEntry.getTitle() != null) {
                // Create VidSrc fallback
                Server vidSrcServer = new Server();
                vidSrcServer.setName("VidSrc.to");
                vidSrcServer.setUrl("https://vidsrc.to/embed/movie/" + currentEntry.getId());
                currentServers.add(vidSrcServer);
                
                // Create VidJoy fallback
                Server vidJoyServer = new Server();
                vidJoyServer.setName("VidJoy.pro");
                vidJoyServer.setUrl("https://vidjoy.pro/embed/movie/" + currentEntry.getId());
                currentServers.add(vidJoyServer);
            }
        }
    }

    private void setupTVSeriesComponents() {
        if (currentEntry.getSeasons() != null && !currentEntry.getSeasons().isEmpty()) {
            seasonSelectorContainer.setVisibility(View.VISIBLE);
            episodeSelectorContainer.setVisibility(View.VISIBLE);
            
            // Setup season adapter
            setupSeasonAdapter();
            
            // Select first season by default
            if (!currentEntry.getSeasons().isEmpty()) {
                currentSeason = currentEntry.getSeasons().get(0);
                updateSeasonButtonText();
                setupEpisodeAdapter();
            }
        } else {
            seasonSelectorContainer.setVisibility(View.GONE);
            episodeSelectorContainer.setVisibility(View.GONE);
        }
    }

    private void setupSeasonAdapter() {
        // For now, we'll use a simple spinner approach
        // You can enhance this with a custom adapter if needed
        updateSeasonButtonText();
    }

    private void setupEpisodeAdapter() {
        if (currentSeason != null && currentSeason.getEpisodes() != null && !currentSeason.getEpisodes().isEmpty()) {
            episodeRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            EpisodeAdapter episodeAdapter = new EpisodeAdapter(this, currentSeason.getEpisodes(), new EpisodeAdapter.OnEpisodeClickListener() {
                @Override
                public void onEpisodeClick(Episode episode, int position) {
                    currentEpisode = episode;
                    currentServerIndex = 0; // Reset server index for new episode
                    updateServerSelector();
                    setupVideoPlayer(); // Restored - important for episode playback
                }
            });
            episodeRecyclerView.setAdapter(episodeAdapter);
            
            // Select first episode by default
            currentEpisode = currentSeason.getEpisodes().get(0);
        }
    }

    private void showServerSpinner() {
        if (currentServers != null && currentServers.size() > 1) {
            smartServerSpinner = new SmartServerSpinner(this, currentServers, currentServerIndex);
            smartServerSpinner.setOnServerSelectedListener(new SmartServerSpinner.OnServerSelectedListener() {
                @Override
                public void onServerSelected(Server server, int position) {
                    currentServerIndex = position;
                    updateServerButtonText();
                    updateServerInfo();
                    setupVideoPlayer(); // Restored - important for server selection
                }
            });
            smartServerSpinner.show(serverSpinnerButton);
        } else {
            Toast.makeText(this, "Only one server available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSeasonSpinner() {
        if (currentEntry.getSeasons() != null && currentEntry.getSeasons().size() > 1) {
            String[] seasonNames = new String[currentEntry.getSeasons().size()];
            for (int i = 0; i < currentEntry.getSeasons().size(); i++) {
                seasonNames[i] = "Season " + currentEntry.getSeasons().get(i).getSeason();
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Select Season");
            builder.setSingleChoiceItems(seasonNames, currentSeasonIndex, (dialog, which) -> {
                currentSeasonIndex = which;
                currentSeason = currentEntry.getSeasons().get(which);
                currentEpisode = null; // Reset episode
                currentServerIndex = 0; // Reset server index
                updateSeasonButtonText();
                updateSeasonInfo();
                setupEpisodeAdapter();
                updateServerSelector();
                setupVideoPlayer(); // Restored - important for season selection
                dialog.dismiss();
            });
            builder.show();
        } else {
            Toast.makeText(this, "Only one season available", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateServerButtonText() {
        if (currentServers != null && currentServers.size() > currentServerIndex) {
            Server server = currentServers.get(currentServerIndex);
            String serverType = VideoServerUtils.getServerType(server.getUrl());
            serverSpinnerButton.setText(serverType);
        }
    }

    private void updateServerInfo() {
        if (currentServers != null) {
            int totalServers = currentServers.size();
            int embedCount = 0;
            int directCount = 0;
            
            for (Server server : currentServers) {
                if (VideoServerUtils.isEmbeddedVideoUrl(server.getUrl())) {
                    embedCount++;
                } else {
                    directCount++;
                }
            }
            
            StringBuilder info = new StringBuilder();
            if (directCount > 0) info.append(directCount).append("D");
            if (embedCount > 0) {
                if (info.length() > 0) info.append("/");
                info.append(embedCount).append("E");
            }
            
            serverInfoText.setText(info.toString());
        }
    }

    private void updateSeasonButtonText() {
        if (currentSeason != null) {
            seasonSpinnerButton.setText("Season " + currentSeason.getSeason());
        }
    }

    private void updateSeasonInfo() {
        if (currentSeason != null && currentSeason.getEpisodes() != null) {
            seasonInfoText.setText(currentSeason.getEpisodes().size() + " episodes");
        }
    }

    private void updateServerSelector() {
        if (currentServers != null && !currentServers.isEmpty()) {
            updateServerButtonText();
            updateServerInfo();
        }
    }
    
    private void setupVideoPlayer() {
        String videoUrl = getCurrentVideoUrl();
        
        if (videoUrl != null && !videoUrl.isEmpty()) {
            try {
                // Use the enhanced CustomPlayerFragment that handles both embedded and direct videos
                customPlayerFragment = CustomPlayerFragment.newInstance(
                    videoUrl,
                    false, // isLive
                    VideoServerUtils.getVideoType(videoUrl),
                    currentEntry != null ? currentEntry.getTitle() : "Video",
                    currentEntry != null ? currentEntry.getDescription() : "",
                    currentEntry != null ? currentEntry.getImageUrl() : "",
                    currentEntry != null ? currentEntry.getId() : 0,
                    "movie" // or "tv" based on content type
                );
                
                // Add fragment to container
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.player_container, customPlayerFragment);
                transaction.commit();
                
            } catch (Exception e) {
                Log.e(TAG, "Error setting up video player: " + e.getMessage(), e);
                Toast.makeText(this, "Error setting up video player", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.w(TAG, "No video URL available");
            Toast.makeText(this, "No video URL available", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentVideoUrl() {
        if (currentServers != null && currentServers.size() > currentServerIndex) {
            String url = currentServers.get(currentServerIndex).getUrl();
            return VideoServerUtils.enhanceVideoUrl(url);
        }
        return null;
    }

    private List<Server> getCurrentServers() {
        // For TV series, get servers from current episode
        if (currentEpisode != null && currentEpisode.getServers() != null && !currentEpisode.getServers().isEmpty()) {
            return currentEpisode.getServers();
        }
        // For movies, get servers from current entry
        if (currentEntry != null) {
            return currentEntry.getServers();
        }
        return null;
    }

    private Entry getEntryFromIntent() {
        try {
            String entryJson = getIntent().getStringExtra("entry");
            if (entryJson != null && !entryJson.isEmpty()) {
                Gson gson = new Gson();
                return gson.fromJson(entryJson, Entry.class);
            }
            
            Log.e(TAG, "No entry data found in intent");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing entry from intent: " + e.getMessage(), e);
            return null;
        }
    }

    // Static method to start DetailsActivity
    public static void start(android.content.Context context, Entry entry) {
        try {
            android.content.Intent intent = new android.content.Intent(context, DetailsActivity.class);
            Gson gson = new Gson();
            intent.putExtra("entry", gson.toJson(entry));
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("DetailsActivity", "Error starting DetailsActivity: " + e.getMessage(), e);
            Toast.makeText(context, "Error opening content details", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Fragment handles its own lifecycle
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fragment handles its own lifecycle
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Fragment handles its own cleanup
    }
    
    // CinemaX-style dialog and action methods
    
    private void showServerSelectionDialog() {
        if (currentServers == null || currentServers.isEmpty()) {
            // Try to create fallback servers
            createFallbackServers();
            
            if (currentServers == null || currentServers.isEmpty()) {
                Toast.makeText(this, "No video servers available for this content", Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        if (currentServers.size() == 1) {
            // If only one server, play directly
            playServer(0);
            return;
        }
        
        // Create bottom dialog similar to CinemaX
        serverSelectionDialog = new Dialog(this, android.R.style.Theme_Dialog);
        serverSelectionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        serverSelectionDialog.setCancelable(true);
        serverSelectionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        Window window = serverSelectionDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        window.setAttributes(wlp);
        
        serverSelectionDialog.setContentView(R.layout.dialog_server_selection);
        
        RelativeLayout dialogCloseArea = serverSelectionDialog.findViewById(R.id.relative_layout_dialog_server_close);
        RecyclerView serverRecyclerView = serverSelectionDialog.findViewById(R.id.recycle_view_dialog_servers);
        
        this.linearLayoutManagerServers = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        ServerSelectionAdapter serverAdapter = new ServerSelectionAdapter();
        serverRecyclerView.setHasFixedSize(true);
        serverRecyclerView.setAdapter(serverAdapter);
        serverRecyclerView.setLayoutManager(linearLayoutManagerServers);
        
        dialogCloseArea.setOnClickListener(v -> serverSelectionDialog.dismiss());
        
        serverSelectionDialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    serverSelectionDialog.dismiss();
                    return true;
                }
                return false;
            }
        });
        
        serverSelectionDialog.show();
    }
    
    private void playServer(int serverIndex) {
        if (currentServers != null && serverIndex >= 0 && serverIndex < currentServers.size()) {
            Server selectedServer = currentServers.get(serverIndex);
            
            // Update current server index
            currentServerIndex = serverIndex;
            
            // Load the video using existing player infrastructure
            loadVideoFromServer(selectedServer);
            
            if (serverSelectionDialog != null) {
                serverSelectionDialog.dismiss();
            }
        }
    }
    
    private void loadVideoFromServer(Server server) {
        try {
            // Create new CustomPlayerFragment instance with the video URL
            customPlayerFragment = CustomPlayerFragment.newInstance(
                server.getUrl(), 
                false, // isLive
                "default", // videoType
                currentEntry != null ? currentEntry.getTitle() : "Movie", // videoTitle
                currentEntry != null ? currentEntry.getDescription() : "", // videoSubTitle
                currentEntry != null ? currentEntry.getPoster() : "", // videoImage
                0, // videoId
                "movie" // videoKind
            );
            
            // Show player container and hide poster
            findViewById(R.id.player_container).setVisibility(View.VISIBLE);
            findViewById(R.id.image_view_activity_movie_cover).setVisibility(View.GONE);
            
            // Replace the fragment in the player container
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.player_container, customPlayerFragment);
            transaction.commit();
            
            Log.d(TAG, "Loading video from server: " + server.getName() + " - " + server.getUrl());
            Toast.makeText(this, "Loading from " + server.getName(), Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading video from server: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    // Removed toggleMyList and shareMovie methods since buttons are hidden
    
    // Server Selection Adapter for the dialog
    public class ServerSelectionAdapter extends RecyclerView.Adapter<ServerSelectionAdapter.ServerHolder> {
        
        @Override
        public ServerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_server_spinner, parent, false);
            return new ServerHolder(v);
        }
        
        @Override
        public void onBindViewHolder(ServerHolder holder, final int position) {
            Server server = currentServers.get(position);
            
            // Set server name
            holder.textViewServerName.setText(server.getName() != null ? server.getName() : "Server " + (position + 1));
            
            // Set server info (quality/type info)
            String serverInfo = "Auto Quality";
            // TODO: Add quality field to Server model if needed
            holder.textViewServerInfo.setText(serverInfo);
            
            // Set quality badge
            String quality = "HD";
            // TODO: Determine quality from server URL or add field to model
            holder.textViewServerQuality.setText(quality);
            
            // Set server type icon
            if (VideoServerUtils.isEmbeddedVideoUrl(server.getUrl())) {
                holder.imageViewServerTypeIcon.setImageResource(R.drawable.ic_movie);
            } else {
                holder.imageViewServerTypeIcon.setImageResource(R.drawable.ic_media_play);
            }
            
            // Show premium indicator if needed
            holder.imageViewServerPremium.setVisibility(View.GONE);
            
            // Set click listener
            holder.imageViewServerPlay.setOnClickListener(v -> playServer(position));
        }
        
        @Override
        public int getItemCount() {
            return currentServers != null ? currentServers.size() : 0;
        }
        
        public class ServerHolder extends RecyclerView.ViewHolder {
            ImageView imageViewServerTypeIcon;
            ImageView imageViewServerPremium;
            TextView textViewServerName;
            TextView textViewServerInfo;
            TextView textViewServerQuality;
            ImageView imageViewServerPlay;
            
            public ServerHolder(View itemView) {
                super(itemView);
                imageViewServerTypeIcon = itemView.findViewById(R.id.image_view_server_type_icon);
                imageViewServerPremium = itemView.findViewById(R.id.image_view_server_premium);
                textViewServerName = itemView.findViewById(R.id.text_view_server_name);
                textViewServerInfo = itemView.findViewById(R.id.text_view_server_info);
                textViewServerQuality = itemView.findViewById(R.id.text_view_server_quality);
                imageViewServerPlay = itemView.findViewById(R.id.image_view_server_play);
            }
        }
    }
}
