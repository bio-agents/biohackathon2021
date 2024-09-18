package io.github.sanctuuary.proteomics;

import java.io.IOException;

import org.json.JSONException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import nl.uu.cs.ape.sat.utils.APEDimensionsException;


public class Main 
{

	public static void main(String[] args) throws IOException, APEDimensionsException, OWLOntologyCreationException, JSONException {
		
		/* Fetch json from bio.agents and convert it to APE annotations. */
//		ConfiguringDomain.setupDomain();
		
		/* Evaluate how specific are bio.agents annotations */
//		AnnotationQualityEvaluation.evaluateAllAnnotations(Utils.TOOLS_DIR + Utils.PRE_BIOHACKATHON, Utils.RESULTS_DIR + Utils.PRE_BIOHACKATHON);

		/* Create suggestions on bio.agents improvements. */
		AnnotationQualityImplementation annotationImplementation = new AnnotationQualityImplementation(Utils.TOOLS_DIR + Utils.PRE_BIOHACKATHON, Utils.RESULTS_DIR);
		annotationImplementation.setupAPE();
		annotationImplementation.getEDAMData();
//		annotationImplementation.evalateBioAgentsByEDAM_isformatof();
		annotationImplementation.evalateBioAgentsByEDAM_hasinput();
//		annotationImplementation.evalateBioAgentsByEDAM_hasoutput();
		
		System.out.println("end");
	
		}
}
