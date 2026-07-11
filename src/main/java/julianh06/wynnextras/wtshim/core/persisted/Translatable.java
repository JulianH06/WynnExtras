// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — Translatable (faithful port, Yarn I18n). */
package julianh06.wynnextras.wtshim.core.persisted;

import com.google.common.base.CaseFormat;
import java.util.Locale;
import net.minecraft.client.resource.language.I18n;

public interface Translatable {
    String getTypeName();

    default String getTranslation(String keySuffix, Object... parameters) {
        return I18n.translate(
                getTypeName().toLowerCase(Locale.ROOT) + ".wynntils." + getTranslationKeyName() + "." + keySuffix,
                parameters);
    }

    default String getTranslationKeyName() {
        String name = this.getClass().getSimpleName().replace(getTypeName(), "");
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
    }

    default String getTranslatedName() {
        return getTranslation("name");
    }
}
