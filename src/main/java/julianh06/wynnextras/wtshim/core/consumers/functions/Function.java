// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — Function<T> (faithful port). */
package julianh06.wynnextras.wtshim.core.consumers.functions;

import com.google.common.base.CaseFormat;
import julianh06.wynnextras.wtshim.core.consumers.functions.arguments.FunctionArguments;
import julianh06.wynnextras.wtshim.core.persisted.Translatable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public abstract class Function<T> implements Translatable {
    private final String name;

    private List<String> aliases;

    protected Function() {
        String name = this.getClass().getSimpleName().replace("Function", "");
        this.name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
    }

    @Override
    public String getTypeName() {
        return "Function";
    }

    public abstract T getValue(FunctionArguments arguments);

    public FunctionArguments.Builder getArgumentsBuilder() {
        return FunctionArguments.OptionalArgumentBuilder.EMPTY;
    }

    public String getName() {
        return name;
    }

    protected List<String> getAliases() {
        return List.of();
    }

    public final List<String> getAliasList() {
        if (aliases == null) {
            aliases = getAliases();
        }

        return aliases;
    }

    public String getDescription() {
        return getTranslation("description");
    }

    public String getArgumentDescription(String argumentName) {
        return getTranslation("argument." + argumentName);
    }

    public String getReturnTypeName() {
        Type typeArgument = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        if (typeArgument instanceof Class clazz) {
            return clazz.getSimpleName();
        }
        assert false;
        return typeArgument.getTypeName();
    }
}
