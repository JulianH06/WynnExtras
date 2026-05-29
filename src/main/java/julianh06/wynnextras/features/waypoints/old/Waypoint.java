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
    public Boolean showOverride;
    public Boolean showNameOverride;
    public Boolean showDistanceOverride;

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
        showOverride = null;
        showNameOverride = null;
        showDistanceOverride = null;
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
        showOverride = null;
        showNameOverride = null;
        showDistanceOverride = null;
        category = null;
        categoryName = "";
    }

    public WaypointCategory getCategory() { return category; }

    public void setCategory(WaypointCategory category) {
        this.category = category;
        this.categoryId = category != null ? category.id : null;
    }

    public boolean shouldShowBlock() {
        return resolveVisibility(showOverride, show, category == null || category.showBlockByDefault);
    }

    public boolean shouldShowName() {
        return resolveVisibility(showNameOverride, showName, category == null || category.showNameByDefault);
    }

    public boolean shouldShowDistance() {
        return resolveVisibility(showDistanceOverride, showDistance, category == null || category.showDistanceByDefault);
    }

    public void setShowOverride(Boolean showOverride) {
        this.showOverride = showOverride;
        if (showOverride != null) show = showOverride;
    }

    public void setShowNameOverride(Boolean showNameOverride) {
        this.showNameOverride = showNameOverride;
        if (showNameOverride != null) showName = showNameOverride;
    }

    public void setShowDistanceOverride(Boolean showDistanceOverride) {
        this.showDistanceOverride = showDistanceOverride;
        if (showDistanceOverride != null) showDistance = showDistanceOverride;
    }

    public void migrateVisibilityOverridesFromLegacy() {
        if (showOverride == null) showOverride = show;
        if (showNameOverride == null) showNameOverride = showName;
        if (showDistanceOverride == null) showDistanceOverride = showDistance;
    }

    private boolean resolveVisibility(Boolean override, boolean waypointValue, boolean categoryValue) {
        if (override != null) return override;
        return category != null ? categoryValue : waypointValue;
    }

    public String getLegacyCategoryName() { return categoryName; }
}
