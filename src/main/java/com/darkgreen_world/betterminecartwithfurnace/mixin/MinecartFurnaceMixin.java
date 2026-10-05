package com.darkgreen_world.betterminecartwithfurnace.mixin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.darkgreen_world.betterminecartwithfurnace.BetterMinecartWithFurnace;
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
import net.minecraft.world.entity.Entity;
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
public abstract class MinecartFurnaceMixin extends AbstractMinecart implements BetterMinecartWithFurnace.Engine {
	@Shadow
	private int fuel;

	@Shadow
	public Vec3 push;

	@Shadow
	public abstract boolean addFuel(Vec3 interactingPos, ItemStack itemStack);

	/** On an inactive powered rail at the start of this tick. */
	@Unique
	private boolean betterMinecartWithFurnace$onBrakeRail;

	/** Velocity the engine gave it this tick. */
	@Unique
	private Vec3 betterMinecartWithFurnace$engineVelocity;

	protected MinecartFurnaceMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public void betterMinecartWithFurnace$setEngineVelocity(Vec3 velocity) {
		this.betterMinecartWithFurnace$engineVelocity = velocity;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void betterMinecartWithFurnace$beforeTick(CallbackInfo ci) {
		if (this.level().isClientSide()) {
			return;
		}

		this.betterMinecartWithFurnace$engineVelocity = null;
		BlockState rail = this.level().getBlockState(this.getCurrentBlockPosOrRailBelow());
		this.betterMinecartWithFurnace$onBrakeRail = rail.is(Blocks.POWERED_RAIL) && !rail.getValue(PoweredRailBlock.POWERED);

		// an extinguished minecart keeps its fuel.
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
			this.removeTag(Extinguishing.TAG);
		}

		boolean burning = this.fuel > 0 && !Extinguishing.isExtinguished(this);

		if (!burning && this.tickCount % 8 == 0) {
			this.betterMinecartWithFurnace$refuelFromHopperAbove();
		}

		if (this.betterMinecartWithFurnace$engineVelocity != null && AbstractMinecart.useExperimentalMovement(this.level())) {
			this.betterMinecartWithFurnace$pushMinecartsAhead(this.betterMinecartWithFurnace$engineVelocity);
		}
	}

	/** Shows the furnace unlit while extinguished. */
	@ModifyVariable(method = "setHasFuel", at = @At("HEAD"), argsOnly = true)
	private boolean betterMinecartWithFurnace$hideFlameWhileExtinguished(boolean fuel) {
		return fuel && !Extinguishing.isExtinguished(this);
	}

	@WrapMethod(method = "applyNaturalSlowdown")
	private Vec3 betterMinecartWithFurnace$applyNaturalSlowdown(Vec3 deltaMovement, Operation<Vec3> original) {
		// The y component is gravity.
		Vec3 movement = deltaMovement.horizontal();
		Vec3 heading = betterMinecartWithFurnace$heading(this.push, movement);

		// Engine off: no fuel, extinguished, or braked by an inactive powered rail.
		if (heading.lengthSqr() < 1.0E-12 || this.betterMinecartWithFurnace$onBrakeRail || Extinguishing.isExtinguished(this)) {
			this.push = Vec3.ZERO;
			Vec3 coasting = original.call(movement);
			this.push = heading;
			return coasting;
		}

		if (ModConfig.thrust < 0.0 && ModConfig.maxAcceleration <= 0.0 || !(this.level() instanceof ServerLevel level)) {
			this.betterMinecartWithFurnace$engineVelocity = original.call(deltaMovement);
			return this.betterMinecartWithFurnace$engineVelocity;
		}

		this.push = heading;
		this.betterMinecartWithFurnace$engineVelocity = this.betterMinecartWithFurnace$accelerate(level, movement, heading, original.call(movement));
		return this.betterMinecartWithFurnace$engineVelocity;
	}

	@Unique
	private static Vec3 betterMinecartWithFurnace$heading(Vec3 push, Vec3 movement) {
		double length = push.horizontalDistance();

		if (length < 1.0E-6) {
			// No direction (empty minecart)
			return push;
		}

		double thrust = ModConfig.thrust > 0.0 ? ModConfig.thrust : length;
		double speed = movement.horizontalDistance();

		if (speed < 1.0E-6) {
			return push.horizontal().scale(thrust / length);
		}

		return movement.scale((push.dot(movement) < 0.0 ? -thrust : thrust) / speed);
	}

	@Unique
	private Vec3 betterMinecartWithFurnace$accelerate(ServerLevel level, Vec3 movement, Vec3 heading, Vec3 vanilla) {
		Vec3 direction = heading.normalize();
		double limit = this.getMaxSpeed(level);
		// Moving the other way counts as standing still.
		double current = Math.max(0.0, movement.dot(direction));

		if (ModConfig.thrust < 0.0) {
			double speed = vanilla.horizontalDistance();
			double allowed = current + ModConfig.maxAcceleration;
			return current >= limit || speed <= allowed ? vanilla : vanilla.scale(allowed / speed);
		}

		if (current >= limit) {
			// Held at the limit.
			return vanilla.horizontalDistance() > limit ? vanilla : direction.scale(limit);
		}

		double acceleration = ModConfig.acceleration(heading.length()) * (this.isInWater() ? 0.1 : 1.0);
		return direction.scale(Math.max(current + acceleration, 0.0101));
	}

	@Unique
	private void betterMinecartWithFurnace$pushMinecartsAhead(Vec3 velocity) {
		List<AbstractMinecart> line = new ArrayList<>();
		Entity last = this;
		Vec3 direction = velocity.normalize();

		// 64 is only a cap for huge piles of minecarts.
		while (line.size() < 64) {
			Vec3 from = last.position();
			Vec3 along = direction;
			// Touching, and ahead within 60 degrees.
			AbstractMinecart next = this.level()
					.getEntitiesOfClass(AbstractMinecart.class, last.getBoundingBox().inflate(0.2, 0.0, 0.2), minecart -> minecart != this
							&& !line.contains(minecart)
							&& minecart.isPushable()
							&& minecart.position().subtract(from).horizontal().normalize().dot(along) > 0.5)
					.stream()
					.min(Comparator.comparingDouble(minecart -> minecart.distanceToSqr(from)))
					.orElse(null);

			if (next == null) {
				break;
			}

			direction = next.position().subtract(from).horizontal().normalize();
			line.add(next);
			last = next;
		}

		if (line.isEmpty()) {
			return;
		}

		this.setDeltaMovement(velocity);

		for (AbstractMinecart minecart : line) {
			minecart.push(this);
		}

		this.setDeltaMovement(velocity);
	}

	/** Takes one fuel item from a hopper above. */
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
				// addFuel would aim it away from the hopper.
				this.push = heading;
				this.addTag(Extinguishing.TAG);
				hopper.removeItem(slot, 1);
				hopper.setChanged();
				return;
			}
		}
	}
}
