package julianh06.wynnextras.features.waypoints;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;
import julianh06.wynnextras.features.waypoints.old.WaypointData;
import julianh06.wynnextras.features.waypoints.old.WaypointPackage;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderManager {
    private static final Path ORDER_FILE = WaypointData.PACKAGE_FOLDER.resolve("order.json");

    public static void saveOrder(List<WaypointPackage> packages) {
        try {
            if (!Files.exists(WaypointData.PACKAGE_FOLDER)) Files.createDirectories(WaypointData.PACKAGE_FOLDER);
            List<String> ids = packages.stream().map(p -> p.id).collect(Collectors.toList());
            try (Writer w = Files.newBufferedWriter(ORDER_FILE, StandardCharsets.UTF_8)) {
                WaypointData.gson.toJson(ids, w);
            }
        } catch (Exception e) {
            System.err.println("[WynnExtras] Couldn't save package order");
            e.printStackTrace();
        }
    }

    public static List<String> loadOrder() {
        if (!Files.exists(ORDER_FILE)) return Collections.emptyList();
        try (Reader r = Files.newBufferedReader(ORDER_FILE, StandardCharsets.UTF_8)) {
            return WaypointData.gson.fromJson(r, new TypeToken<List<String>>(){}.getType());
        } catch (Exception e) {
            System.err.println("[WynnExtras] Couldn't load package order");
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static void applyOrder(List<WaypointPackage> packages) {
        List<String> order = loadOrder();
        if (order.isEmpty()) return;
        Map<String, WaypointPackage> byId = packages.stream().collect(Collectors.toMap(p -> p.id, p -> p));
        List<WaypointPackage> reordered = new ArrayList<>();
        // add known in order
        for (String id : order) {
            WaypointPackage p = byId.remove(id);
            if (p != null) reordered.add(p);
        }
        // append remaining (new packages)
        reordered.addAll(byId.values());
        packages.clear();
        packages.addAll(reordered);
    }
}
