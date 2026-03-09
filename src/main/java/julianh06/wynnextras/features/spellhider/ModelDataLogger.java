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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@WEModule
public class ModelDataLogger {
    public enum State {
        OFF,
        CONSOLE,
        CHAT,
        MASS_ADDING,
        GET_CURRENT;

        public static State from(String state) {
            try {
                return valueOf(state.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum DisplayState {
        OFF,
        ONLY_UNKNOWN,
        ONLY_KNOWN,
        ALL,
        GET_CURRENT;

        public static DisplayState from(String state) {
            try {
                return valueOf(state.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static final ConcurrentLinkedQueue<String> unknownQueue = new ConcurrentLinkedQueue<>(); // file paths
    private static final Set<Pair<Text, WEVec>> toRender = new HashSet<>();
    private static final Set<Integer> recentHashes = new HashSet<>();
    private static State currentState = State.OFF;
    private static DisplayState displayState = DisplayState.OFF;

    @SubscribeEvent
    public void onRender(RenderWorldEvent event) {
        for (Pair<Text, WEVec> pair : toRender) {
            WorldRenderUtils.drawText(event, pair.getRight(), pair.getLeft(), 1, false);
        }
        toRender.clear();
    }

    public static Set<Integer> getRecentHashes() {
        return recentHashes;
    }

    public static void addTextToRender(String nameSpace, Vec3d loc) {
        if (displayState == DisplayState.ALL || displayState == DisplayState.ONLY_KNOWN) {
            toRender.add(new Pair<>(Text.literal(nameSpace), new WEVec(loc)));
        }
    }

    public static void addTextToRender(Integer hash, Vec3d loc) {
        if (displayState == DisplayState.ALL || displayState == DisplayState.ONLY_UNKNOWN) {
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

    public static void setState(State state) {
        currentState = state;
    }

    public static State getCurrentState() {
        return currentState;
    }

    public static DisplayState getDisplayState() {
        return displayState;
    }

    public static void setDisplayState(DisplayState displayState) {
        ModelDataLogger.displayState = displayState;
    }

    public static String peekQueue() {
        if (unknownQueue.isEmpty()) return null;
        return unknownQueue.peek();
    }

    public static void handleUnknownModel(Float customModel, Set<Identifier> names) {
        if (currentState == State.OFF || currentState == State.MASS_ADDING) return;

        for (Identifier id : names) {
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

    public static void addForFineTuning(Set<SpellData> data) {
        if (data == null) {
            ChatUtils.sendMessage("please cast the relevant spell and rerun /fineTune");
        } else if (data.isEmpty()) {
            ChatUtils.sendMessage("nothing found to add");
        } else {
            unknownQueue.clear();
            unknownQueue.addAll(data.stream().map(SpellData::getFilePath).collect(Collectors.toSet()));
        }
    }

    public static boolean progressQueue(String FQName) {
        if (FQName.equals("skip") || FQName.equals("next")) {
            ChatUtils.sendMessage("not changing");
            unknownQueue.poll();
            openFile(unknownQueue.peek());
            return false;
        }

        SpellNamespace newNamespace = SpellNamespace.from(FQName);
        if (newNamespace == null || newNamespace.isEmpty()) {
            openFile(unknownQueue.peek());
            ChatUtils.sendMessage("no provided namespace found reopening");
            return false;
        }

        String itemPath = unknownQueue.poll();
        if (itemPath == null) {
            ChatUtils.sendMessage("queue is empty");
            return false;
        }

        newNamespace.addId(SpellHider.getFromPath(itemPath).getHash()); // update the stored mapping of hash -> namespace
        SpellHider.editNameOfPath(itemPath, newNamespace); // update the in-memory model

        if (unknownQueue.isEmpty()) ChatUtils.sendMessage("reached the end of the queue");
        else openFile(unknownQueue.peek());

        return true;
    }

    public static void openFile(String itemName) {
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
        } else {
            WynnExtras.LOGGER.error("Desktop OPEN is not supported: {}", itemName);
        }
    }
}
