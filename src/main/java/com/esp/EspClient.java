package com.esp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class EspClient implements ModInitializer {

    private boolean isShiftPressed = false;

    @Override
    public void onInitialize() {
        // 1. KillAura Saldırı Döngüsünü Oyuna Tanıtıyoruz
        ClientTickEvents.END_CLIENT_TICK.register(new AuraModule());

        // 2. Right Shift Tuş Kontrolünü Tike Bağlıyoruz
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            // Right Shift tuşuna basılıp basılmadığını kontrol et
            boolean isCurrentlyPressed = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

            // Tuşa ilk basıldığı anı yakala (Sürekli aç-kapat yapmasın diye tetikleyici)
            if (isCurrentlyPressed && !isShiftPressed) {
                Config.auraEnabled = !Config.auraEnabled; // KillAura'yı aç veya kapat
                
                // Oyuncuya sohbetten durum bilgisi gönder
                if (Config.auraEnabled) {
                    mc.player.sendMessage(Text.literal("§b[FluxEsp] §aKillAura Aktif! (Menzil: " + Config.auraRange + ")"), true);
                } else {
                    mc.player.sendMessage(Text.literal("§b[FluxEsp] §cKillAura Kapatıldı."), true);
                }
            }
            isShiftPressed = isCurrentlyPressed;
        });

        // 3. Ekrandaki Şık HUD Çizimini Yapıyoruz
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || !Config.showHud) return;

            TextRenderer textRenderer = mc.textRenderer;

            // Sol üst köşeye şık bir gösterge listesi basıyoruz
            drawContext.drawText(textRenderer, Text.literal("§b§lFLUX ESP §7v1.1"), 10, 10, 0xFFFFFF, true);
            
            if (Config.auraEnabled) {
                drawContext.drawText(textRenderer, Text.literal("§a[+] KillAura §7[R-SHIFT]"), 10, 22, 0xFFFFFF, true);
                
                // Eğer o an vurduğumuz bir hedef varsa adını HUD'da gösterelim
                if (AuraModule.currentTarget != null) {
                    String targetName = AuraModule.currentTarget.getName().getString();
                    drawContext.drawText(textRenderer, Text.literal("§d Target: §f" + targetName), 10, 34, 0xFFFFFF, true);
                }
            } else {
                drawContext.drawText(textRenderer, Text.literal("§c[-] KillAura §7[R-SHIFT]"), 10, 22, 0xFFFFFF, true);
            }
        });
    }
}

