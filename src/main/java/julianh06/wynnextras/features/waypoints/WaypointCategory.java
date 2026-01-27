package julianh06.wynnextras.features.waypoints;

import com.wynntils.utils.colors.CustomColor;

public class WaypointCategory {
    public String name;
    public CustomColor color;
    public float alpha;

    public WaypointCategory(String name) {
        this.name = name;
        color = CustomColor.fromHexString("FFFFFF");
        alpha = 0.5f;
    }

    public WaypointCategory(String name, CustomColor color) {
        this.name = name;
        this.color = color;
        alpha = 0.5f;
    }
}
