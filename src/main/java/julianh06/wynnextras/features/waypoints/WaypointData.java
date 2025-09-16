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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WaypointData {
    public static WaypointData INSTANCE = new WaypointData();

    public List<Waypoint> waypoints = new ArrayList<>();

    public List<WaypointCategory> categories = new ArrayList<>();

    static GsonBuilder builder = new GsonBuilder()
            .registerTypeAdapter(StyledText.class, new StyledTextAdapter());

    static Gson gson = builder
            .registerTypeAdapter(RaidKind.class, new RaidKindAdapter(builder.create()))
            .setPrettyPrinting()
            .create();


    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras/waypoints.json");

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                WaypointData loaded = gson.fromJson(reader, WaypointData.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    for (Waypoint waypoint : INSTANCE.waypoints) {
                        if (waypoint.categoryName != null) {
                            for (WaypointCategory cat : INSTANCE.categories) {
                                if (cat.name.equals(waypoint.categoryName)) {
                                    waypoint.setCategory(cat);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    System.err.println("[WynnExtras] Deserialized data was null, keeping default INSTANCE.");
                }
            } catch (IOException e) {
                System.err.println("[WynnExtras] Couldn't read the waypoints file:");
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            for (Waypoint waypoint : INSTANCE.waypoints) {
                if (waypoint.getCategory() != null && (waypoint.categoryName == null || waypoint.categoryName.isEmpty())) {
                    waypoint.categoryName = waypoint.getCategory().name; //just to make sure the category is saved correctly because i had some problems with it
                }
            }
            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't write the waypoints file:");
            e.printStackTrace();
        }
    }
}
