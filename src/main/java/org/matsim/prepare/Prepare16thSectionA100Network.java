package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

@CommandLine.Command(
	name = "network-16h-section-a100",
	description = "add 16th section of A100 to network."
)
public class Prepare16thSectionA100Network implements MATSimAppCommand {
	Logger log = LogManager.getLogger(Prepare16thSectionA100Network.class);

	@CommandLine.Option(names = "--network", description = "Path to network file", required = true)
	private String networkFile;
	@CommandLine.Option(names = "--output", description = "Output path of the prepared network", required = true)
	private String outputPath;

	public static void main(String[] args) {
		new Prepare16thSectionA100Network().execute(args);
	}


	@Override
	public Integer call() throws Exception {
		Network network = NetworkUtils.readNetwork(networkFile);

		// add 16th section of A100 to network
		add16thSectionA100Links(network);
		NetworkUtils.writeNetwork(network, outputPath);

		log.info("Network with additional 16th section of A100 written to {}", outputPath);

		return 0;
	}

	public static void add16thSectionA100Links(Network network) {

		// Connection to A100
		Node southTowardsSouth = network.getNodes().get(Id.createNodeId(27542427));
		Node southTowardsNorth = network.getNodes().get(Id.createNodeId(596372794));

		// Grenzallee



		// Sonnenallee
		// for Sonnenallee we need to cut the links, such that there is 1 node in the center of the street where all links arrive and depart:
		// all links from/to A100
		// all "normal links"

		// Treptower Park

//		Node northTowardsSouth = network.getNodes().get(Id.createNodeId());
//		Node northTowardsNorth = network.getNodes().get(Id.createNodeId());


	}
}
