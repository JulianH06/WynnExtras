package julianh06.wynnextras.features.wci.service.wynnbuilder;

final class WynnBuilderBitCursor {
    private final String data;
    private int bit;

    WynnBuilderBitCursor(String data) {
        this.data = data == null ? "" : data;
    }

    int bitIndex() {
        return bit;
    }

    int read(int length) {
        int value = 0;
        for (int i = 0; i < length; i++, bit++) {
            int charIndex = bit / 6;
            if (charIndex >= data.length()) {
                throw new IndexOutOfBoundsException("Unexpected end of WynnBuilder bit payload");
            }
            int chunk = WynnBuilderCraftParser.B64.indexOf(data.charAt(charIndex));
            if (chunk < 0) {
                throw new IllegalArgumentException("Invalid WynnBuilder base64 character: " + data.charAt(charIndex));
            }
            value |= ((chunk >> (bit % 6)) & 1) << i;
        }
        return value;
    }

    void skip(int length) {
        if (length < 0) throw new IllegalArgumentException("Cannot skip negative bits");
        bit += length;
        if (bit > data.length() * 6) {
            throw new IndexOutOfBoundsException("Unexpected end of WynnBuilder bit payload");
        }
    }
}
