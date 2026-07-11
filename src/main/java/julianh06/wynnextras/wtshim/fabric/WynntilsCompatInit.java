// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — Wynntils-compat client initializer.
 *
 * Registers the features that WynnExtras mixes into, then wires Minecraft's chat pipeline into
 * the Wynntils-shaped event flow so WynnExtras' chat-dependent mixins continue to fire.
 *
 * Runs as a Fabric ClientModInitializer entry point (declared in fabric.mod.json).
 */
package julianh06.wynnextras.wtshim.fabric;

import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Handlers;
import julianh06.wynnextras.wtshim.core.components.Managers;
import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.core.events.MixinHelper;
import julianh06.wynnextras.wtshim.mc.event.ActionBarUpdatedEvent;
import julianh06.wynnextras.wtshim.mc.event.ConnectionEvent;
import julianh06.wynnextras.wtshim.mc.event.TickAlwaysEvent;
import julianh06.wynnextras.wtshim.mc.event.TickEvent;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.features.chat.MessageFilterFeature;
import julianh06.wynnextras.wtshim.features.inventory.InventoryEmeraldCountFeature;
import julianh06.wynnextras.wtshim.features.inventory.ItemFavoriteFeature;
import julianh06.wynnextras.wtshim.features.inventory.ItemHighlightFeature;
import julianh06.wynnextras.wtshim.features.inventory.ItemTextOverlayFeature;
import julianh06.wynnextras.wtshim.features.inventory.PersonalStorageUtilitiesFeature;
import julianh06.wynnextras.wtshim.features.inventory.UnidentifiedItemIconFeature;
import julianh06.wynnextras.wtshim.features.tooltips.ItemGuessFeature;
import julianh06.wynnextras.wtshim.features.tooltips.ItemStatInfoFeature;
import julianh06.wynnextras.wtshim.features.tooltips.TooltipFittingFeature;
import julianh06.wynnextras.wtshim.features.ui.ContainerScrollFeature;
import julianh06.wynnextras.wtshim.handlers.chat.event.ChatMessageEvent;
import julianh06.wynnextras.wtshim.mc.event.SystemMessageEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.Text;

public final class WynntilsCompatInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerFeatures();
        registerComponentsOnBus();
        initNetStack();
        registerTickFeed();
        registerConnectionFeed();
        registerChatPipeline();
        WynntilsMod.info("WynnExtras Wynntils-compat layer initialized.");
    }

    private void registerComponentsOnBus() {
        // Real Wynntils registers every component as a bus listener at init. Mirror
        // that for all static registry fields so ported models' @SubscribeEvent
        // handlers just work. Components without @SubscribeEvent are skipped by
        // EventBusWrapper's register guard.
        for (Object component : collectComponents()) {
            WynntilsMod.registerEventListener(component);
        }
    }

    private void initNetStack() {
        // Load the bundled url list, let every component declare its managed downloads, then kick them
        // off. Wynntils starts downloads once url processing finishes (an async event); our url load is
        // synchronous (bundled resource), so we trigger right here at init. No downloads are registered
        // yet, so this currently just loads the url list and logs "no downloads registered".
        try {
            Managers.Url.loadUrls();

            java.util.List<julianh06.wynnextras.wtshim.core.components.CoreComponent> components =
                    new java.util.ArrayList<>();
            for (Object component : collectComponents()) {
                if (component instanceof julianh06.wynnextras.wtshim.core.components.CoreComponent coreComponent) {
                    components.add(coreComponent);
                }
            }
            Managers.Download.registerDownloads(components);
            Managers.Download.startDownloads();
        } catch (Throwable t) {
            WynntilsMod.error("Failed to initialize the net stack", t);
        }
    }

    /** All component instances held in the static registries (Models, Handlers, Managers). */
    private java.util.List<Object> collectComponents() {
        java.util.List<Object> components = new java.util.ArrayList<>();
        for (Class<?> registry : new Class<?>[] {
            Models.class, Handlers.class, Managers.class, julianh06.wynnextras.wtshim.core.components.Services.class
        }) {
            for (java.lang.reflect.Field field : registry.getFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                try {
                    Object component = field.get(null);
                    if (component != null) components.add(component);
                } catch (Throwable t) {
                    WynntilsMod.error("Failed to read component " + field.getName(), t);
                }
            }
        }
        return components;
    }

    private void registerTickFeed() {
        // Wynntils posts TickAlwaysEvent (always) then TickEvent (only on Wynncraft)
        // from a Minecraft#tick mixin; feeding from Fabric's END_CLIENT_TICK is
        // equivalent for our consumers.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MixinHelper.postAlways(new TickAlwaysEvent());
            MixinHelper.post(new TickEvent());
        });
    }

    private void registerConnectionFeed() {
        // Wynntils' ConnectionManager derives WynncraftConnectionEvent from raw ConnectionEvents
        // fired by mixins on the connection/screen flow. In the shim, ConnectedEvent is fired from
        // the ClientPlayNetworkHandler onGameJoin mixin; the matching disconnect is taken from
        // Fabric's ClientPlayConnectionEvents.DISCONNECT (no reason string is available here, so
        // ConnectionManager's transfer-suppression is best-effort). The shim ConnectionManager
        // consumes these and re-posts WynncraftConnectionEvent for WorldStateModel.
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> MixinHelper.postAlways(new ConnectionEvent.DisconnectedEvent("")));
    }

    private void registerFeatures() {
        // Register the feature instances WynnExtras' mixins target. These are stand-ins — they
        // don't do anything on their own, but Managers.Feature.getFeatureInstance(X.class) must
        // return a non-null object so WynnExtras' lookups succeed.
        Managers.Feature.register(new MessageFilterFeature());
        Managers.Feature.register(new InventoryEmeraldCountFeature());
        Managers.Feature.register(new ItemFavoriteFeature());
        Managers.Feature.register(new ItemHighlightFeature());
        Managers.Feature.register(new ItemTextOverlayFeature());
        Managers.Feature.register(new PersonalStorageUtilitiesFeature());
        Managers.Feature.register(new UnidentifiedItemIconFeature());
        Managers.Feature.register(new ItemGuessFeature());
        Managers.Feature.register(new ItemStatInfoFeature());
        Managers.Feature.register(new TooltipFittingFeature());
        Managers.Feature.register(new ContainerScrollFeature());
    }

    private void registerChatPipeline() {
        // ALLOW_GAME fires for system/server messages (incl. Wynncraft chat). Returning false
        // suppresses the message from being rendered — we use that to honor cancelChat().
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (message == null) return true;
            if (overlay) {
                // Action-bar messages: feed the shim's action-bar event instead of chat.
                MixinHelper.post(new ActionBarUpdatedEvent(StyledText.fromComponent(message)));
                return true;
            }

            // Post on the shim bus: ChatHandler (HIGHEST priority) classifies the message
            // by RecipientType and posts ChatMessageEvent.Match/Edit for models/features.
            // If a Match listener cancels the chat, ChatHandler cancels this event.
            // ChatHandler (HIGHEST priority) classifies the message and posts
            // ChatMessageEvent.Match/Edit, which the ported models subscribe to directly
            // (BombModel, ProfessionModel, PartyModel, RaidModel, …). No legacy direct parser
            // calls remain here.
            SystemMessageEvent.ChatReceivedEvent event = new SystemMessageEvent.ChatReceivedEvent(message);
            WynntilsMod.postEvent(event);

            // Deviation vs Wynntils: ALLOW_GAME can only cancel, not rewrite — an edited
            // message (ChatMessageEvent.Edit / event.setMessage) is not applied to the chat.
            return !event.isCanceled();
        });
    }

    @SuppressWarnings("unused")
    private static Text toText(StyledText st) {
        return st == null ? Text.empty() : st.getComponent();
    }
}
