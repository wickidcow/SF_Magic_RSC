const HashMap = Java.type('java.util.HashMap');
const ItemStack = org.bukkit.inventory.ItemStack;
const Material = org.bukkit.Material;
const ChatColor = org.bukkit.ChatColor;

// 定义所有发电机类型及其发电率（每单位数量的发电量）和初始槽位
const MAGIC_INFINITY_TYPES = [
    { id: "MAGIC_INFINITY_COBBLE_GEN", name: "魔法圆石生成机", power: 3200 },
    { id: "MAGIC_INFINITY_DUST_EXTRACTOR", name: "魔法磨粉机", power: 28800, },
    { id: "MAGIC_INFINITY_INGOT_FORMER", name: "铸锭机", power: 28800, },
];

// 创建用于存储每个机器独立计数器的HashMap
var machineTickCounters = new HashMap();

function tick(info) {
    var location = info.block().getLocation();
    let containerLocation = location.clone().add(0, 1, 0);
    var machine = info.machine();

    let sfitem = StorageCacheUtils.getSfItem(containerLocation);
    if (sfitem == null) {
        // org.bukkit.Bukkit.broadcastMessage("不是一个粘液物品");
        return;
    }

    let sfitemid = sfitem.getId()

    if (!(sfitemid === "MAGIC_INFINITY_MIX_BOX_1")) {

        // org.bukkit.Bukkit.broadcastMessage("不是对应机器");
        return;


    }


    var blockMenu = StorageCacheUtils.getMenu(containerLocation); // 获取容器背包

    if (blockMenu != null) {
        let totalPower = 0;

        // // 初始化所有槽位为"停止发电"
        // initializeSlots(blockMenu);
        // let item53 = createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "停止发电", [ChatColor.GRAY + "魔法矩阵：" + ChatColor.YELLOW + "发电机"]);
        // blockMenu.addItem(53, item53);
        let itemCounts = {};
        // 处理所有发电机类型并累加总发电量
        for (let generator of MAGIC_INFINITY_TYPES) {
            // 对于每个generator，调用processGenerator并保存结果
            let itemCount = processGenerator(blockMenu, generator.id);
            itemCounts[generator.id] = itemCount; // 以generator的id为键保存数量
            // console.log(itemCounts[generator.id]);
        }
        // console.log(itemCounts["MAGIC_INFINITY_COBBLE_GEN"]);
        // console.log(itemCounts["MAGIC_INFINITY_DUST_EXTRACTOR"]);
        // console.log(itemCounts["MAGIC_INFINITY_INGOT_FORMER"]);

        let needPower = powerCalculate(itemCounts);

        let NowCharge = machine.getCharge(location);

        if (NowCharge == 0 || NowCharge < needPower || needPower == 0) {
            let lore = [];
            let item53 = createItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ChatColor.YELLOW + "电力不足", lore)
            blockMenu.addItem(53, item53);
            return;//电力不足
        }
        

        // 计算产物比例
        let lore = generateLore(itemCounts, needPower);

        itemCounts = processItemCountLogic(itemCounts);

        let emptyNum = countEmptySlots(blockMenu,0,35);
        emptyNum = Math.floor(emptyNum / 3);

        let needCount = 0;

        // console.log("emptyNum" + emptyNum);

        let sfitem = StorageCacheUtils.getSfItem(containerLocation);
        let outslots = sfitem.getOutputSlots();
        // console.log("needCount" + needCount);
        needCount = outPutCobbleNeed(needCount, itemCounts, blockMenu, outslots);
        // console.log("needCount" + needCount);
        needCount = outPutDustNeed(needCount, itemCounts, blockMenu, outslots);
        // console.log("needCount" + needCount);
        needCount = outPutIngotNeed(needCount, itemCounts, blockMenu, outslots);
        // console.log("needCount" + needCount);

        if(emptyNum<needCount){
            let lore = [];
            let item53 = createItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ChatColor.YELLOW + "空间不足", lore)
            blockMenu.addItem(53, item53);
            return;
        }
        machine.removeCharge(location, needPower);


        let count = 0;

        // console.log("count" + count);
        count = outPutCobble(count, itemCounts, blockMenu, outslots);
        // console.log("count" + count);
        count = outPutDust(count, itemCounts, blockMenu, outslots);
        // console.log("count" + count);
        count = outPutIngot(count, itemCounts, blockMenu, outslots);
        // console.log("count" + count);

        itemCounts = adjustItemCount(itemCounts);


        lore = generateLore2(itemCounts, lore)

        let item53 = createItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ChatColor.YELLOW + "魔法无尽一体化工艺", lore)
        blockMenu.addItem(53, item53);





    }




}

const countEmptySlots=(blockMenu, startSlot, endSlot) =>{
    let emptySlots = 0;

    for (let slot = startSlot; slot <= endSlot; slot++) {
        // 获取指定槽位的 ItemStack
        let itemStack = blockMenu.getItemInSlot(slot);

        // 检查 ItemStack 是否为 null 或者其数量是否为 0
        if (itemStack === null || itemStack.getAmount() === 0) {
            emptySlots++;
        }
    }

    return emptySlots;
}

const adjustItemCount = (itemCountsLater) => {
    let cobbleGenCount = itemCountsLater["MAGIC_INFINITY_COBBLE_GEN"];
    let dustExtractorCount = itemCountsLater["MAGIC_INFINITY_DUST_EXTRACTOR"];
    let ingotFormerCount = itemCountsLater["MAGIC_INFINITY_INGOT_FORMER"];

    if (cobbleGenCount >= 12) {
        // 如果圆石生成机的数量大于等于12，则其他两个设为0
        itemCountsLater["MAGIC_INFINITY_COBBLE_GEN"] = 12;
        itemCountsLater["MAGIC_INFINITY_DUST_EXTRACTOR"] = 0;
        itemCountsLater["MAGIC_INFINITY_INGOT_FORMER"] = 0;
    } else {
        // 如果圆石生成机的数量小于12，则需要确保三个数量加起来不超过12
        let remainingCapacity = 12 - cobbleGenCount;

        // 对比剩余容量和第二个数量
        if (remainingCapacity < dustExtractorCount) {
            itemCountsLater["MAGIC_INFINITY_DUST_EXTRACTOR"] = remainingCapacity;
            remainingCapacity = 0; // 更新剩余容量为0
        } else {
            remainingCapacity -= dustExtractorCount; // 减去第二个数量后更新剩余容量
        }

        // 对比剩余容量和第三个数量
        if (remainingCapacity < ingotFormerCount) {
            itemCountsLater["MAGIC_INFINITY_INGOT_FORMER"] = remainingCapacity;
        } else {
            // 剩余容量大于或等于第三个数量，不需要改变第三个数量
            // 剩余容量会自动调整为剩余值
        }
    }

    return itemCountsLater;
}




function processGenerator(menu, targetId) {
    let itemCount = countTargetItemsInMenu(menu, targetId);

    return itemCount; // 返回计数以便在tick函数中使用
}

//仅用作于魔法无尽一体化工艺
function countTargetItemsInMenu(menu, targetId) {
    let count = 0;
    for (let i = 45; i < 52; i++) {
        let itemStack = menu.getItemInSlot(i);
        if (itemStack != null) {
            let slimefunItem = getSfItemByItem(itemStack);
            if (slimefunItem && slimefunItem.getId() === targetId) {
                count += itemStack.getAmount();
            }
        }
    }
    return count;
}

const createItem = (material, displayName, lore) => {
    let item = new ItemStack(material, 1);
    let meta = item.getItemMeta();
    // 添加ItemFlag，这里以隐藏属性为例
    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_POTION_EFFECTS);

    // 设置物品名字
    meta.setDisplayName(displayName);

    // 设置Lore
    meta.setLore(lore);

    item.setItemMeta(meta);
    return item;
}

const generateLore = (itemCounts, needPower) => {
    var lore = [];

    // 推送一条Aqua颜色的当前机器数据
    lore.push(ChatColor.AQUA + "机器数据================================");

    // 检查并添加魔法圆石生成机的信息
    if (itemCounts["MAGIC_INFINITY_COBBLE_GEN"] > 0) {
        lore.push(ChatColor.AQUA + "魔法圆石生成机：" + ChatColor.YELLOW + itemCounts["MAGIC_INFINITY_COBBLE_GEN"] + ChatColor.AQUA + " 个");
    }

    // 如果有魔法磨粉机的数量，则添加到lore
    if (itemCounts["MAGIC_INFINITY_DUST_EXTRACTOR"] > 0) {
        lore.push(ChatColor.AQUA + "魔法磨粉机：" + ChatColor.YELLOW + itemCounts["MAGIC_INFINITY_DUST_EXTRACTOR"] + ChatColor.AQUA + " 个");
    }

    // 如果有魔法铸锭机的数量，则添加到lore
    if (itemCounts["MAGIC_INFINITY_INGOT_FORMER"] > 0) {
        lore.push(ChatColor.AQUA + "魔法铸锭机：" + ChatColor.YELLOW + itemCounts["MAGIC_INFINITY_INGOT_FORMER"] + ChatColor.AQUA + " 个");
    }
    if (needPower > 0) {
        lore.push(ChatColor.AQUA + "当前电力消耗：" + ChatColor.YELLOW + needPower * 2 + ChatColor.AQUA + " J/s");
    }
    // 推送一条Aqua颜色的当前机器数据
    lore.push(ChatColor.AQUA + "机器数据================================");
    return lore;
}

function generateLore2(itemCountsLater, lore) {
    ;

    // 推送一条Aqua颜色的当前机器数据
    lore.push(ChatColor.GREEN + "产物数据================================");

    // 检查并添加魔法圆石生成机的信息
    if (itemCountsLater["MAGIC_INFINITY_COBBLE_GEN"] > 0) {
        lore.push(ChatColor.GREEN + "圆石：" + ChatColor.YELLOW + itemCountsLater["MAGIC_INFINITY_COBBLE_GEN"] * 192 + ChatColor.GREEN + " 个");
    }

    // 如果有魔法磨粉机的数量，则添加到lore
    if (itemCountsLater["MAGIC_INFINITY_DUST_EXTRACTOR"] > 0) {
        lore.push(ChatColor.GREEN + "矿粉：" + ChatColor.YELLOW + itemCountsLater["MAGIC_INFINITY_DUST_EXTRACTOR"] * 192 + ChatColor.GREEN + " 个");
    }

    // 如果有魔法铸锭机的数量，则添加到lore
    if (itemCountsLater["MAGIC_INFINITY_INGOT_FORMER"] > 0) {
        lore.push(ChatColor.GREEN + "矿锭：" + ChatColor.YELLOW + itemCountsLater["MAGIC_INFINITY_INGOT_FORMER"] * 192 + ChatColor.GREEN + " 个");
    }
    // 推送一条Aqua颜色的当前机器数据
    lore.push(ChatColor.GREEN + "产物数据================================");

    return lore;
}

const outPutCobbleNeed = (countNeed, itemCounts) => {
    for (let i = 0; i < itemCounts["MAGIC_INFINITY_COBBLE_GEN"] && countNeed < 12; i++, countNeed++) {
    }
    return countNeed;
}

const outPutDustNeed = (countNeed, itemCounts) => {

    for (let i = 0; i < itemCounts["MAGIC_INFINITY_DUST_EXTRACTOR"] && countNeed < 12; i++, countNeed++) {
    }
    return countNeed;
}

const outPutIngotNeed = (countNeed, itemCounts) => {
    for (let i = 0; i < itemCounts["MAGIC_INFINITY_INGOT_FORMER"] && countNeed < 12; i++, countNeed++) {
    }
    return countNeed;
}





const outPutCobble = (count, itemCounts, blockMenu, outslots) => {

    // 创建一个Material类型为COBBLESTONE，数量为64的ItemStack
    let cobblestoneStack = new ItemStack(Material.COBBLESTONE, 64);

    for (let i = 0; i < itemCounts["MAGIC_INFINITY_COBBLE_GEN"] && count < 12; i++, count++) {
        // 输出圆石到指定输出槽位
        for (let j = 0; j < 3; j++) {
            blockMenu.pushItem(cobblestoneStack, outslots);
        }
    }
    return count;

}

const outPutDust = (count, itemCounts, blockMenu, outslots) => {

    const dustArray = [
        "IRON_DUST", "GOLD_DUST", "TIN_DUST", "COPPER_DUST",
        "SILVER_DUST", "LEAD_DUST", "ALUMINUM_DUST", "ZINC_DUST", "MAGNESIUM_DUST"
    ];

    for (let i = 0; i < itemCounts["MAGIC_INFINITY_DUST_EXTRACTOR"] && count < 12; i++, count++) {
        // 随机选择一个dust类型
        for (let j = 0; j < 3; j++) {
            const randomDustType = dustArray[Math.floor(Math.random() * dustArray.length)];
            const outputItem = getSfItemById(randomDustType);
            const outputItemstack = new ItemStack(outputItem.getItem());
            outputItemstack.setAmount(64);

            blockMenu.pushItem(outputItemstack, outslots);
        }
    }

    return count;

}

const outPutIngot = (count, itemCounts, blockMenu, outslots) => {

    const ingotArray = [
        "TIN_INGOT", "COPPER_INGOT",
        "SILVER_INGOT", "LEAD_INGOT", "ALUMINUM_INGOT", "ZINC_INGOT", "MAGNESIUM_INGOT"
    ];
    let ironIngotStack = new ItemStack(Material.IRON_INGOT, 64);
    let goldIngotStack = new ItemStack(Material.GOLD_INGOT, 64);


    for (let i = 0; i < itemCounts["MAGIC_INFINITY_INGOT_FORMER"] && count < 12; i++, count++) {
        for (let j = 0; j < 3; j++) {
            if (Math.random() < 2 / 9) { // 1/9的概率选择ironIngotStack
                if (Math.random() < 1 / 2) {
                    blockMenu.pushItem(ironIngotStack, outslots);
                }
                else {
                    blockMenu.pushItem(goldIngotStack, outslots);
                }
            } else {
                const randomIngotType = ingotArray[Math.floor(Math.random() * ingotArray.length)];
                const outputItem = getSfItemById(randomIngotType);
                const outputItemstack = new ItemStack(outputItem.getItem());
                outputItemstack.setAmount(64);

                blockMenu.pushItem(outputItemstack, outslots);
            }
        }
    }
    return count;
}

const powerCalculate = (itemCounts) => {

    let cobbleGenCount = itemCounts["MAGIC_INFINITY_COBBLE_GEN"] || 0;
    let dustExtractorCount = itemCounts["MAGIC_INFINITY_DUST_EXTRACTOR"] || 0;
    let ingotFormerCount = itemCounts["MAGIC_INFINITY_INGOT_FORMER"] || 0;


    let charge1 = cobbleGenCount * 3200;
    let charge2 = dustExtractorCount * 28800;
    let charge3 = ingotFormerCount * 28800;

    return charge1 + charge2 + charge3;

}


const processItemCountLogic = (itemCounts) => {
    // 检查并初始化数值以防它们不存在于itemCounts中
    let cobbleGenCount = itemCounts["MAGIC_INFINITY_COBBLE_GEN"] || 0;
    let dustExtractorCount = itemCounts["MAGIC_INFINITY_DUST_EXTRACTOR"] || 0;
    let ingotFormerCount = itemCounts["MAGIC_INFINITY_INGOT_FORMER"] || 0;

    // 处理魔法圆石生成机和魔法磨粉机之间的逻辑
    if (cobbleGenCount >= dustExtractorCount) {
        cobbleGenCount -= dustExtractorCount;
    } else {
        dustExtractorCount = cobbleGenCount;
        cobbleGenCount = 0;
    }

    // 更新itemCounts对象中的值
    itemCounts["MAGIC_INFINITY_COBBLE_GEN"] = cobbleGenCount;
    itemCounts["MAGIC_INFINITY_DUST_EXTRACTOR"] = dustExtractorCount;

    // 处理魔法磨粉机和铸锭机之间的逻辑
    if (dustExtractorCount >= ingotFormerCount) {
        dustExtractorCount -= ingotFormerCount;
    } else {
        ingotFormerCount = dustExtractorCount;
        dustExtractorCount = 0;
    }

    // 再次更新itemCounts对象中的值
    itemCounts["MAGIC_INFINITY_DUST_EXTRACTOR"] = dustExtractorCount;
    itemCounts["MAGIC_INFINITY_INGOT_FORMER"] = ingotFormerCount;

    return itemCounts;
}

function onPlace(event) {

    machineTickCounters.clear();
    // org.bukkit.Bukkit.broadcastMessage("放置了一个物品");
}

function onBreak(event, itemStack, drops) {

    var player = event.getPlayer();
    let world = player.getWorld();
    machineTickCounters.clear();
    // org.bukkit.Bukkit.broadcastMessage("拆除了一个物品");
}