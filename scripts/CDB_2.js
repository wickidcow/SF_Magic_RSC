function onUse(event, itemStack) {
    var player = event.getPlayer();
    //检查主手是否持有物品
    if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
        sendMessage(player, "§bHold the item in your main hand.");
        return;
    }

    


    // 检查副手是否持有有效且可充电的物品
    var itemInOffHand = player.getInventory().getItemInOffHand();
    
    if (!itemInOffHand || itemInOffHand.getType() === org.bukkit.Material.AIR) {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return ;
    }

    // 检查物品堆叠数量是否为1
    if (itemInOffHand.getAmount() !== 1) {
    player.sendMessage("§bMagic Legacy action could not be completed.");
    return;
  }
    


    let slimefunItem = getSfItemByItem(player.getInventory().getItemInOffHand());


    


    if (slimefunItem == null) {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }

    // 检查 getMaxItemCharge 和 getItemCharge 方法是否存在
    if (typeof slimefunItem.getMaxItemCharge !== 'function' || typeof slimefunItem.getItemCharge !== 'function') {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }

    var MAX_Charge = slimefunItem.getMaxItemCharge(itemInOffHand)

    var Now_Charge = slimefunItem.getItemCharge(itemInOffHand)

    if (!(MAX_Charge > 0)) {
        // 如果副手持有有效且可充电的物品，则继续执行其他逻辑
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }


    // sendMessage(player, "这个物品的最大电量为"+Judge_Charge);   //debug

    // if (slimefunItem == null) {
    //     // 如果副手持有有效且可充电的物品，则继续执行其他逻辑
    //     sendMessage(player, slimefunItem);
    // }

    var Need_Charge = MAX_Charge - Now_Charge ;

    var onUseItem = event.getItem();
    var itemCharge = itemStack.getItemCharge(onUseItem);
    var itemMaxCharge = itemStack.getMaxItemCharge(onUseItem);

    if (itemCharge <= Need_Charge*2){
        sendMessage(player, "§bMagic Legacy action could not be completed.");  
        sendMessage(player, "§bMagic Legacy action could not be completed."+itemCharge+"J§a/"+ itemMaxCharge +"J §b。"); 
        return;

    }


    if (Now_Charge < MAX_Charge){

        var RemoveCharge = Need_Charge*2;    //删除电量

        itemStack.removeItemCharge(onUseItem, RemoveCharge);

        slimefunItem.setItemCharge(itemInOffHand, MAX_Charge);

        sendMessage(player, "§bMagic Legacy action could not be completed.");
        sendMessage(player, "§bMagic Legacy action could not be completed."+ RemoveCharge + "§bMagic Legacy action could not be completed.");


        return;
    }


    
    sendMessage(player, "§bMagic Legacy action could not be completed.");



}    





