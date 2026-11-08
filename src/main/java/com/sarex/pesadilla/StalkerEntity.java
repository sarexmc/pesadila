package com.sarex.pesadilla;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * El Acechador.
 * - Vigilante: se queda quieto a lo lejos mirandote. Si lo miras fijo desaparece; si lo ignoras o te acercas, se vuelve cazador.
 * - Cazador: corre hacia ti, pero se congela mientras lo miras (como un angel llorón). Si te alcanza de espaldas: jumpscare.
 * - Invulnerable: si lo golpeas, desaparece entre humo (y volvera).
 */
public class StalkerEntity extends Monster {
    private boolean hunter = false;
    private boolean frozen = false;
    private int seenTicks = 0;
    private int unseenTicks = 0;
    private int lifeTicks = 0;
    private int heartbeat = 0;

    public StalkerEntity(EntityType<? extends StalkerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void setHunter(boolean hunter) {
        this.hunter = hunter;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || frozen;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (++lifeTicks > 7200) {            // 6 minutos y se esfuma
            discard();
            return;
        }

        Player near = level().getNearestPlayer(getX(), getY(), getZ(), 100.0D, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (!(near instanceof ServerPlayer sp)) {
            frozen = true;
            if (lifeTicks > 400) discard();
            return;
        }

        boolean seen = isSeenBy(sp);
        double dist = distanceTo(sp);
        if (seen) {
            seenTicks++;
            unseenTicks = 0;
        } else {
            seenTicks = 0;
            unseenTicks++;
        }

        // ---- Vigilante ----
        if (!hunter) {
            frozen = true;
            faceTarget(sp);
            if (seenTicks > 50) {            // lo miras fijo 2,5 s -> desaparece
                vanish();
                return;
            }
            if (unseenTicks > 400 || dist < 9.0D) {
                becomeHunter(sp);
            }
            return;
        }

        // ---- Cazador ----
        if (seen) {
            frozen = true;
            faceTarget(sp);
            getNavigation().stop();
            if (seenTicks > 300) {           // aguantarle la mirada 15 s lo ahuyenta
                vanish();
                return;
            }
        } else {
            frozen = false;
            if (tickCount % 4 == 0) {
                getNavigation().moveTo(sp, 1.4D);
            }
            if (unseenTicks % 300 == 299 && dist > 30.0D) {
                teleportNear(sp);
            }
        }

        if (dist < 24.0D) {
            if (--heartbeat <= 0) {
                ScareDirector.playOn(sp, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
                heartbeat = (int) (14 + dist * 1.5D);
            }
            if (dist < 14.0D && tickCount % 20 == 0) {
                sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, false));
            }
        }

        if (!seen && dist < 1.8D) {
            catchPlayer(sp);
        }
    }

    private void becomeHunter(ServerPlayer sp) {
        hunter = true;
        ScareDirector.playOn(sp, SoundEvents.WARDEN_NEARBY_CLOSEST, 1.0F, 0.8F);
    }

    private void catchPlayer(ServerPlayer sp) {
        ScareDirector.jumpscare(sp, 0);
        float dmg = ScareDirector.lethal ? 6.0F : Math.min(6.0F, sp.getHealth() - 1.0F);
        if (dmg > 0) sp.hurt(damageSources().mobAttack(this), dmg);
        sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        discard();
    }

    private void teleportNear(ServerPlayer sp) {
        Vec3 spot = ScareDirector.findSpot((ServerLevel) level(), sp, 18, 26, true);
        if (spot != null) {
            moveTo(spot.x, spot.y, spot.z, getYRot(), 0.0F);
            getNavigation().stop();
        }
    }

    private void vanish() {
        ServerLevel sl = (ServerLevel) level();
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.2D, getZ(), 40, 0.3D, 0.9D, 0.3D, 0.02D);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_DEATH, SoundSource.HOSTILE, 1.0F, 0.4F);
        discard();
    }

    private void faceTarget(Player p) {
        this.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
        this.setYHeadRot(this.getYRot());
        this.setYBodyRot(this.getYRot());
    }

    private boolean isSeenBy(Player p) {
        Vec3 view = p.getViewVector(1.0F).normalize();
        Vec3 diff = new Vec3(getX() - p.getX(), getEyeY() - p.getEyeY(), getZ() - p.getZ());
        double d = diff.length();
        if (d < 0.001D) return true;
        double dot = view.dot(diff.scale(1.0D / d));
        return dot > 0.55D && p.hasLineOfSight(this);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (!level().isClientSide && source.getEntity() instanceof ServerPlayer) {
            vanish();
        }
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hunter", hunter);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hunter = tag.getBoolean("Hunter");
    }
}
