package io.github.sanctuuary.proteomics;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import nl.uu.cs.ape.sat.APE;
import nl.uu.cs.ape.sat.configuration.APEConfigException;
import nl.uu.cs.ape.sat.configuration.APERunConfig;
import nl.uu.cs.ape.sat.core.solutionStructure.SolutionsList;
import nl.uu.cs.ape.sat.utils.APEUtils;

public class Evaluation {

	/**
	 * 
	 *
	 * @param args APE expects only one (1) argument: The absolute or relative path
	 *             to the configuration file.
	 */
	public static void main(String[] args) {
		
	}
	
	private static void runSynthesis() {

//		List<String> subpaths = Arrays.asList("MassSpectometry/No1/", "MassSpectometry/No1_NoConstr/",
//				"MassSpectometry/No1_Extended/", "MassSpectometry/No1_Extended_NoConstr/","MassSpectometry/No3/", "MassSpectometry/No3_NoConstr/",
//				"MassSpectometry/No3_Extended/", "MassSpectometry/No3_Extended_NoConstr/");
		List<String> subpaths = Arrays.asList("MassSpectometry/No1/");
		for (String subPath : subpaths) {
			System.out.println(subPath);
			String path = "./res/";
			String fileName = "ape.configuration";
			if (!APEUtils.isValidReadFile(path + subPath + fileName)) {
				System.err.println("Bad path.");
				return;
			}

			File file = null;
			try {
				file = File.createTempFile("temp", null);
				file.deleteOnExit();
				String content = APEUtils.readFile(path + subPath + fileName, Charset.defaultCharset());
				content = content.replace("./", path);
				APEUtils.write2file(content, file, false);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			APE apeFramework = null;
			try {
				// set up the APE framework
				apeFramework = new APE(file.getAbsolutePath());

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
				APERunConfig runCongif = new APERunConfig(APEUtils.readFileToJSONObject(file), apeFramework.getDomainSetup());
				runCongif.setDebugMode(false);
				runCongif.setMaxNoSolutions(100);
				runCongif.setNoGraphs(5);
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
			}

			/*
			 * Writing solutions to the specified file in human readable format
			 */
			if (solutions.isEmpty()) {
				System.out.println("UNSAT");
			} else {
				try {
					APE.writeSolutionToFile(solutions);
					APE.writeDataFlowGraphs(solutions);
				} catch (IOException e) {
					System.err.println("Error in writing the solutions. to the file system.");
					e.printStackTrace();
				}
			}
		}
	
	}
	
}
