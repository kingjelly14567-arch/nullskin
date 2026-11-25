package com.feni.nullskin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TunnelBreacherEntity extends Monster {

    private int breachCooldown = 0;

    public TunnelBreacherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 65.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        if (breachCooldown > 0) {
            breachCooldown--;
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        // Breaches soft blocks/torches when closing in, especially underground
        if (this.distanceTo(target) < 14.0D && (this.getY() < 52 || this.horizontalCollision)) {
            if (tryBreachForwardBlock()) {
                breachCooldown = 14;
            }
        }
    }

    private boolean tryBreachForwardBlock() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        BlockPos base = this.blockPosition().relative(this.getDirection());
        BlockPos[] candidates = { base, base.above() };

        for (BlockPos pos : candidates) {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.isAir() || state.liquid()) {
                continue;
            }
            if (isSoftBreachable(state)) {
                serverLevel.playSound(null, pos, state.getSoundType(serverLevel, pos, this).getBreakSound(), SoundSource.HOSTILE, 1.5F, 0.8F);
                serverLevel.playSound(null, pos, SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, 1.0F, 0.7F);
                serverLevel.destroyBlock(pos, false);
                return true;
            }
        }
        return false;
    }

    private boolean isSoftBreachable(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SAND)
                || state.is(Blocks.OAK_PLANKS)
                || state.is(Blocks.SPRUCE_PLANKS)
                || state.is(Blocks.BIRCH_PLANKS)
                || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH);
    }
}