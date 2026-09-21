package io.github.wickidcow.magiclegacy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

final class StorageCompatibilityListener implements Listener {

    private static final String STORAGE_PREFIX = "NTW_EXPANSION_CARGO_STORAGE_UNIT_";

    private final MagicLegacyPlugin plugin;
    private File configFile;
    private long configModified = Long.MIN_VALUE;
    private boolean storageRepairEnabled;

    StorageCompatibilityListener(MagicLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!storageRepairEnabled()) {
            return;
        }

        ItemStack stack = event.getItemInHand();
        int tier = storageTier(stack);
        if (tier < 1 || stack.getType() != Material.CHISELED_BOOKSHELF) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(
            ChatColor.RED + "This Magic Drawer skin is damaged. "
                + ChatColor.GRAY + "Use Magic Store Fix " + tier + " before placing it."
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || !storageRepairEnabled()) {
            return;
        }

        ItemStack stack = event.getItem().getItemStack();
        int tier = storageTier(stack);
        if (tier < 1 || stack.getType() != Material.CHISELED_BOOKSHELF) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        List<String> lore = meta.hasLore() && meta.getLore() != null
            ? new ArrayList<>(meta.getLore())
            : new ArrayList<>();

        lore.removeIf(line -> {
            String plain = ChatColor.stripColor(line);
            if (plain == null) {
                return false;
            }
            String normalized = plain.toLowerCase();
            return normalized.contains("damaged storage skin")
                || normalized.contains("magic store fix")
                || normalized.contains("(skin:");
        });

        lore.add(ChatColor.RED + "Damaged storage skin");
        lore.add(
            ChatColor.GRAY + "Use " + ChatColor.WHITE + "Magic Store Fix " + tier
                + ChatColor.GRAY + " to apply a new cosmetic block skin."
        );

        meta.setDisplayName(
            ChatColor.RED + "Damaged Magic Drawer " + tier
                + ChatColor.GRAY + " (Networks Cargo Storage)"
        );
        meta.setLore(lore);
        stack.setItemMeta(meta);
        event.getItem().setItemStack(stack);

        player.playSound(player.getEyeLocation(), Sound.ITEM_TOTEM_USE, 0.7F, 1.2F);
        player.sendMessage(
            ChatColor.AQUA + "Cargo Storage Unit " + tier
                + " needs Magic Store Fix " + tier + " before it can be placed again."
        );
    }

    private int storageTier(ItemStack stack) {
        String id = SlimefunItemIdentity.idOf(stack);
        if (id == null || !id.startsWith(STORAGE_PREFIX)) {
            return -1;
        }

        try {
            int tier = Integer.parseInt(id.substring(STORAGE_PREFIX.length()));
            return tier >= 1 && tier <= 13 ? tier : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private boolean storageRepairEnabled() {
        File file = configFile();
        if (file == null || !file.isFile()) {
            storageRepairEnabled = false;
            configModified = Long.MIN_VALUE;
            return false;
        }

        long modified = file.lastModified();
        if (modified != configModified) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            storageRepairEnabled = config.getBoolean("options.magic.storage", false);
            configModified = modified;
        }

        return storageRepairEnabled;
    }

    private File configFile() {
        Plugin rsc = plugin.getServer().getPluginManager().getPlugin("RykenSlimefunCustomizer");
        if (rsc == null) {
            return null;
        }

        File resolved = new File(rsc.getDataFolder(), "configs/Magic/config.yml");
        if (!resolved.equals(configFile)) {
            configFile = resolved;
            configModified = Long.MIN_VALUE;
        }
        return configFile;
    }
}
