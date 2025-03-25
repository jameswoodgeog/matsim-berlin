package org.matsim.run.policies;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.parking.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.kernel.ConstantKernelDistance;
import org.matsim.core.network.kernel.DefaultKernelFunction;
import org.matsim.core.network.kernel.KernelDistance;
import org.matsim.core.network.kernel.NetworkKernelFunction;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.OpenBerlinScenario;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingCapacityInitializer.LINK_OFF_STREET_SPOTS;
import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingCapacityInitializer.LINK_ON_STREET_SPOTS;

/*
This class extends the matsim berlin scenario by parking functionality
 */

public class OpenBerlinWithParking extends OpenBerlinScenario {


	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinWithParking.class, args);
	}


	@Override
	protected Config prepareConfig(Config config) {
		return super.prepareConfig(config);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);

		Map<String, ParkingData> parkingMap = readCSV("input/v6.4/parking/parking_per_link_wip_2025_03_24.csv");

		for (Link link : scenario.getNetwork().getLinks().values()) {

			if (parkingMap.containsKey(link.getId().toString())) {
				link.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, parkingMap.get(link.getId()).onstreetSpots);
				link.getAttributes().putAttribute(LINK_OFF_STREET_SPOTS, parkingMap.get(link.getId()).offstreetSpots);
			}
		}

		NetworkUtils.writeNetwork(scenario.getNetwork(), "testNetwork.xml.gz");

	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);
		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addQSimComponentBinding("ParkingOccupancyOberserver").to(ParkingOccupancyObserver.class);
				addMobsimScopeEventHandlerBinding().to(ParkingOccupancyObserver.class);
				addVehicleHandlerBinding().to(ParkingVehicleHandler.class);
				bind(ParkingOccupancyObservingSearchTimeCalculator.class).in(Singleton.class);
				addParkingSearchTimeCalculatorBinding().to(ParkingOccupancyObservingSearchTimeCalculator.class);
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ParkingOccupancyObserver.class).in(Singleton.class);
				bind(ParkingCapacityInitializer.class).to(PlanBasedParkingCapacityInitializer.class);
				bind(NetworkKernelFunction.class).to(DefaultKernelFunction.class);
				bind(KernelDistance.class).toInstance(new ConstantKernelDistance(100));
				// use parameters from Belloche Paper
				bind(ParkingSearchTimeFunction.class).toInstance(new BellochePenaltyFunction(2, -6));
				addControlerListenerBinding().to(ParkingOccupancyObserver.class);
				addMobsimListenerBinding().to(ParkingOccupancyObserver.class);
			}
		});
	}

	public static class ParkingData {
		int onstreetSpots;
		int offstreetSpots;

		public ParkingData(int onstreetSpots, int offstreetSpots) {
			this.onstreetSpots = onstreetSpots;
			this.offstreetSpots = offstreetSpots;
		}

		@Override
		public String toString() {
			return "Onstreet spots: " + onstreetSpots + ", Offstreet spots: " + offstreetSpots;
		}
	}

	public static Map<String, ParkingData> readCSV(String filePath) {
		Map<String, ParkingData> parkingMap = new HashMap<>();
		String line;

		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			// Skip the header line
			br.readLine();

			// Read each subsequent line in the CSV
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");

				// Assuming the order: id, onstreet_spots, offstreet_spots
				String id = values[0];
				int onstreetSpots = parseSpotValue(values[1]);
				int offstreetSpots = parseSpotValue(values[2]);

				// Create a ParkingData object and store it in the map
				ParkingData parkingData = new ParkingData(onstreetSpots, offstreetSpots);
				parkingMap.put(id, parkingData);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			System.out.println("Error: Invalid number format.");
		}

		return parkingMap;
	}

	// Helper method to handle NA and parse the number
	private static int parseSpotValue(String value) {
		if (value.equalsIgnoreCase("NA")) {
			return 0; // Treat "NA" as zero
		}
		return Integer.parseInt(value); // Otherwise, parse the integer
	}


}
