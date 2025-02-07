package org.matsim.run.scoring.parking;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class ConstantKernelFunctionTest {
	@RegisterExtension
	MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void calculateKernel_oneInDisk() {
		// Chessboard network has 10x10 nodes, starting from (0,0) to (9000,9000). The distance between two adjacent nodes is 1000 on the x- or y-axis.
		Network network = getNetwork();
		Link link = getLink(network);
		KernelFunction kernel = new ConstantKernelFunction(network);

		//for distances lower than sqrt(2*500^2)=708, the kernel should return one link id with a weight of 1
		IntStream.of(0, 100, 707).forEach(distance -> {
			Map<Id<Link>, Double> idDoubleMap = kernel.calculateKernel(link, distance);
			Assertions.assertEquals(1, idDoubleMap.size());
			Assertions.assertEquals(1.0, idDoubleMap.get(Id.createLinkId("158")));
		});
	}

	@Test
	void calculateKernel_moreInDisk() {
		Network network = getNetwork();
		Link link = getLink(network);
		KernelFunction kernel = new ConstantKernelFunction(network);

		//for distances between 708 and less than 1000, the kernel returns 5 link ids with a weight of 1
		IntStream.of(708, 999).forEach(distance -> {
			Map<Id<Link>, Double> idDoubleMap = kernel.calculateKernel(link, distance);
			Assertions.assertEquals(5, idDoubleMap.size());
			Stream.of("23", "68", "24", "69", "158").forEach(id -> Assertions.assertEquals(1.0, idDoubleMap.get(Id.createLinkId(id))));
		});
	}

	@Test
	void calculateKernel_border() {
		Network network = getNetwork();
		Link link = network.getLinks().get(Id.createLinkId("1"));
		KernelFunction kernel = new ConstantKernelFunction(network);

		//for distances between 708 and less than 1000, the kernel returns 3 link ids with a weight of 1
		IntStream.of(708, 999).forEach(distance -> {
			Map<Id<Link>, Double> idDoubleMap = kernel.calculateKernel(link, distance);
			Assertions.assertEquals(3, idDoubleMap.size());
			Stream.of("1", "136", "91").forEach(id -> Assertions.assertEquals(1.0, idDoubleMap.get(Id.createLinkId(id))));
		});
	}

	private Network getNetwork() {
		// Chessboard network has 10x10 nodes, starting from (0,0) to (9000,9000). The distance between two adjacent nodes is 1000 on the x- or y-axis.
		return NetworkUtils.readNetwork(testUtils.getPackageInputDirectory() + "./chessboard_network.xml");
	}

	/**
	 * @formatter:off
	 *  (5000,5500)
	 *     |
	 *     |
	 *     55 - - - (5500,5000) - - - 65
	 *     |
	 *     |
	 *  (5000,4500) <- this is the middle of the link
	 *     |
	 *     |
	 *     56 - - - (5500,4000)
	 *     |
	 *     |
	 *  (5000, 3500)
	 *  @formatter:on
	 */
	@NotNull
	private static Link getLink(Network network) {
		// (5000, 4000)
		Node node55 = network.getNodes().get(Id.createNodeId("55"));
		// (5000, 5000)
		Node node56 = network.getNodes().get(Id.createNodeId("56"));

		// create link between both nodes. Coord of link is then (5000,4500)
		// the original link id in the chessboard network is 158
		return NetworkUtils.createLink(Id.createLinkId("dummy_158"), node55, node56, network, 1000, 0, 0, 0);
	}
}
