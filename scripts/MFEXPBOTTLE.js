function onUse(event) {
    const player = event.getPlayer();
    //检查主手是否持有物品
    if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
        sendMessage(player, "Please hold an item in your main hand");
        return;
    }

    if(player.getLevel() < 100){
        sendMessage(player, "&c你的经验不足100");
        return;
    }

    const invs = player.getInventory();
    const itemInMainHand = invs.getItemInMainHand();

    const selectedItem = "MAGIC_EXP_BOTTLE"

    const slimefunItem = getSfItemById(selectedItem);
    const itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem().getType());
    itemstack.setItemMeta(slimefunItem.getItem().getItemMeta());
    itemstack.setAmount(1);

    

    if (itemInMainHand.getAmount() > 1) {
        itemInMainHand.setAmount(itemInMainHand.getAmount() - 1);

        player.setLevel(player.getLevel() - 100);
        
        if (invs.firstEmpty() === -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), itemstack);
            sendMessage(player, "&bExperience was successfully infused into the Bottle of Magical Knowledge");
            sendMessage(player, "&b背包已满，物品已掉落在地面上");
        } else {
            invs.addItem(itemstack);
            sendMessage(player, "&bExperience was successfully infused into the Bottle of Magical Knowledge");
            sendMessage(player, "&b成功获得物品 " + itemstack.getItemMeta().getDisplayName() + " *1");
        }
    

    } else {
        invs.setItemInMainHand(null); // 如果只剩下一个，则移除物品

        player.setLevel(player.getLevel() - 100);

        if (invs.firstEmpty() === -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), itemstack);
            sendMessage(player, "&bExperience was successfully infused into the Bottle of Magical Knowledge");
            sendMessage(player, "&b背包已满，物品已掉落在地面上");
        } else {
            invs.addItem(itemstack);
            sendMessage(player, "&bExperience was successfully infused into the Bottle of Magical Knowledge");
            sendMessage(player, "&b成功获得物品 " + itemstack.getItemMeta().getDisplayName() + " *1");
        }

        
    }







}