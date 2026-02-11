package julianh06.wynnextras.features.waypoints.old;

public class Waypoint {
    public String id;
    public String name;
    public int x;
    public int y;
    public int z;
    public boolean show;
    public boolean showName;
    public boolean showDistance;
    public boolean seeThrough;

    public String categoryId;
    private transient WaypointCategory category;

    //LEGACY
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
        categoryId = null;
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

    public WaypointCategory getCategory() { return category; }

    public void setCategory(WaypointCategory category) {
        this.category = category;
        this.categoryId = category != null ? category.id : null;
    }

    public String getLegacyCategoryName() { return categoryName; }
}
