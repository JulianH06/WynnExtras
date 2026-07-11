// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.core.events;

import java.util.Arrays;
import net.neoforged.bus.BusBuilderImpl;
import net.neoforged.bus.EventBus;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;

public class EventBusWrapper extends EventBus {
    private EventBusWrapper(BusBuilderImpl busBuilder) {
        super(busBuilder);
    }

    public static IEventBus createEventBus() {
        return new EventBusWrapper((BusBuilderImpl) BusBuilder.builder());
    }

    @Override
    public void register(Object target) {
        // NeoForge's bus throws when registering objects without any @SubscribeEvent;
        // we deliberately register all shim components, many of which have none.
        boolean anyEvents = Arrays.stream(target.getClass().getMethods())
                .anyMatch(method -> method.isAnnotationPresent(SubscribeEvent.class));
        if (!anyEvents) return;

        super.register(target);
    }
}
