package julianh06.wynnextras.features.crafting;

import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.crafting.model.*;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4i;

import java.util.*;
import java.util.stream.Collectors;

public record CraftingResult(
        Recipe recipe,
        CraftableType type,
        List<StatPossibleValues> possibleValues,
        GearRequirements requirements,
        RangedValue health,
        RangedValue durability,
        Integer powderSlots,
        Integer charges,
        RangedValue duration,
        Map<DamageType, Vector4i> damage
) {
    private static final Map<String, Integer> WYNNBUILDER_STAT_ORDER = createWynnBuilderStatOrder();

    public List<Text> getTooltip() {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal("Crafting " + type.getDisplayName()).formatted(Formatting.DARK_AQUA));
        addBlank(tooltip);

        if (health != null && type.canHaveHealth()) {
            tooltip.add(Text.literal("Health: " + health.low() + "-" + health.high()).formatted(Formatting.RED));
            addBlank(tooltip);
        }

        if (type.isWeapon()) {
            addDamage(tooltip);
        }

        addReqs(tooltip);

        addIds(tooltip);

        if (type.isArmour()) {
            addBaseFormat(tooltip, "Powder Slots", powderSlots);
        }

        if (type.hasDurability()) {
            addBaseFormat(tooltip, "Durability", durability);
        }

        if (type.isConsumable()) {
            addBaseFormat(tooltip, "Charges", charges);
            addBaseFormat(tooltip, "Duration", duration);
        }

        return tooltip;
    }

    private void addDamage(List<Text> tooltip) {
        for (DamageType damageType : List.of(DamageType.NEUTRAL, DamageType.EARTH, DamageType.THUNDER,
                DamageType.WATER, DamageType.FIRE, DamageType.AIR)) {
            Vector4i value = damage.get(damageType);
            if (value == null) continue;
            MutableText append = applyElementFormatting(damageType.name()).append(" Damage: ")
                    .append(String.valueOf(value.x)).append("-")
                    .append(String.valueOf(value.y)).append("➜")
                    .append(String.valueOf(value.z)).append("-")
                    .append(String.valueOf(value.w));
            tooltip.add(append);
        }
        if (!damage.isEmpty()) {
            addBlank(tooltip);
        }
    }

    public void addReqs(List<Text> tooltip) {
        if (requirements.classType().isPresent() && requirements.classType().get() != ClassType.NONE) {
            MutableText suffix = Text.literal(
                    requirements.classType().get().getActualName(false) + "/" +
                            requirements.classType().get().getActualName(true)
            );
            tooltip.add(Text.literal("Class Req: ").append(suffix));
        }
        addBaseFormat(tooltip, "Combat Level Min", new RangedValue(requirements.level() - 2, requirements.level()));
        for (Pair<Skill, Integer> req : requirements.skills()) {
            String name = req.a().name().charAt(0) + req.a().name().substring(1).toLowerCase();
            addBaseFormat(tooltip, name + " Min", req.b());
        }
        addBlank(tooltip);
    }

    private void addIds(List<Text> tooltip) {
        for (StatPossibleValues id : sortedPossibleValues()) {
            String unit = id.statType().getUnit().getDisplayName();

            if (id.range().low() == 0 && id.range().high() == 0) continue;

            Formatting lowColor = id.range().low() >= 0 || id.statType().displayAsInverted() ? Formatting.GREEN : Formatting.RED;
            Formatting highColor = id.range().high() >= 0 || id.statType().displayAsInverted() ? Formatting.GREEN : Formatting.RED;

            Text lowText = Text.literal(id.range().low() + unit).formatted(lowColor);
            Text highText = Text.literal(id.range().high() + unit).formatted(highColor);

            tooltip.add(Text.literal("")
                    .append(lowText)
                    .append(" ")
                    .append(applyElementFormatting(id.statType().getDisplayName()))
                    .append(unit).append(": ")
                    .append(highText));
        }
        if (!possibleValues.isEmpty()) {
            addBlank(tooltip);
        }
    }

    private List<StatPossibleValues> sortedPossibleValues() {
        return possibleValues.stream()
                .sorted(Comparator
                        .comparingInt((StatPossibleValues value) -> statOrder(value.statType()))
                        .thenComparing(value -> value.statType().getDisplayName()))
                .toList();
    }

    private static int statOrder(StatType statType) {
        Integer order = WYNNBUILDER_STAT_ORDER.get(statType.getInternalRollName());
        if (order != null) return order;
        order = WYNNBUILDER_STAT_ORDER.get(statType.getApiName());
        if (order != null) return order;
        return WYNNBUILDER_STAT_ORDER.getOrDefault(statType.getKey(), Integer.MAX_VALUE);
    }

    private static Map<String, Integer> createWynnBuilderStatOrder() {
        String[] wynnBuilderIds = {
                "str", "dex", "int", "def", "agi",
                "hpBonus", "hprRaw", "hprPct", "healPct", "mr", "ms", "ref", "thorns", "ls",
                "poison", "expd", "spd", "atkTier",
                "sdRaw", "nSdRaw", "rSdRaw", "sdPct", "nSdPct", "rSdPct",
                "mdRaw", "nMdRaw", "rMdRaw", "mdPct", "nMdPct", "rMdPct",
                "damRaw", "nDamRaw", "rDamRaw", "damPct", "nDamPct", "rDamPct",
                "fSdRaw", "wSdRaw", "aSdRaw", "tSdRaw", "eSdRaw",
                "fSdPct", "wSdPct", "aSdPct", "tSdPct", "eSdPct",
                "fMdRaw", "wMdRaw", "aMdRaw", "tMdRaw", "eMdRaw",
                "fMdPct", "wMdPct", "aMdPct", "tMdPct", "eMdPct",
                "fDamRaw", "wDamRaw", "aDamRaw", "tDamRaw", "eDamRaw",
                "fDamPct", "wDamPct", "aDamPct", "tDamPct", "eDamPct",
                "fDefPct", "wDefPct", "aDefPct", "tDefPct", "eDefPct",
                "critDamPct", "rDefPct",
                "spPct1", "spRaw1", "spPct2", "spRaw2", "spPct3", "spRaw3", "spPct4", "spRaw4",
                "sprint", "sprintReg", "jh", "xpb", "lb", "lq", "spRegen", "eSteal",
                "gXp", "gSpd", "kb", "weakenEnemy", "slowEnemy", "maxMana", "mainAttackRange"
        };
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < wynnBuilderIds.length; i++) {
            result.put(wynnBuilderIds[i], i);
        }
        putAlias(result, "STRENGTH", "str");
        putAlias(result, "STRENGTHPOINTS", "str");
        putAlias(result, "DEXTERITY", "dex");
        putAlias(result, "DEXTERITYPOINTS", "dex");
        putAlias(result, "INTELLIGENCE", "int");
        putAlias(result, "INTELLIGENCEPOINTS", "int");
        putAlias(result, "DEFENCE", "def");
        putAlias(result, "DEFENSE", "def");
        putAlias(result, "DEFENCEPOINTS", "def");
        putAlias(result, "DEFENSEPOINTS", "def");
        putAlias(result, "AGILITY", "agi");
        putAlias(result, "AGILITYPOINTS", "agi");
        putAlias(result, "HEALTHREGEN", "hprRaw");
        putAlias(result, "HEALTH_REGEN_RAW", "hprRaw");
        putAlias(result, "HEALTHREGENRAW", "hprRaw");
        putAlias(result, "HEALTHREGENPERCENT", "hprPct");
        putAlias(result, "HEALTH_REGEN_PERCENT", "hprPct");
        putAlias(result, "MANAREGEN", "mr");
        putAlias(result, "MANA_REGEN", "mr");
        putAlias(result, "SPELLDAMAGE", "sdPct");
        putAlias(result, "SPELLDAMAGERAW", "sdRaw");
        putAlias(result, "DAMAGEBONUS", "mdPct");
        putAlias(result, "DAMAGEBONUSRAW", "mdRaw");
        putAlias(result, "WATERSPELLDAMAGE", "wSdPct");
        putAlias(result, "WATERSPELLDAMAGERAW", "wSdRaw");
        putAlias(result, "WATERDAMAGEBONUS", "wDamPct");
        putAlias(result, "WATERDAMAGEBONUSRAW", "wDamRaw");
        return Map.copyOf(result);
    }

    private static void putAlias(Map<String, Integer> result, String alias, String wynnBuilderId) {
        Integer order = result.get(wynnBuilderId);
        if (order != null) result.put(alias, order);
    }

    public void addBaseFormat(List<Text> tooltip, String name, RangedValue value) {
        String line = name + ": " + value.low() + "-" + value.high();
        tooltip.add(Text.of(applyElementFormatting(line)));
    }

    public void addBaseFormat(List<Text> tooltip, String name, int value) {
        String line = name + ": " + value;
        tooltip.add(Text.of(applyElementFormatting(line)));
    }

    public void addBlank(List<Text> tooltip) {
        tooltip.add(Text.of(""));
    }

    private MutableText applyElementFormatting(String text) {
        MutableText result = Text.empty();
        String[] words = text.split(" ");

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            Text wordText = switch (word) {
                case "Earth" -> Text.literal("✤ ").formatted(Formatting.DARK_GREEN)
                        .append(Text.literal(word));
                case "Thunder" -> Text.literal("✦ ").formatted(Formatting.YELLOW)
                        .append(Text.literal(word));
                case "Water" -> Text.literal("❉ ").formatted(Formatting.AQUA)
                        .append(Text.literal(word));
                case "Fire" -> Text.literal("✹ ").formatted(Formatting.RED)
                        .append(Text.literal(word));
                case "Air" -> Text.literal("❋ ").formatted(Formatting.WHITE)
                        .append(Text.literal(word));
                case "Neutral" -> Text.literal("✣ ").formatted(Formatting.GOLD)
                        .append(Text.literal(word));
                case "NEUTRAL" -> Text.literal("✣ Neutral").formatted(Formatting.GOLD);
                case "Health", "Health:" -> Text.literal("♥ ").formatted(Formatting.RED)
                        .append(Text.literal(word));
                default -> Text.literal(word);
            };

            result.append(wordText);
            if (i < words.length - 1) result.append(" ");
        }

        return result;
    }

    /*
    public static List<Text> modifyTooltip(List<Text> tooltips, ItemStack itemStack, CraftedGearItem crafted) {
        if (!tooltips.isEmpty()) {

        }
    }

     */

    public String getAllInfoString() {
        return "Recipe{" +
                "recipe=" + recipe +
                ", type=" + type +
                ", possibleValues=" + possibleValues +
                ", requirements=" + requirements +
                ", health=" + health +
                ", durability=" + durability +
                ", powderSlots=" + powderSlots +
                ", charges=" + charges +
                ", duration=" + duration +
                ", damage=" + damage +
                '}';
    }

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder();
        for (Text line : getTooltip()) {
            sb.append(line.getString()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CraftingResult that = (CraftingResult) o;

        // Compare non-list fields first
        if (type != that.type) return false;
        if (!Objects.equals(powderSlots, that.powderSlots)) return false;
        if (!Objects.equals(charges, that.charges)) return false;
        if (!Objects.equals(health, that.health)) return false;
        if (!Objects.equals(durability, that.durability)) return false;
        if (!Objects.equals(duration, that.duration)) return false;
        if (!Objects.equals(damage, that.damage)) return false;

        // Compare requirements using custom matcher
        if (!requirementsMatch(requirements, that.requirements, false)) return false;

        // Compare possibleValues, filtering out zero ranges
        Set<StatPossibleValues> filteredThis = filterZeroStats(possibleValues);
        Set<StatPossibleValues> filteredThat = filterZeroStats(that.possibleValues);

        return filteredThis.equals(filteredThat);
    }

    @Override
    public int hashCode() {
        Set<StatPossibleValues> filtered = filterZeroStats(possibleValues);
        return Objects.hash(type, filtered,
                requirementsHash(requirements),
                health, durability, powderSlots, charges, duration, damage);
    }

    public static boolean requirementsMatch(GearRequirements a, GearRequirements b, boolean debug) {
        //if (Math.abs(a.level() - b.level()) > 2) return false;
        if (!Objects.equals(a.classType(), b.classType())) return false;
        Map<Skill, Integer> aSkills = filterNonZeroSkills(a.skills());
        Map<Skill, Integer> bSkills = filterNonZeroSkills(b.skills());
        boolean skillsMatch = aSkills.equals(bSkills);
        if (debug) {
            WynnExtras.LOGGER.info("aSkills: " + aSkills);
            WynnExtras.LOGGER.info("bSkills: " + bSkills);
            WynnExtras.LOGGER.info("skillsMatch: " + skillsMatch);
        }
        return skillsMatch;
    }

    private static int requirementsHash(GearRequirements req) {
        if (req == null) return 0;
        return Objects.hash(
                req.level(),
                req.classType(),
                filterNonZeroSkills(req.skills())
        );
    }

    //TODO check if the recipe can pass through 0
    private static Set<StatPossibleValues> filterZeroStats(List<StatPossibleValues> stats) {
        if (stats == null) return Set.of();
        return stats.stream()
                .filter(stat -> stat != null &&
                        !(stat.range().low() == 0 && stat.range().high() == 0))
                .collect(Collectors.toSet());
    }

    private static Map<Skill, Integer> filterNonZeroSkills(List<Pair<Skill, Integer>> skills) {
        if (skills == null) return Map.of();
        return skills.stream()
                .filter(pair -> pair != null && pair.b() != null && pair.b() != 0)
                .collect(Collectors.toMap(Pair::a, Pair::b));
    }
}
