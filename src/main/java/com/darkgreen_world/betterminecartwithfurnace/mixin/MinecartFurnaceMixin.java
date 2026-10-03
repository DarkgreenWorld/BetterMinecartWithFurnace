package com.darkgreen_world.betterminecartwithfurnace.mixin;

import com.darkgreen_world.betterminecartwithfurnace.Extinguishing;
import com.darkgreen_world.betterminecartwithfurnace.ModConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(MinecartFurnace.class)
public abstract class MinecartFurnaceMixin extends AbstractMinecart {
	@Shadow
	private int fuel;

	@Shadow
	public Vec3 push;

	@Shadow
	public abstract boolean addFuel(Vec3 interactingPos, ItemStack itemStack);

	/** On an inactive powered rail at the start of this tick. */
	@Unique
	private boolean betterMinecartWithFurnace$onBrakeRail;

	/** Speed the engine last delivered, and the tick it did so. */
	@Unique
	private double betterMinecartWithFurnace$lastSpeed;

	@Unique
	private int betterMinecartWithFurnace$lastSpeedTick;

	protected MinecartFurnaceMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void betterMinecartWithFurnace$beforeTick(CallbackInfo ci) {
		if (this.level().isClientSide()) {
			return;
		}

		BlockState rail = this.level().getBlockState(this.getCurrentBlockPosOrRailBelow());
		this.betterMinecartWithFurnace$onBrakeRail = rail.is(Blocks.POWERED_RAIL) && !rail.getValue(PoweredRailBlock.POWERED);

		// Cancels the --fuel at the end of the vanilla tick.
		if (this.fuel > 0 && Extinguishing.isExtinguished(this)) {
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
			this.removeTag(Extinguishing.TAG);
		}

		boolean burning = this.fuel > 0 && !Extinguishing.isExtinguished(this);

		// 8 ticks is the vanilla hopper rate.
		if (!burning && this.tickCount % 8 == 0) {
			this.betterMinecartWithFurnace$refuelFromHopperAbove();
		}
	}

	/** Shows the furnace unlit while extinguished. */
	@ModifyVariable(method = "setHasFuel", at = @At("HEAD"), argsOnly = true)
	private boolean betterMinecartWithFurnace$hideFlameWhileExtinguished(boolean fuel) {
		return fuel && !Extinguishing.isExtinguished(this);
	}

	/** Takes one fuel item from a hopper above, without lighting the minecart or changing its direction. */
	@Unique
	private void betterMinecartWithFurnace$refuelFromHopperAbove() {
		BlockPos hopperPos = this.blockPosition().above();
		BlockState state = this.level().getBlockState(hopperPos);

		// Like a vanilla hopper: pointing down and not locked.
		if (!state.is(Blocks.HOPPER) || state.getValue(HopperBlock.FACING) != Direction.DOWN || !state.getValue(HopperBlock.ENABLED)
				|| !(this.level().getBlockEntity(hopperPos) instanceof HopperBlockEntity hopper)) {
			return;
		}

		Vec3 heading = this.push;

		for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
			if (this.addFuel(this.position(), hopper.getItem(slot))) {
				// addFuel aims push from the given position.
				this.push = heading;
				this.addTag(Extinguishing.TAG);
				hopper.removeItem(slot, 1);
				hopper.setChanged();
				return;
			}
		}
	}

	@WrapMethod(method = "applyNaturalSlowdown")
	private Vec3 betterMinecartWithFurnace$applyNaturalSlowdown(Vec3 deltaMovement, Operation<Vec3> original) {
		// The y component is gravity; vanilla drops it too.
		Vec3 movement = deltaMovement.horizontal();
		Vec3 heading = betterMinecartWithFurnace$alignedPush(this.push, movement);

		// Engine off: extinguished, or braked by an inactive powered rail like a normal minecart.
		boolean engineOff = this.betterMinecartWithFurnace$onBrakeRail || Extinguishing.isExtinguished(this);
		this.push = engineOff ? Vec3.ZERO : heading;
		Vec3 result = original.call(movement);
		this.push = heading;

		return this.betterMinecartWithFurnace$limitAcceleration(movement, result);
	}

	/**
	 * Push scaled to the configured thrust and aligned with the track. Vanilla only aligns it above
	 * 0.032 blocks/tick, and zeroes it when it is perpendicular to the track.
	 */
	@Unique
	private static Vec3 betterMinecartWithFurnace$alignedPush(Vec3 push, Vec3 movement) {
		double length = push.horizontalDistance();
		double speed = movement.horizontalDistance();
		double thrust = ModConfig.thrust;

		if (length < 1.0E-6) {
			// No direction (empty minecart)
			return push;
		}

		if (speed < 1.0E-6) {
			return push.horizontal().scale(thrust / length);
		}

		return movement.scale((push.dot(movement) < 0.0 ? -thrust : thrust) / speed);
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
		double current = before.dot(after) / speed;

		if (current >= this.getMaxSpeed(level)) {
			return after;
		}

		double allowed = ModConfig.acceleration() + (current < 0.0 ? 0.0 : ranLastTick ? Math.max(current, lastSpeed) : current);

		if (speed <= allowed) {
			return after;
		}

		this.betterMinecartWithFurnace$lastSpeed = allowed;
		return after.scale(allowed / speed);
	}
}
