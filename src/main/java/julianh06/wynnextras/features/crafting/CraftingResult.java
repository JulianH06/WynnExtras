package julianh06.wynnextras.features.crafting;

import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
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
    public List<Text> getTooltip() {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal("Crafting " + type.getDisplayName()).formatted(Formatting.DARK_AQUA));
        addBlank(tooltip);

        if (health != null && !type.isWeapon()) {
            addBaseFormat(tooltip, "Health", health);
            addBlank(tooltip);
        }

        if (type.isWeapon()) {
            addDamage(tooltip);
        }

        addReqs(tooltip);
        addBlank(tooltip);

        addIds(tooltip);
        addBlank(tooltip);

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
        for (Map.Entry<DamageType, Vector4i> entry : damage.entrySet()) {
            MutableText append = applyElementFormatting(entry.getKey().name()).append(" Damage: ")
                    .append(String.valueOf(entry.getValue().x)).append("-")
                    .append(String.valueOf(entry.getValue().y)).append("➜")
                    .append(String.valueOf(entry.getValue().z)).append("-")
                    .append(String.valueOf(entry.getValue().w));
            tooltip.add(append);
        }
    }

    public void addReqs(List<Text> tooltip) {
        addBaseFormat(tooltip, "Combat Level Min", new RangedValue(requirements.level() - 2, requirements.level()));
        for (Pair<Skill, Integer> req : requirements.skills()) {
            String name = req.a().name().charAt(0) + req.a().name().substring(1).toLowerCase();
            addBaseFormat(tooltip, name + " Min", req.b());
        }
    }

    private void addIds(List<Text> tooltip) {
        for (StatPossibleValues id : possibleValues) {
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

    public boolean equalsIgnoreDurability(CraftedGearItem targetItem, boolean debug) {
        if (this.type != CraftableType.fromGearType(targetItem.getGearType())) {
            if (debug) System.out.println("different type");
            return false;
        }

        List<StatPossibleValues> targetIds = targetItem.getPossibleValues();

        Set<StatType> targetTypes = targetIds.stream().map(StatPossibleValues::statType).collect(Collectors.toSet());
        Set<StatType> thisTypes = possibleValues.stream().map(StatPossibleValues::statType).collect(Collectors.toSet());
        if (!targetTypes.equals(thisTypes)) return false;

        // Check each possible value against target
        Map<StatType, Integer> targetMap = targetIds.stream().collect(Collectors.toMap(StatPossibleValues::statType, StatPossibleValues::baseValue));
        for (StatPossibleValues possible : possibleValues) {
            Integer target = targetMap.get(possible.statType());
            if (possible.range().low() == 0 && possible.range().high() == 0) continue;
            if (target == null || !possible.range().inRange(target)) {
                //if (debug) System.out.println("different id for " + possible + " target: " + target + "\n");
                return false;
            }
        }

        if (!requirementsMatch(targetItem.getRequirements(), this.requirements, debug)) {
            if (debug) System.out.println("different reqs\n" + targetItem.getRequirements() + "\n" + this.requirements);
            return false;
        }

       /*
       if(!this.health.inRange(targetItem.getHealth())) {
           System.out.println("health out of range\n" + targetItem.getHealth() + "\n" + this.health);
           return false;
       }
        */
        List<Pair<DamageType, RangedValue>> damages = targetItem.getDamages(); // TODO

        return true;
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
            System.out.println("aSkills: " + aSkills);
            System.out.println("bSkills: " + bSkills);
            System.out.println("skillsMatch: " + skillsMatch);
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
