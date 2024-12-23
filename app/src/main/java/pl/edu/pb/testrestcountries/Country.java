package pl.edu.pb.testrestcountries;

import com.google.gson.annotations.SerializedName;

public class Country {
    @SerializedName("name")
    public Name name;

    @SerializedName("flags")
    public Flags flags;

    public static class Name {
        @SerializedName("official")
        public String official; // Oficjalna nazwa kraju

        @SerializedName("common")
        public String common; // Powszechnie używana nazwa kraju
    }

    public static class Flags {
        @SerializedName("png")
        public String png; // URL flagi w formacie PNG

        @SerializedName("svg")
        public String svg; // URL flagi w formacie SVG
    }
}
