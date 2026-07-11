// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 *
 * Faithful port of Wynntils' WorldStateModel. Deviations from upstream:
 *   - extends the shim Model (no super(List.of())).
 *   - No ActionBarHandler segment system: the CHARACTER_CREATION / CHARACTER_SELECTION /
 *     CHARACTER_WARDROBE glyph patterns (taken from Wynntils' segment matchers) are matched
 *     directly against the raw action-bar text carried by ActionBarUpdatedEvent. The fixed
 *     run of spaces between the two halves of each menu line is relaxed to \\s+.
 *   - No StreamerMode model: the isInStream() guard in update() is dropped.
 *   - No Housing model: the Models.Housing.updateHousingState(...) call in setWorldIfMatched
 *     is dropped (housing world-name detection itself is kept).
 *   - Connection state is fed by a slim shim ConnectionManager (see WynncraftConnectionEvent);
 *     the four WynncraftConnectionEvent handlers below are byte-identical to upstream.
 */
package julianh06.wynnextras.wtshim.models.worlds;

import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Handlers;
import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.core.mod.event.WynncraftConnectionEvent;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.mc.event.ActionBarUpdatedEvent;
import julianh06.wynnextras.wtshim.mc.event.ContainerSetContentEvent;
import julianh06.wynnextras.wtshim.mc.event.PlayerInfoEvent;
import julianh06.wynnextras.wtshim.models.worlds.bossbars.SkipCutsceneBar;
import julianh06.wynnextras.wtshim.models.worlds.event.CutsceneStartedEvent;
import julianh06.wynnextras.wtshim.models.worlds.event.WorldStateEvent;
import julianh06.wynnextras.wtshim.models.worlds.type.CutsceneState;
import julianh06.wynnextras.wtshim.models.worlds.type.ServerRegion;
import julianh06.wynnextras.wtshim.models.worlds.type.WorldState;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

public final class WorldStateModel extends Model {
    private static final UUID WORLD_NAME_UUID = UUID.fromString("16ff7452-714f-2752-b3cd-c3cb2068f6af");
    private static final Pattern WORLD_NAME = Pattern.compile("^§f {2}§lGlobal \\[(.*)\\]$");
    private static final Pattern HOUSING_NAME = Pattern.compile("^§f  §l([^§\"\\\\]{1,35})$");
    private static final Pattern QUICK_CONNECT_PATTERN = Pattern.compile("§aQuick Connect");
    private static final String WYNNCRAFT_BETA_NAME = "beta";
    private static final String UNKNOWN_WORLD = "WC??";

    // Adapted from Wynntils' action-bar segment matchers (CharacterCreationSegmentMatcher,
    // CharacterSelectionSegmentMatcher, CharacterWardrobeSegmentMatcher). Matched against the
    // formatting-stripped action-bar text since the shim has no ActionBarSegment system.
    // Private-use glyphs are written as \\u escapes so they cannot be mangled; the fixed run of
    // spaces between the two halves of each menu line is relaxed to \\s+.
    private static final Pattern CHARACTER_CREATION_PATTERN =
            Pattern.compile(" Left-Click to select\s+ Scroll up/down to browse");
    private static final Pattern CHARACTER_SELECTION_PATTERN =
            Pattern.compile(" Left-Click to play\s+ Right-Click to switch");
    private static final Pattern CHARACTER_WARDROBE_PATTERN = Pattern.compile(
            "C󏿾h󏿾a󏿾r󏿾a󏿾c󏿾t󏿾e󏿾r W󏿾a󏿾r󏿾d󏿾r󏿾o󏿾b󏿾e");

    private static final SkipCutsceneBar skipCutsceneBar = new SkipCutsceneBar();
    private CutsceneState cutsceneState = CutsceneState.NOT_IN_CUTSCENE;

    private String currentWorldName = "";
    private ServerRegion currentRegion = ServerRegion.WC;
    private long serverJoinTimestamp = 0;
    private boolean onBetaServer;
    private boolean hasJoinedAnyWorld = false;
    private boolean inCharacterWardrobe = false;

    private WorldState currentState = WorldState.NOT_CONNECTED;

    public WorldStateModel() {
        Handlers.BossBar.registerBar(skipCutsceneBar);
    }

    public boolean onWorld() {
        return currentState == WorldState.WORLD;
    }

    public boolean inCharacterWardrobe() {
        return inCharacterWardrobe;
    }

    public boolean isOnBetaServer() {
        return onBetaServer;
    }

    public WorldState getCurrentState() {
        return currentState;
    }

    private void setState(WorldState newState, String newWorldName, boolean isFirstJoinWorld) {
        if (newState == currentState && newWorldName.equals(currentWorldName)) return;

        WynntilsMod.info("Changing world state to " + newState);
        cutsceneEnded();
        WorldState oldState = currentState;
        // Switch state before sending event
        currentState = newState;
        currentWorldName = newWorldName;
        if (newState == WorldState.WORLD) {
            serverJoinTimestamp = System.currentTimeMillis();
        }

        if (currentWorldName.length() >= 2) {
            String region = currentWorldName.substring(0, 2);
            currentRegion = ServerRegion.fromString(region);
        }

        WynntilsMod.postEvent(new WorldStateEvent(newState, oldState, newWorldName, isFirstJoinWorld));
    }

    private void setState(WorldState newState) {
        setState(newState, "", false);
    }

    @SubscribeEvent
    public void disconnected(WynncraftConnectionEvent.Disconnected e) {
        setState(WorldState.NOT_CONNECTED);
    }

    @SubscribeEvent
    public void connectionAborted(WynncraftConnectionEvent.ConnectingAborted e) {
        setState(WorldState.NOT_CONNECTED);
    }

    @SubscribeEvent
    public void connecting(WynncraftConnectionEvent.Connecting e) {
        if (currentState != WorldState.NOT_CONNECTED) {
            WynntilsMod.error("Got connected event while already connected to server: " + e.getHost());
            currentState = WorldState.NOT_CONNECTED;
            currentWorldName = "";
        }

        String host = e.getHost();
        onBetaServer = host.equals(WYNNCRAFT_BETA_NAME);
        setState(WorldState.CONNECTING);
    }

    @SubscribeEvent
    public void connected(WynncraftConnectionEvent.Connected e) {
        if (currentState != WorldState.CONNECTING) {
            WynntilsMod.error("Got connected event without getting connecting event to server: " + e.getHost());
            currentState = WorldState.CONNECTING;
            currentWorldName = "";
        }

        setState(WorldState.INTERIM);
    }

    @SubscribeEvent
    public void remove(PlayerInfoEvent.PlayerLogOutEvent e) {
        if (e.getId().equals(WORLD_NAME_UUID) && !currentWorldName.isEmpty()) {
            setState(WorldState.INTERIM);
        }
    }

    @SubscribeEvent
    public void onActionBarUpdate(ActionBarUpdatedEvent event) {
        String actionBar = event.getContent().getStringWithoutFormatting();

        if (CHARACTER_CREATION_PATTERN.matcher(actionBar).find()) {
            onCharacterCreation();
        }
        if (CHARACTER_SELECTION_PATTERN.matcher(actionBar).find()) {
            onCharacterSelection();
        }
        inCharacterWardrobe = false;
        if (CHARACTER_WARDROBE_PATTERN.matcher(actionBar).find()) {
            onCharacterWardrobe();
        }
    }

    @SubscribeEvent
    public void onContainerSetEvent(ContainerSetContentEvent.Post e) {
        ScreenHandler inventoryMenu = McUtils.inventoryMenu();
        if (inventoryMenu == null) return;
        if (e.getContainerId() != inventoryMenu.syncId) return;
        ItemStack firstHotbarSlot = e.getItems().get(36);

        if (firstHotbarSlot.getItem().equals(Items.COMPASS)) {
            StyledText name = StyledText.fromComponent(firstHotbarSlot.getName());
            if (name.matches(QUICK_CONNECT_PATTERN)) {
                setState(WorldState.HUB);
                return;
            }
        }

        if (currentState == WorldState.HUB) {
            setState(WorldState.INTERIM);
        }
    }

    private void onCharacterCreation() {
        setState(WorldState.CHARACTER_SELECTION);
    }

    private void onCharacterSelection() {
        setState(WorldState.CHARACTER_SELECTION);
    }

    private void onCharacterWardrobe() {
        inCharacterWardrobe = true;
    }

    @SubscribeEvent
    public void update(PlayerInfoEvent.PlayerDisplayNameChangeEvent e) {
        if (!e.getId().equals(WORLD_NAME_UUID)) return;

        Text displayName = e.getDisplayName();
        StyledText name = StyledText.fromComponent(displayName);
        Matcher m = name.getMatcher(WORLD_NAME);
        if (setWorldIfMatched(m, false)) return;
        // must check in this order as housing name regex matches anything that WORLD_NAME would match, housing names
        // need to exclude world names.
        Matcher housingNameMatcher = name.getMatcher(HOUSING_NAME);
        setWorldIfMatched(housingNameMatcher, true);
    }

    private boolean setWorldIfMatched(Matcher m, boolean housing) {
        if (m.find()) {
            String worldName = housing ? currentWorldName : m.group(1);
            if (worldName.isEmpty() && housing) {
                worldName = UNKNOWN_WORLD;
                WynntilsMod.warn("Changed world via housing join, current world name is unknown");
            }
            setState(WorldState.WORLD, worldName, !hasJoinedAnyWorld);
            hasJoinedAnyWorld = true;
            return true;
        }
        return false;
    }

    public void cutsceneStarted(boolean groupCutscene) {
        if (cutsceneState == CutsceneState.NOT_IN_CUTSCENE) {
            cutsceneState = CutsceneState.IN_CUTSCENE;

            CutsceneStartedEvent event = new CutsceneStartedEvent(groupCutscene);
            WynntilsMod.postEvent(event);

            if (event.isCanceled()) {
                cutsceneState = CutsceneState.SKIPPED_CUTSCENE;
            }
        }
    }

    public void cutsceneEnded() {
        cutsceneState = CutsceneState.NOT_IN_CUTSCENE;
    }

    /**
     * @return Full name of the current world, such as "NA32"
     */
    public String getCurrentWorldName() {
        return currentWorldName;
    }

    public ServerRegion getCurrentServerRegion() {
        return currentRegion;
    }

    public long getServerJoinTimestamp() {
        return serverJoinTimestamp;
    }
}
