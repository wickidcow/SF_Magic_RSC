
const sfIdGetter = (itemStack) => {
    return getSfItemByItem(itemStack).getId();
}

const MainHandIf = (hand) => {
    if (hand !== org.bukkit.inventory.EquipmentSlot.HAND) {
        return true;
    } else {
        return false;
    }
}


const modifyItemLore = (item, num) => {

    let itemNum = item.getAmount();
    if(itemNum!=1){
        return;
    }
    let itemMeta = item.getItemMeta();
    // 获取物品的Lore
    let lore = itemMeta.getLore() ? itemMeta.getLore() : [];
    // 复制0-4行
    let newLore = lore.slice(0, 4);
    let killNumLine = lore[4];
    let matchResult = killNumLine.match(/(\D*)(\d+)(\D*)/);
    if (matchResult) {
        let prefix = matchResult[1] || "";
        //原始灵魂数量
        let originnumber = parseInt(matchResult[2], 10);
        //灵魂数量+1
        let number = parseInt(matchResult[2], 10) + 100*num;
        let suffix = matchResult[3] || "";
        let updatedKillNumLine = `${prefix}${number}${suffix}`;
        newLore.push(updatedKillNumLine);
    } else {
        newLore.push(killNumLine);
    }
    newLore = newLore.concat(lore.slice(5));
    // org.bukkit.Bukkit.broadcastMessage("newLore:" + newLore);
    lore = newLore;
    itemMeta.setLore(lore);
    item.setItemMeta(itemMeta);

}


const decreaseItemInWhichHand =(item,num)=>{

    item.setAmount(item.getAmount() - num);

}

const onUseStart = (event) => {
    let player = event.getPlayer();

    if (MainHandIf(event.getHand())) {
        sendMessage(player, "§bPlease hold an item in your main hand");
        return;
    }

    //获取主手物品
    let item = player.getInventory().getItemInMainHand();
    // 检查副手堆叠
    let itemInOffHand = player.getInventory().getItemInOffHand();

    // 检查物品堆叠数量是否为1
    if (itemInOffHand.getAmount() !== 1) {
        player.sendMessage("§b不可以贪心哦~");
        return;
    }

    //副手物品sfid
    let offHandItemSfId = sfIdGetter(event.getItem());

    if (offHandItemSfId === "MAGIC_BANNER_SOUL"){
        let isShift = event.getPlayer().isSneaking()
        if(isShift){
            if (item.getAmount()===64){
            decreaseItemInWhichHand(item, 64);
            modifyItemLore(itemInOffHand, 64);
            player.sendMessage("§b注入成功~");
            return;
            }
        }
        decreaseItemInWhichHand(item, 1);
        modifyItemLore(itemInOffHand, 1);
        player.sendMessage("§b注入成功~");

    }



}




function onUse(event, itemStack) {

    onUseStart(event);

}





