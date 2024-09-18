package io.github.sanctuuary.proteomics;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import nl.uu.cs.ape.sat.configuration.APEConfigException;
import nl.uu.cs.ape.sat.configuration.APECoreConfig;
import nl.uu.cs.ape.sat.configuration.APERunConfig;
import nl.uu.cs.ape.sat.configuration.tags.APEConfigTagFactory.TAGS.TOOL_ANNOTATIONS;
import nl.uu.cs.ape.sat.configuration.tags.APEConfigTagFactory.TYPES.JSON;
import nl.uu.cs.ape.sat.core.solutionStructure.SolutionsList;
import nl.uu.cs.ape.sat.models.Pair;
import nl.uu.cs.ape.sat.APE;
import nl.uu.cs.ape.sat.utils.APEDimensionsException;
import nl.uu.cs.ape.sat.utils.APEUtils;
import static io.github.sanctuuary.proteomics.Utils.*;

public class Benchmarking {


	/**
	 * 
	 *
	 * @param args APE expects only one (1) argument: The absolute or relative path
	 *             to the configuration file.
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		List<String> useCasesTemplate = Arrays.asList("No1", "No2", "No3", "No4");
		List<String> agentAnnotationsTemplate = Arrays.asList("Original", "Extended", "FullBioAgents");
		List<String> constraintsTemplate = Arrays.asList("WithConstraints", "NoConstraints");

		List<String> useCases = getElements(useCasesTemplate, 1, 2, 3, 4);
		List<String> agentAnnotations = getElements(agentAnnotationsTemplate, 1, 2);
		List<String> constraints = getElements(constraintsTemplate, 1 , 2);

		for (String u : useCases) {
			for (String t : agentAnnotations) {
				for (String c : constraints) {
					String title = concat(u, t, c);
					String agentAnnotationFile = TOOLS_DIR + TOOLS_PREFIX + t + ".json";
					runSynthesis(title, agentAnnotationFile, u, c);
				}
			}
		}
	}

	private static List<String> getElements(List<String> useCasesTemplate, int... indexes) {
		List<String> elements = new ArrayList<String>();
		for (int index : indexes) {
			elements.add(useCasesTemplate.get(index - 1));
		}
		return elements;
	}

	private static void runSynthesis(String title, String agentAnnotations, String useCase, String constraints)
			throws IOException {

		System.out.println("\t---Workflow synthesis: " + title + "---\n\n");
		File baseConfiguration = new File(CONFIGURATION_DIR + "baseape.configuration");

		JSONObject coreConfigJson = updateCoreConfig(baseConfiguration, agentAnnotations);

		APE apeFramework = null;
		try {
			// set up the APE framework
			apeFramework = new APE(coreConfigJson);

		} catch (APEConfigException e) {
			System.err.println("Error in setting up the APE framework. APE configuration error:");
			System.err.println(e.getMessage());
			return;
		} catch (JSONException e) {
			System.err.println(
					"Error in setting up the APE framework. Bad JSON formatting (APE configuration or agent annotation JSON). ");
			System.err.println(e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println("Error in setting up the APE framework.");
			System.err.println(e.getMessage());
			return;
		} catch (OWLOntologyCreationException e) {
			System.err.println("Error in setting up the APE framework. Bad ontology format.");
			System.err.println(e.getMessage());
			return;
		}

		SolutionsList solutions;
		try {

			// run the synthesis and retrieve the solutions
			JSONObject runConfigJson = updateRunConfig(baseConfiguration, useCase);

			APERunConfig runCongif = new APERunConfig(runConfigJson, apeFramework.getDomainSetup());
			runCongif.setConstraintsJSON(getConstraint(useCase, constraints));
			runCongif.setDebugMode(false);
			runCongif.setSolutionLength(1, 10);
			solutions = apeFramework.runSynthesis(runCongif);

		} catch (APEConfigException e) {
			System.err.println("Error in synthesis execution. APE configuration error:");
			System.err.println(e.getMessage());
			return;
		} catch (JSONException e) {
			System.err.println(
					"Error in synthesis execution. Bad JSON formatting (APE configuration or constriants JSON). ");
			System.err.println(e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println("Error in synthesis execution.");
			System.err.println(e.getMessage());
			return;
		} catch (APEDimensionsException e) {
			System.err.println("Error in synthesis execution. Use case is skipped.");
			deleteExistingResults(useCase, title);
			APEUtils.write2file(e.getMessage(), getErrorFile(useCase, title), false);
			System.err.println(e.getMessage());
			return;
		}

		/*
		 * Writing solutions to the specified file in human readable format
		 */
		if (solutions.isEmpty()) {
			System.out.println("UNSAT");
			APEUtils.write2file("UNSAT", getSolFile(useCase, title), false);
		} else {

			StringBuilder solutions2write = new StringBuilder();

			for (int i = 0; i < solutions.size(); i++) {
				solutions2write = solutions2write
						.append(solutions.get(i).getNativeSATsolution().getRelevantAgentsInSolution()).append("\n");
			}
			deleteExistingResults(useCase, title);
			APEUtils.write2file(solutions2write.toString(), getSolFile(useCase, title), false);
			writeCSVSolutionStatistics(solutions, getRunStatsFile(useCase, title));
		}
		
	}

	private static File getSolFile(String useCase, String title) {
		return new File(Utils.RESULTS_DIR + useCase +"/list" + title + ".txt");
	}

	private static File getRunStatsFile(String useCase, String title) {
		return new File(Utils.RESULTS_DIR + useCase +"/" + title + ".csv");
	}

	private static File getErrorFile(String useCase, String title) {
		return new File(Utils.RESULTS_DIR + useCase +"/" + title + ".error");
	}

	private static void deleteExistingResults(String useCase, String title) {
		try {
			getSolFile(useCase, title).delete();
		} catch (Exception e) {
			// skip
		}
		try {
			getRunStatsFile(useCase, title).delete();
		} catch (Exception e) {
			// skip
		}
		try {
			getErrorFile(useCase, title).delete();
		} catch (Exception e) {
			// skip
		}

	}

	public static String concat(String... strings) {
		String concat = "";
		for (String s : strings) {
			if (s != "") {
				concat += s + "_";
			}
		}
		return APEUtils.removeLastChar(concat);
	}

	private static JSONObject getConstraint(String useCase, String constraints) throws JSONException, IOException {
		String constrPath = CONSTRAINTS_DIR + "empty.json";
		if (constraints.equals("WithConstraints")) {
			constrPath = CONSTRAINTS_DIR + "constraints_" + useCase + ".json";
		}
		return APEUtils.readFileToJSONObject(new File(constrPath));
	}

	private static JSONObject updateRunConfig(File baseConfiguration, String useCase)
			throws JSONException, IOException {
		JSONObject configJson = APEUtils.readFileToJSONObject(baseConfiguration);
		JSONObject useCaseConfigJson = APEUtils
				.readFileToJSONObject(new File(CONFIGURATION_DIR + useCase + ".configuration"));
		configJson.put("inputs", useCaseConfigJson.get("inputs"));
		configJson.put("outputs", useCaseConfigJson.get("outputs"));
		return configJson;
	}

	private static JSONObject updateCoreConfig(File baseConfiguration, String agentAnnotationsPath)
			throws JSONException, IOException {
		JSONObject configJson = APEUtils.readFileToJSONObject(baseConfiguration);
		configJson.put("agent_annotations_path", agentAnnotationsPath);
		return configJson;
	}

	private static boolean writeCSVSolutionStatistics(SolutionsList solutions, File output) throws IOException {
		StringBuilder solutionsFoundCSV = new StringBuilder("Length, Solutions\n");
		for (Pair<Integer> solutionsForLength : solutions.getSolutionsPerLength()) {
			solutionsFoundCSV = solutionsFoundCSV.append(solutionsForLength.getFirst()).append(",")
					.append(solutionsForLength.getSecond()).append("\n");
		}
		APEUtils.write2file(solutionsFoundCSV.toString(), output, false);
		return true;
	}

}
