const Particle = Java.type('org.bukkit.Particle');

function onUse(event, itemStack){


    let player = event.getPlayer();
    //检查主手是否持有物品
    if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
        sendMessage(player, "§bHold the item in your main hand.");
        return;
    }
    
    var onUseItem = event.getItem();
    
    var Charge = itemStack.getItemCharge(onUseItem)

    if ( Charge < 250 ){
        player.sendMessage("§bNot enough charge. Please recharge the item.")
        return;
    }


    itemStack.removeItemCharge(onUseItem, 50);

    // 获取触发事件的玩家对象
    let world = player.getWorld();
    let eyeLocation = player.getEyeLocation();
    let direction = eyeLocation.getDirection();

    // 计算射线起点位置（眼睛位置向下0.8个单位并加上方向向量）
    let startLocation = eyeLocation.clone().subtract(0, 0, 0).add(direction);
    let maxDistance = 5;

    // 加载必要的 Java 类型
    var FluidCollisionMode = Java.type('org.bukkit.FluidCollisionMode');

    // 执行射线追踪
    let rayTraceResults = world.rayTrace(startLocation, direction, maxDistance, FluidCollisionMode.ALWAYS, true, 0, null);

    generateParticleBeam(world, startLocation, direction, maxDistance);

    if (rayTraceResults == null) {
        player.sendMessage("§bAim at a mob.")
        return;
    }

    

    let entity = rayTraceResults.getHitEntity();

    if (entity == null){
        return;
    }
   
    if(!entity.isAdult()){
        player.sendMessage("§bYou cannot capture baby mobs.")
    }
    
    if (entity.getHealth() <= 0) {
        sendMessage(player, "§bYou cannot capture a dead mob.");
        sendMessage(player, "§bNice try!");
    
    return;

    }

    if (entity instanceof org.bukkit.entity.Allay) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);


        let selectedItem = "MAGIC_ALLAY_1";

        let slimefunItem = getSfItemById(selectedItem);

        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);

        return;
        
    }

    if (entity instanceof org.bukkit.entity.Bee && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);


        let selectedItem = "MAGIC_EGG_BEE";

        let slimefunItem = getSfItemById(selectedItem);

        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);

        return;
        
    }

    if (entity instanceof org.bukkit.entity.Cat && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);


        let selectedItem = "MAGIC_CAT_1";

        let slimefunItem = getSfItemById(selectedItem);

        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);

        return;
        
    }


    if (entity instanceof org.bukkit.entity.Chicken && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);


        let selectedItem = "MAGIC_CHICKEN_1";

        let slimefunItem = getSfItemById(selectedItem);

        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);

        return;
        
    }

    if (entity instanceof org.bukkit.entity.Cow && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);


        let selectedItem = "MAGIC_COW_1";

        let slimefunItem = getSfItemById(selectedItem);

        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);

        return;
        
    }

    if (entity instanceof org.bukkit.entity.Donkey && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);
  
  
        let selectedItem = "MAGIC_DONKEY_1";
  
        let slimefunItem = getSfItemById(selectedItem);
  
        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());
  
        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);
  
        return;
        
    }

  
    if (entity instanceof org.bukkit.entity.Fox && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);
  
  
        let selectedItem = "MAGIC_FOX_1";
  
        let slimefunItem = getSfItemById(selectedItem);
  
        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());
  
        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);
  
        return;
        
    }


  if (entity instanceof org.bukkit.entity.Frog && entity.isAdult()) {
      entity.remove();
    
      itemStack.removeItemCharge(onUseItem, 250);


      let selectedItem = "MAGIC_FROG_1";

      let slimefunItem = getSfItemById(selectedItem);

      let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

      let location = entity.getLocation();
      world.dropItemNaturally(location, itemstack);

      return;
      
  }

if (entity instanceof org.bukkit.entity.Goat && entity.isAdult()) {
    entity.remove();
  
    itemStack.removeItemCharge(onUseItem, 250);


    let selectedItem = "MAGIC_GOAT_1";

    let slimefunItem = getSfItemById(selectedItem);

    let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

    let location = entity.getLocation();
    world.dropItemNaturally(location, itemstack);

    return;
}


  if (entity instanceof org.bukkit.entity.Hoglin && entity.isAdult()) {
      entity.remove();
    
      itemStack.removeItemCharge(onUseItem, 250);


      let selectedItem = "MAGIC_HOGLIN_1";

      let slimefunItem = getSfItemById(selectedItem);

      let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

      let location = entity.getLocation();
      world.dropItemNaturally(location, itemstack);

      return;
  }



  if (entity instanceof org.bukkit.entity.Horse && entity.isAdult()) {
      entity.remove();
    
      itemStack.removeItemCharge(onUseItem, 250);


      let selectedItem = "MAGIC_HORSE_1";

      let slimefunItem = getSfItemById(selectedItem);

      let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

      let location = entity.getLocation();
      world.dropItemNaturally(location, itemstack);

      return;
  } 


if (entity instanceof org.bukkit.entity.Llama && entity.isAdult()) {
    entity.remove();
  
    itemStack.removeItemCharge(onUseItem, 250);


    let selectedItem = "MAGIC_LLAMA_1";

    let slimefunItem = getSfItemById(selectedItem);

    let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

    let location = entity.getLocation();
    world.dropItemNaturally(location, itemstack);

    return;
}


if (entity instanceof org.bukkit.entity.MushroomCow && entity.isAdult()) {
    entity.remove();
  
    itemStack.removeItemCharge(onUseItem, 250);


    let selectedItem = "MAGIC_MOOSHROOM_1";

    let slimefunItem = getSfItemById(selectedItem);

    let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

    let location = entity.getLocation();
    world.dropItemNaturally(location, itemstack);

    return;
}

if (entity instanceof org.bukkit.entity.Mule && entity.isAdult()) {
    entity.remove();
  
    itemStack.removeItemCharge(onUseItem, 250);


    let selectedItem = "MAGIC_MULE_1";

    let slimefunItem = getSfItemById(selectedItem);

    let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

    let location = entity.getLocation();
    world.dropItemNaturally(location, itemstack);

    return;
}

if (entity instanceof org.bukkit.entity.Ocelot && entity.isAdult()) {
    entity.remove();
  
    itemStack.removeItemCharge(onUseItem, 250);


    let selectedItem = "MAGIC_OCELOT_1";

    let slimefunItem = getSfItemById(selectedItem);

    let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

    let location = entity.getLocation();
    world.dropItemNaturally(location, itemstack);

    return;
}



if (entity instanceof org.bukkit.entity.Panda && entity.isAdult()) {
    entity.remove();
  
    itemStack.removeItemCharge(onUseItem, 250);


    let selectedItem = "MAGIC_PANDA_1";

    let slimefunItem = getSfItemById(selectedItem);

    let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

    let location = entity.getLocation();
    world.dropItemNaturally(location, itemstack);

    return;
    
}

if (entity instanceof org.bukkit.entity.Pig && entity.isAdult()) {
    entity.remove();
  
    itemStack.removeItemCharge(onUseItem, 250);


    let selectedItem = "MAGIC_PIG_1";

    let slimefunItem = getSfItemById(selectedItem);

    let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

    let location = entity.getLocation();
    world.dropItemNaturally(location, itemstack);

    return;
    
}


    if (entity instanceof org.bukkit.entity.Rabbit && entity.isAdult()) {
    entity.remove();
  
    itemStack.removeItemCharge(onUseItem, 250);


    let selectedItem = "MAGIC_RABBIT_1";

    let slimefunItem = getSfItemById(selectedItem);

    let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

    let location = entity.getLocation();
    world.dropItemNaturally(location, itemstack);

    return;
    
    }



    if (entity instanceof org.bukkit.entity.Sheep && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);


        let selectedItem = "MAGIC_SHEEP_1";

        let slimefunItem = getSfItemById(selectedItem);

        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);

        return;
        
    }
    
  
    if (entity instanceof org.bukkit.entity.Strider && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);
  
  
        let selectedItem = "MAGIC_STRIDER_1";
  
        let slimefunItem = getSfItemById(selectedItem);
  
        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());
  
        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);
  
        return;
    }


  
    if (entity instanceof org.bukkit.entity.TraderLlama && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);
  
  
        let selectedItem = "MAGIC_TRADER_LLAMA_1";
  
        let slimefunItem = getSfItemById(selectedItem);
  
        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());
  
        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);
  
        return;
    }
    

  
    if (entity instanceof org.bukkit.entity.Turtle && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);
  
  
        let selectedItem = "MAGIC_TURTLE_1";
  
        let slimefunItem = getSfItemById(selectedItem);
  
        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());
  
        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);
  
        return;
        
    }



  
    if (entity instanceof org.bukkit.entity.Wolf && entity.isAdult()) {
        entity.remove();
      
        itemStack.removeItemCharge(onUseItem, 250);
  
  
        let selectedItem = "MAGIC_WOLF_1";
  
        let slimefunItem = getSfItemById(selectedItem);
  
        let itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());
  
        let location = entity.getLocation();
        world.dropItemNaturally(location, itemstack);
  
        return;
    }

    player.sendMessage("§bMagic Legacy action could not be completed.")

}


// 生成粒子效果
function generateParticleBeam(world, startLocation, direction, maxDistance) {
    const PARTICLE_TYPE = Particle.VILLAGER_HAPPY; // 使用绿色粒子（农作物催熟）作为示例
    const PARTICLE_INTERVAL = 0.1; // 粒子之间的间隔距离

    for (let distance = 0; distance <= maxDistance; distance += PARTICLE_INTERVAL) {
        let particleLocation = startLocation.clone().add(direction.clone().multiply(distance));
        world.spawnParticle(PARTICLE_TYPE, particleLocation.getX(), particleLocation.getY(), particleLocation.getZ(), 0);
    }
}
