function onUse(event, itemStack) {
    var player = event.getPlayer();
    //检查主手是否持有物品
    if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
        sendMessage(player, "Please hold an item in your main hand");
        return;
    }

    


    // 检查副手是否持有有效且可充电的物品
    var itemInOffHand = player.getInventory().getItemInOffHand();
    
    if (!itemInOffHand || itemInOffHand.getType() === org.bukkit.Material.AIR) {
        sendMessage(player, "You must hold an item in your off hand");
        return ;
    }

    // 检查物品堆叠数量是否为1
    if (itemInOffHand.getAmount() !== 1) {
    player.sendMessage("§b一次只能给一件物品充电哦~");
    return;
  }
    
    var onUseItem = event.getItem();
    var itemCharge = itemStack.getItemCharge(onUseItem);
    var itemMaxCharge = itemStack.getMaxItemCharge(onUseItem);

    if (itemCharge < itemMaxCharge*0.5){
        sendMessage(player, "§b充电宝电量不足§c50%§b，无法进行充电！");  
        sendMessage(player, "§b当前电量剩余：§c"+itemCharge+"J§a/"+ itemMaxCharge +"J §b。"); 
        return;

    }


    let slimefunItem = getSfItemByItem(player.getInventory().getItemInOffHand());


    


    if (slimefunItem == null) {
        // 如果副手持有有效且可充电的物品，则继续执行其他逻辑
        sendMessage(player, "§bThis item cannot be recharged");
        return;
    }

    // 检查 getMaxItemCharge 和 getItemCharge 方法是否存在
    if (typeof slimefunItem.getMaxItemCharge !== 'function' || typeof slimefunItem.getItemCharge !== 'function') {
        sendMessage(player, "§b当前物品不支持电量操作，请检查物品或插件版本。");
        return;
    }

    var MAX_Charge = slimefunItem.getMaxItemCharge(itemInOffHand)

    var Now_Charge = slimefunItem.getItemCharge(itemInOffHand)

    if (!(MAX_Charge > 0)) {
        // 如果副手持有有效且可充电的物品，则继续执行其他逻辑
        sendMessage(player, "§bThis item cannot be recharged");
        return;
    }


    // sendMessage(player, "这个物品的最大电量为"+Judge_Charge);   //debug

    // if (slimefunItem == null) {
    //     // 如果副手持有有效且可充电的物品，则继续执行其他逻辑
    //     sendMessage(player, slimefunItem);
    // }



    if (Now_Charge < MAX_Charge){

        var RemoveCharge = Math.floor(Math.random() * 500000);    //删除电量

        itemStack.removeItemCharge(onUseItem, RemoveCharge);

        slimefunItem.setItemCharge(itemInOffHand, MAX_Charge);

        sendMessage(player, "§b物品已经充满电啦~");
        sendMessage(player, "§b本次充电总共消耗§e "+ RemoveCharge + "J §b电量~");


        return;
    }


    
    sendMessage(player, "§b你手中的物品电量已经满啦！充不进去啦~");



}    





