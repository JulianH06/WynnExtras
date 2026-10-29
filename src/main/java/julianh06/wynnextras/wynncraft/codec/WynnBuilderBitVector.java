package julianh06.wynnextras.wynncraft.codec;

import java.util.ArrayList;
import java.util.List;

/** Mutable counterpart to {@link WynnBuilderBitCursor}. */
public final class WynnBuilderBitVector {
    private final List<Boolean> bits = new ArrayList<>();

    public WynnBuilderBitVector append(int value, int length) {
        if (length < 0 || length > 31) throw new IllegalArgumentException("Invalid bit length");
        for (int bit = 0; bit < length; bit++) bits.add(((value >> bit) & 1) != 0);
        return this;
    }

    public WynnBuilderBitVector appendBit(boolean value) {
        bits.add(value);
        return this;
    }

    public WynnBuilderBitVector append(WynnBuilderBitVector value) {
        bits.addAll(value.bits);
        return this;
    }

    public WynnBuilderBitVector appendBase64(String value, int bitLength) {
        if (value == null || bitLength < 0 || bitLength > value.length() * 6
                || !WynnBuilderEncoding.isValid(value)) {
            throw new IllegalArgumentException("Invalid WynnBuilder bit payload");
        }
        for (int bit = 0; bit < bitLength; bit++) {
            appendBit((WynnBuilderEncoding.value(value.charAt(bit / 6)) & 1 << bit % 6) != 0);
        }
        return this;
    }

    public int size() { return bits.size(); }

    /** Pads to the next Base64 character, including a complete padding character at a boundary. */
    public WynnBuilderBitVector padToNextCharacter() {
        int padding = 6 - bits.size() % 6;
        for (int i = 0; i < padding; i++) bits.add(false);
        return this;
    }

    public String toBase64() {
        StringBuilder result = new StringBuilder((bits.size() + 5) / 6);
        for (int offset = 0; offset < bits.size(); offset += 6) {
            int value = 0;
            for (int bit = 0; bit < 6 && offset + bit < bits.size(); bit++) {
                if (bits.get(offset + bit)) value |= 1 << bit;
            }
            result.append(WynnBuilderEncoding.ALPHABET.charAt(value));
        }
        return result.toString();
    }
}
