package net.sevenstars.middleearth.item.items;

import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.sevenstars.middleearth.item.ToolItemsME;
import net.sevenstars.middleearth.utils.sounds.SoundUtils;
import org.jetbrains.annotations.Nullable;

public class WoodenBucketHandler {

    public static boolean tryPickupFluid(
            World world,
            PlayerEntity user,
            Hand hand,
            ItemStack stack,
            Fluid fluid,
            BlockHitResult hit,
            ItemStack resultStack
    ) {
        if (fluid != Fluids.EMPTY) return false;

        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (!(block instanceof FluidDrainable drainable)) return false;

        if (state.getFluidState().getFluid() == Fluids.LAVA) return false;

        ItemStack drained = drainable.tryDrainFluid(user, world, pos, state);
        if (drained.isEmpty() || drained.getItem() != Items.WATER_BUCKET) return false;

        if (!world.isClient) {
            drainable.getBucketFillSound().ifPresent(sound ->
                    world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F)
            );
            world.emitGameEvent(user, GameEvent.FLUID_PICKUP, pos);
            replaceOrGive(user, hand, resultStack);
        }

        return true;
    }

    public static boolean tryPlaceFluid(
            @Nullable LivingEntity user,
            World world,
            Fluid fluid,
            BlockPos pos,
            @Nullable BlockHitResult hitResult
    ) {
        if (!(fluid instanceof FlowableFluid flowableFluid)) return false;

        BlockState blockState = world.getBlockState(pos);
        boolean canPlaceDirectly = blockState.canBucketPlace(fluid);

        if (!blockState.isAir() && !canPlaceDirectly) {
            if (blockState.getBlock() instanceof FluidFillable fluidFillable &&
                    fluidFillable.canFillWithFluid(user, world, pos, blockState, fluid)) {

                fluidFillable.tryFillWithFluid(world,
                        pos,
                        blockState,
                        flowableFluid.getStill(false));
                playEmptyingSound(user, world, pos);
                return true;
            }

            if (hitResult == null) return false;
            return tryPlaceFluid(user,
                    world,
                    fluid,
                    hitResult.getBlockPos().offset(hitResult.getSide()),
                    null);
        }

        if (!world.isClient && canPlaceDirectly && !blockState.getFluidState().isStill()) {
            world.breakBlock(pos, true);
        }

        if (!world.setBlockState(pos, fluid.getDefaultState().getBlockState(), 11) &&
                !blockState.getFluidState().isStill()) {
            return false;
        }

        playEmptyingSound(user, world, pos);
        return true;
    }

    public static boolean tryPickupPowderedSnow(
            World world,
            PlayerEntity user,
            Hand hand,
            ItemStack stack,
            BlockHitResult hit,
            ItemStack resultStack
    ) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (!state.isOf(Blocks.POWDER_SNOW)) return false;

        if (!world.isClient) {
            world.playSound(null,
                    pos,
                    SoundEvents.ITEM_BUCKET_FILL_POWDER_SNOW,
                    SoundCategory.BLOCKS,
                    1.0F,
                    1.0F);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            world.emitGameEvent(user, GameEvent.FLUID_PICKUP, pos);
            replaceOrGive(user, hand, resultStack);
        }

        return true;
    }

    public static boolean tryPlacePowderedSnow(
            @Nullable LivingEntity user,
            World world,
            BlockPos pos,
            @Nullable BlockHitResult hitResult
    ) {
        if (!world.isInBuildLimit(pos)) return false;

        if (!world.isAir(pos)) {
            if (hitResult == null) return false;
            pos = hitResult.getBlockPos().offset(hitResult.getSide());

            if (!world.isInBuildLimit(pos) || !world.isAir(pos)) return false;
        }

        if (!world.isClient) {
            world.setBlockState(pos, Blocks.POWDER_SNOW.getDefaultState(), 3);
            world.playSound(null,
                    pos,
                    SoundEvents.ITEM_BUCKET_EMPTY_POWDER_SNOW,
                    SoundCategory.BLOCKS,
                    1.0F,
                    1.0F);
            world.emitGameEvent(user, GameEvent.FLUID_PLACE, pos);
        }

        return true;
    }

    private static void playEmptyingSound(@Nullable LivingEntity user, World world, BlockPos pos) {
        world.playSound(user, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.emitGameEvent(user, GameEvent.FLUID_PLACE, pos);
    }

    public static ItemStack getEmptiedStack(ItemStack stack, PlayerEntity player) {
        return player.isInCreativeMode() ? stack : new ItemStack(ToolItemsME.WOODEN_BUCKET_ITEM);
    }

    public static void replaceOrGive(PlayerEntity player, Hand hand, ItemStack result) {
        result.setCount(1);

        if (player.isInCreativeMode()) {
            if (!player.getInventory().contains(result)) {
                player.getInventory().insertStack(result.copy());
            }
        } else {
            player.setStackInHand(hand, result.copy());
        }
    }
}
