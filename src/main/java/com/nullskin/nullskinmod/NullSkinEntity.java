package com.feni.nullskin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.UUID;

public class NullSkinEntity extends Monster implements GeoEntity {
    public static final int STATE_IDLE_STALK = 0;
    public static final int STATE_MIMIC_FREEZE = 1;
    public static final int STATE_CREEP_IN = 2;
    public static final int STATE_LUNGE = 3;

    public static final int MIMIC_NONE = 0;
    public static final int MIMIC_PASSIVE_MOB = 1;
    public static final int MIMIC_PLAYER = 2;

    private static final EntityDataAccessor<Integer> DATA_STALK_STATE =
            SynchedEntityData.defineId(NullSkinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MIMIC_TYPE =
            SynchedEntityData.defineId(NullSkinEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.nullskin.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.nullskin.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID possessedPlayerId;
    @Nullable
    private UUID friendPlayerId;
    private boolean playingBackVoice = false;
    private int lungeCooldown = 0;
    private int possessionTicksRemaining = 0;
    private boolean recordedFirstManifest = false;

    public NullSkinEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STALK_STATE, STATE_IDLE_STALK);
        this.entityData.define(DATA_MIMIC_TYPE, MIMIC_NONE);
    }

    public int getStalkState() {
        return this.entityData.get(DATA_STALK_STATE);
    }

    public void setStalkState(int state) {
        this.entityData.set(DATA_STALK_STATE, state);
    }

    public int getMimicType() {
        return this.entityData.get(DATA_MIMIC_TYPE);
    }

    public void setMimicType(int mimicType) {
        this.entityData.set(DATA_MIMIC_TYPE, mimicType);
    }

    public void startPlayerPossession(Player target, @Nullable Player friend) {
        this.possessedPlayerId = target.getUUID();
        this.friendPlayerId = friend != null ? friend.getUUID() : null;
        this.possessionTicksRemaining = 200; // 10 seconds
        this.setMimicType(MIMIC_PLAYER);
    }

    public void clearPossession() {
        this.possessedPlayerId = null;
        this.friendPlayerId = null;
        this.playingBackVoice = false;
        this.possessionTicksRemaining = 0;
        if (this.getMimicType() == MIMIC_PLAYER) {
            this.setMimicType(MIMIC_NONE);
        }
    }

    @Nullable
    public UUID getPossessedPlayerId() {
        return this.possessedPlayerId;
    }

    @Nullable
    public UUID getFriendPlayerId() {
        return this.friendPlayerId;
    }

    public boolean isPlayingBackVoice() {
        return this.playingBackVoice;
    }

    public void setPlayingBackVoice(boolean playingBackVoice) {
        this.playingBackVoice = playingBackVoice;
    }

    public boolean isVoiceChatLoaded() {
        return ModList.get().isLoaded("voicechat");
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide && target instanceof Player victim && this.possessedPlayerId == null) {
            Player nearestFriend = this.level().players().stream()
                    .map(p -> (Player) p)
                    .filter(p -> p != victim && p.isAlive() && !p.isSpectator())
                    .min(Comparator.comparingDouble((Player p) -> p.distanceToSqr(this)))
                    .orElse(null);

            this.startPlayerPossession(victim, nearestFriend);
        }
        return hit;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new StalkAndMimicGoal(this, 1.15D, 28.0D, 8.0D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.lungeCooldown > 0) {
            this.lungeCooldown--;
        }

        if (!this.level().isClientSide) {
            if (!this.recordedFirstManifest) {
                Player nearest = this.level().getNearestPlayer(this, 32.0D);
                if (nearest instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator()) {
                    this.recordedFirstManifest = true;
                    NullSkinEnforcement.onFirstManifestation(sp);
                }
            }

            if (this.possessedPlayerId != null) {
                if (--this.possessionTicksRemaining <= 0) {
                    this.clearPossession();
                    return;
                }

                Player possessed = this.level().getPlayerByUUID(this.possessedPlayerId);
                if (possessed instanceof ServerPlayer serverPossessed && serverPossessed.isAlive()) {
                    serverPossessed.setDeltaMovement(Vec3.ZERO);
                    serverPossessed.hurtMarked = true;

                    if (this.isVoiceChatLoaded() && this.friendPlayerId != null) {
                        Player friend = this.level().getPlayerByUUID(this.friendPlayerId);
                        if (friend != null && friend.isAlive() && friend.distanceToSqr(this) <= 256.0D) {
                            NullSkinVoiceAddon.triggerPlaybackToFriend(this, this.possessedPlayerId);
                        }
                    }
                } else {
                    this.clearPossession();
                }
            }
        }
    }

    private boolean isPlayerLookingAtMe(Player player, double threshold) {
        Vec3 lookVec = player.getLookAngle().normalize();
        Vec3 toEntity = new Vec3(
                this.getX() - player.getX(),
                this.getEyeY() - player.getEyeY(),
                this.getZ() - player.getZ()
        ).normalize();
        return lookVec.dot(toEntity) > threshold;
    }

    private void performLungeToward(Player target) {
        Vec3 dir = new Vec3(
                target.getX() - this.getX(),
                0.0D,
                target.getZ() - this.getZ()
        );
        if (dir.lengthSqr() > 1.0E-4D) {
            Vec3 impulse = dir.normalize().scale(0.95D).add(0.0D, 0.28D, 0.0D);
            this.setDeltaMovement(impulse);
            this.hasImpulse = true;
        }
        this.getNavigation().moveTo(target, 1.45D);
        if (this.distanceToSqr(target) <= 4.0D) {
            this.doHurtTarget(target);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "locomotion", 4, state -> {
            if (this.getStalkState() == STATE_MIMIC_FREEZE) {
                return PlayState.STOP;
            }
            if (state.isMoving()) {
                state.setAndContinue(WALK_ANIM);
            } else {
                state.setAndContinue(IDLE_ANIM);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private static class StalkAndMimicGoal extends Goal {
        private final NullSkinEntity mob;
        private final double speedModifier;
        private final double stalkRadius;
        private final double ambushDistance;
        private Player targetPlayer;
        private int repathCooldown;

        public StalkAndMimicGoal(NullSkinEntity mob, double speedModifier, double stalkRadius, double ambushDistance) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.stalkRadius = stalkRadius;
            this.ambushDistance = ambushDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player nearest = this.mob.level().getNearestPlayer(this.mob, this.stalkRadius);
            if (nearest == null || nearest.isCreative() || nearest.isSpectator()) {
                return false;
            }
            this.targetPlayer = nearest;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
                return false;
            }
            if (this.targetPlayer.isCreative() || this.targetPlayer.isSpectator()) {
                return false;
            }
            return this.mob.distanceToSqr(this.targetPlayer) <= (this.stalkRadius * this.stalkRadius);
        }

        @Override
        public void start() {
            this.repathCooldown = 0;
            this.mob.setStalkState(STATE_IDLE_STALK);
        }

        @Override
        public void stop() {
            this.targetPlayer = null;
            this.mob.getNavigation().stop();
            this.mob.setStalkState(STATE_IDLE_STALK);
        }

        @Override
        public void tick() {
            if (this.targetPlayer == null) return;

            this.mob.getLookControl().setLookAt(
                    this.targetPlayer.getX(),
                    this.targetPlayer.getEyeY(),
                    this.targetPlayer.getZ(),
                    30.0F,
                    30.0F
            );

            double distSq = this.mob.distanceToSqr(this.targetPlayer);
            double ambushSq = this.ambushDistance * this.ambushDistance;
            boolean watched = this.mob.isPlayerLookingAtMe(this.targetPlayer, 0.78D);
            boolean isPassiveOrPlayerMimic = this.mob.getMimicType() != MIMIC_NONE;

            if (watched && distSq > ambushSq && !isPassiveOrPlayerMimic) {
                this.mob.setStalkState(STATE_MIMIC_FREEZE);
                this.mob.getNavigation().stop();
                this.mob.setDeltaMovement(Vec3.ZERO);
                return;
            }

            if (distSq <= ambushSq) {
                this.mob.setStalkState(STATE_LUNGE);
                if (this.mob.lungeCooldown <= 0) {
                    this.mob.lungeCooldown = 25;
                    this.mob.performLungeToward(this.targetPlayer);
                } else {
                    this.mob.getNavigation().moveTo(this.targetPlayer, this.speedModifier * 1.35D);
                }
                return;
            }

            this.mob.setStalkState(STATE_CREEP_IN);
            if (--this.repathCooldown <= 0) {
                this.repathCooldown = 10;
                this.mob.getNavigation().moveTo(this.targetPlayer, this.speedModifier);
            }
        }
    }
}