package my.cinemax.app.free.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Plan implements Parcelable {
        @SerializedName("id")
        @Expose
        private Integer id;

        @SerializedName("title")
        @Expose
        private String title;

        @SerializedName("description")
        @Expose
        private String description;

        @SerializedName("discount")
        @Expose
        private String discount;

        @SerializedName("price")
        @Expose
        private double price;

        @SerializedName("duration")
        @Expose
        private String duration;

        @SerializedName("enabled")
        @Expose
        private boolean enabled = true;

        @SerializedName("billing_type")
        @Expose
        private String billingType = "monthly";

        @SerializedName("currency")
        @Expose
        private String currency = "USD";

        @SerializedName("features")
        @Expose
        private List<String> features;

        public Plan() {

        }

    protected Plan(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readInt();
        }
        title = in.readString();
        description = in.readString();
        discount = in.readString();
        price = in.readDouble();
        duration = in.readString();
        enabled = in.readByte() != 0;
        billingType = in.readString();
        currency = in.readString();
        features = in.createStringArrayList();
    }

    public static final Creator<Plan> CREATOR = new Creator<Plan>() {
        @Override
        public Plan createFromParcel(Parcel in) {
            return new Plan(in);
        }

        @Override
        public Plan[] newArray(int size) {
            return new Plan[size];
        }
    };

    public void setId(Integer id) {
            this.id = id;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setDiscount(String discount) {
            this.discount = discount;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public Integer getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getDiscount() {
            return discount;
        }

        public double getPrice() {
            return price;
        }

        public String getDuration() {
            return duration;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBillingType() {
            return billingType;
        }

        public void setBillingType(String billingType) {
            this.billingType = billingType;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public List<String> getFeatures() {
            return features;
        }

        public void setFeatures(List<String> features) {
            this.features = features;
        }

        @Override
        public int describeContents() {
            return 0;
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
            dest.writeString(description);
                    dest.writeString(discount);
        dest.writeDouble(price);
        dest.writeString(duration);
        dest.writeByte((byte) (enabled ? 1 : 0));
        dest.writeString(billingType);
        dest.writeString(currency);
        dest.writeStringList(features);
        }
}
