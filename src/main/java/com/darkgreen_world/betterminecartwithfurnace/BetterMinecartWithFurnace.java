package com.darkgreen_world.betterminecartwithfurnace;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterMinecartWithFurnace implements ModInitializer {
	public static final String MOD_ID = "better_minecart_with_furnace";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Entity tag marking an extinguished minecart with furnace. */
	public static final String EXTINGUISHED_TAG = MOD_ID + ".extinguished";

	/** Pulls a Linkart train of 8 at 32 blocks/second. */
	private static final double DEFAULT_THRUST = 0.6;
	private static final String THRUST_KEY = "thrust";

	/** acceleration = thrust / MASS */
	private static final double MASS = 15.0;

	private static double thrust = DEFAULT_THRUST;

	/** Speed (blocks/tick) the engine adds every tick. */
	public static double thrust() {
		return thrust;
	}

	/** Most the speed delivered by the engine (blocks/tick) may rise per tick. */
	public static double acceleration() {
		return thrust / MASS;
	}

	@Override
	public void onInitialize() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".properties");

		try {
			Double configured = Files.exists(file) ? readThrust(file) : null;

			if (configured != null) {
				thrust = configured;
			} else {
				// Missing file, or no usable thrust (e.g. an old "acceleration" config)
				writeDefaults(file);
			}
		} catch (IOException e) {
			LOGGER.warn("Could not read or write config file {}, using the default thrust {}", file, DEFAULT_THRUST, e);
		}
	}

	private static Double readThrust(Path file) throws IOException {
		Properties properties = new Properties();

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}

		String raw = properties.getProperty(THRUST_KEY);

		if (raw == null) {
			return null;
		}

		try {
			double value = Double.parseDouble(raw.trim());

			// !(a > 0) also rejects NaN
			if (!(value > 0.0) || Double.isInfinite(value)) {
				LOGGER.warn("{}: {}={} must be a positive number, resetting it to the default {}", file, THRUST_KEY, raw, DEFAULT_THRUST);
				return null;
			}

			return value;
		} catch (NumberFormatException e) {
			LOGGER.warn("{}: {}={} is not a number, resetting it to the default {}", file, THRUST_KEY, raw, DEFAULT_THRUST);
			return null;
		}
	}

	private static void writeDefaults(Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Files.write(file, List.of(
				"# Thrust of a minecart with furnace: speed (blocks/tick) its engine adds every tick.",
				"# Top speed in blocks/second pulling N linked minecarts: thrust * 19.5 / (0.22 + 0.02 * N).",
				"# Acceleration is thrust / " + (int) MASS + ". Restart the server after changing this.",
				THRUST_KEY + "=" + DEFAULT_THRUST
		), StandardCharsets.UTF_8);
	}
}
