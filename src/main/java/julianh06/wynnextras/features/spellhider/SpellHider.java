package julianh06.wynnextras.features.spellhider;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.InitEvent;
import julianh06.wynnextras.utils.ItemUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Items;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@WEModule
public class SpellHider {

    private static final Map<Integer, SpellNamespace> customModelNameMao = new HashMap<>();
    private static final Map<SpellNamespace, SpellModifiers> modifiersMap = new HashMap<>();

    public static final SpellNamespace Mage = new SpellNamespace("mage");
    public static final SpellNamespace LB = Mage.with("lightbender");
    public static final SpellNamespace RW = Mage.with("riftwalker");
    public static final SpellNamespace Arc = Mage.with("arcanist");

    public static final SpellNamespace Arch = new SpellNamespace("archer");
    public static final SpellNamespace Bolt = Arch.with("boltslinger");
    public static final SpellNamespace Trap = Arch.with("trapper");
    public static final SpellNamespace Sharp = Arch.with("sharpshooter");

    public static final SpellNamespace Ass = new SpellNamespace("assassin");
    public static final SpellNamespace Shade = Ass.with("shadestepper");
    public static final SpellNamespace Trick = Ass.with("trickster");
    public static final SpellNamespace Acro = Ass.with("acrobat");

    public static final SpellNamespace War = new SpellNamespace("warrior");
    public static final SpellNamespace Fallen = War.with("fallen");
    public static final SpellNamespace BMonk = War.with("battlemonk");
    public static final SpellNamespace Paladin = War.with("paladin");

    public static final SpellNamespace Sham = new SpellNamespace("shaman");
    public static final SpellNamespace Summ = Sham.with("summoner");
    public static final SpellNamespace Ritual = Sham.with("ritualist");
    public static final SpellNamespace Aco = Sham.with("acolyte");

    public static void addModel(int model, SpellNamespace nameSpace) {
        customModelNameMao.put(model, nameSpace);
    }

    public static void addModel(int min, int max, SpellNamespace name) {
        for (int i = min; i <= max; i++) {
            addModel(i, name);
        }
    }

    public static SpellNamespace getNameForModel(float model) {
        return customModelNameMao.get((int) model);
    }

    public static SpellNamespace getNameSpace(Entity e) {
        if (e instanceof DisplayEntity.ItemDisplayEntity display) {
            if (display.getItemStack().getItem() != Items.OAK_BOAT) return null;
            Float model = ItemUtils.getFirsCustomModelDataFloat(display.getItemStack());
            if (model == null) return null;
            return SpellHider.getNameForModel(model);
        }
        return null;
    }

    public static SpellModifiers getModifiers(Entity e) {
        SpellNamespace nameSpace = getNameSpace(e);
        if (nameSpace == null) return null;
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
    private void init(InitEvent empty) {
        addKnownMage();
        addKnownArcher();
        addKnownAssassin();
        addKnownWarrior();
        addKnownShaman();
    }

    private static void addKnownMage() {
        SpellNamespace snake = Mage.with("ice_snake");
        SpellNamespace meteor = Mage.with("meteor");

        snake.addModel(11935, 11943);
    }

    private static void addKnownArcher() {
    }

    private static void addKnownAssassin() {
    }

    private static void addKnownWarrior() {
    }

    private static void addKnownShaman() {
    }
}
