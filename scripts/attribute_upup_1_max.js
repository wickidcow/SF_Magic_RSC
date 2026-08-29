function onUse(event) {
  let player = event.getPlayer();
  
  // 检查是否使用主手进行操作
  if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
    player.sendMessage("§bMagic Legacy action could not be completed.");
    return;
  }

  let offHandItem = player.getInventory().getItem(org.bukkit.inventory.EquipmentSlot.OFF_HAND);

  // 检查玩家是否按住Shift键
  if (player.isSneaking()) {
    updateLore(event);
  } else {
    upgradeOffHandArmor(event);
  }
}

function updateLore(event) {
  let player = event.getPlayer();
  let item = event.getItem();
  let itemMeta = item.getItemMeta();
  let lore = itemMeta.getLore() || [];
  
  // 更新描述以切换不同装备类型的增幅位置
  let newLore = lore.map((line) => {
    switch (line) {
      case "§bMagic Legacy action could not be completed.": player.sendMessage("§bMagic Legacy action could not be completed."); return "§bMagic Legacy action could not be completed.";
      case "§bMagic Legacy action could not be completed.": player.sendMessage("§bMagic Legacy action could not be completed."); return "§bMagic Legacy action could not be completed.";
      case "§bMagic Legacy action could not be completed.": player.sendMessage("§bMagic Legacy action could not be completed."); return "§bMagic Legacy action could not be completed.";
      case "§bMagic Legacy action could not be completed.": player.sendMessage("§bMagic Legacy action could not be completed."); return "§bMagic Legacy action could not be completed.";
      case "§bMagic Legacy action could not be completed.": player.sendMessage("§bMagic Legacy action could not be completed."); return "§bMagic Legacy action could not be completed.";
      case "§bMagic Legacy action could not be completed.": player.sendMessage("§bMagic Legacy action could not be completed."); return "§bMagic Legacy action could not be completed.";
      default: return line;
    }
  });
  
  // 应用新的描述到物品元数据
  itemMeta.setLore(newLore);
  item.setItemMeta(itemMeta);
}

function upgradeOffHandArmor(event) {
  let player = event.getPlayer();
  let offHandItem = player.getInventory().getItem(org.bukkit.inventory.EquipmentSlot.OFF_HAND);
  
  // 检查副手中是否有物品
  if (offHandItem === null || offHandItem.getType() === org.bukkit.Material.AIR) {
    player.sendMessage("§bMagic Legacy action could not be completed.");
    return;
  }

   // 检查物品堆叠数量是否为1
   if (offHandItem.getAmount() !== 1) {
    player.sendMessage("§bMagic Legacy action could not be completed.");
    return;
  }
  let item = event.getItem();
  let itemMeta = item.getItemMeta();
  let lore = itemMeta.getLore() || [];
  let equipmentType = null;
  let newLore = lore.map((line) => {
    switch (line) {
      case "§e§lShift + 右键切换增幅位置 : §c§l 头盔": case "§e§lShift + 右键切换增幅位置 : §c§l 胸甲": case "§e§lShift + 右键切换增幅位置 : §c§l 护腿": case "§e§lShift + 右键切换增幅位置 : §c§l 鞋子": case "§e§lShift + 右键切换增幅位置 : §c§l 主手": case "§e§lShift + 右键切换增幅位置 : §c§l 副手":
        equipmentType = line;  
        return line;
      default:
        return line;
    }
  });

  let equipmentSlot = getEquipmentSlot(equipmentType);

  // 强化副手物品
  let success = attemptUpgrade(player, offHandItem, event, equipmentSlot);
  if (success) {
    player.sendMessage("§bMagic Legacy action could not be completed.");
  } else {
    player.sendMessage("§bMagic Legacy action could not be completed.");
  }
}

function getEquipmentSlot(line) {
  switch (line) {
    case "§e§lShift + 右键切换增幅位置 : §c§l 头盔": return org.bukkit.inventory.EquipmentSlot.HEAD;
    case "§e§lShift + 右键切换增幅位置 : §c§l 胸甲": return org.bukkit.inventory.EquipmentSlot.CHEST;
    case "§e§lShift + 右键切换增幅位置 : §c§l 护腿": return org.bukkit.inventory.EquipmentSlot.LEGS;
    case "§e§lShift + 右键切换增幅位置 : §c§l 鞋子": return org.bukkit.inventory.EquipmentSlot.FEET;
    case "§e§lShift + 右键切换增幅位置 : §c§l 主手": return org.bukkit.inventory.EquipmentSlot.HAND;
    case "§e§lShift + 右键切换增幅位置 : §c§l 副手": return org.bukkit.inventory.EquipmentSlot.OFF_HAND;
    default: return null;
  }
}

// 添加或更新多个属性的方法
function attemptUpgrade(player, item, event1, equipmentSlot) {
  let itemMeta = item.getItemMeta();

  // 确保 itemMeta 不是 null 并且存在属性修饰器
  if (!itemMeta || !itemMeta.hasAttributeModifiers()) {
    addMultipleAttributes(item, player, equipmentSlot);
    return true;
  }
  // player.sendMessage("输出值："+ equipmentSlot);

  if (equipmentSlot == "HEAD"){

    var attributesToModify = [
      { key: org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH, 
        name: "生命值", 
        amount: 1, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
      },
      { key: org.bukkit.attribute.Attribute.GENERIC_ARMOR, 
        name: "护甲值", 
        amount: 0.2, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
      },
      { key: org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS, 
        name: "护甲韧性", 
        amount: 0.15, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
      },
      { 
        key: org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE, 
        name: "攻击伤害数值", 
        amount: 0.3, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
      }
    ];
    var attributeModifiers_head = itemMeta.getAttributeModifiers(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
    }else if (equipmentSlot == "HAND"){
    var attributesToModify = [
      
      // 攻击伤害数值增加（例如5点）
      { 
        key: org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE, 
        name: "攻击伤害数值", 
        amount: 1, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
      },
      
      // 攻击速度百分比增加（例如5%）
      { 
        key: org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED, 
        name: "攻击速度百分比", 
        amount: 0.03, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR 
      }
    ];
    var attributeModifiers_hand = itemMeta.getAttributeModifiers(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE);
  }
  else if (equipmentSlot == "OFF_HAND"){
    var attributesToModify = [
      
      // 攻击伤害数值增加（例如5点）
      { 
        key: org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE, 
        name: "攻击伤害数值", 
        amount: 1, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
      },
      
      // 移动速度百分比增加（例如5%）
      { 
        key: org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED, 
        name: "移动速度", 
        amount: 0.02, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR 
      }
    ];
    var attributeModifiers_offhand = itemMeta.getAttributeModifiers(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE);
  }
  else if (equipmentSlot == "CHEST"){
    var attributesToModify = [
      
      // 攻击伤害百分比增加（例如5点）
      { 
        key: org.bukkit.attribute.Attribute.GENERIC_KNOCKBACK_RESISTANCE, 
        name: "击退抗性", 
        amount: 0.01, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
      },
      // 生命数值增加
      { 
        key: org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH, 
        name: "生命值", 
        amount: 1.5, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
      },
      {
        key: org.bukkit.attribute.Attribute.GENERIC_ARMOR, 
        name: "护甲值", 
        amount: 0.6, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
      },
      {
        key: org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS, 
        name: "护甲韧性", 
        amount: 0.15, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
      }
    ];
    var attributeModifiers_chest = itemMeta.getAttributeModifiers(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
  }
  else if (equipmentSlot == "LEGS"){
    var attributesToModify = [
      
      // 攻击伤害数值增加（例如5点）
      { 
        key: org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE, 
        name: "攻击伤害数值", 
        amount: 0.1, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
      },
      {
        key: org.bukkit.attribute.Attribute.GENERIC_ARMOR, 
        name: "护甲值", 
        amount: 0.4, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
      },
      {
        key: org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS, 
        name: "护甲韧性", 
        amount: 0.15, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
      }
    ];
    var attributeModifiers_legs = itemMeta.getAttributeModifiers(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
  }
  else {
    var attributesToModify = [
      //靴子
      {
        key: org.bukkit.attribute.Attribute.GENERIC_ARMOR, 
        name: "护甲值", 
        amount: 0.2, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
      },
      {
        key: org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS, 
        name: "护甲韧性", 
        amount: 0.15, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
      },
      { key: org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED, 
        name: "移动速度", 
        amount: 0.02, 
        operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR 
      }
    ];
    var attributeModifiers_feets = itemMeta.getAttributeModifiers(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
  }
    let maxAmount = 0;
    let maxAmount2 = 0;
    let maxAmount3 = 0;
    let maxAmount4 = 0;
    let maxAmount5 = 0;
    let maxAmount6 = 0;
    if (attributeModifiers_head){
    maxAmount  = getMaxDamageAmount(attributeModifiers_head);
    }
    if (attributeModifiers_hand){
    maxAmount2 = getMaxDamageAmount(attributeModifiers_hand);
    }
    if (attributeModifiers_offhand){
      maxAmount3 = getMaxDamageAmount(attributeModifiers_offhand);
    }
    if (attributeModifiers_chest){
      maxAmount4 = getMaxDamageAmount(attributeModifiers_chest);
    }
    if (attributeModifiers_legs){
      maxAmount5 = getMaxDamageAmount(attributeModifiers_legs);
    }
    if (attributeModifiers_feets){
      maxAmount6 = getMaxDamageAmount(attributeModifiers_feets);
    }
    
    
    let upgradeChance = calculateUpgradeChance(maxAmount,maxAmount2,maxAmount3,maxAmount4,maxAmount5,maxAmount6,player);
    let randomChance = Math.random();

    if (randomChance < 2) {
  for (let attribute of attributesToModify) {
    incrementOrAddAttribute(item, player, attribute, equipmentSlot);
  }
  if(equipmentSlot == "HEAD"){
    player.sendMessage("§bMagic Legacy action could not be completed.");
    player.sendMessage("§bMagic Legacy action could not be completed."+ (1).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (0.2).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (0.15).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (0.3).toFixed(2));
  }else if(equipmentSlot == "HAND"){
    player.sendMessage("§bMagic Legacy action could not be completed.");
    player.sendMessage("§bMagic Legacy action could not be completed."+ (1).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (3).toFixed(2)+"%");
  }else if(equipmentSlot == "OFF_HAND"){
    player.sendMessage("§bMagic Legacy action could not be completed.");
    player.sendMessage("§bMagic Legacy action could not be completed."+ (1).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (2).toFixed(2)+"%");
  }else if(equipmentSlot == "CHEST"){
    player.sendMessage("§bMagic Legacy action could not be completed.");
    player.sendMessage("§bMagic Legacy action could not be completed."+ (1).toFixed(2)+"%");
    player.sendMessage("§bMagic Legacy action could not be completed."+ (1.5).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (0.6).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (0.15).toFixed(2));
  }else if(equipmentSlot == "LEGS"){
    player.sendMessage("§bMagic Legacy action could not be completed.");
    player.sendMessage("§bMagic Legacy action could not be completed."+ (0.4).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (0.15).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (0.1).toFixed(2));
  }else {
    player.sendMessage("§bMagic Legacy action could not be completed.");
    player.sendMessage("§bMagic Legacy action could not be completed."+ (0.4).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (0.15).toFixed(2));
    player.sendMessage("§bMagic Legacy action could not be completed."+ (2).toFixed(2)+"%");
  }

  decrementItemAmount(event1.getItem());
  return true;
}else {
  // 如果随机数大于等于升级成功的几率，则附魔失败
  // 销毁副手物品
  player.getInventory().setItemInOffHand(null);
  decrementItemAmount(event1.getItem());
  
  return false;

}

}


// 增加或更新单个属性的方法
function incrementOrAddAttribute(item, player, attribute, slot) {
  let itemMeta = item.getItemMeta();
  if (!itemMeta) {
    return; // 如果物品没有元数据，也直接返回
  }

  let existingModifiers = itemMeta.getAttributeModifiers(attribute.key);

  if (existingModifiers != null && !existingModifiers.isEmpty()) {
    // 如果属性已经存在，则增加现有属性的值
    let currentAmount = 0;
    for (let modifier of existingModifiers) {
      currentAmount += modifier.getAmount();
    }

    let newAmount = currentAmount + attribute.amount;

    updateSingleAttribute(item, { ...attribute, amount: newAmount }, slot);
  } else {
    // 如果属性不存在，则添加新属性
    addSingleAttribute(item, player, attribute, slot);
  }
}

// 添加单个属性的方法
function addSingleAttribute(item, player, attribute, slot) {
  let itemMeta = item.getItemMeta();
  if (!itemMeta) {
    return; // 如果物品没有元数据，也直接返回
  }

  let newModifier = new org.bukkit.attribute.AttributeModifier(
    java.util.UUID.randomUUID(), // 随机UUID
    attribute.name, // 属性名称
    attribute.amount, // 数值
    attribute.operation, // 运算方式
    slot // 槽位
  );
  itemMeta.addAttributeModifier(attribute.key, newModifier);

  // 将更新后的 itemMeta 应用回物品
  item.setItemMeta(itemMeta);

}

// 更新单个属性的方法
function updateSingleAttribute(item, attribute, slot) {
  let itemMeta = item.getItemMeta();
  if (!itemMeta) {
    return; // 如果物品没有元数据，也直接返回
  }

  // 移除旧的属性修饰器（如果有）
  itemMeta.getAttributeModifiers(attribute.key).forEach(modifier => {
    itemMeta.removeAttributeModifier(attribute.key, modifier);
  });

  // 添加新的属性修饰器
  let newModifier = new org.bukkit.attribute.AttributeModifier(
    java.util.UUID.randomUUID(), // 随机UUID
    attribute.name, // 属性名称
    attribute.amount, // 新的值
    attribute.operation, // 运算方式
    slot // 槽位
  );
  itemMeta.addAttributeModifier(attribute.key, newModifier);

  // 将更新后的 itemMeta 应用回物品
  item.setItemMeta(itemMeta);

}

// 减少物品数量的方法
function decrementItemAmount(item) {
  if (item && item.getAmount() > 1) {
    item.setAmount(item.getAmount() - 1);
  } else if (item) {
    item.setAmount(0); 
  }
}

// 计算附魔升级成功的几率
function calculateUpgradeChance(level,level2,level3,level4,level5,level6,player) {
  // 初始成功几率为0.99（即99%）
  let baseChance = 0.99;
  // player.sendMessage("level1,2"+level + "----" + level2)
  // 每5点数值降低的成功几率
  let decreasePerTenLevels = 0.01;
  if (level){
  // 每2点生命减少一次成功几率
  while (level >= 2) {
    baseChance -= decreasePerTenLevels;
    level -= 2;
  }
  }
  if(level2){
    // 每10点攻击减少一次成功几率
    while (level2 >= 10) {
      baseChance -= decreasePerTenLevels;
      level2 -= 10;
  }
}
  if(level3){
    // 每8点攻击减少一次成功几率
    while (level3 >= 8) {
      baseChance -= decreasePerTenLevels;
      level3 -= 8;
  }
}
  if(level4){
    // 每3点护甲值减少一次成功几率
    while (level4 >= 3) {
      baseChance -= decreasePerTenLevels;
      level4 -= 3;
  }
}
  if(level5){
    // 每1.2点护甲减少一次成功几率
    while (level5 >= 1.2) {
      baseChance -= decreasePerTenLevels;
      level5 -= 1.2;
  }
}
  if(level6){
    // 每0.6点护甲减少一次成功几率
    while (level6 >= 0.6) {
      baseChance -= decreasePerTenLevels;
      level6 -= 0.6;
  }
}
  // 返回计算后的成功几率
  return baseChance;
}



// 添加多个属性的方法
function addMultipleAttributes(item, player, equipmentSlot) {

  if (equipmentSlot == "HEAD"){

  var attributesToAdd = [
    { key: org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH, 
      name: "生命值", 
      amount: 1, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
    },
    { key: org.bukkit.attribute.Attribute.GENERIC_ARMOR, 
      name: "护甲值", 
      amount: 0.2, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
    },
    { key: org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS, 
      name: "护甲韧性", 
      amount: 0.15, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
    },
    { 
      key: org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE, 
      name: "攻击伤害数值", 
      amount: 0.3, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
    }
  ];
  }else if (equipmentSlot == "HAND"){
  var attributesToAdd = [
    
    // 攻击伤害数值增加（例如5点）
    { 
      key: org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE, 
      name: "攻击伤害数值", 
      amount: 1, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
    },
    
    // 攻击速度百分比增加（例如5%）
    { 
      key: org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED, 
      name: "攻击速度百分比", 
      amount: 0.03, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR 
    }
  ];
}
else if (equipmentSlot == "OFF_HAND"){
  var attributesToAdd = [
    
    // 攻击伤害数值增加（例如5点）
    { 
      key: org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE, 
      name: "攻击伤害百分比", 
      amount: 1, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
    },
    
    // 移动速度百分比增加（例如5%）
    { 
      key: org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED, 
      name: "移动速度", 
      amount: 0.02, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR 
    }
  ];
}
else if (equipmentSlot == "CHEST"){
  var attributesToAdd = [
    
    // 攻击伤害百分比增加（例如5点）
    { 
      key: org.bukkit.attribute.Attribute.GENERIC_KNOCKBACK_RESISTANCE, 
      name: "击退抗性", 
      amount: 0.01, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
    },
    // 生命百分比增加
    { 
      key: org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH, 
      name: "生命值", 
      amount: 1.5, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
    },
    {
      key: org.bukkit.attribute.Attribute.GENERIC_ARMOR, 
      name: "护甲值", 
      amount: 0.6, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
    },
    {
      key: org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS, 
      name: "护甲韧性", 
      amount: 0.15, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
    }
  ];
}
else if (equipmentSlot == "LEGS"){
  var attributesToAdd = [
    
    // 攻击伤害数值增加（例如5点）
    { 
      key: org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE, 
      name: "攻击伤害数值", 
      amount: 0.1, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER 
    },
    {
      key: org.bukkit.attribute.Attribute.GENERIC_ARMOR, 
      name: "护甲值", 
      amount: 0.4, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
    },
    {
      key: org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS, 
      name: "护甲韧性", 
      amount: 0.15, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
    }
  ];
}
else {
  var attributesToAdd = [
    //靴子
    {
      key: org.bukkit.attribute.Attribute.GENERIC_ARMOR, 
      name: "护甲值", 
      amount: 0.2, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
    },
    {
      key: org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS, 
      name: "护甲韧性", 
      amount: 0.15, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
    },
    { key: org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED, 
      name: "移动速度", 
      amount: 0.02, 
      operation: org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR 
    }
  ];
}



  for (let attribute of attributesToAdd) {
    addSingleAttribute(item, player, attribute, equipmentSlot);
  }
}


function getMaxDamageAmount(modifiers) {
  return modifiers.reduce((max, modifier) => Math.max(max, modifier.getAmount()), 0);
}

