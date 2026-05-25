package com.esp;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
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
        if (mc.player == null || mc.world == null || !Config.auraEnabled) {
            currentTarget = null;
            return;
        }

        // 1.20.1 Yapısına uygun temiz hedef filtreleme
        List<LivingEntity> targets = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> entity != mc.player && entity.isAlive())
                .filter(entity -> mc.player.distanceTo(entity) <= Config.auraRange)
                .filter(entity -> (entity instanceof PlayerEntity)) 
                .filter(entity -> mc.player.canSee(entity)) 
                .sorted(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity)))
                .collect(Collectors.toList());

        if (!targets.isEmpty()) {
            currentTarget = targets.get(0);

            // Vuruş barı dolduysa saldır
            if (mc.player.getAttackCooldownProgress(0.0F) >= 1.0F) {
                rotateTowards(mc, currentTarget);
                mc.interactionManager.attackEntity(mc.player, currentTarget);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            currentTarget = null;
        }
    }

    private void rotateTowards(MinecraftClient mc, LivingEntity target) {
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        double diffY = target.getEyeY() - mc.player.getEyeY();

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, dist)));

        float speedMultiplier = Config.aimSpeed / 100.0f;
        mc.player.setYaw(mc.player.getYaw() + (yaw - mc.player.getYaw()) * speedMultiplier);
        mc.player.setPitch(mc.player.getPitch() + (pitch - mc.player.getPitch()) * speedMultiplier);
    }
}
