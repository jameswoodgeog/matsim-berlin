package org.matsim.run.scoring.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A kernel function that assigns a constant weight to all links within a certain distance. Takes the middle of the link as the center of the disk.
 */
public class ConstantKernelFunction implements KernelFunction {
	QuadTree<Id<Link>> quadTree;

	public ConstantKernelFunction(Network network) {
		double xMin = network.getNodes().values().parallelStream().mapToDouble(node -> node.getCoord().getX()).min().orElse(Double.POSITIVE_INFINITY);
		double yMin = network.getNodes().values().parallelStream().mapToDouble(node -> node.getCoord().getY()).min().orElse(Double.POSITIVE_INFINITY);
		double xMax = network.getNodes().values().parallelStream().mapToDouble(node -> node.getCoord().getX()).max().orElse(Double.NEGATIVE_INFINITY);
		double yMax = network.getNodes().values().parallelStream().mapToDouble(node -> node.getCoord().getY()).max().orElse(Double.NEGATIVE_INFINITY);

		quadTree = new QuadTree<>(xMin, yMin, xMax, yMax);

		network.getLinks().values().forEach(link -> {
			quadTree.put(link.getCoord().getX(), link.getCoord().getY(), link.getId());
		});
	}

	@Override
	public Map<Id<Link>, Double> calculateKernel(Link link, double distance) {
		return quadTree.getDisk(link.getCoord().getX(), link.getCoord().getY(), distance).stream().collect(Collectors.toMap(id -> id, id -> 1.0));
	}
}
