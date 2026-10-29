package julianh06.wynnextras.wynncraft.codec;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Versioned, UI-independent representation of a complete WynnBuilder link. */
public record WynnBuilderBuild(
        int dataVersionIndex,
        Map<EquipmentSlot, Equipment> equipment,
        Map<EquipmentSlot, List<Powder>> powders,
        List<Integer> tomeIds,
        Integer[] assignedSkillPoints,
        int level,
        List<Aspect> aspects,
        BitPayload abilityTree
) {
    public WynnBuilderBuild {
        equipment = Map.copyOf(new EnumMap<>(equipment));
        EnumMap<EquipmentSlot, List<Powder>> powderCopy = new EnumMap<>(EquipmentSlot.class);
        powders.forEach((slot, values) -> powderCopy.put(slot, List.copyOf(values)));
        powders = Map.copyOf(powderCopy);
        tomeIds = java.util.Collections.unmodifiableList(new ArrayList<>(tomeIds));
        assignedSkillPoints = assignedSkillPoints.clone();
        aspects = java.util.Collections.unmodifiableList(new ArrayList<>(aspects));
    }

    @Override public Integer[] assignedSkillPoints() { return assignedSkillPoints.clone(); }

    public enum EquipmentSlot {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS, RING_1, RING_2, BRACELET, NECKLACE, WEAPON;
        public boolean powderable() { return ordinal() < 4 || this == WEAPON; }
    }

    public sealed interface Equipment {
        record None() implements Equipment {}
        record Normal(int itemId) implements Equipment {}
        record Crafted(WynnBuilderCraft craft) implements Equipment {}
        record Custom(BitPayload payload) implements Equipment {}
    }

    public record Powder(int element, int tier) {}
    public record Aspect(int id, int tier) {}
    public record BitPayload(String base64, int bitLength) {}
}
