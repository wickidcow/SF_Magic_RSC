package io.github.wickidcow.magiclegacy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

final class RandomFoodListener implements Listener {

    private static final String ITEM_ID = "MAGIC_FOODS_RANDOMFOOD";
    private static final List<String> EFFECT_KEYS = List.of(
        "speed",
        "regeneration",
        "fire_resistance",
        "water_breathing",
        "invisibility",
        "blindness",
        "night_vision",
        "hunger",
        "weakness",
        "poison",
        "wither",
        "health_boost",
        "absorption",
        "saturation",
        "glowing",
        "levitation",
        "luck",
        "unluck",
        "slow_falling",
        "conduit_power",
        "dolphins_grace",
        "bad_omen",
        "hero_of_the_village",
        "darkness"
    );

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!SlimefunItemIdentity.is(event.getItem(), ITEM_ID)) {
            return;
        }

        Player player = event.getPlayer();
        List<PotionEffectType> available = new ArrayList<>();
        for (String key : EFFECT_KEYS) {
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(key));
            if (type != null) {
                available.add(type);
            }
        }

        if (available.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No compatible random-food potion effects are available.");
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int effectCount = random.nextInt(1, 4);

        for (int i = 0; i < effectCount; i++) {
            PotionEffectType type = available.get(random.nextInt(available.size()));
            int amplifier = random.nextInt(50);
            int durationSeconds = random.nextInt(1, 101);
            player.addPotionEffect(new PotionEffect(type, durationSeconds * 20, amplifier, true, true, true));
        }

        player.sendMessage(
            ChatColor.AQUA + "The random food granted " + effectCount
                + (effectCount == 1 ? " magical effect." : " magical effects.")
        );
    }
}
