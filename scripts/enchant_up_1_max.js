function onUse(event) {
  // 获取触发事件的玩家对象
  var player = event.getPlayer();
  
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


      
      // 添加一个随机附魔
      addRandomEnchantment(offHandItem,player);

      // 减少主手物品的数量
      decrementItemAmount(player.getInventory().getItemInMainHand());


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

    player.sendMessage("§bMagic Legacy action could not be completed.");

  }else {

    player.sendMessage("§bMagic Legacy action could not be completed.");

  }
}
