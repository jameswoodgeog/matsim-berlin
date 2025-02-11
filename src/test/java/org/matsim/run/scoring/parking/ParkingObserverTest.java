package org.matsim.run.scoring.parking;

import com.google.inject.Injector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonScoreEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.modechoice.estimators.ActivityEstimator;
import org.matsim.modechoice.estimators.DefaultActivityEstimator;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;

class ParkingObserverTest {
	@RegisterExtension
	MatsimTestUtils testUtils = new MatsimTestUtils();

	private Injector injector;

	@BeforeEach
	void setUp() {
		injector = null;
	}

	@Test
	void testScoreEvents_sameLink() {
		List<PersonScoreEvent> expectedScoreEvents = List.of(
			new PersonScoreEvent(0, Id.createPersonId("p1"), 0.0, "parking"),
			new PersonScoreEvent(10, Id.createPersonId("p2"), 0.0, "parking")
		);
		EventsManager eventsManager = prepareParkingObserver(expectedScoreEvents, true);

		// Person p1 parks at link 55.
		eventsManager.processEvent(new VehicleLeavesTrafficEvent(0, Id.createPersonId("p1"), Id.createLinkId("55"), Id.createVehicleId("v1"), "car", 0));
		// Person p2 parks at link 55.
		eventsManager.processEvent(new VehicleLeavesTrafficEvent(10, Id.createPersonId("p2"), Id.createLinkId("55"), Id.createVehicleId("v2"), "car", 0));
	}

	@Test
	void testScoreEvents_sameLink_OneVehicleLeft() {
		List<PersonScoreEvent> expectedScoreEvents = List.of(
			new PersonScoreEvent(0, Id.createPersonId("p1"), 0.0, "parking"),
			new PersonScoreEvent(10, Id.createPersonId("p2"), 0.0, "parking") //TODO change amount
		);
		EventsManager eventsManager = prepareParkingObserver(expectedScoreEvents, true);

		// Person p1 parks at link 55.
		eventsManager.processEvent(new VehicleLeavesTrafficEvent(0, Id.createPersonId("p1"), Id.createLinkId("55"), Id.createVehicleId("v1"), "car", 0));
		// Person p1 unparks at link 55.
		eventsManager.processEvent(new VehicleEntersTrafficEvent(5, Id.createPersonId("p1"), Id.createLinkId("55"), Id.createVehicleId("v1"), "car", 0));
		// Person p2 parks at link 55.
		eventsManager.processEvent(new VehicleLeavesTrafficEvent(10, Id.createPersonId("p2"), Id.createLinkId("55"), Id.createVehicleId("v2"), "car", 0));
	}

	@Test
	void testScoreEvents_onlyOffStreetSpots() {
		List<PersonScoreEvent> expectedScoreEvents = List.of(
			new PersonScoreEvent(0, Id.createPersonId("p1"), 0.0, "parking"),
			new PersonScoreEvent(10, Id.createPersonId("p2"), 0.0, "parking")
		);
		EventsManager eventsManager = prepareParkingObserver(expectedScoreEvents, false);

		// Person p1 parks at link 55.
		eventsManager.processEvent(new VehicleLeavesTrafficEvent(0, Id.createPersonId("p1"), Id.createLinkId("55"), Id.createVehicleId("v1"), "car", 0));
		// Person p2 parks at link 55.
		eventsManager.processEvent(new VehicleLeavesTrafficEvent(10, Id.createPersonId("p2"), Id.createLinkId("55"), Id.createVehicleId("v2"), "car", 0));
	}

	EventsManager prepareParkingObserver(List<PersonScoreEvent> expectedScoreEvents, boolean offStreet) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(testUtils.getPackageInputDirectory() + "chessboard_network.xml");
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		prepareNetwork(scenario.getNetwork(), offStreet);

		Controller controller = new Controler(scenario);
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// bind classes from informed mode choice explicitly
				bind(ActivityEstimator.class).to(DefaultActivityEstimator.class);
				bind(LegEstimator.class).to(DefaultLegScoreEstimator.class);

				// bind parking classes
				bind(KernelFunction.class).to(ConstantKernelFunction.class);
				bind(PenaltyFunction.class).toInstance(new BellochePenaltyFunction(0.4, -6));
				bind(ParkingCapacityInitializer.class).to(ZeroParkingCapacityInitializer.class);
				bind(ParkingObserver.class);
				addEventHandlerBinding().to(ParkingObserver.class);

				// bind test event handler
				addEventHandlerBinding().toInstance(new TestHandler(expectedScoreEvents));
			}
		});

		injector = controller.getInjector();
		return injector.getInstance(EventsManager.class);
	}

	private void prepareNetwork(Network network, boolean offStreet) {
		network.getLinks().values().forEach(l -> {
			l.getAttributes().putAttribute(ParkingObserver.LINK_ON_STREET_SPOTS, 1);
			if (offStreet) {
				l.getAttributes().putAttribute(ParkingObserver.LINK_OFF_STREET_SPOTS, 1);
			}
		});
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
