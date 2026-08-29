function onUse(event) {
    const player = event.getPlayer();
    //检查主手是否持有物品
    if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
        sendMessage(player, "§bHold the item in your main hand.");
        return;
    }

    const invs = player.getInventory();
    const itemInMainHand = invs.getItemInMainHand();
    let world = player.getWorld();
    let eyeLocation = player.getEyeLocation();


    if (itemInMainHand.getAmount() > 1) {
        itemInMainHand.setAmount(itemInMainHand.getAmount() - 1);

        let ExperienceOrb =  world.spawn(eyeLocation, org.bukkit.entity.ExperienceOrb);
        ExperienceOrb.setCustomName("大量经验球");
        ExperienceOrb.setExperience(11111); // 设置经验值数量
        
        org.bukkit.Bukkit.broadcastMessage("§b"+player.getName()+"§bMagic Legacy action could not be completed.");

    } else {
        invs.setItemInMainHand(null); // 如果只剩下一个，则移除物品

        let ExperienceOrb =  world.spawn(eyeLocation, org.bukkit.entity.ExperienceOrb);
        ExperienceOrb.setCustomName("大量经验球");
        ExperienceOrb.setExperience(11111); // 设置经验值数量

        org.bukkit.Bukkit.broadcastMessage("§b"+player.getName()+"§bMagic Legacy action could not be completed.");
        
    }









}
