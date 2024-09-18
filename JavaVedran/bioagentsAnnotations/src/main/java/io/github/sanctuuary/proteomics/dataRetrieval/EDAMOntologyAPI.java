package io.github.sanctuuary.proteomics.dataRetrieval;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

public class EDAMOntologyAPI {

	public static void writeEDAM2file(String pathToFile) throws IOException {
		fetchAndWriteOntology("http://edamontology.org/EDAM.owl", pathToFile);
	}
	
	public static void fetchAndWriteOntology(String urlToOntology, String pathToFile) throws IOException {
		try {
			/** URL to the ontology **/
			URL ONTOLOGY_URL = new URL(urlToOntology);
			/** Ontology gets locally saved in the apeInputs folder **/
			File ontologyFile = new File(pathToFile); // temp file
			FileUtils.copyURLToFile(ONTOLOGY_URL, ontologyFile); // Get ontology and copy to the temp file
		} catch (MalformedURLException e) {
			System.err.println("Ontology not provided correctly");
		}
		System.out.println("Ontology fetched.");
	}
}
