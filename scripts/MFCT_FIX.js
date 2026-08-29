const storageUnits = [
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_1", sfid_Fix: "MAGIC_STORE_FIX_1" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_2", sfid_Fix: "MAGIC_STORE_FIX_2" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_3", sfid_Fix: "MAGIC_STORE_FIX_3" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_4", sfid_Fix: "MAGIC_STORE_FIX_4" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_5", sfid_Fix: "MAGIC_STORE_FIX_5" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_6", sfid_Fix: "MAGIC_STORE_FIX_6" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_7", sfid_Fix: "MAGIC_STORE_FIX_7" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_8", sfid_Fix: "MAGIC_STORE_FIX_8" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_9", sfid_Fix: "MAGIC_STORE_FIX_9" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_10", sfid_Fix: "MAGIC_STORE_FIX_10" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_11", sfid_Fix: "MAGIC_STORE_FIX_11" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_12", sfid_Fix: "MAGIC_STORE_FIX_12" },
    { sfid_Store: "NTW_EXPANSION_CARGO_STORAGE_UNIT_13", sfid_Fix: "MAGIC_STORE_FIX_13" }
];
function onUse(event, itemStack) {
    var player = event.getPlayer();
    //检查主手是否持有物品
    if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
        sendMessage(player, "§bHold the item in your main hand.");
        return;
    }



    // 检查副手是否持有有效且可充电的物品
    var itemInOffHand = player.getInventory().getItemInOffHand();
    var itemInMainHand = player.getInventory().getItemInMainHand();
    
    if (!itemInOffHand || itemInOffHand.getType() === org.bukkit.Material.AIR) {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return ;
    }

    // let item1 = event.getInOffHand();
    // player.sendMessage("itemInOffHand: " + itemInOffHand);


    let slimefunItem = getSfItemByItem(player.getInventory().getItemInOffHand());
    // player.sendMessage("slimefunItem: " + slimefunItem);
    let target_sfid_Store = slimefunItem.getId();

    let slimefunItem2 = getSfItemByItem(player.getInventory().getItemInMainHand());
    let target_sfid_Fix = slimefunItem2.getId();
    // player.sendMessage("target_sfid_Store: " + target_sfid_Store);
    // player.sendMessage("target_sfid_Fix: " + target_sfid_Fix);

    
    const found = storageUnits.some(unit => 
        unit.sfid_Store === target_sfid_Store && unit.sfid_Fix === target_sfid_Fix
    );
    
    if (!found) {
        // console.log("未找到匹配的 sfid_Store 和 sfid_Fix 组合，程序将返回。");
        return; // 如果没有找到匹配项，则直接返回
    }

    
    // player.sendMessage("sfid: " + sfid);

    // 使用 includes 方法检查 sfid 是否存在于 CARGO_STORAGE_UNITS 中
    // if (!(CARGO_STORAGE_UNITS.includes(sfid))) {
    //     player.sendMessage("不是对应的修复品");
    //     return;
    // }

    //只需添加对应判定即可，主手副手，以及所有物品，并且重写魔法抽屉配方

    // 检查物品堆叠数量是否为1
    if (itemInOffHand.getAmount() !== 1) {
        player.sendMessage("§bMagic Legacy action could not be completed.");
        return;
      }


    let amount = 1;
    decreaseItemInWhichHand(itemInMainHand, amount);


    // 定义一个包含方块信息的对象数组
const blocksArray = [
    { material_id: org.bukkit.Material.STONE, material_name: "石头" },
    { material_id: org.bukkit.Material.GRASS_BLOCK, material_name: "草方块" },
    { material_id: org.bukkit.Material.DIRT, material_name: "泥土" },
    { material_id: org.bukkit.Material.COBBLESTONE, material_name: "圆石" },
    { material_id: org.bukkit.Material.OAK_WOOD, material_name: "橡木" },
    { material_id: org.bukkit.Material.SANDSTONE, material_name: "沙石" },
    { material_id: org.bukkit.Material.BRICKS, material_name: "砖块" },
    { material_id: org.bukkit.Material.MOSSY_COBBLESTONE, material_name: "苔藓圆石" },
    { material_id: org.bukkit.Material.OBSIDIAN, material_name: "黑曜石" },
    { material_id: org.bukkit.Material.IRON_ORE, material_name: "铁矿石" },
    { material_id: org.bukkit.Material.GOLD_ORE, material_name: "金矿石" },
    { material_id: org.bukkit.Material.DIAMOND_ORE, material_name: "钻石矿石" },
    { material_id: org.bukkit.Material.EMERALD_ORE, material_name: "绿宝石矿石" },
    { material_id: org.bukkit.Material.RED_SANDSTONE, material_name: "红沙石" },
    { material_id: org.bukkit.Material.END_STONE, material_name: "末地石" },
    { material_id: org.bukkit.Material.NETHERRACK, material_name: "地狱岩" },
    { material_id: org.bukkit.Material.QUARTZ_BLOCK, material_name: "石英块" },
    { material_id: org.bukkit.Material.COAL_ORE, material_name: "煤矿石" },
    { material_id: org.bukkit.Material.FURNACE, material_name: "熔炉" },
    { material_id: org.bukkit.Material.CHEST, material_name: "箱子" },
    { material_id: org.bukkit.Material.BOOKSHELF, material_name: "书架" },
    { material_id: org.bukkit.Material.CRAFTING_TABLE, material_name: "工作台" },
    { material_id: org.bukkit.Material.FURNACE, material_name: "熔炉" },
    { material_id: org.bukkit.Material.ENCHANTING_TABLE, material_name: "附魔台" },
    { material_id: org.bukkit.Material.ANVIL, material_name: "铁砧" },
    { material_id: org.bukkit.Material.BEACON, material_name: "信标" },
    { material_id: org.bukkit.Material.DAYLIGHT_DETECTOR, material_name: "日光感应器" },
    { material_id: org.bukkit.Material.HOPPER, material_name: "漏斗" },
    { material_id: org.bukkit.Material.DISPENSER, material_name: "发射器" },
    { material_id: org.bukkit.Material.DROPPER, material_name: "投掷器" }
];

    // 计算随机索引
    const randomIndex = Math.floor(Math.random() * blocksArray.length);

    // 获取随机项
    const selectedBlock = blocksArray[randomIndex];

    // 返回结果对象
    let material_id = selectedBlock.material_id;
    let material_name = selectedBlock.material_name;
    
    modifyOffHandItem(itemInOffHand, material_id, material_name);
    modifyOffHandItem(itemInOffHand, material_id, material_name);


    let world = player.getWorld();

    let eyeLocation = player.getEyeLocation();
    playIgniteTNTSound(world, eyeLocation);



}    

function decreaseItemInWhichHand(itemInWitch, amount){

    itemInWitch.setAmount(itemInWitch.getAmount() - amount);

}

function modifyOffHandItem(offHandItemStack, material_id, material_name) {
    

    if (offHandItemStack != null && offHandItemStack.getType() != org.bukkit.Material.AIR) {
        // 修改显示名称
        let itemMeta = offHandItemStack.getItemMeta();

        itemMeta.setDisplayName("§a注§b入§c魔§d力§e的§f魔§3法§4抽§1屉§2（§5皮§6肤§7：§b" + material_name + "§8）");  // 设置你想要的显示名称

        // console.log("itemMeta: "+ itemMeta);
        
        // 修改材质为 PLAYER_HEAD
        offHandItemStack.setType(material_id);
        
        // 如果需要进一步设置头颅的皮肤，请参考之前的讨论添加相关代码
        
        // 应用修改后的元数据到 ItemStack
        offHandItemStack.setItemMeta(itemMeta);
        // offHandItemStack.setItemMeta(itemMeta);
        // console.log("offHandItemStack: "+ offHandItemStack);
        
        // 将修改后的物品重新设置回玩家的副手
        // player.getInventory().setItemInOffHand(offHandItemStack);
        
        // console.log("副手物品已被修改");
    }

}



function playIgniteTNTSound(world, location) {
    const TNT_IGNITE_SOUND_NAME = "block.end_portal.spawn"; // 使用完整的命名空间字符串
    const VOLUME = 1; // 音量
    const PITCH = 1; // 音调

    // 播放点燃TNT的音效，使用声音名称字符串
    world.playSound(location, TNT_IGNITE_SOUND_NAME, VOLUME, PITCH);
}
