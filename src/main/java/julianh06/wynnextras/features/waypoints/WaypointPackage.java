package julianh06.wynnextras.features.waypoints;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WaypointPackage {
    public String name;
    public boolean enabled;
    public List<WaypointCategory> categories = new ArrayList<>();
    public List<Waypoint> waypoints = new ArrayList<>();

    public WaypointPackage(String name) {
        this.name = name;
        enabled = true;
    }

    public void saveToFile(Path folder) {
        Path path = folder.resolve(name + ".json");
        try (Writer writer = Files.newBufferedWriter(path)) {
            WaypointData.gson.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't save package: " + name);
            e.printStackTrace();
        }
    }

    public static WaypointPackage loadFromFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            WaypointPackage pkg = WaypointData.gson.fromJson(reader, WaypointPackage.class);

            for (Waypoint waypoint : pkg.waypoints) {
                if (waypoint.categoryName != null) {
                    for (WaypointCategory cat : pkg.categories) {
                        if (cat.name.equals(waypoint.categoryName)) {
                            waypoint.setCategory(cat);
                            break;
                        }
                    }
                }
            }

            return pkg;
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't load package: " + file.getFileName());
            e.printStackTrace();
            return null;
        }
    }

}
