const MAGIC_FOODS = [
    { id: "MAGIC_FOODS_JIAOZI", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').GLOWING, description: "发光" },
    { id: "MAGIC_FOODS_APPLE", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').SPEED, description: "速度" },
    { id: "MAGIC_FOODS_A_1", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').FIRE_RESISTANCE, description: "防火" },
    { id: "MAGIC_FOODS_A_2", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').HEALTH_BOOST, description: "生命提升" },
    { id: "MAGIC_FOODS_A_3", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').INVISIBILITY, description: "隐身" },
    { id: "MAGIC_FOODS_A_4", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').LEVITATION, description: "漂浮" },
    { id: "MAGIC_FOODS_A_5", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').SATURATION, description: "饱和" },
    { id: "MAGIC_FOODS_A_6", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').WEAKNESS, description: "虚弱" },
    { id: "MAGIC_FOODS_A_7", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').CONDUIT_POWER, description: "潮涌能量" },
    { id: "MAGIC_FOODS_A_8", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').ABSORPTION, description: "伤害吸收" },
    { id: "MAGIC_FOODS_A_9", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').REGENERATION, description: "生命恢复" },
    { id: "MAGIC_FOODS_A_10", PotionEffectType: Java.type('org.bukkit.potion.PotionEffectType').LUCK, description: "幸运" }
];


function onEat(event, player, itemStack) {

    // 获取触发事件的玩家对象  
    var player = event.getPlayer();

    var PotionEffect = Java.type('org.bukkit.potion.PotionEffect');
    // var PotionEffectType = Java.type('org.bukkit.potion.PotionEffectType');

    var onUseItem = event.getItem();
    var itemStack = getSfItemByItem(onUseItem);
    var sfitemid = itemStack.getId();
      // 查找匹配的配置
     let entityConfig = MAGIC_FOODS.find(entity => entity.id === sfitemid);

    if (entityConfig) {

        var effect = entityConfig.PotionEffectType;
        var name = entityConfig.description;

    } else {
        return;
        // org.bukkit.Bukkit.broadcastMessage("未找到匹配的实体配置");
    }


    var A = Math.floor(Math.random() * 50); //随机效果等级

    var B = Math.floor(Math.random() * 100); //随机效果时间  0-100

    var C = A+1;

    let effect_new = new PotionEffect(effect, 20 * B, A, true, true, true); // 持续60秒，等级2

     // 添加药水效果到玩家
    player.addPotionEffect(effect_new);

    // 发送消息给玩家
    player.sendMessage("§b你获得了等为§e " + C + " §b的 §e" + name + "§b效果，此效果将持续 §e" + B + "§b  seconds。");



    // 运行op指令
    // runOpCommand(player, "effect give " + player.getName() + " fire_resistance 61 50");



}