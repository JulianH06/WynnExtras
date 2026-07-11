// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.mc.event;

import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Fired for Menu events */
public abstract class MenuEvent extends Event {
    /** Fired for Menu opened events */
    public abstract static class MenuOpenedEvent extends MenuEvent {
        private final ScreenHandlerType<?> menuType;
        private final Text title;
        private final int containerId;

        protected MenuOpenedEvent(ScreenHandlerType<?> menuType, Text title, int containerId) {
            this.menuType = menuType;
            this.title = title;
            this.containerId = containerId;
        }

        public ScreenHandlerType<?> getMenuType() {
            return menuType;
        }

        public Text getTitle() {
            return title;
        }

        public int getContainerId() {
            return containerId;
        }

        public static final class Pre extends MenuOpenedEvent implements ICancellableEvent {
            public Pre(ScreenHandlerType<?> menuType, Text title, int containerId) {
                super(menuType, title, containerId);
            }
        }

        public static final class Post extends MenuOpenedEvent {
            public Post(ScreenHandlerType<?> menuType, Text title, int containerId) {
                super(menuType, title, containerId);
            }
        }
    }

    /** Fired for Menu closed events */
    public static class MenuClosedEvent extends MenuEvent implements ICancellableEvent {
        private final int containerId;

        public MenuClosedEvent(int containerId) {
            this.containerId = containerId;
        }

        public int getContainerId() {
            return containerId;
        }
    }
}
