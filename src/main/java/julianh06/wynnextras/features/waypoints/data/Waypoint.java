package julianh06.wynnextras.features.waypoints.data;

import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

public class Waypoint {
    public static final float MIN_SIZE = 0.05f;
    public static final float MAX_SIZE = 64f;

    public String id;
    public String name;
    public int x;
    public int y;
    public int z;
    // Sub block placement, added on top of the block coordinates. Kept separate so the
    // in world editor can keep working on the block grid.
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public float size = 1f;
    public boolean show;
    public boolean showName;
    public boolean showDistance;
    public boolean seeThrough;
    public Boolean showOverride;
    public Boolean showNameOverride;
    public Boolean showDistanceOverride;
    public Boolean seeThroughOverride;

    public String categoryId;
    private transient WaypointCategory category;

    private transient Text nameText;
    private transient String nameTextSource;

    //LEGACY
    @Deprecated public String categoryName;

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
        seeThroughOverride = null;
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
        seeThroughOverride = null;
        category = null;
        categoryName = "";
    }

    public Text getNameText() {
        if (nameText == null || !name.equals(nameTextSource)) {
            nameTextSource = name;
            nameText = Text.of(name);
        }
        return nameText;
    }

    public float getSize() {
        if (size <= 0f) return 1f;
        return Math.min(size, MAX_SIZE);
    }

    public void setSize(float size) {
        this.size = Math.clamp(size, MIN_SIZE, MAX_SIZE);
    }

    public double displayX() { return x + offsetX; }
    public double displayY() { return y + offsetY; }
    public double displayZ() { return z + offsetZ; }

    public void setDisplayX(double value) {
        this.x = (int) Math.floor(value);
        this.offsetX = (float) (value - this.x);
    }

    public void setDisplayY(double value) {
        this.y = (int) Math.floor(value);
        this.offsetY = (float) (value - this.y);
    }

    public void setDisplayZ(double value) {
        this.z = (int) Math.floor(value);
        this.offsetZ = (float) (value - this.z);
    }

    public Box getRenderBox() {
        return boxAt(x, y, z, offsetX, offsetY, offsetZ, getSize());
    }

    public static Box boxAt(int x, int y, int z, float offsetX, float offsetY, float offsetZ, float size) {
        double half = size / 2.0;
        double centerX = x + 0.5 + offsetX;
        double centerY = y + 0.5 + offsetY;
        double centerZ = z + 0.5 + offsetZ;
        return new Box(centerX - half, centerY - half, centerZ - half, centerX + half, centerY + half, centerZ + half);
    }

    public static String formatCoord(double value) {
        double rounded = Math.round(value * 1000.0) / 1000.0;
        if (rounded == Math.rint(rounded)) return String.valueOf((long) Math.rint(rounded));
        return String.valueOf(rounded);
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

    public boolean shouldSeeThrough() {
        return resolveVisibility(seeThroughOverride, seeThrough, category != null && category.showSeeThroughByDefault);
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

    public void setSeeThroughOverride(Boolean seeThroughOverride) {
        this.seeThroughOverride = seeThroughOverride;
        if (seeThroughOverride != null) seeThrough = seeThroughOverride;
    }

    public void migrateVisibilityOverridesFromLegacy() {
        if (showOverride == null) showOverride = show;
        if (showNameOverride == null) showNameOverride = showName;
        if (showDistanceOverride == null) showDistanceOverride = showDistance;
        if (seeThroughOverride == null) seeThroughOverride = seeThrough;
    }

    private boolean resolveVisibility(Boolean override, boolean waypointValue, boolean categoryValue) {
        if (override != null) return override;
        return category != null ? categoryValue : waypointValue;
    }

    public String getLegacyCategoryName() { return categoryName; }
}
