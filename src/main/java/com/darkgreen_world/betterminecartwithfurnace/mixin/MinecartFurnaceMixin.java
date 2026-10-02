package com.darkgreen_world.betterminecartwithfurnace.mixin;

import com.darkgreen_world.betterminecartwithfurnace.BetterMinecartWithFurnace;
import com.darkgreen_world.betterminecartwithfurnace.ExtinguishableMinecartWithFurnace;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(MinecartFurnace.class)
public abstract class MinecartFurnaceMixin extends AbstractMinecart implements ExtinguishableMinecartWithFurnace {
	/** Vanilla hoppers move one item every 8 ticks. */
	@Unique
	private static final int HOPPER_TRANSFER_INTERVAL = 8;

	@Shadow
	private int fuel;

	@Shadow
	public Vec3 push;

	@Shadow
	protected abstract void setHasFuel(boolean fuel);

	@Shadow
	public abstract boolean addFuel(Vec3 interactingPos, ItemStack itemStack);

	/** On an inactive powered rail at the start of this tick. */
	@Unique
	private boolean betterMinecartWithFurnace$onBrakeRail;

	@Unique
	private boolean betterMinecartWithFurnace$refuellingFromHopper;

	/** Speed the engine last delivered, and the tick it did so. */
	@Unique
	private double betterMinecartWithFurnace$lastSpeed;

	@Unique
	private int betterMinecartWithFurnace$lastSpeedTick;

	protected MinecartFurnaceMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean betterMinecartWithFurnace$isExtinguished() {
		return this.entityTags().contains(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
	}

	@Override
	public boolean betterMinecartWithFurnace$isBurning() {
		return this.fuel > 0 && !this.betterMinecartWithFurnace$isExtinguished();
	}

	@Override
	public boolean betterMinecartWithFurnace$extinguish() {
		if (!this.betterMinecartWithFurnace$isBurning()) {
			return false;
		}

		this.addTag(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
		this.setHasFuel(false);
		this.playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + this.random.nextFloat() * 0.4F);

		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.8, this.getZ(), 8, 0.2, 0.1, 0.2, 0.02);
		}

		return true;
	}

	@Override
	public void betterMinecartWithFurnace$reignite(Vec3 igniterPos) {
		this.removeTag(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
		this.setHasFuel(this.fuel > 0);

		// An empty minecart refuelled by a hopper has no push: head away from the igniter.
		if (this.push.lengthSqr() <= 1.0E-7) {
			this.push = this.position().subtract(igniterPos).horizontal();
		}
	}

	@Inject(method = "interact", at = @At("HEAD"))
	private void betterMinecartWithFurnace$useBucketOrFlintAndSteel(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		if (this.level().isClientSide()) {
			return;
		}

		// Neither item is fuel, so vanilla interact does nothing afterwards.
		ItemStack itemStack = player.getItemInHand(hand);

		if (itemStack.is(Items.WATER_BUCKET)) {
			this.betterMinecartWithFurnace$extinguish();
		} else if (itemStack.is(Items.FLINT_AND_STEEL) && this.betterMinecartWithFurnace$isExtinguished()) {
			this.betterMinecartWithFurnace$reignite(player.position());
			this.playSound(SoundEvents.FLINTANDSTEEL_USE, 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
			itemStack.hurtAndBreak(1, player, hand);
		}
	}

	/** Fuel added by anything but a hopper reignites. */
	@Inject(method = "addFuel", at = @At("RETURN"))
	private void betterMinecartWithFurnace$reigniteOnRefuel(Vec3 interactingPos, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue() && !this.betterMinecartWithFurnace$refuellingFromHopper && this.betterMinecartWithFurnace$isExtinguished()) {
			this.betterMinecartWithFurnace$reignite(interactingPos);
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void betterMinecartWithFurnace$beforeTick(CallbackInfo ci) {
		if (this.level().isClientSide()) {
			return;
		}

		BlockState rail = this.level().getBlockState(this.getCurrentBlockPosOrRailBelow());
		this.betterMinecartWithFurnace$onBrakeRail = rail.is(Blocks.POWERED_RAIL) && !rail.getValue(PoweredRailBlock.POWERED);

		// Cancels the --fuel at the end of the vanilla tick.
		if (this.fuel > 0 && this.betterMinecartWithFurnace$isExtinguished()) {
			this.fuel++;
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void betterMinecartWithFurnace$afterTick(CallbackInfo ci) {
		if (this.level().isClientSide()) {
			return;
		}

		if (this.fuel <= 0) {
			// Extinguished without fuel is just an empty minecart.
			this.removeTag(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
		}

		if (!this.betterMinecartWithFurnace$isBurning() && this.tickCount % HOPPER_TRANSFER_INTERVAL == 0) {
			this.betterMinecartWithFurnace$refuelFromHopperAbove();
		}
	}

	/** Shows the furnace unlit while extinguished. */
	@ModifyVariable(method = "setHasFuel", at = @At("HEAD"), argsOnly = true)
	private boolean betterMinecartWithFurnace$hideFlameWhileExtinguished(boolean fuel) {
		return fuel && !this.betterMinecartWithFurnace$isExtinguished();
	}

	/** Takes one fuel item from a hopper above, without lighting the minecart or changing its direction. */
	@Unique
	private void betterMinecartWithFurnace$refuelFromHopperAbove() {
		BlockPos hopperPos = this.blockPosition().above();
		BlockState state = this.level().getBlockState(hopperPos);

		// Like a vanilla hopper: pointing down and not locked.
		if (!state.is(Blocks.HOPPER) || state.getValue(HopperBlock.FACING) != Direction.DOWN || !state.getValue(HopperBlock.ENABLED)) {
			return;
		}

		if (!(this.level().getBlockEntity(hopperPos) instanceof HopperBlockEntity hopper)) {
			return;
		}

		// addFuel would reset push and reignite.
		Vec3 heading = this.push;
		this.betterMinecartWithFurnace$refuellingFromHopper = true;

		try {
			for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
				ItemStack itemStack = hopper.getItem(slot);

				if (!itemStack.isEmpty() && this.addFuel(this.position(), itemStack)) {
					hopper.removeItem(slot, 1);
					hopper.setChanged();
					this.addTag(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
					return;
				}
			}
		} finally {
			this.betterMinecartWithFurnace$refuellingFromHopper = false;
			this.push = heading;
		}
	}

	@WrapMethod(method = "applyNaturalSlowdown")
	private Vec3 betterMinecartWithFurnace$applyNaturalSlowdown(Vec3 deltaMovement, Operation<Vec3> original) {
		// The y component is gravity; vanilla drops it too.
		Vec3 movement = deltaMovement.horizontal();
		Vec3 heading = betterMinecartWithFurnace$heading(this.push, movement);
		Vec3 result;

		// Engine off: extinguished, or braked by an inactive powered rail like a normal minecart.
		if (this.betterMinecartWithFurnace$onBrakeRail || this.betterMinecartWithFurnace$isExtinguished()) {
			this.push = Vec3.ZERO;

			try {
				result = original.call(movement);
			} finally {
				this.push = heading;
			}
		} else {
			this.push = heading;
			result = original.call(movement);
		}

		return this.betterMinecartWithFurnace$limitAcceleration(movement, result);
	}

	/**
	 * Push scaled to the configured thrust and aligned with the track. Vanilla only aligns it above
	 * 0.032 blocks/tick, and zeroes it when it is perpendicular to the track.
	 */
	@Unique
	private static Vec3 betterMinecartWithFurnace$heading(Vec3 push, Vec3 movement) {
		double length = push.horizontalDistance();

		if (length < 1.0E-6) {
			// No direction (empty minecart)
			return push;
		}

		double thrust = BetterMinecartWithFurnace.thrust();
		double speed = movement.horizontalDistance();

		if (speed < 1.0E-6) {
			return new Vec3(push.x * thrust / length, 0.0, push.z * thrust / length);
		}

		double scale = (push.x * movement.x + push.z * movement.z < 0.0 ? -thrust : thrust) / speed;
		return new Vec3(movement.x * scale, 0.0, movement.z * scale);
	}

	/**
	 * Below the speed limit the engine delivers at most acceleration more than it did the tick before.
	 * Compared with what it delivered, not with the current speed, which slopes and linked trains keep reducing.
	 */
	@Unique
	private Vec3 betterMinecartWithFurnace$limitAcceleration(Vec3 before, Vec3 after) {
		double speed = after.horizontalDistance();
		boolean ranLastTick = this.betterMinecartWithFurnace$lastSpeedTick == this.tickCount - 1;
		double lastSpeed = this.betterMinecartWithFurnace$lastSpeed;
		this.betterMinecartWithFurnace$lastSpeedTick = this.tickCount;
		this.betterMinecartWithFurnace$lastSpeed = speed;

		if (speed < 1.0E-9 || !(this.level() instanceof ServerLevel level)) {
			return after;
		}

		// Speed along the new direction; negative when reversing.
		double current = (before.x * after.x + before.z * after.z) / speed;

		if (current >= this.getMaxSpeed(level)) {
			return after;
		}

		double allowed = BetterMinecartWithFurnace.acceleration();

		if (current >= 0.0) {
			allowed += ranLastTick ? Math.max(current, lastSpeed) : current;
		}

		if (speed <= allowed) {
			return after;
		}

		this.betterMinecartWithFurnace$lastSpeed = allowed;
		double scale = allowed / speed;
		return new Vec3(after.x * scale, after.y, after.z * scale);
	}
}
