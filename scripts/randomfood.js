function onEat(event, player, itemStack) {

    // 获取触发事件的玩家对象  
    var player = event.getPlayer();

    // var D = Math.floor(Math.random() * 1) + 1;

    // var effect_item = "";

    // switch (D) {
    //     case 1:
    //         effect_item = "SPEED";
    //         break;
    //     case 2:
    //         effect_item = "REGENERATION";
    //         break;
    //     case 3:
    //         effect_item = "FIRE_RESISTANCE";
    //         break;
    //     case 4:
    //         effect_item = "WATER_BREATHING";
    //         break;
    //     case 5:
    //         effect_item = "INVISIBILITY";
    //         break;
    //     case 6:
    //         effect_item = "BLINDNESS";
    //         break;
    //     case 7:
    //         effect_item = "NIGHT_VISION";
    //         break;
    //     case 8:
    //         effect_item = "HUNGER";
    //         break;
    //     case 9:
    //         effect_item = "WEAKNESS";
    //         break;
    //     case 10:
    //         effect_item = "POISON";
    //         break;
    //     case 11:
    //         effect_item = "WITHER";
    //         break;
    //     case 12:
    //         effect_item = "HEALTH_BOOST";
    //         break;
    //     case 13:
    //         effect_item = "ABSORPTION";
    //         break;
    //     case 14:
    //         effect_item = "SATURATION";
    //         break;
    //     case 15:
    //         effect_item = "GLOWING";
    //         break;
    //     case 16:
    //         effect_item = "LEVITATION";
    //         break;
    //     case 17:
    //         effect_item = "LUCK";
    //         break;
    //     case 18:
    //         effect_item = "UNLUCK";
    //         break;
    //     case 19:
    //         effect_item = "SLOW_FALLING";
    //         break;
    //     case 20:
    //         effect_item = "CONDUIT_POWER";
    //         break;
    //     case 21:
    //         effect_item = "DOLPHINS_GRACE";
    //         break;
    //     case 22:
    //         effect_item = "BAD_OMEN";
    //         break;
    //     case 23:
    //         effect_item = "HERO_OF_THE_VILLAGE";
    //         break;
    //     case 24:
    //         effect_item = "DARKNESS";
    //         break;
    // }
    

    var PotionEffect = Java.type('org.bukkit.potion.PotionEffect');
    var PotionEffectType = Java.type('org.bukkit.potion.PotionEffectType');

    // var A = Math.floor(Math.random() * 50); //随机效果等级

    // var B = Math.floor(Math.random() * 100); //随机效果时间  0-100

    // var C = A+1

    // let Random_effect = new PotionEffect(PotionEffectType(effect_item), 20 * B, A, true, true, true); // 持续?秒，等级?

    //  // 添加药水效果到玩家
    // player.addPotionEffect(Random_effect);

    // 发送消息给玩家

    var D = Math.floor(Math.random() * 3) + 1;   //随机获得1-3个效果    效果可能会重复


    for (E = 0; E < D; E++){
     // 创建一个映射表，将字符串键映射到对应的 PotionEffectType 枚举值
     var effectMapping = {
        "SPEED": PotionEffectType.SPEED,
        "REGENERATION": PotionEffectType.REGENERATION,
        "FIRE_RESISTANCE": PotionEffectType.FIRE_RESISTANCE,
        "WATER_BREATHING": PotionEffectType.WATER_BREATHING,
        "INVISIBILITY": PotionEffectType.INVISIBILITY,
        "BLINDNESS": PotionEffectType.BLINDNESS,
        "NIGHT_VISION": PotionEffectType.NIGHT_VISION,
        "HUNGER": PotionEffectType.HUNGER,
        "WEAKNESS": PotionEffectType.WEAKNESS,
        "POISON": PotionEffectType.POISON,
        "WITHER": PotionEffectType.WITHER,
        "HEALTH_BOOST": PotionEffectType.HEALTH_BOOST,
        "ABSORPTION": PotionEffectType.ABSORPTION,
        "SATURATION": PotionEffectType.SATURATION,
        "GLOWING": PotionEffectType.GLOWING,
        "LEVITATION": PotionEffectType.LEVITATION,
        "LUCK": PotionEffectType.LUCK,
        "UNLUCK": PotionEffectType.UNLUCK,
        "SLOW_FALLING": PotionEffectType.SLOW_FALLING,
        "CONDUIT_POWER": PotionEffectType.CONDUIT_POWER,
        "DOLPHINS_GRACE": PotionEffectType.DOLPHINS_GRACE,
        "BAD_OMEN": PotionEffectType.BAD_OMEN,
        "HERO_OF_THE_VILLAGE": PotionEffectType.HERO_OF_THE_VILLAGE,
        "DARKNESS": PotionEffectType.DARKNESS
    };




    // 随机选择一个效果类型
    var keys = Object.keys(effectMapping);
    var randomEffectKey = keys[Math.floor(Math.random() * keys.length)];
    var effectItem = effectMapping[randomEffectKey];

    if (effectItem === undefined) {
        // 处理可能的错误情况
        player.sendMessage("§bMagic Legacy action could not be completed.");
        return;
    }

    // 随机效果等级和持续时间
    var A = Math.floor(Math.random() * 50); // 随机效果等级
    var B = Math.floor(Math.random() * 100); // 随机效果时间（0-100秒）

    var C = A+1;

    // 创建并添加药水效果
    let Random_effect = new PotionEffect(effectItem, 20 * B, A, true, true, true);

    player.addPotionEffect(Random_effect);

    




    player.sendMessage("§bMagic Legacy action could not be completed." + C + "§bMagic Legacy action could not be completed." + B + "§bMagic Legacy action could not be completed.");


    }


    player.sendMessage("§bMagic Legacy action could not be completed." + E + "§bMagic Legacy action could not be completed.");





    // 运行op指令
    // runOpCommand(player, "effect give " + player.getName() + " " + effect_item);



}
