// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemModel.
 *
 * Classifies a Wynncraft ItemStack into a WynnItem subclass. The primary path is an EXACT lookup
 * against the gear database (GearModel/Models.Gear, gear.json from the CDN): a matched gear item
 * gets its authoritative GearType/GearTier/requirements/stat-ranges from the DB, and its actual
 * rolled identifications from the lore. Items not in the DB (crafted, consumables, ingredients,
 * gear boxes, aspects, tomes, misc) fall back to lore-keyword + name-colour heuristics.
 *
 * This gives exact weapon detection, tier/type identification, bank-search accuracy and raid-reward
 * mythic detection. Aspects/tomes (absent from gear.json) carry the tier derived from their name
 * colour, which is what the raid loot tracker keys on.
 *
 * NOT parsed here: exact roll percentages / stat encoding IDs (needs id_keys.json + the encoded
 * item string — see ItemEncodingModel, Phase 7) and crafted-gear variable stats.
 */
package julianh06.wynnextras.wtshim.models.items;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.models.gear.type.GearInfo;
import julianh06.wynnextras.wtshim.models.gear.type.GearRequirements;
import julianh06.wynnextras.wtshim.models.gear.type.GearTier;
import julianh06.wynnextras.wtshim.models.gear.type.GearType;
import julianh06.wynnextras.wtshim.models.items.items.game.AmplifierItem;
import julianh06.wynnextras.wtshim.models.items.items.game.AspectItem;
import julianh06.wynnextras.wtshim.models.items.items.game.CharmItem;
import julianh06.wynnextras.wtshim.models.items.items.game.CraftedConsumableItem;
import julianh06.wynnextras.wtshim.models.items.items.game.CraftedGearItem;
import julianh06.wynnextras.wtshim.models.items.items.game.CrafterBagItem;
import julianh06.wynnextras.wtshim.models.items.items.game.DungeonKeyItem;
import julianh06.wynnextras.wtshim.models.items.items.game.EmeraldPouchItem;
import julianh06.wynnextras.wtshim.models.items.items.game.GatheringToolItem;
import julianh06.wynnextras.wtshim.models.items.items.game.GearBoxItem;
import julianh06.wynnextras.wtshim.models.items.items.game.GearItem;
import julianh06.wynnextras.wtshim.models.items.items.game.HorseItem;
import julianh06.wynnextras.wtshim.models.items.items.game.IngredientItem;
import julianh06.wynnextras.wtshim.models.items.items.game.InsulatorItem;
import julianh06.wynnextras.wtshim.models.items.items.game.MaterialItem;
import julianh06.wynnextras.wtshim.models.items.items.game.MultiHealthPotionItem;
import julianh06.wynnextras.wtshim.models.items.items.game.PotionItem;
import julianh06.wynnextras.wtshim.models.items.items.game.PowderItem;
import julianh06.wynnextras.wtshim.models.items.items.game.RuneItem;
import julianh06.wynnextras.wtshim.models.items.items.game.TeleportScrollItem;
import julianh06.wynnextras.wtshim.models.items.items.game.TomeItem;
import julianh06.wynnextras.wtshim.models.items.items.game.TrinketItem;
import julianh06.wynnextras.wtshim.models.items.items.game.UnknownGearItem;
import julianh06.wynnextras.wtshim.models.items.items.gui.SkillPointItem;
import julianh06.wynnextras.wtshim.utils.mc.LoreUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class ItemModel extends Model {
    public Optional<WynnItem> getWynnItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        return classify(stack);
    }

    public <T extends WynnItem> Optional<T> asWynnItem(ItemStack stack, Class<T> type) {
        return getWynnItem(stack).filter(type::isInstance).map(type::cast);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> asWynnItemProperty(ItemStack stack, Class<T> propertyType) {
        Optional<WynnItem> item = getWynnItem(stack);
        if (item.isPresent() && propertyType.isInstance(item.get())) {
            return Optional.of((T) item.get());
        }
        return Optional.empty();
    }

    // ---- classification ----

    private Optional<WynnItem> classify(ItemStack stack) {
        Text nameText = stack.getName();
        String name = nameText == null ? "" : stripColors(nameText.getString()).trim();

        // Tier derived from the display-name colour (used for items not in the gear DB — gear boxes,
        // aspects, tomes). Gear items get their exact tier from the DB below instead.
        GearTier colorTier = tierFromName(nameText);

        List<Text> lore = LoreUtils.getLore(stack);
        String firstLore = lore.isEmpty() ? "" : stripColors(lore.get(0).getString()).trim();
        String firstLoreLower = firstLore.toLowerCase();

        // Parse plain-text lore for stats / requirements / damages (used by gear branches below).
        List<String> plainLore = new ArrayList<>(lore.size());
        for (Text l : lore) plainLore.add(stripColors(l.getString()).trim());
        julianh06.wynnextras.wtshim.models.items.parsing.ItemLoreParser.ParseResult parsed =
                julianh06.wynnextras.wtshim.models.items.parsing.ItemLoreParser.parse(plainLore);

        // Crafted items are never in the gear DB and have their own item classes — check first.
        if (firstLoreLower.contains("crafted") && (firstLoreLower.contains("consumable") || firstLoreLower.contains("potion"))) {
            return wrap(new CraftedConsumableItemNamed(name));
        }
        if (firstLoreLower.contains("crafted") && firstLoreLower.matches(".*(weapon|helmet|chestplate|leggings|boots|ring|bracelet|necklace).*")) {
            return wrap(new CraftedGearItemNamed(name));
        }

        // Exact recognition from the gear database — authoritative type/tier/requirements/stat-ranges.
        // This is what makes weapon detection, tier/type identification and bank search exact. Falls
        // through to the lore heuristics below when the DB is not loaded yet or the item is not gear.
        Optional<GearInfo> dbInfo = Models.Gear.getFromDisplayName(name);
        if (dbInfo.isPresent()) {
            return wrap(new GearItemNamed(name, dbInfo.get(), parsed));
        }

        // Specific recognitions — these short-circuit before the generic gear check.
        if (firstLoreLower.startsWith("tier") && firstLoreLower.contains("ingredient")) {
            return wrap(new IngredientItemNamed(name));
        }
        if (firstLoreLower.contains("ingredient pouch")) return Optional.empty();
        if (firstLoreLower.contains("emerald pouch")) return wrap(new EmeraldPouchItemNamed(name));
        if (firstLoreLower.contains("gear box")) return wrap(new GearBoxItemNamed(name, colorTier));
        if (firstLoreLower.contains("dungeon key")) return wrap(new DungeonKeyItemNamed(name));
        if (firstLoreLower.contains("teleport scroll")) return wrap(new TeleportScrollItemNamed(name));
        if (firstLoreLower.contains("amplifier")) return wrap(new AmplifierItemNamed(name));
        if (firstLoreLower.contains("charm")) return wrap(new CharmItemNamed(name));
        if (firstLoreLower.contains("trinket")) return wrap(new TrinketItemNamed(name));
        if (firstLoreLower.contains("rune")) return wrap(new RuneItemNamed(name));
        if (firstLoreLower.contains("insulator")) return wrap(new InsulatorItemNamed(name));
        if (firstLoreLower.contains("gathering tool")) return wrap(new GatheringToolItemNamed(name));
        if (firstLoreLower.contains("material")) return wrap(new MaterialItemNamed(name));
        if (firstLoreLower.contains("aspect")) return wrap(new AspectItemNamed(name, colorTier));
        if (firstLoreLower.contains("crafter")) return wrap(new CrafterBagItemNamed(name));
        if (firstLoreLower.contains("tome")) return wrap(new TomeItemNamed(name, colorTier));
        if (firstLoreLower.contains("potion")) return wrap(new PotionItemNamed(name));
        if (firstLoreLower.contains("horse")) return wrap(new HorseItemNamed(name));
        if (firstLoreLower.contains("powder")) return wrap(new PowderItemNamed(name));

        // Generic gear detection from the lore header "Spear Lv. 50", "Helmet Lv. 70", etc. — used when
        // the item is genuine gear but absent from (or predates) the loaded gear DB.
        GearType gearType = gearTypeFromLore(firstLoreLower);
        if (gearType != GearType.UNKNOWN) {
            GearInfo loreInfo = new GearInfo(name, gearType, colorTier, parsed.requirements(), List.of());
            return wrap(new GearItemNamed(name, loreInfo, parsed));
        }

        if (firstLoreLower.contains("skill") && firstLoreLower.contains("point")) {
            return wrap(new SkillPointItemNamed(name));
        }

        // Couldn't classify — still return an UnknownGearItem so callers can see *something*.
        return wrap(new UnknownGearItemNamed(name));
    }

    private static Optional<WynnItem> wrap(WynnItem w) { return Optional.of(w); }

    /**
     * Tier from the display name's first colour code. Prefers the faithful StyledText path (which
     * reconstructs § legacy codes from the component), falling back to an RGB→legacy mapping.
     */
    private static GearTier tierFromName(Text nameText) {
        if (nameText == null) return GearTier.NORMAL;
        String code = firstColorCode(
                julianh06.wynnextras.wtshim.core.text.StyledText.fromComponent(nameText).getString());
        if (code == null) code = firstColorCode(legacyOf(nameText));
        return tierFromColor(code);
    }

    private static GearTier tierFromColor(String colorCode) {
        // Source: Wynntils GearTier enum. SET is GRAY (§7), not GREEN.
        if (colorCode == null) return GearTier.NORMAL;
        return switch (colorCode) {
            case "§f" -> GearTier.NORMAL;
            case "§e" -> GearTier.UNIQUE;
            case "§d" -> GearTier.RARE;
            case "§b" -> GearTier.LEGENDARY;
            case "§c" -> GearTier.FABLED;
            case "§5" -> GearTier.MYTHIC;
            case "§7" -> GearTier.SET;
            case "§3" -> GearTier.CRAFTED;
            default -> GearTier.NORMAL;
        };
    }

    private static GearType gearTypeFromLore(String first) {
        if (first.contains("spear")) return GearType.SPEAR;
        if (first.contains("wand")) return GearType.WAND;
        if (first.contains("dagger")) return GearType.DAGGER;
        if (first.contains("bow")) return GearType.BOW;
        if (first.contains("relik")) return GearType.RELIK;
        if (first.contains("helmet")) return GearType.HELMET;
        if (first.contains("chestplate")) return GearType.CHESTPLATE;
        if (first.contains("leggings")) return GearType.LEGGINGS;
        if (first.contains("boots")) return GearType.BOOTS;
        if (first.contains("ring")) return GearType.RING;
        if (first.contains("bracelet")) return GearType.BRACELET;
        if (first.contains("necklace")) return GearType.NECKLACE;
        return GearType.UNKNOWN;
    }

    /** Extract the first "§x" color code from a legacy-formatted string, or null. */
    private static String firstColorCode(String legacy) {
        if (legacy == null) return null;
        for (int i = 0; i < legacy.length() - 1; i++) {
            if (legacy.charAt(i) == '\u00A7') return legacy.substring(i, i + 2);
        }
        return null;
    }

    private static String legacyOf(Text text) {
        StringBuilder sb = new StringBuilder();
        text.visit((style, s) -> {
            if (style.getColor() != null) {
                // Use a best-effort tier-color mapping based on RGB hex.
                String hex = String.format("%06X", style.getColor().getRgb() & 0xFFFFFF);
                sb.append(hexToLegacy(hex));
            }
            sb.append(s);
            return Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        return sb.toString();
    }

    private static String hexToLegacy(String hex) {
        // Standard Minecraft chat-color mappings. Unknown hex → nothing.
        return switch (hex.toUpperCase()) {
            case "FFFFFF" -> "§f";
            case "FFFF55" -> "§e";
            case "FF55FF" -> "§d";
            case "55FFFF" -> "§b";
            case "FF5555" -> "§c";
            case "AA00AA" -> "§5";
            case "55FF55" -> "§a";
            case "00AAAA" -> "§3";
            default -> "";
        };
    }

    private static String stripColors(String s) {
        return s == null ? "" : s.replaceAll("§[0-9a-fk-or]", "");
    }

    // ---- concrete named-only item subclasses ----
    // Each extends the matching item type class and overrides getName().
    private static final class GearItemNamed extends GearItem {
        // NOTE: fully-qualified — inside a GearItem subclass the simple name "GearInfo" would resolve
        // to the inherited nested GearItem.GearInfo, not the gear-DB record.
        private final String name;
        private final julianh06.wynnextras.wtshim.models.gear.type.GearInfo info;
        private final julianh06.wynnextras.wtshim.models.items.parsing.ItemLoreParser.ParseResult parsed;

        GearItemNamed(String n, julianh06.wynnextras.wtshim.models.gear.type.GearInfo info,
                      julianh06.wynnextras.wtshim.models.items.parsing.ItemLoreParser.ParseResult parsed) {
            this.name = n;
            this.info = info;
            this.parsed = parsed;
        }

        @Override public String getName() { return name; }
        @Override public GearTier getGearTier() { return info.tier(); }
        @Override public GearType getGearType() { return info.type(); }
        @Override public GearRequirements getRequirements() { return info.requirements(); }
        // Identifications are the item's actual rolled values, which only exist in the lore.
        @Override public java.util.List<julianh06.wynnextras.wtshim.models.stats.type.StatActualValue> getIdentifications() {
            return parsed == null ? super.getIdentifications() : parsed.identifications();
        }
        // Possible values (roll ranges) come from the gear DB when available; empty for lore-only gear.
        @Override public java.util.List<julianh06.wynnextras.wtshim.models.stats.type.StatPossibleValues> getPossibleValues() {
            java.util.List<julianh06.wynnextras.wtshim.models.stats.type.StatPossibleValues> out = new ArrayList<>();
            for (var pair : info.variableStats()) out.add(pair.b());
            return out;
        }
        // getItemInfo() returns GearItem's legacy 2-field record (requirements + variableStats),
        // which is what CompassMenuOverlay reads; distinct from the 5-field gear-DB GearInfo.
        @Override public GearItem.GearInfo getItemInfo() {
            return new GearItem.GearInfo(info.requirements(), info.variableStats());
        }
    }
    private static final class CraftedGearItemNamed extends CraftedGearItem {
        private final String name; CraftedGearItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class CraftedConsumableItemNamed extends CraftedConsumableItem {
        private final String name; CraftedConsumableItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class IngredientItemNamed extends IngredientItem {
        private final String name; IngredientItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class EmeraldPouchItemNamed extends EmeraldPouchItem {
        private final String name; EmeraldPouchItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class GearBoxItemNamed extends GearBoxItem {
        private final String name; private final GearTier tier;
        GearBoxItemNamed(String n, GearTier t) { name = n; tier = t; }
        @Override public String getName() { return name; }
        @Override public GearTier getGearTier() { return tier; }
    }
    private static final class UnknownGearItemNamed extends UnknownGearItem {
        private final String name; UnknownGearItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class SkillPointItemNamed extends SkillPointItem {
        private final String name; SkillPointItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class DungeonKeyItemNamed extends DungeonKeyItem {
        private final String name; DungeonKeyItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class TeleportScrollItemNamed extends TeleportScrollItem {
        private final String name; TeleportScrollItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class AmplifierItemNamed extends AmplifierItem {
        private final String name; AmplifierItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class CharmItemNamed extends CharmItem {
        private final String name; CharmItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class TrinketItemNamed extends TrinketItem {
        private final String name; TrinketItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class RuneItemNamed extends RuneItem {
        private final String name; RuneItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class InsulatorItemNamed extends InsulatorItem {
        private final String name; InsulatorItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class GatheringToolItemNamed extends GatheringToolItem {
        private final String name; GatheringToolItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class MaterialItemNamed extends MaterialItem {
        private final String name; MaterialItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class AspectItemNamed extends AspectItem {
        private final String name; private final GearTier tier;
        AspectItemNamed(String n, GearTier t) { name = n; tier = t; }
        @Override public String getName() { return name; }
        @Override public GearTier getGearTier() { return tier; }
    }
    private static final class CrafterBagItemNamed extends CrafterBagItem {
        private final String name; CrafterBagItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class TomeItemNamed extends TomeItem {
        private final String name; private final GearTier tier;
        TomeItemNamed(String n, GearTier t) { name = n; tier = t; }
        @Override public String getName() { return name; }
        @Override public GearTier getGearTier() { return tier; }
    }
    private static final class PotionItemNamed extends PotionItem {
        private final String name; PotionItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class HorseItemNamed extends HorseItem {
        private final String name; HorseItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class PowderItemNamed extends PowderItem {
        private final String name; PowderItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
    private static final class MultiHealthPotionItemNamed extends MultiHealthPotionItem {
        private final String name; MultiHealthPotionItemNamed(String n) { name = n; }
        @Override public String getName() { return name; }
    }
}
