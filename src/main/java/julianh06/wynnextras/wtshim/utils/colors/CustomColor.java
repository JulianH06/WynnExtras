// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — reimplementation of the Wynntils CustomColor API contract.
 * Fresh yarn-mapped code, written to match the public surface WynnExtras uses.
 */
package julianh06.wynnextras.wtshim.utils.colors;

import net.minecraft.util.Formatting;

public record CustomColor(int r, int g, int b, int a) {
    public static final CustomColor NONE = new CustomColor(-1, -1, -1, -1);

    public CustomColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public CustomColor(float r, float g, float b) {
        this(r, g, b, 1f);
    }

    public CustomColor(float r, float g, float b, float a) {
        this((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
    }

    public static CustomColor fromInt(int argb) {
        // Source: Wynntils CustomColor#fromInt — if the alpha byte is 0 (bare RGB int), fill to
        // 0xFF so we don't render fully-transparent for `fromInt(0xRRGGBB)` callers.
        if ((argb & 0xFF000000) == 0) argb |= 0xFF000000;
        return new CustomColor(
                (argb >> 16) & 0xFF,
                (argb >> 8) & 0xFF,
                argb & 0xFF,
                (argb >>> 24) & 0xFF);
    }

    // Source: Wynntils CustomColor#fromARGBInt — interprets the int as packed ARGB, keeping
    // the alpha byte as-is (unlike fromInt, which forces opaque when the alpha byte is 0).
    public static CustomColor fromARGBInt(int num) {
        return new CustomColor(num >> 16 & 255, num >> 8 & 255, num & 255, num >> 24 & 255);
    }

    public static CustomColor fromHexString(String hex) {
        if (hex == null || hex.isEmpty()) return NONE;
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            if (s.length() == 6) {
                int rgb = Integer.parseInt(s, 16);
                return new CustomColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
            } else if (s.length() == 8) {
                long rgba = Long.parseLong(s, 16);
                return new CustomColor(
                        (int) ((rgba >> 24) & 0xFF),
                        (int) ((rgba >> 16) & 0xFF),
                        (int) ((rgba >> 8) & 0xFF),
                        (int) (rgba & 0xFF));
            }
        } catch (NumberFormatException ignored) {}
        return NONE;
    }

    public static CustomColor fromHSV(float h, float s, float v) {
        return fromHSV(h, s, v, 1f);
    }

    public static CustomColor fromHSV(float h, float s, float v, float a) {
        float hh = (h % 1f + 1f) % 1f * 6f;
        int sector = (int) hh;
        float f = hh - sector;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        float rf, gf, bf;
        switch (sector) {
            case 0 -> { rf = v; gf = t; bf = p; }
            case 1 -> { rf = q; gf = v; bf = p; }
            case 2 -> { rf = p; gf = v; bf = t; }
            case 3 -> { rf = p; gf = q; bf = v; }
            case 4 -> { rf = t; gf = p; bf = v; }
            default -> { rf = v; gf = p; bf = q; }
        }
        return new CustomColor(rf, gf, bf, a);
    }

    public static CustomColor fromChatFormatting(Formatting f) {
        Integer c = f.getColorValue();
        if (c == null) return NONE;
        return fromInt(c | 0xFF000000);
    }

    public CustomColor withAlpha(int alpha) {
        return new CustomColor(r, g, b, alpha & 0xFF);
    }

    public CustomColor withAlpha(float alpha) {
        return withAlpha((int) (alpha * 255));
    }

    public int asInt() {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    // Source: Wynntils CustomColor#toHexString — "#rrggbbaa" (RGBA order). Matches fromHexString's
    // 8-digit parse, and is the representation StyledText.getString() emits for non-vanilla colors.
    public String toHexString() {
        return "#" + String.format("%08x", ((r << 24) | (g << 16) | (b << 8) | a));
    }

    /** Returns H, S, B as floats in 0..1. */
    public float[] asHSB() {
        return java.awt.Color.RGBtoHSB(r & 0xFF, g & 0xFF, b & 0xFF, null);
    }
}
