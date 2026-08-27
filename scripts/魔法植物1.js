const SlimefunItem = Java.type('io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem');


// 创建两个 HashMap 来跟踪每个机器的最后使用时间和计数器
let lastUseTimes = new java.util.HashMap();
let machineTickCounters = new java.util.HashMap(); // 用于存储每个机器的独立计数器
let initializedLocations = new java.util.HashSet(); // 用于记录已经初始化的位置
let giftif = new java.util.HashMap(); 
//====================================================================================================================
var SpawnEntitytick = 2;            //刷怪笼预热时间     数值x2=描述   例如 2tick = 1second

var GrowTimems_INFINITE = 5000;           //无尽植物生长周期间隔,单位毫秒   如30000ms=30s

var NeedperSpawnerCharge = 520;         //每次生成生物耗电

var step1 = 1/3;

var step2 = 2/3;

var step3 = 1;

var step4 = 4/3;

var step5 = 5/3;

var step6 = 2;

var step7 = 7/3;

var sstep0 = 1/10;

var sstep1 = 1/6;

var sstep2 = 1/3;

var sstep3 = 1/2;

var sstep4 = 2/3;

var sstep5 = 5/6;

var sstep6 = 1;

var sstep7 = 7/6;

// var NeedperCraftCharge = 10;            //每秒耗电      默认去除


//====================================================================================================================

function tick(info) {
    var machine = info.machine();
    
    var location = info.block().getLocation();
    let world = location.getWorld();

    var machinesf = machine.getId();

    // org.bukkit.Bukkit.broadcastMessage("机器是: " + machinesf);   //测试

    var block = info.block();

    // 获取该机器的计数器，如果不存在则初始化为0
    let tickCounter = machineTickCounters.getOrDefault(location, 0);


    if (tickCounter < SpawnEntitytick) {
        // 更新该机器的计数器
        machineTickCounters.put(location, tickCounter + 1);

        // org.bukkit.Bukkit.broadcastMessage(`§7[系统] §f在 ${tickCounter} 计数测试`);   //测试
        return;
    }
    
    // org.bukkit.Bukkit.broadcastMessage("验证");      //测试

    // let NowCharge = machine.getCharge(location);


    //每秒耗电，默认关闭

    // if (NowCharge < NeedperCraftCharge) {
    //     //电量不足
    //     return;
    // }

    // machine.removeCharge(location, NeedperCraftCharge);

    const currentTime = new Date().getTime();

    // 初始化最后使用时间，仅在首次运行时执行
    if (!initializedLocations.contains(location)) {
        lastUseTimes.put(location, currentTime);
        initializedLocations.add(location); // 标记此位置已初始化
        // org.bukkit.Bukkit.broadcastMessage("首次初始化时间"); // 测试消息
        return;
    }

    // 获取该位置对应的最后使用时间
    let lastUseTime = lastUseTimes.get(location);

    if (machinesf === "MAGIC_PLANT_1"){

    if (currentTime - lastUseTime < GrowTimems_INFINITE * sstep0) {
        // 枯木
        block.setType(org.bukkit.Material.DEAD_BUSH);

    }
    

    if (currentTime - lastUseTime < GrowTimems_INFINITE * sstep1 && currentTime - lastUseTime > GrowTimems_INFINITE * sstep0) {
        // 执行小麦生长逻辑
        growWheatAtLocation(world, location, step1);

    }

    if (currentTime - lastUseTime < GrowTimems_INFINITE * sstep2 && currentTime - lastUseTime > GrowTimems_INFINITE * sstep1) {

        // 执行小麦生长逻辑
        growWheatAtLocation(world, location, step2);

    }

    if (currentTime - lastUseTime < GrowTimems_INFINITE * sstep3 && currentTime - lastUseTime > GrowTimems_INFINITE * sstep2) {

        // 执行小麦生长逻辑
        growWheatAtLocation(world, location, step3);

    }

    if (currentTime - lastUseTime < GrowTimems_INFINITE * sstep4 && currentTime - lastUseTime > GrowTimems_INFINITE * sstep3) {

        // 执行小麦生长逻辑
        growWheatAtLocation(world, location, step4);

    }

    if (currentTime - lastUseTime < GrowTimems_INFINITE * sstep5 && currentTime - lastUseTime > GrowTimems_INFINITE * sstep4) {

        // 执行小麦生长逻辑
        growWheatAtLocation(world, location, step5);

    }

    if (currentTime - lastUseTime < GrowTimems_INFINITE * sstep6 && currentTime - lastUseTime > GrowTimems_INFINITE * sstep5) {

        // 执行小麦生长逻辑
        growWheatAtLocation(world, location, step6);

    }

    if (currentTime - lastUseTime < GrowTimems_INFINITE * sstep7 && currentTime - lastUseTime > GrowTimems_INFINITE * sstep6) {

        // 执行小麦生长逻辑
        growWheatAtLocation(world, location, step7);

        giftif.put(location, true);
        // org.bukkit.Bukkit.broadcastMessage("植物成熟物品设定"+ giftif.get(location));      //测试

    }
    }


    
}

function onPlace(event) {
    // var player = event.getPlayer();
    // sendMessage(player, "植物被放置");

    // 当机器被放置时，清空该位置的最后使用时间和计数器记录
    let location = event.getBlock().getLocation();
    lastUseTimes.remove(location);
    machineTickCounters.remove(location);
    initializedLocations.remove(location); // 移除初始化标记，以便重新初始化

    giftif.put(location, false);

    let block = event.getBlock();

    // org.bukkit.Bukkit.broadcastMessage("blockOutput: "+ block);      //测试


}

function onBreak(event, itemStack, drops) {
    var player = event.getPlayer();
    // sendMessage(player, "方块被破坏");
    let world = player.getWorld();
    // 阻止方块掉落物品
    // event.setDropItems(false);

    // 当机器被破坏时，从映射中移除它的最后使用时间记录
    let location = event.getBlock().getLocation();
    lastUseTimes.remove(location);
    machineTickCounters.remove(location);
    initializedLocations.remove(location); // 移除初始化标记




    

    if (giftif.get(location) === true){

        
        let sfitem =  StorageCacheUtils.getSfItem(location);

        let sfplantid = sfitem.getId();

        // let BsfItem = location1.getSfItem();

        // org.bukkit.Bukkit.broadcastMessage("Output: "+ sfplantid);        //测试

        // org.bukkit.Bukkit.broadcastMessage("植物粘液id: "+ sfplantid);      //测试


        //无尽植物
        if (sfplantid === "MAGIC_PLANT_1"){

        // org.bukkit.Bukkit.broadcastMessage("这个是无尽植物");  
        //判定是否有无尽锭的奖励
        let Infinite_Yes_1 = Math.random();

        let A = 0.1;    // 一重奖励概率

        if(Infinite_Yes_1 < A){
        let selectedItem = "INFINITE_INGOT";

        let slimefunItem = getSfItemById(selectedItem);
  
        let iitemstack = new org.bukkit.inventory.ItemStack(slimefunItem.getItem());

        world.dropItemNaturally(location, iitemstack);
        }

        }
        // org.bukkit.Bukkit.broadcastMessage("植物成熟物品设定: "+ giftif.get(location));      //测试

    }else{
    
    // org.bukkit.Bukkit.broadcastMessage("植物成熟物品设定: false"+ giftif.get(location));      //测试

    }
    giftif.put(location, false);

}


function growWheatAtLocation(world, location, step) {
    let block = world.getBlockAt(location);

    let growstep = step *3;
    
    // 设置方块为小麦，并设置其生长阶段为2
    block.setType(org.bukkit.Material.WHEAT);
    let wheatBlockState = block.getState();
    let wheatCrop = wheatBlockState.getBlockData();

    // org.bukkit.Bukkit.broadcastMessage("验证");      //测试

    if (wheatCrop instanceof org.bukkit.block.data.Ageable) {
        wheatCrop.setAge(growstep); // 设置小麦生长阶段为growstep
        wheatBlockState.setBlockData(wheatCrop);
        wheatBlockState.update(true); // 更新方块状态

        // org.bukkit.Bukkit.broadcastMessage("成长了");      //测试

        // org.bukkit.Bukkit.broadcastMessage("当前为第 "+ growstep +" 阶段植物");      //测试

        // console.log("方块已成功更改为第"+ growstep +"阶段的小麦");
    } 

    if (step === 7){
        // org.bukkit.Bukkit.broadcastMessage("植物已经成熟了");      //测试
    }

}

