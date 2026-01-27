package julianh06.wynnextras.event.api;

import net.neoforged.bus.BusBuilderImpl;
import net.neoforged.bus.EventBus;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Arrays;

public class WEEventBus extends EventBus {
    private static IEventBus eventBus = new WEEventBus((BusBuilderImpl) BusBuilder.builder());

    private WEEventBus(BusBuilderImpl busBuilder) { super(busBuilder); }

    @Override
    public void register(Object target) {
        boolean anyEvents = Arrays.stream(target.getClass().getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(SubscribeEvent.class));

        if (!anyEvents) return;

        super.register(target);
    }

    public static void registerEventListener(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        eventBus.register(listener);
    }

    public static <T extends WEEvent> boolean postEvent(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        try {
            eventBus.post(event);
            return event instanceof ICancellableEvent cancellableEvent && cancellableEvent.isCanceled();
        } catch (Throwable t) {
            return false;
        }
    }
}
