package org.matsim.run.scoring;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vtts.VTTSHandler;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;


/*
public class TestVTTS {

	@Test
	public void testWithNoIncome() {
		String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
		Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
		config.controller().setLastIteration(1);
		config.controller().setOutputDirectory("output/VTTSTest/");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		for( ScoringConfigGroup.ActivityParams activityParam : config.scoring().getActivityParams() ){
			if (activityParam.getActivityType().contains("w")) {
				activityParam.setTypicalDuration(20.0);
			}
			if (activityParam.getActivityType().contains("h")) {
			}

		}




		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		VTTSHandler vttsHandler= new VTTSHandler(scenario, new String[]{"freight"}, "staging", noIncomeDependentScoring);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(vttsHandler);
			}
		});
		controler.run();
		vttsHandler.computeFinalVTTS();
		vttsHandler.printVTTS(controler.getConfig().controller().getOutputDirectory()+"vtts.csv");
		assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("1"))).isEqualTo(6.0);
		assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("6"))).isEqualTo(22.309690970751944);
	}

	@Test
	public void testWithIncome() {
		String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
		Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
		config.controller().setLastIteration(1);
		config.controller().setOutputDirectory("output/VTTSTestWithIncome/");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		for (Person person: scenario.getPopulation().getPersons().values()) {
			double income = 10.0 + Double.parseDouble(person.getId().toString());
			PersonUtils.setIncome(person, income);
		}
		Controler controler = new Controler(scenario);
		VTTSHandler vttsHandler= new VTTSHandler(scenario, new String[]{"freight"}, "staging", VTTSHandler.VTTSCalculationMethod.incomeDependentScoring);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(vttsHandler);
				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();
			}
		});
		controler.run();
		vttsHandler.printVTTS(controler.getConfig().controller().getOutputDirectory()+"vtts.csv");
		assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("1"))).isEqualTo(4.258064516129031);
		assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("6"))).isEqualTo(23.029358421421364);
	}






}*/
