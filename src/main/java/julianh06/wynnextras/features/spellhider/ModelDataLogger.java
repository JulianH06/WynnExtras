package julianh06.wynnextras.features.spellhider;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.SpellHiderConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.ChatUtils;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@WEModule
public class ModelDataLogger {

    private static final Set<Pair<Text, WEVec>> toRender = new HashSet<>();
    private static final Set<Integer> recentHashes = new HashSet<>();

    @SubscribeEvent
    public void onRender(RenderWorldEvent event) {
        for (Pair<Text, WEVec> pair : toRender) {
            WorldRenderUtils.drawText(event, pair.getRight(), pair.getLeft(), 1, false);
        }
        toRender.clear();
    }

    public static void addTextToRender(String nameSpace, Vec3d loc) {
        if (currentState == State.MASS_ADDING) {
            toRender.add(new Pair<>(Text.literal(nameSpace), new WEVec(loc)));
        }
    }

    public static void addTextToRender(Integer hash, Vec3d loc) {
        if (currentState == State.MASS_ADDING) {
            recentHashes.add(hash);
            toRender.add(new Pair<>(Text.literal(String.valueOf(hash)), new WEVec(loc)));
        }
    }

    public static void addAll(String FQName) {
        if (FQName.isEmpty()) {
            recentHashes.clear();
            return;
        }
        SpellNamespace nameSpace = SpellNamespace.from(FQName);
        for (Integer hash : recentHashes) {
            SpellHiderConfig.INSTANCE.addSpellIdentifier(hash, nameSpace);
        }
        recentHashes.clear();
    }

    public enum State {
        OFF,
        CONSOLE,
        CHAT,
        MASS_ADDING,
        GET_CURRENT,
        ON;

        public static State from(String state) {
            try {
                return valueOf(state.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static State currentState = State.OFF;
    private static boolean hasOpenedFiles = false;

    public static void setState(State state) {
        currentState = state;
    }

    public static State getCurrentState() {
        return currentState;
    }
    private static final Map<String, List<Float>> pathToModels = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> unknownQueue = new ConcurrentLinkedQueue<>();

    public static String peekQueue() {
        if (unknownQueue.isEmpty()) return null;
        return unknownQueue.peek();
    }

    public static void handleUnknownModel(Float customModel, Set<Identifier> names) {
        if (currentState == State.OFF || currentState == State.MASS_ADDING) return;

        for (Identifier id : names) {
            pathToModels.computeIfAbsent(id.getPath(), k -> new ArrayList<>()).add(customModel);
            if (unknownQueue.contains(id.getPath())) continue;
            unknownQueue.add(id.getPath());

            switch (currentState) {
                case CONSOLE: {
                    WynnExtras.LOGGER.info("unknown model: {} names: {}", customModel, names.stream().map(Identifier::getPath).collect(Collectors.toSet()));
                    break;
                }
                case CHAT: {
                    ChatUtils.sendMessage("unknown model: " + customModel + " names: " + names.stream().map(Identifier::getPath).collect(Collectors.toSet()));
                    break;
                }
            }
        }
    }

    public static boolean progressQueue(String FQName) {
        if (!hasOpenedFiles) {
            if (!unknownQueue.isEmpty()) {
                openFile(unknownQueue.peek());
            }
            return false;
        }
        SpellNamespace nameSpace = SpellNamespace.from(FQName);
        String itemPath = unknownQueue.poll();
        if (itemPath == null) {
            WynnExtras.LOGGER.warn("queue is empty");
            return false;
        }
        nameSpace.addId(SpellHider.hashMap.get(itemPath)); // permanently store the mapping for hash -> namespace
        List<Float> models = pathToModels.get(itemPath);
        for (Float model : models) {
            SpellHider.addModel(model, nameSpace); // add the model mapping to memory
        }
        if (unknownQueue.isEmpty()) {
            WynnExtras.LOGGER.info("reached the end of the queue");
            return true;
        }
        openFile(unknownQueue.peek());
        return true;
    }

    public static void openFile(String itemName) {
        hasOpenedFiles = true;
        Desktop desktop = Desktop.getDesktop();
        File betaFile = new File("extracted-packs/beta.wynncraft.com/assets/minecraft/textures/" + itemName + ".png");
        File mainFile = new File("extracted-packs/play.wynncraft.com/assets/minecraft/textures/" + itemName + ".png");
        if (desktop.isSupported(Desktop.Action.OPEN)) {
            if (betaFile.exists()) {
                try {
                    desktop.open(betaFile);
                } catch (IOException e) {
                    WynnExtras.LOGGER.error("Error opening beta file: {}", itemName, e);
                }
            } else if (mainFile.exists()) {
                try {
                    desktop.open(mainFile);
                } catch (IOException e) {
                    WynnExtras.LOGGER.error("Error opening main file: {}", itemName, e);
                }
            }
        } else  {
            WynnExtras.LOGGER.error("Desktop OPEN is not supported: {}", itemName);
        }
    }
}
