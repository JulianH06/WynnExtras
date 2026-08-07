package julianh06.wynnextras.utils.colors;

import net.minecraft.util.Formatting;

import java.awt.Color;

public record CustomColor(int r, int g, int b, int a) {
    public static final CustomColor NONE = new CustomColor(255, 255, 255, 255);
    public static final CustomColor WHITE = fromInt(0xFFFFFF);
    public static final CustomColor RED = fromInt(0xFF0000);
    public static final CustomColor YELLOW = fromInt(0xFFFF00);
    public static final CustomColor MAGENTA = fromInt(0xFF00FF);
    public static final CustomColor RAINBOW = fromInt(0x00F000);
    public static final CustomColor SHINE = fromInt(0x00F014);
    public static final CustomColor GRADIENT = fromInt(0x00EFF4);
    public static final CustomColor GRADIENT_2 = fromInt(0x00F010);

    public CustomColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public CustomColor(float r, float g, float b, float a) {
        this(channel(r), channel(g), channel(b), channel(a));
    }

    public CustomColor {
        r = Math.clamp(r, 0, 255);
        g = Math.clamp(g, 0, 255);
        b = Math.clamp(b, 0, 255);
        a = Math.clamp(a, 0, 255);
    }

    public static CustomColor fromInt(int value) {
        int alpha = value >>> 24;
        if (alpha == 0) alpha = 255;
        return new CustomColor(value >> 16 & 255, value >> 8 & 255, value & 255, alpha);
    }

    public static CustomColor fromHexString(String value) {
        String hex = value.trim().replace("#", "");
        if (hex.length() == 6) return fromInt(Integer.parseUnsignedInt(hex, 16));
        if (hex.length() == 8) return fromInt((int) Long.parseLong(hex, 16));
        throw new IllegalArgumentException("Invalid color: " + value);
    }

    public static CustomColor fromHSV(float hue, float saturation, float brightness, float alpha) {
        float normalizedHue = Math.abs(hue) > 1 ? hue / 360f : hue;
        float normalizedBrightness = brightness > 1 ? brightness / 1000f : brightness;
        return fromInt(Color.HSBtoRGB(normalizedHue, Math.clamp(saturation, 0, 1),
                Math.clamp(normalizedBrightness, 0, 1))).withAlpha(alpha);
    }

    public static CustomColor fromChatFormatting(Formatting formatting) {
        Integer value = formatting == null ? null : formatting.getColorValue();
        return fromInt(value == null ? 0xFFFFFF : value);
    }

    public CustomColor withAlpha(float alpha) {
        return withAlpha(channel(alpha));
    }

    public CustomColor withAlpha(int alpha) {
        return new CustomColor(r, g, b, alpha);
    }

    public int asInt() {
        return a << 24 | r << 16 | g << 8 | b;
    }

    public float[] asHSB() {
        return Color.RGBtoHSB(r, g, b, null);
    }

    private static int channel(float value) {
        return Math.round(Math.clamp(value, 0, 1) * 255);
    }
}
