package julianh06.wynnextras.features.misc;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.sound.ModSounds;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class SourceOfThruth extends WEScreen {
    SourceOfTruthWidget sourceOfTruthWidget = null;
    public static List<SoundEvent> sounds = new ArrayList<>();
    static String answer = "";

    protected SourceOfThruth() {
        super(Text.of("Source of truth"));
        answer = "";
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(sourceOfTruthWidget == null) {
            sourceOfTruthWidget = new SourceOfTruthWidget(0, 0, 0, 0);
            addRootWidget(sourceOfTruthWidget);
        }

        sourceOfTruthWidget.setBounds((int) ((width * ui.getScaleFactor()) / 2) - 350, (int) ((height * ui.getScaleFactor()) / 2) - 350, 700, 700);
    }

    private static class SourceOfTruthWidget extends Widget {
        public static Identifier shellTexture = Identifier.of("wynnextras", "textures/gui/misc/sourceoftruth.png");
        PullThingWidget pullThingWidget = null;

        public SourceOfTruthWidget(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(pullThingWidget == null) {
                pullThingWidget = new PullThingWidget(x + 475, y + 210);
                addChild(pullThingWidget);
            }

            pullThingWidget.setBounds(x + 475, y + 210, (int) (20 * ui.getScaleFactor()), (int) (20 * ui.getScaleFactor()));

            ui.drawImage(shellTexture, x, y, width, height);
            ui.drawCenteredText("Pull the cord to receive an answer to all your questions.", x + width / 2f, y);

            if(answer.isEmpty()) return;

            ui.drawCenteredText(answer, x + width / 2f, y + height);
        }

        private static class PullThingWidget extends Widget {
            static Identifier pullThingTexture = Identifier.of("wynnextras", "textures/gui/misc/pullthisforawisemessage.png");

            boolean holding = false;
            int posX;
            int posY;

            public PullThingWidget(int x, int y) {
                super(0, 0, 0, 0);
                posX = x;
                posY = y;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                if(holding) {
                    posX = (int) (mouseX * ui.getScaleFactor() - width / 2f);
                    posY = (int) (mouseY * ui.getScaleFactor() - height / 2f);
                } else {
                    float snapValue = 5f;

                    float speed = 0.5f;

                    float diffX = (x - posX);
                    if(Math.abs(diffX) < snapValue) posX = x;
                    else posX += (int) (diffX * speed * tickDelta);

                    float diffY = (y - posY);
                    if(Math.abs(diffY) < snapValue) posY = y;
                    else posY += (int) (diffY * speed * tickDelta);
                }

                ui.drawLine(x, y + height / 2f + 15, posX, posY + height / 2f + 15, 4, CustomColor.fromHexString("FFFFFF"));

                ui.drawImage(pullThingTexture, posX, posY, width, height);
            }

            @Override
            protected boolean onClick(int button) {
                holding = true;
                return true;
            }

            @Override
            public boolean mouseReleased(double mx, double my, int button) {
                holding = false;

                double distance = Math.hypot((x - posX), (y - posY));

                if(distance > 300) {
                    SoundEvent sound = getRandomSound();
                    answer = getAnswerString(sound);

                    if(MinecraftClient.getInstance().player != null) {
                        int rnd = (int) (Math.random() * 5);
                        if (MinecraftClient.getInstance().player.getName().getString().equals("D4MIT") && sound.equals(ModSounds.YES) && rnd == 0) {
                            answer = "YES D4MIT YOU SHOULD GAMBLE A RESONANCE!!!!!!";
                            sound = ModSounds.SKELETON;
                        }
                    }

                    if(sound == null) {
                        return true;
                    }

                    if(MinecraftClient.getInstance().player == null) return true;
                    MinecraftClient.getInstance().player.playSound(sound, 10f, 1f);
                } else if(distance != 0) {
                    answer = "You didn't pull hard enough.";
                }

                return true;
            }

            private SoundEvent getRandomSound() {
                int i = (int) (Math.random() * sounds.size());
                return sounds.get(i);
            }

            private String getAnswerString(SoundEvent event) {
                if(event == null) return "";

                if(event.equals(ModSounds.YES)) return "Yes.";
                if(event.equals(ModSounds.NO)) return "No.";
                if(event.equals(ModSounds.NOTHING)) return "Nothing.";
                if(event.equals(ModSounds.IDTS)) return "I don't think so.";
                if(event.equals(ModSounds.ASKAGAIN)) return "Ask again.";
                if(event.equals(ModSounds.NEITHER)) return "Neither.";

                return "";
            }
        }
    }
}
