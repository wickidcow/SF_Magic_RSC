package io.github.wickidcow.magiclegacy;

import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

final class ExperienceItemListener implements Listener {

    private static final String COLLECTOR_ID = "MAGIC_EXP_COLLECTOR";
    private static final String BOTTLE_ID = "MAGIC_EXP_BOTTLE";
    private static final int REQUIRED_LEVELS = 100;
    private static final int RELEASED_EXPERIENCE = 11111;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack stack = event.getItem();
        if (SlimefunItemIdentity.is(stack, COLLECTOR_ID)) {
            collect(event, stack);
        } else if (SlimefunItemIdentity.is(stack, BOTTLE_ID)) {
            release(event, stack);
        }
    }

    private static void collect(PlayerInteractEvent event, ItemStack collector) {
        Player player = event.getPlayer();
        event.setCancelled(true);

        if (player.getLevel() < REQUIRED_LEVELS) {
            player.sendMessage(ChatColor.AQUA + "You need at least 100 experience levels to fill this collector.");
            return;
        }

        ItemStack bottle = SlimefunItemIdentity.copyById(BOTTLE_ID);
        if (bottle == null) {
            player.sendMessage(ChatColor.RED + "Magic EXP Bottle is not registered. Nothing was consumed.");
            return;
        }

        player.setLevel(player.getLevel() - REQUIRED_LEVELS);
        consumeOne(player, collector);

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(bottle);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.sendMessage(ChatColor.AQUA + "Stored 100 experience levels in a Magic EXP Bottle.");
    }

    private static void release(PlayerInteractEvent event, ItemStack bottle) {
        Player player = event.getPlayer();
        event.setCancelled(true);
        consumeOne(player, bottle);

        ExperienceOrb orb = player.getWorld().spawn(player.getEyeLocation(), ExperienceOrb.class);
        orb.setCustomName("Magic Experience Orb");
        orb.setExperience(RELEASED_EXPERIENCE);

        player.sendMessage(ChatColor.AQUA + "Released the Magic EXP Bottle.");
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
