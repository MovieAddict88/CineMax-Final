package my.cinemax.app.free.entity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AdsResponse {
    @SerializedName("ads_config")
    @Expose
    private AdsConfig adsConfig;

    public AdsConfig getAdsConfig() {
        return adsConfig;
    }

    public void setAdsConfig(AdsConfig adsConfig) {
        this.adsConfig = adsConfig;
    }

    public static class AdsConfig {
        @SerializedName("admob")
        @Expose
        private AdmobConfig admob;

        @SerializedName("facebook")
        @Expose
        private FacebookConfig facebook;

        @SerializedName("settings")
        @Expose
        private AdsSettings settings;

        public AdmobConfig getAdmob() {
            return admob;
        }

        public void setAdmob(AdmobConfig admob) {
            this.admob = admob;
        }

        public FacebookConfig getFacebook() {
            return facebook;
        }

        public void setFacebook(FacebookConfig facebook) {
            this.facebook = facebook;
        }

        public AdsSettings getSettings() {
            return settings;
        }

        public void setSettings(AdsSettings settings) {
            this.settings = settings;
        }
    }

    public static class AdmobConfig {
        @SerializedName("banner_id")
        @Expose
        private String bannerId;

        @SerializedName("interstitial_id")
        @Expose
        private String interstitialId;

        @SerializedName("rewarded_id")
        @Expose
        private String rewardedId;

        @SerializedName("native_id")
        @Expose
        private String nativeId;

        @SerializedName("app_id")
        @Expose
        private String appId;

        public String getBannerId() {
            return bannerId;
        }

        public void setBannerId(String bannerId) {
            this.bannerId = bannerId;
        }

        public String getInterstitialId() {
            return interstitialId;
        }

        public void setInterstitialId(String interstitialId) {
            this.interstitialId = interstitialId;
        }

        public String getRewardedId() {
            return rewardedId;
        }

        public void setRewardedId(String rewardedId) {
            this.rewardedId = rewardedId;
        }

        public String getNativeId() {
            return nativeId;
        }

        public void setNativeId(String nativeId) {
            this.nativeId = nativeId;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }
    }

    public static class FacebookConfig {
        @SerializedName("banner_id")
        @Expose
        private String bannerId;

        @SerializedName("interstitial_id")
        @Expose
        private String interstitialId;

        @SerializedName("rewarded_id")
        @Expose
        private String rewardedId;

        @SerializedName("native_id")
        @Expose
        private String nativeId;

        public String getBannerId() {
            return bannerId;
        }

        public void setBannerId(String bannerId) {
            this.bannerId = bannerId;
        }

        public String getInterstitialId() {
            return interstitialId;
        }

        public void setInterstitialId(String interstitialId) {
            this.interstitialId = interstitialId;
        }

        public String getRewardedId() {
            return rewardedId;
        }

        public void setRewardedId(String rewardedId) {
            this.rewardedId = rewardedId;
        }

        public String getNativeId() {
            return nativeId;
        }

        public void setNativeId(String nativeId) {
            this.nativeId = nativeId;
        }
    }

    public static class AdsSettings {
        @SerializedName("banner_enabled")
        @Expose
        private boolean bannerEnabled;

        @SerializedName("interstitial_enabled")
        @Expose
        private boolean interstitialEnabled;

        @SerializedName("rewarded_enabled")
        @Expose
        private boolean rewardedEnabled;

        @SerializedName("native_enabled")
        @Expose
        private boolean nativeEnabled;

        @SerializedName("interstitial_clicks")
        @Expose
        private int interstitialClicks;

        @SerializedName("native_lines")
        @Expose
        private int nativeLines;

        @SerializedName("banner_type")
        @Expose
        private String bannerType;

        @SerializedName("interstitial_type")
        @Expose
        private String interstitialType;

        @SerializedName("native_type")
        @Expose
        private String nativeType;

        public boolean isBannerEnabled() {
            return bannerEnabled;
        }

        public void setBannerEnabled(boolean bannerEnabled) {
            this.bannerEnabled = bannerEnabled;
        }

        public boolean isInterstitialEnabled() {
            return interstitialEnabled;
        }

        public void setInterstitialEnabled(boolean interstitialEnabled) {
            this.interstitialEnabled = interstitialEnabled;
        }

        public boolean isRewardedEnabled() {
            return rewardedEnabled;
        }

        public void setRewardedEnabled(boolean rewardedEnabled) {
            this.rewardedEnabled = rewardedEnabled;
        }

        public boolean isNativeEnabled() {
            return nativeEnabled;
        }

        public void setNativeEnabled(boolean nativeEnabled) {
            this.nativeEnabled = nativeEnabled;
        }

        public int getInterstitialClicks() {
            return interstitialClicks;
        }

        public void setInterstitialClicks(int interstitialClicks) {
            this.interstitialClicks = interstitialClicks;
        }

        public int getNativeLines() {
            return nativeLines;
        }

        public void setNativeLines(int nativeLines) {
            this.nativeLines = nativeLines;
        }

        public String getBannerType() {
            return bannerType;
        }

        public void setBannerType(String bannerType) {
            this.bannerType = bannerType;
        }

        public String getInterstitialType() {
            return interstitialType;
        }

        public void setInterstitialType(String interstitialType) {
            this.interstitialType = interstitialType;
        }

        public String getNativeType() {
            return nativeType;
        }

        public void setNativeType(String nativeType) {
            this.nativeType = nativeType;
        }
    }
}