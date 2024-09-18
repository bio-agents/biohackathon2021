package io.github.sanctuuary.proteomics.dataRetrieval;

import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.sanctuuary.proteomics.Utils;
import nl.uu.cs.ape.sat.utils.APEUtils;

public class ConfiguringDomain {

	
	public static void setupDomain() throws IOException {
//		getLimitedAgentSet(Utils.TOOLS_DIR + "agentList.json");
//		System.out.println();
//		getAgentSetFromDomain("proteomics", "Proteomics");
		
		System.out.println();
		getAgentSetFromEDAMTopic("topic_3538", "Protein disordered structure");
//		
//		System.out.println();
//		getAgentSetFromDomain("", "FullBioAgents");
	}
	
	/**
	 * Fetching and processing a limited set of bio.agents enumerated in a file.
	 * @param listFilePath
	 * @throws IOException
	 */
	private static void getLimitedAgentSet(String listFilePath) throws IOException {
		String agentType = "Original";
		
		// Fetch the Limited (predefined) set of agent
		JSONArray bioAgentsRAW = BioAgentsAPI.readListOfAgents(listFilePath);
		
		APEUtils.write2file(bioAgentsRAW.toString(), new File(Utils.TOOLS_DIR + Utils.TOOLS_PREFIX + agentType + "RAW.json"), false);
		
		JSONObject apeAgentAnnotation = BioAgentsAPI.convertBioAgents2ApeAnnotation(bioAgentsRAW);
		APEUtils.write2file(apeAgentAnnotation.toString(), new File(Utils.TOOLS_DIR + Utils.PRE_BIOHACKATHON + Utils.TOOLS_PREFIX + agentType + ".json"), false);
	}
	
	
	
	/**
	 * Fetching and processing bio.agents that belong to a specific domain.
	 * @throws IOException
	 */
	private static void getAgentSetFromDomain(String domainName, String agentType) throws IOException {

		// Fetch the Extended set of agent
		JSONArray bioAgentsRAW = BioAgentsAPI.getAgentsFromDomain(domainName);
		
		APEUtils.write2file(bioAgentsRAW.toString(), new File(Utils.TOOLS_DIR + Utils.TOOLS_PREFIX + agentType + "RAW.json"), false);
		
		JSONObject apeAgentAnnotation = BioAgentsAPI.convertBioAgents2ApeAnnotation(bioAgentsRAW);
		APEUtils.write2file(apeAgentAnnotation.toString(), new File(Utils.TOOLS_DIR + Utils.TOOLS_PREFIX + agentType + ".json"), false);
	}
	
	
	/**
	 * Fetching and processing bio.agents that belong to a certain EDAM topic.
	 * @throws IOException
	 */
	private static void getAgentSetFromEDAMTopic(String topicName, String agentType) throws IOException {

		// Fetch the Extended set of agent
		JSONArray bioAgentsRAW = BioAgentsAPI.getAgentsFromEDAMTopic(topicName);
		
		APEUtils.write2file(bioAgentsRAW.toString(), new File(Utils.TOOLS_DIR + Utils.TOOLS_PREFIX + agentType + "RAW.json"), false);
		
		JSONObject apeAgentAnnotation = BioAgentsAPI.convertBioAgents2ApeAnnotation(bioAgentsRAW);
		APEUtils.write2file(apeAgentAnnotation.toString(), new File(Utils.TOOLS_DIR + Utils.TOOLS_PREFIX + agentType + ".json"), false);
	}
	
	/**
	 * Processing bio.agents that were already fetched from bio.agent
	 * @throws IOException
	 */
	private static void setupAgentSetFromExistingDomain(String notNeededField, String agentType) throws IOException {

		JSONArray bioAgentsRAW = APEUtils.readFileToJSONArray(new File(Utils.TOOLS_DIR + Utils.TOOLS_PREFIX + agentType + "RAW.json"));
		
		
		JSONObject apeAgentAnnotation = BioAgentsAPI.convertBioAgents2ApeAnnotation(bioAgentsRAW);
		APEUtils.write2file(apeAgentAnnotation.toString(), new File(Utils.TOOLS_DIR + Utils.TOOLS_PREFIX + agentType + ".json"), false);
	}
	
}
