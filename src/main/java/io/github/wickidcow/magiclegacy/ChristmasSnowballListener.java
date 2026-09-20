package io.github.wickidcow.magiclegacy;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

final class ChristmasSnowballListener implements Listener {

    private static final String ITEM_ID = "MAGIC_CHRISTMAS_SNOWBALL";
    private static final double BEAM_LENGTH = 1.0;
    private static final double BEAM_STEP = 0.05;

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
        consumeOne(player, stack);

        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location start = eye.clone().add(direction);

        for (double distance = 0.0; distance <= BEAM_LENGTH; distance += BEAM_STEP) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.END_ROD, point, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
