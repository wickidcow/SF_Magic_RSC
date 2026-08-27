function onUse(event) {
  // 获取触发事件的玩家对象
  var player = event.getPlayer();
  
  // 获取玩家副手中的物品
  let offHandItem = player.getInventory().getItemInOffHand();

  // 检查主手是否是用主手进行操作
  if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
    // 如果不是主手，则发送消息并返回
    player.sendMessage("§e[魔法-强化] §fUse your main hand for this action");
    return;
  }

  // 检查副手中是否有物品
  if (offHandItem === null || offHandItem.getType() === org.bukkit.Material.AIR) {
    // 如果没有物品或持有空气，则发送消息并返回
    player.sendMessage("§e[魔法-强化] §fYou must hold an item in your off hand");
    return;
  }

  // 获取副手物品的所有附魔
  let enchantments = offHandItem.getEnchantments();
  
  // 初始化最高附魔等级为0
  let maxLevel = 0;
  
  // 获取附魔条目的集合
  let entrySet = enchantments.entrySet();
  
  // 遍历所有附魔条目
  for (let entry of entrySet) {

    // 获取当前附魔等级
    let level = entry.getValue();
    
    // 更新最高附魔等级
    if (level > maxLevel) {
      maxLevel = level;
    }
  }

  // 如果有附魔
    // 计算升级成功的几率
    let upgradeChance = calculateUpgradeChance(maxLevel);
    
    // 生成一个随机数
    let randomChance = Math.random();
    
    // 如果随机数小于升级成功的几率
    if (randomChance < upgradeChance) {
      
      // 添加一个随机附魔
      addRandomEnchantment(offHandItem,player);

      // 减少主手物品的数量（假设主手持有增幅书）
      decrementItemAmount(player.getInventory().getItemInMainHand());
      
    } else {
      // 如果随机数大于等于升级成功的几率，则附魔失败
      // 销毁副手物品
      player.getInventory().setItemInOffHand(null);
      
      // 减少主手物品的数量（假设主手持有增幅书）
      decrementItemAmount(player.getInventory().getItemInMainHand());
      
      // 发送失败消息给玩家
      player.sendMessage("§e[魔法-强化] §f强化失败，装备已破碎。");
      org.bukkit.Bukkit.broadcastMessage("§b§l玩家"+player.getName()+"§e§l 的§a§l 装§c§l 备§b§l 爆 §3§l炸 §4§l咯 §5§l~ §6§l~");
    }
  

}

// 计算附魔升级成功的几率
function calculateUpgradeChance(level) {
  // 初始成功几率为1.0（即100%）
  let baseChance = 0.99;
  
  // 每10级降低的成功几率
  let decreasePerTenLevels = 0.01;
  
  // 每10级减少一次成功几率
  while (level >= 10) {
    baseChance -= decreasePerTenLevels;
    level -= 10;
  }
  
  // 返回计算后的成功几率
  return baseChance;
}

// 减少物品数量
function decrementItemAmount(item) {
  // 如果物品存在且数量大于1，则减少1个
  if (item && item.getAmount() > 1) {
    item.setAmount(item.getAmount() - 1);
  } 
  // 如果物品存在但数量为1，则将数量设置为0
  else if (item) {
    item.setAmount(0);
  }
}

// 添加一个随机附魔
function addRandomEnchantment(item,player) {
  // 获取所有可用的附魔类型
  let availableEnchantments = org.bukkit.enchantments.Enchantment.values();
  
  // 随机选择一个附魔类型
  let randomIndex = Math.floor(Math.random() * availableEnchantments.length);
  let randomEnchantment = availableEnchantments[randomIndex];
  
  // 检查物品是否已经拥有该附魔
  let existingLevel = item.getEnchantmentLevel(randomEnchantment);
  
  // 设置附魔等级
  let newLevel = existingLevel > 0 ? existingLevel + 1 : 1;
  
  
  // 添加或升级附魔
  item.addUnsafeEnchantment(randomEnchantment, newLevel);

  if (newLevel==1){

    player.sendMessage("§e[魔法-强化] §f获得了新的附魔。");

  }else {

    player.sendMessage("§e[魔法-强化] §f原有的附魔等提升了。");

  }
}