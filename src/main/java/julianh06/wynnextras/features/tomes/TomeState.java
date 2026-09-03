package julianh06.wynnextras.features.tomes;

import julianh06.wynnextras.wynncraft.item.WynnItemData;
import julianh06.wynnextras.wynncraft.item.WynnItemParser;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import julianh06.wynnextras.wynncraft.state.CharacterState;
import julianh06.wynnextras.wynncraft.state.SkillPoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TomeState {
    private static final Map<Integer, TomeType> TOME_SLOTS = tomeSlots();

    private static String loadedCharacterId;
    private static List<EquippedTome> equipped = List.of();

    private TomeState() {}

    public static void updateFromCurrentMenu() {
        syncCharacter();
        if (loadedCharacterId == null || !WynncraftMenuService.isCurrent(MenuType.TOME)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;

        List<EquippedTome> scanned = new ArrayList<>();
        for (Map.Entry<Integer, TomeType> entry : TOME_SLOTS.entrySet()) {
            int slotIndex = entry.getKey();
            if (slotIndex >= screen.getScreenHandler().slots.size()) continue;
            ItemStack stack = screen.getScreenHandler().slots.get(slotIndex).getStack();
            if (isEquippedTome(stack)) scanned.add(new EquippedTome(slotIndex, entry.getValue(), stack));
        }

        List<EquippedTome> snapshot = List.copyOf(scanned);
        if (sameEquipment(equipped, snapshot)) return;
        equipped = snapshot;
        LocalTomeStorage.save(loadedCharacterId, equipped);
    }

    public static int[] guildSkillBonuses() {
        syncCharacter();
        for (EquippedTome tome : equipped) {
            if (tome.type() == TomeType.GUILD) return skillBonuses(tome.stack());
        }
        return new int[SkillPoint.values().length];
    }

    private static void syncCharacter() {
        String characterId = CharacterState.id().orElse(null);
        if (Objects.equals(characterId, loadedCharacterId)) return;
        loadedCharacterId = characterId;
        equipped = characterId == null ? List.of() : LocalTomeStorage.load(characterId);
    }

    private static int[] skillBonuses(ItemStack stack) {
        Optional<WynnItemData> parsed = WynnItemParser.parse(stack);
        if (parsed.isPresent()) {
            int[] bonuses = parsed.get().bonusesArray();
            if (hasBonus(bonuses)) return bonuses;

            for (SkillPoint skill : SkillPoint.values()) {
                String key = skill.name().toLowerCase(Locale.ROOT);
                int value = parsed.get().identifications().getOrDefault(key,
                        parsed.get().identifications().getOrDefault("raw" + key, 0));
                bonuses[skill.ordinal()] = value;
            }
            if (hasBonus(bonuses)) return bonuses;
        }

        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        int[] fallback = new int[SkillPoint.values().length];
        if (name.contains("assimilator's tome of allegiance")) {
            java.util.Arrays.fill(fallback, 1);
        } else if (name.contains("brute's tome of allegiance")) {
            fallback[SkillPoint.STRENGTH.ordinal()] = 4;
        } else if (name.contains("sadist's tome of allegiance")) {
            fallback[SkillPoint.DEXTERITY.ordinal()] = 4;
        } else if (name.contains("mastermind's tome of allegiance")) {
            fallback[SkillPoint.INTELLIGENCE.ordinal()] = 4;
        } else if (name.contains("arsonist's tome of allegiance")) {
            fallback[SkillPoint.DEFENCE.ordinal()] = 4;
        } else if (name.contains("ghost's tome of allegiance")) {
            fallback[SkillPoint.AGILITY.ordinal()] = 4;
        }
        return fallback;
    }

    private static boolean isEquippedTome(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        return name.startsWith("tome of ") || name.contains(" tome of ");
    }

    private static boolean hasBonus(int[] bonuses) {
        for (int bonus : bonuses) if (bonus != 0) return true;
        return false;
    }

    private static boolean sameEquipment(List<EquippedTome> left, List<EquippedTome> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            EquippedTome a = left.get(i);
            EquippedTome b = right.get(i);
            if (a.slot() != b.slot() || a.type() != b.type()
                    || a.stack().getCount() != b.stack().getCount()
                    || !ItemStack.areItemsAndComponentsEqual(a.stack(), b.stack())) return false;
        }
        return true;
    }

    private static Map<Integer, TomeType> tomeSlots() {
        LinkedHashMap<Integer, TomeType> slots = new LinkedHashMap<>();
        slots.put(4, TomeType.GUILD);
        slots.put(11, TomeType.COMBAT_MASTERY);
        slots.put(19, TomeType.COMBAT_MASTERY);
        slots.put(28, TomeType.MYSTICISM);
        slots.put(38, TomeType.MYSTICISM);
        slots.put(49, TomeType.LOOTRUNNING);
        slots.put(42, TomeType.EXPERTISE);
        slots.put(34, TomeType.EXPERTISE);
        slots.put(25, TomeType.MARATHON);
        slots.put(14, TomeType.MARATHON);
        slots.put(22, TomeType.DEFENSIVE_MASTERY);
        slots.put(30, TomeType.DEFENSIVE_MASTERY);
        slots.put(31, TomeType.DEFENSIVE_MASTERY);
        slots.put(32, TomeType.DEFENSIVE_MASTERY);
        return Collections.unmodifiableMap(slots);
    }
}
