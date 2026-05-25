package com.esp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class EspClient implements ModInitializer {

    private static KeyBinding auraToggleKey;

    @Override
    public void onInitialize() {
        // 1. Right Shift Tuşunu Fabric Sistemine Güvenli Şekilde Kaydediyoruz
        auraToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fluxesp.toggle", 
                InputUtil.Type.KEYSYM, 
                GLFW.GLFW_KEY_RIGHT_SHIFT, 
                "category.fluxesp.keys"
        ));

        // 2. KillAura Ana Döngüsünü Çalıştırıyoruz
        ClientTickEvents.END_CLIENT_TICK.register(new AuraModule());

        // 3. Tuş Algılama Kontrolü (Her Basışta Bir Kez Tetiklenir)
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            while (auraToggleKey.wasPressed()) {
                Config.auraEnabled = !Config.auraEnabled;
                
                if (Config.auraEnabled) {
                    mc.player.sendMessage(Text.literal("§b[FluxEsp] §aKillAura Aktif! (Menzil: " + Config.auraRange + ")"), true);
                } else {
                    mc.player.sendMessage(Text.literal("§b[FluxEsp] §cKillAura Kapatıldı."), true);
                }
            }
        });

        // 4. Ekrana HUD Yazılarını Basıyoruz
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || !Config.showHud) return;

            TextRenderer textRenderer = mc.textRenderer;

            // Sol üst köşeye şık gösterge
            drawContext.drawText(textRenderer, Text.literal("§b§lFLUX ESP §7v1.1"), 10, 10, 0xFFFFFF, true);
            
            if (Config.auraEnabled) {
                drawContext.drawText(textRenderer, Text.literal("§a[+] KillAura §7[R-SHIFT]"), 10, 22, 0xFFFFFF, true);
                if (AuraModule.currentTarget != null) {
                    String targetName = AuraModule.currentTarget.getName().getString();
                    drawContext.drawText(textRenderer, Text.literal("§d Hedef: §f" + targetName), 10, 34, 0xFFFFFF, true);
                }
            } else {
                drawContext.drawText(textRenderer, Text.literal("§c[-] KillAura §7[R-SHIFT]"), 10, 22, 0xFFFFFF, true);
            }
        });
    }
}
