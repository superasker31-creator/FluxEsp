package com.esp;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AuraModule implements ClientTickEvents.EndTick {
    
    public static LivingEntity currentTarget = null;

    @Override
    public void onEndTick(MinecraftClient mc) {
        // Eğer oyuncu dünyada değilse veya hile menüden kapalıysa çalışma
        if (mc.player == null || mc.world == null || !Config.auraEnabled) {
            currentTarget = null;
            return;
        }

        // Etraftaki vurabileceğimiz hedefleri filtrele ve listele
        List<LivingEntity> targets = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> entity != mc.player && !entity.isDead()) // Kendimiz olmayacağız ve ölü olmayacak
                .filter(entity -> mc.player.distanceTo(entity) <= Config.auraRange) // Menzil kontrolü
                .filter(entity -> !Config.showHud || (entity instanceof PlayerEntity)) // Sadece oyuncu filtresi
                .filter(entity -> mc.player.canSee(entity)) // Görüş hattı kontrolü (Duvar arkası vurmaz)
                .sorted(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity))) // En yakındakini seç
                .collect(Collectors.toList());

        if (!targets.isEmpty()) {
            currentTarget = targets.get(0); // En yakın hedefi kitle

            // 1.20.1 Vuruş Barı (Cooldown) Kontrolü: Sadece bar tam dolduğunda vur (Maksimum Hasar)
            if (mc.player.getAttackCooldownProgress(0.0F) >= 1.0F) {
                // Kafayı hedefe çevirme simülasyonu (Rotation)
                rotateTowards(mc, currentTarget);
                
                // Paket seviyesinde adama sol tık at (Vur)
                mc.interactionManager.attackEntity(mc.player, currentTarget);
                mc.player.swingHand(Hand.MAIN_HAND); // El sallama animasyonu
            }
        } else {
            currentTarget = null;
        }
    }

    // Kamerayı hedefin gövdesine/kafasına matematiksel olarak çeviren fonksiyon
    private void rotateTowards(MinecraftClient mc, Entity target) {
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        double diffY = (target.getEyeY() - 0.4) - mc.player.getEyeY();

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, dist)));

        // Hızı Config.aimSpeed üzerinden ayarlayarak yumuşatıyoruz (Anticheat yakalamasın diye)
        float speedMultiplier = Config.aimSpeed / 100.0f;
        mc.player.setYaw(mc.player.getYaw() + (yaw - mc.player.getYaw()) * speedMultiplier);
        mc.player.setPitch(mc.player.getPitch() + (pitch - mc.player.getPitch()) * speedMultiplier);
    }
}
