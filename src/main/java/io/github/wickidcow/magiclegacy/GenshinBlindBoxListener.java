package io.github.wickidcow.magiclegacy;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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

final class GenshinBlindBoxListener implements Listener {

    private static final String BOX_ID = "MAGIC_GENSHIN_IMPACT_RADDOM";
    private static final int REWARD_COUNT = 27;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack box = event.getItem();
        if (!SlimefunItemIdentity.is(box, BOX_ID)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        int roll = ThreadLocalRandom.current().nextInt(1, REWARD_COUNT + 1);
        String rewardId = "MAGIC_GENSHIN_IMPACT_" + roll;
        ItemStack reward = SlimefunItemIdentity.copyById(rewardId);

        if (reward == null || reward.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "The selected Genshin reward is not registered. The box was not consumed.");
            return;
        }

        reward.setAmount(1);
        consumeOne(player, box);

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(reward);
        if (!leftovers.isEmpty()) {
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            player.sendMessage(
                ChatColor.AQUA + "Opened the Magic Genshin Blind Box: "
                    + ChatColor.WHITE + displayName(reward)
                    + ChatColor.GRAY + " (inventory full, dropped nearby)."
            );
        } else {
            player.sendMessage(
                ChatColor.AQUA + "Opened the Magic Genshin Blind Box: "
                    + ChatColor.WHITE + displayName(reward) + ChatColor.GRAY + "."
            );
        }
    }

    private static String displayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().getKey().getKey();
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }
}
