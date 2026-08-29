const Location = Java.type('org.bukkit.Location');
const LivingEntity = Java.type('org.bukkit.entity.LivingEntity');
const Particle = Java.type('org.bukkit.Particle');

const sfIdGetter = (itemStack) => {
    return getSfItemByItem(itemStack).getId();
}

const MAGIC_BANNER = [
    { id: 1, sfId: "MAGIC_BANNER_LIANHUN", particleType1: Particle.SOUL_FIRE_FLAME, particleType2: Particle.SOUL, radius1: 10, height1: 10, radius2: 30, height2: 30, damage1: 0.003, damage2: 8, soulConsume1: 10, soulConsume2: 0.5, particleNum1: 1, particleNum2: 10, reduce1: 7, reduce2: 1 / 3, knock1: 2, knock2: 15, description: "炼魂幡,reduce1向下取整，0-3，reduce2，固定值1/3" },
];

const bannerConfigGetter = (MainHandItemSfId) => {
    // 查找匹配的配置
    return MAGIC_BANNER.find(banner => banner.sfId === MainHandItemSfId);
}

const MainHandIf = (hand) => {
    if (hand !== org.bukkit.inventory.EquipmentSlot.HAND) {
        return true;
    } else {
        return false;
    }
}


const mainHandItem = (player) => {
    return player.getInventory().getItemInMainHand();
}


const modifyItemLore = (item) => {

    let itemNum = item.getAmount();
    if(itemNum!=1){
        return;
    }

    let itemMeta = item.getItemMeta();
    // 获取物品的Lore
    let lore = itemMeta.getLore() ? itemMeta.getLore() : [];
    // 复制0-4行
    let newLore = lore.slice(0, 4);
    let killNumLine = lore[4];
    let matchResult = killNumLine.match(/(\D*)(\d+)(\D*)/);
    if (matchResult) {
        let prefix = matchResult[1] || "";
        //原始灵魂数量
        let originnumber = parseInt(matchResult[2], 10);
        //灵魂数量+1
        let number = parseInt(matchResult[2], 10) + 1;
        if(number>1888888)number=1888888;
        let suffix = matchResult[3] || "";
        let updatedKillNumLine = `${prefix}${number}${suffix}`;
        newLore.push(updatedKillNumLine);
    } else {
        newLore.push(killNumLine);
    }
    newLore = newLore.concat(lore.slice(5));
    // org.bukkit.Bukkit.broadcastMessage("newLore:" + newLore);
    lore = newLore;
    itemMeta.setLore(lore);
    item.setItemMeta(itemMeta);

}

const getSoulNum = (item) => {
    let itemMeta = item.getItemMeta();
    let lore = itemMeta.getLore() ? itemMeta.getLore() : [];
    let killNumLine = lore[4];
    let matchResult = killNumLine.match(/(\D*)(\d+)(\D*)/);
    return originnumber = parseInt(matchResult[2], 10);
}

const modifyItemLoreReduceLittle = (item, reduce, player, damage) => {

    let itemMeta = item.getItemMeta();
    // 获取物品的Lore
    let lore = itemMeta.getLore() ? itemMeta.getLore() : [];
    // 复制0-4行
    let newLore = lore.slice(0, 4);
    let killNumLine = lore[4];
    let matchResult = killNumLine.match(/(\D*)(\d+)(\D*)/);
    if (matchResult) {
        let prefix = matchResult[1] || "";
        //原始灵魂数量
        let originnumber = parseInt(matchResult[2], 10);
        let reduceNum = Math.floor(Math.random() * reduce)
        //消耗灵魂数量
        let number = originnumber - reduceNum;
        let suffix = matchResult[3] || "";
        let updatedKillNumLine = `${prefix}${number}${suffix}`;
        // 播报
        let messages = [
            "§f§l======================================================",
            "§a§l你挥动了炼魂幡，",
            "§6§l一道光芒闪过，周围的敌人受到了轻微的冲击！",
            "§c§l当前灵魂数量: §e" + originnumber,
            "§b§l造成的伤害: §e" + damage * originnumber,
            "§7§l炼魂幡汲取了幡内的灵魂能量，随机消耗了§e" + reduceNum + " §7个灵魂。",
            "§c§l剩余灵魂数量: §e" + number,
            "§a§l你的力量得到了短暂的提升！",
            "§f§l======================================================",
        ];
        messages.forEach(msg => player.sendMessage(msg));
        // 播报
        newLore.push(updatedKillNumLine);
    } else {
        newLore.push(killNumLine);
    }
    newLore = newLore.concat(lore.slice(5));
    // org.bukkit.Bukkit.broadcastMessage("newLore:" + newLore);
    lore = newLore;
    itemMeta.setLore(lore);
    item.setItemMeta(itemMeta);

}

const modifyItemLoreReduceUltra = (item, reduce, player, damage) => {

    let itemMeta = item.getItemMeta();
    // 获取物品的Lore
    let lore = itemMeta.getLore() ? itemMeta.getLore() : [];
    // 复制0-4行
    let newLore = lore.slice(0, 4);
    let killNumLine = lore[4];
    let matchResult = killNumLine.match(/(\D*)(\d+)(\D*)/);
    if (matchResult) {
        let prefix = matchResult[1] || "";
        //原始灵魂数量
        let originnumber = parseInt(matchResult[2], 10);
        let reduceNum = Math.ceil(originnumber * reduce);
        //消耗灵魂数量
        let number = originnumber - reduceNum;
        let suffix = matchResult[3] || "";
        let updatedKillNumLine = `${prefix}${number}${suffix}`;
        // 播报
        let messages = [
            "§f§l======================================================",
            "§a§l你深吸一口气，挥舞着炼魂幡，",
            "§4§l一阵强大的波动席卷四周，所有敌人都被震退！",
            "§c§l当前灵魂数量: §e" + originnumber,
            "§b§l造成的伤害: §e" + damage * originnumber,
            "§7§l炼魂幡汲取了幡内的大量的灵魂能量，消耗了§e" + reduceNum + " §7个灵魂。",
            "§c§l剩余灵魂数量: §e" + number,
            "§c§l你感到一阵虚弱，身体变得沉重...",
            "§f§l======================================================",
        ];
        messages.forEach(msg => player.sendMessage(msg));
        // 播报
        newLore.push(updatedKillNumLine);
    } else {
        newLore.push(killNumLine);
    }
    newLore = newLore.concat(lore.slice(5));
    // org.bukkit.Bukkit.broadcastMessage("newLore:" + newLore);
    lore = newLore;
    itemMeta.setLore(lore);
    item.setItemMeta(itemMeta);

}


//处理击杀事件
const killSoul = (event, item) => {

    if (item.getAmount() !== 1) {
        return;
    }
    const entity = event.getEntity();
    let finalHealth = entity.getHealth() - event.getDamage();
    // org.bukkit.Bukkit.broadcastMessage("finalHealth:" + finalHealth);
    if (finalHealth <= 0) {
        modifyItemLore(item);
    }

}

const onUseStart = (event) => {
    let player = event.getPlayer();
    if (MainHandIf(event.getHand())) {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }
    let bannerConfig = bannerConfigGetter(sfIdGetter(event.getItem()));

    if (!bannerConfig) {
        return;
    }

    let item = mainHandItem(player);
    let soulNum = getSoulNum(item);
    if (soulNum <= 100) {
        sendMessage(player, "§bMagic Legacy action could not be completed.");
        return;
    }

    let isShift = event.getPlayer().isSneaking()
    // org.bukkit.Bukkit.broadcastMessage("isShift:" + isShift);

    if (isShift) {
        ultreEffect(bannerConfig.id, player);
        modifyItemLoreReduceUltra(item, bannerConfig.reduce2, player, bannerConfig.damage2);
        ultraSkill(soulNum, player, bannerConfig.radius2, bannerConfig.height2, bannerConfig.particleType2, bannerConfig.damage2, bannerConfig.particleNum2, bannerConfig.knock2);
        return;
    }
    littleEffect(bannerConfig.id, player);
    modifyItemLoreReduceLittle(item, bannerConfig.reduce1, player, bannerConfig.damage1);
    littleSkill(soulNum, player, bannerConfig.radius1, bannerConfig.height1, bannerConfig.particleType1, bannerConfig.damage1, bannerConfig.particleNum1, bannerConfig.knock1);
}

// 辅助函数用于播放粒子效果，形成空心圆柱
function playHollowCylinderParticles(player, radius, height, particleType, num) {
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
            world.spawnParticle(particleType, particleLoc, num);

        }
    }
}

// 辅助函数用于对范围内的生物造成伤害
function damageEntitiesInRange(player, centerLocation, radius, height, damage, sound, knock) {
    let world = centerLocation.getWorld();
    let entities = world.getNearbyEntities(centerLocation, radius, height, radius);

    playSoundAround(world, centerLocation, sound);

    entities.forEach(function (entity) {
        if (entity instanceof LivingEntity) {
            // 计算方向向量
            let direction = entity.getLocation().subtract(centerLocation).toVector().normalize();
            if (entity === player) {
                // console.log("过滤了执行技能的玩家: "+player);
                return;
            }
            // 检查并修正任何非有限的向量分量
            if (!Number.isFinite(direction.getX()) || !Number.isFinite(direction.getY()) || !Number.isFinite(direction.getZ())) {
                // console.log("检测到非有限的向量分量: " + direction);
                direction.setX(Number.isFinite(direction.getX()) ? direction.getX() : 0);
                direction.setY(Number.isFinite(direction.getY()) ? direction.getY() : 0);
                direction.setZ(Number.isFinite(direction.getZ()) ? direction.getZ() : 0);
            }
            // 应用击退效果
            entity.setVelocity(direction.multiply(knock));
            entity.damage(damage,player);
        }
    });
}



const littleSkill = (soulNum, player, radius, height, particleType, damage, num, knock) => {

    playHollowCylinderParticles(player, radius, height, particleType, num);

    damageEntitiesInRange(player, player.getLocation(), radius, height, soulNum * damage, "entity.allay.death", knock);

    // sendMessage(player, "§b对范围内的生物造成了"+soulNum*damage+"点伤害！");

}

const ultraSkill = (soulNum, player, radius, height, particleType, damage, num, knock) => {

    playHollowCylinderParticles(player, radius, height, particleType, num);

    damageEntitiesInRange(player, player.getLocation(), radius, height, soulNum * damage, "entity.zombie_villager.cure", knock);

    // sendMessage(player, "§b对范围内的生物造成了"+soulNum*damage+"点伤害！");

}


const ultreEffect = (id, player) => {
    if (id == 1) {
        // 创建PotionEffect实例
        let blindnessEffect = new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.BLINDNESS, // 效果类型：失明
            100, // 持续时间(ticks)，5秒=100 ticks (20 ticks/秒)
            255 // 等级(强度)-最大值
        );

        let slownessEffect = new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.LUCK, // 效果类型：缓慢
            100, // 持续时间(ticks)，5秒=100 ticks
            255 // 等级(强度)-最大值
        );

        let weaknessEffect = new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WEAKNESS, // 效果类型：虚弱
            600, // 持续时间(ticks)，30秒=600 ticks
            255 // 等级(强度)-最大值
        );
        // 播报
        let messages = [
            "§f§l======================================================",
            "§c你的眼前突然陷入了一片黑暗！",
            "§c你的身体变得异常沉重，行动迟缓。",
            "§c一股无力感迅速蔓延全身，力量消逝..."
        ];

        messages.forEach(msg => player.sendMessage(msg));

        // 给予玩家效果
        if(blindnessEffect!=null){
        player.addPotionEffect(blindnessEffect);
        }else{

        }
        if(slownessEffect!=null){
        player.addPotionEffect(slownessEffect);
        }else{

        }
        if(weaknessEffect!=null){
        player.addPotionEffect(weaknessEffect);
        }else{

        }
    }

}

const littleEffect = (id, player) => {
    if (id == 1) {
        // 创建PotionEffect实例
        let nightVisionEffect = new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.NIGHT_VISION, // 效果类型：夜视
            100, // 持续时间(ticks)，5秒=100 ticks (20 ticks/秒)
            4 // 等级(强度)-等级值为实际等级减1，因此5级对应值为4
        );

        let speedEffect = new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SPEED, // 效果类型：速度
            100, // 持续时间(ticks)，5秒=100 ticks
            4 // 等级(强度)-等级值为实际等级减1，因此5级对应值为4
        );

        let strengthEffect = new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.LUCK, // 效果类型：力量（在API中名为INCREASE_DAMAGE）
            600, // 持续时间(ticks)，30秒=600 ticks
            4 // 等级(强度)-等级值为实际等级减1，因此5级对应值为4
        );
        // 播报
        let messages = [
            "§f§l======================================================",
            "§a你的眼睛适应了黑暗，视野变得清晰无比！",
            "§a你感到一阵轻盈，仿佛能瞬间穿越战场。",
            "§a一股强大的力量注入体内，每一次攻击都充满了破坏力！"
        ];

        messages.forEach(msg => player.sendMessage(msg));

        // 给予玩家效果
        if(nightVisionEffect!=null){
            player.addPotionEffect(nightVisionEffect);
        }else{
            // console.log("1");
        }
        if(speedEffect!=null){
            player.addPotionEffect(speedEffect);
        }else{
            // console.log("2");
        }  
        if(strengthEffect!=null){
            player.addPotionEffect(strengthEffect);
        }else{
            // console.log("3");
        }
    }

}

function onWeaponHit(event, player, item) {

    killSoul(event, item);

}




function onUse(event) {

    onUseStart(event);

}

const playSoundAround = (world, location, SOUND) => {
    // const SOUND = "block.beacon.activate"; // 使用完整的命名空间字符串
    const VOLUME = 1.0; // 音量
    const PITCH = 1.0; // 音调
    // 播放音效
    world.playSound(location, SOUND, VOLUME, PITCH);
}
