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

	/** Entity tag carried by an extinguished minecart with furnace. Saved with the vanilla Tags list, and can be added or removed with /tag. */
	public static final String EXTINGUISHED_TAG = MOD_ID + ".extinguished";

	private static final double DEFAULT_ACCELERATION = 0.04;
	private static final String ACCELERATION_KEY = "acceleration";

	private static double acceleration = DEFAULT_ACCELERATION;

	/** The most speed (blocks/tick) a minecart with furnace may gain per tick until it reaches its top speed. */
	public static double acceleration() {
		return acceleration;
	}

	@Override
	public void onInitialize() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".properties");

		try {
			if (Files.exists(file)) {
				acceleration = readAcceleration(file);
			} else {
				writeDefaults(file);
			}
		} catch (IOException e) {
			LOGGER.warn("Could not read or write config file {}, using the default acceleration {}", file, DEFAULT_ACCELERATION, e);
		}
	}

	private static double readAcceleration(Path file) throws IOException {
		Properties properties = new Properties();

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}

		String raw = properties.getProperty(ACCELERATION_KEY);

		if (raw == null) {
			return DEFAULT_ACCELERATION;
		}

		double value;

		try {
			value = Double.parseDouble(raw.trim());
		} catch (NumberFormatException e) {
			LOGGER.warn("{}: {}={} is not a number, using the default {}", file, ACCELERATION_KEY, raw, DEFAULT_ACCELERATION);
			return DEFAULT_ACCELERATION;
		}

		// Written as !(a > 0) so that NaN is rejected as well
		if (!(value > 0.0) || Double.isInfinite(value)) {
			LOGGER.warn("{}={} must be a positive number, using the default {}", ACCELERATION_KEY, raw, DEFAULT_ACCELERATION);
			return DEFAULT_ACCELERATION;
		}

		return value;
	}

	private static void writeDefaults(Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Files.write(file, List.of(
				"# Better Minecart with Furnace",
				"#",
				"# Speed a minecart with furnace gains per tick while starting up, in blocks/tick.",
				"# Its top speed is 0.2 blocks/tick, so 0.04 means about 5 ticks to reach top speed.",
				"# For reference, an active powered rail accelerates minecarts by 0.06.",
				"# If set too low (below about 0.03), starting uphill or while pushing other minecarts becomes sluggish or fails.",
				"# Restart the server after changing this.",
				ACCELERATION_KEY + "=" + DEFAULT_ACCELERATION
		), StandardCharsets.UTF_8);
	}
}
