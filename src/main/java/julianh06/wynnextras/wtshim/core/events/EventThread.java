// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.core.events;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inform what type of thread the event is allowed to be sent on. Events without an
 * explicit annotation is considered to be allowed on Type == RENDER only.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventThread {
    Type value() default Type.RENDER;

    enum Type {
        RENDER, // The main thread a.k.a the Render thread
        IO, // Any Netty Epoll Client IO thread
        WORKER, // A worker thread, from a Minecraft or Wynntils thread pool
        ANY // Any thread at all
    }
}
