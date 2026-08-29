let Entity_type = org.bukkit.entity.EntityType.CREEPER;

let customName = "§a§l§kl§a§l魔法-人造苦力怕§a§l§al"; // 自定义名称
const ARTIFICIAL_ENTITIES = [
  { id: "MAGIC_EGG_IRON_GOLEM", entityType: Java.type('org.bukkit.entity.EntityType').IRON_GOLEM, customName: "§f§l§kl§f§l魔法-人造铁傀儡§f§l§kl" },
  { id: "MAGIC_ARTIFICIAL_WITHER_SKELETON", entityType: Java.type('org.bukkit.entity.EntityType').WITHER_SKELETON, customName: "§c§l§kl§c§l魔法-人造凋零骷髅§c§l§kl" },
  { id: "MAGIC_ARTIFICIAL_SKELETON", entityType: Java.type('org.bukkit.entity.EntityType').SKELETON, customName: "§f§l§kl§f§l魔法-人造骷髅§f§l§kl" },
  { id: "MAGIC_ARTIFICIAL_CREEPER", entityType: Java.type('org.bukkit.entity.EntityType').CREEPER, customName: "§a§l§kl§a§l魔法-人造苦力怕§a§l§al" },
  { id: "MAGIC_ARTIFICIAL_ZOMBIE", entityType: Java.type('org.bukkit.entity.EntityType').ZOMBIE, customName: "§a§l§kl§a§l魔法-人造僵尸§a§l§kl" },
  { id: "MAGIC_ARTIFICIAL_GIANT", entityType: Java.type('org.bukkit.entity.EntityType').GIANT, customName: "§a§l§kl§a§l魔法-人造巨人§a§l§kl" },
  { id: "MAGIC_ALLAY_1", entityType: Java.type('org.bukkit.entity.EntityType').ALLAY, customName: "" }
];

function onUse(event, itemStack) {

  let player = event.getPlayer();
  if(event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND){
    player.sendMessage("§bHold the item in your main hand.");
    return;
  }
  
  var onUseItem = event.getItem();
  var itemStack = getSfItemByItem(onUseItem);
  var sfitemid = itemStack.getId();
  // 查找匹配的人造实体配置
  let entityConfig = ARTIFICIAL_ENTITIES.find(entity => entity.id === sfitemid);

  if (entityConfig) {
    // 如果找到匹配项，则应用对应的 entityType 和 customName
    var Entity_type = entityConfig.entityType;
    var customName = entityConfig.customName;

  } else {
    return;
    // org.bukkit.Bukkit.broadcastMessage("未找到匹配的人造实体配置");
  }
  

  
  let item = event.getItem();
  item.setAmount(item.getAmount() - 1);
  
  let block = player.getTargetBlock(null, 5);
  let location = block.getLocation().add(0, 1, 0); 
  let world = location.getWorld();

  let entity = world.spawnEntity(location, Entity_type);

  // 设置自定义名称
  entity.setCustomName(customName);
  // 使自定义名称在游戏中可见
  entity.setCustomNameVisible(true);




  // let WITHER_SKELETONClass = Java.type('org.bukkit.entity.EntityType').WITHER_SKELETON;

  // let WITHER_SKELETON = world.spawnEntity(location, WITHER_SKELETONClass);
  
  // WITHER_SKELETON.setAge(-24000);



  
}
