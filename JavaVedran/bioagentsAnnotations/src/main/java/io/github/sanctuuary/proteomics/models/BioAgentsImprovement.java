package io.github.sanctuuary.proteomics.models;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;

import nl.uu.cs.ape.sat.models.logic.constructs.TaxonomyPredicate;
import nl.uu.cs.ape.sat.utils.APEUtils;


public class BioAgentsImprovement {

	private TaxonomyPredicate module;
	private String infoIO;
	private TaxonomyPredicate existingFormat;
	private TaxonomyPredicate existignType;
	private Set<TaxonomyPredicate> suggestedType;
	
	
	public BioAgentsImprovement(TaxonomyPredicate module, String infoIO, TaxonomyPredicate existingFormat, TaxonomyPredicate existignType,
			Set<TaxonomyPredicate> suggestedType) {
		super();
		this.module = module;
		this.infoIO = infoIO;
		this.existingFormat = existingFormat;
		this.existignType = existignType;
		this.suggestedType = suggestedType;
	}
	
	public void print2CSV(String resultFilePath) {
		try {
			APEUtils.write2file(toCSV(), new File(resultFilePath), true);
		} catch (Exception e) {
			// skip
			System.err.println("Writing to CSV failed.");
		}
	}
	
	public String toCSV() {
		String newTypes = suggestedType.stream()
				 .map(type -> type.getPredicateLabel().replace(",", ";"))
                 .collect(Collectors.joining("\t "));
		String csvROw = module.getPredicateLabel().replace(",", ";") + "\t " + infoIO + "\t " + existingFormat.getPredicateLabel().replace(",", ";") + "\t" + existignType.getPredicateLabel().replace(",", ";") + "\t" + newTypes + "\n"; 
		return csvROw; 
	}
	
	private static String createCSVRow(String module, String infoIO, String exFormat, String exType, String newTYpes) {
		String csvROw = module.replace(",", ";") + "\t " + infoIO + "\t " + exFormat + "\t" + exType+ "\t" + newTYpes + "\n"; 
		return csvROw;
	}
	
	
	public static void setupHeader(String resultFilePath) {
		try {
			APEUtils.write2file(createCSVRow("Module", "IO_info", "Existing Format", "Existing Type", "New Types"), new File(resultFilePath), false);
		} catch (Exception e) {
			// skip
			System.err.println("Delete failed.");
		}

	}
}
