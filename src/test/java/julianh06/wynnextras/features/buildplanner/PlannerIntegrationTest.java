package julianh06.wynnextras.features.buildplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import org.junit.jupiter.api.Test;

class PlannerIntegrationTest {
    private static final String ASSETS = "/assets/wynnextras/";

    @Test
    void moduleRegistersExactlyOneNativeCommandWithoutLoadingItemData() {
        assertTrue(BuildPlannerFeature.class.isAnnotationPresent(WEModule.class));
        new BuildPlannerFeature();
        new BuildPlannerFeature();
        assertEquals(1, Command.COMMAND_LIST.stream()
                .filter(command -> command.getName().equals("wynnbuilder")).count());
        assertTrue(Thread.getAllStackTraces().keySet().stream()
                .noneMatch(thread -> thread.getName().equals("WynnExtras-PlannerItems")));
    }

    @Test
    void everyRequiredTextureIsPackaged() {
        for (String name : List.of("equipment", "tomes", "aspects")) {
            resource("textures/gui/buildplanner/" + name + ".png");
        }
        String tree = "textures/gui/buildplanner/abilitytree/";
        for (String name : List.of("treetabbackground", "pageline", "node/node", "node/selected")) {
            resource(tree + name + ".png");
        }
        for (String node : List.of("warrior", "shaman", "archer", "mage", "assassin",
                "white", "yellow", "blue", "purple", "red")) {
            resource(tree + "node/" + node + ".png");
            resource(tree + "node/" + node + "_active.png");
        }
        for (String connector : List.of("vertical", "horizontal", "down_left", "right_down",
                "right_down_left", "up_right_down", "up_down_left", "up_right_left", "up_right_down_left")) {
            resource(tree + "connector/" + connector + ".png");
            resource(tree + "connector/" + connector + "_active.png");
        }
        for (String archetype : List.of(
                "archer/boltslinger", "archer/sharpshooter", "archer/trapper",
                "assassin/acrobat", "assassin/shadestepper", "assassin/trickster",
                "mage/arcanist", "mage/light_bender", "mage/riftwalker",
                "shaman/acolyte", "shaman/ritualist", "shaman/summoner",
                "warrior/battle_monk", "warrior/fallen", "warrior/paladin")) {
            resource(tree + "node/" + archetype + ".png");
            resource(tree + "node/" + archetype + "_selected.png");
        }
    }

    @Test
    void sourceHasNoStandalonePackagesResourcesOrWritablePaths() throws IOException {
        Path root = Path.of("src", "main", "java", "julianh06", "wynnextras", "features", "buildplanner");
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("dev.flizy.wynnqol"), file.toString());
                assertFalse(source.contains("/assets/wynnqol/"), file.toString());
                assertFalse(source.contains("\"wynnqol\""), file.toString());
                if (source.contains("getConfigDir()")) {
                    assertTrue(source.contains(".resolve(\"wynnextras\").resolve(\"buildplanner\")"),
                            file.toString());
                }
            }
        }
        resource("buildplanner/NOTICE");
        resource("buildplanner/LICENSE");
    }

    private static void resource(String path) {
        assertNotNull(PlannerIntegrationTest.class.getResource(ASSETS + path), path);
    }
}
