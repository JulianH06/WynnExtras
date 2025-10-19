package julianh06.wynnextras.features.abilitytree;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class ImportTreeData {
    public String name;
    public List<String> input;
    public static Map<String, ImportTreeData> trees = new HashMap<>();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static ImportTreeData safeLoadTree(Path pathToFile) {
        try (BufferedReader reader = Files.newBufferedReader(pathToFile)) {
            reader.mark(2);
            int firstChar = reader.read();
            if (firstChar == '[') {
                throw new IOException("Tree file " + pathToFile.getFileName() +
                        " is in API array form! Use /savePlayerAbilityTree to convert it to object form.");
            }
            reader.reset();
            return gson.fromJson(reader, ImportTreeData.class);
        } catch (IOException | JsonParseException e) {
            System.err.println("[ImportTreeData] Failed to load " + pathToFile + ":");
            e.printStackTrace();
            return null;
        }
    }

    public static void loadAll() {
        trees.clear();
        // Use relative path just like your original TreeData logic!
        Path treesDir = Paths.get("wynncraftimportdata");
        if (!Files.isDirectory(treesDir)) {
            try { Files.createDirectories(treesDir); } catch (IOException ignore) {}
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(treesDir, "*.json")) {
            for (Path file : stream) {
                ImportTreeData t = safeLoadTree(file);
                if (t != null && t.name != null) {
                    trees.put(t.name, t);
                    System.out.println("[ImportTreeData] Loaded ability tree: " + t.name);
                }
            }
        } catch (IOException e) {
            System.err.println("[ImportTreeData] Failed to scan directory: " + treesDir);
        }
    }

    public static ImportTreeData getTree(String name) {
        return trees.get(name);
    }
}
