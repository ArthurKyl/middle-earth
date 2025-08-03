package net.sevenstars.middleearth.item.items;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.FluidModificationItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sevenstars.middleearth.item.ToolItemsME;
import org.jetbrains.annotations.Nullable;

public class WoodenBucketItem extends Item implements FluidModificationItem {
    private final WoodenBucketContentType contentType;

    public enum WoodenBucketContentType {
        EMPTY,
        WATER,
        POWDERED_SNOW,
        MILK,
        FISH
    }

    public WoodenBucketItem(WoodenBucketContentType contentType, Settings settings) {
        super(settings);
        this.contentType = contentType;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        BlockHitResult hit = world.raycast(new RaycastContext(
                user.getEyePos(),
                user.getEyePos().add(user.getRotationVec(1.0F).multiply(5.0D)),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.SOURCE_ONLY,
                user
        ));

        if (hit.getType() != HitResult.Type.BLOCK) return ActionResult.PASS;

        if (contentType == WoodenBucketContentType.EMPTY) {
            return tryPickup(world, user, hand, stack, hit);
        } else {
            return tryPlace(world, user, hand, stack, hit);
        }
    }

    private ActionResult tryPickup(
            World world,
            PlayerEntity user,
            Hand hand,
            ItemStack stack,
            BlockHitResult hit) {

        if (WoodenBucketHandler.tryPickupPowderedSnow(world, user, hand, stack, hit,
                new ItemStack(ToolItemsME.WOODEN_POWDER_SNOW_BUCKET_ITEM))) {
            return ActionResult.SUCCESS;
        }

        if (WoodenBucketHandler.tryPickupFluid(world, user, hand, stack, Fluids.EMPTY, hit,
                new ItemStack(ToolItemsME.WOODEN_WATER_BUCKET_ITEM))) {
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    private ActionResult tryPlace(
            World world,
            PlayerEntity user,
            Hand hand,
            ItemStack stack,
            BlockHitResult hit) {

        boolean success = switch (contentType) {
            case WATER -> WoodenBucketHandler.tryPlaceFluid(user,
                    world,
                    Fluids.WATER,
                    hit.getBlockPos(),
                    hit);
            case POWDERED_SNOW ->
                    WoodenBucketHandler.tryPlacePowderedSnow(user, world, hit.getBlockPos(), hit);
            // MILK, FISH: not implemented yet
            default -> false;
        };

        if (success && !world.isClient) {
            ItemStack newStack = WoodenBucketHandler.getEmptiedStack(stack, user);
            user.setStackInHand(hand, newStack);
        }

        return success ? ActionResult.SUCCESS : ActionResult.FAIL;
    }

    @Override
    public boolean placeFluid(
            @Nullable LivingEntity user,
            World world,
            BlockPos pos,
            @Nullable BlockHitResult hitResult) {
        return switch (contentType) {
            case WATER ->
                    WoodenBucketHandler.tryPlaceFluid(user, world, Fluids.WATER, pos, hitResult);
            case POWDERED_SNOW ->
                    WoodenBucketHandler.tryPlacePowderedSnow(user, world, pos, hitResult);
            default -> false;
        };
    }
}
