package julianh06.wynnextras.wynncraft.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.EnumMap;

/** Shared structural decoder for modern full-build links. */
public final class WynnBuilderBuildCodec {
    private static final int VECTOR_FLAG_MIN = 12;
    private static final int VERSION_BITS = 10;
    private static final int EQUIPMENT_SLOTS = 9;
    private static final int MIN_BUILD_HASH_LENGTH = 26;

    private WynnBuilderBuildCodec() {}

    public static String encode(WynnBuilderBuild build) {
        WynnBuilderBitVector bits = new WynnBuilderBitVector()
                .append(VECTOR_FLAG_MIN, 6).append(build.dataVersionIndex(), VERSION_BITS);
        for (var slot : WynnBuilderBuild.EquipmentSlot.values()) {
            WynnBuilderBuild.Equipment piece = build.equipment().getOrDefault(
                    slot, new WynnBuilderBuild.Equipment.None());
            if (piece instanceof WynnBuilderBuild.Equipment.None) {
                bits.append(0, 2).append(0, 13);
            } else if (piece instanceof WynnBuilderBuild.Equipment.Normal normal) {
                bits.append(0, 2).append(normal.itemId() + 1, 13);
            } else if (piece instanceof WynnBuilderBuild.Equipment.Crafted crafted) {
                String encoded = WynnBuilderCraftCodec.encode(crafted.craft(), slot == WynnBuilderBuild.EquipmentSlot.WEAPON);
                bits.append(1, 2).appendBase64(encoded, encoded.length() * 6);
            } else if (piece instanceof WynnBuilderBuild.Equipment.Custom custom) {
                int characters = (custom.payload().bitLength() + 5) / 6;
                bits.append(2, 2).append(characters, 12)
                        .appendBase64(custom.payload().base64(), characters * 6);
            }
            if (slot.powderable()) appendPowders(bits, build.powders().getOrDefault(slot, List.of()));
        }
        boolean hasTomes = build.tomeIds().stream().anyMatch(java.util.Objects::nonNull);
        bits.appendBit(hasTomes);
        if (hasTomes) {
            for (int i = 0; i < 14; i++) {
                Integer id = i < build.tomeIds().size() ? build.tomeIds().get(i) : null;
                bits.appendBit(id != null);
                if (id != null) bits.append(id, 8);
            }
        }
        Integer[] assigned = build.assignedSkillPoints();
        boolean automatic = java.util.Arrays.stream(assigned).allMatch(java.util.Objects::isNull);
        bits.appendBit(automatic);
        if (!automatic) {
            for (Integer value : assigned) {
                bits.appendBit(value != null);
                if (value != null) bits.append(value & 0xFFF, 12);
            }
        }
        bits.appendBit(build.level() != 121);
        if (build.level() != 121) bits.append(build.level(), 7);
        boolean hasAspects = build.aspects().stream().anyMatch(java.util.Objects::nonNull);
        bits.appendBit(hasAspects);
        if (hasAspects) {
            for (int i = 0; i < 5; i++) {
                WynnBuilderBuild.Aspect aspect = i < build.aspects().size() ? build.aspects().get(i) : null;
                bits.appendBit(aspect != null);
                if (aspect != null) bits.append(aspect.id(), 5).append(aspect.tier() - 1, 2);
            }
        }
        bits.appendBase64(build.abilityTree().base64(), build.abilityTree().bitLength());
        return bits.toBase64();
    }

    public static boolean isBuildHash(String payload) {
        String hash = WynnBuilderEncoding.hash(payload);
        return hash.length() >= MIN_BUILD_HASH_LENGTH && !hash.startsWith("CR-")
                && WynnBuilderEncoding.isValid(hash)
                && WynnBuilderEncoding.value(hash.charAt(0)) >= VECTOR_FLAG_MIN;
    }

    public static int dataVersionIndex(String payload) {
        WynnBuilderBitCursor cursor = cursor(payload);
        cursor.read(6);
        return cursor.read(VERSION_BITS);
    }

    public static List<WynnBuilderCraft> craftedItems(
            String payload, IntFunction<Boolean> weaponRecipeLookup
    ) {
        WynnBuilderBitCursor cursor = cursor(payload);
        if (cursor.read(6) < VECTOR_FLAG_MIN) return List.of();
        cursor.read(VERSION_BITS);
        List<WynnBuilderCraft> crafts = new ArrayList<>();
        for (int slot = 0; slot < EQUIPMENT_SLOTS; slot++) {
            int kind = cursor.read(2);
            switch (kind) {
                case 0 -> cursor.skip(13);
                case 1 -> {
                    int start = cursor.bitIndex();
                    WynnBuilderCraft craft = WynnBuilderCraftCodec.decodeEmbedded(cursor, true);
                    if (craft == null) throw new IllegalArgumentException("Unsupported embedded WynnBuilder craft");
                    Boolean weapon = weaponRecipeLookup.apply(craft.recipeId());
                    if (weapon == null) throw new IllegalArgumentException("Unknown WynnBuilder recipe ID: " + craft.recipeId());
                    craft = WynnBuilderCraftCodec.finishEmbedded(cursor, start, craft, weapon);
                    crafts.add(craft);
                }
                case 2 -> cursor.skip(cursor.read(12));
                default -> throw new IllegalArgumentException("Unknown WynnBuilder equipment kind: " + kind);
            }
            if (powderable(slot) && cursor.read(1) == 1) skipPowders(cursor);
        }
        return List.copyOf(crafts);
    }

    /** Decodes the current V12 layout into a typed build without resolving numeric item IDs. */
    public static WynnBuilderBuild decode(String payload, IntFunction<Boolean> weaponRecipeLookup) {
        WynnBuilderBitCursor cursor = cursor(payload);
        cursor.read(6);
        int version = cursor.read(VERSION_BITS);
        Map<WynnBuilderBuild.EquipmentSlot, WynnBuilderBuild.Equipment> equipment =
                new EnumMap<>(WynnBuilderBuild.EquipmentSlot.class);
        Map<WynnBuilderBuild.EquipmentSlot, List<WynnBuilderBuild.Powder>> powders =
                new EnumMap<>(WynnBuilderBuild.EquipmentSlot.class);
        for (var slot : WynnBuilderBuild.EquipmentSlot.values()) {
            int kind = cursor.read(2);
            WynnBuilderBuild.Equipment piece = switch (kind) {
                case 0 -> {
                    int id = cursor.read(13);
                    yield id == 0 ? new WynnBuilderBuild.Equipment.None()
                            : new WynnBuilderBuild.Equipment.Normal(id - 1);
                }
                case 1 -> {
                    int start = cursor.bitIndex();
                    WynnBuilderCraft craft = WynnBuilderCraftCodec.decodeEmbedded(cursor, true);
                    if (craft == null) throw new IllegalArgumentException("Unsupported embedded WynnBuilder craft");
                    Boolean weapon = weaponRecipeLookup.apply(craft.recipeId());
                    if (weapon == null) throw new IllegalArgumentException("Unknown WynnBuilder recipe ID: " + craft.recipeId());
                    craft = WynnBuilderCraftCodec.finishEmbedded(cursor, start, craft, weapon);
                    yield new WynnBuilderBuild.Equipment.Crafted(craft);
                }
                case 2 -> {
                    int bitLength = cursor.read(12) * 6;
                    yield new WynnBuilderBuild.Equipment.Custom(readPayload(cursor, bitLength));
                }
                default -> throw new IllegalArgumentException("Unknown WynnBuilder equipment kind: " + kind);
            };
            equipment.put(slot, piece);
            powders.put(slot, slot.powderable() && cursor.read(1) == 1 ? readPowders(cursor) : List.of());
        }

        List<Integer> tomes = new ArrayList<>(java.util.Collections.nCopies(14, null));
        if (cursor.read(1) == 1) {
            for (int i = 0; i < tomes.size(); i++) if (cursor.read(1) == 1) tomes.set(i, cursor.read(8));
        }
        Integer[] skills = new Integer[5];
        if (cursor.read(1) == 0) {
            for (int i = 0; i < skills.length; i++) if (cursor.read(1) == 1) skills[i] = cursor.readSigned(12);
        }
        int level = cursor.read(1) == 0 ? 121 : cursor.read(7);
        List<WynnBuilderBuild.Aspect> aspects = new ArrayList<>(java.util.Collections.nCopies(5, null));
        if (cursor.read(1) == 1) {
            for (int i = 0; i < aspects.size(); i++) {
                if (cursor.read(1) == 1) aspects.set(i, new WynnBuilderBuild.Aspect(cursor.read(5), cursor.read(2) + 1));
            }
        }
        WynnBuilderBuild.BitPayload tree = readPayload(cursor, cursor.remaining());
        return new WynnBuilderBuild(version, equipment, powders, tomes, skills, level, aspects, tree);
    }

    private static WynnBuilderBitCursor cursor(String payload) {
        String hash = WynnBuilderEncoding.hash(payload);
        if (!isBuildHash(hash)) throw new IllegalArgumentException("Not a modern WynnBuilder build link");
        return new WynnBuilderBitCursor(hash);
    }

    private static void skipPowders(WynnBuilderBitCursor cursor) {
        cursor.skip(6);
        for (int guard = 0; guard < 512; guard++) {
            if (cursor.read(1) == 0) continue;
            if (cursor.read(1) == 0) { cursor.skip(2); continue; }
            if (cursor.read(1) == 0) { cursor.skip(6); continue; }
            return;
        }
        throw new IllegalArgumentException("Powder data did not terminate");
    }

    private static void appendPowders(WynnBuilderBitVector bits, List<WynnBuilderBuild.Powder> powders) {
        bits.appendBit(!powders.isEmpty());
        if (powders.isEmpty()) return;
        WynnBuilderBuild.Powder previous = powders.getFirst();
        bits.append(powderCode(previous), 6);
        for (int i = 1; i < powders.size(); i++) {
            WynnBuilderBuild.Powder powder = powders.get(i);
            if (powder.equals(previous)) {
                bits.appendBit(false);
            } else if (powder.tier() == previous.tier()) {
                int delta = Math.floorMod(powder.element() - previous.element() - 1, 5);
                if (delta < 4) bits.appendBit(true).appendBit(false).append(delta, 2);
                else bits.append(3, 2).appendBit(false).append(powderCode(powder), 6);
            } else {
                bits.append(3, 2).appendBit(false).append(powderCode(powder), 6);
            }
            previous = powder;
        }
        bits.append(7, 3);
    }

    private static int powderCode(WynnBuilderBuild.Powder powder) {
        if (powder.element() < 0 || powder.element() > 4 || powder.tier() < 1 || powder.tier() > 7) {
            throw new IllegalArgumentException("Invalid powder");
        }
        return powder.element() * 7 + powder.tier() - 1;
    }

    private static List<WynnBuilderBuild.Powder> readPowders(WynnBuilderBitCursor cursor) {
        List<WynnBuilderBuild.Powder> result = new ArrayList<>();
        WynnBuilderBuild.Powder previous = powder(cursor.read(6));
        result.add(previous);
        for (int guard = 0; guard < 512; guard++) {
            if (cursor.read(1) == 0) {
                result.add(previous);
            } else if (cursor.read(1) == 0) {
                int element = (previous.element() + cursor.read(2) + 1) % 5;
                previous = new WynnBuilderBuild.Powder(element, previous.tier());
                result.add(previous);
            } else if (cursor.read(1) == 0) {
                previous = powder(cursor.read(6));
                result.add(previous);
            } else {
                return List.copyOf(result);
            }
        }
        throw new IllegalArgumentException("Powder data did not terminate");
    }

    private static WynnBuilderBuild.Powder powder(int encoded) {
        return new WynnBuilderBuild.Powder(encoded / 7, encoded % 7 + 1);
    }

    private static WynnBuilderBuild.BitPayload readPayload(WynnBuilderBitCursor cursor, int length) {
        WynnBuilderBitVector bits = new WynnBuilderBitVector();
        for (int i = 0; i < length; i++) bits.appendBit(cursor.readBit());
        return new WynnBuilderBuild.BitPayload(bits.toBase64(), length);
    }

    private static boolean powderable(int slot) {
        return slot < 4 || slot == 8;
    }
}
