package julianh06.wynnextras.features.waypoints.old;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WaypointPackage {
    public String id;
    public String name;
    public String description;
    public boolean enabled;
    public int packageVersion = 1;
    public List<WaypointCategory> categories = new ArrayList<>();
    public List<Waypoint> waypoints = new ArrayList<>();

    public WaypointPackage() {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = "New Package";
        enabled = true;
    }

    public WaypointPackage(String name) {
        this.id = java.util.UUID.randomUUID().toString();
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
}