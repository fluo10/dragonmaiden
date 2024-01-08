package net.fireturtle.rufina_mc;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class RufinaPersistentState  extends PersistentState{
    @Nullable
    public UUID rufinaUuid = null;

    @Nullable
    public UUID ownerUuid = null;

    public String rufinaWorldRegistryKey;

    static final String RUFINA_UUID_KEY = "rufinaUuid";

    static final String OWNER_UUID_KEY = "ownerUuid";
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        
        if (rufinaUuid != null) {
            nbt.putUuid(RUFINA_UUID_KEY, rufinaUuid);
        }
        if (ownerUuid != null) {
            nbt.putUuid(OWNER_UUID_KEY, ownerUuid);
        }
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        if(nbt.containsUuid(RUFINA_UUID_KEY)) {
            this.rufinaUuid = nbt.getUuid(RUFINA_UUID_KEY);
        } else {
            this.rufinaUuid = null;
        }
        if(nbt.containsUuid(OWNER_UUID_KEY)) {
            this.ownerUuid = nbt.getUuid(OWNER_UUID_KEY);
        } else {
            this.ownerUuid = null;
        }
    }

    public static RufinaPersistentState createFromNbt(NbtCompound tag) {
        RufinaPersistentState state = new RufinaPersistentState();
        state.readNbt(tag);
        return state;
    }
    private static Type<RufinaPersistentState> type = new Type<>(
            RufinaPersistentState::new, // If there's no 'StateSaverAndLoader' yet create one
            RufinaPersistentState::createFromNbt, // If there is a 'StateSaverAndLoader' NBT, parse it with 'createFromNbt'
            null // Supposed to be an 'DataFixTypes' enum, but we can just pass null
    );
 
    public static RufinaPersistentState getServerState(MinecraftServer server) {
        // (Note: arbitrary choice to use 'World.OVERWORLD' instead of 'World.END' or 'World.NETHER'.  Any work)
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
 
        // The first time the following 'getOrCreate' function is called, it creates a brand new 'StateSaverAndLoader' and
        // stores it inside the 'PersistentStateManager'. The subsequent calls to 'getOrCreate' pass in the saved
        // 'StateSaverAndLoader' NBT on disk to our function 'StateSaverAndLoader::createFromNbt'.
        RufinaPersistentState state = persistentStateManager.getOrCreate(type, Rufina.MOD_ID);
 
        // If state is not marked dirty, when Minecraft closes, 'writeNbt' won't be called and therefore nothing will be saved.
        // Technically it's 'cleaner' if you only mark state as dirty when there was actually a change, but the vast majority
        // of mod writers are just going to be confused when their data isn't being saved, and so it's best just to 'markDirty' for them.
        // Besides, it's literally just setting a bool to true, and the only time there's a 'cost' is when the file is written to disk when
        // there were no actual change to any of the mods state (INCREDIBLY RARE).
        state.markDirty();
 
        return state;
    }
}
