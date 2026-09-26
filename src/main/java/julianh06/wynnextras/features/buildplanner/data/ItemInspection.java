package julianh06.wynnextras.features.buildplanner.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ItemInspection {
    private static final String[] SKILLS = {"str", "dex", "int", "def", "agi"};
    private static final String[] SKILL_NAMES = {"Strength", "Dexterity", "Intelligence", "Defense", "Agility"};
    private static final String[] ELEMENTS = {"Earth", "Thunder", "Water", "Fire", "Air"};
    private static final String[] PREFIXES = {"e", "t", "w", "f", "a"};

    private ItemInspection() {}

    public static List<Row> rows(WynnItem item, String powders) {
        return rows(item, powders, false);
    }

    public static List<Row> rows(WynnItem item, String powders, boolean ranges) {
        List<Row> rows = new ArrayList<>();
        if (!item.attackSpeed().isBlank()) {
            rows.add(row(readable(item.attackSpeed()) + " Attack Speed", "", Kind.TEXT));
        }
        if (item.stat("hp") != 0) {
            rows.add(row("consumable".equals(item.type()) ? "Healing" : "Health",
                    ranges && item.stat("hpLow") > 0 ? item.stat("hpLow") + "-" + item.stat("hp")
                            : Integer.toString(item.stat("hp")), Kind.INLINE));
        }
        for (int i = -1; i < 5; i++) {
            String prefix = i < 0 ? "n" : PREFIXES[i];
            int min = item.stat(prefix + "DamMin");
            int max = item.stat(prefix + "DamMax");
            if (min != 0 || max != 0) {
                rows.add(new Row((i < 0 ? "Neutral" : ELEMENTS[i]) + " Damage",
                        min + "-" + max, Kind.INLINE, i, false));
            }
        }
        for (int i = 0; i < 5; i++) {
            int value = item.stat(PREFIXES[i] + "Def");
            if (value != 0) {
                rows.add(new Row(ELEMENTS[i] + " Defense", Integer.toString(value), Kind.INLINE, i, false));
            }
        }
        rows.add(row("Combat Level Min", Integer.toString(item.stat(item.isCrafted() ? "lvlLow" : "lvl")), Kind.INLINE));
        if ("weapon".equals(item.type())) {
            rows.add(row("Class Req", AbilityTreeClass.fromWeaponSubtype(item.subType()).displayName(), Kind.INLINE));
        }
        for (int i = 0; i < 5; i++) {
            int requirement = item.stat(SKILLS[i] + "Req");
            if (requirement != 0) {
                rows.add(row(SKILL_NAMES[i] + " Min", Integer.toString(requirement), Kind.INLINE));
            }
        }
        rows.add(gap());
        for (int i = 0; i < 5; i++) {
            Identification id = item.identifications().get(SKILLS[i]);
            if (id != null && id.max() != 0) {
                rows.add(new Row(SKILL_NAMES[i], Integer.toString(id.max()), Kind.SKILL, -1, id.max() > 0));
            }
        }
        item.identifications().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String key = entry.getKey();
            int value = entry.getValue().max();
            if (List.of(SKILLS).contains(key) || value == 0) {
                return;
            }
            String label = readable(key) + (IdentificationUnits.unit(key) == IdentificationUnits.Unit.PERCENT ? " %" : "");
            String formatted = IdentificationUnits.format(key, value);
            if (formatted.startsWith("+")) {
                formatted = formatted.substring(1);
            }
            if (ranges && entry.getValue().min() != value) {
                String minimum = IdentificationUnits.format(key, entry.getValue().min());
                formatted = (minimum.startsWith("+") ? minimum.substring(1) : minimum) + " to " + formatted;
            }
            boolean reversed = key.matches("(?i)(raw)?[1-4](st|nd|rd|th)SpellCost");
            int element = -1;
            for (int i = 0; i < 5; i++) {
                if (key.startsWith(ELEMENTS[i].toLowerCase(Locale.ROOT))) {
                    element = i;
                }
            }
            rows.add(new Row(label, formatted, Kind.IDENTIFICATION, element, reversed ? value < 0 : value > 0));
        });
        rows.add(gap());
        if ("weapon".equals(item.type()) || "armour".equals(item.type())) {
            rows.add(row("Powder Slots", item.stat("powderSlots") + " [" + (powders == null ? "" : powders) + "]", Kind.INFO));
        }
        if (item.isCrafted()) {
            if ("consumable".equals(item.type())) {
                rows.add(row("Duration", item.stat("durationMin") + "-" + item.stat("durationMax") + "s", Kind.INLINE));
                rows.add(row("Charges", Integer.toString(item.stat("charges")), Kind.INLINE));
                if (item.stat("durationClamped") != 0) {
                    rows.add(row("Ingredient penalties reduce duration below 1s; shown at the 1s minimum.", "", Kind.WARNING));
                }
            } else {
                rows.add(row("Durability", item.stat("durabilityMin") + "-" + item.stat("durabilityMax"), Kind.INLINE));
            }
            rows.add(row("Material tiers", item.stat("materialTier1") + " / " + item.stat("materialTier2"), Kind.INLINE));
            rows.add(row("Code", item.craftedCode(), Kind.INLINE));
            if (!"consumable".equals(item.type())) {
                rows.add(row(ranges ? "Full durability; identification roll ranges." : "Max rolls; full durability.", "", Kind.NOTE));
            }
        }
        if (!item.majorIds().isEmpty()) {
            rows.add(gap());
            item.majorIds().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                rows.add(row(entry.getKey(), "", Kind.MAJOR));
                rows.add(row(entry.getValue(), "", Kind.TEXT));
            });
            rows.add(row("Special Major ID effects are not simulated.", "", Kind.WARNING));
        }
        if (!item.lore().isBlank()) {
            rows.add(gap());
            rows.add(row(item.lore(), "", Kind.LORE));
        }
        if (!item.restriction().isBlank() && !"none".equalsIgnoreCase(item.restriction())) {
            rows.add(row(item.restriction().replace('_', ' '), "", Kind.RESTRICTION));
        }
        rows.add(row(readable(item.tier()) + " " + item.subType(), "", Kind.RARITY));
        return List.copyOf(rows);
    }

    static String readable(String value) {
        String spaced = value.replace('_', ' ').replaceFirst("(?i)^raw(?=\\d)", "raw ")
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        return spaced.isBlank() ? "" : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static Row gap() {
        return row("", "", Kind.GAP);
    }

    private static Row row(String label, String value, Kind kind) {
        return new Row(label, value, kind, -1, false);
    }

    public record Row(String label, String value, Kind kind, int element, boolean beneficial) {}

    public enum Kind {
        GAP, TEXT, INLINE, SKILL, IDENTIFICATION, MODIFIER, INFO, NOTE, MAJOR, WARNING, LORE, RESTRICTION, RARITY
    }
}
