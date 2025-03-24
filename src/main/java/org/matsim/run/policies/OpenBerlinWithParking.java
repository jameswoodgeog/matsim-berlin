package org.matsim.run.policies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.run.OpenBerlinScenario;

/*
This class extends the matsim berlin scenario by parking functionality
 */


public class OpenBerlinWithParking extends OpenBerlinScenario {



	@Override
	protected Config prepareConfig(Config config) {
		return super.prepareConfig(config);
	}


	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
	}


	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);
	}

}
