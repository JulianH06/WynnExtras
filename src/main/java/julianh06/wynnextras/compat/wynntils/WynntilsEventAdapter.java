package julianh06.wynnextras.compat.wynntils;

import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class WynntilsEventAdapter {
    private record ContainerClickBinding(Constructor<?> constructor, Method post, Method isCanceled) {}

    private static final WynntilsCapability<ContainerClickBinding> CONTAINER_CLICK = new WynntilsCapability<>(
            "container-click-event",
            () -> {
                Class<?> eventClass = WynntilsCompat.requireClass("com.wynntils.mc.event.ContainerClickEvent");
                Constructor<?> constructor = eventClass.getConstructor(
                        ScreenHandler.class, int.class, SlotActionType.class, int.class);
                Class<?> mixinHelper = WynntilsCompat.requireClass("com.wynntils.core.events.MixinHelper");
                Method post = null;
                for (Method method : mixinHelper.getMethods()) {
                    if (method.getName().equals("post") && method.getParameterCount() == 1
                            && method.getParameterTypes()[0].isAssignableFrom(eventClass)) {
                        post = method;
                        break;
                    }
                }
                if (post == null) throw new NoSuchMethodException("MixinHelper.post(event)");
                Method isCanceled = eventClass.getMethod("isCanceled");
                return new ContainerClickBinding(constructor, post, isCanceled);
            }
    );

    private WynntilsEventAdapter() {}

    public static boolean postContainerClick(ScreenHandler handler, int slot, SlotActionType action, int button) {
        if (handler == null) return false;
        return CONTAINER_CLICK.invoke(binding -> {
            Object event = binding.constructor.newInstance(handler, slot, action, button);
            binding.post.invoke(null, event);
            return (boolean) binding.isCanceled.invoke(event);
        }).orElse(false);
    }
}
