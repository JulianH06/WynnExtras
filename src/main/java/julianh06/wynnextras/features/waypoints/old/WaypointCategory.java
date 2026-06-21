package julianh06.wynnextras.features.waypoints.old;

import com.wynntils.utils.colors.CustomColor;

public class WaypointCategory {
    public String id;
    public String name;
    public CustomColor color;
    public float alpha;
    public boolean showBlockByDefault;
    public boolean showNameByDefault;
    public boolean showDistanceByDefault;
    public boolean showSeeThroughByDefault;

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
