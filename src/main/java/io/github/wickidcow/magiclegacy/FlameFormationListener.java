package io.github.wickidcow.magiclegacy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

final class FlameFormationListener implements Listener {

    private static final String ITEM_ID = "MAGIC_ZHENFA_FIRE_1";
    private static final long COOLDOWN_MILLIS = 1000L;
    private static final double RADIUS = 5.0;
    private static final double HALF_HEIGHT = 2.5;
    private static final double DAMAGE = 100.0;
    private static final int ANGLE_STEP = 10;
    private static final double HEIGHT_STEP = 1.0;

    private final Map<UUID, Long> lastUse = new HashMap<>();

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack stack = event.getItem();
        if (!SlimefunItemIdentity.is(stack, ITEM_ID)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long previous = lastUse.getOrDefault(player.getUniqueId(), 0L);
        long remaining = COOLDOWN_MILLIS - (now - previous);

        if (remaining > 0L) {
            player.sendMessage("§cFlame Formation is cooling down for " + Math.max(1L, (remaining + 999L) / 1000L) + "s.");
            return;
        }

        lastUse.put(player.getUniqueId(), now);

        player.setFoodLevel(0);
        player.setSaturation(0.0F);

        Location center = player.getLocation();
        drawFormation(center);
        damageNearby(center, player);

        player.sendMessage("§6Flame Formation ignited. §7Your hunger was consumed.");
    }

    private static void drawFormation(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        for (double y = -HALF_HEIGHT; y <= HALF_HEIGHT; y += HEIGHT_STEP) {
            for (int angle = 0; angle < 360; angle += ANGLE_STEP) {
                double radians = Math.toRadians(angle);
                double x = center.getX() + RADIUS * Math.cos(radians);
                double z = center.getZ() + RADIUS * Math.sin(radians);
                world.spawnParticle(
                    Particle.FLAME,
                    x,
                    center.getY() + y,
                    z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
                );
            }
        }
    }

    private static void damageNearby(Location center, Player caster) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(center, RADIUS, HALF_HEIGHT, RADIUS)) {
            if (entity == caster) {
                continue;
            }

            if (entity instanceof LivingEntity living) {
                living.damage(DAMAGE, caster);
            }
        }
    }
}
