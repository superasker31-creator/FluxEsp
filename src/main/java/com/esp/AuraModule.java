package com.esp;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AuraModule implements ClientTickEvents.EndTick {
    
    public static LivingEntity currentTarget = null;
    private final Random random = new Random();
    private int attackDelay = 0;

    @Override
    public void onEndTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || !Config.auraEnabled) {
            currentTarget = null;
            return;
        }

        // AttackAura'nın gelişmiş hedef filtreleme ve en yakın düşmanı seçme mantığı
        List<LivingEntity> targets = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> entity != mc.player && entity.isAlive())
                .filter(entity -> mc.player.distanceTo(entity) <= Config.auraRange)
                .filter(entity -> (entity instanceof PlayerEntity)) // Sadece oyunculara odaklan
                .filter(entity -> mc.player.canSee(entity)) // Görüş kontrolü
                .sorted(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity)))
                .collect(Collectors.toList());

        if (!targets.isEmpty()) {
            currentTarget = targets.get(0);

            // AttackAura'dan alınan dinamik kilitlenme mekanizması
            rotateTowards(mc, currentTarget);

            // Anticheat'i bypass eden dinamik vuruş zamanlayıcısı (Rastgele gecikme ekler)
            if (attackDelay > 0) {
                attackDelay--;
            }

            // Hem vuruş barı dolduysa hem de rastgele gecikme süresi bittiyse vur
            if (mc.player.getAttackCooldownProgress(0.0F) >= 1.0F && attackDelay == 0) {
                // Sunucuya vuruş paketini gönder
                mc.interactionManager.attackEntity(mc.player, currentTarget);
                mc.player.swingHand(Hand.MAIN_HAND);
                
                // Bir sonraki vuruş için 2 ila 5 tick arasında rastgele bir gecikme ata (Bypass koruması)
                attackDelay = 2 + random.nextInt(4);
            }
        } else {
            currentTarget = null;
        }
    }

    // AttackAura'nın hassas kafa/gövde açı hesaplama rotasyonu
    private void rotateTowards(MinecraftClient mc, LivingEntity target) {
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        // Hedefin tam gövde merkezine odaklanmak için boyunun yarısını hesaplıyoruz
        double diffY = (target.getY() + (target.getHeight() / 2.0F)) - mc.player.getEyeY();

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, dist)));

        // Config'deki hıza göre kamerayı yumuşakça hedefe kaydır
        float speedMultiplier = Config.aimSpeed / 100.0f;
        mc.player.setYaw(mc.player.getYaw() + (yaw - mc.player.getYaw()) * speedMultiplier);
        mc.player.setPitch(mc.player.getPitch() + (pitch - mc.player.getPitch()) * speedMultiplier);
    }
}
