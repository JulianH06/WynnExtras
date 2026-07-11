// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — WynncraftShaderColor.
 * Cycling shader-style colors referenced by a few UI pages. Pure lookup; no shader code.
 */
package julianh06.wynnextras.wtshim.utils.colors;

public enum WynncraftShaderColor {
    RAINBOW(CommonColors.MAGENTA),
    SHINE(CommonColors.YELLOW),
    // Representative start-colours of Wynncraft's animated gradients (real: GRADIENT #f56217→#0b486b,
    // GRADIENT_2 #560505→#8a0303) — the shim has no shader, only a lookup colour for badge display.
    GRADIENT(CustomColor.fromInt(0xF56217)),
    GRADIENT_2(CustomColor.fromInt(0x560505));

    public final CustomColor color;

    WynncraftShaderColor(CustomColor color) {
        this.color = color;
    }
}
