package julianh06.wynnextras.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.InitEvent;
import julianh06.wynnextras.features.spellhider.SpellNamespace;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@WEModule
public class SpellHiderConfig {
    private static final Path MAPPINGS_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras")
            .resolve("default_spell_mappings.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static SpellHiderConfig INSTANCE = new SpellHiderConfig();


    @SubscribeEvent
    public void init(InitEvent empty) {
        SpellNamespace Mage = new SpellNamespace("mage");
        SpellNamespace LB = Mage.with("lightbender");
        SpellNamespace RW = Mage.with("riftwalker");
        SpellNamespace Arc = Mage.with("arcanist");
        SpellNamespace meteor = Mage.with("meteor");
        SpellNamespace teleport = Mage.with("teleport");
        SpellNamespace heal = Mage.with("heal");
        SpellNamespace snake = Mage.with("ice_snake");

        SpellNamespace Arch = new SpellNamespace("archer");
        SpellNamespace Bolt = Arch.with("boltslinger");
        SpellNamespace Trap = Arch.with("trapper");
        SpellNamespace Sharp = Arch.with("sharpshooter");
        SpellNamespace bomb = Arch.with("arrow_bomb");
        SpellNamespace escape = Arch.with("escape");
        SpellNamespace storm = Arch.with("arrow_storm");
        SpellNamespace shield = Arch.with("arrow_shield");

        SpellNamespace Ass = new SpellNamespace("assassin");
        SpellNamespace Shade = Ass.with("shadestepper");
        SpellNamespace Trick = Ass.with("trickster");
        SpellNamespace Acro = Ass.with("acrobat");
        SpellNamespace spin = Ass.with("spin_attack");
        SpellNamespace dash = Ass.with("dash");
        SpellNamespace smoke = Ass.with("smoke_bomb");
        SpellNamespace multi = Ass.with("multihit");

        SpellNamespace War = new SpellNamespace("warrior");
        SpellNamespace Fallen = War.with("fallen");
        SpellNamespace BMonk = War.with("battlemonk");
        SpellNamespace Paladin = War.with("paladin");
        SpellNamespace bash = War.with("bash");
        SpellNamespace charge = War.with("charge");
        SpellNamespace upper = War.with("uppercut");
        SpellNamespace scream = War.with("war_scream");

        SpellNamespace Sham = new SpellNamespace("shaman");
        SpellNamespace Summ = Sham.with("summoner");
        SpellNamespace Ritual = Sham.with("ritualist");
        SpellNamespace Aco = Sham.with("acolyte");
        SpellNamespace totem = Sham.with("totem");
        SpellNamespace haul = Sham.with("haul");
        SpellNamespace uproot = Sham.with("uproot");
        SpellNamespace aura = Sham.with("aura");

        snake.addId("item/w1206912931366766147");
    }

    private final Map<String, SpellNamespace> idMappings = new HashMap<>();

    public void addSpellIdentifier(String path, SpellNamespace namespace) {
        idMappings.put(path, namespace);
    }

    public SpellNamespace getSpellMapping(Identifier id) {
        return idMappings.get(id.getPath());
    }


}
