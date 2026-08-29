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

    // 减少主手物品的数量
    decrementItemAmount(player.getInventory().getItemInMainHand());

    // 获取世界和玩家眼睛位置及朝向
    let world = player.getWorld();
    let eyeLocation = player.getEyeLocation();
    let direction = eyeLocation.getDirection().normalize(); // 获取标准化后的方向向量
    let startLocation = eyeLocation.clone().add(direction); // 射线起点位置（眼睛位置加上方向向量）

    let maxDistance = 1; // 最大射程
    // 生成粒子效果
    generateParticleBeam(world, startLocation, direction, maxDistance);


    // player.sendMessage("验证执行");
}

// 生成粒子效果
function generateParticleBeam(world, startLocation, direction, maxDistance) {
    const PARTICLE_TYPE = Particle.END_ROD; // 使用末影杆粒子作为示例
    const PARTICLE_INTERVAL = 0.01; // 粒子之间的间隔距离

    for (let distance = 0; distance <= maxDistance; distance += PARTICLE_INTERVAL) {
        let particleLocation = startLocation.clone().add(direction.clone().multiply(distance));
        world.spawnParticle(PARTICLE_TYPE, particleLocation.getX(), particleLocation.getY(), particleLocation.getZ(), 50);
    }
}

function decrementItemAmount(item) {
    if (item && item.getAmount() > 1) {
      item.setAmount(item.getAmount() - 1);
    } else if (item) {
      item.setAmount(0);
    }
  }
  
