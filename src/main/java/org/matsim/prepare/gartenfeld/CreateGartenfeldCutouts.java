package org.matsim.prepare.gartenfeld;

import org.matsim.application.prepare.population.DownSamplePopulation;
import org.matsim.application.prepare.population.PersonNetworkLinkCheck;
import org.matsim.application.prepare.scenario.CreateScenarioCutOut;
import org.matsim.prepare.network.ModifyNetwork;
import org.matsim.run.OpenBerlinScenario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This is not meant to be used the actual Gartenfeld-scenario, but to test the cutout procedure. Gartenfeld is used as an example project. <br>
 * Procedure is copied from {@link CreateGartenfeldComplete} class. <br>
 * This class may be deleted later on.
 */
public class CreateGartenfeldCutouts {

	final static String OUTPUT_PATH = "input/cutouts";
	final static String BERLIN_POPULATION_SOURCE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v%s/output/berlin-v%s-10pct/berlin-v%s.output_plans.xml.gz".formatted(OpenBerlinScenario.VERSION, OpenBerlinScenario.VERSION, OpenBerlinScenario.VERSION);
	final static String BERLIN_NETWORK_SOURCE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v%s/input/berlin-v%s-network-with-pt.xml.gz".formatted(OpenBerlinScenario.VERSION, OpenBerlinScenario.VERSION);
	final static String BERLIN_FACILITIES_SOURCE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v%s/input/berlin-v%s-facilities.xml.gz".formatted(OpenBerlinScenario.VERSION, OpenBerlinScenario.VERSION);
	final static String BERLIN_EVENTS_SOURCE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v%s/output/berlin-v%s-10pct/berlin-v%s.output_events.xml.gz".formatted(OpenBerlinScenario.VERSION, OpenBerlinScenario.VERSION, OpenBerlinScenario.VERSION);

	String networkPath;
	String populationPath;
	String facilitiesPath;
	String networkChangeEventsPath;

	private void prepareOutputPaths(String testedMethod){
		try {
			Files.createDirectories(Path.of(OUTPUT_PATH + "/" + testedMethod));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		populationPath = OUTPUT_PATH + "/" + testedMethod + "/gartenfeld-v6.4." + testedMethod + ".population.xml.gz";
		networkPath = OUTPUT_PATH + "/" + testedMethod + "/gartenfeld-v6.4." + testedMethod + ".network.xml.gz";
		facilitiesPath = OUTPUT_PATH + "/" + testedMethod + "/gartenfeld-v6.4." + testedMethod + ".facilities.xml.gz";
		networkChangeEventsPath = OUTPUT_PATH + "/" + testedMethod + "/gartenfeld-v6.4." + testedMethod + ".network-change-events.xml.gz";
	}

	private void finish(){
		createNetwork(networkPath, networkPath, "input/gartenfeld/DNG_network.gpkg");

		new PersonNetworkLinkCheck().execute(
			"--input", populationPath,
			"--network", networkPath,
			"--output", populationPath
		);

		// Was originally in the Gartenfeld preparation code but too small to use as a comparison
		/*
		new DownSamplePopulation().execute(
			populationPath,
			"--sample-size", "0.1",
			"--samples", "0.01"
		);
		*/
	}

	//TODO Discuss whether to use the gartenfeld generated pop or the berlin pop
	//TODO Also discuss if it should be allowed to use the berlin events file
	/**
	 * In the original code, {@link CreateGartenfeldPopulation} is used for the population. Check if this makes a large difference.
	 */
	public void testGPKeepCapacities(){
		final String generatedPopulationPath = OUTPUT_PATH + "/gartenfeld-v6.4.generated.population.xml.gz";

		if (!Files.exists(Path.of(generatedPopulationPath))) {
			new CreateGartenfeldPopulation().execute(
				"--output", generatedPopulationPath
			);
		}

		prepareOutputPaths("gp-keepCapacities");

		new CreateScenarioCutOut().execute(
			"--population", generatedPopulationPath,
			"--network", BERLIN_NETWORK_SOURCE,
			"--facilities", BERLIN_FACILITIES_SOURCE,
			"--events", BERLIN_EVENTS_SOURCE,
			"--shp", "input/gartenfeld/DNG_dilution_area.gpkg",
			"--input-crs", OpenBerlinScenario.CRS,
			"--buffer", "1000",
			"--network-modes", "car,bike",
			"--clean-modes", "truck,freight,ride",
			"--capacity-calculation", "keepCapacities",
			"--output-network", networkPath,
			"--output-population", populationPath,
			"--output-facilities", facilitiesPath,
			"--output-network-change-events", networkChangeEventsPath
		);

		finish();
	}

	public void testGPSubtractLostVehiclesCapacities(){
		final String generatedPopulationPath = OUTPUT_PATH + "/gartenfeld-v6.4.generated.population.xml.gz";

		if (!Files.exists(Path.of(generatedPopulationPath))) {
			new CreateGartenfeldPopulation().execute(
				"--output", generatedPopulationPath
			);
		}

		prepareOutputPaths("gp-subtractLostVehiclesCapacities");

		new CreateScenarioCutOut().execute(
			"--population", generatedPopulationPath,
			"--network", BERLIN_NETWORK_SOURCE,
			"--facilities", BERLIN_FACILITIES_SOURCE,
			"--events", BERLIN_EVENTS_SOURCE,
			"--shp", "input/gartenfeld/DNG_dilution_area.gpkg",
			"--input-crs", OpenBerlinScenario.CRS,
			"--buffer", "1000",
			"--network-modes", "car,bike",
			"--clean-modes", "truck,freight,ride",
			"--capacity-calculation", "subtractLostVehiclesCapacities",
			"--output-network", networkPath,
			"--output-population", populationPath,
			"--output-facilities", facilitiesPath,
			"--output-network-change-events", networkChangeEventsPath
		);

		finish();
	}

	public void testGPRelativeAdjustmentOfCapacities(){
		final String generatedPopulationPath = OUTPUT_PATH + "/gartenfeld-v6.4.generated.population.xml.gz";

		if (!Files.exists(Path.of(generatedPopulationPath))) {
			new CreateGartenfeldPopulation().execute(
				"--output", generatedPopulationPath
			);
		}

		prepareOutputPaths("gp-relativeAdjustmentOfCapacities");

		new CreateScenarioCutOut().execute(
			"--population", generatedPopulationPath,
			"--network", BERLIN_NETWORK_SOURCE,
			"--facilities", BERLIN_FACILITIES_SOURCE,
			"--events", BERLIN_EVENTS_SOURCE,
			"--shp", "input/gartenfeld/DNG_dilution_area.gpkg",
			"--input-crs", OpenBerlinScenario.CRS,
			"--buffer", "1000",
			"--network-modes", "car,bike",
			"--clean-modes", "truck,freight,ride",
			"--capacity-calculation", "relativeAdjustmentOfCapacities",
			"--output-network", networkPath,
			"--output-population", populationPath,
			"--output-facilities", facilitiesPath,
			"--output-network-change-events", networkChangeEventsPath
		);

		finish();
	}

	public void testGPProportionalFreespeedsCleaning(){
		final String generatedPopulationPath = OUTPUT_PATH + "/gartenfeld-v6.4.generated.population.xml.gz";

		if (!Files.exists(Path.of(generatedPopulationPath))) {
			new CreateGartenfeldPopulation().execute(
				"--output", generatedPopulationPath
			);
		}

		prepareOutputPaths("gp-proportionalFreespeedsCleaning");

		new CreateScenarioCutOut().execute(
			"--population", generatedPopulationPath,
			"--network", BERLIN_NETWORK_SOURCE,
			"--facilities", BERLIN_FACILITIES_SOURCE,
			"--events", BERLIN_EVENTS_SOURCE,
			"--shp", "input/gartenfeld/DNG_dilution_area.gpkg",
			"--input-crs", OpenBerlinScenario.CRS,
			"--buffer", "1000",
			"--network-modes", "car,bike",
			"--clean-modes", "truck,freight,ride",
			"--capacity-calculation", "proportionalFreespeedsCleaning",
			"--output-network", networkPath,
			"--output-population", populationPath,
			"--output-facilities", facilitiesPath,
			"--output-network-change-events", networkChangeEventsPath
		);

		finish();
	}

	public void testGPModeledFreespeedsCleaning(){
		final String generatedPopulationPath = OUTPUT_PATH + "/gartenfeld-v6.4.generated.population.xml.gz";

		if (!Files.exists(Path.of(generatedPopulationPath))) {
			new CreateGartenfeldPopulation().execute(
				"--output", generatedPopulationPath
			);
		}

		prepareOutputPaths("gp-modeledFreespeedsCleaning");

		new CreateScenarioCutOut().execute(
			"--population", generatedPopulationPath,
			"--network", BERLIN_NETWORK_SOURCE,
			"--facilities", BERLIN_FACILITIES_SOURCE,
			"--events", BERLIN_EVENTS_SOURCE,
			"--shp", "input/gartenfeld/DNG_dilution_area.gpkg",
			"--input-crs", OpenBerlinScenario.CRS,
			"--buffer", "1000",
			"--network-modes", "car,bike",
			"--clean-modes", "truck,freight,ride",
			"--capacity-calculation", "modeledFreespeedsCleaning",
			"--output-network", networkPath,
			"--output-population", populationPath,
			"--output-facilities", facilitiesPath,
			"--output-network-change-events", networkChangeEventsPath
		);

		finish();
	}

	public void testKeepCapacities(){
		prepareOutputPaths("keepCapacities");

		new CreateScenarioCutOut().execute(
			"--population", BERLIN_POPULATION_SOURCE,
			"--network", BERLIN_NETWORK_SOURCE,
			"--facilities", BERLIN_FACILITIES_SOURCE,
			"--events", BERLIN_EVENTS_SOURCE,
			"--shp", "input/gartenfeld/DNG_dilution_area.gpkg",
			"--input-crs", OpenBerlinScenario.CRS,
			"--buffer", "1000",
			"--network-modes", "car,bike",
			"--clean-modes", "truck,freight,ride",
			"--capacity-calculation", "keepCapacities",
			"--output-network", networkPath,
			"--output-population", populationPath,
			"--output-facilities", facilitiesPath,
			"--output-network-change-events", networkChangeEventsPath
		);

		finish();
	}

	public void testSubtractLostVehiclesCapacities(){
		prepareOutputPaths("subtractLostVehiclesCapacities");

		new CreateScenarioCutOut().execute(
			"--population", BERLIN_POPULATION_SOURCE,
			"--network", BERLIN_NETWORK_SOURCE,
			"--facilities", BERLIN_FACILITIES_SOURCE,
			"--events", BERLIN_EVENTS_SOURCE,
			"--shp", "input/gartenfeld/DNG_dilution_area.gpkg",
			"--input-crs", OpenBerlinScenario.CRS,
			"--buffer", "1000",
			"--network-modes", "car,bike",
			"--clean-modes", "truck,freight,ride",
			"--capacity-calculation", "subtractLostVehiclesCapacities",
			"--output-network", networkPath,
			"--output-population", populationPath,
			"--output-facilities", facilitiesPath,
			"--output-network-change-events", networkChangeEventsPath
		);

		finish();
	}

	public void testRelativeAdjustmentOfCapacities(){
		prepareOutputPaths("relativeAdjustmentOfCapacities");

		new CreateScenarioCutOut().execute(
			"--population", BERLIN_POPULATION_SOURCE,
			"--network", BERLIN_NETWORK_SOURCE,
			"--facilities", BERLIN_FACILITIES_SOURCE,
			"--events", BERLIN_EVENTS_SOURCE,
			"--shp", "input/gartenfeld/DNG_dilution_area.gpkg",
			"--input-crs", OpenBerlinScenario.CRS,
			"--buffer", "1000",
			"--network-modes", "car,bike",
			"--clean-modes", "truck,freight,ride",
			"--capacity-calculation", "relativeAdjustmentOfCapacities",
			"--output-network", networkPath,
			"--output-population", populationPath,
			"--output-facilities", facilitiesPath,
			"--output-network-change-events", networkChangeEventsPath
		);

		finish();
	}

	public void testProportionalFreespeedsCleaning(){
		prepareOutputPaths("proportionalFreespeedsCleaning");

		new CreateScenarioCutOut().execute(
			"--population", BERLIN_POPULATION_SOURCE,
			"--network", BERLIN_NETWORK_SOURCE,
			"--facilities", BERLIN_FACILITIES_SOURCE,
			"--events", BERLIN_EVENTS_SOURCE,
			"--shp", "input/gartenfeld/DNG_dilution_area.gpkg",
			"--input-crs", OpenBerlinScenario.CRS,
			"--buffer", "1000",
			"--network-modes", "car,bike",
			"--clean-modes", "truck,freight,ride",
			"--capacity-calculation", "proportionalFreespeedsCleaning",
			"--output-network", networkPath,
			"--output-population", populationPath,
			"--output-facilities", facilitiesPath,
			"--output-network-change-events", networkChangeEventsPath
		);

		finish();
	}

	public void testModeledFreespeedsCleaning(){
		prepareOutputPaths("modeledFreespeedsCleaning");

		new CreateScenarioCutOut().execute(
			"--population", BERLIN_POPULATION_SOURCE,
			"--network", BERLIN_NETWORK_SOURCE,
			"--facilities", BERLIN_FACILITIES_SOURCE,
			"--events", BERLIN_EVENTS_SOURCE,
			"--shp", "input/gartenfeld/DNG_dilution_area.gpkg",
			"--input-crs", OpenBerlinScenario.CRS,
			"--buffer", "1000",
			"--network-modes", "car,bike",
			"--clean-modes", "truck,freight,ride",
			"--capacity-calculation", "modeledFreespeedsCleaning",
			"--output-network", networkPath,
			"--output-population", populationPath,
			"--output-facilities", facilitiesPath,
			"--output-network-change-events", networkChangeEventsPath
		);

		finish();
	}

	private static void createNetwork(String network, String outputNetwork, String shp) {

		new ModifyNetwork().execute(
			"--network", network,
			"--remove-links", "input/gartenfeld/DNG_LinksToDelete.txt",
			"--shp", shp,
			"--output", outputNetwork
		);

	}

	public static void main(String[] args) {
		CreateGartenfeldCutouts c = new CreateGartenfeldCutouts();

		c.testGPKeepCapacities();
		c.testGPSubtractLostVehiclesCapacities();
		c.testGPRelativeAdjustmentOfCapacities();
		c.testGPProportionalFreespeedsCleaning();
		c.testGPModeledFreespeedsCleaning();

		c.testKeepCapacities();
		c.testSubtractLostVehiclesCapacities();
		c.testRelativeAdjustmentOfCapacities();
		c.testProportionalFreespeedsCleaning();
		c.testModeledFreespeedsCleaning();
	}
}
