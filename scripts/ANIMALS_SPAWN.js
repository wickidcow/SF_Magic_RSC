const ENTITY_TYPES = [
  { id: "MAGIC_EGG_BEE", type: Java.type('org.bukkit.entity.EntityType').BEE },
  { id: "MAGIC_PIG_1", type: Java.type('org.bukkit.entity.EntityType').PIG },
  { id: "MAGIC_SHEEP_1", type: Java.type('org.bukkit.entity.EntityType').SHEEP },
  { id: "MAGIC_CHICKEN_1", type: Java.type('org.bukkit.entity.EntityType').CHICKEN },
  { id: "MAGIC_COW_1", type: Java.type('org.bukkit.entity.EntityType').COW },
  { id: "MAGIC_OCELOT_1", type: Java.type('org.bukkit.entity.EntityType').OCELOT },
  { id: "MAGIC_CAT_1", type: Java.type('org.bukkit.entity.EntityType').CAT },
  { id: "MAGIC_DONKEY_1", type: Java.type('org.bukkit.entity.EntityType').DONKEY },
  { id: "MAGIC_FOX_1", type: Java.type('org.bukkit.entity.EntityType').FOX },
  { id: "MAGIC_FROG_1", type: Java.type('org.bukkit.entity.EntityType').FROG },
  { id: "MAGIC_GOAT_1", type: Java.type('org.bukkit.entity.EntityType').GOAT },
  { id: "MAGIC_HOGLIN_1", type: Java.type('org.bukkit.entity.EntityType').HOGLIN },
  { id: "MAGIC_HORSE_1", type: Java.type('org.bukkit.entity.EntityType').HORSE },
  { id: "MAGIC_LLAMA_1", type: Java.type('org.bukkit.entity.EntityType').LLAMA },
  { id: "MAGIC_TRADER_LLAMA_1", type: Java.type('org.bukkit.entity.EntityType').TRADER_LLAMA },
  { id: "MAGIC_MOOSHROOM_1", type: Java.type('org.bukkit.entity.EntityType').MUSHROOM_COW },
  { id: "MAGIC_MULE_1", type: Java.type('org.bukkit.entity.EntityType').MULE },
  { id: "MAGIC_PANDA_1", type: Java.type('org.bukkit.entity.EntityType').PANDA },
  { id: "MAGIC_RABBIT_1", type: Java.type('org.bukkit.entity.EntityType').RABBIT },
  { id: "MAGIC_STRIDER_1", type: Java.type('org.bukkit.entity.EntityType').STRIDER },
  { id: "MAGIC_TURTLE_1", type: Java.type('org.bukkit.entity.EntityType').TURTLE },
  { id: "MAGIC_WOLF_1", type: Java.type('org.bukkit.entity.EntityType').WOLF }
];


function onUse(event) {

  let player = event.getPlayer();
  if(event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND){
    player.sendMessage("§bHold the item in your main hand.");
    return;
  }
  var onUseItem = event.getItem();
  var itemStack = getSfItemByItem(onUseItem);
  var sfitemid = itemStack.getId();
  // 查找匹配的实体配置
  let entityConfig = ENTITY_TYPES.find(entity => entity.id === sfitemid);

  if (entityConfig) {
    // 如果找到匹配项，则应用对应的 entityType 和 customName
    var entityTYPE = entityConfig.type;

  } else {
    return;
    // org.bukkit.Bukkit.broadcastMessage("未找到匹配的实体配置");
  }

  let item = event.getItem();
  item.setAmount(item.getAmount() - 1);
  
  let block = player.getTargetBlock(null, 1);
  let location = block.getLocation().add(0, 1, 0); 
  let world = location.getWorld();

  // world.spawnEntity(location, org.bukkit.entity.EntityType.WOLF);


  let entity = world.spawnEntity(location, entityTYPE);
  
  entity.setAge(-24000);



  
}
