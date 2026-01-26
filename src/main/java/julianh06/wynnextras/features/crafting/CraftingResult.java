package julianh06.wynnextras.features.crafting;

import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Vector4i;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CraftingResult {
    public CraftableType type;
    public List<StatPossibleValues> possibleValues;
    public GearRequirements requirements;

    public RangedValue health;
    public Integer powderSlots;
    public RangedValue durability;

    public RangedValue duration;
    public Integer charges;

    public Map<DamageType, Vector4i> damage;

    public CraftingResult(
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
        this.type = type;
        this.possibleValues = possibleValues;
        this.requirements = requirements;
        this.health = health;
        this.durability = durability;
        this.powderSlots = powderSlots;
        this.charges = charges;
        this.duration = duration;
        this.damage = damage;
    }

    public List<Text> getTooltip() {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal("Crafting " + type.getDisplayName()).formatted(Formatting.DARK_AQUA));
        addBlank(tooltip);

        if (type.isArmour()) {
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
            if (req.b() <= 0) continue;
            String name = req.a().name().charAt(0) + req.a().name().substring(1).toLowerCase();
            addBaseFormat(tooltip, applyElementFormatting(name) + " Min", req.b());
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
}
