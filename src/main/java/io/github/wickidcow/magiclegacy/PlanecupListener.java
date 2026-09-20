package io.github.wickidcow.magiclegacy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class PlanecupListener implements Listener {

    private static final String ITEM_ID = "MAGIC_NEW_PLANECUP";
    private static final int HUNGER_COST = 4;
    private static final int XP_REWARD = 100;
    private static final double REWARD_CHANCE = 0.01;

    private final Map<UUID, Integer> usageCounts = new HashMap<>();
    private int serverRewardCount;

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
        UUID playerId = player.getUniqueId();
        int uses = usageCounts.merge(playerId, 1, Integer::sum);
        boolean rewarded = ThreadLocalRandom.current().nextDouble() < REWARD_CHANCE;

        if (rewarded) {
            ExperienceOrb orb = player.getWorld().spawn(player.getEyeLocation(), ExperienceOrb.class);
            orb.setExperience(XP_REWARD);
            orb.setCustomName("§dMagic Planecup Reward");
            orb.setCustomNameVisible(false);

            serverRewardCount++;
            usageCounts.put(playerId, 0);
            player.getServer().broadcastMessage(
                "§d" + player.getName() + " §7released §f" + XP_REWARD
                    + " XP §7from a Magic Planecup!"
            );
        }

        updateDisplay(stack, rewarded ? 0 : uses);

        if (player.getFoodLevel() <= 0) {
            player.setHealth(0.0);
            return;
        }

        player.setFoodLevel(Math.max(0, player.getFoodLevel() - HUNGER_COST));
        player.setSaturation(Math.max(0.0F, player.getSaturation() - HUNGER_COST));
    }

    private void updateDisplay(ItemStack stack, int uses) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName("§dMagic Planecup §e[Uses: " + uses + "]");
        meta.setLore(List.of(
            "§8Magic Legacy",
            "§7Right-click to invoke the Planecup.",
            "§7Costs §f4 hunger §7per use.",
            "§71% chance to release §f100 XP§7.",
            "§7Personal uses: §f" + uses,
            "§7Server rewards this restart: §f" + serverRewardCount
        ));
        stack.setItemMeta(meta);
    }
}
