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
import nl.uu.cs.ape.sat.models.AbstractModule;
import nl.uu.cs.ape.sat.models.AllModules;
import nl.uu.cs.ape.sat.models.AllTypes;
import nl.uu.cs.ape.sat.models.AuxTypePredicate;
import nl.uu.cs.ape.sat.models.Module;
import nl.uu.cs.ape.sat.models.Pair;
import nl.uu.cs.ape.sat.models.logic.constructs.TaxonomyPredicate;
import nl.uu.cs.ape.sat.APE;
import nl.uu.cs.ape.sat.utils.APEDimensionsException;
import nl.uu.cs.ape.sat.utils.APEUtils;

public class AnnotationQualityEvaluation {

	public static void evaluateAllAnnotations(String agentAnnotationDirPath, String resultsDirPath) throws IOException {
		List<String> agentAnnotationsDomain = Arrays.asList("agentAnnotationFullBioAgents", "agentAnnotationMetabolomics", "agentAnnotationProteomics", "agentAnnotationOriginal");
		
		for (String agentsName : Utils.getElements(agentAnnotationsDomain, 4)) {
			String agentAnnotations = agentAnnotationDirPath + agentsName + ".json";
			String resultsFile = resultsDirPath + agentsName;
			evaluateAnnotations(agentsName, agentAnnotations, resultsFile);
		}
		
	}
	
	/**
	 * Function used to evaluate the quality of a agent
	 * @param title
	 * @param agentAnnotations
	 * @throws IOException
	 */
	private static void evaluateAnnotations(String title, String agentAnnotations, String resultFilePath)
			throws IOException {

		System.out.println("\t---Agent annotation Evaluation: " + title + "---\n\n");
		File baseConfiguration = new File(Utils.CONFIGURATION_DIR + "baseape.configuration");

		JSONObject coreConfigJson = APEUtils.readFileToJSONObject(baseConfiguration);
		coreConfigJson = updateCoreConfig(coreConfigJson, "agent_annotations_path", agentAnnotations);
		coreConfigJson = updateCoreConfig(coreConfigJson, "strict_agent_annotations", "false");
		
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
		
		deleteExistingResults(resultFilePath);
		setupHeaders(resultFilePath);
		
		AllModules allModules = apeFramework.getDomainSetup().getAllModules();
		AllTypes allTypes = apeFramework.getDomainSetup().getAllTypes();

		for(TaxonomyPredicate absModule : allModules.getModules()) {
			if(absModule instanceof Module) {
				Module currModule = (Module) absModule;
				Integer orderNo = 1;
				for(TaxonomyPredicate input : currModule.getModuleInput()) {
					AuxTypePredicate concreteInput = (AuxTypePredicate) input;
					List<Integer> depths = new ArrayList<Integer>();
					for(TaxonomyPredicate inputDimension : concreteInput.getGeneralizedPredicates()) {
						String rootLabel = allTypes.get(inputDimension.getRootNodeID()).getPredicateLabel();
						if(rootLabel.matches("APE_label")) {
							continue;
						}
						depths.add(calculateDepth(inputDimension));
					}
					APEUtils.write2file(createCSVRow(currModule.getPredicateLabel(), orderNo.toString(), depths.get(0).toString(), depths.get(1).toString()), getInFile(resultFilePath), true);
					orderNo++;
				}
				orderNo = 1;
				for(TaxonomyPredicate output : currModule.getModuleOutput()) {
					AuxTypePredicate concreteOutput = (AuxTypePredicate) output;
					List<Integer> depths = new ArrayList<Integer>();
					for(TaxonomyPredicate outputDimension : concreteOutput.getGeneralizedPredicates()) {
						String rootLabel = allTypes.get(outputDimension.getRootNodeID()).getPredicateLabel();
						if(rootLabel.matches("APE_label")) {
							continue;
						}
						depths.add(calculateDepth(outputDimension));
						}
					APEUtils.write2file(createCSVRow(currModule.getPredicateLabel(), orderNo.toString(), depths.get(0).toString(), depths.get(1).toString()), getOutFile(resultFilePath), true);
					orderNo++;
					
				}
				
				
			}
		}

	}

	private static String createCSVRow(String predicateLabel, String orderNo, String dataDepth, String formatDepth) {
		String csvROw = predicateLabel.replace(",", ";") + "\t " + orderNo + "\t " + dataDepth + "\t" + formatDepth + "\n"; 
		return csvROw;
	}

	/**
	 * Calculate the furtherest leaf in the taxonomy from the currData class.
	 * @param currData - class in the taxonomy that is evaluated
	 * @return
	 */
	private static int calculateDepth(TaxonomyPredicate currData) {
		int maxDepth = 0;
		for(TaxonomyPredicate subData : APEUtils.safe(currData.getSubPredicates())) {
			int newDepth = calculateDepth(subData) + 1;
			maxDepth = Math.max(maxDepth, newDepth);
		}
		
		return maxDepth;
	}

	private static File getOutFile(String resultFilePath) {
		return new File(resultFilePath + "_outputs.csv");
	}

	private static File getInFile(String resultFilePath) {
		return new File(resultFilePath + "_inputs.csv");
	}

	private static void deleteExistingResults(String resultFilePath) {
		try {
			getOutFile(resultFilePath).delete();
		} catch (Exception e) {
			// skip
			System.err.println("Delete failed.");
		}
		try {
			getInFile(resultFilePath).delete();
		} catch (Exception e) {
			// skip
			System.err.println("Delete failed.");
		}

	}
	
	private static void setupHeaders(String resultFilePath) {
		try {
			APEUtils.write2file(createCSVRow("Name", "OrderNo", "Data Depth", "Format depth"), getOutFile(resultFilePath), true);
		} catch (Exception e) {
			// skip
			System.err.println("Delete failed.");
		}
		try {
			APEUtils.write2file(createCSVRow("Name", "OrderNo", "Data Depth", "Format depth"), getInFile(resultFilePath), true);
		} catch (Exception e) {
			// skip
			System.err.println("Delete failed.");
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


	/**
	 * Update agent annotation field in the configuration file.
	 * @param coreConfigJson
	 * @param agentAnnotationsPath
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	private static JSONObject updateCoreConfig(JSONObject coreConfigJson, String tag, String agentAnnotationsPath)
			throws JSONException, IOException {
		coreConfigJson.put(tag, agentAnnotationsPath);
		return coreConfigJson;
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
