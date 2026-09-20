package io.github.wickidcow.magiclegacy;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

final class MagicLaserStickListener implements Listener {

    private static final String ITEM_ID = "MAGIC_STICK_JIGUANG_1";
    private static final float CHARGE_COST = 50.0F;
    private static final double RANGE = 25.0;
    private static final double DAMAGE = 1000.0;
    private static final double BEAM_STEP = 0.5;
    private static final double SPIRAL_RADIUS = 0.5;
    private static final double SPIRAL_PITCH = Math.PI / 2.0;

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
        Float charge = SlimefunItemIdentity.getCharge(stack);
        if (charge == null) {
            player.sendMessage("§cMagic Legacy could not read this item's charge.");
            return;
        }

        if (charge < CHARGE_COST) {
            player.sendMessage("§bNot enough charge. Please recharge the item.");
            return;
        }

        if (!SlimefunItemIdentity.removeCharge(stack, CHARGE_COST)) {
            player.sendMessage("§cMagic Legacy could not consume the required charge.");
            return;
        }

        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location start = eye.clone().add(direction);

        playActivationSound(world, eye);
        drawBeam(world, start, direction);
        drawDualSpiral(world, start, direction);
        spawnBurst(world, start);

        RayTraceResult result = world.rayTrace(
            start,
            direction,
            RANGE,
            FluidCollisionMode.ALWAYS,
            true,
            0.1,
            null
        );

        if (result == null) {
            player.sendMessage("§bNo living target hit.");
            return;
        }

        Entity entity = result.getHitEntity();
        if (entity instanceof LivingEntity living) {
            living.damage(DAMAGE, player);
            player.sendMessage("§bSuccessfully hit and damaged " + living.getName() + "!");
        } else {
            player.sendMessage("§bNo living target hit.");
        }
    }

    private static void drawBeam(World world, Location start, Vector direction) {
        for (double distance = 0.0; distance <= RANGE; distance += BEAM_STEP) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.END_ROD, point, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void drawDualSpiral(World world, Location start, Vector direction) {
        Vector up = new Vector(0.0, 1.0, 0.0);
        Vector right = direction.clone().crossProduct(up);

        if (right.lengthSquared() < 0.01) {
            up = new Vector(1.0, 0.0, 0.0);
            right = direction.clone().crossProduct(up);
        }

        right.normalize();
        Vector localUp = right.clone().crossProduct(direction).normalize();

        for (double distance = 0.0; distance <= RANGE; distance += BEAM_STEP) {
            Location center = start.clone().add(direction.clone().multiply(distance));
            double angle = distance * SPIRAL_PITCH;
            double offsetX = Math.sin(angle) * SPIRAL_RADIUS;
            double offsetY = Math.cos(angle) * SPIRAL_RADIUS;
            Vector offset = right.clone().multiply(offsetX).add(localUp.clone().multiply(offsetY));

            world.spawnParticle(Particle.END_ROD, center.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticle(Particle.END_ROD, center.clone().subtract(offset), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void spawnBurst(World world, Location start) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            double x = random.nextDouble(-0.2, 0.2);
            double y = random.nextDouble(-0.2, 0.2);
            double z = random.nextDouble(-0.2, 0.2);
            world.spawnParticle(Particle.CLOUD, start.clone().add(x, y, z), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void playActivationSound(World world, Location location) {
        Sound sound = Registry.SOUND_EVENT.get(NamespacedKey.minecraft("block.beacon.activate"));
        if (sound != null) {
            world.playSound(location, sound, 1.0F, 1.0F);
        }
    }
}
