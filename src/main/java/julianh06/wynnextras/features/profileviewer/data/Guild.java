package julianh06.wynnextras.features.profileviewer.data;

public class Guild {
    private String uuid;
    private String name;
    private String prefix;
    private String rank;
    private String rankStars;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getRankStars() {
        return rankStars;
    }

    public void setRankStars(String rankStars) {
        this.rankStars = rankStars;
    }
}

