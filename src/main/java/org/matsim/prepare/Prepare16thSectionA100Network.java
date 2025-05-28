package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.Set;

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


		// Following links are examples, found around Britzer Damm, from which we copy the attributes:
		//TODO: check whether this examples are sensible
		Link exampleAutobahnLink = network.getLinks().get(Id.createLinkId("253770361#0")); 		// 3 lanes -- 22.225 free speed -- 5703.0 capacity
		Link exampleConnectorToA100 = network.getLinks().get(Id.createLinkId("24159735#0")); 	// 2 lanes -- 14.465 free speed -- 3556.0 capacity
		Link exampleConnectorFromA100 = network.getLinks().get(Id.createLinkId("5095581#0")); 	// 3 lanes --  8.844 free speed -- 2667.0 capacity


		// PART 1: DEFINE/GET ALL NECESSARY NODES ( Neukölln --> Treptower Park)



		// Neukoelln:
		Node a100NeukoellnClock = network.getNodes().get(Id.createNodeId(27542427));
		Node a100NeukoellnCounterclock = network.getNodes().get(Id.createNodeId(596372794));

		// Sonnenallee: new nodes on a100 (southern and northern points) + get connector to Sonnenallee
		Node a100SonnenalleeSouth = NetworkUtils.createAndAddNode(network, Id.createNodeId("a100SonnenAlleeSouth"), new Coord(802865.39,5822553.50));
		Node a100SonnenalleeNorth = NetworkUtils.createAndAddNode(network, Id.createNodeId("a100SonnenAlleeNorth"), new Coord(803009.62, 5823121.89));
		Node connectorSonnenallee = network.getNodes().get(Id.createNodeId("3386901044"));

		//	Treptower Park: new node on a100 to connect to Treptower Park this node is NOT yet needed, but we implement it to prepare a potential connection of BA17
		Node a100TreptowerParkSouth = NetworkUtils.createAndAddNode(network, Id.createNodeId("a100TreptowerParkSouth"), new Coord(802650.11,5824819.05));
		//		we use the existing node with id 287932781 to connect the existing street network to a100
		Node connectorTreptowerPark = network.getNodes().get(Id.createNodeId(287932781));



		// PART 2: LINKS
		// a) From NK Dreieck to Sonnenallee
		copyLinkAttributesAndAddToNetwork(network, "a100NeukoellnCounterClockToA100SonnenalleeSouth", a100NeukoellnCounterclock, a100SonnenalleeSouth, exampleAutobahnLink);
		copyLinkAttributesAndAddToNetwork(network, "a100SonnenalleeSouthToNeukoellnClock", a100SonnenalleeSouth, a100NeukoellnClock, exampleAutobahnLink);


		// b) Grenzallee - kreuzungsfrei, not needed

		// c) Sonnenallee
		// for Sonnenallee we need to cut link 497789990#0 such that there is 1 node in the center of the street (existing node 3386901044) where all connector links arrive and depart:
		Link sonnenalleeOld = network.getLinks().get(Id.createLinkId("497789990#0"));

		copyLinkAttributesAndAddToNetwork(network, "497789990_right", sonnenalleeOld.getFromNode(), connectorSonnenallee, sonnenalleeOld);
		copyLinkAttributesAndAddToNetwork(network, "497789990_left", connectorSonnenallee, sonnenalleeOld.getToNode(), sonnenalleeOld);

//		also remove the old sonnenallee link, which we just cut
		network.removeLink(sonnenalleeOld.getId());

//		connect a100 sonnenallee south to sonnenallee
		copyLinkAttributesAndAddToNetwork(network, "a100SonnenalleeSouthToConnectorSonnenallee", a100SonnenalleeSouth, connectorSonnenallee, exampleConnectorFromA100);
		copyLinkAttributesAndAddToNetwork(network, "connectorSonnenalleeToA100SonnenalleeSouth", connectorSonnenallee, a100SonnenalleeSouth, exampleConnectorToA100);

//		connect a100 sonnenallee south to a100 sonnenallee north
		copyLinkAttributesAndAddToNetwork(network, "a100SonnenalleeSouthToA100SonnenalleeNorth", a100SonnenalleeSouth, a100SonnenalleeNorth, exampleAutobahnLink);
		copyLinkAttributesAndAddToNetwork(network, "a100SonnenalleeNorthToA100SonnenalleeSouth", a100SonnenalleeNorth, a100SonnenalleeSouth, exampleAutobahnLink);

//		connect connector sonnenallee to a100 sonnenallee north
		copyLinkAttributesAndAddToNetwork(network, "connectorSonnenalleeToA100SonnenalleeNorth", connectorSonnenallee, a100SonnenalleeNorth, exampleConnectorToA100);
		copyLinkAttributesAndAddToNetwork(network, "a100SonnenalleeNorthToConnectorSonnenallee", a100SonnenalleeNorth, connectorSonnenallee, exampleConnectorFromA100);


		// Treptower Park
		//		create a100 between treptower park and sonnenallee north
		copyLinkAttributesAndAddToNetwork(network, "a100TreptowerParkSouthToA100SonnenAlleeNorth", a100TreptowerParkSouth, a100SonnenalleeNorth, exampleAutobahnLink);
		copyLinkAttributesAndAddToNetwork(network, "a100SonnenAlleeNorthToA100TreptowerParkSouth", a100SonnenalleeNorth, a100TreptowerParkSouth, exampleAutobahnLink);

		//		connect a100 to treptower park
		copyLinkAttributesAndAddToNetwork(network, "a100TreptowerParkSouthToConnectorTreptowerPark", a100TreptowerParkSouth, connectorTreptowerPark, exampleConnectorFromA100);
		copyLinkAttributesAndAddToNetwork(network, "connectorTreptowerParkToA100TreptowerParkSouth", connectorTreptowerPark, a100TreptowerParkSouth, exampleConnectorToA100);


		// we also have to add an extra link between this node towards elsenstr.
		Link towardsElsenStr = network.getLinks().get(Id.createLinkId("69166216#0"));
		copyLinkAttributesAndAddToNetwork(network, "-69166216#0", towardsElsenStr.getToNode(), towardsElsenStr.getFromNode(), towardsElsenStr);

		new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.car, "freight", TransportMode.ride, TransportMode.truck));
	}

	private static void copyLinkAttributesAndAddToNetwork(Network network, String linkId, Node fromNode, Node toNode, Link originalLink) {
		double length = NetworkUtils.getEuclideanDistance(fromNode.getCoord(), toNode.getCoord());
		//TODO cut signficant digits on length
		Link newLink = NetworkUtils.createAndAddLink(network, Id.createLinkId(linkId), fromNode, toNode, length, originalLink.getFreespeed(), originalLink.getCapacity(), originalLink.getNumberOfLanes());
		newLink.setAllowedModes(originalLink.getAllowedModes());

		newLink.getAttributes().putAttribute("allowed_speed", originalLink.getAttributes().getAttribute("allowed_speed"));
		newLink.getAttributes().putAttribute("speed_factor", originalLink.getAttributes().getAttribute("speed_factor"));
		newLink.getAttributes().putAttribute("type", originalLink.getAttributes().getAttribute("type"));
//		newLink.getAttributes().putAttribute("restricted_lanes", originalLink.getAttributes().getAttribute("restricted_lanes"));
	}
}
