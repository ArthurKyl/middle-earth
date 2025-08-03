package net.sevenstars.middleearth.item.items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.FluidModificationItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.sevenstars.middleearth.item.ToolItemsME;
import org.jetbrains.annotations.Nullable;

public class WoodenBucketItem extends Item implements FluidModificationItem {

    public enum WoodenBucketContentType {
        EMPTY,
        WATER,
        POWDERED_SNOW,
        MILK,
        FISH
    }

    private final WoodenBucketContentType contentType;

    public WoodenBucketItem(WoodenBucketContentType contentType, Settings settings) {
        super(settings);
        this.contentType = contentType;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Drink milk
        if (contentType == WoodenBucketContentType.MILK) {
            user.setCurrentHand(hand);
            return ActionResult.SUCCESS;
        }

        // Raycast for block hit (for fluid/powdered snow interaction)
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

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (this.contentType != WoodenBucketContentType.EMPTY) return ActionResult.PASS;

        //@TODO: Move to Handler
        if (entity instanceof CowEntity) {
            World world = user.getWorld();

            if (!world.isClient) {
                world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_COW_MILK, user.getSoundCategory(), 1.0F, 1.0F);
                world.emitGameEvent(user, GameEvent.FLUID_PICKUP, entity.getBlockPos());

                ItemStack milkBucket = new ItemStack(ToolItemsME.WOODEN_MILK_BUCKET_ITEM);

                if (!user.getAbilities().creativeMode) {
                    user.setStackInHand(hand, milkBucket);
                } else {
                    // In creative, don't consume the wooden bucket — just give the milk bucket separately
                    if (!user.getInventory().contains(milkBucket)) {
                        user.getInventory().insertStack(milkBucket.copy());
                    }
                }
            }

            return ActionResult.SUCCESS;
        }

        return super.useOnEntity(stack, user, entity, hand);
    }

    private ActionResult tryPickup(World world, PlayerEntity user, Hand hand, ItemStack stack, BlockHitResult hit) {
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

    private ActionResult tryPlace(World world, PlayerEntity user, Hand hand, ItemStack stack, BlockHitResult hit) {
        //@TODO: Add cauldron fill
        boolean success = switch (contentType) {
            case WATER -> WoodenBucketHandler.tryPlaceFluid(user, world, Fluids.WATER, hit.getBlockPos(), hit);
            case POWDERED_SNOW -> WoodenBucketHandler.tryPlacePowderedSnow(user, world, hit.getBlockPos(), hit);
            default -> false;
        };

        if (success && !world.isClient) {
            ItemStack newStack = WoodenBucketHandler.getEmptiedStack(stack, user);
            user.setStackInHand(hand, newStack);
        }

        return success ? ActionResult.SUCCESS : ActionResult.FAIL;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        return super.finishUsing(stack, world, user);
    }

    @Override
    public boolean placeFluid(@Nullable LivingEntity user, World world, BlockPos pos, @Nullable BlockHitResult hitResult) {
        return switch (contentType) {
            case WATER -> WoodenBucketHandler.tryPlaceFluid(user, world, Fluids.WATER, pos, hitResult);
            case POWDERED_SNOW -> WoodenBucketHandler.tryPlacePowderedSnow(user, world, pos, hitResult);
            default -> false;
        };
    }
}
