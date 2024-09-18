package io.github.sanctuuary.proteomics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import io.github.sanctuuary.proteomics.models.BioAgentsImprovement;
import nl.uu.cs.ape.sat.APE;
import nl.uu.cs.ape.sat.configuration.APEConfigException;
import nl.uu.cs.ape.sat.models.AllTypes;
import nl.uu.cs.ape.sat.models.AuxTypePredicate;
import nl.uu.cs.ape.sat.models.Module;
import nl.uu.cs.ape.sat.models.Type;
import nl.uu.cs.ape.sat.models.logic.constructs.TaxonomyPredicate;
import nl.uu.cs.ape.sat.utils.APEDimensionsException;
import nl.uu.cs.ape.sat.utils.APEUtils;

public class AnnotationQualityImplementation {

	private String agentAnnotations;
	private String resultsDir;
	private APE apeFramework;
	private Map<String, Set<String>> dataTypeFormatDependencies;
	private Map<String, Set<String>> operationDataTypeInDependencies;
	private Map<String, Set<String>> operationDataTypeOutDependencies;
	private List<BioAgentsImprovement> bioAgentImprovements;

	public AnnotationQualityImplementation(String agentAnnotationDirPath, String resultsDirPath) {
		List<String> agentAnnotationsDomain = Arrays.asList("agentAnnotationFullBioAgents", "agentAnnotationMetabolomics",
				"agentAnnotationProteomics", "agentAnnotationOriginal");

		for (String agentsName : Utils.getElements(agentAnnotationsDomain, 1)) {
			agentAnnotations = agentAnnotationDirPath + agentsName + ".json";
			resultsDir = resultsDirPath + agentsName;
		}
		bioAgentImprovements = new ArrayList<BioAgentsImprovement>();
	}

	/**
	 * Function used to setup APE framework in bioinformatics using the bio.agents
	 * annotation.
	 * 
	 * @throws IOException
	 */
	public void setupAPE() throws IOException {

		File baseConfiguration = new File(Utils.CONFIGURATION_DIR + "baseape.configuration");

		JSONObject coreConfigJson = APEUtils.readFileToJSONObject(baseConfiguration);
		coreConfigJson = updateCoreConfig(coreConfigJson, "agent_annotations_path", agentAnnotations);
		coreConfigJson = updateCoreConfig(coreConfigJson, "strict_agent_annotations", "false");

		apeFramework = null;
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
	}

	/**
	 * Retrieve EDAM data properties used to specify dependencies format-type and
	 * operation - input/output.
	 * 
	 * @throws APEDimensionsException
	 * @throws OWLOntologyCreationException
	 * @throws JSONException
	 * @throws IOException
	 */
	public void getEDAMData() throws APEDimensionsException, OWLOntologyCreationException, JSONException, IOException {

		ExtendedOWLReader extOwlReader = new ExtendedOWLReader(apeFramework.getDomainSetup(),
				apeFramework.getConfig().getOntologyFile());

		dataTypeFormatDependencies = extOwlReader.getObjectProperties(Utils.DATA_FORMAT_IRI,
				"http://edamontology.org/is_format_of");
		operationDataTypeInDependencies = extOwlReader.getObjectProperties(Utils.OPERATION_IRI,
				"http://edamontology.org/has_input");
		operationDataTypeOutDependencies = extOwlReader.getObjectProperties(Utils.OPERATION_IRI,
				"http://edamontology.org/has_output");
		
//		Utils.printStringMap(operationDataTypeInDependencies, apeFramework.getDomainSetup().getAllModules(), apeFramework.getDomainSetup().getAllTypes());
//		Utils.printStringMap(operationDataTypeOutDependencies, apeFramework.getDomainSetup().getAllModules(), apeFramework.getDomainSetup().getAllTypes());
	}

	/**
	 * Evaluate the quality of fetched bio.agents based on the <b>is_format_of</b> EDAM object property 
	 * that reflects data 'format' -> 'type' dependency.
	 */
	public void evalateBioAgentsByEDAM_isformatof() {
		for (TaxonomyPredicate abstractModule : apeFramework.getDomainSetup().getAllModules().getModules()) {
			if (abstractModule instanceof Module) {
				Module module = (Module) abstractModule;
				for (Type input : module.getModuleInput()) {
					evaluateIODataTypeByEDAM_isformatof(module, input, "inputFromFormat");
				}
				for (Type output : module.getModuleOutput()) {
					evaluateIODataTypeByEDAM_isformatof(module, output, "outputFromFormat");
				}
			}
		}
		BioAgentsImprovement.setupHeader(resultsDir + "_suggestions.csv");
		bioAgentImprovements.forEach(impr -> impr.print2CSV(resultsDir + "_suggestionsBy_is_format_of.csv"));

	}
	
	/**
	 * Evaluate the quality of fetched bio.agents based on the <b>has_input</b> EDAM object property 
	 * that reflects 'operation' -> 'input data type' dependency.
	 */
	public void evalateBioAgentsByEDAM_hasinput() {
		String fileDecr = "_has_input";
		
		for (TaxonomyPredicate abstractModule : apeFramework.getDomainSetup().getAllModules().getModules()) {
			if (abstractModule instanceof Module) {
				Module module = (Module) abstractModule;
				for (Type input : module.getModuleInput()) {
					evaluateIODataTypeByEDAM_hasIO(module, input, "inputFromOperation");
				}
				for (Type output : module.getModuleOutput()) {
//					evaluateIODataTypeByEDAM_hasinput(module, output, "outputFromOperation");
				}
			}
		}
		
		BioAgentsImprovement.setupHeader(resultsDir + "_suggestionsBy " + fileDecr+ ".csv");
		bioAgentImprovements.forEach(impr -> impr.print2CSV(resultsDir + "_suggestionsBy " + fileDecr+ ".csv"));

	}
	
	/**
	 * Evaluate the module 
	 * @param module
	 * @param inputOrOutput
	 * @param infoIO
	 */
	private void evaluateIODataTypeByEDAM_hasIO(Module module, Type inputOrOutput, String infoIO) {
		
	}
	

	/**
	 * Evaluate a data type that is agent input or agent output based on the given set of dependencies.
	 * The evaluation focuses strictly on checking whether the expected type annotation is more 
	 * concrete than the annotated type in bio.agents. In that case it suggests the expected <b> data  type</b> instead.
	 *  
	 * @param module
	 * @param inputOrOutput
	 * @param infoIO
	 * @param dataTypeDependencies - map of dependencies that is looked at.
//	 * @param useIsFormatOf - {@code true} if the dependency is based on <b>is_format_of</b> EDAM object property. 
 								 {@code false} if the dependency is based on <b>has_input</b> or <b>has_output</b> EDAM object property. 
	 */
	private void evaluateIODataTypeByEDAM_isformatof(Module module, Type inputOrOutput, String infoIO) {

		SortedSet<TaxonomyPredicate> predicates = ((AuxTypePredicate) inputOrOutput).getGeneralizedPredicates();
		TaxonomyPredicate dataFormat = predicates.stream().filter(x -> x.getRootNodeID().equals(Utils.DATA_FORMAT_IRI))
				.findFirst().get();
		if (dataFormat instanceof AuxTypePredicate) {
			return;
		}
		/* Check if the format was annotated using the EDAM format/type dependencies. */
		if (dataTypeFormatDependencies.get(dataFormat.getPredicateID()) == null) {
			return;
		}

		TaxonomyPredicate dataType = predicates.stream().filter(x -> x.getRootNodeID().equals(Utils.DATA_TYPE_IRI))
				.findFirst().get();

		if (dataType instanceof AuxTypePredicate) {
			return;
		}
		
		Set<String> altTypeIDs = evaluateFormatTypeDepencency(module, dataFormat, dataType);
		if (altTypeIDs.size() > 0) {
			AllTypes allTypes = apeFramework.getDomainSetup().getAllTypes();
			Set<TaxonomyPredicate> altTypes = altTypeIDs.stream().map(typeID -> allTypes.get(typeID))
					.collect(Collectors.toSet());
			bioAgentImprovements.add(new BioAgentsImprovement(module, infoIO, dataFormat, dataType, altTypes));

		}

	}

	/**
	 * Evaluate the relation between the given and expected data type, based on the annotated concept.
	 * @param module - module that is being used
	 * @param conceptCreatingDependecy - concept that is used to infer the expected data type
	 * @param usedDataType - data type that is evaluated
	 * @return
	 */
	private Set<String> evaluateFormatTypeDepencency(TaxonomyPredicate module, TaxonomyPredicate conceptCreatingDependecy,
			TaxonomyPredicate usedDataType) {
		Set<String> setOfNewTypes = new HashSet<String>();
		Set<String> expectedDataTypeIDs = dataTypeFormatDependencies.get(conceptCreatingDependecy.getPredicateID());
		for (String expectedDataTypeID : expectedDataTypeIDs) {
			TaxonomyPredicate expectedDataType = apeFramework.getDomainSetup().getAllTypes().get(expectedDataTypeID);
			if (isTermMoreSpecific(module, expectedDataType, usedDataType, true)) {
				setOfNewTypes.add(expectedDataTypeID);
			}
		}
		return setOfNewTypes;
	}

	private boolean isTermMoreSpecific(TaxonomyPredicate module, TaxonomyPredicate expectedDataType,
			TaxonomyPredicate usedDataType, boolean firstCall) {
		boolean moreSpecific = false;
		if (firstCall && usedDataType.equals(expectedDataType)) {
			return false;
		}
		// test if it is superclass? bio.agents are better in that case
		for (TaxonomyPredicate subType : usedDataType.getSubPredicates()) {
			if (subType.equals(expectedDataType)) {
				return true;
			} else {
				moreSpecific = moreSpecific || isTermMoreSpecific(module, expectedDataType, subType, false);
			}
		}
		return moreSpecific;
	}

	/**
	 * Update agent annotation field in the configuration file.
	 * 
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

}
