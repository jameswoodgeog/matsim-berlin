package org.matsim.run.scoring.parking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZeroParkingCapacityInitializerTest {
	@RegisterExtension
	MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testInitialize() {
		Network network = getNetwork();

		ZeroParkingCapacityInitializer zeroParkingCapacityInitializer = new ZeroParkingCapacityInitializer(network);
		Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialize = zeroParkingCapacityInitializer.initialize(List.of(), List.of());
		for (ParkingCapacityInitializer.ParkingInitialCapacity value : initialize.values()) {
			assertEquals(0, value.initial());
		}
	}

	private Network getNetwork() {
		// Chessboard network has 10x10 nodes, starting from (0,0) to (9000,9000). The distance between two adjacent nodes is 1000 on the x- or y-axis.
		return NetworkUtils.readNetwork(testUtils.getPackageInputDirectory() + "./chessboard_network.xml");
	}
}
