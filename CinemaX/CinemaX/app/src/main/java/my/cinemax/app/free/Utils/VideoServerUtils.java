package my.cinemax.app.free.Utils;

import android.util.Log;

/**
 * Utility class for handling video server configurations and fallbacks
 */
public class VideoServerUtils {
    
    private static final String TAG = "VideoServerUtils";
    
    // Video server configurations
    public static final String VIDSRC_BASE = "https://vidsrc.net/embed";
    public static final String VIDJOY_BASE = "https://vidjoy.pro/embed";
    
    // Fallback server options
    public static final String[] VIDSRC_SERVERS = {"cloudserve", "superembed", "auto"};
    public static final String[] VIDJOY_QUALITIES = {"1080p", "720p", "480p"};
    
    /**
     * Enhance video URL with fallback parameters
     */
    public static String enhanceVideoUrl(String originalUrl) {
        if (originalUrl == null) return originalUrl;
        
        // Handle VidSrc.net URLs
        if (originalUrl.contains("vidsrc.net")) {
            return addVidSrcParameters(originalUrl);
        }
        
        // Handle VidJoy.pro URLs
        if (originalUrl.contains("vidjoy.pro")) {
            return addVidJoyParameters(originalUrl);
        }
        
        return originalUrl;
    }
    
    /**
     * Add VidSrc.net parameters for better reliability
     */
    private static String addVidSrcParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        enhancedUrl.append("server=cloudserve")
                  .append("&backup=superembed")
                  .append("&fallback=auto")
                  .append("&quality=1080p");
        
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
                  .append("&fallback=true")
                  .append("&backup=cloudserve");
        
        Log.d(TAG, "Enhanced VidJoy URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Get fallback URL when primary server fails
     */
    public static String getFallbackUrl(String failingUrl, int attempt) {
        if (failingUrl == null) return null;
        
        if (failingUrl.contains("vidsrc.net")) {
            return getVidSrcFallback(failingUrl, attempt);
        } else if (failingUrl.contains("vidjoy.pro")) {
            return getVidJoyFallback(failingUrl, attempt);
        }
        
        return null;
    }
    
    /**
     * Get VidSrc fallback URL
     */
    private static String getVidSrcFallback(String url, int attempt) {
        if (attempt >= VIDSRC_SERVERS.length) return null;
        
        String server = VIDSRC_SERVERS[attempt];
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
        if (attempt >= VIDJOY_QUALITIES.length) return null;
        
        String quality = VIDJOY_QUALITIES[attempt];
        if (url.contains("quality=")) {
            return url.replaceAll("quality=[^&]*", "quality=" + quality);
        } else {
            return url + (url.contains("?") ? "&" : "?") + "quality=" + quality;
        }
    }
    
    /**
     * Check if URL is from a supported video server
     */
    public static boolean isSupportedVideoServer(String url) {
        return url != null && (url.contains("vidsrc.net") || url.contains("vidjoy.pro"));
    }
    
    /**
     * Get server type from URL
     */
    public static String getServerType(String url) {
        if (url == null) return "unknown";
        
        if (url.contains("vidsrc.net")) return "vidsrc";
        if (url.contains("vidjoy.pro")) return "vidjoy";
        
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
}