function onUse(event) {
    let player = event.getPlayer();
    let mainHandItem = player.getInventory().getItemInMainHand();
    let offHandItem = player.getInventory().getItemInOffHand();

    // 检查主手是否持有指定的强化书
    if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
        sendMessage(player, "§bHold the item in your main hand.");
        return;
    }

    // 检查副手是否有物品
    if (offHandItem === null) {
        player.sendMessage("§bMagic Legacy action could not be completed.");
        return;
    }



    // 添加不可破坏属性到副手物品
    let itemMeta = offHandItem.getItemMeta();
    if (itemMeta) {
        itemMeta.setUnbreakable(true);
        offHandItem.setItemMeta(itemMeta);
        sendMessage(player, "§bMagic Legacy action could not be completed.");

        let world = player.getWorld();
        let eyeLocation = player.getEyeLocation();
        let LightningStrike =  world.spawn(eyeLocation, org.bukkit.entity.LightningStrike);     //闪电
        let LightningStrike2 =  world.spawn(eyeLocation, org.bukkit.entity.LightningStrike);    
        let LightningStrike3 =  world.spawn(eyeLocation, org.bukkit.entity.LightningStrike);     
        let LightningStrike4 =  world.spawn(eyeLocation, org.bukkit.entity.LightningStrike);     
        let LightningStrike5 =  world.spawn(eyeLocation, org.bukkit.entity.LightningStrike); 
        let LightningStrike6 =  world.spawn(eyeLocation, org.bukkit.entity.LightningStrike); 
        let LightningStrike7 =  world.spawn(eyeLocation, org.bukkit.entity.LightningStrike); 
        let LightningStrike8 =  world.spawn(eyeLocation, org.bukkit.entity.LightningStrike); 
        let LightningStrike9 =  world.spawn(eyeLocation, org.bukkit.entity.LightningStrike); 
        let LightningStrike0 =  world.spawn(eyeLocation, org.bukkit.entity.LightningStrike);     
        LightningStrike.setCustomName("雷劫");
        LightningStrike2.setCustomName("雷劫");
        LightningStrike3.setCustomName("雷劫");
        LightningStrike4.setCustomName("雷劫");
        LightningStrike5.setCustomName("雷劫");
        LightningStrike6.setCustomName("雷劫");
        LightningStrike7.setCustomName("雷劫");
        LightningStrike8.setCustomName("雷劫");
        LightningStrike9.setCustomName("雷劫");
        LightningStrike0.setCustomName("雷劫");

        // 减少主手物品的数量
        decrementItemAmount(player.getInventory().getItemInMainHand());

        player.getInventory().setItemInMainHand(mainHandItem);
    } else {
        player.sendMessage("§bMagic Legacy action could not be completed.");
    }

}

function decrementItemAmount(item) {
    if (item && item.getAmount() > 1) {
      item.setAmount(item.getAmount() - 1);
    } else if (item) {
      item.setAmount(0);
    }
  }
  

