package julianh06.wynnextras.features.waypoints;

import com.wynntils.utils.colors.CustomColor;

public class WaypointCategory {
    public String name;
    public CustomColor color;

    public WaypointCategory(String name) {
        this.name = name;
        color = CustomColor.fromHexString("FFFFFF");
    }

    public WaypointCategory(String name, CustomColor color) {
        this.name = name;
        this.color = color;
    }
}
