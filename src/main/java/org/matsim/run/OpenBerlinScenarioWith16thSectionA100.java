package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.prepare.Prepare16thSectionA100Network;

public class OpenBerlinScenarioWith16thSectionA100 extends OpenBerlinScenario {


	@Override
	public Config prepareConfig(Config config) {
		super.prepareConfig(config);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		super.prepareScenario(scenario);
		// add hbefa link attributes.
		// HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
		// roadTypeMapping.addHbefaMappings(scenario.getNetwork());

		// add 16th section of A100 to network
		Prepare16thSectionA100Network.add16thSectionA100Links(scenario.getNetwork());

	}





	}
