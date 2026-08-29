// 定义矿粉及其等概率分布
const dustArray = [
    "IRON_DUST", "GOLD_DUST", "TIN_DUST", "COPPER_DUST",
    "SILVER_DUST", "LEAD_DUST", "ALUMINUM_DUST", "ZINC_DUST", "MAGNESIUM_DUST"
];

// 导入必要的 Java 类型
const Material = Java.type('org.bukkit.Material');
const ItemStack = Java.type('org.bukkit.inventory.ItemStack');
const Bukkit = Java.type('org.bukkit.Bukkit');
const Random = Java.type('java.util.Random');

// 辅助函数用于发送消息给玩家
function sendMessage(player, message) {
    player.sendMessage(message);
}

// 定义可疑沙子的掉落物及其概率
function getSuspiciousSandDrops() {
    // 所有陶器碎片以 1/8 的总概率掉落，平均分配给每个陶器碎片
    const shards = [
        "ANGLER_POTTERY_SHERD",
        "ARCHER_POTTERY_SHERD",
        "ARMS_UP_POTTERY_SHERD",
        "BREWER_POTTERY_SHERD",
        "MINER_POTTERY_SHERD",
        "PRIZE_POTTERY_SHERD",
        "SHELTER_POTTERY_SHERD",
        "SKULL_POTTERY_SHERD",
        "SNORT_POTTERY_SHERD",
        "SNIFFER_EGG" // 添加嗅探兽蛋到陶器碎片列表中
    ].map(shard => new ItemStack(Material.valueOf(shard), 1));

    // 其他掉落物
    const otherDrops = [
        new ItemStack(Material.GOLD_NUGGET, 1), // 金粒
        new ItemStack(Material.EMERALD, 1), // 绿宝石
        new ItemStack(Material.WHEAT, 1), // 小麦
        new ItemStack(Material.WOODEN_HOE, 1), // 木锄
        new ItemStack(Material.COAL, 1), // 煤炭
        new ItemStack(Material.IRON_AXE, 1), // 铁斧
        new ItemStack(Material.SUSPICIOUS_STEW, 1), // 谜之炖菜
        new ItemStack(Material.BRICK, 1), // 红砖头（这里用红石代替，因为 Minecraft 中没有红砖头）
        new ItemStack(Material.STICK, 1), // 木棒
        new ItemStack(Material.DIAMOND, 1), // 钻石
        new ItemStack(Material.GUNPOWDER, 1), // 火药
        new ItemStack(Material.TNT, 1), // TNT
        // SNIFTER_SPAWN_EGG 已经包含在陶器碎片列表中，所以这里不再重复添加
    ];


    return { shards, otherDrops };
}



// 当方块被破坏时触发
function onBreak(event) {
    // 确保传入的是一个有效的玩家对象和事件对象
    let player = event.getPlayer();
    let block = event.getBlock();


    // 获取方块数据
    let material = block.getType();

    // 检查是否为可疑沙子（假设通过材质类型判断）
    if (material === Material.SAND) {
        let dropsInfo = getSuspiciousSandDrops();
        let random = new Random();
        let selectedDrop = null;

        // 决定是掉落陶器碎片还是其他物品
        if (random.nextDouble() < 0.125) { // 1/8 的概率掉落陶器碎片
            // 从陶器碎片中随机选择一个掉落物
            selectedDrop = dropsInfo.shards[random.nextInt(dropsInfo.shards.length)];
        } else { // 7/8 的概率掉落其他物品
            // 从其他物品中随机选择一个掉落物
            selectedDrop = dropsInfo.otherDrops[random.nextInt(dropsInfo.otherDrops.length)];
        }


        // 在方块位置掉落选定的物品
        if (selectedDrop) {

        block.getWorld().dropItemNaturally(block.getLocation(), selectedDrop);

        // 随机选择一种矿粉
        const selectedItem = dustArray[Math.floor(Math.random() * dustArray.length)];

        const slimefunItem = getSfItemById(selectedItem);
        var itemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());
        var randomNumber = Math.floor(Math.random() * 3) + 1;
        itemstack.setAmount(randomNumber);

        block.getWorld().dropItemNaturally(block.getLocation(), itemstack);
        

        // 发送消息给玩家确认
        sendMessage(player, "§bMagic Legacy action could not be completed." +selectedDrop.getType().name()+ "！");
        sendMessage(player, "§bMagic Legacy action could not be completed." + itemstack.getItemMeta().getDisplayName() + " §b*" + randomNumber);
        
       
        }else {
            sendMessage(player, "§bMagic Legacy action could not be completed.");
        }

        // 阻止默认的方块掉落行为（可选）
        event.setDropItems(false);

        
        }else{
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        }
     
        
}



// 示例调用：假设你需要手动测试
// onBreak(event);
