const getPlayerData = (event) => {
    let playerData = {};
    playerData.player = event.getPlayer();
    playerData.world = playerData.player.getWorld();
    playerData.location = playerData.player.getLocation();
    playerData.playerName = playerData.player.getName();
    playerData.worldName = playerData.world.getName();
    playerData.XNum = parseFloat(playerData.location.getX().toFixed(2));
    playerData.YNum = parseFloat(playerData.location.getY().toFixed(2));
    playerData.ZNum = parseFloat(playerData.location.getZ().toFixed(2));
    // 提取 Yaw 和 Pitch
    playerData.yaw = parseFloat(playerData.location.getYaw().toFixed(2)); // 水平方向
    playerData.pitch = parseFloat(playerData.location.getPitch().toFixed(2)); // 垂直方向
    return playerData;
};

const mainHandItem = (player) => {
    return player.getInventory().getItemInMainHand();
}

const MainHandIf = (hand) => {
    if (hand !== org.bukkit.inventory.EquipmentSlot.HAND) {
        return true;
    } else {
        return false;
    }
}

//播放声音
const playTPSound=(world, location) => {
    const SOUND_NAME = "block.portal.travel"; // 使用完整的命名空间字符串
    const VOLUME = 1.0; // 音量
    const PITCH = 1.0; // 音调

    // 播放点燃TNT的音效，使用声音名称字符串
    world.playSound(location, SOUND_NAME, VOLUME, PITCH);
}

// 随机生成 yaw 和 pitch
const getRandomYawAndPitch = () => {
    const yaw = (Math.random() * 360 - 180).toFixed(2); // -180 到 +180
    const pitch = (Math.random() * 180 - 90).toFixed(2); // -90 到 +90
    return { yaw: parseFloat(yaw), pitch: parseFloat(pitch) };
};

// 随机生成 x, y, z 偏移量
const getRandomOffset = () => {
    const xOffset = (Math.random() * 500 - 250).toFixed(2); // -250 到 +250
    const yOffset = (Math.random() * 50 - 25).toFixed(2);    // -25 到 +25
    const zOffset = (Math.random() * 500 - 250).toFixed(2); // -250 到 +250
    return {
        xOffset: parseFloat(xOffset),
        yOffset: parseFloat(yOffset),
        zOffset: parseFloat(zOffset)
    };
};

const toLocation = (itemStack,player,playerData) => {

    // 解析匹配结果
    let worldName = playerData.worldName ? playerData.worldName : null; // 提取世界名称
    let xValue = playerData.XNum ? playerData.XNum : null; // 提取 X 坐标
    let yValue = playerData.YNum ? playerData.YNum : null; // 提取 Y 坐标
    let zValue = playerData.ZNum ? playerData.ZNum : null; // 提取 Z 坐标
    // 获取随机 yaw 和 pitch
    const { yaw, pitch } = getRandomYawAndPitch();

    // 获取随机偏移量
    const { xOffset, yOffset, zOffset } = getRandomOffset();

    xValue = xValue + xOffset;
    yValue = yValue + yOffset;
    zValue = zValue + zOffset;

    if(!worldName){
        player.sendMessage("§bMagic Legacy action could not be completed." + worldName + "'！");
        return;
    }

    let world = org.bukkit.Bukkit.getWorld(worldName);
    if (!world) {
        player.sendMessage("§bMagic Legacy action could not be completed." + worldName + "'！");
        return;
    }

    if (isNaN(xValue) || isNaN(yValue) || isNaN(zValue) || isNaN(yaw) || isNaN(pitch)) {
        player.sendMessage("§bMagic Legacy action could not be completed.");
        return;
    }

    // org.bukkit.Bukkit.broadcastMessage("实现传送！");
    // 创建目标位置
    let location = new org.bukkit.Location(world, xValue, yValue, zValue, yaw, pitch);
     // 执行传送
     try {
        player.teleport(location);
        player.sendMessage("§bMagic Legacy action could not be completed." + worldName + "§bMagic Legacy action could not be completed." + xValue + " Y:" + yValue + " Z:" + zValue + "！");
    } catch (error) {
        player.sendMessage("§bMagic Legacy action could not be completed." + error.message);
    }

    playTPSound(world,location);

    // 添加粒子效果
    player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, player.getLocation(), 1500, 1, 1, 1);

}

let playerLastUseTimes = new java.util.HashMap();

function onUse(event) {
    let player = event.getPlayer();
    // 检查主手是否持有物品
    if (MainHandIf(event.getHand())) {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }

    let item = mainHandItem(player)

    let playerData = getPlayerData(event);

    const currentTime = new Date().getTime();
    const playerId = player.getUniqueId();
    let lastUseTime = playerLastUseTimes.getOrDefault(playerId, 0);   
    if (currentTime - lastUseTime < 10000) {
        const remainingTime = parseFloat((10000 - (currentTime - lastUseTime)) / 2500).toFixed(5);
        player.sendTitle("§c§l你已经虚脱了！", "距离下次使用还要休息：" + remainingTime + "坤秒", 10, 40, 10);
        return; 
    }
    playerLastUseTimes.put(playerId, currentTime);
    toLocation(item,player,playerData);


}
