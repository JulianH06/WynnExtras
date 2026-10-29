package julianh06.wynnextras.wynncraft.codec;

/** Least-significant-bit-first reader used by WynnBuilder's modern encodings. */
public final class WynnBuilderBitCursor {
    private final String data;
    private int bit;

    public WynnBuilderBitCursor(String data) {
        if (!WynnBuilderEncoding.isValid(data)) throw new IllegalArgumentException("Invalid WynnBuilder payload");
        this.data = data;
    }

    public int bitIndex() { return bit; }
    public int remaining() { return data.length() * 6 - bit; }

    public int read(int length) {
        if (length < 0 || length > 31) throw new IllegalArgumentException("Invalid bit length");
        if (remaining() < length) throw new IllegalArgumentException("Unexpected end of WynnBuilder bit payload");
        int value = 0;
        for (int i = 0; i < length; i++, bit++) {
            int chunk = WynnBuilderEncoding.value(data.charAt(bit / 6));
            value |= ((chunk >> (bit % 6)) & 1) << i;
        }
        return value;
    }

    public boolean readBit() { return read(1) != 0; }

    public int readSigned(int length) {
        int value = read(length);
        int sign = 1 << (length - 1);
        return (value & sign) == 0 ? value : value - (1 << length);
    }

    public void position(int bit) {
        if (bit < 0 || bit > data.length() * 6) throw new IllegalArgumentException("Invalid bit position");
        this.bit = bit;
    }

    public void skip(int length) {
        if (length < 0) throw new IllegalArgumentException("Cannot skip negative bits");
        if (remaining() < length) throw new IllegalArgumentException("Unexpected end of WynnBuilder bit payload");
        bit += length;
    }
}
