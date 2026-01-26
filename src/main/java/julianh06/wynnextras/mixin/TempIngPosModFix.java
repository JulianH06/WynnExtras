package julianh06.wynnextras.mixin;

import com.wynntils.models.ingredients.type.IngredientPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Locale;

@Mixin(IngredientPosition.class)
public class TempIngPosModFix {
    /**
     * @author e
     * @reason e
     */
    @Overwrite(remap = false)
    public String getApiName() {
        IngredientPosition self = (IngredientPosition) (Object) this;
        return self.name().toLowerCase(Locale.ROOT);
    }
}
