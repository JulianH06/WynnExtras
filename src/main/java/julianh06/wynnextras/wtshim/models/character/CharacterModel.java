// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — CharacterModel.
 *
 * Reproduces Wynntils' character ID format so that WynnExtras' bank overlay (which stores data
 * keyed by character ID) keeps working with pre-existing saves:
 *
 *   • `id` is the first lore line of the compass item in inventory slot 7 (CHARACTER_INFO_SLOT).
 *   • It must match ^[a-z0-9]{8}$ — Wynncraft embeds an 8-char alphanumeric there.
 *
 * WynnExtras' CharacterModelMixin @Shadows `id` and `level`, and @Injects into
 * onWorldStateChanged() right after scanCharacterInfo() runs. So:
 *   - Fields must stay non-static instance fields named exactly `id` and `level`.
 *   - onWorldStateChanged(WorldStateEvent) must call scanCharacterInfo() at least once.
 *
 * Class + combat level come from the character-info container: when the player opens it, we
 * scan any slot's lore for Wynntils' patterns:
 *   §7Class: §f<name>
 *   §7Combat Lv: §f<number>
 * and cache the result.
 */
package julianh06.wynnextras.wtshim.models.character;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.models.character.type.ClassType;
import julianh06.wynnextras.wtshim.models.worlds.event.WorldStateEvent;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

public class CharacterModel extends Model {
    /** Inventory slot Wynncraft uses for the character-info compass. */
    public static final int CHARACTER_INFO_SLOT = 7;

    /** The 8-char lowercase-alphanumeric pattern Wynntils validates against. */
    private static final Pattern CHARACTER_ID_PATTERN = Pattern.compile("^[a-z0-9]{8}$");

    // Source: Wynntils CharacterModel#INFO_MENU_{CLASS,LEVEL}_PATTERN — § codes stripped.
    //   class: "§7Class: §f(.+)" → "Class: (.+)"
    //   level: "§7Combat Lv: §f(\\d+)" → "Combat Lv: (\\d+)"
    private static final Pattern CLASS_PATTERN = Pattern.compile("Class: (.+)");
    private static final Pattern COMBAT_LEVEL_PATTERN = Pattern.compile("Combat Lv: (\\d+)");

    /** SHADOWED by WynnExtras CharacterModelMixin — do not rename. */
    private String id = "-";
    /** SHADOWED by WynnExtras CharacterModelMixin — do not rename. */
    private int level = 0;

    private ClassType classType = ClassType.NONE;
    private boolean reskinned = false;
    private String actualName = "";

    private String lastWorld = "";

    public CharacterModel() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
    }

    public ClassType getClassType() { return classType; }
    public String getActualName() { return actualName; }
    public String getId() { return id == null ? "-" : id; }
    public boolean isReskinned() { return reskinned; }
    public int getLevel() { return level; }

    private void tick() {
        String currentWorld = Models.WorldState.getCurrentWorldName();
        if (currentWorld == null) currentWorld = "";

        boolean worldChanged = !currentWorld.equals(lastWorld);
        if (worldChanged) {
            lastWorld = currentWorld;
            if (currentWorld.isEmpty()) {
                id = "-";
                classType = ClassType.NONE;
                level = 0;
                actualName = "";
            }
        }

        // onWorldStateChanged is now driven by the real WorldStateModel via the event bus
        // (the @SubscribeEvent below); tick only keeps the passive compass/container scans.
        if ("-".equals(id) && !currentWorld.isEmpty()) {
            scanCharacterInfo();
        }

        // Passive class/level scan: if the character info screen happens to be open, snarf it.
        scanOpenContainerForClassAndLevel();
    }

    /** MIXIN TARGET — WynnExtras injects AFTER the call to scanCharacterInfo() inside this. */
    @SubscribeEvent
    public void onWorldStateChanged(WorldStateEvent event) {
        scanCharacterInfo();
    }

    /** MIXIN TARGET — reads the compass item's first lore line into `id`. */
    public void scanCharacterInfo() {
        PlayerInventory inv = McUtils.inventory();
        if (inv == null) return;
        if (CHARACTER_INFO_SLOT >= inv.getMainStacks().size()) return;

        ItemStack compass = inv.getMainStacks().get(CHARACTER_INFO_SLOT);
        if (compass == null || compass.isEmpty()) return;

        LoreComponent lore = compass.get(DataComponentTypes.LORE);
        if (lore == null) return;
        List<Text> lines = lore.lines();
        if (lines.isEmpty()) return;

        String stripped = stripColors(lines.get(0).getString()).trim();
        if (!CHARACTER_ID_PATTERN.matcher(stripped).matches()) return;

        if (!stripped.equals(id)) {
            id = stripped;
        }
    }

    /**
     * Scan any currently-open screen for class+level info. The character-info screen stores
     * them in one of its slots' item lores under "Class:" / "Combat Lv:" lines.
     */
    private void scanOpenContainerForClassAndLevel() {
        ScreenHandler menu = McUtils.containerMenu();
        if (menu == null) return;

        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;

            ClassType foundClass = null;
            int foundLevel = 0;

            for (Text line : lore.lines()) {
                String lineText = stripColors(line.getString()).trim();

                Matcher classM = CLASS_PATTERN.matcher(lineText);
                if (classM.matches() && foundClass == null) {
                    String rawName = classM.group(1).trim();
                    foundClass = classFromAnyName(rawName);
                }

                Matcher lvlM = COMBAT_LEVEL_PATTERN.matcher(lineText);
                if (lvlM.matches() && foundLevel == 0) {
                    try { foundLevel = Integer.parseInt(lvlM.group(1)); } catch (NumberFormatException ignored) {}
                }
            }

            if (foundClass != null || foundLevel > 0) {
                if (foundClass != null) {
                    this.classType = foundClass;
                    // Detect reskin — if the raw name differs from the base name it's reskinned.
                    this.reskinned = !foundClass.getName().equalsIgnoreCase(getRawClassNameFromLore(stack));
                    if (this.reskinned) {
                        this.actualName = getRawClassNameFromLore(stack);
                    } else {
                        this.actualName = foundClass.getName();
                    }
                }
                if (foundLevel > 0) this.level = foundLevel;
                return; // one hit is enough — don't keep scanning
            }
        }
    }

    private static String getRawClassNameFromLore(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return "";
        for (Text line : lore.lines()) {
            Matcher m = CLASS_PATTERN.matcher(stripColors(line.getString()).trim());
            if (m.matches()) return m.group(1).trim();
        }
        return "";
    }

    /** Accepts base class names (Archer, Mage, ...) AND reskinned forms (Hunter, Dark Wizard, ...). */
    private static ClassType classFromAnyName(String name) {
        if (name == null || name.isEmpty()) return null;
        String upper = name.trim().toUpperCase();
        for (ClassType t : ClassType.values()) {
            if (t == ClassType.NONE) continue;
            if (t.name().equals(upper)) return t;
            if (t.getActualName(true).equalsIgnoreCase(name)) return t;
            if (t.getActualName(false).equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    private static String stripColors(String s) {
        return s == null ? "" : s.replaceAll("§[0-9a-fk-or]", "");
    }
}
