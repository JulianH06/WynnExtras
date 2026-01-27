package julianh06.wynnextras.features.profileviewer.data;

import com.google.gson.annotations.SerializedName;

public class FeaturedStats {
    private String firstJoin;
    private double playtime;
    @SerializedName("globalData.totalLevel")
    private int totalLevel;
    @SerializedName("globalData.mobsKilled")
    private int mobsKilled;
    @SerializedName("globalData.completedQuests")
    private int completedQuests;

    public String getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(String firstJoin) {
        this.firstJoin = firstJoin;
    }

    public double getPlaytime() {
        return playtime;
    }

    public void setPlaytime(double playtime) {
        this.playtime = playtime;
    }

    public int getTotalLevel() {
        return totalLevel;
    }

    public void setTotalLevel(int totalLevel) {
        this.totalLevel = totalLevel;
    }

    public int getMobsKilled() {
        return mobsKilled;
    }

    public void setMobsKilled(int mobsKilled) {
        this.mobsKilled = mobsKilled;
    }

    public int getCompletedQuests() {
        return completedQuests;
    }

    public void setCompletedQuests(int completedQuests) {
        this.completedQuests = completedQuests;
    }
}

