package com.feni.nullskin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.Random;

public class NullSkinEscalationData extends SavedData {

    private static final String DATA_NAME = "nullskin_escalation";

    private int escalationPoints = 0;
    private int stage = 0;
    private int rngOffsetTicks = 0;
    private boolean hasManifestedOnce = false;
    private boolean nullSystemCorrupted = false;
    private boolean nullSystemOverwritten = false;

    public NullSkinEscalationData() {
        Random random = new Random();
        this.rngOffsetTicks = random.nextInt(18_001);
    }

    public static NullSkinEscalationData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(NullSkinEscalationData::load, NullSkinEscalationData::new, DATA_NAME);
    }

    public static NullSkinEscalationData load(CompoundTag tag) {
        NullSkinEscalationData data = new NullSkinEscalationData();
        data.escalationPoints = tag.getInt("EscalationPoints");
        data.stage = clampStage(tag.getInt("Stage"));
        data.rngOffsetTicks = tag.getInt("RngOffsetTicks");
        data.hasManifestedOnce = tag.getBoolean("HasManifestedOnce");
        data.nullSystemCorrupted = tag.getBoolean("NullSystemCorrupted");
        data.nullSystemOverwritten = tag.getBoolean("NullSystemOverwritten");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("EscalationPoints", this.escalationPoints);
        tag.putInt("Stage", this.stage);
        tag.putInt("RngOffsetTicks", this.rngOffsetTicks);
        tag.putBoolean("HasManifestedOnce", this.hasManifestedOnce);
        tag.putBoolean("NullSystemCorrupted", this.nullSystemCorrupted);
        tag.putBoolean("NullSystemOverwritten", this.nullSystemOverwritten);
        return tag;
    }

    public int getEscalationPoints() {
        return this.escalationPoints;
    }

    public int getStage() {
        return this.stage;
    }

    public int getRngOffsetTicks() {
        return this.rngOffsetTicks;
    }

    public boolean hasManifestedOnce() {
        return this.hasManifestedOnce;
    }

    public void setHasManifestedOnce(boolean manifested) {
        if (this.hasManifestedOnce != manifested) {
            this.hasManifestedOnce = manifested;
            setDirty();
        }
    }

    public boolean isNullSystemCorrupted() {
        return this.nullSystemCorrupted;
    }

    public boolean isNullSystemOverwritten() {
        return this.nullSystemOverwritten;
    }

    public void addEscalationPoints(int delta) {
        if (delta == 0) {
            return;
        }
        this.escalationPoints = Math.max(0, Math.min(100, this.escalationPoints + delta));
        recalculateStageFromPoints();
        setDirty();
    }

    public void setStage(int newStage) {
        int clamped = clampStage(newStage);
        if (this.stage != clamped) {
            this.stage = clamped;
            syncSystemStatesWithStage();
            setDirty();
        }
    }

    private void recalculateStageFromPoints() {
        int computed;
        if (this.escalationPoints < 16) {
            computed = 0;
        } else if (this.escalationPoints < 36) {
            computed = 1;
        } else if (this.escalationPoints < 61) {
            computed = 2;
        } else if (this.escalationPoints < 86) {
            computed = 3;
        } else {
            computed = 4;
        }

        if (computed != this.stage) {
            this.stage = computed;
            syncSystemStatesWithStage();
        }
    }

    private void syncSystemStatesWithStage() {
        this.nullSystemCorrupted = this.stage >= 2 && this.stage < 4;
        this.nullSystemOverwritten = this.stage >= 4;
    }

    private static int clampStage(int value) {
        return Math.max(0, Math.min(4, value));
    }
}