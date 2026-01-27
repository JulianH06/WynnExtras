package julianh06.wynnextras.features.profileviewer.data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private String username;
    private boolean online;
    private String server;
    private UUID activeCharacter;
    private Object nickname;
    private UUID uuid;
    private String rank;
    private String rankBadge;
    private LegacyRankColour legacyRankColour;
    private Object shortenedRank;
    private String supportRank;
    private Object veteran;
    private OffsetDateTime lastJoin;
    private Guild guild;
    private Map<String, Long> ranking;
    private Map<String, Long> previousRanking;
    private OffsetDateTime firstJoin;
    private double playtime;
    private Global globalData;
    private FeaturedStats featuredStats;
    private String wallpaper;
    private String avatar;
    private Restrictions restrictions;
    private Map<String, CharacterData> characters;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public UUID getActiveCharacter() {
        return activeCharacter;
    }

    public void setActiveCharacter(UUID activeCharacter) {
        this.activeCharacter = activeCharacter;
    }

    public Object getNickname() {
        return nickname;
    }

    public void setNickname(Object nickname) {
        this.nickname = nickname;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getRankBadge() {
        return rankBadge;
    }

    public void setRankBadge(String rankBadge) {
        this.rankBadge = rankBadge;
    }

    public LegacyRankColour getLegacyRankColour() {
        return legacyRankColour;
    }

    public void setLegacyRankColour(LegacyRankColour legacyRankColour) {
        this.legacyRankColour = legacyRankColour;
    }

    public Object getShortenedRank() {
        return shortenedRank;
    }

    public void setShortenedRank(Object shortenedRank) {
        this.shortenedRank = shortenedRank;
    }

    public String getSupportRank() {
        return supportRank;
    }

    public void setSupportRank(String supportRank) {
        this.supportRank = supportRank;
    }

    public Object getVeteran() {
        return veteran;
    }

    public void setVeteran(Object veteran) {
        this.veteran = veteran;
    }

    public OffsetDateTime getLastJoin() {
        return lastJoin;
    }

    public void setLastJoin(OffsetDateTime lastJoin) {
        this.lastJoin = lastJoin;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public Map<String, Long> getRanking() {
        return ranking;
    }

    public void setRanking(Map<String, Long> ranking) {
        this.ranking = ranking;
    }

    public Map<String, Long> getPreviousRanking() {
        return previousRanking;
    }

    public void setPreviousRanking(Map<String, Long> previousRanking) {
        this.previousRanking = previousRanking;
    }

    public OffsetDateTime getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(OffsetDateTime firstJoin) {
        this.firstJoin = firstJoin;
    }

    public double getPlaytime() {
        return playtime;
    }

    public void setPlaytime(double playtime) {
        this.playtime = playtime;
    }

    public Global getGlobalData() {
        return globalData;
    }

    public void setGlobalData(Global globalData) {
        this.globalData = globalData;
    }

    public FeaturedStats getFeaturedStats() {
        return featuredStats;
    }

    public void setFeaturedStats(FeaturedStats featuredStats) {
        this.featuredStats = featuredStats;
    }

    public String getWallpaper() {
        return wallpaper;
    }

    public void setWallpaper(String wallpaper) {
        this.wallpaper = wallpaper;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Restrictions getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(Restrictions restrictions) {
        this.restrictions = restrictions;
    }

    public Map<String, CharacterData> getCharacters() {
        return characters;
    }

    public void setCharacters(Map<String, CharacterData> characters) {
        this.characters = characters;
    }
}