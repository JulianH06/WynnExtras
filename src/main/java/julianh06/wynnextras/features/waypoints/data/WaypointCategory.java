package julianh06.wynnextras.features.waypoints.data;

import julianh06.wynnextras.utils.colors.CustomColor;

import java.awt.Color;

public class WaypointCategory {
    public String id;
    public String name;
    public CustomColor color;
    public float alpha;
    public boolean showBlockByDefault;
    public boolean showNameByDefault;
    public boolean showDistanceByDefault;
    public boolean showSeeThroughByDefault;

    private transient Color cachedAwtColor;
    private transient int cachedAwtColorSource = Integer.MIN_VALUE;

    public Color asAwtColor() {
        int rgb = color.asInt();
        if (cachedAwtColor == null || rgb != cachedAwtColorSource) {
            float[] hsb = color.asHSB();
            cachedAwtColor = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
            cachedAwtColorSource = rgb;
        }
        return cachedAwtColor;
    }

    public WaypointCategory() {
        this.id = java.util.UUID.randomUUID().toString();
        name = "New Category";
        color = CustomColor.fromHexString("FFFFFF");
        alpha = 1.0f;
        showBlockByDefault = true;
        showNameByDefault = true;
        showDistanceByDefault = true;
        showSeeThroughByDefault = false;
    }

    public WaypointCategory(String name) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        color = CustomColor.fromHexString("FFFFFF");
        alpha = 0.5f;
        showBlockByDefault = true;
        showNameByDefault = true;
        showDistanceByDefault = true;
        showSeeThroughByDefault = false;
    }

    public WaypointCategory(String name, CustomColor color) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.color = color;
        alpha = 0.5f;
        showBlockByDefault = true;
        showNameByDefault = true;
        showDistanceByDefault = true;
        showSeeThroughByDefault = false;
    }
}
