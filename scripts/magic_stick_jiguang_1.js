// 导入必要的 Java 类型
const EquipmentSlot = Java.type('org.bukkit.inventory.EquipmentSlot');
const FluidCollisionMode = Java.type('org.bukkit.FluidCollisionMode');
const Particle = Java.type('org.bukkit.Particle');
const Sound = Java.type('org.bukkit.Sound');

// 主函数：当物品被使用时触发
function onUse(event,itemStack) {
    let player = event.getPlayer();

    // 检查主手是否持有物品
    if (event.getHand() !== EquipmentSlot.HAND) {
        sendMessage(player, "§bPlease hold an item in your main hand");
        return;
    }

    var onUseItem = event.getItem();
  
    var Charge = itemStack.getItemCharge(onUseItem)

    if ( Charge < 50 ){
      player.sendMessage("§b电量不足，请进行充电~")
      return;
    }

    itemStack.removeItemCharge(onUseItem, 50);

    // 播放点燃TNT的音效
    

    // 获取世界和玩家眼睛位置及朝向
    let world = player.getWorld();
    let eyeLocation = player.getEyeLocation();
    let direction = eyeLocation.getDirection().normalize(); // 获取标准化后的方向向量
    let startLocation = eyeLocation.clone().add(direction); // 射线起点位置（眼睛位置加上方向向量）
    let maxDistance = 25; // 最大射程

    playIgniteTNTSound(world, eyeLocation);

    // 执行射线追踪
    let rayTraceResults = world.rayTrace(startLocation, direction, maxDistance, FluidCollisionMode.ALWAYS, true, 0.1, null);

    // 生成粒子效果
    generateParticleBeam(world, startLocation, direction, maxDistance);

    generateDualSpiralParticleBeam(world, startLocation, direction, maxDistance)

    // 处理射线追踪结果
    handleRayTraceResult(rayTraceResults, player);

    // player.sendMessage("验证执行");
}

// 生成粒子效果
function generateParticleBeam(world, startLocation, direction, maxDistance) {
    const PARTICLE_TYPE = Particle.END_ROD; // 使用末影杆粒子作为示例
    const PARTICLE_INTERVAL = 0.1; // 粒子之间的间隔距离

    for (let distance = 0; distance <= maxDistance; distance += PARTICLE_INTERVAL) {
        let particleLocation = startLocation.clone().add(direction.clone().multiply(distance));
        world.spawnParticle(PARTICLE_TYPE, particleLocation.getX(), particleLocation.getY(), particleLocation.getZ(), 0);
    }
}

// 生成双螺旋粒子效果（3D中心旋转180度对称）
function generateDualSpiralParticleBeam(world, startLocation, direction, maxDistance) {
    const PARTICLE_TYPE = Particle.END_ROD; // 使用末影杆粒子作为示例
    const PARTICLE_INTERVAL = 0.1; // 粒子之间的间隔距离
    const SPIRAL_RADIUS = 0.5; // 螺旋半径
    const SPIRAL_PITCH = Math.PI / 2; // 每单位距离的螺旋角度增量

    // 在起点生成大量爆破粒子
    spawnExplosionParticles(world, startLocation);

    // 构建局部坐标系
    let upVector = new org.bukkit.util.Vector(0, 1, 0); // 默认向上向量
    let rightVector = direction.clone().crossProduct(upVector).normalize(); // 右侧向量
    if (rightVector.lengthSquared() < 0.01) { // 如果方向几乎垂直于upVector，则重新选择upVector
        upVector = new org.bukkit.util.Vector(1, 0, 0);
        rightVector = direction.clone().crossProduct(upVector).normalize();
    }
    let upVectorLocal = rightVector.clone().crossProduct(direction).normalize(); // 本地向上向量

    for (let distance = 0; distance <= maxDistance; distance += PARTICLE_INTERVAL) {
        let particleLocation = startLocation.clone().add(direction.clone().multiply(distance));
        let angle = distance * SPIRAL_PITCH;

        // 计算偏移量，这里我们使用螺旋公式来计算3D空间中的偏移
        let offsetX = Math.sin(angle) * SPIRAL_RADIUS;
        let offsetZ = Math.cos(angle) * SPIRAL_RADIUS;

        // 将偏移转换到世界坐标系中
        let offsetVector = rightVector.clone().multiply(offsetX).add(upVectorLocal.clone().multiply(offsetZ));

        // 生成第一个螺旋的粒子
        spawnSpiralParticle(world, particleLocation.clone(), offsetVector, PARTICLE_TYPE);

        // 对于第二个螺旋，我们反转偏移向量来实现180度旋转的效果
        spawnSpiralParticle(world, particleLocation.clone(), offsetVector.clone().multiply(-1), PARTICLE_TYPE);
    }
}

// 辅助函数用于生成单个粒子
function spawnSpiralParticle(world, location, offsetVector, particleType) {
    location.add(offsetVector);
    world.spawnParticle(particleType, location.getX(), location.getY(), location.getZ(), 0);
}


// 辅助函数用于生成爆破粒子
function spawnExplosionParticles(world, location) {
    const EXPLOSION_PARTICLE_COUNT = 8; // 爆炸粒子的数量
    const PARTICLE_OFFSET = 0.2; // 粒子的随机偏移范围
    const EXPLOSION_PARTICLE_TYPE = Particle.CLOUD; 

    for (let i = 0; i < EXPLOSION_PARTICLE_COUNT; i++) {
        // 随机生成每个粒子的位置偏移
        let offsetX = (Math.random() - 0.5) * PARTICLE_OFFSET * 2;
        let offsetY = (Math.random() - 0.5) * PARTICLE_OFFSET * 2;
        let offsetZ = (Math.random() - 0.5) * PARTICLE_OFFSET * 2;

        // 生成爆破粒子
        world.spawnParticle(EXPLOSION_PARTICLE_TYPE, 
                            location.getX() + offsetX, 
                            location.getY() + offsetY, 
                            location.getZ() + offsetZ, 
                            0);
    }
}

// 处理射线追踪结果
function handleRayTraceResult(rayTraceResults, player) {
    if (rayTraceResults == null || rayTraceResults.getHitEntity() == null) {
        sendMessage(player, "§b没有击中任何目标~");
        return;
    }

    let entity = rayTraceResults.getHitEntity();

    // 检查是否击中了生物
    if (entity instanceof org.bukkit.entity.LivingEntity) {
        applyDamage(entity, player);
    } else {
        sendMessage(player, "§b击中了一个非生物目标~");
    }
}

// 对生物应用伤害
function applyDamage(entity, player) {
    const DAMAGE_AMOUNT = 1000; // 伤害值（半心为单位）

    entity.damage(DAMAGE_AMOUNT, player); // 对生物造成伤害，并指定伤害来源为玩家

    sendMessage(player, `§b成功击中并伤害了 ${entity.getName()}！`);
}

// 发送消息给玩家
function sendMessage(player, message) {
    player.sendMessage(message);
}

//信标激活声音
function playIgniteTNTSound(world, location) {
    const TNT_IGNITE_SOUND_NAME = "block.beacon.activate"; // 使用完整的命名空间字符串
    const VOLUME = 1.0; // 音量
    const PITCH = 1.0; // 音调

    // 播放点燃TNT的音效，使用声音名称字符串
    world.playSound(location, TNT_IGNITE_SOUND_NAME, VOLUME, PITCH);
}