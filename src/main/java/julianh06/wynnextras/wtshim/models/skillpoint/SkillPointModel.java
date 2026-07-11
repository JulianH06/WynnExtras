// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras - SkillPointModel.
 *
 * Event-driven foreground reader. When the Character Info menu is open (ContainerModel reports a
 * CharacterInfoContainer) the skill-point icons sit in fixed slots 11-15 = Str/Dex/Int/Def/Agi.
 * We read them from the ContainerSetContent packet and parse each icon's lore with Wynntils'
 * SkillPointAnnotator LORE_PATTERN (the first, §7 'X points' capture).
 *
 * Why foreground (not a background ScriptedContainerQuery): the only WynnExtras consumer
 * (CompassMenuOverlay) reads getAssignedSkillPoints WHILE the Character Info screen is open, and
 * ContainerQueryHandler refuses to run a background query while a container screen is open. A
 * background populate would also require the item pipeline (SkillPointItem annotator) which is
 * phase 6. So we read the open container's slots directly via the real container events instead.
 *
 * TODO(phase6-item-pipeline): Wynntils computes the true ASSIGNED points as
 *   total - gear - setBonus - tome - crafted - statusEffect (needs Models.Item/Set/StatusEffect +
 *   the SkillPointItem/GearItem annotators). Until the item pipeline lands, this returns the raw
 *   Character-Info skill value parsed from lore (no gear subtraction). The sole caller currently
 *   sits behind a disabled branch, so this is non-critical for now.
 */
package julianh06.wynnextras.wtshim.models.skillpoint;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.mc.event.ContainerCloseEvent;
import julianh06.wynnextras.wtshim.mc.event.ContainerSetContentEvent;
import julianh06.wynnextras.wtshim.models.containers.containers.CharacterInfoContainer;
import julianh06.wynnextras.wtshim.models.elements.type.Skill;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

public final class SkillPointModel extends Model {
    // Wynntils SkillPointModel: character-info skill icons live at these fixed slots, in Skill order.
    private static final int[] SKILL_POINT_TOTAL_SLOTS = {11, 12, 13, 14, 15};

    // Wynntils SkillPointAnnotator LORE_PATTERN (byte-exact). group(1) = the icon's point count.
    private static final Pattern LORE_PATTERN = Pattern.compile("^.*§7(-?\\d+) points§r.*§6-?\\d+ points$");

    private final Map<Skill, Integer> assignedSkillPoints = new EnumMap<>(Skill.class);

    public SkillPointModel() {
        for (Skill s : Skill.values()) assignedSkillPoints.put(s, 0);
    }

    public int getAssignedSkillPoints(Skill skill) {
        if (skill == null) return 0;
        return assignedSkillPoints.getOrDefault(skill, 0);
    }

    @SubscribeEvent
    public void onContainerSetContent(ContainerSetContentEvent.Pre event) {
        if (event.getContainerId() == 0) return;
        if (!(Models.Container.getCurrentContainer() instanceof CharacterInfoContainer)) return;

        List<ItemStack> items = event.getItems();
        Skill[] skills = Skill.values();
        for (int i = 0; i < SKILL_POINT_TOTAL_SLOTS.length && i < skills.length; i++) {
            int slot = SKILL_POINT_TOTAL_SLOTS[i];
            if (slot >= items.size()) continue;
            Integer points = parsePoints(items.get(slot));
            if (points != null) assignedSkillPoints.put(skills[i], points);
        }
    }

    @SubscribeEvent
    public void onScreenClose(ContainerCloseEvent.Post e) {
        // Keep the last-read values cached; nothing to reset.
    }

    private static Integer parsePoints(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return null;
        for (Text line : lore.lines()) {
            Matcher m = StyledText.fromComponent(line).getMatcher(LORE_PATTERN);
            if (m.matches()) {
                try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) { return null; }
            }
        }
        return null;
    }
}
