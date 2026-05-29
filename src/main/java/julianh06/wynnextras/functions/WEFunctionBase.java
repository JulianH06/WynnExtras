package julianh06.wynnextras.functions;

import com.google.common.base.CaseFormat;
import com.wynntils.core.consumers.functions.Function;

public abstract class WEFunctionBase<T> extends Function<T> {
    private final String WEName;

    protected WEFunctionBase(){
        String name = this.getClass().getSimpleName().replace("Function", "");
        WEName = "wynnextras_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
    }

    @Override
    public String getTypeName() {
        return "WynnExtrasFunction";
    }

    @Override
    public String getName() {
        return this.WEName;
    }
}

