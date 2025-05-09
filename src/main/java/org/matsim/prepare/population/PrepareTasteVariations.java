package org.matsim.prepare.population;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import picocli.CommandLine;

import java.util.Map;

@CommandLine.Command(
	name = "taste-variations",
	description = "Prepare taste variations for population.",
	mixinStandardHelpOptions = true
)
public class PrepareTasteVariations implements MATSimAppCommand, PersonAlgorithm {

	@CommandLine.Option(names = "--input", description = "Input population file.", required = true)
	private String input;

	@CommandLine.Option(names = "--output", description = "Output population file.", required = true)
	private String output;

	public static void main(String[] args) {
		new PrepareTasteVariations().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input);

		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), this);

		PopulationUtils.writePopulation(population, output);

		return 0;
	}

	@Override
	public void run(Person person) {

		Map<String, Map<ModeUtilityParameters.Type, Double>> variations = PersonUtils.getModeTasteVariations(person);

		if (variations != null) {
			// This is stored as variation during calibration. For the final version it is removed because it is constant
			variations.keySet().removeIf(mode -> mode.equals("bus"));

			PersonUtils.setModeTasteVariations(person, variations);
		}

	}
}
