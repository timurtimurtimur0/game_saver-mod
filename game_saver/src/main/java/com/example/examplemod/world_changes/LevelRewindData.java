package com.example.examplemod.world_changes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class LevelRewindData extends SavedData {

    private BlockPos activeLampPos = null;
    private BlockPos globalSaverPos = null;
    private long savedTime = 0;

    public long getSavedTime() {
        return this.savedTime;
    }

    private final Map<UUID, CompoundTag> playerSaves = new HashMap<>(); //The player's inventory and health
    private final List<WorldAction> actionLog = new ArrayList<>();

    private final Set<UUID> spawnedAfterDeath = new HashSet<>();
    private final Set<UUID> pendingDeletion = new HashSet<>();

    private final List<CompoundTag> deadMobsLog = new ArrayList<>();
    private final List<CompoundTag> pendingRestores = new ArrayList<>();

    public void logEntityDeath(Entity entity) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", EntityType.getKey(entity.getType()).toString());
        entity.saveWithoutId(nbt);

        //Bug fix for mob coordinates not being saved
        if (!nbt.contains("Pos")) {
            ListTag posList = new ListTag();
            posList.add(net.minecraft.nbt.DoubleTag.valueOf(entity.getX()));
            posList.add(net.minecraft.nbt.DoubleTag.valueOf(entity.getY()));
            posList.add(net.minecraft.nbt.DoubleTag.valueOf(entity.getZ()));
            nbt.put("Pos", posList);
        }

        nbt.remove("UUID");      // Giving the mob a new ID
        nbt.remove("Health");    // Give the mob a new HP
        nbt.remove("DeathTime"); // Resets the gradual recovery timer
        nbt.remove("HurtTime");  // Resets the damage animation

        deadMobsLog.add(nbt);
        this.setDirty();
    }

    public void logNewSpawn(UUID uuid) {
        spawnedAfterDeath.add(uuid);
        this.setDirty();
    }

    public void prepareDeletions() {
        pendingDeletion.addAll(spawnedAfterDeath);
        spawnedAfterDeath.clear();
        this.setDirty();
    }

    public boolean shouldDelete(UUID uuid) {
        if (pendingDeletion.contains(uuid)) {
            pendingDeletion.remove(uuid); // Removes the mob from the list, as it will now be deleted from the world
            this.setDirty();
            return true;
        }
        return false;
    }

    // Triggered when a player dies
    public void prepareMobRestoration() {
        // Adds all dead mobs to the resurrection queue
        pendingRestores.addAll(deadMobsLog);
        deadMobsLog.clear(); // Clears the logs
        this.setDirty();
    }

    // Called every 20 ticks
    public void processPendingRestores(ServerLevel level, BlockPos playerPos, double radius) {
        if (pendingRestores.isEmpty()) return;

        double radiusSq = radius * radius;
        Iterator<CompoundTag> iterator = pendingRestores.iterator();
        int spawnedThisTick = 0;

        while (iterator.hasNext() && spawnedThisTick < 3) { // No more than 3 mobs per tick to prevent lag
            CompoundTag nbt = iterator.next();
            ListTag posList = nbt.getList("Pos", 6);

            if (posList.size() >= 3) {
                double ex = posList.getDouble(0);
                double ey = posList.getDouble(1);
                double ez = posList.getDouble(2);
                BlockPos mobPos = BlockPos.containing(ex, ey, ez);

                if (playerPos.distToCenterSqr(ex, ey, ez) <= radiusSq && level.isLoaded(mobPos)) {
                    restoreEntityFromNBT(level, nbt);
                    iterator.remove();
                    spawnedThisTick++;
                }
            } else {
                iterator.remove();
            }
        }
        if (spawnedThisTick > 0) this.setDirty();
    }

    private void restoreEntityFromNBT(ServerLevel level, CompoundTag nbt) {
        if (nbt.contains("id")) {
            EntityType.byString(nbt.getString("id")).ifPresent(type -> {
                Entity entity = type.create(level);
                if (entity != null) {
                    entity.load(nbt);
                    entity.addTag("rewind_revived");
                    level.addFreshEntity(entity);
                }
            });
        }
    }

    public void saveAllPlayers(ServerLevel level, BlockPos saverPos){
        this.globalSaverPos = saverPos;
        this.playerSaves.clear(); // Clears old saves
        this.savedTime = level.getDayTime(); //Remembers the time of day
        this.spawnedAfterDeath.clear();
        this.pendingDeletion.clear();
        this.deadMobsLog.clear();
        this.pendingRestores.clear();

        for (var player : level.players()){
            CompoundTag playerData = new CompoundTag();

            // Save the inventory in tag
            ListTag inventoryTag = new ListTag();
            player.getInventory().save(inventoryTag);
            playerData.put("Inventory", inventoryTag);

            // Save player health
            playerData.putFloat("Health", player.getHealth());

            ListTag effectsTag = new ListTag();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                effectsTag.add(effect.save(new CompoundTag()));
            }
            playerData.put("Effects", effectsTag);

            // Writes the player's ID
            playerSaves.put(player.getUUID(), playerData);
        }
        this.setDirty();
    }

    public BlockPos getGlobalSaverPos(){
        return globalSaverPos;
    }

    public CompoundTag getPlayerData(UUID playerId){
        return playerSaves.get(playerId);
    }

    // A method for adding an event to the log
    public void addAction(WorldAction action){
        actionLog.add(action);
        this.setDirty();
    }

    public List<WorldAction> getLog(){
        return actionLog;
    }

    public void clearLog(){
        actionLog.clear();
        this.setDirty();
    }

    // Saving the list in NBT
    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        ListTag listTag = new ListTag();
        for (WorldAction action : actionLog){
            listTag.add(action.save());
        }
        compoundTag.put("ActionLog", listTag);

        compoundTag.putLong("SavedTime", this.savedTime);

        if(this.globalSaverPos != null){
            compoundTag.put("SaverPos", NbtUtils.writeBlockPos(this.globalSaverPos));
        }

        CompoundTag playersTag = new CompoundTag();
        for(Map.Entry<UUID, CompoundTag> entry : this.playerSaves.entrySet()){
            playersTag.put(entry.getKey().toString(), entry.getValue());
        }
        compoundTag.put("PlayerSaves", playersTag);

        ListTag deadTag = new ListTag();
        for (CompoundTag nbt : this.deadMobsLog) deadTag.add(nbt);
        compoundTag.put("DeadMobs", deadTag);

        ListTag spawnedTag = new ListTag();
        for (UUID uuid : spawnedAfterDeath) {
            spawnedTag.add(StringTag.valueOf(uuid.toString()));
        }
        compoundTag.put("SpawnedAfterDeath", spawnedTag);

        ListTag deletionsTag = new ListTag();
        for (UUID uuid : pendingDeletion) {
            deletionsTag.add(StringTag.valueOf(uuid.toString()));
        }
        compoundTag.put("PendingDeletion", deletionsTag);

        ListTag pendingTag = new ListTag();
        for (CompoundTag nbt : this.pendingRestores) pendingTag.add(nbt);
        compoundTag.put("PendingRestores", pendingTag);

        return compoundTag;
    }

    // Method for completely clearing the cache
    public void clearAllData() {
        this.globalSaverPos = null;
        this.playerSaves.clear();
        this.spawnedAfterDeath.clear();
        this.pendingDeletion.clear();
        this.activeLampPos = null;
        this.deadMobsLog.clear();
        this.pendingRestores.clear();
        this.actionLog.clear();
        this.setDirty();
    }

    // Loading from NBT when starting the world
    public static LevelRewindData load(CompoundTag compoundTag){
        LevelRewindData data = new LevelRewindData();
        ListTag listTag = compoundTag.getList("ActionLog", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++){
            data.actionLog.add(WorldAction.load(listTag.getCompound(i)));
        }

        data.savedTime = compoundTag.getLong("SavedTime");

        if(compoundTag.contains("SaverPos")){
            data.globalSaverPos = NbtUtils.readBlockPos(compoundTag.getCompound("SaverPos"));
        }

        if (compoundTag.contains("PlayerSaves")){
            CompoundTag playersTag = compoundTag.getCompound("PlayerSaves");

            // Checks all saved UUID
            for (String uuidStr : playersTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    CompoundTag playerData = playersTag.getCompound(uuidStr);
                    data.playerSaves.put(uuid, playerData);
                } catch (IllegalArgumentException e) {

                    // Ignores the error if the key happens to be an invalid UUID
                }
            }
        }

        if (compoundTag.contains("DeadMobs")) {
            ListTag deadTag = compoundTag.getList("DeadMobs", Tag.TAG_COMPOUND);
            for (int i = 0; i < deadTag.size(); i++) data.deadMobsLog.add(deadTag.getCompound(i));
        }

        if (compoundTag.contains("PendingRestores")) {
            ListTag pendingTag = compoundTag.getList("PendingRestores", Tag.TAG_COMPOUND);
            for (int i = 0; i < pendingTag.size(); i++) data.pendingRestores.add(pendingTag.getCompound(i));
        }

        if (compoundTag.contains("SpawnedAfterDeath", Tag.TAG_LIST)) {
            ListTag list = compoundTag.getList("SpawnedAfterDeath", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    data.spawnedAfterDeath.add(UUID.fromString(list.getString(i)));
                } catch (IllegalArgumentException e) {

                }
            }
        }

        if (compoundTag.contains("PendingDeletion", Tag.TAG_LIST)) {
            ListTag list = compoundTag.getList("PendingDeletion", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    data.pendingDeletion.add(UUID.fromString(list.getString(i)));
                } catch (IllegalArgumentException e) {

                }
            }
        }

        return data;
    }

    // A method for creating a folder in a world data file
    public static LevelRewindData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                LevelRewindData::load,
                LevelRewindData::new,
                "rewind_data" // File name in the data folder
        );
    }

    public BlockPos getActiveLampPos() {
        return this.activeLampPos;
    }

    public void setActiveLampPos(BlockPos pos) {
        this.activeLampPos = pos;
        this.setDirty();
    }
}

