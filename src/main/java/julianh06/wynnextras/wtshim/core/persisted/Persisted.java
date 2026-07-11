// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Adapted for the WynnExtras standalone compat shim (wtshim).
 *
 * NOTE: In Wynntils this annotation drives a config/storage persistence framework. The shim has
 * no such framework, so @Persisted is a no-op marker here — it only exists so faithful ports that
 * annotate their storage fields (e.g. RaidModel) compile unchanged. See {@link
 * julianh06.wynnextras.wtshim.core.persisted.storage.Storage} for the (in-memory) deviation.
 */
package julianh06.wynnextras.wtshim.core.persisted;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Persisted {
    String i18nKey() default "";
}
