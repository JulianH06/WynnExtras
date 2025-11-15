package julianh06.wynnextras.features.guildviewer.data;

import java.util.List;
import java.util.Map;

public class GuildData {
    public String uuid;
    public String name;
    public String prefix;
    public int level;
    public int xpPercent;
    public int territories;
    public int wars;
    public String created;
    public Members members;
    public int online;
    public Banner banner;
    public Map<String, SeasonRank> seasonRanks;

    public static class Members {
        public int total;
        public Map<String, Member> owner;
        public Map<String, Member> chief;
        public Map<String, Member> strategist;
        public Map<String, Member> captain;
        public Map<String, Member> recruiter;
        public Map<String, Member> recruit;
    }

    public static class Member {
        public String uuid;
        public String username;
        public String joined;
        public long contributed;
        public boolean online;
    }

    public static class Banner {
        public String base;
        public int tier;
        public String structure;
        public List<BannerLayer> layers;
    }

    public static class BannerLayer {
        public String colour;
        public String pattern;
    }

    public static class SeasonRank {
        public int rating;
        public int finalTerritories;
    }
}

