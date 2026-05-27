package julianh06.wynnextras.features.raid.tna;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.features.raid.tna.graphs.GrottoGraph;
import julianh06.wynnextras.utils.path.Path;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public class TnaApi {

    private static boolean collectedHeart = false;
    private static String playerInTree = "";
    private static Grotto playerGrotto = Grotto.None;
    private static Grotto heartGrotto = Grotto.None;

    public static boolean inTree() {
        if (MinecraftClient.getInstance().player == null) {
            reset();
            return false;
        }
        return playerInTree.equals(MinecraftClient.getInstance().player.getName().getString());
    }

    public static boolean hasHeart() {
        return collectedHeart;
    }

    public static Grotto getHeartGrotto() {
        return heartGrotto;
    }

    public static Grotto getTargetGrotto() {
        return hasHeart() ? Grotto.Entrance : getHeartGrotto();
    }

    public static Grotto getPlayerGrotto() {
        return playerGrotto;
    }

    public static String getPlayerInTree() {
        return playerInTree;
    }

    private GrottoGraph getGraph() {
        return switch (playerGrotto) {
            case Entrance -> GrottoGraph.EntranceGraph;
            case Gray -> GrottoGraph.GrayGraph;
            case Blue -> GrottoGraph.BlueGraph;
            case White -> GrottoGraph.WhiteGraph;
            case Orange -> GrottoGraph.OrangeGraph;
            case Black -> GrottoGraph.BlackGraph;
            case None, Outside -> null;
        };
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        String raw = event.message.getString().replaceAll("\u00a7[0-9a-fk-orx]", "");
        handleMessage(raw);
    }

    @SubscribeEvent
    public void onWorldRedner(RenderWorldEvent event) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;;
        if (player == null || heartGrotto == Grotto.None) return;
        else if (player.getEntityPos().getX() < 24100 || player.getEntityPos().getX() > 24300 || player.getEntityPos().getZ() > -22100 || player.getEntityPos().getZ() < -22400) return;

        if (!inTree()) return;
        GrottoGraph graph = getGraph();
        if (graph == null) return;
        Path path = graph.pathTo(getTargetGrotto());
        if (path != null && WynnExtrasConfig.INSTANCE.drawPathInTree) path.draw(event, Color.green);
        /*
        GrottoGraph.EntranceGraph.drawFullGraph(event, Color.GREEN);
        GrottoGraph.BlackGraph.drawFullGraph(event, Color.BLACK);
        GrottoGraph.GrayGraph.drawFullGraph(event, Color.GRAY);
        GrottoGraph.OrangeGraph.drawFullGraph(event, Color.ORANGE);
        GrottoGraph.BlueGraph.drawFullGraph(event, Color.BLUE);
        GrottoGraph.WhiteGraph.drawFullGraph(event, Color.WHITE);
         */
    }

    public static void reset() {
        collectedHeart = false;
        playerInTree = "";
        playerGrotto = Grotto.None;
        heartGrotto = Grotto.None;
    }

    private static final Pattern ENTER_TREE_PATTERN =
            Pattern.compile(".*?\\b([A-Za-z0-9_]{3,16}) has entered the tree!$");

    private static final Pattern ENTER_GROTTO_PATTERN =
            Pattern.compile(".*?\\b([A-Za-z0-9_]{3,16}) has entered the (Gray|Black|White|Orange|Blue) Grotto$");

    private static final Pattern HEART_PATTERN =
            Pattern.compile(".*?\\[\\+1 Isoptera Heart]$");

    private static final Pattern DEPOSITED_HEART_PATTERN =
            Pattern.compile(".*?\\[-1 Isoptera Heart]$");

    private static final Pattern ISOPTERA_PATTERN =
            Pattern.compile(".*?The Interdimensional Isoptera is in the (Gray|Black|White|Orange|Blue) Grotto$");

    public static void handleMessage(String message) {
        message = message.replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("\uDAFF\uDFFC\uE001\uDB00\uDC06 ", "")
                .trim();

        Matcher treeMatcher = ENTER_TREE_PATTERN.matcher(message);
        if (treeMatcher.matches()) {
            playerInTree = treeMatcher.group(1);
            collectedHeart = false;
            playerGrotto = Grotto.Entrance;
            return;
        }

        Matcher grottoMatcher = ENTER_GROTTO_PATTERN.matcher(message);
        if (grottoMatcher.matches()) {
            playerInTree = grottoMatcher.group(1);
            playerGrotto = Grotto.from(grottoMatcher.group(2));
            return;
        }

        Matcher heartMatcher = HEART_PATTERN.matcher(message);
        if (heartMatcher.matches()) {
            collectedHeart = true;
            heartGrotto = Grotto.None;
            return;
        }

        Matcher depositedHeartMatcher = DEPOSITED_HEART_PATTERN.matcher(message);
        if (depositedHeartMatcher.matches()) {
            reset();
            return;
        }

        Matcher isoMatcher = ISOPTERA_PATTERN.matcher(message);
        if (isoMatcher.matches()) {
            heartGrotto = Grotto.from(isoMatcher.group(1));
            return;
        }
    }
}
