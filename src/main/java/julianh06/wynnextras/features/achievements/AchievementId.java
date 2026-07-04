package julianh06.wynnextras.features.achievements;

public enum AchievementId {
    CONTENT_COMPLETION("content.completion"),

    ASPECT_MAX_ALL("aspect.max.all"),
    ASPECT_MAX_ALL_MYTHIC("aspect.max.all.mythic"),
    ASPECT_MAX_ALL_FABLED("aspect.max.all.fabled"),
    ASPECT_MAX_ALL_LEGENDARY("aspect.max.all.legendary"),
    ASPECT_MAX_ALL_WARRIOR("aspect.max.all.warrior"),
    ASPECT_MAX_ALL_SHAMAN("aspect.max.all.shaman"),
    ASPECT_MAX_ALL_MAGE("aspect.max.all.mage"),
    ASPECT_MAX_ALL_ARCHER("aspect.max.all.archer"),
    ASPECT_MAX_ALL_ASSASSIN("aspect.max.all.assassin"),

    RAID_TNA("raid.tna"),
    RAID_NOTG("raid.notg"),
    RAID_NOL("raid.nol"),
    RAID_TWP("raid.twp"),
    RAID_TCC("raid.tcc"),

    PROF_GATHER_100("prof.gather.100"),
    PROF_GATHER_115("prof.gather.115"),
    PROF_GATHER_132("prof.gather.132"),
    PROF_CRAFT_100("prof.craft.100"),
    PROF_CRAFT_115("prof.craft.115"),
    PROF_CRAFT_132("prof.craft.132"),

    CLASS_LEVEL_120("class.level120"),
    CLASS_LEVEL_121("class.level121");

    private final String id;

    AchievementId(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}