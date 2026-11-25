package com.feni.nullskin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MimicStalkerEntity extends PathfinderMob {

    private int lifeTicks = 0;
    private int staredAtTicks = 0;
    private boolean mimicNameAssigned = false;

    public MimicStalkerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 36.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.85D));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        lifeTicks++;

        // Impersonate a nearby player's username on spawn to mess with peripheral vision
        if (!mimicNameAssigned) {
            mimicNameAssigned = true;
            List<? extends Player> players = this.level().players();
            if (!players.isEmpty()) {
                Player target = players.get(this.random.nextInt(players.size()));
                this.setCustomName(Component.literal(target.getName().getString()));
                this.setCustomNameVisible(true);
            }
        }

        Player nearest = this.level().getNearestPlayer(this, 28.0D);
        if (nearest == null) {
            if (lifeTicks > 400) {
                dissolveQuietly();
            }
            return;
        }

        // Keep a stalking distance rather than rushing like a normal zombie
        double dist = this.distanceTo(nearest);
        if (dist > 9.0D) {
            this.getNavigation().moveTo(nearest, 1.0D);
        } else {
            this.getNavigation().stop();
            this.getLookControl().setLookAt(nearest, 30.0F, 30.0F);
        }

        // Check if the player is aiming their crosshair near this mimic
        if (isPlayerLookingAtMe(nearest)) {
            staredAtTicks++;
            if (staredAtTicks == 15) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.9F, 0.65F);
            }
            if (staredAtTicks >= 35 || dist < 3.5D) {
                dissolveQuietly();
            }
        } else {
            staredAtTicks = Math.max(0, staredAtTicks - 1);
        }

        // Vanish after ~25 seconds so it stays a sighting rather than a permanent pet mob
        if (lifeTicks > 500) {
            dissolveQuietly();
        }
    }

    private boolean isPlayerLookingAtMe(Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 toEntity = this.getEyePosition().subtract(player.getEyePosition()).normalize();
        double dot = look.dot(toEntity);
        return dot > 0.94D && player.hasLineOfSight(this);
    }

    private void dissolveQuietly() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(), 18, 0.3, 0.6, 0.3, 0.02);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
        this.discard();
    }
}