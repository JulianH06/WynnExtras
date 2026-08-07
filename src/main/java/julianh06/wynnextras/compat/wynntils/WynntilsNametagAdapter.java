package julianh06.wynnextras.compat.wynntils;

import net.minecraft.client.render.entity.state.EntityRenderState;

import java.lang.reflect.Method;
import java.util.Optional;

public final class WynntilsNametagAdapter {
    private record NametagEventMethods(Class<?> eventClass, Method getEntityRenderState) {}

    private static final WynntilsCapability<NametagEventMethods> NAMETAG_EVENT = new WynntilsCapability<>(
            "player-nametag-event",
            () -> {
                Class<?> eventClass = WynntilsCompat.requireClass("com.wynntils.mc.event.PlayerNametagRenderEvent");
                return new NametagEventMethods(eventClass, eventClass.getMethod("getEntityRenderState"));
            }
    );

    private WynntilsNametagAdapter() {}

    public static Optional<EntityRenderState> getEntityRenderState(Object event) {
        if (event == null) return Optional.empty();
        return NAMETAG_EVENT.invoke(methods -> {
            if (!methods.eventClass.isInstance(event)) return null;
            Object state = methods.getEntityRenderState.invoke(event);
            return state instanceof EntityRenderState entityState ? entityState : null;
        });
    }
}
