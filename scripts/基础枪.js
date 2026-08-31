// Magic Gun 1 - restored Legacy implementation.
//
// The original Magic_RSC configuration referenced this script, but the file
// was never committed upstream. Keep the implementation intentionally simple:
// a short-cooldown hitscan magic weapon for the existing magic_test item.

const EquipmentSlot = Java.type('org.bukkit.inventory.EquipmentSlot');
const FluidCollisionMode = Java.type('org.bukkit.FluidCollisionMode');
const LivingEntity = Java.type('org.bukkit.entity.LivingEntity');
const Material = Java.type('org.bukkit.Material');
const Particle = Java.type('org.bukkit.Particle');
const Sound = Java.type('org.bukkit.Sound');

const RANGE = 20.0;
const DAMAGE = 5.0;
const COOLDOWN_TICKS = 8;
const BEAM_STEP = 0.5;

function onUse(event) {
    if (event.getHand() !== EquipmentSlot.HAND) {
        return;
    }

    const player = event.getPlayer();
    if (player.hasCooldown(Material.STICK)) {
        return;
    }

    player.setCooldown(Material.STICK, COOLDOWN_TICKS);

    const world = player.getWorld();
    const eye = player.getEyeLocation();
    const direction = eye.getDirection().normalize();
    const start = eye.clone().add(direction.clone().multiply(0.5));

    // Match the ray-tracing style already used by the original Magic scripts.
    const result = world.rayTrace(
        start,
        direction,
        RANGE,
        FluidCollisionMode.NEVER,
        true,
        0.15,
        null
    );

    let beamLength = RANGE;
    if (result != null && result.getHitPosition() != null) {
        beamLength = start.toVector().distance(result.getHitPosition());
    }

    drawBeam(world, start, direction, beamLength);
    world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.45, 1.65);

    if (result == null) {
        return;
    }

    const entity = result.getHitEntity();
    if (entity != null && LivingEntity.class.isInstance(entity) && entity !== player) {
        entity.damage(DAMAGE, player);
    }
}

function drawBeam(world, start, direction, length) {
    for (let distance = 0.0; distance <= length; distance += BEAM_STEP) {
        const point = start.clone().add(direction.clone().multiply(distance));
        world.spawnParticle(Particle.END_ROD, point, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
