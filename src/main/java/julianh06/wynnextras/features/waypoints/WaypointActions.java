package julianh06.wynnextras.features.waypoints;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.features.waypoints.old.Waypoint;
import julianh06.wynnextras.features.waypoints.old.WaypointCategory;
import julianh06.wynnextras.features.waypoints.old.WaypointData;
import julianh06.wynnextras.features.waypoints.old.WaypointPackage;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WaypointActions {
    public static final int MAX_PACKAGE_NAME_LENGTH = 50;

    private WaypointActions() {}

    public static WaypointPackage createPackage(String baseName) {
        WaypointPackage pkg = new WaypointPackage(uniquePackageName(null, cleanPackageName(baseName, "New Package")));
        pkg.description = "";
        WaypointData.ensureUncategorizedCategory(pkg);
        WaypointData.INSTANCE.packages.add(pkg);
        savePackagesAndOrder();
        return pkg;
    }

    public static void renamePackage(WaypointPackage pkg, String name) {
        if (pkg == null) return;
        String oldName = pkg.name;
        String newName = uniquePackageName(pkg, cleanPackageName(name, "New Package"));
        if (newName.equals(oldName)) return;

        pkg.name = newName;
        WaypointData.save();
        deletePackageFile(oldName);
        OrderManager.saveOrder(WaypointData.INSTANCE.packages);
    }

    public static void setPackageDescription(WaypointPackage pkg, String description) {
        if (pkg == null) return;
        pkg.description = description == null ? "" : description.trim();
        WaypointData.save();
    }

    public static void setPackageEnabled(WaypointPackage pkg, boolean enabled) {
        if (pkg == null) return;
        pkg.enabled = enabled;
        WaypointData.save();
    }

    public static void deletePackage(WaypointPackage pkg) {
        if (pkg == null) return;
        String oldName = pkg.name;
        WaypointData.INSTANCE.packages.remove(pkg);
        if (WaypointData.INSTANCE.activePackage == pkg) WaypointData.INSTANCE.activePackage = null;
        if (WaypointEditMode.activePackage == pkg) {
            WaypointEditMode.activePackage = null;
            WaypointEditMode.activeCategory = null;
        }
        deletePackageFile(oldName);
        if (WaypointData.INSTANCE.packages.isEmpty()) {
            createPackage("Default");
        } else {
            savePackagesAndOrder();
        }
    }

    public static WaypointPackage duplicatePackage(WaypointPackage original) {
        if (original == null) return null;
        WaypointData.ensureUncategorizedCategory(original);
        WaypointData.resolveWaypointCategories(original);

        WaypointPackage copy = new WaypointPackage(WaypointData.INSTANCE.generateUniqueName(original.name == null ? "Package" : original.name));
        copy.description = original.description;
        copy.enabled = original.enabled;
        copy.packageVersion = WaypointData.CURRENT_PACKAGE_VERSION;

        Map<String, WaypointCategory> categoryCopies = new HashMap<>();
        for (WaypointCategory category : original.categories) {
            WaypointCategory categoryCopy = copyCategory(category);
            copy.categories.add(categoryCopy);
            categoryCopies.put(category.id, categoryCopy);
        }

        WaypointCategory uncategorized = WaypointData.ensureUncategorizedCategory(copy);
        categoryCopies.put(WaypointData.UNCATEGORIZED_CATEGORY_ID, uncategorized);
        for (Waypoint waypoint : original.waypoints) {
            Waypoint waypointCopy = copyWaypoint(waypoint);
            WaypointCategory categoryCopy = categoryCopies.get(waypoint.categoryId);
            waypointCopy.setCategory(categoryCopy == null ? uncategorized : categoryCopy);
            copy.waypoints.add(waypointCopy);
        }

        WaypointData.INSTANCE.packages.add(copy);
        savePackagesAndOrder();
        return copy;
    }

    public static WaypointPackage importPackageFromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Clipboard is empty.");
        }

        WaypointPackage imported = WaypointData.gson.fromJson(json, WaypointPackage.class);
        if (imported == null) {
            throw new IllegalArgumentException("Clipboard does not contain a valid package.");
        }

        normalizeImportedPackage(imported);
        WaypointData.INSTANCE.packages.add(imported);
        savePackagesAndOrder();
        return imported;
    }

    public static Waypoint createWaypoint(WaypointPackage pkg, WaypointCategory category, BlockPos pos) {
        if (pkg == null) return null;
        WaypointData.resolveWaypointCategories(pkg);
        Waypoint waypoint = new Waypoint(pos.getX(), pos.getY(), pos.getZ());
        waypoint.id = UUID.randomUUID().toString();
        waypoint.setCategory(category != null && pkg.categories.contains(category) ? category : WaypointData.ensureUncategorizedCategory(pkg));
        pkg.waypoints.add(waypoint);
        WaypointData.save();
        return waypoint;
    }

    public static Waypoint duplicateWaypoint(WaypointPackage pkg, Waypoint waypoint) {
        if (pkg == null || waypoint == null) return null;
        Waypoint copy = copyWaypoint(waypoint);
        copy.name = uniqueWaypointName(pkg, waypoint.name == null || waypoint.name.isBlank() ? "Waypoint" : waypoint.name);
        copy.setCategory(waypoint.getCategory() == null ? WaypointData.ensureUncategorizedCategory(pkg) : waypoint.getCategory());
        pkg.waypoints.add(copy);
        WaypointData.save();
        return copy;
    }

    public static void deleteWaypoint(WaypointPackage pkg, Waypoint waypoint) {
        if (pkg == null || waypoint == null) return;
        pkg.waypoints.remove(waypoint);
        if (WaypointEditMode.selectedWaypoint == waypoint) {
            WaypointEditMode.selectedWaypoint = null;
            WaypointEditMode.selectedWaypointPackage = null;
            WaypointEditMode.selectedSnapshot = null;
        }
        WaypointData.save();
    }

    public static void updateWaypoint(Waypoint waypoint, String name, Integer x, Integer y, Integer z) {
        if (waypoint == null) return;
        waypoint.name = cleanName(name, "Waypoint");
        if (x != null) waypoint.x = x;
        if (y != null) waypoint.y = y;
        if (z != null) waypoint.z = z;
        WaypointData.save();
    }

    public static void setWaypointCategory(Waypoint waypoint, WaypointCategory category) {
        if (waypoint == null || category == null) return;
        waypoint.setCategory(category);
        WaypointData.save();
    }

    public static void setWaypointVisibility(Waypoint waypoint, VisibilityTarget target, Boolean value) {
        if (waypoint == null) return;
        switch (target) {
            case NAME -> waypoint.setShowNameOverride(value);
            case BLOCK -> waypoint.setShowOverride(value);
            case DISTANCE -> waypoint.setShowDistanceOverride(value);
            case SEE_THROUGH -> waypoint.setSeeThroughOverride(value);
        }
        WaypointData.save();
    }

    public static WaypointCategory createCategory(WaypointPackage pkg, String baseName) {
        if (pkg == null) return null;
        WaypointCategory category = new WaypointCategory(uniqueCategoryName(pkg, cleanName(baseName, "New Category")));
        pkg.categories.add(category);
        WaypointData.save();
        return category;
    }

    public static void renameCategory(WaypointPackage pkg, WaypointCategory category, String name) {
        if (pkg == null || category == null || WaypointData.isUncategorizedCategory(category)) return;
        category.name = uniqueCategoryName(pkg, category, cleanName(name, "New Category"));
        WaypointData.save();
    }

    public static void deleteCategory(WaypointPackage pkg, WaypointCategory category) {
        if (pkg == null || category == null || WaypointData.isUncategorizedCategory(category)) return;
        WaypointCategory fallback = WaypointData.ensureUncategorizedCategory(pkg);
        for (Waypoint waypoint : pkg.waypoints) {
            if (waypoint.getCategory() == category || category.id.equals(waypoint.categoryId)) {
                waypoint.setCategory(fallback);
            }
        }
        pkg.categories.remove(category);
        if (WaypointEditMode.activeCategory == category) WaypointEditMode.activeCategory = fallback;
        WaypointData.save();
    }

    public static void setCategoryColor(WaypointCategory category, int rgb) {
        if (category == null) return;
        category.color = CustomColor.fromInt(rgb & 0xFFFFFF);
        WaypointData.save();
    }

    public static void setCategoryAlpha(WaypointCategory category, float alpha) {
        if (category == null) return;
        category.alpha = Math.clamp(alpha, 0f, 1f);
        WaypointData.save();
    }

    public static void setCategoryDefault(WaypointCategory category, VisibilityTarget target, boolean value) {
        if (category == null) return;
        switch (target) {
            case NAME -> category.showNameByDefault = value;
            case BLOCK -> category.showBlockByDefault = value;
            case DISTANCE -> category.showDistanceByDefault = value;
            case SEE_THROUGH -> category.showSeeThroughByDefault = value;
        }
        WaypointData.save();
    }

    public static void savePackagesAndOrder() {
        OrderManager.saveOrder(WaypointData.INSTANCE.packages);
        WaypointData.save();
    }

    public static String cleanName(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isBlank() ? fallback : clean;
    }

    private static String cleanPackageName(String value, String fallback) {
        String clean = cleanName(value, fallback);
        return clean.length() > MAX_PACKAGE_NAME_LENGTH ? clean.substring(0, MAX_PACKAGE_NAME_LENGTH) : clean;
    }

    public static String uniqueCategoryName(WaypointPackage pkg, String base) {
        return uniqueCategoryName(pkg, null, base);
    }

    private static String uniqueCategoryName(WaypointPackage pkg, WaypointCategory self, String base) {
        String candidate = base;
        int i = 1;
        while (true) {
            String check = candidate;
            boolean exists = pkg.categories.stream()
                    .anyMatch(category -> category != self && category.name != null && category.name.equalsIgnoreCase(check));
            if (!exists) return candidate;
            candidate = base + " " + i;
            i++;
        }
    }

    private static String uniquePackageName(WaypointPackage self, String base) {
        base = cleanPackageName(base, "New Package");
        String candidate = base;
        int i = 1;
        while (true) {
            String check = candidate;
            boolean exists = WaypointData.INSTANCE.packages.stream()
                    .anyMatch(pkg -> pkg != self && pkg.name != null && pkg.name.equals(check));
            if (!exists) return candidate;
            String suffix = " (" + i + ")";
            String prefix = base.substring(0, Math.max(0, Math.min(base.length(), MAX_PACKAGE_NAME_LENGTH - suffix.length())));
            candidate = prefix + suffix;
            i++;
        }
    }

    private static String uniqueWaypointName(WaypointPackage pkg, String base) {
        String candidate = base + " Copy";
        int i = 1;
        while (true) {
            String check = candidate;
            boolean exists = pkg.waypoints.stream().anyMatch(waypoint -> waypoint.name != null && waypoint.name.equalsIgnoreCase(check));
            if (!exists) return candidate;
            candidate = base + " Copy " + i;
            i++;
        }
    }

    private static void normalizeImportedPackage(WaypointPackage pkg) {
        pkg.id = UUID.randomUUID().toString();
        pkg.name = uniquePackageName(null, cleanPackageName(pkg.name, "Imported Package"));
        pkg.description = pkg.description == null ? "" : pkg.description;
        pkg.packageVersion = WaypointData.CURRENT_PACKAGE_VERSION;
        if (pkg.categories == null) pkg.categories = new ArrayList<>();
        if (pkg.waypoints == null) pkg.waypoints = new ArrayList<>();

        Set<String> seenCategoryIds = new HashSet<>();
        for (WaypointCategory category : pkg.categories) {
            if (WaypointData.isUncategorizedCategory(category)) {
                category.id = WaypointData.UNCATEGORIZED_CATEGORY_ID;
            } else if (category.id == null || category.id.isBlank() || seenCategoryIds.contains(category.id)) {
                category.id = UUID.randomUUID().toString();
            }
            seenCategoryIds.add(category.id);
            if (category.name == null || category.name.isBlank()) category.name = "New Category";
            if (category.color == null) category.color = CustomColor.fromHexString("FFFFFF");
        }

        Set<String> seenWaypointIds = new HashSet<>();
        for (Waypoint waypoint : pkg.waypoints) {
            if (waypoint.id == null || waypoint.id.isBlank() || seenWaypointIds.contains(waypoint.id)) {
                waypoint.id = UUID.randomUUID().toString();
            }
            seenWaypointIds.add(waypoint.id);
            if (waypoint.name == null || waypoint.name.isBlank()) waypoint.name = "Waypoint";
        }

        WaypointData.ensureUncategorizedCategory(pkg);
        WaypointData.resolveWaypointCategories(pkg);
    }

    private static WaypointCategory copyCategory(WaypointCategory category) {
        WaypointCategory copy = new WaypointCategory(category.name, category.color);
        copy.id = WaypointData.isUncategorizedCategory(category) ? WaypointData.UNCATEGORIZED_CATEGORY_ID : UUID.randomUUID().toString();
        copy.alpha = category.alpha;
        copy.showBlockByDefault = category.showBlockByDefault;
        copy.showNameByDefault = category.showNameByDefault;
        copy.showDistanceByDefault = category.showDistanceByDefault;
        copy.showSeeThroughByDefault = category.showSeeThroughByDefault;
        return copy;
    }

    private static Waypoint copyWaypoint(Waypoint waypoint) {
        Waypoint copy = new Waypoint(waypoint.x, waypoint.y, waypoint.z);
        copy.id = UUID.randomUUID().toString();
        copy.name = waypoint.name;
        copy.show = waypoint.show;
        copy.showName = waypoint.showName;
        copy.showDistance = waypoint.showDistance;
        copy.seeThrough = waypoint.seeThrough;
        copy.showOverride = waypoint.showOverride;
        copy.showNameOverride = waypoint.showNameOverride;
        copy.showDistanceOverride = waypoint.showDistanceOverride;
        copy.seeThroughOverride = waypoint.seeThroughOverride;
        return copy;
    }

    private static void deletePackageFile(String name) {
        if (name == null || name.isBlank()) return;
        Path path = WaypointData.PACKAGE_FOLDER.resolve(name + ".json");
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't delete package file: " + name);
            e.printStackTrace();
        }
    }

    public enum VisibilityTarget {
        NAME,
        BLOCK,
        DISTANCE,
        SEE_THROUGH
    }
}