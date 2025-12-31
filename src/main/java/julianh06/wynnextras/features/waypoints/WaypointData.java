package julianh06.wynnextras.features.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.raid.raids.RaidKind;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.misc.StyledTextAdapter;
import julianh06.wynnextras.features.raid.RaidData;
import julianh06.wynnextras.features.raid.RaidKindAdapter;
import julianh06.wynnextras.features.raid.RaidListData;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WaypointData {
    public static WaypointData INSTANCE = new WaypointData();

    public List<WaypointPackage> packages = new ArrayList<>();

    public WaypointPackage activePackage = null;

//    public List<Waypoint> waypoints = new ArrayList<>();
//
//    public List<WaypointCategory> categories = new ArrayList<>();

    static GsonBuilder builder = new GsonBuilder()
            .registerTypeAdapter(StyledText.class, new StyledTextAdapter());

    static Gson gson = builder
            .setPrettyPrinting()
            .create();


    private static final Path PACKAGE_FOLDER = FabricLoader.getInstance()
            .getConfigDir().resolve("wynnextras/packages");

    public static void load() {
        INSTANCE = new WaypointData();

        try {
            if (!Files.exists(PACKAGE_FOLDER)) {
                Files.createDirectories(PACKAGE_FOLDER);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(PACKAGE_FOLDER, "*.json")) {
                for (Path file : stream) {
                    WaypointPackage pkg = WaypointPackage.loadFromFile(file);
                    if (pkg != null) {
                        INSTANCE.packages.add(pkg);
                    }
                }
            }
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
            newWaypoint.categoryName = waypoint.categoryName;

            for (WaypointCategory cat : copy.categories) {
                if (cat.name.equals(newWaypoint.categoryName)) {
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
