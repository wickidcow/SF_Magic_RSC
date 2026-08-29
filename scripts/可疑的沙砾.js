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
        "PLENTY_POTTERY_SHERD", // 富饶陶器碎片
        "BLADE_POTTERY_SHERD", // 利刃陶器碎片
        "MOURNER_POTTERY_SHERD", // 悼惜陶器碎片
        "EXPLORER_POTTERY_SHERD", // 探险陶器碎片
        "SNORT_POTTERY_SHERD", // 嗅探陶器碎片
        "SHELTER_POTTERY_SHERD", // 树荫陶器碎片
        "ANGLER_POTTERY_SHERD", // 垂钓陶器碎片
        "HOWL_POTTERY_SHERD", // 狼嚎陶器碎片
        "BURN_POTTERY_SHERD", // 烈焰陶器碎片
        "RAISER_ARMOR_TRIM_SMITHING_TEMPLATE", // 牧者盔甲纹样锻造模板
        "SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE", // 工匠盔甲纹样锻造模板
        "HOST_ARMOR_TRIM_SMITHING_TEMPLATE", // 主人盔甲纹样锻造模板
        "WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE", // 主人盔甲纹样锻造模板
        "DANGER_POTTERY_SHERD", // 危机陶器碎片
        "HEARTBREAK_POTTERY_SHERD", // 心碎陶器碎片
        "FRIEND_POTTERY_SHERD", // 挚友陶器碎片
        "SHEAF_POTTERY_SHERD", // 麦捆陶器碎片
        "HEART_POTTERY_SHERD", // 爱心陶器碎片
        "MUSIC_DISC_RELIC", // 唱片-Relic
        "SNIFFER_EGG" // 嗅探兽蛋
    ].map(shard => new ItemStack(Material.valueOf(shard), 1));

    // 其他掉落物
    const otherDrops = [
        new ItemStack(Material.IRON_AXE, 1), // 铁斧
        new ItemStack(Material.EMERALD, 1), // 绿宝石
        new ItemStack(Material.WHEAT, 1), // 小麦
        new ItemStack(Material.WOODEN_HOE, 1), // 木锄
        new ItemStack(Material.COAL, 1), // 煤炭
        new ItemStack(Material.GOLD_NUGGET, 1), // 金粒
        new ItemStack(Material.EMERALD, 1), // 绿宝石（古迹废墟普通战利品）
        new ItemStack(Material.WHEAT, 1), // 小麦（古迹废墟普通战利品）
        new ItemStack(Material.WOODEN_HOE, 1), // 木锄（古迹废墟普通战利品）
        new ItemStack(Material.CLAY, 1), // 黏土块
        new ItemStack(Material.RED_TERRACOTTA, 1), // 红砖
        new ItemStack(Material.YELLOW_DYE, 1), // 黄色染料
        new ItemStack(Material.BLUE_DYE, 1), // 蓝色染料
        new ItemStack(Material.LIGHT_BLUE_DYE, 1), // 淡蓝色染料
        new ItemStack(Material.WHITE_DYE, 1), // 白色染料
        new ItemStack(Material.ORANGE_DYE, 1), // 橙色染料
        new ItemStack(Material.GREEN_CANDLE, 1), // 绿色蜡烛
        new ItemStack(Material.RED_CANDLE, 1), // 红色蜡烛
        new ItemStack(Material.PURPLE_CANDLE, 1), // 紫色蜡烛
        new ItemStack(Material.BROWN_CANDLE, 1), // 棕色蜡烛
        new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE, 1), // 品红色染色玻璃板
        new ItemStack(Material.PINK_STAINED_GLASS_PANE, 1), // 粉红色染色玻璃板
        new ItemStack(Material.BLUE_STAINED_GLASS_PANE, 1), // 蓝色染色玻璃板
        new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1), // 淡蓝色染色玻璃板
        new ItemStack(Material.RED_STAINED_GLASS_PANE, 1), // 红色染色玻璃板
        new ItemStack(Material.YELLOW_STAINED_GLASS_PANE, 1), // 黄色染色玻璃板
        new ItemStack(Material.PURPLE_STAINED_GLASS_PANE, 1), // 紫色染色玻璃板
        new ItemStack(Material.SPRUCE_SIGN, 1), // 悬挂式云杉木告示牌
        new ItemStack(Material.OAK_SIGN, 1), // 悬挂式橡木告示牌
        new ItemStack(Material.GOLD_NUGGET, 1), // 金粒
        new ItemStack(Material.COAL, 1), // 煤炭
        new ItemStack(Material.WHEAT_SEEDS, 1), // 小麦种子
        new ItemStack(Material.BEETROOT_SEEDS, 1), // 甜菜种子
        new ItemStack(Material.DEAD_BUSH, 1), // 枯萎的灌木
        new ItemStack(Material.FLOWER_POT, 1), // 花盆
        new ItemStack(Material.STRING, 1), // 线
        new ItemStack(Material.LEAD, 1) // 拴绳
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
    if (material === Material.GRAVEL) {
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
