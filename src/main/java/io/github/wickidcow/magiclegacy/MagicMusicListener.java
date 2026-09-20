package io.github.wickidcow.magiclegacy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class MagicMusicListener implements Listener {

    private static final String ITEM_ID = "MAGIC_MUSIC";
    private static final long COOLDOWN_MILLIS = 150_000L;

    private final Map<UUID, Long> lastUse = new HashMap<>();
    private int usageCount;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack item = event.getItem();
        if (!SlimefunItemIdentity.is(item, ITEM_ID)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long previous = lastUse.getOrDefault(player.getUniqueId(), 0L);
        long remaining = COOLDOWN_MILLIS - (now - previous);

        if (remaining > 0L) {
            long seconds = (remaining + 999L) / 1000L;
            player.sendTitle(
                ChatColor.RED + "" + ChatColor.BOLD + "Magic Music is cooling down",
                ChatColor.GRAY + "Ready in " + ChatColor.AQUA + seconds + "s",
                10, 40, 10
            );
            updateDisplay(item, seconds);
            return;
        }

        player.setFoodLevel(0);
        player.setSaturation(0.0F);
        usageCount++;
        lastUse.put(player.getUniqueId(), now);

        int grown = 0;
        for (Entity entity : player.getNearbyEntities(4.0, 4.0, 4.0)) {
            if (entity instanceof Chicken chicken && !chicken.isAdult()) {
                chicken.setAdult();
                grown++;
            }
        }

        updateDisplay(item, 150L);
        player.getServer().broadcastMessage(
            ChatColor.LIGHT_PURPLE + player.getName()
                + ChatColor.GRAY + " used Magic Music. Server uses this restart: "
                + ChatColor.WHITE + usageCount
                + ChatColor.GRAY + ". Chickens matured: "
                + ChatColor.WHITE + grown
                + ChatColor.GRAY + "."
        );
    }

    private void updateDisplay(ItemStack item, long cooldownSeconds) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Magic Music");
        meta.setLore(List.of(
            ChatColor.DARK_GRAY + "Magic Legacy",
            ChatColor.GRAY + "Use: empties hunger and matures baby chickens within 4 blocks.",
            ChatColor.GRAY + "Per-player cooldown: " + ChatColor.WHITE + "150 seconds",
            ChatColor.GRAY + "Cooldown remaining: " + ChatColor.AQUA + cooldownSeconds + "s",
            ChatColor.GRAY + "Server uses this restart: " + ChatColor.WHITE + usageCount
        ));
        item.setItemMeta(meta);
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }
}
