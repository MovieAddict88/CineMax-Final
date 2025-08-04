package com.cinecraze.free.stream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;
import android.widget.Toast;
import android.media.AudioManager;
import android.provider.Settings;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.cinecraze.free.stream.utils.VideoServerUtils;

import android.view.View;
import android.view.WindowManager;
import android.net.Uri;

public class FullScreenActivity extends AppCompatActivity implements 
        CustomPlayerView.PlayerDoubleTapListener,
        CustomPlayerView.PlayerScrollListener,
        CustomPlayerView.PlayerTapListener,
        CustomPlayerView.ResizeModeListener {

    private CustomPlayerView playerView;
    private ExoPlayer player;
    private ImageButton resizeModeButton;
    private ImageButton fullscreenButton;
    private ImageButton qualityButton;
    private int currentResizeMode = 0;
    private int currentServerIndex = 0;
    private AudioManager audioManager;
    private int maxVolume;
    private static final int[] RESIZE_MODES = {
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    };
    private static final int[] RESIZE_MODE_ICONS = {
            R.drawable.ic_fit,
            R.drawable.ic_fill,
            R.drawable.ic_zoom
    };

    public static void start(Context context, String videoUrl, long currentPosition, boolean wasPlaying, int serverIndex) {
        Intent intent = new Intent(context, FullScreenActivity.class);
        intent.putExtra("video_url", videoUrl);
        intent.putExtra("current_position", currentPosition);
        intent.putExtra("was_playing", wasPlaying);
        intent.putExtra("server_index", serverIndex);
        if (context instanceof DetailsActivity) {
            ((DetailsActivity) context).startActivityForResult(intent, 1001);
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen);

        hideSystemUI();
        initializeViews();
        setupGestureListeners();
        
        // Initialize audio manager for volume control
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        String videoUrl = getIntent().getStringExtra("video_url");
        long currentPosition = getIntent().getLongExtra("current_position", 0);
        boolean wasPlaying = getIntent().getBooleanExtra("was_playing", true);
        currentServerIndex = getIntent().getIntExtra("server_index", 0);

        if (videoUrl != null) {
            handleVideoPlayback(videoUrl, currentPosition, wasPlaying);
        }

        setupButtons();
    }

    private void initializeViews() {
        playerView = findViewById(R.id.player_view_fullscreen);
        resizeModeButton = findViewById(R.id.exo_resize_mode);
        fullscreenButton = findViewById(R.id.exo_fullscreen_button);
        qualityButton = findViewById(R.id.exo_quality_button);
    }

    private void setupGestureListeners() {
        playerView.setPlayerDoubleTapListener(this);
        playerView.setPlayerScrollListener(this);
        playerView.setPlayerTapListener(this);
        playerView.setResizeModeListener(this);
    }

    private void setupButtons() {
        // Setup fullscreen button to exit fullscreen
        fullscreenButton.setOnClickListener(v -> finishWithResult());

        // Setup resize mode button
        resizeModeButton.setOnClickListener(v -> {
            playerView.cycleResizeMode();
        });

        // Setup quality button (hide it in fullscreen for now)
        qualityButton.setVisibility(View.GONE);
    }

    private void handleVideoPlayback(String videoUrl, long currentPosition, boolean wasPlaying) {
        // Check if it's an embedded video URL
        if (VideoServerUtils.isEmbeddedVideoUrl(videoUrl)) {
            // Launch EmbedActivity for embedded videos
            EmbedActivity.start(this, videoUrl);
            finish(); // Close this activity since we're using EmbedActivity
            return;
        }

        // For direct video URLs, use ExoPlayer
        initializePlayer(videoUrl, currentPosition, wasPlaying);
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void initializePlayer(String videoUrl, long currentPosition, boolean wasPlaying) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Determine video type and create appropriate MediaSource
        String videoType = VideoServerUtils.getVideoType(videoUrl);
        MediaSource mediaSource = createMediaSource(videoUrl, videoType);
        
        if (mediaSource != null) {
            player.setMediaSource(mediaSource);
            player.seekTo(currentPosition);
            player.prepare();
            
            if (wasPlaying) {
                player.addListener(new com.google.android.exoplayer2.Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == com.google.android.exoplayer2.Player.STATE_READY) {
                            player.play();
                            player.removeListener(this);
                        }
                    }
                });
            }
        } else {
            Toast.makeText(this, "Unsupported video format", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Set controller timeout to show controls longer
        playerView.setControllerShowTimeoutMs(5000);
        playerView.setControllerHideOnTouch(true);
    }

    private MediaSource createMediaSource(String videoUrl, String videoType) {
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "CineCraze"));
        
        Uri videoUri = Uri.parse(videoUrl);
        
        switch (videoType) {
            case "m3u8":
                return new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(videoUri));
            case "dash":
                return new DashMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(videoUri));
            case "mp4":
            default:
                return new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(videoUri));
        }
    }

    // Gesture listener implementations
    @Override
    public void onDoubleTapForward() {
        if (player != null) {
            long newPosition = Math.min(player.getCurrentPosition() + 10000, player.getDuration());
            player.seekTo(newPosition);
            showSeekMessage("Forward +10s");
        }
    }

    @Override
    public void onDoubleTapRewind() {
        if (player != null) {
            long newPosition = Math.max(player.getCurrentPosition() - 10000, 0);
            player.seekTo(newPosition);
            showSeekMessage("Rewind -10s");
        }
    }

    @Override
    public void onHorizontalScroll(float seekDelta) {
        if (player != null && player.getDuration() > 0) {
            long currentPosition = player.getCurrentPosition();
            long newPosition = Math.max(0, Math.min(currentPosition + (long)seekDelta, player.getDuration()));
            player.seekTo(newPosition);
            
            int seekSeconds = (int)(seekDelta / 1000);
            String direction = seekSeconds > 0 ? "+" : "";
            showSeekMessage("Seek " + direction + seekSeconds + "s");
        }
    }

    @Override
    public void onVerticalScroll(float volumeDelta, boolean isRightSide) {
        if (isRightSide) {
            // Right side - volume control
            adjustVolume(volumeDelta);
        } else {
            // Left side - brightness control
            adjustBrightness(volumeDelta);
        }
    }

    @Override
    public void onSingleTap() {
        // Toggle controls visibility
        if (playerView.isControllerVisible()) {
            playerView.hideController();
        } else {
            playerView.showController();
        }
    }

    @Override
    public void onResizeModeChanged(int resizeMode) {
        currentResizeMode = getResizeModeIndex(resizeMode);
        resizeModeButton.setImageResource(RESIZE_MODE_ICONS[currentResizeMode]);
        
        String modeName = getResizeModeName(resizeMode);
        Toast.makeText(this, "Resize: " + modeName, Toast.LENGTH_SHORT).show();
    }

    private void adjustVolume(float delta) {
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int newVolume = Math.max(0, Math.min(maxVolume, (int)(currentVolume + delta * maxVolume)));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        
        int volumePercent = (int)((float)newVolume / maxVolume * 100);
        showVolumeMessage("Volume: " + volumePercent + "%");
    }

    private void adjustBrightness(float delta) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        float currentBrightness = layoutParams.screenBrightness;
        
        if (currentBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
            try {
                currentBrightness = Settings.System.getInt(getContentResolver(), 
                        Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            } catch (Settings.SettingNotFoundException e) {
                currentBrightness = 0.5f;
            }
        }
        
        float newBrightness = Math.max(0.01f, Math.min(1.0f, currentBrightness + delta));
        layoutParams.screenBrightness = newBrightness;
        getWindow().setAttributes(layoutParams);
        
        int brightnessPercent = (int)(newBrightness * 100);
        showBrightnessMessage("Brightness: " + brightnessPercent + "%");
    }

    private void showSeekMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showVolumeMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showBrightnessMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int getResizeModeIndex(int resizeMode) {
        for (int i = 0; i < RESIZE_MODES.length; i++) {
            if (RESIZE_MODES[i] == resizeMode) {
                return i;
            }
        }
        return 0;
    }

    private String getResizeModeName(int resizeMode) {
        switch (resizeMode) {
            case AspectRatioFrameLayout.RESIZE_MODE_FIT:
                return "Fit";
            case AspectRatioFrameLayout.RESIZE_MODE_FILL:
                return "Fill";
            case AspectRatioFrameLayout.RESIZE_MODE_ZOOM:
                return "Zoom";
            default:
                return "Unknown";
        }
    }

    @Override
    public void onBackPressed() {
        finishWithResult();
    }
    
    private void finishWithResult() {
        Intent resultIntent = new Intent();
        if (player != null) {
            resultIntent.putExtra("final_position", player.getCurrentPosition());
            resultIntent.putExtra("was_playing", player.isPlaying());
        }
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && !isFinishing()) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
