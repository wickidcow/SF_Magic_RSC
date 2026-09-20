package io.github.wickidcow.magiclegacy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

final class MagicSoundListener implements Listener {

    private static final String ITEM_ID = "MAGIC_SOUND";
    private final List<Sound> sounds = new ArrayList<>();

    MagicSoundListener() {
        for (Sound sound : Registry.SOUND_EVENT) {
            sounds.add(sound);
        }
    }

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

        if (sounds.isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Player online : event.getPlayer().getServer().getOnlinePlayers()) {
            Sound sound = sounds.get(random.nextInt(sounds.size()));
            online.playSound(online.getLocation(), sound, 1.0F, 1.0F);
        }
    }
}
