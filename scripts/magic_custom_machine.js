const HashMap = Java.type('java.util.HashMap');
const ItemStack = org.bukkit.inventory.ItemStack;
const Material = org.bukkit.Material;
const ChatColor = org.bukkit.ChatColor;

// 所有custom机器
const MAGIC_CUSTOM_MACHINE_TYPES = [
    { id: "MAGIC_FLOWER_MIX_1", name: "魔法鲜花转换机"},
    { id: "MAGIC_GEOMINER", name: "魔法资源开采机"},


];

// 创建用于存储每个机器独立计数器的HashMap
var machineTickCounters = new HashMap();
var lastUseTimes = new java.util.HashMap();

function tick(info) {
    var machine = info.machine();
    var location = info.block().getLocation();
    var containerLocation = location.clone().add(0, 1, 0);
    var downContainerLocation = location.clone().add(0, -1, 0);
    var machinesf = machine.getId();

    if ((machinesf === "MAGIC_FLOWER_MIX_1")){
        //魔法鲜花转换机
        let sfitem =  StorageCacheUtils.getSfItem(containerLocation);
        if (sfitem == null){
            // org.bukkit.Bukkit.broadcastMessage("不是一个粘液物品");
            return;
        }

        let sfitemid = sfitem.getId()
        //检测载体是否正确
        if (!(sfitemid === "MAGIC_FLOWER_MIX_BOX_1")){
            // org.bukkit.Bukkit.broadcastMessage("不是对应机器");
            return;
        }
        
        var blockMenu = StorageCacheUtils.getMenu(containerLocation); // 获取容器背包

        if (blockMenu == null) {
            // org.bukkit.Bukkit.broadcastMessage("没有菜单");
            return;
        }

        const flower_types = [
            // 花种类
            { id: "poppy", name: "虞美人", slot: 0, slot2: 1 ,id2: "blue_orchid"},
            { id: "blue_orchid", name: "兰花", slot: 1, slot2: 2, id2: "allium" },
            { id: "allium", name: "绒球葱", slot: 2, slot2: 3, id2: "azure_bluet" },
            { id: "azure_bluet", name: "雏菊", slot: 3, slot2: 4, id2: "red_tulip" },
            { id: "red_tulip", name: "红郁金香", slot: 4, slot2: 5, id2: "orange_tulip" },
            { id: "orange_tulip", name: "橙郁金香", slot: 5, slot2: 6, id2: "white_tulip" },
            { id: "white_tulip", name: "白郁金香", slot: 6, slot2: 7, id2: "pink_tulip" },
            { id: "pink_tulip", name: "粉郁金香", slot: 7, slot2: 8, id2: "oxeye_daisy" },
            { id: "oxeye_daisy", name: "滨菊", slot: 8, slot2: 18, id2: "sunflower" },

            { id: "sunflower", name: "向日葵", slot: 18, slot2: 19, id2: "lilac" },
            { id: "lilac", name: "丁香丛", slot: 19, slot2: 20, id2: "rose_bush" },
            { id: "rose_bush", name: "玫瑰丛", slot: 20, slot2: 21, id2: "peony" },
            { id: "peony", name: "牡丹", slot: 21, slot2: 22, id2: "wither_rose" },
            { id: "wither_rose", name: "凋零玫瑰", slot: 22, slot2: 23, id2: "cornflower" },
            { id: "cornflower", name: "矢车菊", slot: 23, slot2: 24, id2: "lily_of_the_valley" },
            { id: "lily_of_the_valley", name: "铃兰", slot: 24, slot2: 25, id2: "dandelion" },
            { id: "dandelion", name: "蒲公英", slot: 25, slot2: 26, id2: "torchflower" },
            { id: "torchflower", name: "火把花", slot: 26, slot2: 36, id2: "pitcher_plant" },

            { id: "pitcher_plant", name: "瓶子花", slot: 36, slot2: 37, id2: "pink_petals" },
            { id: "pink_petals", name: "粉红色花簇", slot: 37, slot2: 38, id2: "spore_blossom" },
            { id: "spore_blossom", name: "孢子花", slot: 38, slot2: 0, id2: "poppy"},
        ];
        //判断终止
        var Target_itemStack = blockMenu.getItemInSlot(44);
        if (Target_itemStack != null){

            var Target_itemStack_id = Target_itemStack.getType().name();

            var maybe1 = flower_types.find(g => g.id.toUpperCase() === Target_itemStack_id);



        }
        // org.bukkit.Bukkit.broadcastMessage("maybe1 = "+ maybe1 + "-----" + Target_itemStack_id);
        if (maybe1) {
            // org.bukkit.Bukkit.broadcastMessage("测试通过");
            var stop_item = Target_itemStack_id; 

            var stop_if = false;
        }

        // 定义需要遍历的槽位范围
        const ranges = [
            { start: 0, end: 8 },
            { start: 18, end: 26 },
            { start: 36, end: 38 }
        ];

        // 遍历每个定义的范围
        ranges.forEach(range => {
            for (let i = range.start; i <= range.end; i++) {
                let itemStack3 = blockMenu.getItemInSlot(i);

                if (itemStack3 != null){

                    var itemStack3_id = itemStack3.getType().name();
        
                    if (itemStack3_id === stop_item){
                        stop_if = true;
                    }
        
                }
                
            }
        });

        if (stop_if){
            return;
        }

        var NowCharge = machine.getCharge(location);
        var craftpertick = 10;
        if (NowCharge < craftpertick){
            return;
        }

        for (let flower of flower_types) {
            if(usage){
                return;
            }
            var usage = flowerDetective(flower, blockMenu, usage, machine, location, craftpertick);
        }

        return;


    }


    if ((machinesf === "MAGIC_GEOMINER")){


        let sfitem =  StorageCacheUtils.getSfItem(downContainerLocation);

        if (sfitem == null){
            // org.bukkit.Bukkit.broadcastMessage("不是一个粘液物品");
            return;
        }

        let sfitemid = sfitem.getId()
        //检测载体是否正确
        if (!(sfitemid === "MAGIC_GEOMINER_BOX")){
            // org.bukkit.Bukkit.broadcastMessage("不是对应机器");
            return;
        }
        var blockMenu = StorageCacheUtils.getMenu(downContainerLocation); // 获取容器背包



        if (blockMenu == null) {
            // org.bukkit.Bukkit.broadcastMessage("没有菜单");
            return;
        }


        let item13 = createItemLore(Material.BARRIER, ChatColor.GREEN + "信息", [ChatColor.YELLOW + "魔法矩阵资源开采机：" + ChatColor.RED + "停止开采"]);
        

        let NowCharge = machine.getCharge(location);

        //每秒耗电，默认关闭
        let NeedperCraftCharge = 1314;

        if (NowCharge < NeedperCraftCharge) {
            blockMenu.addItem(13, item13);
            //电量不足
            return;
        }

        machine.removeCharge(location, NeedperCraftCharge);

        let item13_2 = createItemLore(Material.LIGHT, ChatColor.GREEN + "信息", [ChatColor.YELLOW + "魔法矩阵资源开采机：" + ChatColor.GREEN + "工作中"]);
        blockMenu.addItem(13, item13_2);


        // 获取该机器的计数器，如果不存在则初始化为0
        let tickCounter = machineTickCounters.getOrDefault(location, 0);

        let defaultTick = 30;


        if (tickCounter < defaultTick) {
        // 更新该机器的计数器
        machineTickCounters.put(location, tickCounter + 1);

        // org.bukkit.Bukkit.broadcastMessage(`§7[系统] §f在 ${tickCounter} 计数测试`);   //测试
        return;
    }
        
        const currentTime = new Date().getTime();
        // 获取该位置对应的最后使用时间，如果不存在则初始化为0
        let lastUseTime = lastUseTimes.getOrDefault(location, 0);

        let msPerCraft = 30000;
        if (!(currentTime - lastUseTime > msPerCraft)) {

            return;

        }
    
    lastUseTimes.put(location, currentTime);        //重置计时  防止卡机器    

    var b = info.block();

    var outslots = sfitem.getInputSlots();

    // org.bukkit.Bukkit.broadcastMessage("outslots: " + outslots);

    const NamespacedKey = Java.type('org.bukkit.NamespacedKey');


    const Slimefun = Java.type("io.github.thebusybiscuit.slimefun4.implementation.Slimefun");




    //逐个处理魔法资源


    var key1 = Slimefun.getRegistry().getGEOResources().get(new NamespacedKey("rykenslimefuncustomizer", "magic_redstone")).orElse(null);

    var supplies1 = Slimefun.getGPSNetwork().getResourceManager().getSupplies(key1, b.getWorld(), b.getX() >> 4, b.getZ() >> 4);

        // org.bukkit.Bukkit.broadcastMessage("当前区域魔法红石数量: " + supplies1);

        // org.bukkit.Bukkit.broadcastMessage("是否存在魔法红石: " + supplies1.isPresent());

    if (supplies1.isPresent()){
    

        // org.bukkit.Bukkit.broadcastMessage("剩余魔法红石数量: " + supplies1.getAsInt());
    
        if (supplies1.isPresent() && supplies1.getAsInt() > 0) {
    
            Slimefun.getGPSNetwork().getResourceManager().setSupplies(key1, b.getWorld(), b.getX() >> 4, b.getZ() >> 4, supplies1.getAsInt() - 1);
    
    
            const outputItem = getSfItemById("MAGIC_REDSTONE");
            const outputItemstack = new org.bukkit.inventory.ItemStack(outputItem.getItem().getType());
            outputItemstack.setItemMeta(outputItem.getItem().getItemMeta());
            outputItemstack.setAmount(1);

    
            blockMenu.pushItem(outputItemstack,outslots);
    
            // org.bukkit.Bukkit.broadcastMessage("pushItem: " + blockMenu.pushItem(outputItemstack,outslots));
    
    
    
    
    
    
            // org.bukkit.Bukkit.broadcastMessage("成功挖掘1个魔法红石");
    
            return;
    
            }
        }



    
    var key2 = Slimefun.getRegistry().getGEOResources().get(new NamespacedKey("rykenslimefuncustomizer", "magic_cosmic_dust")).orElse(null);


    var supplies2 = Slimefun.getGPSNetwork().getResourceManager().getSupplies(key2, b.getWorld(), b.getX() >> 4, b.getZ() >> 4);

    // org.bukkit.Bukkit.broadcastMessage("当前区域宇宙尘数量: " + supplies2);

    // org.bukkit.Bukkit.broadcastMessage("是否存在宇宙尘: " + supplies2.isPresent());

    if (supplies2.isPresent()){
    

    // org.bukkit.Bukkit.broadcastMessage("剩余宇宙尘数量: " + supplies2.getAsInt());

    if (supplies2.isPresent() && supplies2.getAsInt() > 0) {

        Slimefun.getGPSNetwork().getResourceManager().setSupplies(key2, b.getWorld(), b.getX() >> 4, b.getZ() >> 4, supplies2.getAsInt() - 1);


        const outputItem = getSfItemById("MAGIC_COSMIC_DUST");
        const outputItemstack = new org.bukkit.inventory.ItemStack(outputItem.getItem().getType());
        outputItemstack.setItemMeta(outputItem.getItem().getItemMeta());
        outputItemstack.setAmount(1);


        blockMenu.pushItem(outputItemstack,outslots);

        // org.bukkit.Bukkit.broadcastMessage("pushItem: " + blockMenu.pushItem(outputItemstack,outslots));






        // org.bukkit.Bukkit.broadcastMessage("成功挖掘1个宇宙尘");

        return;

        }


    }

    var key3 = Slimefun.getRegistry().getGEOResources().get(new NamespacedKey("rykenslimefuncustomizer", "magic_soul")).orElse(null);


    var supplies2 = Slimefun.getGPSNetwork().getResourceManager().getSupplies(key3, b.getWorld(), b.getX() >> 4, b.getZ() >> 4);

    // org.bukkit.Bukkit.broadcastMessage("当前区域宇宙尘数量: " + supplies2);

    // org.bukkit.Bukkit.broadcastMessage("是否存在宇宙尘: " + supplies2.isPresent());

    if (supplies2.isPresent()){
    

    // org.bukkit.Bukkit.broadcastMessage("剩余宇宙尘数量: " + supplies2.getAsInt());

    if (supplies2.isPresent() && supplies2.getAsInt() > 0) {

        Slimefun.getGPSNetwork().getResourceManager().setSupplies(key3, b.getWorld(), b.getX() >> 4, b.getZ() >> 4, supplies2.getAsInt() - 1);


        const outputItem = getSfItemById("MAGIC_SOUL");
        const outputItemstack = new org.bukkit.inventory.ItemStack(outputItem.getItem().getType());
        outputItemstack.setItemMeta(outputItem.getItem().getItemMeta());
        outputItemstack.setAmount(1);


        blockMenu.pushItem(outputItemstack,outslots);

        // org.bukkit.Bukkit.broadcastMessage("pushItem: " + blockMenu.pushItem(outputItemstack,outslots));






        // org.bukkit.Bukkit.broadcastMessage("成功挖掘1个宇宙尘");

        return;

        }


    }

    let item4 = createItemLore(Material.NETHER_STAR, ChatColor.GREEN + "开采完成", [ChatColor.AQUA + "保护环境，人人有责" + ChatColor.AQUA + "切莫开采过度" + ChatColor.AQUA + "给后来人留一些资源"]);
    blockMenu.addItem(13, item13);
    blockMenu.addItem(4, item4);
    
    }
    


}

function flowerDetective(flower, menu, usage, machine, location, craftpertick){

    let itemStack = menu.getItemInSlot(flower.slot);
    if (itemStack == null){
        // org.bukkit.Bukkit.broadcastMessage("物品栏为空");
        return;
    }

    let material_id = itemStack.getType().name();
    if (material_id != flower.id.toUpperCase()){
        // org.bukkit.Bukkit.broadcastMessage("物品栏不为:" + flower.id);
        return;
    }

    let itemStack2 = menu.getItemInSlot(flower.slot2);
    if (itemStack2 != null){
        // org.bukkit.Bukkit.broadcastMessage("物品栏有物品阻拦,槽位为"+ flower.slot2);
        return;
    }
    machine.removeCharge(location, craftpertick);
    let amount = itemStack.getAmount();
    let item = createItem(Material.STONE, 0);
    menu.addItem(flower.slot, item);
    let item2 = createItem(Material[flower.id2.toUpperCase()], amount);
    menu.addItem(flower.slot2, item2);
    // org.bukkit.Bukkit.broadcastMessage("已将物品从槽位 " + flower.slot + " 转换到槽位 " + flower.slot2);
    
    usage = true;
    return usage;

}

function createItem(material, amount) {
    let item = new ItemStack(material, amount);
    return item;
}

function onPlace(event) {

    // 当机器被破坏时，从映射中移除它的最后使用时间记录
    let location = event.getBlock().getLocation();
    lastUseTimes.remove(location);
    machineTickCounters.remove(location);
}

function onBreak(event, itemStack, drops) {

    var player = event.getPlayer();
    let world = player.getWorld();
    // 当机器被破坏时，从映射中移除它的最后使用时间记录
    let location = event.getBlock().getLocation();
    lastUseTimes.remove(location);
    machineTickCounters.remove(location);
}


function createItemLore(material, displayName, lore) {
    let item = new ItemStack(material, 1);
    let meta = item.getItemMeta();
    
    // 设置物品名字
    meta.setDisplayName(displayName);

    // 设置Lore
    meta.setLore(lore);

    item.setItemMeta(meta);
    return item;
}
