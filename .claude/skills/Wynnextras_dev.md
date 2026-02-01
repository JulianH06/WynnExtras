# WynnExtras Development Skill

Develop the WynnExtras Minecraft mod - add features, test, debug, and deploy.

## Project Overview

WynnExtras is a Fabric mod for Minecraft 1.21.11 that enhances the Wynncraft experience.

- **Location:** `C:/Users/tim/Wynnextras_11`
- **Mod Version:** 0.13.x
- **Wynntils Dependency:** 4.0.0-beta.2
- **Java Version:** 21

## Build & Deploy Commands

### Build Only
```bash
cd C:/Users/tim/Wynnextras_11 && ./gradlew.bat build
```

### Build and Deploy (copies JAR to mods folder)
```bash
cd C:/Users/tim/Wynnextras_11 && ./gradlew.bat build && rm -f "/c/Users/tim/AppData/Roaming/ModrinthApp/profiles/newest version/mods/"wynnextras-*.jar && cp $(ls build/libs/wynnextras-*.jar | grep -v -E '(-all|-sources)\.jar$') "/c/Users/tim/AppData/Roaming/ModrinthApp/profiles/newest version/mods/"
```

## Log & Crash Report Locations

### Game Logs
```
C:/Users/tim/AppData/Roaming/ModrinthApp/profiles/newest version/logs/latest.log
```

### Crash Reports
```
C:/Users/tim/AppData/Roaming/ModrinthApp/profiles/newest version/crash-reports/
```

## Adding a New Feature Module

1. Create package: `src/main/java/julianh06/wynnextras/features/yourfeature/`

2. Create main feature class with `@WEModule` annotation:
```java
package julianh06.wynnextras.features.yourfeature;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;

@WEModule
public class YourFeature {
    @SubscribeEvent
    public void onTick(TickEvent event) {
        // Feature logic here
    }
}
```

3. The module is auto-discovered via the `@WEModule` annotation - no registration needed.

## Adding Config Options

Edit `src/main/java/julianh06/wynnextras/config/WynnExtrasConfig.java`:

```java
public boolean yourOption = false;
public int yourValue = 100;
```

Access via: `WynnExtrasConfig.get().yourOption`

## Adding a Mixin

1. Create mixin class in `src/main/java/julianh06/wynnextras/mixin/`

2. Add to `src/main/resources/wynnextras.mixins.json` in the "client" array:
```json
{
  "client": [
    "YourNewMixin"
  ]
}
```

## Adding a Command

Create in `src/main/java/julianh06/wynnextras/core/command/commands/`:

```java
public class YourCommand extends Command {
    public YourCommand() {
        super("yourcommand", "Description");
    }

    @Override
    public void execute(String[] args) {
        // Command logic
    }
}
```

## Creating a Custom Screen

Always use `WEScreen` as the base class:

```java
public class YourScreen extends WEScreen {
    public YourScreen() {
        super(Text.literal("Screen Title"));
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        drawText("Hello", 10, 10);
    }
}

// Open with:
WEScreen.open(() -> new YourScreen());
```

## Sending Chat Messages

Always use the WynnExtras prefix:

```java
McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Your message"));
```

## Available Events

Subscribe with `@SubscribeEvent`:
- `TickEvent` - Every game tick
- `RenderEvent` - Screen rendering
- `RenderWorldEvent` - World rendering
- `ChatEvent` - Chat messages
- `KeyInputEvent` - Key presses
- `WorldChangeEvent` - World/dimension change
- `DisconnectEvent` - Server disconnect
- `InventoryKeyPressEvent` - Keys in inventory
- `RaidEndedEvent` - Raid completion

## Common Wynntils APIs

```java
// Check if on Wynncraft
if (!Models.WorldState.onWorld()) return;

// Get player safely
LocalPlayer player = McUtils.player();
if (player == null) return;

// Get current raid
RaidInfo raid = Models.Raid.getCurrentRaid();

// Get player class
ClassType classType = Models.Character.getClassType();

// Get container
Container container = Models.Container.getCurrentContainer();

// Send command
Managers.Command.queueCommand("party list");
```

## Utilities

### TickScheduler
```java
TickScheduler.schedule(20, () -> {
    // Runs after 20 ticks (1 second)
});
```

### Logging
```java
WELogger.info("Message");
WELogger.info("DEBUG: value=" + value);
```

## Key Source Directories

| Directory | Purpose |
|-----------|---------|
| `features/` | Feature modules (13 total) |
| `core/` | Mod initialization, commands, event bus |
| `config/` | Configuration classes |
| `utils/` | Utility classes |
| `mixin/` | Mixin classes |
| `event/` | Custom event definitions |

## Existing Features Reference

| Feature | Description |
|---------|-------------|
| abilitytree | Ability tree viewer/importer |
| achievements | Achievement tracking |
| aspects | Aspect tracking system |
| bankoverlay | Enhanced bank UI with search |
| chat | Chat channels + notifications |
| crafting | Crafting recipe helper |
| guildviewer | Guild info viewer `/gv` |
| inventory | Bank data, weight display |
| misc | FastRequeue, PlayerHider, Provoke, Totem |
| profileviewer | Player profiles `/pv` |
| raid | Raid tracking + loot |
| render | Player render filtering |
| waypoints | Waypoint system |

## Development Workflow

1. **Make changes** to the code
2. **Build and deploy** using the build command
3. **Launch Minecraft** and test on Wynncraft
4. **Check logs** if issues occur
5. **Check crash reports** if game crashes
6. **Debug** with `WELogger.info("DEBUG: ...")` statements
7. **Iterate** until working correctly

## Common Issues

### Crash on Startup
- Check mixin targets - method signatures may have changed
- Check `wynnextras.mixins.json` for typos
- Read the crash report for stack trace

### Feature Not Loading
- Ensure `@WEModule` annotation is present
- Check for exceptions in logs during module discovery
- Ensure no-arg constructor exists

### Null Pointer Exceptions
- Always null-check `McUtils.player()`
- Always check `Models.WorldState.onWorld()`
- Null-check container contents
