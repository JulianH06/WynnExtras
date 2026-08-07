package julianh06.wynnextras.utils.color;

import java.util.ArrayList;
import java.util.List;

public final class ShaderColor {
    private final List<Integer> colors = new ArrayList<>();
    private float fadeSpeed = 1.0f;
    private int lastColor = 0xFFFFFF;

    public ShaderColor add(int rgb) {
        colors.add(rgb & 0xFFFFFF);
        return this;
    }

    public ShaderColor add(String hex) {
        String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
        if (!normalized.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Color must be a six-digit hex value");
        }
        return add(Integer.parseInt(normalized, 16));
    }

    public ShaderColor fadeSpeed(float fadeSpeed) {
        if (fadeSpeed <= 0) throw new IllegalArgumentException("Fade speed must be greater than zero");
        this.fadeSpeed = fadeSpeed;
        return this;
    }

    public int currentColor() {
        if (colors.isEmpty()) return lastColor;
        if (colors.size() == 1) return useColor(colors.getFirst());

        double progress = (System.currentTimeMillis() / 1000.0 * fadeSpeed) % colors.size();
        int fromIndex = (int) progress;
        int from = colors.get(fromIndex);
        int to = colors.get((fromIndex + 1) % colors.size());
        float blend = (float) (progress - fromIndex);

        return useColor(interpolate(from, to, blend));
    }

    private int useColor(int color) {
        if (isWynnShaderColor(color)) return lastColor;

        lastColor = color;
        return color;
    }

    private static int interpolate(int from, int to, float blend) {
        int red = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * blend);
        int green = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * blend);
        int blue = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * blend);
        return red << 16 | green << 8 | blue;
    }

    private static boolean isWynnShaderColor(int color) {
        if((color >> 8 & 0xEE) >= 0xE8) return true;
        if((color >> 8 & 0xEE) <= 0x30 && (color >> 8 & 0xEE) >= 0x27) return true;
        return false;
    }
}