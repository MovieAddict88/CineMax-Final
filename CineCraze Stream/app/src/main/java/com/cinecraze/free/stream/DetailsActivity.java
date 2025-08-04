package com.cinecraze.free.stream;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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
import com.cinecraze.free.stream.repository.DataRepository;
import com.cinecraze.free.stream.utils.VideoServerUtils;
import com.cinecraze.free.stream.player.CustomPlayerFragment;
import com.google.gson.Gson;
import com.bumptech.glide.Glide;

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
    // Removed my list and share components
    private FloatingActionButton floatingActionButtonPlay;
    
    // Removed server selector components
    
    // Server dialog
    private Dialog serverSelectionDialog;
    private LinearLayoutManager linearLayoutManagerServers;
    
    // TV Series components
    private androidx.appcompat.widget.AppCompatSpinner seasonSpinner;
    private LinearLayout seriesSeasonsContainer;
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
    private EpisodeAdapter episodeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        
        try {
            initializeViews();
            setupData();
            setupVideoPlayer();
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
        floatingActionButtonPlay = findViewById(R.id.floating_action_button_activity_movie_play);
        
        // Initialize TV Series components (CinemaX-style)
        seriesSeasonsContainer = findViewById(R.id.linear_layout_activity_serie_seasons);
        seasonSpinner = findViewById(R.id.spinner_activity_serie_season_list);
        episodeRecyclerView = findViewById(R.id.recycle_view_activity_activity_serie_episodes);
        
        // Setup click listeners
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        // Floating action button click listener (main play button)
        floatingActionButtonPlay.setOnClickListener(v -> showServerSelectionDialog());
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
                if (seriesSeasonsContainer != null) {
                    seriesSeasonsContainer.setVisibility(View.GONE);
                }
            }
            
            // Setup related content
            setupRelatedContent();
        } else {
            Log.e(TAG, "No entry data received");
            Toast.makeText(this, "No content data available", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void loadMovieImages() {
        // Use Glide to load thumbnail and poster images
        if (imageViewMovieBackground != null && currentEntry.getThumbnail() != null) {
            Glide.with(this).load(currentEntry.getThumbnail()).into(imageViewMovieBackground);
        }
        if (imageViewMovieCover != null && currentEntry.getPoster() != null) {
            Glide.with(this).load(currentEntry.getPoster()).into(imageViewMovieCover);
        }
    }

    private void setupServerSelector() {
        currentServers = getCurrentServers();
        // Server selector UI components removed - now handled by floating play button dialog
    }

    private void setupTVSeriesComponents() {
        if (currentEntry.getSeasons() != null && !currentEntry.getSeasons().isEmpty()) {
            seriesSeasonsContainer.setVisibility(View.VISIBLE);
            
            // Setup season spinner adapter
            setupSeasonSpinner();
            
            // Select first season by default
            if (!currentEntry.getSeasons().isEmpty()) {
                currentSeason = currentEntry.getSeasons().get(0);
                currentSeasonIndex = 0;
                setupEpisodeAdapter();
            }
        } else {
            seriesSeasonsContainer.setVisibility(View.GONE);
        }
    }

    private void setupSeasonSpinner() {
        if (seasonSpinner != null && currentEntry.getSeasons() != null && !currentEntry.getSeasons().isEmpty()) {
            // Create season names array for spinner
            String[] seasonNames = new String[currentEntry.getSeasons().size()];
            for (int i = 0; i < currentEntry.getSeasons().size(); i++) {
                seasonNames[i] = "Season " + currentEntry.getSeasons().get(i).getSeason();
            }
            
            // Create and set adapter
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, seasonNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            seasonSpinner.setAdapter(adapter);
            
            // Set spinner selection listener
            seasonSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (position != currentSeasonIndex) {
                        currentSeasonIndex = position;
                        currentSeason = currentEntry.getSeasons().get(position);
                        setupEpisodeAdapter();
                    }
                }
                
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                    // Do nothing
                }
            });
        }
    }

    private void setupEpisodeAdapter() {
        if (currentSeason != null && currentSeason.getEpisodes() != null && !currentSeason.getEpisodes().isEmpty()) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            layoutManager.setInitialPrefetchItemCount(4); // Prefetch items for smoother scrolling
            episodeRecyclerView.setLayoutManager(layoutManager);
            
            // Performance optimizations for smooth scrolling
            episodeRecyclerView.setHasFixedSize(true); // Items have fixed size
            episodeRecyclerView.setItemViewCacheSize(10); // Cache more views for smoother scrolling
            episodeRecyclerView.setDrawingCacheEnabled(true);
            episodeRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
            
            episodeAdapter = new EpisodeAdapter(this, currentSeason.getEpisodes(), new EpisodeAdapter.OnEpisodeClickListener() {
                @Override
                public void onEpisodeClick(Episode episode, int position) {
                    currentEpisode = episode;
                    currentServerIndex = 0; // Reset server index for new episode
                    // Update server list for the selected episode
                    updateServerSelector();
                    // Show server selection dialog for episode (like movies)
                    showServerSelectionDialog();
                }
                
                @Override
                public void onEpisodeDownload(Episode episode, int position) {
                    // Handle episode download
                    downloadEpisode(episode);
                }
            });
            
            // Enable item view type optimization if all items are the same type
            episodeAdapter.setHasStableIds(false);
            episodeRecyclerView.setAdapter(episodeAdapter);
            
            // Select first episode by default
            currentEpisode = currentSeason.getEpisodes().get(0);
        }
    }

    private void showServerSpinner() {
        // Server spinner UI removed - functionality moved to floating action button dialog
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
                setupEpisodeAdapter();
                updateServerSelector();
                setupVideoPlayer();
                dialog.dismiss();
            });
            builder.show();
        } else {
            Toast.makeText(this, "Only one season available", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateServerButtonText() {
        // Server button removed - no longer needed
    }

    private void updateServerInfo() {
        // Server info text removed - no longer needed
    }



    private void updateServerSelector() {
        currentServers = getCurrentServers();
        // Server selector UI removed - functionality moved to floating play button
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
            // Removed toast message as requested
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
            Toast.makeText(this, "No servers available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "showServerSelectionDialog called with " + currentServers.size() + " servers");
        
        // FOR NOW: ALWAYS SHOW SERVER SELECTION DIALOG
        // This ensures the user always chooses a server before playback
        // We can add back auto-play logic for embedded content later if needed
        
        String mainCategory = currentEntry != null ? currentEntry.getMainCategory() : "";
        Log.d(TAG, "Content category: " + mainCategory);
        
        for (int i = 0; i < currentServers.size(); i++) {
            Server server = currentServers.get(i);
            Log.d(TAG, "Server " + i + ": " + server.getName() + " - " + server.getUrl());
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
                }
                return true;
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
            // Launch FullScreenActivity in landscape mode like CinemaX
            String videoUrl = VideoServerUtils.enhanceVideoUrl(server.getUrl());
            FullScreenActivity.start(this, videoUrl, 0, true, currentServerIndex);
            
            Log.d(TAG, "Loading video from server: " + server.getName() + " - " + server.getUrl());
            Toast.makeText(this, "Loading from " + server.getName(), Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading video from server: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    // Removed toggleMyList and shareMovie methods - UI components no longer exist
    
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
            holder.imageViewServerPlay.setOnClickListener(v -> {
                String videoUrl = currentServers.get(position).getUrl();
                FullScreenActivity.start(DetailsActivity.this, videoUrl, 0, true, position);
                if (serverSelectionDialog != null) serverSelectionDialog.dismiss();
            });
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
    

    
    private void setupRelatedContent() {
        if (currentEntry == null) return;
        
        // Initialize the repository
        DataRepository dataRepository = new DataRepository(this);
        
        // Get related content based on country and category
        dataRepository.getPaginatedFilteredData(
            null, // No genre filter (Entry doesn't have getGenre method)
            currentEntry.getCountry(), // Filter by same country
            null, // No year filter  
            0, // First page
            10, // Show 10 related items
            new DataRepository.PaginatedDataCallback() {
                @Override
                public void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount) {
                    // Filter out the current entry from related content
                    List<Entry> relatedEntries = new ArrayList<>();
                    for (Entry entry : entries) {
                        if (entry.getId() != currentEntry.getId()) { // Compare int IDs
                            relatedEntries.add(entry);
                        }
                    }
                    
                    // Limit to 8 items max for UI performance
                    if (relatedEntries.size() > 8) {
                        relatedEntries = relatedEntries.subList(0, 8);
                    }
                    
                    // Setup the related content RecyclerView
                    setupRelatedContentRecyclerView(relatedEntries);
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "Failed to load related content: " + error);
                    // Hide related content section if there's an error
                    LinearLayout relatedSection = findViewById(R.id.linear_layout_activity_movie_more_movies);
                    if (relatedSection != null) {
                        relatedSection.setVisibility(View.GONE);
                    }
                }
            }
        );
    }
    
    private void setupRelatedContentRecyclerView(List<Entry> relatedEntries) {
        if (relatedEntries == null || relatedEntries.isEmpty()) {
            // Hide related content section if no related items
            LinearLayout relatedSection = findViewById(R.id.linear_layout_activity_movie_more_movies);
            if (relatedSection != null) {
                relatedSection.setVisibility(View.GONE);
            }
            return;
        }
        
        // Show related content section
        LinearLayout relatedSection = findViewById(R.id.linear_layout_activity_movie_more_movies);
        if (relatedSection != null) {
            relatedSection.setVisibility(View.VISIBLE);
        }
        
        // Setup the main related content RecyclerView
        RecyclerView relatedRecyclerView = findViewById(R.id.recycle_view_activity_activity_movie_more_movies);
        if (relatedRecyclerView != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            relatedRecyclerView.setLayoutManager(layoutManager);
            
            // Create adapter for related content
            MovieAdapter relatedAdapter = new MovieAdapter(this, relatedEntries, false); // Grid view = false for horizontal
            relatedRecyclerView.setAdapter(relatedAdapter);
        }
        
        // Hide the legacy RecyclerView to avoid duplication
        if (relatedContentRecyclerView != null) {
            relatedContentRecyclerView.setVisibility(View.GONE);
        }
    }

    private void downloadEpisode(Episode episode) {
        if (episode != null && episode.getServers() != null && !episode.getServers().isEmpty()) {
            // For now, show a simple message
            Toast.makeText(this, "Download functionality coming soon for: " + episode.getTitle(), Toast.LENGTH_SHORT).show();
            
            // TODO: Implement actual download functionality
            // You can integrate with DownloadManager or custom download service here
        } else {
            Toast.makeText(this, "No download sources available", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Handle return from FullScreenActivity (video player)
        if (requestCode == 1001) {
            // Player was closed, ensure any background playback is stopped
            // This helps prevent the issue where video continues playing in background
            Log.d(TAG, "Returned from video player, ensuring cleanup");
            
            // If there's any ongoing playback in CustomPlayerFragment or other components,
            // we ensure they are properly reset
            // The actual player cleanup should happen in the respective activities/fragments
        }
    }
}
