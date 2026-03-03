package julianh06.wynnextras.features.spellhider;

import com.wynntils.mc.extension.EntityExtension;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.SetEntityDataEvent;
import julianh06.wynnextras.utils.EntityUtils;
import julianh06.wynnextras.utils.ItemUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@WEModule
public class SpellHider {

    private static final Map<Integer, SpellNamespace> customModelNameMao = new HashMap<>();
    private static final Map<SpellNamespace, SpellModifiers> modifiersMap = new HashMap<>();

    public static void addModel(float model, SpellNamespace nameSpace) {
        if (customModelNameMao.containsKey((int) model)) return;
        customModelNameMao.put((int) model, nameSpace);
    }

    public static SpellNamespace getNameForModel(float model) {
        return customModelNameMao.get((int) model);
    }

    public static SpellNamespace getNameSpace(DisplayEntity.ItemDisplayEntity display) {
        Float model = ItemUtils.getFirsCustomModelDataFloat(display.getItemStack());
        if (model == null) return null;
        return SpellHider.getNameForModel(model);
    }

    public static SpellModifiers getModifiers(DisplayEntity.ItemDisplayEntity display) {
        if (display.getItemStack().getItem() != Items.OAK_BOAT) return null;
        SpellNamespace nameSpace = getNameSpace(display);
        if (nameSpace == null || nameSpace.isEmpty()) return null;
        return modifiersMap.get(nameSpace);
    }

    public static boolean modify(SpellNamespace nameSpace, SpellModifier type, Object value) {
        SpellModifiers modifiers = modifiersMap.compute(nameSpace, (k, v) -> v == null ? new SpellModifiers() : v);
        return modifiers.set(type, value);
    }

    public static Set<SpellNamespace> getAllCurrentNamespaces() {
        return new HashSet<>(customModelNameMao.values());
    }

    @SubscribeEvent
    public void onEntitySetData(SetEntityDataEvent event) {
        if (MinecraftClient.getInstance().world == null) return;
        Entity entity = MinecraftClient.getInstance().world.getEntityById(event.getId());
        if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
            SpellModifiers modifiers = getModifiers(display);
            if (modifiers == null) return;


            if (Boolean.FALSE.equals(modifiers.get(SpellModifier.VISIBLE))) ((EntityExtension) entity).setRendered(false);

            Vector3f scale = modifiers.get(SpellModifier.SCALE);
            if (scale != null) {
                EntityUtils.setScale(display, scale);
            }
        }
    }
}
