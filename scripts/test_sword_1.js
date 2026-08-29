// 导入必要的 Java 类型
const EquipmentSlot = Java.type('org.bukkit.inventory.EquipmentSlot');
const FluidCollisionMode = Java.type('org.bukkit.FluidCollisionMode');
const Particle = Java.type('org.bukkit.Particle');

// 主函数：当物品被使用时触发
function onUse(event) {
    let player = event.getPlayer();

    // 检查主手是否持有物品
    if (event.getHand() !== EquipmentSlot.HAND) {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }

    // 获取世界和玩家眼睛位置及朝向
    let world = player.getWorld();
    let eyeLocation = player.getEyeLocation();
    let direction = eyeLocation.getDirection().normalize(); // 获取标准化后的方向向量
    let startLocation = eyeLocation.clone().add(direction); // 射线起点位置（眼睛位置加上方向向量）
    let maxDistance = 5; // 最大射程

    // 执行射线追踪
    let rayTraceResults = world.rayTrace(startLocation, direction, maxDistance, FluidCollisionMode.ALWAYS, true, 0.1, null);

    // 生成粒子效果
    generateParticleBeam(world, startLocation, direction, maxDistance);

    // 处理射线追踪结果
    handleRayTraceResult(rayTraceResults, player);

    player.sendMessage("§bMagic Legacy action could not be completed.");
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

// 处理射线追踪结果
function handleRayTraceResult(rayTraceResults, player) {
    if (rayTraceResults == null || rayTraceResults.getHitEntity() == null) {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }

    let entity = rayTraceResults.getHitEntity();

    // 检查是否击中了生物
    if (entity instanceof org.bukkit.entity.LivingEntity) {
        applyDamage(entity, player);
    } else {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
    }
}

// 对生物应用伤害
function applyDamage(entity, player) {
    const DAMAGE_AMOUNT = 5; // 伤害值（半心为单位）

    entity.damage(DAMAGE_AMOUNT, player); // 对生物造成伤害，并指定伤害来源为玩家

    sendMessage(player, `§bSuccessfully hit and damaged ${entity.getName()}!`);
}

// 发送消息给玩家
function sendMessage(player, message) {
    player.sendMessage(message);
}
