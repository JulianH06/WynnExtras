package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.data.BuildCalculator;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.ItemInspection;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.wynncraft.state.CharacterClass;
import julianh06.wynnextras.wynncraft.state.ProfessionState;
import julianh06.wynnextras.utils.enums.WEProfessionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

final class WynnItemTooltip {
    private final TextRenderer font;
    private final CharacterClass selectedClass;
    private final int level;
    private final int[] finalSkills;

    private WynnItemTooltip(TextRenderer font, CharacterClass selectedClass, int level, int[] finalSkills) {
        this.font = font;
        this.selectedClass = selectedClass;
        this.level = level;
        this.finalSkills = finalSkills;
    }

    static void render(PlannerScreen screen, DrawContext context, TextRenderer font, WynnItem item,
            int mouseX, int mouseY, CharacterClass selectedClass, int level, int[] finalSkills) {
        WynnItemTooltip tooltip = new WynnItemTooltip(font, selectedClass, level, finalSkills);
        String tier = item.tier() == null ? "normal" : item.tier().toLowerCase(Locale.ROOT);
        if ("set".equals(tier)) tier = "rare";
        screen.renderPlannerTooltip(context, tooltip.lines(item), Identifier.ofVanilla(tier), mouseX, mouseY);
    }

    static void renderIngredient(PlannerScreen screen, DrawContext context, TextRenderer font,
            CraftedItemCodec.Ingredient ingredient, int mouseX, int mouseY) {
        WynnItemTooltip tooltip = new WynnItemTooltip(font, CharacterClass.ARCHER, 121, new int[5]);
        Identifier style = Identifier.ofVanilla(switch (ingredient.tier()) {
            case 1 -> "unique";
            case 2 -> "rare";
            case 3 -> "legendary";
            default -> "normal";
        });
        screen.renderPlannerTooltip(context, tooltip.ingredientLines(ingredient), style, mouseX, mouseY);
    }

    static Card itemCard(TextRenderer font, WynnItem item) {
        return itemCard(font, item, item.displayName());
    }

    static Card itemCard(TextRenderer font, WynnItem item, String displayName) {
        CharacterClass abilityClass = "weapon".equals(item.type())
                ? CharacterClass.fromWeaponSubtype(item.subType()) : CharacterClass.ARCHER;
        WynnItemTooltip tooltip = new WynnItemTooltip(font, abilityClass, 121,
                new int[]{200, 200, 200, 200, 200});
        String tier = item.tier() == null ? "normal" : item.tier().toLowerCase(Locale.ROOT);
        if ("set".equals(tier)) tier = "rare";
        return new Card(font, tooltip.lines(item, displayName), Identifier.ofVanilla(tier));
    }

    static Card ingredientCard(TextRenderer font, CraftedItemCodec.Ingredient ingredient) {
        WynnItemTooltip tooltip = new WynnItemTooltip(font, CharacterClass.ARCHER, 121, new int[5]);
        return new Card(font, tooltip.ingredientLines(ingredient), Identifier.ofVanilla(switch (ingredient.tier()) {
            case 1 -> "unique";
            case 2 -> "rare";
            case 3 -> "legendary";
            default -> "normal";
        }));
    }

    static Card messageCard(TextRenderer font, String title, List<ItemInspection.Row> rows) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.empty());
        lines.add(text(title, "language/wynncraft", 0xFFFF5555));
        lines.add(Text.empty());
        for (ItemInspection.Row row : rows) {
            String value = row.value().isEmpty() ? row.label() : row.label() + ": " + row.value();
            lines.add(text(value, "language/wynncraft",
                    row.kind() == ItemInspection.Kind.WARNING ? 0xFFFFAA00 : 0xFFAAAAAA));
        }
        return new Card(font, lines, Identifier.ofVanilla("normal"));
    }

    private List<Text> lines(WynnItem item) {
        return lines(item, item.displayName());
    }

    private List<Text> lines(WynnItem item, String displayName) {
        int tooltipWidth = tooltipWidth(item, displayName);
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.empty());
        tooltip.add(nameLine(item, displayName));
        tooltip.add(typeLine(item));
        Text tags = tagsLine(item);
        if (!tags.getString().isBlank()) tooltip.add(tags);
        tooltip.add(Text.empty());

        int dividerColor = dividerColor(item.tier());
        if ("weapon".equals(item.type())) {
            String dps = String.format(Locale.US, "%,d", Math.round(BuildCalculator.weaponBaseDps(item, "")));
            tooltip.add(text(dps, "offset/wynncraft_quad/12", dividerColor)
                    .append(text(" DPS", "language/wynncraft", 0xFFFFFFFF)));
        } else {
            tooltip.add(text(signed(item.stat("hp")), "offset/wynncraft_quad/12", dividerColor)
                    .append(text(" Health", "language/wynncraft", 0xFFFFFFFF)));
        }

        if (!item.attackSpeed().isBlank()) {
            String speed = readable(item.attackSpeed());
            tooltip.add(text("\uE007", "tooltip/attribute/sprite", 0xFFFFFFFF)
                    .append(text(" " + speed + " ", "language/wynncraft", 0xFFAAAAAA))
                    .append(text("(" + attackSpeed(item.attackSpeed()) + " hits/s)",
                            "language/wynncraft", 0xFF555555)));
        }

        String[] damagePrefixes = {"n", "e", "t", "w", "f", "a"};
        String[] damageSprites = {"\uE005", "\uE000", "\uE001", "\uE002", "\uE003", "\uE004"};
        MutableText damages = Text.empty();
        for (int i = 0; i < damagePrefixes.length; i++) {
            int minimum = item.stat(damagePrefixes[i] + "DamMin");
            int maximum = item.stat(damagePrefixes[i] + "DamMax");
            if (minimum == 0 && maximum == 0) continue;
            damages.append(sprite(damageSprites[i]));
            damages.append(text(" " + minimum + "-" + maximum + " ", "language/wynncraft", 0xFFAAAAAA));
        }
        if (!damages.getString().isBlank()) tooltip.add(damages);

        String[] defensePrefixes = {"e", "t", "w", "f", "a"};
        String[] defenseSprites = {"\uE000", "\uE001", "\uE002", "\uE003", "\uE004"};
        MutableText defenses = Text.empty();
        for (int i = 0; i < defensePrefixes.length; i++) {
            int value = item.stat(defensePrefixes[i] + "Def");
            if (value == 0) continue;
            defenses.append(sprite(defenseSprites[i]));
            defenses.append(text(" " + signed(value) + " ", "language/wynncraft", 0xFFAAAAAA));
        }
        if (!defenses.getString().isBlank()) tooltip.add(defenses);
        tooltip.add(divider(item.tier()));

        String[] skillKeys = {"strReq", "dexReq", "intReq", "defReq", "agiReq"};
        if (Arrays.stream(skillKeys).anyMatch(key -> item.stat(key) != 0)) {
            tooltip.add(Text.empty());
            tooltip.add(centered(skillIcons(item, skillKeys), tooltipWidth));
            tooltip.add(Text.empty());
            tooltip.add(centered(skillValues(item, skillKeys), tooltipWidth));
            tooltip.add(Text.empty());
        }

        if ("weapon".equals(item.type())) {
            CharacterClass requiredClass = CharacterClass.fromWeaponSubtype(item.subType());
            tooltip.add(aligned(requirement(" Class Type", requiredClass == selectedClass),
                    text(requiredClass.displayName(), "language/wynncraft", 0xFFAAAAAA), tooltipWidth));
        }
        int requiredLevel = item.stat(item.isCrafted() ? "lvlLow" : "lvl");
        if (requiredLevel != 0) {
            tooltip.add(aligned(requirement(" Combat Level", level >= requiredLevel),
                    text(Integer.toString(requiredLevel), "language/wynncraft", 0xFFAAAAAA), tooltipWidth));
        }
        tooltip.add(divider(item.tier()));

        for (ItemInspection.Row row : ItemInspection.rows(item, "", true)) {
            if (row.kind() != ItemInspection.Kind.SKILL
                    && row.kind() != ItemInspection.Kind.IDENTIFICATION) continue;
            String value = row.value();
            if (row.beneficial() && !value.startsWith("+") && !value.startsWith("-")) value = "+" + value;
            tooltip.add(aligned(text(row.label().replaceFirst(" %$", "") + " ",
                            "language/wynncraft", 0xFFFFFFFF),
                    identificationValue(value, row.beneficial()), tooltipWidth));
        }
        if (("weapon".equals(item.type()) || "armour".equals(item.type()))
                && item.stat("powderSlots") > 0) {
            tooltip.add(aligned(text("Powder Slots ", "language/wynncraft", 0xFFFFFFFF),
                    text(Integer.toString(item.stat("powderSlots")), "language/wynncraft", 0xFFAAAAAA),
                    tooltipWidth));
        }
        if (item.isCrafted()) {
            if ("consumable".equals(item.type())) {
                tooltip.add(aligned(text("Duration ", "language/wynncraft", 0xFFFFFFFF),
                        text(item.stat("durationMin") + "-" + item.stat("durationMax") + "s",
                                "language/wynncraft", 0xFFAAAAAA), tooltipWidth));
                tooltip.add(aligned(text("Charges ", "language/wynncraft", 0xFFFFFFFF),
                        text(Integer.toString(item.stat("charges")), "language/wynncraft", 0xFFAAAAAA),
                        tooltipWidth));
            } else {
                tooltip.add(aligned(text("Durability ", "language/wynncraft", 0xFFFFFFFF),
                        text(item.stat("durabilityMin") + "-" + item.stat("durabilityMax"),
                                "language/wynncraft", 0xFFAAAAAA), tooltipWidth));
            }
            tooltip.add(aligned(text("Material Tiers ", "language/wynncraft", 0xFFFFFFFF),
                    text(item.stat("materialTier1") + " / " + item.stat("materialTier2"),
                            "language/wynncraft", 0xFFAAAAAA), tooltipWidth));
        }
        for (Map.Entry<String, String> majorId : item.majorIds().entrySet()) {
            tooltip.add(Text.empty());
            appendWrapped(tooltip, "◆ " + majorId.getKey() + ": " + majorId.getValue(), dividerColor(item.tier()));
        }
        if (!item.lore().isBlank()) {
            tooltip.add(Text.empty());
            appendWrapped(tooltip, item.lore(), 0xFFAAAAAA);
        }
        if (!item.restriction().isBlank() && !"none".equalsIgnoreCase(item.restriction())) {
            tooltip.add(Text.empty());
            String restriction = item.restriction().toLowerCase(Locale.ROOT).contains("untrad")
                    ? "Untradable" : readable(item.restriction());
            tooltip.add(centered(text(restriction, "language/wynncraft", 0xFFFF5555), tooltipWidth));
        }
        return tooltip;
    }

    private List<Text> ingredientLines(CraftedItemCodec.Ingredient ingredient) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal(ingredient.name()).formatted(Formatting.GRAY).append(ingredientTier(ingredient.tier())));
        tooltip.add(Text.literal("Crafting Ingredient").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.empty());
        if (ingredient.id() == 4000) {
            tooltip.add(Text.literal("Empty ingredient slot").formatted(Formatting.DARK_GRAY));
            return tooltip;
        }
        addIngredientRows(tooltip, ingredient.rows().stream()
                .filter(row -> row.kind() == ItemInspection.Kind.IDENTIFICATION).toList(), false);
        addIngredientRows(tooltip, ingredient.rows().stream()
                .filter(row -> row.label().startsWith("Effectiveness ")).toList(), true);
        addIngredientRows(tooltip, ingredient.rows().stream()
                .filter(row -> row.kind() == ItemInspection.Kind.MODIFIER
                        && !row.label().startsWith("Effectiveness ")).toList(), false);
        boolean craftingLevelMet = ingredient.professions().stream().map(WEProfessionType::fromString)
                .filter(java.util.Objects::nonNull)
                .anyMatch(profession -> ProfessionState.level(profession) >= ingredient.level());
        tooltip.add(Text.literal(craftingLevelMet ? "✔" : "✖")
                .formatted(craftingLevelMet ? Formatting.GREEN : Formatting.RED)
                .append(Text.literal(" Crafting Lv. Min: " + ingredient.level()).formatted(Formatting.GRAY)));
        for (String profession : ingredient.professions()) {
            WEProfessionType type = WEProfessionType.fromString(profession);
            boolean met = type != null && ProfessionState.level(type) >= ingredient.level();
            tooltip.add(Text.literal("   " + (met ? "✔" : "✖") + " ").formatted(Formatting.DARK_GRAY)
                    .append(Text.literal(professionIcon(profession) + " ").formatted(Formatting.WHITE))
                    .append(Text.literal(readable(profession)).formatted(Formatting.GRAY)));
        }
        return tooltip;
    }

    private static void addIngredientRows(List<Text> tooltip, List<ItemInspection.Row> rows, boolean positions) {
        if (rows.isEmpty()) return;
        for (ItemInspection.Row row : rows) {
            if (positions) {
                tooltip.add(ingredientValue(row, "Ingredient Effectiveness"));
                tooltip.add(Text.literal("(To ingredients " + positionDescription(row.label()) + " this one)")
                        .formatted(Formatting.GRAY));
            } else {
                String label = row.label().replaceFirst(" %$", "");
                if (label.endsWith(" Min")) label += ".";
                tooltip.add(ingredientValue(row, label));
            }
        }
        tooltip.add(Text.empty());
    }

    private static Text ingredientValue(ItemInspection.Row row, String label) {
        Formatting bright = row.beneficial() ? Formatting.GREEN : Formatting.RED;
        Formatting dark = row.beneficial() ? Formatting.DARK_GREEN : Formatting.DARK_RED;
        boolean percent = row.label().endsWith(" %") || label.equals("Ingredient Effectiveness");
        String value = row.value();
        int separator = value.indexOf(" to ");
        if (separator < 0) {
            if (percent && !value.endsWith("%")) value += "%";
            return Text.literal(value + " ").formatted(bright)
                    .append(Text.literal(label).formatted(Formatting.GRAY));
        }
        String minimum = value.substring(0, separator).replace("%", "");
        String maximum = value.substring(separator + 4).replace("%", "") + (percent ? "%" : "");
        return Text.literal(minimum + " ").formatted(bright)
                .append(Text.literal("to").formatted(dark))
                .append(Text.literal(" " + maximum).formatted(bright))
                .append(Text.literal(" " + label).formatted(Formatting.GRAY));
    }

    private static String positionDescription(String label) {
        return switch (label.substring("Effectiveness ".length())) {
            case "left" -> "to the left of";
            case "right" -> "to the right of";
            case "above" -> "above";
            case "under" -> "under";
            case "touching" -> "touching";
            case "notTouching" -> "not touching";
            default -> "near";
        };
    }

    private static String professionIcon(String profession) {
        return switch (profession.toUpperCase(Locale.ROOT)) {
            case "COOKING" -> "Ⓐ";
            case "JEWELING" -> "Ⓓ";
            case "SCRIBING" -> "Ⓔ";
            case "TAILORING" -> "Ⓕ";
            case "WEAPONSMITHING" -> "Ⓖ";
            case "ARMOURING" -> "Ⓗ";
            case "WOODWORKING" -> "Ⓘ";
            case "ALCHEMISM" -> "Ⓛ";
            default -> "";
        };
    }

    private static Text ingredientTier(int tier) {
        MutableText result = Text.literal(" [").formatted(tier == 1 ? Formatting.GOLD
                : tier == 2 ? Formatting.DARK_PURPLE : tier == 3 ? Formatting.DARK_AQUA : Formatting.GRAY);
        if (tier == 1) result.append(Text.literal("✫").formatted(Formatting.YELLOW))
                .append(Text.literal("✫✫").formatted(Formatting.DARK_GRAY));
        else if (tier == 2) result.append(Text.literal("✫").formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal("✫✫").formatted(Formatting.DARK_GRAY));
        else if (tier == 3) result.append(Text.literal("✫✫").formatted(Formatting.AQUA))
                .append(Text.literal("✫").formatted(Formatting.DARK_AQUA));
        else result.append(Text.literal("✫✫✫").formatted(Formatting.DARK_GRAY));
        return result.append(Text.literal("]").formatted(tier == 1 ? Formatting.GOLD
                : tier == 2 ? Formatting.DARK_PURPLE : tier == 3 ? Formatting.DARK_AQUA : Formatting.GRAY));
    }

    private int tooltipWidth(WynnItem item, String displayName) {
        int width = Math.max(140, font.getWidth(displayName) + 30);
        for (ItemInspection.Row row : ItemInspection.rows(item, "", true)) {
            if (row.kind() == ItemInspection.Kind.SKILL || row.kind() == ItemInspection.Kind.IDENTIFICATION) {
                width = Math.max(width, font.getWidth(row.label() + "  " + row.value()));
            }
        }
        return width;
    }

    private Text nameLine(WynnItem item, String displayName) {
        MutableText line = shadowText("\uDAFF\uDFF0", "language/wynncraft", 0xFFFFFFFF);
        line.append(text(emblemFrame(item.emblem()), "tooltip/emblem/frame", 0xFFFFFFFF));
        line.append(text("\uDAFF\uDFCF", "language/wynncraft", 0xFFFFFFFF));
        String sprite = typeSprite(item);
        line.append(sprite.isEmpty() ? space(32) : text(sprite, "tooltip/emblem/sprite", 0xFF00EB1C));
        line.append(space(5));
        line.append(text(displayName, "language/wynncraft", tierColor(item.tier())));
        return line;
    }

    private Text typeLine(WynnItem item) {
        MutableText line = shadowText("", "language/wynncraft", dividerColor(item.tier())).append(space(38));
        line.append(banner(readable(item.tier()), tierColor(item.tier()), 0xFF000000));
        line.append(space(1));
        line.append(banner(itemTypeName(item), dividerColor(item.tier()), 0xFF000000));
        return line;
    }

    private Text tagsLine(WynnItem item) {
        String[] prefixes = {"e", "t", "w", "f", "a"};
        String[] base = {"\uE006", "\uE007", "\uE008", "\uE009", "\uE00A"};
        String[] overlay = {"\uF006", "\uF007", "\uF008", "\uF009", "\uF00A"};
        MutableText line = space(38);
        boolean any = false;
        for (int i = 0; i < prefixes.length; i++) {
            if (item.stat(prefixes[i] + "DamMin") == 0 && item.stat(prefixes[i] + "DamMax") == 0
                    && item.stat(prefixes[i] + "Def") == 0) continue;
            if (any) line.append(space(-1));
            line.append(shadowText(base[i], "tooltip/banner", dividerColor(item.tier())));
            line.append(text("\uDAFF\uDFF6" + overlay[i], "tooltip/banner", 0xFFFFFFFF));
            any = true;
        }
        return any ? line : Text.empty();
    }

    private Text skillIcons(WynnItem item, String[] keys) {
        MutableText line = Text.empty();
        for (int i = 0; i < keys.length; i++) {
            int required = item.stat(keys[i]);
            String frame = required == 0 ? "\uE007" : String.valueOf((char) ('\uE000' + tierIndex(item.tier())));
            String icon = String.valueOf((char) ((required == 0 ? '\uE010' : '\uE000') + i));
            line.append(shadowText(frame, "tooltip/requirement/frame", 0xFFFFFFFF));
            line.append(shadowText("\uDAFF\uDFE7" + icon, "tooltip/requirement/sprite", 0xFFFFFFFF));
            line.append(space(2));
        }
        return line;
    }

    private Text skillValues(WynnItem item, String[] keys) {
        MutableText line = Text.empty();
        for (int i = 0; i < keys.length; i++) {
            int required = item.stat(keys[i]);
            boolean fulfilled = required == 0 || finalSkills[i] >= required;
            line.append(shadowText((required == 0 ? "\uE005" : fulfilled ? "\uE006" : "\uE007")
                    + "\uDAFF\uDFFF", "tooltip/requirement/sprite", 0xFFFFFFFF));
            line.append(space(3));
            line.append(text(Integer.toString(required), "language/wynncraft",
                    required == 0 ? 0xFF555555 : fulfilled ? 0xFFACFAC6 : 0xFFFAACAC));
            line.append(space(10));
        }
        return line;
    }

    private Text requirement(String label, boolean fulfilled) {
        return shadowText((fulfilled ? "\uE006" : "\uE007") + "\uDAFF\uDFFF",
                "tooltip/requirement/sprite", 0xFFFFFFFF)
                .append(text(label, "language/wynncraft", 0xFFFFFFFF));
    }

    private Text divider(String tier) {
        return shadowText("\uE000", "tooltip/divider", dividerColor(tier));
    }

    private Text sprite(String value) {
        return shadowText(value, "tooltip/attribute/sprite", 0xFFFFFFFF);
    }

    private Text identificationValue(String value, boolean beneficial) {
        int color = beneficial ? 0xFFACFAC6 : 0xFFFAACAC;
        int darkColor = beneficial ? 0xFF6D9E7E : 0xFFA87474;
        int separator = value.indexOf(" to ");
        if (separator < 0) return text(value, "language/wynncraft", color);
        return text(value.substring(0, separator), "language/wynncraft", color)
                .append(text(" to ", "language/wynncraft", darkColor))
                .append(text(value.substring(separator + 4), "language/wynncraft", color));
    }

    private MutableText aligned(Text left, Text right, int width) {
        return Text.empty().append(left)
                .append(space(width - font.getWidth(left) - font.getWidth(right))).append(right);
    }

    private MutableText centered(Text value, int width) {
        return space(Math.max(0, (width - font.getWidth(value)) / 2)).append(value);
    }

    private MutableText banner(String value, int backgroundColor, int foregroundColor) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder background = new StringBuilder("\uE060\uDAFF\uDFFF");
        StringBuilder foreground = new StringBuilder();
        for (char character : lower.toCharArray()) {
            if (character == ' ') {
                background.append('\uE061');
                foreground.append(' ');
            } else {
                background.append(bannerGlyph(character, true));
                foreground.append(bannerGlyph(character, false));
            }
            background.append("\uDAFF\uDFFF");
        }
        background.append('\uE062');
        MutableText line = text(background.toString(), "banner/box", backgroundColor);
        line.append(space(2 - font.getWidth(line)));
        line.append(text(foreground.toString(), "banner/box", foregroundColor));
        return line;
    }

    private void appendWrapped(List<Text> tooltip, String value, int color) {
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.getWidth(candidate) > 150) {
                tooltip.add(text(line.toString(), "language/wynncraft", color));
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) tooltip.add(text(line.toString(), "language/wynncraft", color));
    }

    private static MutableText text(String value, String font, int color) {
        return Text.literal(value).fillStyle(Style.EMPTY
                .withFont(new StyleSpriteSource.Font(Identifier.ofVanilla(font))).withColor(color));
    }

    private static MutableText shadowText(String value, String font, int color) {
        return Text.literal(value).fillStyle(Style.EMPTY
                .withFont(new StyleSpriteSource.Font(Identifier.ofVanilla(font)))
                .withColor(color).withShadowColor(0xFFFFFF));
    }

    private static MutableText space(int pixels) {
        if (pixels == 0) return Text.empty();
        return text(new String(Character.toChars(0xD0000 + pixels)), "space", 0xFFFFFFFF);
    }

    private static char bannerGlyph(char value, boolean background) {
        int base;
        if (value >= 'a' && value <= 'z') base = value - 'a';
        else if (value >= '0' && value <= '9') base = 0x20 + value - '0';
        else base = switch (value) {
            case '?' -> 0x1A;
            case '[' -> 0x1B;
            case ']' -> 0x1C;
            case '/' -> 0x1D;
            case '%' -> 0x1E;
            case '&' -> 0x1F;
            case '!' -> 0x2A;
            case '(' -> 0x2B;
            case ')' -> 0x2C;
            case '<' -> 0x2D;
            case '=' -> 0x2E;
            case '>' -> 0x2F;
            case '+' -> 0x70;
            case '-' -> 0x71;
            case '.' -> 0x72;
            default -> 0x1A;
        };
        if (background) base += base >= 0x70 ? 0x10 : 0x30;
        return (char) ('\uE000' + base);
    }

    private static int dividerColor(String tier) {
        if (tier == null) return 0xFFE0E0E0;
        return switch (tier.toLowerCase(Locale.ROOT)) {
            case "unique" -> 0xFFFFF2B3;
            case "rare", "set" -> 0xFFF2C2F2;
            case "legendary" -> 0xFFC2F2F2;
            case "fabled" -> 0xFFF2C2C2;
            case "mythic" -> 0xFFE0B3E6;
            default -> 0xFFE0E0E0;
        };
    }

    private static int tierIndex(String tier) {
        if (tier == null) return 0;
        return switch (tier.toLowerCase(Locale.ROOT)) {
            case "unique" -> 1;
            case "rare", "set" -> 2;
            case "legendary" -> 3;
            case "fabled" -> 4;
            case "mythic" -> 5;
            case "crafted" -> 6;
            default -> 0;
        };
    }

    private static int tierColor(String tier) {
        if (tier == null) return 0xFFE8E8E8;
        return switch (tier.toLowerCase(Locale.ROOT)) {
            case "unique" -> 0xFFFFFF55;
            case "rare" -> 0xFFFF55FF;
            case "legendary" -> 0xFF55FFFF;
            case "fabled" -> 0xFFFF5555;
            case "mythic" -> 0xFFAA00AA;
            case "set" -> 0xFF55FF55;
            case "crafted" -> 0xFF00AAAA;
            default -> 0xFFE8E8E8;
        };
    }

    private static String emblemFrame(String emblem) {
        String[] parts = emblem == null ? new String[0] : emblem.toLowerCase(Locale.ROOT).split("_");
        int shape = parts.length == 0 ? 0 : switch (parts[0]) {
            case "square" -> 1;
            case "hexagon" -> 2;
            case "shield" -> 3;
            case "sticker" -> 4;
            case "circle" -> 5;
            default -> 0;
        };
        int variant = 1;
        if (parts.length > 1) {
            try {
                variant = Math.clamp(Integer.parseInt(parts[1]), 1, 6);
            } catch (NumberFormatException ignored) {}
        }
        return String.valueOf((char) ('\uE000' + (variant - 1) * 0x10 + shape));
    }

    private static String typeSprite(WynnItem item) {
        return switch (item.subType().toLowerCase(Locale.ROOT)) {
            case "helmet" -> "\uE000";
            case "chestplate" -> "\uE001";
            case "leggings" -> "\uE002";
            case "boots" -> "\uE003";
            case "bow" -> "\uE004";
            case "dagger" -> "\uE005";
            case "wand" -> "\uE006";
            case "relik" -> "\uE007";
            case "spear" -> "\uE008";
            case "potion" -> "\uE027";
            case "scroll" -> "\uE032";
            case "food" -> "\uE033";
            case "ring" -> "\uE014";
            case "bracelet" -> "\uE015";
            case "necklace" -> "\uE016";
            default -> "";
        };
    }

    private static String itemTypeName(WynnItem item) {
        return item.subType().isBlank() ? item.type().toUpperCase(Locale.ROOT)
                : item.subType().toUpperCase(Locale.ROOT);
    }

    private static String attackSpeed(String speed) {
        return switch (speed.toLowerCase(Locale.ROOT).replace("_", "")) {
            case "superfast" -> "4.3";
            case "veryfast" -> "3.1";
            case "fast" -> "2.5";
            case "normal" -> "2.05";
            case "slow" -> "1.5";
            case "veryslow" -> "0.83";
            case "superslow" -> "0.51";
            default -> "?";
        };
    }

    private static String readable(String value) {
        String readable = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return readable.isEmpty() ? "" : Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    static final class Card {
        private final List<TooltipComponent> components;
        private final Identifier style;
        private final int contentWidth;
        private final int contentHeight;

        private Card(TextRenderer font, List<Text> lines, Identifier style) {
            components = lines.stream().map(Text::asOrderedText).map(TooltipComponent::of).toList();
            this.style = style;
            contentWidth = components.stream().mapToInt(component -> component.getWidth(font)).max().orElse(1);
            contentHeight = components.stream().mapToInt(component -> component.getHeight(font)).sum()
                    + (components.size() > 1 ? 2 : -2);
        }

        int width() {
            return contentWidth + 24;
        }

        int height() {
            return contentHeight + 24;
        }

        int width(float scale) {
            return Math.round(width() * scale);
        }

        int height(float scale) {
            return Math.round(height() * scale);
        }

        void render(DrawContext context, TextRenderer font, int x, int y) {
            render(context, font, x, y, width());
        }

        private void render(DrawContext context, TextRenderer font, int x, int y, int width) {
            int contentX = x + 12;
            int contentY = y + 12;
            int renderedContentWidth = Math.max(contentWidth, width - 24);
            TooltipBackgroundRenderer.render(context, contentX, contentY,
                    renderedContentWidth, contentHeight, style);
            int lineY = contentY;
            for (int i = 0; i < components.size(); i++) {
                TooltipComponent component = components.get(i);
                component.drawText(context, font, contentX, lineY);
                component.drawItems(font, contentX, lineY, renderedContentWidth, contentHeight, context);
                lineY += component.getHeight(font) + (i == 0 ? 2 : 0);
            }
        }

        void render(DrawContext context, TextRenderer font, int x, int y, float scale) {
            context.getMatrices().pushMatrix();
            try {
                context.getMatrices().translate(x, y);
                context.getMatrices().scale(scale, scale);
                render(context, font, 0, 0);
            } finally {
                context.getMatrices().popMatrix();
            }
        }

        void render(DrawContext context, TextRenderer font, int x, int y, float scale, int width) {
            context.getMatrices().pushMatrix();
            try {
                context.getMatrices().translate(x, y);
                context.getMatrices().scale(scale, scale);
                render(context, font, 0, 0, width);
            } finally {
                context.getMatrices().popMatrix();
            }
        }
    }
}
