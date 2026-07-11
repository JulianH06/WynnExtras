// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.models.items.encoding.type;

public enum ItemType {
    GEAR(0),
    TOME(1),
    CHARM(2),
    CRAFTED_GEAR(3),
    CRAFTED_CONSUMABLE(4);

    private final byte encodingId;

    ItemType(int encodingId) {
        this.encodingId = (byte) encodingId;
    }

    public static ItemType fromEncodingId(byte id) {
        for (ItemType itemType : values()) {
            if (itemType.encodingId == id) {
                return itemType;
            }
        }

        return null;
    }

    public byte getEncodingId() {
        return encodingId;
    }
}
