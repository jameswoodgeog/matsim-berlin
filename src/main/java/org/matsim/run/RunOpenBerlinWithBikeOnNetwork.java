package org.matsim.run;

import org.apache.commons.compress.utils.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import picocli.CommandLine;
import scala.util.parsing.combinator.testing.Str;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Extend the {@link OpenBerlinScenario} to simulated bike on the network. <br>
 */
public class RunOpenBerlinWithBikeOnNetwork extends OpenBerlinScenario {

	private static final Logger log = LogManager.getLogger(RunOpenBerlinWithBikeOnNetwork.class);

	@CommandLine.Option(names = "--bike", defaultValue = "bikeTeleportedStandardMatsim", description = "Define how bicycles are simulated")
	private BicycleHandling bicycleHandling;

	public static void main(String[] args) {
		MATSimApplication.run(RunOpenBerlinWithBikeOnNetwork.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		super.prepareConfig(config);

		// no mode choice if we simulate bikes
		for (ReplanningConfigGroup.StrategySettings strategySettings: config.replanning().getStrategySettings()) {
			if (strategySettings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)) {
				strategySettings.setWeight(0.0);
			}
		}

		switch (bicycleHandling) {
			case onNetworkWithStandardMatsim -> {
				log.info("Simulating with bikes on the network");
				// add bike to network modes in qsim:
				Set<String> modes = Sets.newHashSet(TransportMode.bike);
				modes.addAll(config.qsim().getMainModes());
				config.qsim().setMainModes(modes);
				config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

				//add bike as a network mode
				Collection<String> networkModes = Sets.newHashSet(TransportMode.bike);

				networkModes.addAll(config.routing().getNetworkModes());
				config.routing().setNetworkModes(networkModes);

				//remove teleported modes params
				config.routing().removeTeleportedModeParams(TransportMode.bike);
			}

			case onNetworkWithBicycleContrib -> {
				// bike is routed on the network per the xml config.

				log.info("Simulating with bikes on the network and bicycle contrib");

				// add bike to network modes in qsim:
				Set<String> modes = Sets.newHashSet(TransportMode.bike);
				modes.addAll(config.qsim().getMainModes());
				config.qsim().setMainModes(modes);
				config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

				// this activates the bicycleConfigGroup.  But the module still needs to be loaded for the controler.
				BicycleConfigGroup bikeConfigGroup = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
				bikeConfigGroup.setBicycleMode(TransportMode.bike);
			}
			case bikeTeleportedStandardMatsim -> {
				log.info("Simulating assuming bikes are teleported, this is the default in the input config");
			}
			default -> throw new IllegalStateException("Unexpected value: " + bicycleHandling);
		}
		return config;
	}


	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		if (bicycleHandling == BicycleHandling.onNetworkWithStandardMatsim || bicycleHandling == BicycleHandling.onNetworkWithBicycleContrib) {
			for (Person person: scenario.getPopulation().getPersons().values()) {
				PopulationUtils.resetRoutes(person.getSelectedPlan());
			}
		}

		// we need to define a vehicle type if we want to route bikes on the network
		if (bicycleHandling == BicycleHandling.onNetworkWithStandardMatsim || bicycleHandling == BicycleHandling.onNetworkWithBicycleContrib) {
			/*Id<VehicleType> typeId = Id.create(TransportMode.bike, VehicleType.class);
			VehicleType bikeVehicle = VehicleUtils.createVehicleType(typeId);
			// I took these values from: https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.3/input/leipzig-v1.3-vehicle-types.xml
			bikeVehicle.setMaximumVelocity(4.16);
			bikeVehicle.setLength(2.0);
			bikeVehicle.setWidth(1.0);
			bikeVehicle.setNetworkMode(TransportMode.bike);
			bikeVehicle.setPcuEquivalents(0.2);
			scenario.getVehicles().addVehicleType(bikeVehicle); */
		}

		if (bicycleHandling == BicycleHandling.onNetworkWithStandardMatsim || bicycleHandling == BicycleHandling.onNetworkWithBicycleContrib) {
			for (Link link: scenario.getNetwork().getLinks().values()) {
				// if it is a car link bikes can be added
				if (link.getAllowedModes().contains(TransportMode.car)) {
					//check OSM link type
					String type = (String) link.getAttributes().getAttribute("type");
					//not add bikes to trunk and motorways
					if (!type.contains("motorway") && !type.contains("trunk")) {
						Set<String> allowedModes = Sets.newHashSet(TransportMode.bike);
						allowedModes.addAll(link.getAllowedModes());
						link.setAllowedModes(allowedModes);
					}
				}
			}
			//run network cleaner for
			new MultimodalNetworkCleaner(scenario.getNetwork()).run(Collections.singleton(TransportMode.bike));
		}

		//List<Map<String, Object>> osmAttributes = annotateNetworkWithOSMAttributes("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/input/berlin-v6.4-network-ft.csv.gz");



	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);

		if (bicycleHandling == BicycleHandling.onNetworkWithBicycleContrib) {
			controler.addOverridingModule(new BicycleModule());
		}
	}

	/**
	 * Defines how bicycles are scored.
	 */
	@SuppressWarnings("checkstyle:OneTopLevelClass")
	enum BicycleHandling {onNetworkWithStandardMatsim, onNetworkWithBicycleContrib, bikeTeleportedStandardMatsim}

	private static List<Map<String, Object>> annotateNetworkWithOSMAttributes(String filePath) {
		BufferedReader reader = null;
		String line = "";

		List<Map<String, Object>> osmAttribbutesList = null;
		try {
			// Create a BufferedReader to read the CSV file
			reader = new BufferedReader(new FileReader(filePath));
			osmAttribbutesList = new ArrayList<>();

			// Read the header (first line)
			String header = reader.readLine();
			if (header == null) {
				throw new IOException("CSV file is empty or does not contain a header");
			}

			// Split header line into column names (keys for the map)
			String[] headers = header.split(",");

			// Read each line of the CSV file
			while ((line = reader.readLine()) != null) {
				String[] data = line.split(",");  // Split the line by commas

				// Create a map for this row
				Map<String, Object> linkAttribute = new HashMap<>();

				// Populate the map with key-value pairs, checking if the value is numeric
				for (int i = 0; i < headers.length; i++) {
					String value = data[i].trim();  // Trim any extra whitespace
					if (isNumeric(value)) {
						// If the value is numeric, try to parse it to an Integer or Double
						try {
							linkAttribute.put(headers[i], Integer.parseInt(value));
						} catch (NumberFormatException e) {
							// If parsing as an integer fails, try parsing as a double
							linkAttribute.put(headers[i], Double.parseDouble(value));
						}
					} else {
						// If the value is not numeric, store it as a String
						linkAttribute.put(headers[i], value);
					}
				}

				// Add the map to the list of osmAttribbutesList
				osmAttribbutesList.add(linkAttribute);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
					// Close the reader after use
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return osmAttribbutesList;
	}

	// Helper method to determine if a value is numeric
	private static boolean isNumeric(String str) {
		try {
			// Try parsing the string to an integer or double
			Integer.parseInt(str);  // Try parsing as an integer
			return true;  // If no exception is thrown, it's numeric
		} catch (NumberFormatException e1) {
			try {
				Double.parseDouble(str);  // Try parsing as a double
				return true;
			} catch (NumberFormatException e2) {
				return false;  // If both parsing attempts fail, it's not numeric
			}
		}
	}
}



