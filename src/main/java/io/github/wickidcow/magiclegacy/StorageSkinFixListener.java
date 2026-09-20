package io.github.wickidcow.magiclegacy;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class StorageSkinFixListener implements Listener {

    private static final String FIX_PREFIX = "MAGIC_STORE_FIX_";
    private static final String STORAGE_PREFIX = "NTW_EXPANSION_CARGO_STORAGE_UNIT_";

    private static final List<Skin> SKINS = List.of(
        new Skin(Material.STONE, "Stone"),
        new Skin(Material.GRASS_BLOCK, "Grass Block"),
        new Skin(Material.DIRT, "Dirt"),
        new Skin(Material.COBBLESTONE, "Cobblestone"),
        new Skin(Material.OAK_WOOD, "Oak Wood"),
        new Skin(Material.SANDSTONE, "Sandstone"),
        new Skin(Material.BRICKS, "Bricks"),
        new Skin(Material.MOSSY_COBBLESTONE, "Mossy Cobblestone"),
        new Skin(Material.OBSIDIAN, "Obsidian"),
        new Skin(Material.IRON_ORE, "Iron Ore"),
        new Skin(Material.GOLD_ORE, "Gold Ore"),
        new Skin(Material.DIAMOND_ORE, "Diamond Ore"),
        new Skin(Material.EMERALD_ORE, "Emerald Ore"),
        new Skin(Material.RED_SANDSTONE, "Red Sandstone"),
        new Skin(Material.END_STONE, "End Stone"),
        new Skin(Material.NETHERRACK, "Netherrack"),
        new Skin(Material.QUARTZ_BLOCK, "Quartz Block"),
        new Skin(Material.COAL_ORE, "Coal Ore"),
        new Skin(Material.FURNACE, "Furnace"),
        new Skin(Material.CHEST, "Chest"),
        new Skin(Material.BOOKSHELF, "Bookshelf"),
        new Skin(Material.CRAFTING_TABLE, "Crafting Table"),
        new Skin(Material.FURNACE, "Furnace"),
        new Skin(Material.ENCHANTING_TABLE, "Enchanting Table"),
        new Skin(Material.ANVIL, "Anvil"),
        new Skin(Material.BEACON, "Beacon"),
        new Skin(Material.DAYLIGHT_DETECTOR, "Daylight Detector"),
        new Skin(Material.HOPPER, "Hopper"),
        new Skin(Material.DISPENSER, "Dispenser"),
        new Skin(Material.DROPPER, "Dropper")
    );

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack fixer = event.getItem();
        String fixerId = SlimefunItemIdentity.idOf(fixer);
        int tier = parseTier(fixerId, FIX_PREFIX);
        if (tier < 1 || tier > 13) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack storage = player.getInventory().getItemInOffHand();

        if (storage == null || storage.getType().isAir()) {
            player.sendMessage(ChatColor.AQUA + "Hold the matching Networks cargo storage unit in your off hand.");
            return;
        }

        if (storage.getAmount() != 1) {
            player.sendMessage(ChatColor.AQUA + "The off-hand storage unit must be a single item.");
            return;
        }

        String storageId = SlimefunItemIdentity.idOf(storage);
        String expectedId = STORAGE_PREFIX + tier;
        if (!expectedId.equals(storageId)) {
            player.sendMessage(
                ChatColor.AQUA + "Store Fix " + tier + " only works with Cargo Storage Unit " + tier + "."
            );
            return;
        }

        Skin skin = SKINS.get(ThreadLocalRandom.current().nextInt(SKINS.size()));
        ItemMeta meta = storage.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "The storage unit has no item metadata, so it was not changed.");
            return;
        }

        storage.setType(skin.material());
        meta.setDisplayName(
            ChatColor.GREEN + "Infused Magic Drawer "
                + ChatColor.GRAY + "(Skin: " + ChatColor.AQUA + skin.name() + ChatColor.GRAY + ")"
        );
        storage.setItemMeta(meta);

        consumeOne(player, fixer);
        player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);
        player.sendMessage(
            ChatColor.AQUA + "Applied " + ChatColor.WHITE + skin.name()
                + ChatColor.AQUA + " skin to Cargo Storage Unit " + tier + "."
        );
    }

    private static int parseTier(String id, String prefix) {
        if (id == null || !id.startsWith(prefix)) {
            return -1;
        }
        try {
            return Integer.parseInt(id.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return -1;
        }
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

    private record Skin(Material material, String name) {
    }
}
