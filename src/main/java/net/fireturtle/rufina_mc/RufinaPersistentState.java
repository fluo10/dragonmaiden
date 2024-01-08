package net.fireturtle.rufina_mc;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

public class RufinaPersistentState  extends PersistentState{
    @Nullable
    public UUID rufinaUuid = null;

    @Nullable
    public UUID ownerUuid = null;

    public String rufinaWorldRegistryKey;

    static final String RUFINA_UUID_KEY = "rufinaUuid";

    static final String OWNER_UUID_KEY = "ownerUuid";
    static final String RUFINA_WORLD_REGISTRY_KEY_KEY = "rufinaWorldKey";
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putUuid(RUFINA_UUID_KEY, rufinaUuid);
        nbt.putUuid(OWNER_UUID_KEY, ownerUuid);
        nbt.putString(RUFINA_WORLD_REGISTRY_KEY_KEY, rufinaWorldRegistryKey);
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
        if(nbt.contains(RUFINA_WORLD_REGISTRY_KEY_KEY)) {
            this.rufinaWorldRegistryKey = nbt.getString(RUFINA_WORLD_REGISTRY_KEY_KEY);
            
        }
    }

    public static RufinaPersistentState createFromNbt(NbtCompound tag) {
        RufinaPersistentState state = new RufinaPersistentState();
        state.readNbt(tag);
        return state;
    }
}
