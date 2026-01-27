package julianh06.wynnextras.mixin.Accessor;

import com.wynntils.features.inventory.PersonalStorageUtilitiesFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin (value = PersonalStorageUtilitiesFeature.class, remap = false)
public interface PersonalStorageUtilitiesFeatureAccessor {
    @Accessor (value = "lastPage", remap = false)
    int getLastPage();

    @Accessor (value = "lastPage", remap = false)
    void setLastPage(int lastPage);
}
