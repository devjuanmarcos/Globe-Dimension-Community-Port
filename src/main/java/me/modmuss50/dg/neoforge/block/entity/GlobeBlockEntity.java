package me.modmuss50.dg.neoforge.block.entity;

import me.modmuss50.dg.neoforge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GlobeBlockEntity extends BlockEntity {
    private static final String GLOBE_ID = "globe_id";
    private static final String BASE_BLOCK = "base_block";
    private static final String RETURN_X = "return_x";
    private static final String RETURN_Y = "return_y";
    private static final String RETURN_Z = "return_z";
    private static final String RETURN_DIMENSION = "return_dim";

    private int globeId = -1;
    private Block baseBlock = Blocks.OAK_PLANKS;
    private BlockPos returnPos;
    private ResourceKey<Level> returnDimension;

    public GlobeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GLOBE.get(), pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        globeId = tag.contains(GLOBE_ID) ? tag.getInt(GLOBE_ID) : -1;
        ResourceLocation baseId = ResourceLocation.tryParse(tag.getString(BASE_BLOCK));
        baseBlock = baseId == null ? Blocks.OAK_PLANKS : BuiltInRegistries.BLOCK.getOptional(baseId).orElse(Blocks.OAK_PLANKS);

        if (tag.contains(RETURN_X) && tag.contains(RETURN_Y) && tag.contains(RETURN_Z)) {
            returnPos = new BlockPos(tag.getInt(RETURN_X), tag.getInt(RETURN_Y), tag.getInt(RETURN_Z));
            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(RETURN_DIMENSION));
            returnDimension = dimensionId == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, dimensionId);
        } else {
            returnPos = null;
            returnDimension = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(GLOBE_ID, globeId);
        tag.putString(BASE_BLOCK, BuiltInRegistries.BLOCK.getKey(baseBlock).toString());
        if (returnPos != null && returnDimension != null) {
            tag.putInt(RETURN_X, returnPos.getX());
            tag.putInt(RETURN_Y, returnPos.getY());
            tag.putInt(RETURN_Z, returnPos.getZ());
            tag.putString(RETURN_DIMENSION, returnDimension.location().toString());
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithFullMetadata(registries);
    }

    public void setGlobeId(int globeId) {
        this.globeId = globeId;
        setChangedAndSync();
    }

    public int getGlobeId() {
        return globeId;
    }

    public void setBaseBlock(Block baseBlock) {
        this.baseBlock = baseBlock == null ? Blocks.OAK_PLANKS : baseBlock;
        setChangedAndSync();
    }

    public Block getBaseBlock() {
        return baseBlock;
    }

    public void setReturnLocation(BlockPos pos, ResourceKey<Level> dimension) {
        returnPos = pos;
        returnDimension = dimension;
        setChangedAndSync();
    }

    public BlockPos getReturnPos() {
        return returnPos;
    }

    public ResourceKey<Level> getReturnDimension() {
        return returnDimension;
    }

    public boolean isInsideGlobe() {
        return level != null && level.dimension().location().equals(ResourceLocation.fromNamespaceAndPath("globedimension", "globe"));
    }

    public void use(ServerPlayer player) {
        if (level == null || level.isClientSide) return;
        me.modmuss50.dg.neoforge.teleport.GlobeTeleportService.use(this, player);
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
