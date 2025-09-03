package julianh06.wynnextras.features.chat;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.core.components.Models;
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

        new SingleOccurrenceDetector("is preparing to descend! [1/2]", "§2[WynnExtras] §bDescend 1/2 §c@ "),
        new SingleOccurrenceDetector("is preparing to descend! [2/2]", "§2[WynnExtras] §bDescend 2/2 §c@ "),
        new SingleOccurrenceDetector("Upper Level must kill the Slime Chomper", "§2[WynnExtras] §bSlime Chomper Spawned §c@ "),
        new SingleOccurrenceDetector("players on the Upper Level must kill the Carnivorous", "§2[WynnExtras] §bCarnivor spawned §c@ "),
        new SingleOccurrenceDetector("players on the Upper Level must kill the Invasive", "§2[WynnExtras] §bTarantula spawned §c@ "),
        new SingleOccurrenceDetector("players on the Upper Level must kill the Unfurling", "§2[WynnExtras] §bHorsefly spawned §c@ "),
        new SingleOccurrenceDetector( "The Void Holes have begun to destabi", "§2[WynnExtras] §b[4/5] Void Matters §c@ "),

        new MultiOccurrenceDetector("A new platform has appeared on the Lower Area!", "§2[WynnExtras] §bLower Mini spawned §c@ "),
        new MultiOccurrenceDetector("3/3 Clouds Purified", "§2[WynnExtras] §bPurified 3/3 clouds "),
        new MultiOccurrenceDetector("The Team has reached the Checkpoint!", "§2[WynnExtras] §bReached Checkpoint §c@ "),
        new MultiOccurrenceDetector("100% Rock Destroyed", "§2[WynnExtras] §bRock destroyed §c@ "),
        new MultiOccurrenceDetector("[+1 Slimey Goo]", "§2[WynnExtras] §fGot 1 Slimey Goo §c@ "),
        new MultiOccurrenceDetector("[+2 Slimey Goo]", "§2[WynnExtras] §fGot 2 Slimey Goo §c@ "),
        new MultiOccurrenceDetector("+1 [Isoptera Heart]","§2[WynnExtras] §fGot heart §c@ "),
        new MultiOccurrenceDetector("has entered the tree","§2[WynnExtras] §bEntered the Tree §c@ ")
    );
    private static final WynnExtrasConfig config = SimpleConfig.getInstance(WynnExtrasConfig.class);

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
                        McUtils.sendMessageToClient(Text.literal(finalMsg));
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
        Pattern.compile("has entered the tree", Pattern.CASE_INSENSITIVE),


        Pattern.compile("goo to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("binding seal! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("light crystals to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("has been killed! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("the obelisks have appeared; they must be", Pattern.CASE_INSENSITIVE)
    );



    private static class SlimeGatheringDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Goo to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        private final Map<String, Long> sessionPBs = new HashMap<>();

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
                return "§2[WynnExtras] §aAdded Slime " + progress + " §c@ " + timestamp;
            }

            // aktuelle Zeit seit Raumstart
            long elapsed = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();

            // bisherige PB für diesen Split
            Long pb = sessionPBs.get(progress);

            StringBuilder output = new StringBuilder("§2[WynnExtras] §aAdded Slime " + progress +
                    " §c@ " + formatTime(elapsed));

            if (pb == null || elapsed < pb) {
                sessionPBs.put(progress, elapsed);
                if (pb != null) {
                    output.append(" §e[New Session PB! Old: ").append(formatTime(pb)).append("]");
                } else {
                    output.append(" §e[First PB]");
                }
            } else {
                output.append(" §7[Session PB: ").append(formatTime(pb)).append("]");
            }

            return output.toString();
        }
    }

    private static class BindingSealDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Binding Seal! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        private final Map<String, Long> sessionPBs = new HashMap<>();

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
            Long pb = sessionPBs.get(progress);
            StringBuilder msg = new StringBuilder("§2[WynnExtras] §bCompleted Seal " + progress + " §c@ " + timestamp);

            if (pb == null || currentMillis < pb) {
                sessionPBs.put(progress, currentMillis);
                if (pb != null) {
                    msg.append(" §e[New Session PB! Old: ").append(formatTime(pb)).append("]");
                } else {
                    msg.append(" §e[First PB]");
                }
            } else {
                msg.append(" §7[Session PB: ").append(formatTime(pb)).append("]");
            }
            return msg.toString();
        }
    }

    private static class LightGatheringDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Light Crystals to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        private final Map<String, Long> sessionPBs = new HashMap<>();

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
            Long pb = sessionPBs.get(progress);
            StringBuilder msg = new StringBuilder("§2[WynnExtras] §bAdded light " + progress + " §c@ " + timestamp);

            if (pb == null || currentMillis < pb) {
                sessionPBs.put(progress, currentMillis);
                if (pb != null) {
                    msg.append(" §e[New Session PB! Old: ").append(formatTime(pb)).append("]");
                } else {
                    msg.append(" §e[First PB]");
                }
            } else {
                msg.append(" §7[Session PB: ").append(formatTime(pb)).append("]");
            }
            return msg.toString();
        }
    }

    private static class ShadowlingDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("has been killed! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        private final Map<String, Long> sessionPBs = new HashMap<>();

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
            Long pb = sessionPBs.get(progress);
            StringBuilder msg = new StringBuilder("§2[WynnExtras] §bKilled Shadowling " + progress + " §c@ " + timestamp);

            if (pb == null || currentMillis < pb) {
                sessionPBs.put(progress, currentMillis);
                if (pb != null) {
                    msg.append(" §e[New Session PB! Old: ").append(formatTime(pb)).append("]");
                } else {
                    msg.append(" §e[First PB]");
                }
            } else {
                msg.append(" §7[Session PB: ").append(formatTime(pb)).append("]");
            }
            return msg.toString();
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

        private long lastWatchPhaseTime = -1;
        private long firstWatchPhasePB = -1;
        private long watchPhasePB = -1;
        public void resetForNewRaid() {
            lastWatchPhaseTime = -1; // nur die Zeit zurücksetzen
        }

        private static final Pattern PATTERN = Pattern.compile(
            "The Obelisks have appeared; they must be", Pattern.CASE_INSENSITIVE);

        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            return null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            if (Models.Raid.getCurrentRaid() == null || Models.Raid.getCurrentRaid().getCurrentRoom() == null) {
                return "§2[WynnExtras] §bStarted Watchphase (no raid data)";
            }

            long currentTime = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
            String message;

            if (lastWatchPhaseTime == -1) {
                message = "§2[WynnExtras] §bFirst Watchphase started §c@ " + timestamp;

                if (firstWatchPhasePB == -1 || currentTime < firstWatchPhasePB) {
                    firstWatchPhasePB = currentTime;
                    message += " §e[New Session PB!]";
                } else {
                    message += " §7[Session PB: " + formatTime(firstWatchPhasePB) + "]";
                }
            } else {
                long duration = currentTime - lastWatchPhaseTime;
                long oldPB = watchPhasePB;
                if (watchPhasePB == -1 || duration < watchPhasePB) {
                    watchPhasePB = duration;
                    message = "§2[WynnExtras] §bWatchphase took §c" + formatTime(duration) + " §7(" + timestamp + ") §e[New Session PB!]";
                    if (oldPB != -1) {
                        message += " §7[Old: " + formatTime(oldPB) + "]";
                    }
                } else {
                    message = "§2[WynnExtras] §bWatchphase took §c" + formatTime(duration) + " §7(" + timestamp + ") §7[Session PB: " + formatTime(watchPhasePB) + "]";
                }
            }

            lastWatchPhaseTime = currentTime;
            return message;
        }
    }

    private static class SingleOccurrenceDetector implements RaidMessageDetector {
        private final Pattern pattern;
        private final String formattedMessage;
        private long sessionPB = -1;

        public SingleOccurrenceDetector(String regex, String formattedMessage) {
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
           String message = formattedMessage + timestamp;

           if (Models.Raid.getCurrentRaid() != null && Models.Raid.getCurrentRaid().getCurrentRoom() != null) {
               long currentTime = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
               if (sessionPB == -1 || currentTime < sessionPB) {
                   sessionPB = currentTime;
                   message += " §e[New Session PB!]";
               } else {
                   message += " §7[Session PB: " + formatTime(sessionPB) + "]";
               }
           }
           return message;
       }
    }




     private static class MultiOccurrenceDetector implements RaidMessageDetector {
         private final Pattern pattern;
         private final String baseMessage;
         private int occurrenceCount = 0;
         private final Map<Integer, Long> pbs = new HashMap<>();

         private long lastTriggerTime = -1;

         public MultiOccurrenceDetector(String regex, String baseMessage) {
             this.pattern = Pattern.compile(Pattern.quote(regex), Pattern.CASE_INSENSITIVE);
             this.baseMessage = baseMessage;
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
             if (lastTriggerTime != -1 && (now - lastTriggerTime) < 3000) {
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

             String msg;

             if (Models.Raid.getCurrentRaid() != null && Models.Raid.getCurrentRaid().getCurrentRoom() != null) {
                 long currentTime = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
                 Long pb = pbs.get(occurrenceCount);

                 if (pb == null || currentTime < pb) {
                     pbs.put(occurrenceCount, currentTime);
                     msg = baseMessage + progress + " §c@ " + timestamp + " §e[New Session PB!]";
                 } else {
                     msg = baseMessage + progress + " §c@ " + timestamp +
                           " §7[Session PB: " + formatTime(pb) + "]";
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