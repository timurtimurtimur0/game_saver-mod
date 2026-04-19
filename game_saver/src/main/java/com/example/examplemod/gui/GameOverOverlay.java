package com.example.examplemod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "game_saver", value = Dist.CLIENT)
public class GameOverOverlay {

    private static long startFadeTime = 0;
    private static final long FadeContinue = 3000;
    private static final ResourceLocation GAME_OVER_PNG = new ResourceLocation("game_saver", "textures/gui/game_over.png");
    private static final ResourceLocation BACKGROUND = new ResourceLocation("game_saver", "textures/gui/background.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null || mc.level != null) {

            var blindessEffect = mc.player.getEffect(MobEffects.BLINDNESS);

            if (blindessEffect != null && blindessEffect.getAmplifier() == 100) {

                event.setCanceled(true);

                int screenWidth = event.getWindow().getGuiScaledWidth();
                int screenHeight = event.getWindow().getGuiScaledHeight();

                GuiGraphics graphics_black = event.getGuiGraphics();

                graphics_black.blit(BACKGROUND, 0, 0, screenWidth, screenHeight, 0.0F, 0.0F, 1920, 1080, 1920, 1080);

                if (startFadeTime == 0){

                    startFadeTime = System.currentTimeMillis();

                    BlockPos pos = mc.player.blockPosition();
                    mc.level.playLocalSound(pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 3.0F, 0.3F, false);
                }

                long elapsed = System.currentTimeMillis() - startFadeTime;

                float Timer = Math.min(elapsed / (float) FadeContinue, 1.0f);

                GuiGraphics graphics = event.getGuiGraphics();


                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                // Sets transparency
                graphics.setColor(1.0f, 1.0f, 1.0f, Timer);

                //Image sizes
                int imageWidth = 1920;
                int imageHeight = 1080;

                graphics.blit(GAME_OVER_PNG, 0, 0, screenWidth, screenHeight, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);

                // Discard changes so as not to break the GUI
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
            }
            else{
                startFadeTime = 0;
            }

        }
    }
}
