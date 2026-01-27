package julianh06.wynnextras.features.waypoints;

public class Waypoint {
    public String name;
    public int x;
    public int y;
    public int z;
    public boolean show;
    public boolean showName;
    public boolean showDistance;
    public boolean seeThrough;
    private transient WaypointCategory category;
    public String categoryName;

    public Waypoint() {
        name = "Waypoint";
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.show = true;
        this.showName = true;
        this.showDistance = true;
        this.seeThrough = false;
        category = null;
        categoryName = "";
    }

    public Waypoint(int x, int y, int z) {
        name = "Waypoint";
        this.x = x;
        this.y = y;
        this.z = z;
        this.show = true;
        this.showName = true;
        this.showDistance = true;
        this.seeThrough = false;
        category = null;
        categoryName = "";
    }

    public WaypointCategory getCategory() {
        return category;
    }

    public void setCategory(WaypointCategory category) {
        this.category = category;
        this.categoryName = category != null ? category.name : null;
    }
}
