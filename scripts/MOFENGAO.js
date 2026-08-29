// 定义矿粉及其等概率分布
const dustArray = [
    "IRON_DUST", "GOLD_DUST", "TIN_DUST", "COPPER_DUST",
    "SILVER_DUST", "LEAD_DUST", "ALUMINUM_DUST", "ZINC_DUST", "MAGNESIUM_DUST"
];

const giftArray = [
    "FILLED_FLASK_OF_KNOWLEDGE","DAMASCUS_STEEL_INGOT","MAGIC_REDSTONE","DAMASCUS_STEEL_INGOT","STEEL_INGOT",
    "NETHER_ICE","NEPTUNIUM","REDSTONE_ALLOY","REINFORCED_ALLOY_INGOT","HARDENED_METAL_INGOT",

];

let usageCount = 0;

function onUse(event) {
    const player = event.getPlayer();
    //检查主手是否持有物品
    if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
        sendMessage(player, "§bHold the item in your main hand.");
        return;
    }
    let item = player.getInventory().getItemInMainHand();
    let itemMeta = item.getItemMeta();

    const invs = player.getInventory();
    const itemInMainHand = invs.getItemInMainHand();
    const itemInOffHand = invs.getItemInOffHand(); // 确保这里定义了 itemInOffHand

    // 检查主手是否持有钻石稿
    if (!itemInMainHand || itemInMainHand.getType() !== org.bukkit.Material.DIAMOND_PICKAXE) {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }

    // 检查副手是否持有钻石
    if (itemInOffHand && itemInOffHand.getType() == org.bukkit.Material.DIAMOND ) {

        if(usageCount <250){
            sendMessage(player, "§bMagic Legacy action could not be completed.");
            return;
        }
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        sendMessage(player, "§bMagic Legacy action could not be completed.");

        let eyeLocation = player.getEyeLocation();
        let world = player.getWorld();


        let ExperienceOrb =  world.spawn(eyeLocation, org.bukkit.entity.ExperienceOrb);
        ExperienceOrb.setCustomName("精验球");
        ExperienceOrb.setExperience(100); // 设置经验值数量

        let durability = itemInMainHand.getDurability();

        itemInMainHand.setDurability(durability-30);
        
        if (itemInOffHand.getAmount() > 1) {
            itemInOffHand.setAmount(itemInOffHand.getAmount() - 1);
    
            usageCount = usageCount - 250 ;
    
        } else {
            invs.setItemInOffHand(null); // 如果只剩下一个，则移除物品
    
            usageCount = usageCount - 250 ;
            
        }

        const selectedItem = giftArray[Math.floor(Math.random() * dustArray.length)];

        const slimefunItem = getSfItemById(selectedItem);
        const itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());
        itemstack.setAmount(1);


        if (invs.firstEmpty() === -1) {
        player.getWorld().dropItemNaturally(player.getLocation(), itemstack);
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        } else {
        invs.addItem(itemstack);
        sendMessage(player, "§bMagic Legacy action could not be completed." + itemstack.getItemMeta().getDisplayName() + " §b*1");
        }        

        return;
        
    }




    // 检查副手是否持有圆石
    if (!itemInOffHand || itemInOffHand.getType() !== org.bukkit.Material.COBBLESTONE) {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }

    // 减少钻石稿1点耐久
    let durability = itemInMainHand.getDurability();
    if (durability < itemInMainHand.getType().getMaxDurability() - 1) {
        // 减少副手圆石1个
    if (itemInOffHand.getAmount() > 1) {
        itemInOffHand.setAmount(itemInOffHand.getAmount() - 1);

        usageCount++;
        const lore = itemMeta.getLore().slice(0, -1);
        lore.push("§e你已成功磨粉:" + usageCount + "次"); // 设置物品描述
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);

    } else {
        invs.setItemInOffHand(null); // 如果只剩下一个，则移除物品

        usageCount++;
        const lore = itemMeta.getLore().slice(0, -1);
        lore.push("§e你已成功磨粉:" + usageCount + "次"); // 设置物品描述
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);

    }
        itemInMainHand.setDurability(durability + 1);
        invs.setItemInMainHand(itemInMainHand);
    } else {
        // 如果耐久达到上限，则销毁工具
        invs.setItemInMainHand(null);
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }

    // 随机选择一种矿粉
    const selectedItem = dustArray[Math.floor(Math.random() * dustArray.length)];

    const slimefunItem = getSfItemById(selectedItem);
    const itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());
    itemstack.setAmount(1);

    if (invs.firstEmpty() === -1) {
        player.getWorld().dropItemNaturally(player.getLocation(), itemstack);
        sendMessage(player, "§bMagic Legacy action could not be completed.");
    } else {
        invs.addItem(itemstack);
        sendMessage(player, "§bMagic Legacy action could not be completed." + itemstack.getItemMeta().getDisplayName() + " §b*1");
    }
}




