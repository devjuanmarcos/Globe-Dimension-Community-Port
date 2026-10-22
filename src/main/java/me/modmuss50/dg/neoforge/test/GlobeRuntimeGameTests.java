package me.modmuss50.dg.neoforge.test;

import me.modmuss50.dg.neoforge.block.entity.GlobeBlockEntity;
import me.modmuss50.dg.neoforge.registry.ModBlocks;
import me.modmuss50.dg.neoforge.world.GlobeManager;
import me.modmuss50.dg.neoforge.teleport.GlobeTeleportService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("globedimension")
@PrefixGameTestTemplate(false)
public final class GlobeRuntimeGameTests {
    private GlobeRuntimeGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void globeChunkGeneratorCodecIsRegistered(GameTestHelper helper) {
        ResourceLocation codecId = ResourceLocation.fromNamespaceAndPath("globedimension", "globe");
        if (!BuiltInRegistries.CHUNK_GENERATOR.containsKey(codecId)) {
            helper.fail("The globe chunk generator codec was not registered");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void globeBlockEntityKeepsItsMetadata(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.GLOBE.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(pos) instanceof GlobeBlockEntity globe)) {
            helper.fail("Placing the globe block did not create its BlockEntity");
            return;
        }

        globe.setGlobeId(7);
        globe.setBaseBlock(Blocks.COPPER_BLOCK);
        if (globe.getGlobeId() != 7 || globe.getBaseBlock() != Blocks.COPPER_BLOCK) {
            helper.fail("Globe BlockEntity metadata did not retain its assigned values");
            return;
        }
        if (globe.getUpdatePacket() == null) {
            helper.fail("Globe BlockEntity did not produce a client update packet");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void globeIdsAllocateWithinSupportedBounds(GameTestHelper helper) {
        GlobeManager manager = GlobeManager.get(helper.getLevel());
        int id = manager.allocateId();
        if (!manager.isAllocated(id) || id < 0 || id > GlobeManager.MAX_GLOBE_ID) {
            helper.fail("Globe manager allocated an invalid ID: " + id);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void newGlobeIsAnEmptyConstructionRoom(GameTestHelper helper) {
        ServerLevel source = helper.getLevel();
        // GameTestServer creates only vanilla dimensions. Exercise the exact room
        // preparation method on a real ServerLevel, far above its flat test terrain.
        ServerLevel interior = source;
        int id = GlobeManager.get(source).allocateId();
        BlockPos returnPos = helper.absolutePos(new BlockPos(1, 2, 1));
        // The surrounding exterior must never become the player's interior.
        source.setBlockAndUpdate(returnPos.east(), Blocks.GOLD_BLOCK.defaultBlockState());
        GlobeTeleportService.prepareInterior(interior, id, source.dimension(), returnPos, Blocks.OAK_PLANKS);
        BlockPos origin = GlobeTeleportService.globeOrigin(id);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(1, 1, 1), origin.offset(15, 15, 15))) {
            if (!interior.getBlockState(pos).isAir()) {
                helper.fail("A new globe imported a block instead of starting empty at " + pos);
                return;
            }
        }
        if (!(interior.getBlockEntity(origin.offset(8, 0, 8)) instanceof GlobeBlockEntity)) {
            helper.fail("The exit must be in the floor, outside the buildable volume"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void reenteringPreservesConstructionAndChestContents(GameTestHelper helper) {
        ServerLevel source = helper.getLevel();
        ServerLevel interior = source;
        int id = GlobeManager.get(source).allocateId();
        BlockPos returnPos = helper.absolutePos(new BlockPos(1, 2, 1));
        GlobeTeleportService.prepareInterior(interior, id, source.dimension(), returnPos, Blocks.OAK_PLANKS);
        BlockPos origin = GlobeTeleportService.globeOrigin(id);
        BlockPos builtPos = origin.offset(3, 1, 3);
        BlockPos chestPos = origin.offset(4, 1, 3);
        interior.setBlockAndUpdate(builtPos, Blocks.DIAMOND_BLOCK.defaultBlockState());
        interior.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        if (!(interior.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) {
            helper.fail("Chest fixture did not create its inventory"); return;
        }
        chest.setItem(0, new ItemStack(Items.EMERALD, 7));
        // Moving and placing the same globe updates its return point, not its build.
        BlockPos movedReturn = returnPos.offset(3, 0, 0);
        GlobeTeleportService.prepareInterior(interior, id, source.dimension(), movedReturn, Blocks.BIRCH_PLANKS);
        if (!interior.getBlockState(builtPos).is(Blocks.DIAMOND_BLOCK)) {
            helper.fail("Re-entry overwrote the player's construction"); return;
        }
        if (!(interior.getBlockEntity(chestPos) instanceof ChestBlockEntity savedChest)
                || !savedChest.getItem(0).is(Items.EMERALD) || savedChest.getItem(0).getCount() != 7) {
            helper.fail("Re-entry deleted the player's chest or changed its contents"); return;
        }
        GlobeBlockEntity exit = (GlobeBlockEntity) interior.getBlockEntity(origin.offset(8, 0, 8));
        if (exit == null || !movedReturn.equals(exit.getReturnPos()) || exit.getBaseBlock() != Blocks.BIRCH_PLANKS) {
            helper.fail("Moving the globe did not update its return location/base"); return;
        }
        helper.succeed();
    }
}
