package org.matsim.run.scoring;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vtts.VTTSHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.vtts.VTTSHandler.VTTSCalculationMethod.noIncomeDependentScoring;

public class TestVTTSScoring {

	@Test
	public void testWithNoIncome() {
		String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
		//Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputPath + "network.xml");
		config.controller().setOutputDirectory("output/VTTSTest/");
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.scoring().setExplainScores(true);
		config.scoring().setWriteExperiencedPlans(true);
		config.scoring().getOrCreateModeParams(TransportMode.car).setMarginalUtilityOfTraveling(0);

		ScoringConfigGroup.ActivityParams paramsWork = new ScoringConfigGroup.ActivityParams();
		paramsWork.setTypicalDuration(10);
		paramsWork.setActivityType("work");

		ScoringConfigGroup.ActivityParams paramsHome = new ScoringConfigGroup.ActivityParams();
		paramsHome.setActivityType("home");
		paramsHome.setTypicalDuration(10);
		config.scoring().addActivityParams(paramsHome);
		config.scoring().addActivityParams(paramsWork);
		config.scoring().getScoringParameters(null).setLateArrival_utils_hr(0.);
		config.scoring().getScoringParameters(null).setMarginalUtlOfWaiting_utils_hr(0.);
		config.scoring().getScoringParameters(null).setMarginalUtlOfWaitingPt_utils_hr(0.);
		config.scoring().getScoringParameters(null).setEarlyDeparture_utils_hr(0.);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		createTestPopulation(scenario);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.addEventHandlerBinding().toInstance( new VTTSHandler( new String[]{"freight"} ) );
//				this.addEventHandlerBinding().to( VTTSHandler.class ); // this would be better
			}
		} );

		controler.run();

		VTTSHandler vttsHandler = controler.getInjector().getInstance( VTTSHandler.class );

		vttsHandler.computeFinalVTTS();
		vttsHandler.printVTTS(controler.getConfig().controller().getOutputDirectory()+"vtts.csv");
		/*
		this is the average between the work activity which is exact the typical duration --> 5.718610788259497
		and the home activity which is a lot longer then the typical duration --> 7.215831535289929E-4
		*/
        assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("perfectDuration"))).isEqualTo(2.859666185706513);

		/*
		this is the average between the work activity which is below the typical duration --> 10.939293407637269 --> higher than the default of 6
		and the home activity which is a lot longer then the typical duration --> 7.215397658355549E-4
		*/
		assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("timePressure"))).isEqualTo(22.309690970751944);
	}

	private static void createTestPopulation(Scenario scenario) {
		Population population = scenario.getPopulation();
		Person person = population.getFactory().createPerson(Id.createPersonId("perfectDuration"));
		Plan plan = population.getFactory().createPlan();
		Activity activityHome = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.createLinkId("1"));
		activityHome.setEndTime(10.0);
		Activity activityWork = scenario.getPopulation().getFactory().createActivityFromLinkId("work", Id.createLinkId("6"));
		activityWork.setEndTime(380.0);
		Activity activityHome2 = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.createLinkId("1"));
		activityHome2.setEndTime(9000.0);

		plan.addActivity(activityHome);
		//360 seconds travel time
		plan.addLeg(population.getFactory().createLeg(TransportMode.car));
		plan.addActivity(activityWork);
		plan.addLeg(population.getFactory().createLeg(TransportMode.car));
		plan.addActivity(activityHome2);
		person.addPlan(plan);
		population.addPerson(person);

		Person personThatIsUnderTimePressure = population.getFactory().createPerson(Id.createPersonId("timePressure"));
		Plan planForTimePressureAgent = population.getFactory().createPlan();
		Activity activityHomeStartLater = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.createLinkId("1"));
		activityHomeStartLater.setEndTime(30.0);
		//travel time still 360 seconds --> agent performs activity only for 5 secs --> higher vtts
		Activity activityWorkStartLater = scenario.getPopulation().getFactory().createActivityFromLinkId("work", Id.createLinkId("6"));
		activityWorkStartLater.setEndTime(395.0);
		planForTimePressureAgent.addActivity(activityHomeStartLater);
		planForTimePressureAgent.addLeg(population.getFactory().createLeg(TransportMode.car));
		planForTimePressureAgent.addActivity(activityWorkStartLater);
		planForTimePressureAgent.addLeg(population.getFactory().createLeg(TransportMode.car));
		planForTimePressureAgent.addActivity(activityHome2);
		personThatIsUnderTimePressure.addPlan(planForTimePressureAgent);
		population.addPerson(personThatIsUnderTimePressure);
	}


}
