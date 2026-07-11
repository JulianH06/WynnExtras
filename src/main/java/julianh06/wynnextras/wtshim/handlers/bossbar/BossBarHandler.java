// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.handlers.bossbar;

import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Handler;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.handlers.bossbar.event.BossBarAddedEvent;
import julianh06.wynnextras.wtshim.mc.event.BossHealthUpdateEvent;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

public final class BossBarHandler extends Handler {
    private final Map<UUID, TrackedBar> presentBars = new HashMap<>();
    private final List<TrackedBar> knownBars = new ArrayList<>();

    public void registerBar(TrackedBar trackedBar) {
        knownBars.add(trackedBar);
    }

    // FixPacketBugsFeature gets in the way if receiveCanceled is not set
    @SubscribeEvent(receiveCanceled = true)
    public void onHealthBarEvent(BossHealthUpdateEvent event) {
        BossBarS2CPacket packet = event.getPacket();

        // Deviation: Wynntils uses packet.dispatch(ClientboundBossEventPacket.Handler). Yarn exposes
        // the equivalent visitor as BossBarS2CPacket#accept(BossBarS2CPacket.Consumer).
        packet.accept(new TrackedBarHandler(event));
    }

    private final class TrackedBarHandler implements BossBarS2CPacket.Consumer {
        private final BossHealthUpdateEvent event;

        private TrackedBarHandler(BossHealthUpdateEvent event) {
            this.event = event;
        }

        @Override
        public void add(
                UUID id,
                Text name,
                float progress,
                BossBar.Color color,
                BossBar.Style overlay,
                boolean darkenScreen,
                boolean playMusic,
                boolean createWorldFog) {
            Optional<Pair<TrackedBar, Matcher>> trackedBarOpt = matchBar(name);
            if (trackedBarOpt.isEmpty()) return;

            TrackedBar trackedBar = trackedBarOpt.get().a();
            Matcher matcher = trackedBarOpt.get().b();

            ClientBossBar bossEvent =
                    new ClientBossBar(id, name, progress, color, overlay, darkenScreen, playMusic, createWorldFog);
            trackedBar.setEvent(bossEvent);

            // Allow for others to try and cancel event
            BossBarAddedEvent barAddEvent = new BossBarAddedEvent(trackedBar);
            WynntilsMod.postEvent(barAddEvent);

            if (barAddEvent.isCanceled()) {
                trackedBar.setRendered(false);
                event.setCanceled(true);
            } else {
                trackedBar.setRendered(true);
            }

            trackedBar.onUpdateName(matcher);
            trackedBar.onUpdateProgress(progress);

            presentBars.put(id, trackedBar);
        }

        private void handleBarUpdate(UUID id, Consumer<TrackedBar> consumer) {
            TrackedBar trackedBar = presentBars.get(id);

            if (trackedBar != null) {
                if (!trackedBar.isRendered()) {
                    event.setCanceled(true);
                }

                consumer.accept(trackedBar);
            }
        }

        private Optional<Pair<TrackedBar, Matcher>> matchBar(Text name) {
            return knownBars.stream()
                    .flatMap(trackedBar -> trackedBar.patterns.stream().map(pattern -> new Pair<>(trackedBar, pattern)))
                    .map(pair ->
                            new Pair<>(pair.a(), StyledText.fromComponent(name).getMatcher(pair.b())))
                    .filter(pair -> pair.b().matches())
                    .findFirst();
        }

        @Override
        public void remove(UUID id) {
            handleBarUpdate(id, trackedBar -> {
                trackedBar.reset();
                presentBars.remove(id);
            });
        }

        @Override
        public void updateProgress(UUID id, float progress) {
            handleBarUpdate(id, trackedBar -> {
                trackedBar.getEvent().setPercent(progress);
                trackedBar.onUpdateProgress(progress);
            });
        }

        @Override
        public void updateName(UUID id, Text name) {
            // Some bars like the skip cutscene bar start out as an empty component and set the name later
            if (!presentBars.containsKey(id)) {
                Optional<Pair<TrackedBar, Matcher>> trackedBarOpt = matchBar(name);
                trackedBarOpt.ifPresent(trackedBarMatcherPair -> presentBars.put(id, trackedBarMatcherPair.a()));
            }

            handleBarUpdate(id, trackedBar -> {
                StyledText nameText = StyledText.fromComponent(name);

                for (Pattern pattern : trackedBar.patterns) {
                    Matcher matcher = nameText.getMatcher(pattern);
                    if (matcher.matches()) {
                        trackedBar.onUpdateName(matcher);
                        return;
                    }
                }

                WynntilsMod.error("Failed to match already matched boss bar");
                return;
            });
        }

        // We need to cancel the event even though we don't process it here
        @Override
        public void updateStyle(UUID id, BossBar.Color color, BossBar.Style overlay) {
            handleBarUpdate(id, trackedBar -> {});
        }

        @Override
        public void updateProperties(UUID id, boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
            handleBarUpdate(id, trackedBar -> {});
        }
    }
}
