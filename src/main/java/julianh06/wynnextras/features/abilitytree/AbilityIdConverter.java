package julianh06.wynnextras.features.abilitytree;

import com.google.gson.*;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AbilityIdConverter {

    // Static nested maps for each class (add other classes as needed)
    private static final Map<String, Map<String, String>> ALL_MAPPINGS = new HashMap<>();

    static {
        // Example for MAGE (add/expand as needed!)
        Map<String, String> mage = Map.ofEntries(
                Map.entry("airborne", "Diffusion"),
                Map.entry("arcaneOverflow", "Arcane Overflow"),
                Map.entry("arcaneSpeed", "Arcane Speed"),
                Map.entry("arcaneTransfer", "Arcane Transfer"),
                Map.entry("betterChaosExplosion", "Memory Recollection"),
                Map.entry("betterLightDance", "Halo"),
                Map.entry("betterOphanim", "Augury"),
                Map.entry("betterOphanim2", "Divination"),
                Map.entry("betterWinded", "More Winded"),
                Map.entry("betterWinded2", "More Winded II"),
                Map.entry("blink", "Blink"),
                Map.entry("breathless", "Breathless"),
                Map.entry("burningSigil", "Burning Sigil"),
                Map.entry("chaosExplosion", "Chaos Explosion"),
                Map.entry("devitalize", "Devitalize"),
                Map.entry("explosiveEntrance", "Explosive Entrance"),
                Map.entry("eyePiercer", "Eye Piercer"),
                Map.entry("fluidHealing", "Fluid Healing"),
                Map.entry("freezingSigil", "Freezing Sigil"),
                Map.entry("geyserSnake", "Gust"),
                Map.entry("heal", "Heal"),
                Map.entry("healCost1", "Cheaper Heal"),
                Map.entry("healCost2", "Cheaper Heal II"),
                Map.entry("healthierOphanim1", "Resilient Light"),
                Map.entry("healthierOphanim2", "Everlasting Light"),
                Map.entry("icesnake", "Ice Snake"),
                Map.entry("icesnakeCost1", "Cheaper Ice Snake"),
                Map.entry("icesnakeCost2", "Cheaper Ice Snake II"),
                Map.entry("lightBuff", "Fortitude"),
                Map.entry("lightDance", "Lightweaver"),
                Map.entry("mageAirPath", "Air Mastery"),
                Map.entry("mageEarthPath", "Earth Mastery"),
                Map.entry("mageFirePath", "Fire Mastery"),
                Map.entry("mageThunderPath", "Thunder Mastery"),
                Map.entry("mageWaterPath", "Water Mastery"),
                Map.entry("manastorm", "Manastorm"),
                Map.entry("massImmune", "Sunflare"),
                Map.entry("meteor", "Meteor"),
                Map.entry("meteorCost1", "Cheaper Meteor"),
                Map.entry("meteorCost2", "Cheaper Meteor II"),
                Map.entry("meteorDamage1", "Crashing Comet"),
                Map.entry("moreHealRange1", "Larger Heal"),
                Map.entry("moreManaBank1", "Larger Mana Bank"),
                Map.entry("moreManaBank2", "Larger Mana Bank II"),
                Map.entry("moreManaBank3", "Larger Mana Bank III"),
                Map.entry("moreManaBankCharges", "Arcane Power"),
                Map.entry("ophanim", "Ophanim"),
                Map.entry("orphionsPulse", "Orphion's Pulse"),
                Map.entry("psychokinesis", "Psychokinesis"),
                Map.entry("purification", "Purification"),
                Map.entry("pyrokinesis", "Pyrokinesis"),
                Map.entry("pyrokinesisMana", "Arcane Restoration"),
                Map.entry("seance", "Seance"),
                Map.entry("seekingsnake", "Sentient Snake"),
                Map.entry("shootingStar", "Shooting Star"),
                Map.entry("slownessSnake", "Arctic Snake"),
                Map.entry("snakeNest", "Snake Nest"),
                Map.entry("strongerOphanim", "Incandescence"),
                Map.entry("strongerSunshower", "Searing Light"),
                Map.entry("sunshower", "Sunshower"),
                Map.entry("teleport", "Teleport"),
                Map.entry("teleportCost1", "Cheaper Teleport"),
                Map.entry("teleportCost2", "Cheaper Teleport II"),
                Map.entry("thunderstorm", "Thunderstorm"),
                Map.entry("timeDilation", "Time Dilation"),
                Map.entry("timelock", "Timelock"),
                Map.entry("touchOfLight", "Wand Proficiency II"),
                Map.entry("transonicWarp", "Transonic Warp"),
                Map.entry("wandProficiency", "Wand Proficiency I"),
                Map.entry("windSlash", "Wind Slash"),
                Map.entry("windSweeper", "Windsweeper"),
                Map.entry("windcharged", "Windcharged"),
                Map.entry("wisdom", "Wisdom")
                // Add any additional nodes if needed!
        );

        ALL_MAPPINGS.put("mage", mage);

        // For other classes, add: ALL_MAPPINGS.put("archer", Map.ofEntries(...));
        Map<String, String> archer = Map.ofEntries(
                Map.entry("archerAirPath", "Air Mastery"),
                Map.entry("archerEarthPath", "Earth Mastery"),
                Map.entry("archerFirePath", "Fire Mastery"),
                Map.entry("archerThunderPath", "Thunder Mastery"),
                Map.entry("archerWaterPath", "Water Mastery"),
                Map.entry("arrowBombCost1", "Cheaper Arrow Bomb"),
                Map.entry("arrowBombCost2", "Cheaper Arrow Bomb II"),
                Map.entry("arrowShieldCost1", "Cheaper Arrow Shield"),
                Map.entry("arrowShieldCost2", "Cheaper Arrow Shield II"),
                Map.entry("arrowStormCost1", "Cheaper Arrow Storm"),
                Map.entry("arrowStormCost2", "Cheaper Arrow Storm II"),
                Map.entry("arrowbomb", "Arrow Bomb"),
                Map.entry("arrowrain", "Buckshot"),
                Map.entry("arrowshield", "Arrow Shield"),
                Map.entry("arrowstorm", "Arrow Storm"),
                Map.entry("arsenalSynergy", "Arsenal Synergy"),
                Map.entry("beastLore", "Beast Lore"),
                Map.entry("betterArrowShield", "Honed Shield"),
                Map.entry("betterGuardianAngels", "Vigilant Sentinels"),
                Map.entry("betterLeap", "Bounding Stride"),
                Map.entry("betterWindyFeet", "Stormy Feet"),
                Map.entry("bouncing", "Bouncing Bomb"),
                Map.entry("bowProficiency", "Bow Proficiency"),
                Map.entry("concentration", "Twain's Arc"),
                Map.entry("coursingRestraints", "Coursing Restraints"),
                Map.entry("decimator", "Decimator"),
                Map.entry("directHit", "Heart Shatter"),
                Map.entry("divineIntervention", "Divine Intervention"),
                Map.entry("dontGetHit", "Elusive"),
                Map.entry("escape", "Escape"),
                Map.entry("escapeArtist", "Parting Gift"),
                Map.entry("escapeCost1", "Cheaper Escape"),
                Map.entry("escapeCost2", "Cheaper Escape II"),
                Map.entry("explodingTrap", "Basaltic Trap"),
                Map.entry("fierceStomp", "Fierce Stomp"),
                Map.entry("fireCreep", "Fire Creep"),
                Map.entry("focus", "Focus"),
                Map.entry("ghostlyTrigger", "Ghostly Trigger"),
                Map.entry("grapeBomb", "Grape Bomb"),
                Map.entry("grapplingHook", "Grappling Hook"),
                Map.entry("guardianAngels", "Guardian Angels"),
                Map.entry("hastyShots", "Double Shots"),
                Map.entry("helicopter", "Arrow Hurricane"),
                Map.entry("hitToRename", "Frenzy"),
                Map.entry("homingarrows", "Homing Shots"),
                Map.entry("iceTrap", "Chilling Snare"),
                Map.entry("implosion", "Implosion"),
                Map.entry("initiator", "Initiator"),
                Map.entry("ivyrootMamba", "Ivyroot Mamba"),
                Map.entry("leap", "Leap"),
                Map.entry("manaTrap", "Mana Trap"),
                Map.entry("moreFocus", "More Focus"),
                Map.entry("moreFocus2", "More Focus II"),
                Map.entry("moreTraps1", "More Traps"),
                Map.entry("mossyArrowStorm", "Bryophyte Roots"),
                Map.entry("murderFlock", "Murder Flock"),
                Map.entry("nimbleString", "Nimble String"),
                Map.entry("phantomDarts", "Phantom Ray"),
                Map.entry("phantomForce", "Phantom Force"),
                Map.entry("phasingBeam", "Phasing Beam"),
                Map.entry("powerShots", "Power Shots"),
                Map.entry("pyrotechnics", "Pyrotechnics"),
                Map.entry("recycling", "Recycling"),
                Map.entry("rocketJump", "Rocket Jump"),
                Map.entry("scorchedEarth", "Scorched Earth"),
                Map.entry("shockingBomb", "Shocking Bomb"),
                Map.entry("shrapnelBomb", "Shrapnel Bomb"),
                Map.entry("snowStorm", "Snow Storm"),
                Map.entry("spectralHover", "Spectral Hover"),
                Map.entry("strongerHook", "Hookshot"),
                Map.entry("theAscendedOne", "Crepuscular Ray"),
                Map.entry("timeBomb", "Patient Hunter"),
                Map.entry("timeBombMaxDamage", "Swift Primer"),
                Map.entry("traveler", "Traveler"),
                Map.entry("triangulation", "Tangled Traps"),
                Map.entry("tripleShots", "Triple Shots"),
                Map.entry("tripleshield", "Arrow Wall"),
                Map.entry("windstorm", "Windstorm"),
                Map.entry("windyfeet", "Windy Feet"),
                Map.entry("wolfSummon", "Call of the Hound")
                // Add any additional nodes if needed!
        );

        ALL_MAPPINGS.put("archer", archer);

        Map<String, String> warrior = Map.ofEntries(
                Map.entry("aerodynamics", "Aerodynamics"),
                Map.entry("altruism", "Blood Pact"),
                Map.entry("ambidextrous", "Riposte"),
                Map.entry("armourbreaker", "Armour Breaker"),
                Map.entry("autubyr", "Mindless Slaughter"),
                Map.entry("bash", "Bash"),
                Map.entry("bashCost1", "Cheaper Bash"),
                Map.entry("bashCost2", "Cheaper Bash II"),
                Map.entry("bashDamage1", "Crushing Blow"),
                Map.entry("betterAltruism", "Haemorrhage"),
                Map.entry("betterEnragedBlow", "Overwhelming Rage"),
                Map.entry("betterFirewater", "Exhilarate"),
                Map.entry("bigHands", "Pressure"),
                Map.entry("bloodlust", "Bloodlust"),
                Map.entry("boilingBlood", "Boiling Blood"),
                Map.entry("brickwall", "Mythril Skin"),
                Map.entry("brinkOfMadness", "Brink of Madness"),
                Map.entry("charge", "Charge"),
                Map.entry("chargeCost1", "Cheaper Charge"),
                Map.entry("chargeCost2", "Spirit of the Rabbit"),
                Map.entry("chaser", "Vehement"),
                Map.entry("cleansingBreeze", "Cleansing Breeze"),
                Map.entry("collide", "Collide"),
                Map.entry("counter", "Counter"),
                Map.entry("cyclone", "Cyclone"),
                Map.entry("doubleBash", "Double Bash"),
                Map.entry("emboldeningCry", "Emboldening Cry"),
                Map.entry("enragedBlow", "Enraged Blow"),
                Map.entry("fasterRoarOfTheBerserker", "Uncontainable Corruption"),
                Map.entry("fireUppercut", "Flaming Uppercut"),
                Map.entry("firewater", "Intoxicating Blood"),
                Map.entry("flyby", "Flyby Jab"),
                Map.entry("freestreamFlow", "Freestream Flow"),
                Map.entry("furiousBlow", "Massacre"),
                Map.entry("generalist", "Generalist"),
                Map.entry("gust", "Iron Lungs"),
                Map.entry("heavenlyTrumpet", "Heavenly Trumpet"),
                Map.entry("heavyImpact", "Heavy Impact"),
                Map.entry("holystrikes", "Discombobulate"),
                Map.entry("lusterPurge", "Luster Purge"),
                Map.entry("malcontentDissonance", "Reverberance"),
                Map.entry("manachism", "Manachism"),
                Map.entry("massiveBash", "Massive Bash"),
                Map.entry("protectiveBash", "Radiance"),
                Map.entry("provoke", "Provoke"),
                Map.entry("quintupleBash", "Quadruple Bash"),
                Map.entry("radiantDevotee", "Radiant Devotee"),
                Map.entry("rejuvenatingSkin", "Rejuvenating Skin"),
                Map.entry("roarOfTheBerserker", "Bak'al's Grasp"),
                Map.entry("roundabout", "Whirlwind Strike"),
                Map.entry("sacredSurge", "Sacred Surge"),
                Map.entry("secondchance", "Second Chance"),
                Map.entry("shield", "Mantle of the Bovemists"),
                Map.entry("shieldexplosion", "Shield Strike"),
                Map.entry("sparklingHope", "Sparkling Hope"),
                Map.entry("spearProficiency", "Spear Proficiency I"),
                Map.entry("spearProficiency2", "Spear Proficiency II"),
                Map.entry("strongerSacredSurge", "Consecration"),
                Map.entry("strongerShield", "Blessed Bulwark"),
                Map.entry("tackle", "Flying Kick"),
                Map.entry("tempest", "Tempest"),
                Map.entry("thunderclap", "Thunderclap"),
                Map.entry("tougherSkin", "Tougher Skin"),
                Map.entry("uppercut", "Uppercut"),
                Map.entry("uppercutCost1", "Cheaper Uppercut"),
                Map.entry("uppercutCost2", "Cheaper Uppercut II"),
                Map.entry("uppercutDamage1", "Axe Kick"),
                Map.entry("uppercutGrade2", "Fireworks"),
                Map.entry("uppercutGrade3", "Comet"),
                Map.entry("warScream", "War Scream"),
                Map.entry("warriorAirPath", "Air Mastery"),
                Map.entry("warriorEarthPath", "Earth Mastery"),
                Map.entry("warriorFirePath", "Fire Mastery"),
                Map.entry("warriorThunderPath", "Thunder Mastery"),
                Map.entry("warriorWaterPath", "Water Mastery"),
                Map.entry("warscreamCost1", "Cheaper War Scream"),
                Map.entry("warscreamCost2", "Cheaper War Scream II"),
                Map.entry("warscreamGrade2", "Air Shout"),
                Map.entry("worship", "Half-Moon Swipe")
                // Add any additional nodes if needed!
        );

        ALL_MAPPINGS.put("warrior", warrior);

        Map<String, String> shaman = Map.ofEntries(
                Map.entry("aura", "Aura"),
                Map.entry("auraCost1", "Cheaper Aura"),
                Map.entry("auraCost2", "Cheaper Aura II"),
                Map.entry("auraDamage1", "Imbued Totem"),
                Map.entry("auraPull", "Storm Dance"),
                Map.entry("betterMasquerade", "Depersonalization"),
                Map.entry("bloodConnection", "Blood Connection"),
                Map.entry("bloodLament", "Blood Sorrow"),
                Map.entry("bloodPool", "Sacrificial Shrine"),
                Map.entry("chantOfTheCoward", "Chant of the Heretic"),
                Map.entry("chantOfTheFanatic", "Chant of the Fanatic"),
                Map.entry("chantOfTheLunatic", "Chant of the Lunatic"),
                Map.entry("commander", "Commander"),
                Map.entry("danceOfTheRain", "Rain Dance"),
                Map.entry("deeperWounds", "Deeper Wounds"),
                Map.entry("doubleTotem", "Double Totem"),
                Map.entry("eldritchCall", "Eldritch Call"),
                Map.entry("explodingPuppet", "Exploding Puppets"),
                Map.entry("flamingTongue", "Flaming Tongue"),
                Map.entry("haul", "Haul"),
                Map.entry("haulCost1", "Cheaper Haul"),
                Map.entry("haulCost2", "Cheaper Haul II"),
                Map.entry("hauntingMemory", "Haunting Memory"),
                Map.entry("helpingHand2", "Friendly Fire"),
                Map.entry("hummingbirds", "Hummingbird's Song"),
                Map.entry("hummingbirds2", "Soaring Wingbeats"),
                Map.entry("hymnOfFreedom", "Frog Dance"),
                Map.entry("hymnOfHate", "Hymn of Hate"),
                Map.entry("invigoratingWave", "Invigorating Wave"),
                Map.entry("jungleSlayer", "Crimson Effigy"),
                Map.entry("lashingLance", "Lashing Lance"),
                Map.entry("maddeningRoots", "Maddening Roots"),
                Map.entry("maskOfTheAwakened", "Awakened"),
                Map.entry("maskOfTheCoward", "Mask of the Heretic"),
                Map.entry("maskOfTheFanatic", "Mask of the Fanatic"),
                Map.entry("maskOfTheLunatic", "Mask of the Lunatic"),
                Map.entry("maskRotation", "Masquerade"),
                Map.entry("moreBloodPool", "Larger Blood Pool"),
                Map.entry("moreBloodPool2", "Bloodier"),
                Map.entry("moreBloodPool3", "Bloodier II"),
                Map.entry("morePuppets", "More Puppets"),
                Map.entry("morePuppets2", "More Puppets II"),
                Map.entry("naturejolt", "Nature's Jolt"),
                Map.entry("overseer", "Overseer"),
                Map.entry("puppetMaster", "Puppet Master"),
                Map.entry("rebound", "Rebound"),
                Map.entry("regeneration", "Regeneration"),
                Map.entry("relikBeams", "Hand of the Shaman"),
                Map.entry("relikProficiency", "Relik Proficiency I"),
                Map.entry("relikSpread", "Distant Grasp"),
                Map.entry("sanguineStrike", "Sanguine Strike"),
                Map.entry("seekingTotem", "Seeking Totem"),
                Map.entry("shamanAirPath", "Air Mastery"),
                Map.entry("shamanEarthPath", "Earth Mastery"),
                Map.entry("shamanFirePath", "Fire Mastery"),
                Map.entry("shamanThunderPath", "Thunder Mastery"),
                Map.entry("shamanWaterPath", "Water Mastery"),
                Map.entry("sharpHealing", "Fluid Healing"),
                Map.entry("shepherd", "Shepherd"),
                Map.entry("shockingAura", "Shocking Aura"),
                Map.entry("stagnation", "Stagnation"),
                Map.entry("strongerTether", "Bloodletting"),
                Map.entry("summonFocus", "Bullwhip"),
                Map.entry("tankBloodPool", "Blood Rite"),
                Map.entry("tether", "Twisted Tether"),
                Map.entry("totem", "Totem"),
                Map.entry("totemCost1", "Cheaper Totem"),
                Map.entry("totemCost2", "Cheaper Totem II"),
                Map.entry("totemRange", "Totemic Reach"),
                Map.entry("totemShove", "Totem Shove"),
                Map.entry("totemicShatter", "Totemic Shatter"),
                Map.entry("totemicsmash", "Totemic Smash"),
                Map.entry("tripleTotem", "Triple Totem"),
                Map.entry("uproot", "Uproot"),
                Map.entry("uprootCost1", "Cheaper Uproot"),
                Map.entry("uprootCost2", "Cheaper Uproot II"),
                Map.entry("vengefulspirit", "Vengeful Spirit")
                // Add any additional nodes if needed!
        );
        ALL_MAPPINGS.put("shaman", shaman);

        Map<String, String> assassin = Map.ofEntries(
                Map.entry("aerialAce", "Aerial Ace"),
                Map.entry("assassinAirPath", "Air Mastery"),
                Map.entry("assassinEarthPath", "Earth Mastery"),
                Map.entry("assassinFirePath", "Fire Mastery"),
                Map.entry("assassinThunderPath", "Thunder Mastery"),
                Map.entry("assassinWaterPath", "Water Mastery"),
                Map.entry("backstab", "Backstab"),
                Map.entry("bamboozle", "Bamboozle"),
                Map.entry("betterFatalstrike", "Ambush"),
                Map.entry("betterLacerate", "Eviscerate"),
                Map.entry("betterMarkedMana", "Devour"),
                Map.entry("betterMultihit", "Mutilate"),
                Map.entry("betterSmokeBomb", "Rolling Fog"),
                Map.entry("bigHit", "Violent Vortex"),
                Map.entry("bigHit2", "Bladestorm"),
                Map.entry("blackHole", "Black Hole"),
                Map.entry("chainedHits", "Sandbagging"),
                Map.entry("daggerProficiency", "Dagger Proficiency I"),
                Map.entry("dash", "Dash"),
                Map.entry("dashCost1", "Cheaper Dash"),
                Map.entry("dashCost2", "Cheaper Dash II"),
                Map.entry("dashSlash", "Dancing Blade"),
                Map.entry("deflagrate", "Deflagrate"),
                Map.entry("discombobulate", "Blazing Powder"),
                Map.entry("distraction", "Distraction"),
                Map.entry("doubleSlice", "Double Slice"),
                Map.entry("doubleSpin", "Double Spin"),
                Map.entry("duplicity", "Duplicity"),
                Map.entry("echo", "Echo"),
                Map.entry("explodingClones", "Last Laugh"),
                Map.entry("fatalstrike", "Surprise Strike"),
                Map.entry("finality", "Finality"),
                Map.entry("foulPlay", "Foul Play"),
                Map.entry("hoodwink", "Hoodwink"),
                Map.entry("hop", "Hop"),
                Map.entry("jasminBloom", "Jasmine Bloom"),
                Map.entry("lacerate", "Lacerate"),
                Map.entry("maliciousMockery", "Malicious Mockery"),
                Map.entry("markPulls", "Death Magnet"),
                Map.entry("marked", "Marked"),
                Map.entry("markedDamage", "Marked for Death"),
                Map.entry("markedMana", "Harvester"),
                Map.entry("mirage", "Mirage"),
                Map.entry("misdirection", "Misdirection"),
                Map.entry("moreMarks", "More Marks"),
                Map.entry("multiTeleportation", "Blade Fury"),
                Map.entry("multihit", "Multihit"),
                Map.entry("multihitCost1", "Cheaper Multihit"),
                Map.entry("multihitCost2", "Cheaper Multihit II"),
                Map.entry("nightcloakKnives", "Nightcloak Knives"),
                Map.entry("noxiousHaze", "Noxious Haze"),
                Map.entry("poisonedBlade", "Poisoned Blade"),
                Map.entry("psithurism", "Psithurism"),
                Map.entry("rangeMultihit1", "Far Reach"),
                Map.entry("ricochets", "Ricochets"),
                Map.entry("rightingReflex", "Righting Reflex"),
                Map.entry("shadowClones", "Mirror Image"),
                Map.entry("shadowDash", "Dissolution"),
                Map.entry("shadowProjection", "Shadow Projection"),
                Map.entry("shadowSiphon", "Shadow Siphon"),
                Map.entry("shadowtravel", "Shadow Travel"),
                Map.entry("shindeiru", "Satsujin"),
                Map.entry("silentKiller", "Silent Killer"),
                Map.entry("smokeBombCost1", "Cheaper Smoke Bomb"),
                Map.entry("smokeBombCost2", "Cheaper Smoke Bomb II"),
                Map.entry("smokebomb", "Smoke Bomb"),
                Map.entry("soulSiphon", "Soul Siphon"),
                Map.entry("spinAttackCost1", "Cheaper Spin Attack"),
                Map.entry("spinAttackCost2", "Cheaper Spin Attack II"),
                Map.entry("spinAttackmarked", "Fatal Spin"),
                Map.entry("spinattack", "Spin Attack"),
                Map.entry("stanceChange", "Shurikens"),
                Map.entry("stickyBomb", "Sticky Bomb"),
                Map.entry("stomp", "Ripple"),
                Map.entry("swanDive", "Swan Dive"),
                Map.entry("toxicSludge", "Toxic Sludge"),
                Map.entry("vanish", "Vanish"),
                Map.entry("wallOfSmoke", "Wall of Smoke"),
                Map.entry("walljump", "Wall Jump"),
                Map.entry("weightless", "Weightless")
                // Add any additional nodes if needed!
        );

        ALL_MAPPINGS.put("assassin", assassin);



    }

    // Call this function! Arguments: className (e.g. "mage"), fileName (with or without .json)
    public static void convert(String className, String fileName) {
        className = className.toLowerCase(); // normalize
        Map<String, String> map = ALL_MAPPINGS.get(className);
        if (map == null) {
            System.out.println("No mapping found for class: " + className);
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("No mapping found for class: "+ className)));
            return;
        }
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("wynnextras/trees");
            Path inPath = configDir.resolve(fileName.endsWith(".json") ? fileName : fileName + ".json");
            if (!Files.exists(inPath)) {
                System.out.println("Input file not found: " + inPath);
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Input file not found: " + inPath)));
                return;
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject orig = gson.fromJson(Files.newBufferedReader(inPath), JsonObject.class);
            JsonArray input = orig.getAsJsonArray("input");

            JsonArray mappedArr = new JsonArray();
            for (JsonElement el : input) {
                String id = el.getAsString();
                String name = map.getOrDefault(id, id);
                mappedArr.add(name);
            }
            JsonObject out = orig.deepCopy();
            out.add("input", mappedArr);

            String outFileSuffix = fileName.endsWith(".json") ? fileName.replace(".json", ".json") : fileName + ".json";
            Path outPath = configDir.resolve(outFileSuffix);
            try (FileWriter writer = new FileWriter(outPath.toFile())) {
                writer.write(gson.toJson(out));
            }
            System.out.println("Converted file written: " + outPath.getFileName());
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Converted file written: "+ outPath.getFileName())));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error during ability file conversion.");
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Error during ability file conversion.")));
        }
    }
}
