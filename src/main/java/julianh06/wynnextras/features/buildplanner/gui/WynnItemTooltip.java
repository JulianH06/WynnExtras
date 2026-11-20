package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.data.BuildCalculator;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.ItemInspection;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.wynncraft.state.CharacterClass;
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
    private static final int INGREDIENT_ACCENT = 0xFF7DC97D;
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
            int mouseX, int mouseY, CharacterClass selectedClass, int level, int[] finalSkills,
            boolean skillRequirementWarning) {
        WynnItemTooltip tooltip = new WynnItemTooltip(font, selectedClass, level, finalSkills);
        String tier = item.tier() == null ? "normal" : item.tier().toLowerCase(Locale.ROOT);
        if ("set".equals(tier)) tier = "rare";
        screen.renderPlannerTooltip(context, tooltip.lines(item, item.displayName(), skillRequirementWarning, false),
                Identifier.ofVanilla(tier), mouseX, mouseY);
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
        return itemCard(font, item, item.displayName(), false);
    }

    static Card itemCard(TextRenderer font, WynnItem item, String displayName) {
        return itemCard(font, item, displayName, false);
    }

    static Card itemCard(TextRenderer font, WynnItem item, boolean showLore) {
        return itemCard(font, item, item.displayName(), showLore);
    }

    private static Card itemCard(TextRenderer font, WynnItem item, String displayName, boolean showLore) {
        CharacterClass abilityClass = "weapon".equals(item.type())
                ? CharacterClass.fromWeaponSubtype(item.subType()) : CharacterClass.UNKNOWN;
        WynnItemTooltip tooltip = new WynnItemTooltip(font, abilityClass, 121,
                new int[]{200, 200, 200, 200, 200});
        String tier = item.tier() == null ? "normal" : item.tier().toLowerCase(Locale.ROOT);
        if ("set".equals(tier)) tier = "rare";
        return new Card(font, tooltip.lines(item, displayName, false, showLore), Identifier.ofVanilla(tier));
    }

    static Card ingredientCard(TextRenderer font, CraftedItemCodec.Ingredient ingredient) {
        WynnItemTooltip tooltip = new WynnItemTooltip(font, CharacterClass.ARCHER, 121, new int[5]);
        return new Card(font, tooltip.ingredientLines(ingredient), Identifier.ofVanilla(switch (ingredient.tier()) {
            case 1 -> "unique";
            case 2 -> "rare";
            case 3 -> "legendary";
            default -> "normal";
        }), 4);
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

    private List<Text> lines(WynnItem item, String displayName, boolean skillRequirementWarning,
            boolean showLore) {
        int tooltipWidth = tooltipWidth(item, displayName, identificationClass(item));
        List<Text> tooltip = lines(item, displayName, skillRequirementWarning, showLore, tooltipWidth);
        int renderedWidth = tooltip.stream().mapToInt(font::getWidth).max().orElse(tooltipWidth);
        return renderedWidth > tooltipWidth
                ? lines(item, displayName, skillRequirementWarning, showLore, renderedWidth)
                : tooltip;
    }

    private List<Text> lines(WynnItem item, String displayName, boolean skillRequirementWarning, boolean showLore,
            int tooltipWidth) {
        List<Text> tooltip = new ArrayList<>();
        if (skillRequirementWarning) {
            tooltip.add(Text.literal("§lSkill point requirements are not met!")
                    .formatted(Formatting.RED));
        }
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
        if (item.isCrafted()) addCraftedBaseStats(tooltip, item);
        tooltip.add(centered(divider(item.tier()), tooltipWidth));

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
        tooltip.add(centered(divider(item.tier()), tooltipWidth));

        boolean hasIdentificationLine = false;
        boolean pendingGap = false;
        for (ItemInspection.Row row : ItemInspection.rows(item, "", true, identificationClass(item))) {
            if (row.kind() == ItemInspection.Kind.GAP) {
                if (hasIdentificationLine) pendingGap = true;
                continue;
            }
            if (row.kind() != ItemInspection.Kind.SKILL
                    && row.kind() != ItemInspection.Kind.IDENTIFICATION) continue;
            if (pendingGap) tooltip.add(Text.empty());
            pendingGap = false;
            hasIdentificationLine = true;
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
        for (Map.Entry<String, String> majorId : item.majorIds().entrySet()) {
            tooltip.add(Text.empty());
            appendMajorId(tooltip, majorId.getKey(), majorId.getValue(), dividerColor(item.tier()));
        }
        if (showLore && !item.lore().isBlank()) {
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

    private void addCraftedBaseStats(List<Text> tooltip, WynnItem item) {
        if (!"consumable".equals(item.type())) {
            String durability = range(item.stat("durabilityMin"), item.stat("durabilityMax"));
            tooltip.add(meter(item.tier()).append(text(" Durability " + durability,
                    "language/wynncraft", 0xFFAAAAAA)));
            return;
        }

        int charges = item.stat("charges");
        tooltip.add(meter(item.tier())
                .append(text(" " + charges + " ", "language/wynncraft", 0xFFFFFFFF))
                .append(text("Charges", "language/wynncraft", 0xFFAAAAAA)));
        tooltip.add(sprite("\uE00C")
                .append(text(" " + durationRange(item.stat("durationMin"), item.stat("durationMax")) + " ",
                        "language/wynncraft", 0xFFFFFFFF))
                .append(text("Duration", "language/wynncraft", 0xFFAAAAAA)));
    }

    private MutableText meter(String tier) {
        return shadowText("\uE023\uDAFF\uDFF7", "tooltip/identification/meter", 0xFF555555)
                .append(text("\uE023", "tooltip/identification/meter", dividerColor(tier)));
    }

    private static String duration(int seconds) {
        int minutes = seconds / 60;
        int remainder = seconds % 60;
        if (minutes == 0) return remainder + "s";
        return remainder == 0 ? minutes + "m" : minutes + "m " + remainder + "s";
    }

    private static String durationRange(int minimum, int maximum) {
        return minimum == maximum ? duration(minimum) : duration(minimum) + " - " + duration(maximum);
    }

    private static String range(int minimum, int maximum) {
        return minimum == maximum ? Integer.toString(minimum) : minimum + "-" + maximum;
    }

    private List<Text> ingredientLines(CraftedItemCodec.Ingredient ingredient) {
        Text nameLine = ingredientNameLine(ingredient.name());
        Text typeLine = ingredientTypeLine(ingredient.tier());
        int width = Math.max(190, Math.max(font.getWidth(nameLine), font.getWidth(typeLine)));
        List<Text> tooltip = ingredientLines(ingredient, width);
        int renderedWidth = tooltip.stream().mapToInt(font::getWidth).max().orElse(width);
        return renderedWidth > width ? ingredientLines(ingredient, renderedWidth) : tooltip;
    }

    private List<Text> ingredientLines(CraftedItemCodec.Ingredient ingredient, int width) {
        List<Text> tooltip = new ArrayList<>();
        Text nameLine = ingredientNameLine(ingredient.name());
        Text typeLine = ingredientTypeLine(ingredient.tier());
        tooltip.add(nameLine);
        tooltip.add(typeLine);
        tooltip.add(Text.empty());
        if (ingredient.id() == 4000) {
            tooltip.add(centered(text("Empty ingredient slot", "language/wynncraft", 0xFF555555), width));
            return tooltip;
        }
        tooltip.add(space(2)
                .append(text(Integer.toString(ingredient.level()), "offset/wynncraft_quad/12", 0xFF7DC97D))
                .append(text(" Crafting Level", "language/wynncraft", 0xFFFFFFFF)));
        tooltip.add(space(2).append(text("Can be used in recipes for", "language/wynncraft", 0xFFAAAAAA)));
        for (int index = 0; index < ingredient.professions().size(); index += 2) {
            Text left = profession(ingredient.professions().get(index));
            Text right = index + 1 < ingredient.professions().size()
                    ? profession(ingredient.professions().get(index + 1)) : Text.empty();
            tooltip.add(space(2).append(columns(left, right, width - 2)));
        }
        tooltip.add(centeredPadded(ingredientDivider(), width));

        List<ItemInspection.Row> modifiers = ingredient.rows().stream()
                .filter(row -> row.kind() == ItemInspection.Kind.MODIFIER
                        && !row.label().startsWith("Effectiveness ")).toList();
        List<ItemInspection.Row> identifications = ingredient.rows().stream()
                .filter(row -> row.kind() == ItemInspection.Kind.IDENTIFICATION).toList();
        addIngredientStats(tooltip, modifiers, width);
        if (!modifiers.isEmpty() && !identifications.isEmpty()) tooltip.add(Text.empty());
        addIngredientStats(tooltip, identifications, width);

        List<ItemInspection.Row> effectiveness = ingredient.rows().stream()
                .filter(row -> row.label().startsWith("Effectiveness ")).toList();
        if (!effectiveness.isEmpty()) {
            addEffectiveness(tooltip, effectiveness, width,
                    !modifiers.isEmpty() || !identifications.isEmpty());
        }
        return tooltip;
    }

    private Text ingredientNameLine(String name) {
        return shadowText("\uDAFF\uDFF0", "space", 0xFFFFFFFF)
                .append(text("\uE031", "tooltip/emblem/frame", 0xFFFFFFFF))
                .append(text("\uDAFF\uDFCF", "space", 0xFFFFFFFF))
                .append(text("\uE034", "tooltip/emblem/sprite", 0xFF00EB1C))
                .append(space(5))
                .append(text(name, "language/wynncraft", 0xFF20AA20));
    }

    private Text ingredientTypeLine(int tier) {
        return space(38)
                .append(ingredientStarBanner(tier))
                .append(space(1))
                .append(ingredientLabelBanner());
    }

    private Text ingredientLabelBanner() {
        String background = "\uE060" + spacingGlyph(-1)
                + "\uE038" + spacingGlyph(-1)
                + "\uE03D" + spacingGlyph(-1)
                + "\uE036" + spacingGlyph(-1)
                + "\uE041" + spacingGlyph(-1)
                + "\uE034" + spacingGlyph(-1)
                + "\uE033" + spacingGlyph(-1)
                + "\uE038" + spacingGlyph(-1)
                + "\uE034" + spacingGlyph(-1)
                + "\uE03D" + spacingGlyph(-1)
                + "\uE043" + spacingGlyph(-1)
                + "\uE062" + spacingGlyph(-58);
        return text(background, "banner/box", 0xFF7DC97D)
                .append(shadowText("\uE008\uE00D\uE006\uE011\uE004\uE003\uE008\uE004\uE00D\uE013",
                        "banner/box", 0xFF000000))
                .append(space(2));
    }

    private Text ingredientStarBanner(int tier) {
        int filled = Math.clamp(tier, 0, 3);
        MutableText line = Text.empty();
        if (filled > 0) line.append(starBanner(filled, ingredientStarColor(filled)));
        int empty = 3 - filled;
        if (empty > 0) {
            if (filled > 0) line.append(space(-3));
            line.append(starBanner(empty, 0xFF000000));
        }
        return line;
    }

    private Text starBanner(int stars, int color) {
        StringBuilder background = new StringBuilder("\uE060");
        for (int index = 0; index < stars; index++) {
            background.append(spacingGlyph(-1)).append('\uE001');
        }
        background.append(spacingGlyph(-1)).append('\uE062')
                .append(spacingGlyph(-(7 * stars + 2)));
        String foreground = String.valueOf('\uE000').repeat(stars);
        return text(background.toString(), "banner/symbol", 0xFF555555)
                .append(shadowText(foreground, "banner/symbol", color))
                .append(space(2));
    }

    private static int ingredientStarColor(int tier) {
        return switch (tier) {
            case 1 -> 0xFFE6B847;
            case 2 -> 0xFFE647E6;
            case 3 -> 0xFF47C4E6;
            default -> 0xFF555555;
        };
    }

    private void addIngredientStats(List<Text> tooltip, List<ItemInspection.Row> rows, int width) {
        for (ItemInspection.Row row : rows) {
            String label = row.label().replaceFirst(" %$", "");
            tooltip.add(aligned(text(label, "language/wynncraft", 0xFFFFFFFF),
                    identificationValue(row.value(), row.beneficial()), width));
        }
    }

    private void addEffectiveness(List<Text> tooltip, List<ItemInspection.Row> rows, int width,
            boolean separatedFromStats) {
        Map<String, Integer> modifiers = new java.util.HashMap<>();
        for (ItemInspection.Row row : rows) {
            try {
                String direction = row.label().substring("Effectiveness ".length())
                        .replace(" ", "").toLowerCase(Locale.ROOT);
                modifiers.put(direction,
                        Integer.parseInt(row.value().replace("+", "").replace("%", "")));
            } catch (NumberFormatException ignored) {
            }
        }
        if (separatedFromStats) {
            tooltip.add(centered(ingredientDivider(), width));
        }
        tooltip.add(effectivenessBanner());
        tooltip.add(text("Grants an effectiveness multiplier", "language/wynncraft", 0xFFAAAAAA));
        tooltip.add(text("to nearby ingredients when used in", "language/wynncraft", 0xFFAAAAAA));
        tooltip.add(text("a Crafted Item recipe", "language/wynncraft", 0xFFAAAAAA));
        Text gridBackground = shadowText("\uF000", "tooltip/attribute/sprite", 0xFFFFFFFF);
        boolean expandedWidth = width > 190;
        int gridOffset = Math.max(0, (width - 190) / 2);
        tooltip.add(expandedWidth ? space(22 + gridOffset).append(gridBackground)
                : centered(gridBackground, width));
        for (int row = -2; row <= 2; row++) {
            List<Text> cells = new ArrayList<>();
            for (int column = -1; column <= 1; column++) {
                if (row == 0 && column == 0) {
                    cells.add(Text.empty());
                } else {
                    int value = effectiveness(modifiers, row, column);
                    int color = value > 0 ? 0xFFACFAC6 : value < 0 ? 0xFFFAACAC : 0xFFAAAAAA;
                    cells.add(shadowText((value > 0 ? "+" : "") + value + "%", "language/five", color));
                }
            }
            Text gridRow = effectivenessGridRow(cells, row == 0);
            tooltip.add(expandedWidth ? space(27 + gridOffset).append(gridRow) : centered(gridRow, width));
        }
    }

    private static int effectiveness(Map<String, Integer> modifiers, int row, int column) {
        int value = 0;
        if (row == 0 && column == -1) value += modifiers.getOrDefault("left", 0);
        if (row == 0 && column == 1) value += modifiers.getOrDefault("right", 0);
        if (column == 0 && row < 0) value += modifiers.getOrDefault("above", 0);
        if (column == 0 && row > 0) value += modifiers.getOrDefault("under", 0);
        if (Math.abs(row) + Math.abs(column) == 1) value += modifiers.getOrDefault("touching", 0);
        if (Math.abs(row) > 1 || Math.abs(row) == 1 && Math.abs(column) == 1) {
            value += modifiers.getOrDefault("nottouching", 0);
        }
        return value;
    }

    private Text profession(String profession) {
        return text(professionIcon(profession) + " ", "language/wynncraft", 0xFFFFFFFF)
                .append(text(readable(profession), "language/wynncraft", 0xFFAAAAAA));
    }

    private Text columns(Text left, Text right, int width) {
        int columnWidth = width / 2;
        return Text.empty().append(left).append(space(Math.max(4, columnWidth - font.getWidth(left)))).append(right);
    }

    private Text effectivenessGridRow(List<Text> cells, boolean centerRow) {
        int cellWidth = 36;
        MutableText line = Text.empty();
        for (int index = 0; index < cells.size(); index++) {
            Text cell = cells.get(index);
            if (centerRow && index == 1) {
                line.append(space(cellWidth));
                continue;
            }
            int remaining = Math.max(0, cellWidth - font.getWidth(cell));
            line.append(space(remaining / 2)).append(cell).append(space(remaining - remaining / 2));
        }
        return line;
    }

    private Text effectivenessBanner() {
        String background = "\uE060" + spacingGlyph(-1)
                + "\uE034" + spacingGlyph(-1)
                + "\uE035" + spacingGlyph(-1)
                + "\uE035" + spacingGlyph(-1)
                + "\uE034" + spacingGlyph(-1)
                + "\uE032" + spacingGlyph(-1)
                + "\uE043" + spacingGlyph(-1)
                + "\uE038" + spacingGlyph(-1)
                + "\uE045" + spacingGlyph(-1)
                + "\uE034" + spacingGlyph(-1)
                + "\uE03D" + spacingGlyph(-1)
                + "\uE034" + spacingGlyph(-1)
                + "\uE042" + spacingGlyph(-1)
                + "\uE042" + spacingGlyph(-1)
                + "\uE062" + spacingGlyph(-78);
        String foreground = "\uE004\uE005\uE005\uE004\uE002\uE013\uE008\uE015\uE004\uE00D\uE004\uE012\uE012"
                + spacingGlyph(2);
        return shadowText(background, "banner/box", INGREDIENT_ACCENT)
                .append(shadowText(foreground, "banner/box", 0xFF000000));
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

    public static Text ingredientTier(int tier) {
        MutableText result = ingredientTierText(" [", tier == 1 ? Formatting.GOLD
                : tier == 2 ? Formatting.DARK_PURPLE : tier == 3 ? Formatting.DARK_AQUA : Formatting.GRAY);
        if (tier == 1) result.append(ingredientTierText("✫", Formatting.YELLOW))
                .append(ingredientTierText("✫✫", Formatting.DARK_GRAY));
        else if (tier == 2) result.append(ingredientTierText("✫✫", Formatting.LIGHT_PURPLE))
                .append(ingredientTierText("✫", Formatting.DARK_GRAY));
        else if (tier == 3) result.append(ingredientTierText("✫✫✫", Formatting.AQUA));
        else result.append(ingredientTierText("✫✫✫", Formatting.DARK_GRAY));
        return result.append(ingredientTierText("]", tier == 1 ? Formatting.GOLD
                : tier == 2 ? Formatting.DARK_PURPLE : tier == 3 ? Formatting.DARK_AQUA : Formatting.GRAY));
    }

    private static MutableText ingredientTierText(String value, Formatting color) {
        return Text.literal(value).formatted(color).styled(style -> style.withFont(
                new StyleSpriteSource.Font(Identifier.ofVanilla("default"))));
    }

    private int tooltipWidth(WynnItem item, String displayName, CharacterClass identificationClass) {
        int width = Math.max(140, font.getWidth(displayName) + 30);
        for (ItemInspection.Row row : ItemInspection.rows(item, "", true, identificationClass)) {
            if (row.kind() == ItemInspection.Kind.SKILL || row.kind() == ItemInspection.Kind.IDENTIFICATION) {
                width = Math.max(width, font.getWidth(row.label() + "  " + row.value()));
            }
        }
        return width;
    }

    private CharacterClass identificationClass(WynnItem item) {
        return "weapon".equals(item.type())
                ? CharacterClass.fromWeaponSubtype(item.subType()) : selectedClass;
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

    private Text ingredientDivider() {
        return shadowText("\uE000", "tooltip/divider", INGREDIENT_ACCENT);
    }

    private MutableText sprite(String value) {
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
                .append(space(Math.max(0, width - font.getWidth(left) - font.getWidth(right)))).append(right);
    }

    private MutableText centered(Text value, int width) {
        return space(Math.max(0, (width - font.getWidth(value)) / 2)).append(value);
    }

    private MutableText centeredPadded(Text value, int width) {
        MutableText line = centered(value, width);
        return line.append(space(Math.max(0, width - font.getWidth(line))));
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

    private void appendMajorId(List<Text> tooltip, String name, String description, int color) {
        String value = name + ": " + plainDescription(description);
        StringBuilder line = new StringBuilder();
        boolean first = true;
        int iconWidth = font.getWidth(text("\uE000", "tooltip/identification/major", color)) + 2;
        for (String word : value.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            int availableWidth = first ? 150 - iconWidth : 150;
            if (!line.isEmpty() && font.getWidth(candidate) > availableWidth) {
                tooltip.add(majorIdLine(line.toString(), color, first));
                line.setLength(0);
                first = false;
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) tooltip.add(majorIdLine(line.toString(), color, first));
    }

    private Text majorIdLine(String value, int color, boolean icon) {
        MutableText line = Text.empty();
        if (icon) {
            line.append(shadowText("\uE000", "tooltip/identification/major", color));
            line.append(space(2));
        }
        return line.append(text(value, "language/wynncraft", color));
    }

    private static String plainDescription(String value) {
        return value.replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("<[^>]+>", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
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
        return text(spacingGlyph(pixels), "space", 0xFFFFFFFF);
    }

    private static String spacingGlyph(int pixels) {
        return new String(Character.toChars(0xD0000 + pixels));
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
        private final int topPadding;

        private Card(TextRenderer font, List<Text> lines, Identifier style) {
            this(font, lines, style, 0);
        }

        private Card(TextRenderer font, List<Text> lines, Identifier style, int topPadding) {
            components = lines.stream().map(Text::asOrderedText).map(TooltipComponent::of).toList();
            this.style = style;
            this.topPadding = topPadding;
            contentWidth = components.stream().mapToInt(component -> component.getWidth(font)).max().orElse(1);
            contentHeight = components.stream().mapToInt(component -> component.getHeight(font)).sum()
                    + (components.size() > 1 ? 2 : -2) + topPadding;
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
            int lineY = contentY + topPadding;
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
