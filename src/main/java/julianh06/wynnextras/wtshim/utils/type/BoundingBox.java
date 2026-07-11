// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — BoundingBox 2D. */
package julianh06.wynnextras.wtshim.utils.type;

public record BoundingBox(float x1, float y1, float x2, float y2) {
    public boolean contains(float x, float y) { return x >= x1 && x <= x2 && y >= y1 && y <= y2; }

    /** Axis-aligned overlap test (phase 8b — replaces Wynntils' BoundingShape.intersects for the map). */
    public boolean intersects(BoundingBox o) {
        return x1 <= o.x2 && x2 >= o.x1 && y1 <= o.y2 && y2 >= o.y1;
    }

    public static BoundingBox centered(float cx, float cz, float width, float height) {
        return new BoundingBox(cx - width / 2f, cz - height / 2f, cx + width / 2f, cz + height / 2f);
    }
}
