package com.cinecraze.free.stream.utils;

import android.util.Log;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility class for handling video server configurations and fallbacks
 */
public class VideoServerUtils {
    
    private static final String TAG = "VideoServerUtils";
    
    // Video server configurations
    public static final String VIDSRC_BASE = "https://vidsrc.to/embed";
    public static final String VIDSRC_NET_BASE = "https://vidsrc.net/embed";
    public static final String VIDJOY_BASE = "https://vidjoy.pro/embed";
    public static final String VIDBINGE_BASE = "https://vidbinge.dev/embed";
    public static final String MULTIEMBED_BASE = "https://multiembed.mov";
    
    // Popular embedded video servers (including common variations)
    public static final String[] EMBED_SERVERS = {
        "vidsrc.to", "vidsrc.net", "vidjoy.pro", "vidbinge.dev", "multiembed.mov",
        "streamwish.to", "doodstream.com", "mixdrop.co", "streamtape.com",
        "vidcloud.co", "upcloud.to", "nova.video", "streamhub.to"
    };
    
    // Video quality options
    public static final String[] VIDEO_QUALITIES = {"1080p", "720p", "480p", "360p"};
    
    // Video format patterns
    private static final Pattern MP4_PATTERN = Pattern.compile(".*\\.(mp4|m4v).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern HLS_PATTERN = Pattern.compile(".*\\.(m3u8|hls).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern DASH_PATTERN = Pattern.compile(".*\\.(mpd|dash).*", Pattern.CASE_INSENSITIVE);
    
    /**
     * Get video type from URL to determine proper MediaSource
     */
    public static String getVideoType(String url) {
        if (url == null) return "mp4";
        
        url = url.toLowerCase();
        
        if (url.contains(".m3u8") || url.contains("hls")) {
            return "m3u8";
        } else if (url.contains(".mpd") || url.contains("dash")) {
            return "dash";
        } else if (url.contains(".mp4")) {
            return "mp4";
        } else if (url.contains(".mkv")) {
            return "mp4"; // Use MP4 source for MKV as well
        } else if (url.contains(".webm")) {
            return "mp4"; // Use MP4 source for WebM
        } else if (url.contains(".avi")) {
            return "mp4"; // Use MP4 source for AVI
        }
        
        // Default to MP4 for unknown formats
        return "mp4";
    }
    
    /**
     * Check if URL is an embedded video server
     */
    public static boolean isEmbeddedVideoUrl(String url) {
        if (url == null) return false;
        
        for (String server : EMBED_SERVERS) {
            if (url.contains(server)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Enhance video URL with fallback parameters
     */
    public static String enhanceVideoUrl(String originalUrl) {
        if (originalUrl == null) return originalUrl;
        
        // Handle different embed servers
        if (originalUrl.contains("vidsrc.to") || originalUrl.contains("vidsrc.net")) {
            return addVidSrcParameters(originalUrl);
        } else if (originalUrl.contains("vidjoy.pro")) {
            return addVidJoyParameters(originalUrl);
        } else if (originalUrl.contains("vidbinge.dev")) {
            return addVidBingeParameters(originalUrl);
        } else if (originalUrl.contains("multiembed.mov")) {
            return addMultiembedParameters(originalUrl);
        }
        
        return originalUrl;
    }
    
    /**
     * Add VidSrc.to parameters for better reliability
     */
    private static String addVidSrcParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        enhancedUrl.append("server=vidcloud")
                  .append("&quality=1080p")
                  .append("&autoplay=1")
                  .append("&t=1");
        
        Log.d(TAG, "Enhanced VidSrc URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Add VidJoy.pro parameters for better reliability
     */
    private static String addVidJoyParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        enhancedUrl.append("quality=1080p")
                  .append("&server=auto")
                  .append("&autoplay=1")
                  .append("&sub.file=");
        
        Log.d(TAG, "Enhanced VidJoy URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Add VidBinge.dev parameters
     */
    private static String addVidBingeParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        enhancedUrl.append("quality=auto")
                  .append("&autoplay=1")
                  .append("&sub=1");
        
        Log.d(TAG, "Enhanced VidBinge URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Add Multiembed parameters
     */
    private static String addMultiembedParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        enhancedUrl.append("server=vip")
                  .append("&quality=1080p")
                  .append("&autoplay=1");
        
        Log.d(TAG, "Enhanced Multiembed URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Get fallback URL when primary server fails
     */
    public static String getFallbackUrl(String failingUrl, int attempt) {
        if (failingUrl == null) return null;
        
        if (failingUrl.contains("vidsrc.to") || failingUrl.contains("vidsrc.net")) {
            return getVidSrcFallback(failingUrl, attempt);
        } else if (failingUrl.contains("vidjoy.pro")) {
            return getVidJoyFallback(failingUrl, attempt);
        } else if (failingUrl.contains("vidbinge.dev")) {
            return getVidBingeFallback(failingUrl, attempt);
        }
        
        return null;
    }
    
    /**
     * Get VidSrc fallback URL
     */
    private static String getVidSrcFallback(String url, int attempt) {
        String[] servers = {"vidcloud", "upcloud", "nova", "streamwish"};
        if (attempt >= servers.length) return null;
        
        String server = servers[attempt];
        if (url.contains("server=")) {
            return url.replaceAll("server=[^&]*", "server=" + server);
        } else {
            return url + (url.contains("?") ? "&" : "?") + "server=" + server;
        }
    }
    
    /**
     * Get VidJoy fallback URL
     */
    private static String getVidJoyFallback(String url, int attempt) {
        if (attempt >= VIDEO_QUALITIES.length) return null;
        
        String quality = VIDEO_QUALITIES[attempt];
        if (url.contains("quality=")) {
            return url.replaceAll("quality=[^&]*", "quality=" + quality);
        } else {
            return url + (url.contains("?") ? "&" : "?") + "quality=" + quality;
        }
    }
    
    /**
     * Get VidBinge fallback URL
     */
    private static String getVidBingeFallback(String url, int attempt) {
        String[] qualities = {"auto", "1080p", "720p", "480p"};
        if (attempt >= qualities.length) return null;
        
        String quality = qualities[attempt];
        if (url.contains("quality=")) {
            return url.replaceAll("quality=[^&]*", "quality=" + quality);
        } else {
            return url + (url.contains("?") ? "&" : "?") + "quality=" + quality;
        }
    }
    
    /**
     * Build embed URL for TMDB/IMDB ID
     */
    public static String buildEmbedUrl(String server, String tmdbId, String imdbId, String type, int season, int episode) {
        switch (server.toLowerCase()) {
            case "vidsrc":
                if (type.equals("movie")) {
                    return VIDSRC_BASE + "/movie/" + tmdbId;
                } else {
                    return VIDSRC_BASE + "/tv/" + tmdbId + "/" + season + "/" + episode;
                }
            case "vidjoy":
                if (type.equals("movie")) {
                    return VIDJOY_BASE + "/movie/" + tmdbId;
                } else {
                    return VIDJOY_BASE + "/tv/" + tmdbId + "-" + season + "-" + episode;
                }
            case "vidbinge":
                if (type.equals("movie")) {
                    return VIDBINGE_BASE + "/movie?tmdb=" + tmdbId;
                } else {
                    return VIDBINGE_BASE + "/tv?tmdb=" + tmdbId + "&season=" + season + "&episode=" + episode;
                }
            case "multiembed":
                if (type.equals("movie")) {
                    return MULTIEMBED_BASE + "/directstream.php?video_id=" + tmdbId + "&tmdb=1";
                } else {
                    return MULTIEMBED_BASE + "/directstream.php?video_id=" + tmdbId + "&tmdb=1&s=" + season + "&e=" + episode;
                }
            default:
                return null;
        }
    }
    
    /**
     * Check if URL is from a supported video server
     */
    public static boolean isSupportedVideoServer(String url) {
        if (url == null) return false;
        
        for (String server : EMBED_SERVERS) {
            if (url.contains(server)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get server type from URL
     */
    public static String getServerType(String url) {
        if (url == null) return "unknown";
        
        if (url.contains("vidsrc.to")) return "VidSrc.to";
        if (url.contains("vidsrc.net")) return "VidSrc.net";
        if (url.contains("vidjoy.pro")) return "VidJoy";
        if (url.contains("vidbinge.dev")) return "VidBinge";
        if (url.contains("multiembed.mov")) return "MultiEmbed";
        if (url.contains("streamwish.to")) return "StreamWish";
        if (url.contains("doodstream.com")) return "DoodStream";
        if (url.contains("mixdrop.co")) return "MixDrop";
        if (url.contains("streamtape.com")) return "StreamTape";
        
        for (String server : EMBED_SERVERS) {
            if (url.contains(server)) {
                return server.split("\\.")[0]; // Get first part before domain
            }
        }
        
        return "unknown";
    }
    
    /**
     * Validate video URL format
     */
    public static boolean isValidVideoUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        
        return url.startsWith("http://") || url.startsWith("https://") || 
               url.startsWith("rtmp://") || url.startsWith("rtmps://");
    }
    
    /**
     * Extract video ID from various URL formats
     */
    public static String extractVideoId(String url) {
        if (url == null) return null;
        
        // Extract TMDB ID pattern
        Pattern tmdbPattern = Pattern.compile("(?:tmdb|movie|tv)/(\\d+)");
        Matcher matcher = tmdbPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Extract IMDB ID pattern
        Pattern imdbPattern = Pattern.compile("(tt\\d+)");
        matcher = imdbPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Get user agent for better compatibility
     */
    public static String getUserAgent() {
        return "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36";
    }
}