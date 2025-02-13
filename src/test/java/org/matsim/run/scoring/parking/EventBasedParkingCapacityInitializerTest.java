package org.matsim.run.scoring.parking;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class EventBasedParkingCapacityInitializerTest {
	@RegisterExtension
	MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testEmpty() {
		Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialize = getParkingCapacityInitializer().initialize(List.of(), List.of());

		Assertions.assertEquals(180, initialize.size());
		for (int i = 1; i <= 180; i++) {
			Assertions.assertEquals(i, initialize.get(Id.createLinkId(String.valueOf(i))).capacity());
			Assertions.assertEquals(0, initialize.get(Id.createLinkId(String.valueOf(i))).initial());
		}
	}

	@Test
	void testFullInitial() {
		List<VehicleEntersTrafficEvent> enterEvents = new ArrayList<>();
		//For all links: On link i, register i vehicle enters traffic events with different person ids.
		for (int i = 1; i <= 180; i++) {
			for (int j = 0; j < i; j++) {
				enterEvents.add(new VehicleEntersTrafficEvent(i + j, Id.createPersonId("p" + i + "," + j), Id.createLinkId(String.valueOf(i)), null, TransportMode.car, 0));
			}
		}

		Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialize = getParkingCapacityInitializer().initialize(enterEvents, List.of());

		Assertions.assertEquals(180, initialize.size());
		for (int i = 1; i <= 180; i++) {
			Assertions.assertEquals(i, initialize.get(Id.createLinkId(String.valueOf(i))).capacity());
			Assertions.assertEquals(i, initialize.get(Id.createLinkId(String.valueOf(i))).initial());
		}
	}

	@Test
	void testSamePersonEntersTraffic() {
		List<VehicleEntersTrafficEvent> enterEvents = new ArrayList<>();

		//even though two enter events are registered, only one should be counted because the person is the same.
		enterEvents.add(new VehicleEntersTrafficEvent(0, Id.createPersonId("testPerson"), Id.createLinkId(1), null, TransportMode.car, 0));
		enterEvents.add(new VehicleEntersTrafficEvent(10, Id.createPersonId("testPerson"), Id.createLinkId(2), null, TransportMode.car, 0));

		Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialize = getParkingCapacityInitializer().initialize(enterEvents, List.of());

		Assertions.assertEquals(180, initialize.size());
		Assertions.assertEquals(1, initialize.get(Id.createLinkId("1")).capacity());
		Assertions.assertEquals(1, initialize.get(Id.createLinkId("1")).initial());

		//checks in particular that on link 2 no initial parking is registered
		for (int i = 2; i <= 180; i++) {
			Assertions.assertEquals(i, initialize.get(Id.createLinkId(String.valueOf(i))).capacity());
			Assertions.assertEquals(0, initialize.get(Id.createLinkId(String.valueOf(i))).initial());
		}
	}

	@Test
	void testMoreInitialThanCapacity() {
		List<VehicleEntersTrafficEvent> enterEvents = new ArrayList<>();

		//Register 2 parking cars on link 1. We allow over subscription of parking spots.
		enterEvents.add(new VehicleEntersTrafficEvent(0, Id.createPersonId("p1"), Id.createLinkId(1), null, TransportMode.car, 0));
		enterEvents.add(new VehicleEntersTrafficEvent(0, Id.createPersonId("p2"), Id.createLinkId(1), null, TransportMode.car, 0));

		Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialize = getParkingCapacityInitializer().initialize(enterEvents, List.of());

		Assertions.assertEquals(180, initialize.size());
		Assertions.assertEquals(1, initialize.get(Id.createLinkId("1")).capacity());
		Assertions.assertEquals(2, initialize.get(Id.createLinkId("1")).initial());

		for (int i = 2; i <= 180; i++) {
			Assertions.assertEquals(i, initialize.get(Id.createLinkId(String.valueOf(i))).capacity());
			Assertions.assertEquals(0, initialize.get(Id.createLinkId(String.valueOf(i))).initial());
		}
	}

	private ParkingCapacityInitializer getParkingCapacityInitializer() {
		Injector injector = Guice.createInjector(
			new AbstractModule() {
				@Override
				protected void configure() {
					bind(Network.class).toInstance(getNetwork());
					bind(ParkingCapacityInitializer.class).to(EventBasedParkingCapacityInitializer.class);
				}
			});

		ParkingCapacityInitializer initializer = injector.getInstance(ParkingCapacityInitializer.class);
		return initializer;
	}

	private Network getNetwork() {
		// Chessboard network has 10x10 nodes, starting from (0,0) to (9000,9000). The distance between two adjacent nodes is 1000 on the x- or y-axis.
		Network network = NetworkUtils.readNetwork(testUtils.getPackageInputDirectory() + "./chessboard_network.xml");
		network.getLinks().values().forEach(l -> {
			l.getAttributes().putAttribute(ParkingTimeEstimator.LINK_ON_STREET_SPOTS, Integer.valueOf(l.getId().toString()));
			l.getAttributes().putAttribute(ParkingTimeEstimator.LINK_OFF_STREET_SPOTS, 0);
		});

		return network;
	}

}
