package org.matsim.run.scoring.parking;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonScoreEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;

class ParkingObserverTest {
	@RegisterExtension
	MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * what should be tested: full parking
	 */

	@Test
	void testScoreEvents_sameLink() {
		List<PersonScoreEvent> expectedScoreEvents = List.of(
			new PersonScoreEvent(0, Id.createPersonId("p1"), 0.0, "parking"),
			new PersonScoreEvent(10, Id.createPersonId("p2"), 0.0, "parking")
		);
		EventsManagerImpl eventsManager = prepareParkingObserver(expectedScoreEvents);

		// Person p1 parks at link 55.
		eventsManager.processEvent(new VehicleLeavesTrafficEvent(0, Id.createPersonId("p1"), Id.createLinkId("55"), Id.createVehicleId("v1"), "car", 0));
		// Person p2 parks at link 55.
		eventsManager.processEvent(new VehicleLeavesTrafficEvent(10, Id.createPersonId("p2"), Id.createLinkId("55"), Id.createVehicleId("v2"), "car", 0));
	}

	@Test
	void testScoreEvents_sameLink_vehicleLeft() {
		List<PersonScoreEvent> expectedScoreEvents = List.of(
			new PersonScoreEvent(0, Id.createPersonId("p1"), 0.0, "parking"),
			new PersonScoreEvent(10, Id.createPersonId("p2"), 0.0, "parking") //TODO change amount
		);
		EventsManagerImpl eventsManager = prepareParkingObserver(expectedScoreEvents);

		// Person p1 parks at link 55.
		eventsManager.processEvent(new VehicleLeavesTrafficEvent(0, Id.createPersonId("p1"), Id.createLinkId("55"), Id.createVehicleId("v1"), "car", 0));
		// Person p1 unparks at link 55.
		eventsManager.processEvent(new VehicleEntersTrafficEvent(5, Id.createPersonId("p1"), Id.createLinkId("55"), Id.createVehicleId("v1"), "car", 0));
		// Person p2 parks at link 55.
		eventsManager.processEvent(new VehicleLeavesTrafficEvent(10, Id.createPersonId("p2"), Id.createLinkId("55"), Id.createVehicleId("v2"), "car", 0));
	}

	@NotNull
	private EventsManagerImpl prepareParkingObserver(List<PersonScoreEvent> expectedScoreEvents) {
		Network network = loadAndPrepareNetwork();
		KernelFunction kernelFunction = new ConstantKernelFunction(network);
		PenaltyFunction penaltyFunction = new BellochePenaltyFunction(0.4, -6);
		EventsManagerImpl eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(new TestHandler(expectedScoreEvents));
		ParkingObserver parkingObserver = new ParkingObserver(network, kernelFunction, penaltyFunction, eventsManager);
		eventsManager.addHandler(parkingObserver);
		return eventsManager;
	}

	private Network loadAndPrepareNetwork() {
		Network network = NetworkUtils.readNetwork(testUtils.getPackageInputDirectory() + "chessboard_network.xml");
		network.getLinks().values().forEach(l -> {
			l.getAttributes().putAttribute(ParkingObserver.LINK_ON_STREET_SPOTS, 1);
			l.getAttributes().putAttribute(ParkingObserver.LINK_OFF_STREET_SPOTS, 1);
		});
		return network;
	}

	private static class TestHandler implements PersonScoreEventHandler {
		private final List<PersonScoreEvent> expectedEvents;
		private int eventCounter = 0;

		public TestHandler(List<PersonScoreEvent> expectedEvents) {
			this.expectedEvents = expectedEvents;
		}

		@Override
		public void handleEvent(PersonScoreEvent personScoreEvent) {
			PersonScoreEvent expectedEvent = expectedEvents.get(eventCounter);
			Assertions.assertEquals(expectedEvent, personScoreEvent);
			eventCounter++;
		}
	}
}
