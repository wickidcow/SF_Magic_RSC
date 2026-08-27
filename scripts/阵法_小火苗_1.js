// 导入必要的 Java 类型
const Location = Java.type('org.bukkit.Location');
const Particle = Java.type('org.bukkit.Particle');
const Player = Java.type('org.bukkit.entity.Player');
const LivingEntity = Java.type('org.bukkit.entity.LivingEntity');
const Bukkit = Java.type('org.bukkit.Bukkit');

// 创建一个 HashMap 来跟踪每个玩家的最后使用时间
let playerLastUseTimes = new java.util.HashMap();



// 辅助函数用于播放粒子效果，形成空心圆柱
function playHollowCylinderParticles(player, radius, height, particleType) {
    let world = player.getWorld();
    let centerLocation = player.getLocation();

    // 定义圆柱的高度范围
    let minHeight = centerLocation.getY() - height / 2;
    let maxHeight = centerLocation.getY() + height / 2;

    // 计算圆周上的点并生成粒子
    for (let y = minHeight; y <= maxHeight; y++) {
        for (let angle = 0; angle < 360; angle += 2) { // 每10度生成一个粒子
            let radians = angle * (Math.PI / 180); // 将角度转换为弧度
            let x = centerLocation.getX() + radius * Math.cos(radians);
            let z = centerLocation.getZ() + radius * Math.sin(radians);

            let particleLoc = new Location(world, x, y, z);
            world.spawnParticle(particleType, particleLoc, 10);
        }
    }
}

// 辅助函数用于对范围内的生物造成伤害
function damageEntitiesInRange(centerLocation, radius, height, damage, player) {
    let world = centerLocation.getWorld();
    let entities = world.getNearbyEntities(centerLocation, radius, height / 2, radius);

    entities.forEach(function(entity) {
        if (entity instanceof LivingEntity) {
            entity.damage(damage);
        }
    });
}

// 发送消息给玩家
function sendMessage(player, message) {
    player.sendMessage(message);
}

// 使用函数：当物品被使用时触发
function onUse(event) {
    // 确保传入的是一个有效的玩家对象
    let player = event.getPlayer();

     // 检查主手是否持有物品
  if (event.getHand() !== org.bukkit.inventory.EquipmentSlot.HAND) {
    sendMessage(player, "Please hold an item in your main hand");
    return;
  }

    const playerId = player.getUniqueId();

    const currentTime = new Date().getTime();
    
  // 获取当前玩家的最后使用时间，如果不存在则初始化为0
  let lastUseTime = playerLastUseTimes.getOrDefault(playerId, 0);
  //冷却时间10秒
  if (currentTime - lastUseTime < 1000) {
    const remainingTime = Math.ceil((1000 - (currentTime - lastUseTime)) / 1000);
    player.sendTitle("§c§l你干嘛啊~哎呦！", "冷却剩余时间：" + remainingTime + " seconds", 10, 40, 10);
    return; 
  }

  player.setFoodLevel(0);
  player.setSaturation(0);


  // 更新该玩家的最后使用时间为当前时间
  playerLastUseTimes.put(playerId, currentTime);

    // 设置圆柱的参数
    let radius = 5;
    let height = 5;
    let particleType = Particle.FLAME; // 可以根据需要更改粒子类型
    let damage = 100; // 对生物造成的伤害值

    // 播放空心圆柱粒子效果
    playHollowCylinderParticles(player, radius, height, particleType);

    // 对范围内的生物造成伤害
    damageEntitiesInRange(player.getLocation(), radius, height, damage, player);

    // 发送消息给玩家确认
    sendMessage(player, "§b对范围内的生物造成了100点伤害！");
}

// 示例调用：假设你有一个 event 对象
// onUse(event);