package julianh06.wynnextras.wynncraft.codec;

import java.util.List;

/** V10 fallback for links without a weapon, which WynnBuilder's V12 decoder cannot currently open. */
public final class WynnBuilderLegacyBuildCodec {
    private static final int VERSION = 10;
    private static final int FIRST_NONE_ITEM_ID = 10000;
    private static final int[] NONE_TOME_IDS = {61, 61, 62, 62, 62, 62, 162, 162, 163, 163, 164, 164, 63, 93};

    private WynnBuilderLegacyBuildCodec() {}

    public static String encodeWithoutWeapon(WynnBuilderBuild build) {
        if (!(build.equipment().get(WynnBuilderBuild.EquipmentSlot.WEAPON)
                instanceof WynnBuilderBuild.Equipment.None)) {
            throw new IllegalArgumentException("Legacy fallback is only for builds without a weapon");
        }
        StringBuilder hash = new StringBuilder(VERSION + "_");
        for (var slot : WynnBuilderBuild.EquipmentSlot.values()) {
            WynnBuilderBuild.Equipment equipment = build.equipment().getOrDefault(
                    slot, new WynnBuilderBuild.Equipment.None());
            if (equipment instanceof WynnBuilderBuild.Equipment.None) {
                hash.append(base64(FIRST_NONE_ITEM_ID + slot.ordinal(), 3));
            } else if (equipment instanceof WynnBuilderBuild.Equipment.Normal normal) {
                hash.append(base64(normal.itemId(), 3));
            } else if (equipment instanceof WynnBuilderBuild.Equipment.Crafted crafted) {
                hash.append("CR-").append(WynnBuilderCraftCodec.encode(crafted.craft(), false));
            } else {
                throw new IllegalArgumentException("Custom items need the modern WynnBuilder format");
            }
        }
        for (Integer skill : build.assignedSkillPoints()) hash.append(base64(skill == null ? 0 : skill & 0xFFF, 2));
        hash.append(base64(build.level(), 2));
        for (var slot : WynnBuilderBuild.EquipmentSlot.values()) {
            if (slot.powderable()) appendPowders(hash, build.powders().getOrDefault(slot, List.of()));
        }
        for (int i = 0; i < NONE_TOME_IDS.length; i++) {
            Integer tome = i < build.tomeIds().size() ? build.tomeIds().get(i) : null;
            hash.append(base64(tome == null ? NONE_TOME_IDS[i] : tome, 2));
        }
        return hash.toString();
    }

    private static void appendPowders(StringBuilder hash, List<WynnBuilderBuild.Powder> powders) {
        hash.append(base64((powders.size() + 5) / 6, 1));
        for (int start = 0; start < powders.size(); start += 6) {
            int packed = 0;
            int end = Math.min(start + 6, powders.size());
            for (int i = end - 1; i >= start; i--) {
                WynnBuilderBuild.Powder powder = powders.get(i);
                packed = packed << 5 | powder.element() * 7 + powder.tier();
            }
            hash.append(base64(packed, 5));
        }
    }

    private static String base64(int value, int characters) {
        char[] encoded = new char[characters];
        for (int i = characters - 1; i >= 0; i--) {
            encoded[i] = WynnBuilderEncoding.ALPHABET.charAt(value & 63);
            value >>>= 6;
        }
        if (value != 0) throw new IllegalArgumentException("Value does not fit in WynnBuilder field");
        return new String(encoded);
    }
}
