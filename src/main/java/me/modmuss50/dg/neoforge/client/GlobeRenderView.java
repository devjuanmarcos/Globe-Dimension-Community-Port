package me.modmuss50.dg.neoforge.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.client.renderer.LightTexture;

import java.util.Map;

/** Read-only client view of a globe snapshot, used for correct neighbour culling. */
final class GlobeRenderView implements BlockAndTintGetter {
    private final Map<BlockPos, BlockState> states;
    private final BlockAndTintGetter lightingSource;
    private final BlockPos displayPos;
    private final int packedLight;

    GlobeRenderView(Map<BlockPos, BlockState> states, BlockAndTintGetter lightingSource,
                    BlockPos displayPos, int packedLight) {
        this.states = states;
        this.lightingSource = lightingSource;
        this.displayPos = displayPos.immutable();
        this.packedLight = packedLight;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return lightingSource.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return lightingSource.getMinBuildHeight();
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return lightingSource.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return lightingSource.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return lightingSource.getBlockTint(displayPos, resolver);
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        // Snapshot coordinates (1..15) are not world coordinates. Sampling them in
        // the host world reads bedrock-level darkness and turns the miniature black.
        if (layer == LightLayer.SKY) return LightTexture.sky(packedLight);
        BlockState state = states.get(pos);
        return Math.max(LightTexture.block(packedLight), state == null ? 0 : state.getLightEmission());
    }
}
