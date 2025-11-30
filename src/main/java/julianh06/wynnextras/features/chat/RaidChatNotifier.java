package julianh06.wynnextras.features.chat;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.core.components.Models;
import julianh06.wynnextras.core.WynnExtras;
import net.minecraft.text.Text;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaidChatNotifier {
    private static final List<RaidMessageDetector> detectors = Arrays.asList(
        new SlimeGatheringDetector(),
        new BindingSealDetector(),
        new LightGatheringDetector(),
        new WatchPhaseDetector(),
        new ShadowlingDetector(),

        new SingleOccurrenceDetector(
            "is preparing to descend! [1/2]",
            "§bDescend 1/2 §c",
                "descend1"
        ),
        new SingleOccurrenceDetector(
            "is preparing to descend! [2/2]",
            "§bDescend 2/2 §c",
                "descend2"
        ),
        new SingleOccurrenceDetector(
            "Upper Level must kill the Slime Chomper",
            "§bSlime Chomper Spawned §c",
                "slimemini"
        ),
        new SingleOccurrenceDetector(
            "players on the Upper Level must kill the Carnivorous",
            "§bCarnivore spawned §c",
                "carnimini"
        ),
        new SingleOccurrenceDetector(
            "players on the Upper Level must kill the Invasive",
            "§bTarantula spawned §c",
                "taramini"
        ),
        new SingleOccurrenceDetector(
            "players on the Upper Level must kill the Unfurling",
            "§bHorsefly spawned §c",
                "horseflymini"
        ),
        new SingleOccurrenceDetector(
            "The Void Holes have begun to destabi",
            "§b[4/5] Void Matters §c",
                "voidgathered"
        ),
        new SingleOccurrenceDetector(
                "The Altar has opened to the void, you may leave through it.",
                "§bVoid Room Done §c",
                "treedone"
        ),
        new SingleOccurrenceDetector(
                "All the Void Rifts have been destroyed! A path",
                "§bBerry Room Done §c",
                "berryroom"
        ),
        new SingleOccurrenceDetector(
                "has taken the Berserker Berry!",
                "§bBerry Room Done §c",
                "berrytaken"
        ),
        new SingleOccurrenceDetector(
                "A Void Pedestal has been activated! [1/2]",
                "§bVoid Pedestal Activated [1/2] §c",
                "voidpedestal1"
        ),
        new SingleOccurrenceDetector(
                "You have unblocked the voidhole out!",
                "§Void Room done §c",
                "voidholeroompb"
        ),
        new SingleOccurrenceDetector(
                "The Giant Void Hole has opened! Use it to escape!",
                "§Voidgather Room done §c",
                "voidgatherroompb"
        ),

        new MultiOccurrenceDetector(
            "A new platform has appeared on the Lower Area!",
            "§bLower Mini spawned §c",
                "lowermini"
        ),
        new MultiOccurrenceDetector(
                "A Bulb Keeper has spawned!",
                "§bBulb Keeper spawned §c",
                "bulbspawned"
        ),
        new MultiOccurrenceDetector(
                "A Red Bulb has been captured!",
                "§bBulb captured §c",
                "bulbcaptured"
        ),
        new MultiOccurrenceDetector(
            "The void holes inside the tree are open!",
            "§Tree Opened §c",
            "openedtree"
        ),
        new MultiOccurrenceDetector(
                "[1 Void Matter]",
                "§[1 Void Matter] §c",
                "voidmattergathered"
        ),
        new MultiOccurrenceDetector(
            "3/3 Clouds Purified",
            "§bPurified 3/3 clouds §c",
                "clouds"
        ),
        new MultiOccurrenceDetector(
            "The Team has reached the Checkpoint!",
            "§bReached Checkpoint §c",
                "mazecheckpoint"
        ),
        new MultiOccurrenceDetector(
            "100% Rock Destroyed",
            "§bRock destroyed §c",
                "rockdestroyed"
        ),
        new MultiOccurrenceDetector(
            "[+1 Slimey Goo]",
            "§fGot 1 Slimey Goo §c",
                "slimegathered"
        ),
        new MultiOccurrenceDetector(
            "[+2 Slimey Goo]",
            "§fGot 2 Slimey Goo §c",
                "2slimesgathered"
        ),
        new MultiOccurrenceDetector(
            "+1 [Isoptera Heart]",
            "§fGot heart §c",
                "heart"
        ),
        new MultiOccurrenceDetector(
            "has entered the tree",
            "§bEntered the Tree §c",
                "treeenter"
        )
    );



    private static final WynnExtrasConfig config = SimpleConfig.getInstance(WynnExtrasConfig.class);

    private static void savePB(String key, long time) {
        Long old = config.raidPBs.get(key);

        if (old == null || time < old) {
            config.raidPBs.put(key, time);
            SimpleConfig.save(WynnExtrasConfig.class);
        }
    }

    private static Long getPB(String key) {
        return config.raidPBs.get(key);
    }

    private static final long MESSAGE_DELAY_MS = 250;
    private static long lastMessageTime = 0;

    public static void handleMessage(String rawMsg) {
        if (!config.toggleRaidTimestamps) return;



        long currentTime = (Models.Raid.getCurrentRaid() != null && Models.Raid.getCurrentRaid().getCurrentRoom() != null)
                          ? Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime()
                          : 0;

        String msg = stripColorCodes(rawMsg);

        for (RaidMessageDetector detector : detectors) {
            if (detector.matches(msg)) {
                String timestamp = (Models.Raid.getCurrentRaid() != null && Models.Raid.getCurrentRaid().getCurrentRoom() != null)
                    ? formatTime(currentTime)
                    : "??:??.???";

                String progress = detector.extractProgress(msg);
                String finalMsg = detector.getFormattedMessage(progress, timestamp);

                new Thread(() -> {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (!finalMsg.isEmpty()) {
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of(finalMsg)));
                    }
                }).start();

                return;
            }
        }
    }




    private static String getCurrentRoomTimestamp() {
        if (Models.Raid.getCurrentRaid() == null || Models.Raid.getCurrentRaid().getCurrentRoom() == null)
            return "??:??.???";
        return formatTime(Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime());
    }

    private static String formatTime(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        long ms = millis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, ms);
    }

    private static String stripColorCodes(String input) {
        return input.replaceAll("§[0-9a-fk-or]", "");
    }

    private interface RaidMessageDetector {
        boolean matches(String msg);

        String extractProgress(String msg);

        String getFormattedMessage(String progress, String timestamp);
    }


         public static final List<Pattern> BLOCKED_PATTERNS = Arrays.asList(
        Pattern.compile("is preparing to descend! \\[1/2", Pattern.CASE_INSENSITIVE),
        Pattern.compile("is preparing to descend! \\[2/2", Pattern.CASE_INSENSITIVE),
        Pattern.compile("upper level must kill the slime chomper", Pattern.CASE_INSENSITIVE),
        Pattern.compile("players on the upper level must kill the carnivorous", Pattern.CASE_INSENSITIVE),
        Pattern.compile("players on the upper level must kill the invasive", Pattern.CASE_INSENSITIVE),
        Pattern.compile("players on the upper level must kill the unfurling", Pattern.CASE_INSENSITIVE),
        Pattern.compile("the void holes have begun to destabi", Pattern.CASE_INSENSITIVE),
        Pattern.compile("a new platform has appeared on the lower area!", Pattern.CASE_INSENSITIVE),
        Pattern.compile("3/3 clouds purified", Pattern.CASE_INSENSITIVE),
        Pattern.compile("the team has reached the checkpoint!", Pattern.CASE_INSENSITIVE),
        Pattern.compile("100% rock destroyed", Pattern.CASE_INSENSITIVE),
        Pattern.compile("1 slimey goo", Pattern.CASE_INSENSITIVE),
        Pattern.compile("2 slimey goo", Pattern.CASE_INSENSITIVE),
        Pattern.compile("1 \\[isoptera heart", Pattern.CASE_INSENSITIVE),
        Pattern.compile("All the Void Rifts have been destroyed! A path", Pattern.CASE_INSENSITIVE),
        Pattern.compile("The void holes inside the tree are open!", Pattern.CASE_INSENSITIVE),
        Pattern.compile("The Altar has opened to the void, you may leave through it.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("A Red Bulb has been captured!", Pattern.CASE_INSENSITIVE),
        Pattern.compile("A Bulb Keeper has spawned!", Pattern.CASE_INSENSITIVE),
        Pattern.compile("The Giant Void Hole has opened! Use it to escape!", Pattern.CASE_INSENSITIVE),
        Pattern.compile("A Void Pedestal has been activated! [1/2]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("You have unblocked the voidhole out!", Pattern.CASE_INSENSITIVE),
        Pattern.compile("[1 Void Matter]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("has entered the tree", Pattern.CASE_INSENSITIVE),


        Pattern.compile("goo to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("binding seal! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("light crystals to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("has been killed! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("the obelisks have appeared; they must be", Pattern.CASE_INSENSITIVE)
    );



    private static class SlimeGatheringDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Goo to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        static final String PB_PREFIX = "slime";
        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            Matcher matcher = PATTERN.matcher(msg);
            return matcher.find() ? matcher.group(1) : null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            if (Models.Raid.getCurrentRaid() == null || Models.Raid.getCurrentRaid().getCurrentRoom() == null) {
                return "§aAdded Slime " + progress + " §c@ " + timestamp;
            }


            long elapsed = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();


            String key = PB_PREFIX + "_" + progress;

            Long pb = getPB(key);

            String output = "§aAdded Slime " + progress +
                    " §c@ " + formatTime(elapsed);


            if (pb == null || elapsed < pb) {
                savePB(key, elapsed);

                if (pb != null) {
                    output += " §e[New PB! Old: " + formatTime(pb) + "]";
                } else {
                    output += " §e[First PB]";
                }
            }
            else {
                output += " §7[PB: " + formatTime(pb) + "]";
            }

            return output;
        }
    }

    private static class BindingSealDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Binding Seal! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        static final String PB_PREFIX = "seal";

        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            Matcher matcher = PATTERN.matcher(msg);
            return matcher.find() ? matcher.group(1) : null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            long currentMillis = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();

            String key = PB_PREFIX + "_" + progress;

            Long pb = getPB(key);

            String output = "§bCompleted Seal " + progress + " §c@ " + timestamp;

            if (pb == null || currentMillis  < pb) {
                savePB(key, currentMillis);

                if (pb != null) {
                    output += " §e[New PB! Old: " + formatTime(pb) + "]";
                } else {
                    output += " §e[First PB]";
                }
            }
            else {
                output += " §7[PB: " + formatTime(pb) + "]";
            }
            return output;
        }
    }

    private static class LightGatheringDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Light Crystals to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        static final String PB_PREFIX = "lightgathering";

        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            Matcher matcher = PATTERN.matcher(msg);
            return matcher.find() ? matcher.group(1) : null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            long currentMillis = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
            String key = PB_PREFIX + "_" + progress;

            Long pb = getPB(key);

            String output = "§bAdded light " + progress + " §c@ " + timestamp;

            if (pb == null || currentMillis  < pb) {
                savePB(key, currentMillis );

                if (pb != null) {
                    output += " §e[New PB! Old: " + formatTime(pb) + "]";
                } else {
                    output += " §e[First PB]";
                }
            }
            else {
                output += " §7[PB: " + formatTime(pb) + "]";
            }
            return output;
        }
    }

    private static class ShadowlingDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("has been killed! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        static final String PB_PREFIX = "shadowling";

        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            Matcher matcher = PATTERN.matcher(msg);
            return matcher.find() ? matcher.group(1) : null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            long currentMillis = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();

            String key = PB_PREFIX + "_" + progress;

            Long pb = getPB(key);

            String output = "§bKilled Shadowling " + progress + " §c@ " + timestamp;

            if (pb == null || currentMillis  < pb) {
                savePB(key, currentMillis );

                if (pb != null) {
                    output += " §e[New PB! Old: " + formatTime(pb) + "]";
                } else {
                    output += " §e[First PB]";
                }
            }
            else {
                output += " §7[PB: " + formatTime(pb) + "]";
            }
            return output;
        }
    }



    private static class StaticMessageDetector implements RaidMessageDetector {
        private final Pattern pattern;
        private final String formattedMessage;

        public StaticMessageDetector(String regex, String formattedMessage) {
             this.pattern = Pattern.compile(Pattern.quote(regex), Pattern.CASE_INSENSITIVE);
             this.formattedMessage = formattedMessage;
        }

        @Override
        public boolean matches(String msg) {
            return pattern.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            return null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            return formattedMessage + timestamp;
        }
    }

    private static class WatchPhaseDetector implements RaidMessageDetector {

        private long lastWatchPhaseTime = -1; // Zeit des letzten Watchphase-Starts
        private static final Pattern PATTERN = Pattern.compile(
                "The Obelisks have appeared; they must be", Pattern.CASE_INSENSITIVE);

        public void resetForNewRaid() {
            lastWatchPhaseTime = -1; // nur die Zeit zurücksetzen
        }

        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            return null; // keine speziellen Progress-Daten
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            if (Models.Raid.getCurrentRaid() == null || Models.Raid.getCurrentRaid().getCurrentRoom() == null) {
                return "§bStarted Watchphase (no raid data)";
            }

            long currentTime = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
            String message;

            if (lastWatchPhaseTime == -1) {
                // Erste Watchphase im Raum
                message = "§bFirst Watchphase started §c@ " + timestamp;

                Long pb = config.raidPBs.get("watch_phase_first");
                if (pb == null || currentTime < pb) {
                    config.raidPBs.put("watch_phase_first", currentTime);
                    SimpleConfig.save(WynnExtrasConfig.class);

                    message += (pb == null ? " §e[First PB]" : " §e[New PB! Old: " + formatTime(pb) + "]");
                } else {
                    message += " §7[PB: " + formatTime(pb) + "]";
                }

            } else {
                // alle weiteren Watchphasen
                long duration = currentTime - lastWatchPhaseTime;
                message = "§bWatchphase started after §c" + formatTime(duration) + " §7(@" + timestamp + ")";

                Long pb = config.raidPBs.get("watch_phase_duration");
                if (pb == null || duration < pb) {
                    config.raidPBs.put("watch_phase_duration", duration);
                    SimpleConfig.save(WynnExtrasConfig.class);

                    message += (pb == null ? " §e[First PB]" : " §e[New PB! Old: " + formatTime(pb) + "]");
                } else {
                    message += " §7[PB: " + formatTime(pb) + "]";
                }
            }

            lastWatchPhaseTime = currentTime;
            return message;
        }
    }


    private static class SingleOccurrenceDetector implements RaidMessageDetector {
        private final Pattern pattern;
        private final String formattedMessage;
        private final String pbKey;

        public SingleOccurrenceDetector(String regex, String formattedMessage, String pbKey) {
            this.pattern = Pattern.compile(Pattern.quote(regex), Pattern.CASE_INSENSITIVE);
            this.formattedMessage = formattedMessage;
            this.pbKey = pbKey;
        }

        @Override
        public boolean matches(String msg) {
            return pattern.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            return null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            String message = formattedMessage + timestamp;


            if (Models.Raid.getCurrentRaid() != null && Models.Raid.getCurrentRaid().getCurrentRoom() != null) {
                long currentTime = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
                Long pb = getPB(pbKey);

                if (pb == null || currentTime < pb) {
                    savePB(pbKey, currentTime);
                    message += (pb == null ? " §e[First PB]" : " §e[New PB! Old: " + formatTime(pb) + "]");
                } else {
                    message += " §7[PB: " + formatTime(pb) + "]";
                }
            }
            return message;
        }
    }



    private static class MultiOccurrenceDetector implements RaidMessageDetector {
         private final Pattern pattern;
         private final String baseMessage;
         private final String pbKeyPrefix;

         private int occurrenceCount = 0;

         private long lastTriggerTime = -1;

         public MultiOccurrenceDetector(String regex, String baseMessage, String pbKeyPrefix) {
             this.pattern = Pattern.compile(Pattern.quote(regex), Pattern.CASE_INSENSITIVE);
             this.baseMessage = baseMessage;
             this.pbKeyPrefix = pbKeyPrefix;
         }

         @Override
         public boolean matches(String msg) {
             return pattern.matcher(msg).find();
         }

         @Override
         public String extractProgress(String msg) {
             if (Models.Raid.getCurrentRaid() == null || Models.Raid.getCurrentRaid().getCurrentRoom() == null) {
                 return null;
             }

             long now = System.currentTimeMillis();
             if (lastTriggerTime != -1 && (now - lastTriggerTime) < 555) {
                 return null; // anti-spam
             }
             lastTriggerTime = now;

             occurrenceCount++;
             return "[" + occurrenceCount + "]";
         }

         @Override
         public String getFormattedMessage(String progress, String timestamp) {
             if (progress == null) {
                 progress = "[" + occurrenceCount + "]";
             }
             if (timestamp == null) {
                 timestamp = "??:??";
             }

             String key = pbKeyPrefix + "_" + occurrenceCount;

             String msg;

             if (Models.Raid.getCurrentRaid() != null
                     && Models.Raid.getCurrentRaid().getCurrentRoom() != null) {

                 long currentTime = Models.Raid.getCurrentRaid()
                         .getCurrentRoom().getRoomTotalTime();

                 Long pb = getPB(key);

                 if (pb == null || currentTime < pb) {
                     savePB(key, currentTime);

                     if (pb == null) {
                         msg = baseMessage + progress + " §c@ " + timestamp +
                                 " §e[First PB]";
                     } else {
                         msg = baseMessage + progress + " §c@ " + timestamp +
                                 " §e[New PB! Old: " + formatTime(pb) + "]";
                     }
                 } else {
                     msg = baseMessage + progress + " §c@ " + timestamp +
                             " §7[PB: " + formatTime(pb) + "]";
                 }

             } else {
                 msg = baseMessage + progress + " §c@ " + timestamp;
             }

             return msg;
         }
     }


public static void resetCounters() {
        for (RaidMessageDetector detector : detectors) {
            if (detector instanceof MultiOccurrenceDetector m) {
                m.occurrenceCount = 0;
            }
            else if (detector instanceof WatchPhaseDetector w) {
                        w.resetForNewRaid();

          }
      }
  }
}