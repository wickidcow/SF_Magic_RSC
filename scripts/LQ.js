// 创建一个 HashMap 来跟踪每个玩家的最后使用时间
let playerLastUseTimes = new java.util.HashMap();

// 全局的使用次数计数器
let usageCount = 0;

function onUse(event) {
  const player = event.getPlayer();
  const playerId = player.getUniqueId();

  // 检查主手是否持有物品
  if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
    sendMessage(player, "§bHold the item in your main hand.");
    return;
  }

  const currentTime = new Date().getTime();
  
  // 获取当前玩家的最后使用时间，如果不存在则初始化为0
  let lastUseTime = playerLastUseTimes.getOrDefault(playerId, 0);
  
  if (currentTime - lastUseTime < 150000) {
    const remainingTime = Math.ceil((150000 - (currentTime - lastUseTime)) / 1000);
    player.sendTitle("§c§l你干嘛啊~哎呦！", "冷却剩余时间：" + remainingTime + "秒", 10, 40, 10);

    const item = player.getInventory().getItemInMainHand();
    const itemMeta = item.getItemMeta();
    const lore = itemMeta.getLore() ? itemMeta.getLore().slice(0, -2) : [];
    lore.push("§a§l冷§b§l却§c§l剩§d§l余§e§l时§f§l间§2§l :§3§l" + remainingTime + "§4§l秒");
    lore.push("§f§l今§b§l日§e§l全§a§l服§c§l累§d§l积§f§l完§2§l成§2§l " + usageCount + " §3§l次 §4§l唱 §5§l跳 §6§lRAP §7§l篮§8§l球");
    itemMeta.setDisplayName("§e§lMUSIC§f§l~ §2§l[§3§l已§4§l完§5§l成§6§l：§7§l " + usageCount + " §8§l次]"); // 设置物品显示名称
    itemMeta.setLore(lore);
    item.setItemMeta(itemMeta);
    return; 
  }

  player.setFoodLevel(0);
  player.setSaturation(0);
  usageCount++; // 更新全局使用次数

  org.bukkit.Bukkit.broadcastMessage("§b§l" + player.getName() + "§bMagic Legacy action could not be completed." + usageCount + "§bMagic Legacy action could not be completed.");

  // 更新该玩家的最后使用时间为当前时间
  playerLastUseTimes.put(playerId, currentTime);

  // 对附近的实体进行操作（如将小鸡变为成年）
  let entities = player.getNearbyEntities(4, 4, 4);
  for (let entity of entities) {
    if (entity instanceof org.bukkit.entity.Chicken && !entity.isAdult()) {
      entity.setAdult();
    }
  }
}
