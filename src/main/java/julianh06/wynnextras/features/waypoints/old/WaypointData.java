package julianh06.wynnextras.features.waypoints.old;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.text.StyledText;
import julianh06.wynnextras.features.misc.StyledTextAdapter;
import julianh06.wynnextras.features.waypoints.OrderManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaypointData {
    public static WaypointData INSTANCE = new WaypointData();

    public List<WaypointPackage> packages = new ArrayList<>();

    public WaypointPackage activePackage = null;

    static GsonBuilder builder = new GsonBuilder()
            .registerTypeAdapter(StyledText.class, new StyledTextAdapter());

    public static Gson gson = builder
            .setPrettyPrinting()
            .create();


    public static final Path PACKAGE_FOLDER = FabricLoader.getInstance()
            .getConfigDir().resolve("wynnextras/packages");

    public static void load() {
        INSTANCE = new WaypointData();
        try {
            if (!Files.exists(PACKAGE_FOLDER)) {
                Files.createDirectories(PACKAGE_FOLDER);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(PACKAGE_FOLDER, "*.json")) {
                for (Path file : stream) {
                    WaypointPackage pkg = loadFromFile(file);
                    if (pkg != null) {
                        INSTANCE.packages.add(pkg);
                    }
                }
            }

            OrderManager.applyOrder(INSTANCE.packages);
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't load packages:");
            e.printStackTrace();
        }

        // Optional: Default-Package setzen
        if (INSTANCE.packages.isEmpty()) {
            WaypointPackage defaultPkg = new WaypointPackage("Default");
            INSTANCE.packages.add(defaultPkg);
            INSTANCE.activePackage = defaultPkg;
        }
    }

    public static WaypointPackage loadFromFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement root = WaypointData.gson.fromJson(reader, JsonElement.class);
            if (root == null || !root.isJsonObject()) return null;
            JsonObject obj = root.getAsJsonObject();

            // Detect legacy by missing packageVersion
            if (!obj.has("packageVersion")) {
                try {
                    Path parent = file.getParent();
                    Path legacyDir = parent.resolve("legacy");
                    Files.createDirectories(legacyDir);

                    Path readme = legacyDir.resolve("_README.txt");
                    if (!Files.exists(readme)) {
                        String text =
                            """
                            This folder contains BACKUPS of legacy waypoint files.
                            These files are NOT active and are NOT loaded.
                            Legacy waypoint jsons are copied in here before they are automatically converted to the new format.
                            Active waypoint files are located in the parent directory.""";

                        Files.write(
                                readme,
                                text.getBytes(StandardCharsets.UTF_8),
                                StandardOpenOption.CREATE
                        );
                    }

                    Path legacyCopy = legacyDir.resolve(file.getFileName());
                    Files.copy(
                            file,
                            legacyCopy,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES
                    );
                } catch (IOException e) {
                    System.err.println("[WynnExtras] Couldn't backup legacy waypoint file: " + file.getFileName());
                    e.printStackTrace();
                }

                // Legacy -> parse into old class and migrate
                WaypointPackage legacy = WaypointData.gson.fromJson(obj, WaypointPackage.class);
                if (legacy == null) return null;

                // Migrate: create new package
                WaypointPackage migrated = new WaypointPackage();
                migrated.id = java.util.UUID.randomUUID().toString();
                migrated.name = legacy.name != null ? legacy.name : migrated.name;
                migrated.enabled = legacy.enabled;

                // Migrate categories: give each a uuid
                for (WaypointCategory oldCat : legacy.categories) {
                    WaypointCategory newCat = new WaypointCategory();
                    newCat.id = java.util.UUID.randomUUID().toString();
                    newCat.name = oldCat.name;
                    newCat.color = oldCat.color;
                    newCat.alpha = oldCat.alpha;
                    migrated.categories.add(newCat);
                }

                // Helper map name -> categoryId (first occurrence)
                Map<String,String> nameToId = new HashMap<>();
                for (WaypointCategory cat : migrated.categories) {
                    nameToId.put(cat.name, cat.id);
                }

                // Migrate waypoints
                for (Waypoint wp : legacy.waypoints) {
                    wp.id = java.util.UUID.randomUUID().toString();
                    if (wp.getLegacyCategoryName() != null && !wp.getLegacyCategoryName().isEmpty() && nameToId.containsKey(wp.getLegacyCategoryName())) {
                        wp.categoryId = nameToId.get(wp.getLegacyCategoryName());
                        wp.categoryName = null;
                    } else {
                        wp.categoryId = null;
                    }

                    migrated.waypoints.add(wp);
                }

                migrated.packageVersion = 1;
                migrated.description = "";

                migrated.saveToFile(file.getParent());

                // Remove old legacy file if filename changed
                Path newPath = file.getParent().resolve(migrated.name + ".json");
                if (!newPath.equals(file)) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        System.err.println("[WynnExtras] Couldn't delete old legacy file: " + file.getFileName());
                        e.printStackTrace();
                    }
                }

                return migrated;
            } else {
                // New format -> parse normally
                WaypointPackage pkg = WaypointData.gson.fromJson(obj, WaypointPackage.class);
                if (pkg == null) return null;
                // Ensure transient runtime pointers: match categoryId -> category object
                Map<String, WaypointCategory> idToCat = new HashMap<>();
                for (WaypointCategory c : pkg.categories) idToCat.put(c.id, c);
                for (Waypoint w : pkg.waypoints) {
                    if (w.categoryId != null && idToCat.containsKey(w.categoryId)) {
                        w.setCategory(idToCat.get(w.categoryId));
                    }
                }
                return pkg;
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't load package: " + file.getFileName());
            e.printStackTrace();
            return null;
        }
    }


    public static void save() {
        try {
            if (!Files.exists(PACKAGE_FOLDER)) {
                Files.createDirectories(PACKAGE_FOLDER);
            }

            for (WaypointPackage pkg : INSTANCE.packages) {
                pkg.saveToFile(PACKAGE_FOLDER);
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't save packages:");
            e.printStackTrace();
        }
    }

    public void deletePackage(String name) {
        packages.removeIf(pkg -> pkg.name.equals(name));
        try {
            Files.deleteIfExists(PACKAGE_FOLDER.resolve(name + ".json"));
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't delete package file: " + name);
            e.printStackTrace();
        }
    }

    public WaypointPackage duplicatePackage(String originalName) {
        WaypointPackage original = packages.stream()
                .filter(pkg -> pkg.name.equals(originalName))
                .findFirst()
                .orElse(null);

        if (original == null) return null;

        WaypointPackage copy = new WaypointPackage(generateUniqueName(original.name));
        for (WaypointCategory cat : original.categories) {
            copy.categories.add(new WaypointCategory(cat.name, cat.color));
        }

        for (Waypoint waypoint : original.waypoints) {
            Waypoint newWaypoint = new Waypoint(waypoint.x, waypoint.y, waypoint.z);
            newWaypoint.name = waypoint.name;
            newWaypoint.show = waypoint.show;
            newWaypoint.showName = waypoint.showName;
            newWaypoint.showDistance = waypoint.showDistance;
            newWaypoint.seeThrough = waypoint.seeThrough;

            for (WaypointCategory cat : copy.categories) {
                if (cat.id.equals(waypoint.categoryId)) {
                    newWaypoint.setCategory(cat);
                    break;
                }
            }

            copy.waypoints.add(newWaypoint);
        }

        packages.add(copy);
        save();
        return copy;
    }

    public String generateUniqueName(String baseName) {
        int counter = 0;
        while (true) {
            String candidate = (counter == 0)
                    ? baseName
                    : baseName + " (" + counter + ")";
            final String checkName = candidate;

            boolean exists = packages.stream().anyMatch(pkg -> pkg.name != null && pkg.name.equals(checkName));

            if (!exists) {
                return candidate;
            }
            counter++;
        }
    }
}