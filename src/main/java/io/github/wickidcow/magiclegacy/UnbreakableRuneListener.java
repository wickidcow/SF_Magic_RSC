package io.github.wickidcow.magiclegacy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class UnbreakableRuneListener implements Listener {

    private static final String ITEM_ID = "MAGIC_UNBREAKABLE_RUNE";

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack rune = event.getItem();
        if (!SlimefunItemIdentity.is(rune, ITEM_ID)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        ItemStack target = player.getInventory().getItemInOffHand();
        if (target.getType() == Material.AIR) {
            player.sendMessage(ChatColor.AQUA + "Hold the item you want to protect in your off hand.");
            return;
        }

        ItemMeta meta = target.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "That item cannot be made unbreakable.");
            return;
        }

        if (meta.isUnbreakable()) {
            player.sendMessage(ChatColor.AQUA + "That item is already unbreakable.");
            return;
        }

        meta.setUnbreakable(true);
        target.setItemMeta(meta);
        consumeOne(player, rune);

        // Visual-only lightning avoids damage, fire and ten spawned lightning entities from the old script.
        player.getWorld().strikeLightningEffect(player.getLocation());
        player.sendMessage(ChatColor.AQUA + "Your off-hand item is now unbreakable.");
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
