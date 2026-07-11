// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.mc.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ContainerCloseEvent extends Event {
    public static class Pre extends ContainerCloseEvent implements ICancellableEvent {}

    public static class Post extends ContainerCloseEvent {}
}
