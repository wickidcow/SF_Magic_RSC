// 创建一个 HashMap 来跟踪每个玩家的使用次数
let playerUsageCounts = new java.util.HashMap();
var usageCountall = 0;
function onUse(event) {
  let player = event.getPlayer();

  // 检查主手是否持有物品
  if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
    sendMessage(player, "§bHold the item in your main hand.");
    return;
  }

  let item = player.getInventory().getItemInMainHand();
  let itemMeta = item.getItemMeta();
  let randomChance = Math.random();
  let world = player.getWorld();
  let eyeLocation = player.getEyeLocation();

  // 获取当前玩家的唯一标识符
  let playerId = player.getUniqueId();

  // 获取当前玩家的使用次数，如果不存在则初始化为0
  let usageCount = playerUsageCounts.getOrDefault(playerId, 0) + 1;

  // 更新玩家的使用次数
  playerUsageCounts.put(playerId, usageCount);

  // 更新物品的lore和显示名称
  const lore = itemMeta.getLore() ? itemMeta.getLore().slice(0, 8) : [];
  // 设置物品描述
  lore.push("§f§l今§b§l日§e§l全§a§l服§c§l累§d§l积§f§l导§2§l出§2§l " + usageCountall + " §3§l次 §4§l继 §5§l续 §6§l加 §7§l油§8§l哦~");
  itemMeta.setDisplayName("§d§l圣杯 §e§l[已使用：" + usageCount + "次]"); // 设置物品显示名称
  itemMeta.setLore(lore);
  item.setItemMeta(itemMeta);

  if (randomChance < 0.01) {
    let ExperienceOrb = world.spawn(eyeLocation, org.bukkit.entity.ExperienceOrb);
    ExperienceOrb.setCustomName("精验球");
    ExperienceOrb.setExperience(100); // 设置经验值数量
    usageCountall++;
    org.bukkit.Bukkit.broadcastMessage("§b§l" + player.getName() + "§bMagic Legacy action could not be completed." + usageCount + "§bMagic Legacy action could not be completed.");

    // 重置该玩家的使用次数
    playerUsageCounts.put(playerId, 0);

  }

  if (player.getFoodLevel() <= 0) {
    player.setHealth(0);
    org.bukkit.Bukkit.broadcastMessage("§b§l" + player.getName() + "§bMagic Legacy action could not be completed.");
  } else {
    player.setFoodLevel(player.getFoodLevel() - 4);
    player.setSaturation(player.getSaturation() - 4);
  }
}
