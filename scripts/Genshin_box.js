const GiftArray = Array.from({ length: 27 }, function (_, index) {
    return "MAGIC_GENSHIN_IMPACT_" + (index + 1);
});
//这里定义27个原神盲盒物品

function onUse(event) {
    const player = event.getPlayer();
    //检查主手是否持有物品
    if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
        sendMessage(player, "§bHold the item in your main hand.");
        return;
    }

    const invs = player.getInventory();

    const itemInMainHand = invs.getItemInMainHand();

    // 随机选择一项物品
    const selectedItem = GiftArray[Math.floor(Math.random() * GiftArray.length)];

    const slimefunItem = getSfItemById(selectedItem);
    const itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());
    itemstack.setAmount(1);

    if (invs.firstEmpty() === -1) {
        player.getWorld().dropItemNaturally(player.getLocation(), itemstack);
        sendMessage(player, "§bMagic Legacy action could not be completed.");
    } else {
        invs.addItem(itemstack);
        sendMessage(player, "§bMagic Legacy action could not be completed." + itemstack.getItemMeta().getDisplayName());
    }




    // 减少主手物品的数量
    decrementItemAmount(itemInMainHand);


}




//减少物品
function decrementItemAmount(item) {
    if (item && item.getAmount() > 1) {
      item.setAmount(item.getAmount() - 1);
    } else if (item) {
      item.setAmount(0);
    }
  }
  
