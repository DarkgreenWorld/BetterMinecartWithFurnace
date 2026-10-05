package com.darkgreen_world.betterminecartwithfurnace;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import net.fabricmc.loader.api.FabricLoader;

public final class ModConfig {
	/** The push vanilla gives when fuel is added from the block right behind the minecart. */
	private static final double DEFAULT_THRUST = 1.0;
	private static final double DEFAULT_MAX_ACCELERATION = 0.04;

	public static double thrust = DEFAULT_THRUST;
	public static double maxAcceleration = DEFAULT_MAX_ACCELERATION;

	private ModConfig() {
	}

	/** Speed (blocks/tick) a minecart with furnace with this thrust gains per tick, until the speed limit. */
	public static double acceleration(double thrust) {
		double acceleration = thrust / 50.0;
		return maxAcceleration > 0.0 ? Math.min(acceleration, maxAcceleration) : acceleration;
	}

    //Reads the config file. it is rewritten with the valid values kept, defaults for the rest, and unknown settings dropped.
	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(BetterMinecartWithFurnace.MOD_ID + ".properties");
		thrust = DEFAULT_THRUST;
		maxAcceleration = DEFAULT_MAX_ACCELERATION;

		try {
			if (Files.exists(file)) {
				Properties properties = new Properties();

				try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					properties.load(reader);
				}

				Double thrustValue = number(properties, "thrust");
				Double maxAccelerationValue = number(properties, "maxAcceleration");
				boolean thrustValid = thrustValue != null && (thrustValue >= 0.0 || thrustValue == -1.0);
				boolean maxAccelerationValid = maxAccelerationValue != null && maxAccelerationValue >= 0.0;

				if (thrustValid) {
					thrust = thrustValue;
				} else {
					BetterMinecartWithFurnace.LOGGER.warn("{}: thrust is missing or invalid, resetting it to {}", file, DEFAULT_THRUST);
				}

				if (maxAccelerationValid) {
					maxAcceleration = maxAccelerationValue;
				} else {
					BetterMinecartWithFurnace.LOGGER.warn("{}: maxAcceleration is missing or invalid, resetting it to {}", file, DEFAULT_MAX_ACCELERATION);
				}

				Set<String> unknown = new TreeSet<>(properties.stringPropertyNames());
				unknown.removeAll(Set.of("thrust", "maxAcceleration"));

				if (thrustValid && maxAccelerationValid && unknown.isEmpty()) {
					return;
				}

				if (!unknown.isEmpty()) {
					BetterMinecartWithFurnace.LOGGER.warn("{}: removing unknown settings {}", file, unknown);
				}
			}

			Files.createDirectories(file.getParent());
			Files.write(file, List.of(
					"# Thrust of a minecart with furnace. It accelerates by thrust / 50 blocks/tick every tick until the game's",
					"# speed limit. 0 takes the thrust from vanilla. -1 uses the vanilla engine.",
					"# Default: 1.0",
					"thrust=" + thrust,
					"# Upper limit for the acceleration, in blocks/tick per tick, also meant for mods that move trains.",
					"# 0 turns it off.",
					"# Default: 0.04",
					"maxAcceleration=" + maxAcceleration
			), StandardCharsets.UTF_8);
		} catch (IOException e) {
			BetterMinecartWithFurnace.LOGGER.warn("Could not read or write config file {}, using the defaults", file, e);
		}
	}

	/** The value if it is a finite number, otherwise null. */
	private static Double number(Properties properties, String key) {
		try {
			double value = Double.parseDouble(properties.getProperty(key, "").trim());
			return Double.isFinite(value) ? value : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
