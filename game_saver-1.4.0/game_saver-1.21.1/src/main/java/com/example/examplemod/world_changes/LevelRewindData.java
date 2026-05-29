package com.example.examplemod.world_changes;

import com.example.examplemod.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class LevelRewindData extends SavedData {

    private BlockPos activeLampPos = null;
    private BlockPos globalSaverPos = null;
    private long savedTime = 0;

    private boolean limitWarningSent = false;
    private boolean limitWarningChunk = false;

    public long getSavedTime() {
        return this.savedTime;
    }

    private final Map<UUID, CompoundTag> playerSaves = new HashMap<>(); //The player's inventory and health
    private final List<WorldAction> actionLog = new ArrayList<>();

    private final Set<UUID> spawnedAfterDeath = new HashSet<>();
    private final Set<UUID> pendingDeletion = new HashSet<>();

    private final List<CompoundTag> deadMobsLog = new ArrayList<>();

    private final Set<ChunkPos> RemovesMobs = new HashSet<>();

    public LevelRewindData(){}

    public void logEntityDeath(Entity entity) {

        ChunkPos chunkPos = new ChunkPos(entity.blockPosition());
        if (!RemovesMobs.contains(chunkPos)){
            if (this.RemovesMobs.size() >= Config.MAX_CHUNK_SIZE.get()){
                if (!this.limitWarningChunk) {

                    if(entity.level() instanceof ServerLevel serverLevel){
                        serverLevel.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal
                                ("Please note: after saving, you played for too long and the mob's log is full. " +
                                        "I recommend creating a new checkpoint.").withStyle(ChatFormatting.
                                RED), false);

                    }
                    this.limitWarningChunk = true;
                }
                return;
            }
        }
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
        nbt.putInt("ChunkX", chunkPos.x); // Add's the chunk coordinates to the mob's NBT data
        nbt.putInt("ChunkZ", chunkPos.z); // Add's the chunk coordinates to the mob's NBT data

        deadMobsLog.add(nbt);
        RemovesMobs.add(chunkPos);
        this.setDirty();
    }

    //Instant respawn and gradual deletion of mobs
    public void InstantRollback(ServerLevel level){
        if (this.globalSaverPos == null) return;

        List<CompoundTag> mobsToRestore = new ArrayList<>(this.deadMobsLog);

        for (CompoundTag entityNbt : mobsToRestore){
            if(entityNbt.contains("Pos", Tag.TAG_LIST)){
                ListTag posList = entityNbt.getList("Pos", Tag.TAG_DOUBLE);
                if(posList.size() == 3){
                    double x = posList.getDouble(0);
                    double z = posList.getDouble(2);

                    //In this case, I use a bit shift instead of a division by 16, since a bit shift is faster
                    int chunkX = ((int) Math.floor(x)) >> 4;
                    int chunkZ = ((int) Math.floor(z)) >> 4;

                    level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);

                    level.setChunkForced(chunkX, chunkZ, true);

                    Optional<Entity> optionalEntity = EntityType.create(entityNbt, level);
                    if (optionalEntity.isPresent()){
                        Entity revivedEntity = optionalEntity.get();
                        revivedEntity.addTag("rewind_revived");

                        level.addFreshEntity(revivedEntity);
                    }
                    level.setChunkForced(chunkX, chunkZ, false);

                }
            }
        }
        List<Entity> entitiesToRemove = new ArrayList<>();

        for (Entity entity : level.getAllEntities()) {
            if (this.spawnedAfterDeath.contains(entity.getUUID())) {
                entitiesToRemove.add(entity);
            }
            else if(entity instanceof net.minecraft.world.entity.item.ItemEntity){
                entitiesToRemove.add(entity);
            }
        }

        for(Entity entity : entitiesToRemove){
            entity.discard();
        }

        //to gradually remove mobs
        this.pendingDeletion.addAll(this.spawnedAfterDeath);
        this.spawnedAfterDeath.clear();

        // Completely clears the logs of mobs that have respawned
        this.deadMobsLog.clear();
        this.RemovesMobs.clear();
        this.limitWarningChunk = false;
        this.setDirty();

    }

    public void logNewSpawn(UUID uuid) {
        spawnedAfterDeath.add(uuid);
        this.setDirty();
    }

    public int SetChunks(){
        return RemovesMobs.size();
    }

    public void prepareDeletions() {
        pendingDeletion.addAll(spawnedAfterDeath);
        spawnedAfterDeath.clear();
        this.setDirty();
    }

    public int getDeadMobsCount() {
        return this.deadMobsLog.size();
    }

    public int getChunkSize(){
        return  this.RemovesMobs.size();
    }


    public int getSpawnedAfterDeathCount() {
        return this.spawnedAfterDeath.size() + this.pendingDeletion.size();
    }

    public boolean shouldDelete(UUID uuid) {
        if (pendingDeletion.contains(uuid)) {
            pendingDeletion.remove(uuid); // Removes the mob from the list, as it will now be deleted from the world
            this.setDirty();
            return true;
        }
        return false;
    }

    public void saveAllPlayers(ServerLevel level, BlockPos saverPos){
        this.globalSaverPos = saverPos;
        this.playerSaves.clear(); // Clears old saves
        this.savedTime = level.getDayTime(); //Remembers the time of day
        this.spawnedAfterDeath.clear();
        this.pendingDeletion.clear();
        this.deadMobsLog.clear();
        this.RemovesMobs.clear();
        this.limitWarningChunk = false;

        HolderLookup.Provider registryAccess = level.getServer().registryAccess();

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
                MobEffectInstance.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), effect)
                        .result()
                        .ifPresent(effectsTag::add);
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
    public void addAction(WorldAction action, ServerLevel level){
        if (this.actionLog.size() >= Config.MAX_LOG_SIZE.get()){
            if (!this.limitWarningSent) {
                level.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal
                        ("Please note: after saving, you played for too long and the block's log is full. " +
                                "I recommend creating a new checkpoint.").withStyle(ChatFormatting.
                        RED), false);
                this.limitWarningSent = true;
            }
            return;
        }
        this.actionLog.add(action);
        this.setDirty();
    }


    public List<WorldAction> getLog(){
        return actionLog;
    }

    public void clearLog(){
        actionLog.clear();
        this.limitWarningSent = false;
        this.setDirty();
    }

    // Saving the list in NBT
    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        for (WorldAction action : this.actionLog){
            listTag.add(action.save(registries));
        }
        compoundTag.put("ActionLog", listTag);

        compoundTag.putLong("SavedTime", this.savedTime);

        if (this.globalSaverPos != null) {
            compoundTag.putInt("SaverX", this.globalSaverPos.getX());
            compoundTag.putInt("SaverY", this.globalSaverPos.getY());
            compoundTag.putInt("SaverZ", this.globalSaverPos.getZ());
        }

        // Saves the position of the active lamp
        if (this.activeLampPos != null) {
            compoundTag.putInt("LampX", this.activeLampPos.getX());
            compoundTag.putInt("LampY", this.activeLampPos.getY());
            compoundTag.putInt("LampZ", this.activeLampPos.getZ());
        }

        CompoundTag playersTag = new CompoundTag();
        for(Map.Entry<UUID, CompoundTag> entry : this.playerSaves.entrySet()){
            playersTag.put(entry.getKey().toString(), entry.getValue());
        }
        compoundTag.put("PlayerSaves", playersTag);

        ListTag deadTag = new ListTag();
        for (CompoundTag nbt : this.deadMobsLog) deadTag.add(nbt.copy());
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
        this.RemovesMobs.clear();
        this.limitWarningChunk = false;
        this.limitWarningSent = false;
        this.actionLog.clear();
        this.setDirty();
    }

    // Loading from NBT when starting the world
    public static LevelRewindData load(CompoundTag compoundTag, HolderLookup.Provider registries){
        LevelRewindData data = new LevelRewindData();

        // Reads the position of the active lamp in NBT
        if (compoundTag.contains("LampX")) {
            data.activeLampPos = new BlockPos(compoundTag.getInt("LampX"), compoundTag.getInt("LampY"), compoundTag.getInt("LampZ"));
        }

        if (compoundTag.contains("SaverX")) {
            data.globalSaverPos = new BlockPos(compoundTag.getInt("SaverX"), compoundTag.getInt("SaverY"), compoundTag.getInt("SaverZ"));
        }
        if (compoundTag.contains("ActionLog", Tag.TAG_LIST)) {
            ListTag listTag = compoundTag.getList("ActionLog", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                data.actionLog.add(WorldAction.load(listTag.getCompound(i), registries));
            }
        }

        data.savedTime = compoundTag.getLong("SavedTime");



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
            for (int i = 0; i < deadTag.size(); i++) {
                CompoundTag mobTag = deadTag.getCompound(i);
                data.deadMobsLog.add(mobTag);

                // Restores the list of chanks upon entering the world
                if (mobTag.contains("ChunkX") && mobTag.contains("ChunkZ")) {
                    data.RemovesMobs.add(new ChunkPos(mobTag.getInt("ChunkX"), mobTag.getInt("ChunkZ")));
                }
            }
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
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            overworld = level;
        }

        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        LevelRewindData::new,
                        LevelRewindData::load,
                        DataFixTypes.SAVED_DATA_MAP_DATA
                ),
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

