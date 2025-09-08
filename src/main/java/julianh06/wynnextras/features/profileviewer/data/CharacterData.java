package julianh06.wynnextras.features.profileviewer.data;

import java.util.List;
import java.util.Map;

public class CharacterData {
    private String type;
    private String nickname;
    private int level;
    private long xp;
    private int xpPercent;
    private int totalLevel;
    private int wars;
    private float playtime;
    private int mobsKilled;
    private int chestsFound;
    private int blocksWalked;
    private int itemsIdentified;
    private int logins;
    private int deaths;
    private int discoveries;
    private PvP pvp;
    private List<String> gamemode;
    private SkillPoints skillPoints;
    private Map<String, Profession> professions;
    private Dungeons dungeons;
    private Raids raids;
    private List<String> quests;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    public int getXpPercent() {
        return xpPercent;
    }

    public void setXpPercent(int xpPercent) {
        this.xpPercent = xpPercent;
    }

    public int getTotalLevel() {
        return totalLevel;
    }

    public void setTotalLevel(int totalLevel) {
        this.totalLevel = totalLevel;
    }

    public int getWars() {
        return wars;
    }

    public void setWars(int wars) {
        this.wars = wars;
    }

    public float getPlaytime() {
        return playtime;
    }

    public void setPlaytime(float playtime) {
        this.playtime = playtime;
    }

    public int getMobsKilled() {
        return mobsKilled;
    }

    public void setMobsKilled(int mobsKilled) {
        this.mobsKilled = mobsKilled;
    }

    public int getChestsFound() {
        return chestsFound;
    }

    public void setChestsFound(int chestsFound) {
        this.chestsFound = chestsFound;
    }

    public int getBlocksWalked() {
        return blocksWalked;
    }

    public void setBlocksWalked(int blocksWalked) {
        this.blocksWalked = blocksWalked;
    }

    public int getItemsIdentified() {
        return itemsIdentified;
    }

    public void setItemsIdentified(int itemsIdentified) {
        this.itemsIdentified = itemsIdentified;
    }

    public int getLogins() {
        return logins;
    }

    public void setLogins(int logins) {
        this.logins = logins;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getDiscoveries() {
        return discoveries;
    }

    public void setDiscoveries(int discoveries) {
        this.discoveries = discoveries;
    }

    public PvP getPvp() {
        return pvp;
    }

    public void setPvp(PvP pvp) {
        this.pvp = pvp;
    }

    public List<String> getGamemode() {
        return gamemode;
    }

    public void setGamemode(List<String> gamemode) {
        this.gamemode = gamemode;
    }

    public SkillPoints getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(SkillPoints skillPoints) {
        this.skillPoints = skillPoints;
    }

    public Map<String, Profession> getProfessions() {
        return professions;
    }

    public void setProfessions(Map<String, Profession> professions) {
        this.professions = professions;
    }

    public Dungeons getDungeons() {
        return dungeons;
    }

    public void setDungeons(Dungeons dungeons) {
        this.dungeons = dungeons;
    }

    public Raids getRaids() {
        return raids;
    }

    public void setRaids(Raids raids) {
        this.raids = raids;
    }

    public List<String> getQuests() {
        return quests;
    }

    public void setQuests(List<String> quests) {
        this.quests = quests;
    }
}

