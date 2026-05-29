package com.example.examplemod.gui;

import com.example.examplemod.Game_Saver;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Game_Saver.MODID, value = Dist.CLIENT)
public class GameOverOverlay {

    private static long startFadeTime = 0;
    private static final long FadeContinue = 3000;
    private static int maxDuration = -1;
    private static final ResourceLocation GAME_OVER_PNG = ResourceLocation.fromNamespaceAndPath("game_saver", "textures/gui/game_over.png");
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("game_saver", "textures/gui/background.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Pre event) {

        ResourceLocation currentLayer = event.getName();

        // The condition under which hotbar bar, experience bar, hunger bar and health bar are removed from the death screen
        if (!currentLayer.equals(VanillaGuiLayers.HOTBAR) &&
                !currentLayer.equals(VanillaGuiLayers.PLAYER_HEALTH) &&
                !currentLayer.equals(VanillaGuiLayers.FOOD_LEVEL) &&
                !currentLayer.equals(VanillaGuiLayers.EXPERIENCE_BAR)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && mc.level != null) {

            var blindessEffect = mc.player.getEffect(MobEffects.BLINDNESS);

            if (blindessEffect != null && blindessEffect.getAmplifier() == 100) {

                event.setCanceled(true);

                if (currentLayer.equals(VanillaGuiLayers.HOTBAR)) {
                    int screenWidth = event.getGuiGraphics().guiWidth();
                    int screenHeight = event.getGuiGraphics().guiHeight();

                    drawOverlay(event.getGuiGraphics(), mc, blindessEffect, screenWidth, screenHeight);
                }

            }
            else{
                startFadeTime = 0;
                maxDuration = -1;
            }

        }
    }

    private static void drawOverlay(GuiGraphics graphics, Minecraft mc, MobEffectInstance blindness, int screenWidth, int screenHeight) {

        // Background
        graphics.blit(BACKGROUND, 0, 0, screenWidth, screenHeight, 0.0F, 0.0F, 1920, 1080, 1920, 1080);

        // Sound
        if (startFadeTime == 0) {
            startFadeTime = System.currentTimeMillis();
            BlockPos pos = mc.player.blockPosition();
            mc.level.playLocalSound(pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 3.0F, 0.3F, false);
        }

        // Death screen
        long elapsed = System.currentTimeMillis() - startFadeTime;
        float fadeTimer = Math.min(elapsed / (float) FadeContinue, 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0f, 1.0f, 1.0f, fadeTimer);

        //Image sizes
        int imageWidth = 1920;
        int imageHeight = 1080;

        graphics.blit(GAME_OVER_PNG, 0, 0, screenWidth, screenHeight, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);

        // Sets transparency
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        // Loading bar
        int currentDuration = blindness.getDuration();
        if (maxDuration == -1 || currentDuration > maxDuration) {
            maxDuration = currentDuration;
        }

        float progress = 0.0f;
        if (maxDuration > 20){
            progress = 1.0f - ((float) (currentDuration - 20) / (maxDuration - 20));
        }
        else{
            progress = 1.0f - ((float) currentDuration / maxDuration);
        }

        progress = Math.min(1.0f, Math.max(0.0f, progress));

        // Bar's settings
        int barWidth = 200;
        int barHeight = 8;
        int x = (screenWidth - barWidth) / 2;
        int y = screenHeight / 2 + 114;

        // Bar's outline
        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF111111);

        // Bar's background
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF444444);

        // Bar's color
        int fillWidth = (int) (barWidth * progress);
        graphics.fill(x, y, x + fillWidth, y + barHeight, 0xFF22CC22);

        // Bar's text
        String text = "The World's Rollback... " + (int)(progress * 100) + "%";
        graphics.drawCenteredString(mc.font, text, screenWidth / 2, y - 12, 0xFFFFFF);
    }
}
