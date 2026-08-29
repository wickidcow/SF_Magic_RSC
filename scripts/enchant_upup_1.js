function onUse(event) {
  // 获取触发事件的玩家对象
  let player = event.getPlayer();
  
  // 获取玩家副手中的物品
  let offHandItem = player.getInventory().getItemInOffHand();

  // 检查主手是否是用主手进行操作
  if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
    // 如果不是主手，则发送消息并返回
    player.sendMessage("§bMagic Legacy action could not be completed.");
    return;
  }

  // 检查副手中是否有物品
  if (offHandItem === null || offHandItem.getType() === org.bukkit.Material.AIR) {
    // 如果没有物品或持有空气，则发送消息并返回
    player.sendMessage("§bMagic Legacy action could not be completed.");
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
  if (maxLevel > 0) {
    // 计算升级成功的几率
    let upgradeChance = calculateUpgradeChance(maxLevel);
    
    // 生成一个随机数
    let randomChance = Math.random();
    
    // 如果随机数小于升级成功的几率
    if (randomChance < upgradeChance) {
      // 升级副手物品的附魔
      upgradeEnchantments(offHandItem);

      // 减少主手物品的数量
      decrementItemAmount(player.getInventory().getItemInMainHand());
      
      // 发送成功消息给玩家
      player.sendMessage("§bMagic Legacy action could not be completed.");
    } else {
      // 如果随机数大于等于升级成功的几率，则附魔失败
      // 销毁副手物品
      player.getInventory().setItemInOffHand(null);
      
      // 减少主手物品的数量
      decrementItemAmount(player.getInventory().getItemInMainHand());
      
      // 发送失败消息给玩家
      player.sendMessage("§bMagic Legacy action could not be completed.");
      org.bukkit.Bukkit.broadcastMessage("§bMagic Legacy action could not be completed."+player.getName()+"§bMagic Legacy action could not be completed.");
    }
  } else {
    // 如果副手物品没有附魔，发送提示消息
    player.sendMessage("§bMagic Legacy action could not be completed.");
  }
}

// 计算附魔升级成功的几率
function calculateUpgradeChance(level) {
  // 初始成功几率为1.0（即100%）
  let baseChance = 0.99;
  
  // 每10级降低的成功几率
  let decreasePerTenLevels = 0.02;
  
  // 每10级减少一次成功几率
  while (level >= 10) {
    baseChance -= decreasePerTenLevels;
    level -= 10;
  }
  
  // 返回计算后的成功几率
  return baseChance;
}

// 升级副手物品的附魔等级
function upgradeEnchantments(item) {
  // 获取副手物品的所有附魔
  let enchantments = item.getEnchantments();
  
  // 获取附魔条目的集合
  let entrySet = enchantments.entrySet();
  
  // 遍历所有附魔条目
  for (let entry of entrySet) {
    // 获取当前附魔类型
    let enchantment = entry.getKey();
    
    // 获取当前附魔等级
    let level = entry.getValue();
    
    // 将当前附魔等级加1，并重新应用到物品上
    item.addUnsafeEnchantment(enchantment, level + 1);
  }
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

