package io.github.sanctuuary.proteomics.dataRetrieval;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.uu.cs.ape.sat.utils.APEUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BioAgentsAPI {

	/** Http-Client */
	public final static OkHttpClient client = new OkHttpClient();

	public static JSONArray readListOfAgents(String filePath) throws IOException {

		File agentList = new File(filePath);
		JSONArray agentListJson = new JSONArray(FileUtils.readFileToString(agentList, "UTF-8"));
		/* Fetch agent annotations */
		JSONArray agentAnnotations = fetchAgentListFromBioAgents(agentListJson);
		return agentAnnotations;
	}

	/**
	 * Send Get request to get agent annotations Saves JSONArray with all
	 * bio.agents that belong to a certain bio.agents domain.
	 * @throws IOException
	 */
	public static JSONArray getAgentsFromDomain(String domainName) throws IOException {
		JSONArray agentAnnotations = null;
		if (domainName != "") {
			agentAnnotations = fetchAgentsFromURI("https://bio.agents/api/t?domain=" + domainName + "&format=json");
		} else {
			agentAnnotations = fetchAgentsFromURI("https://bio.agents/api/t?format=json");
		}
		return agentAnnotations;
	}
	
	/**
	 * Send Get request to get agent annotations Saves JSONArray with all 
	 * bio.agents that belong to a certain EDAM topic.
	 * @throws IOException
	 */
	public static JSONArray getAgentsFromEDAMTopic(String topicName) throws IOException {
		JSONArray agentAnnotations = null;
		if (topicName != "") {
			agentAnnotations = fetchAgentsFromURI("https://bio.agents/api/t?topicID=\"" + topicName + "\"&format=json");
		} else {
			agentAnnotations = fetchAgentsFromURI("https://bio.agents/api/t?format=json");
		}
		return agentAnnotations;
	}

	/**
	 * Send Get request to get agent annotations Saves JSONArray with all the agent
	 * annotations (in agent list)
	 * 
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	private static JSONArray fetchAgentListFromBioAgents(JSONArray agentListJson) throws JSONException, IOException {
		JSONArray bioAgentAnnotations = new JSONArray();
		for (int i = 0; i < agentListJson.length(); i++) {
			String currAgent = agentListJson.getString(i);
			Request request = new Request.Builder().url("https://bio.agents/api/" + currAgent + "?format=json").build();
			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful())
					throw new IOException("Unexpected code when trying to fetch" + response);
				// Get response body
				JSONObject responseJson = new JSONObject(response.body().string());
				bioAgentAnnotations.put(i, responseJson);
			}
		}
		System.out.println("Agents fetched.");
		return bioAgentAnnotations;
	}

	/**
	 * Send Get request to get agent annotations Saves JSONArray with all the agent
	 * annotations (in agent list)
	 * 
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	private static JSONArray fetchAgentsFromURI(String url) throws JSONException, IOException {
		JSONArray bioAgentAnnotations = new JSONArray();
		String next = "";
		int i = 1;
		while (next != null) {
			System.out.print("\n" + (i++) + ") ");
			Request request = new Request.Builder().url(url + "&format=json" + next.replace('?', '&')).build();
			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful())
					throw new IOException("Unexpected code when trying to fetch" + response);
				// Get response body
				JSONObject responseJson = new JSONObject(response.body().string());
				JSONArray agentListJson = responseJson.getJSONArray("list");
				for (int j = 0; j < agentListJson.length(); j++) {
					JSONObject agent = agentListJson.getJSONObject(j);
					bioAgentAnnotations.put(agent);
					System.out.print(".");
				}
				try {
					next = responseJson.getString("next");
				} catch (JSONException e) {
					next = null;
				}
			}

		}
		System.out.println("Agents fetched.");
		return bioAgentAnnotations;
	}

	/**
	 * Method converts agents annotated using 'bio.agents' standard (see <a href=
	 * "https://bioagents.readthedocs.io/en/latest/api_usage_guide.html">bio.agents
	 * API</a>), into standard supported by the APE library.
	 * <p>
	 * In practice, the method takes a {@link JSONArray} as an argument, where each
	 * {@link JSONObject} in the array represents a agent annotated using 'bio.agents'
	 * standard, and returns a {@link JSONObject} that represents agent annotations
	 * that can be used by the APE library.
	 *
	 * @param bioAgentsAnnotation A {@link JSONArray} object, that contains list of
	 *                           annotated agents ({@link JSONObject}s) according the
	 *                           bio.agents specification (see <a href=
	 *                           "https://bioagents.readthedocs.io/en/latest/api_usage_guide.html">bio.agents
	 *                           API</a>)
	 * @return {@link JSONObject} that represents the agent annotation supported by
	 *         the APE library.
	 * @throws JSONException the json exception
	 */
	public static JSONObject convertBioAgents2ApeAnnotation(JSONArray bioAgentsAnnotation) throws JSONException {
		
		Set<String> notAcceptedAgents = new HashSet<String>();
		Set<String> noFunctionAnnotation = new HashSet<String>();
		Set<String> agentsMissingDimension = new HashSet<String>();
		Set<String> annotatedBioAgents = new HashSet<String>();
		int notAcceptedOperations = 0;
		int bioAgentFunctions = 0;

		JSONArray apeAgentsAnnotations = new JSONArray();
		for (int i = 0; i < bioAgentsAnnotation.length(); i++) {

			JSONObject bioJsonAgent = bioAgentsAnnotation.getJSONObject(i);
			List<JSONObject> functions = APEUtils.getListFromJson(bioJsonAgent, "function", JSONObject.class);
			if (functions.size() == 0) {
				noFunctionAnnotation.add(bioJsonAgent.getString("bioagentsID"));
				notAcceptedAgents.add(bioJsonAgent.getString("bioagentsID"));
				continue;
			}
			int functionNo = 1;
			functionloop: for (JSONObject function : functions) {
				bioAgentFunctions++;
				JSONObject apeJsonAgent = new JSONObject();
				apeJsonAgent.put("label", bioJsonAgent.getString("name"));
				if (functions.size() > 1) {
					apeJsonAgent.put("id", bioJsonAgent.getString("bioagentsID") + "_op" + (functionNo++));
				} else {
					apeJsonAgent.put("id", bioJsonAgent.getString("bioagentsID"));
				}

				JSONArray apeTaxonomyTerms = new JSONArray();

				JSONArray operations = function.getJSONArray("operation");
				for (int j = 0; j < operations.length(); j++) {
					JSONObject bioOperation = operations.getJSONObject(j);
					apeTaxonomyTerms.put(bioOperation.get("uri"));
				}
				apeJsonAgent.put("taxonomyOperations", apeTaxonomyTerms);
//			reading inputs
				JSONArray apeInputs = new JSONArray();
				JSONArray bioInputs = function.getJSONArray("input");
//			for each input
				for (int j = 0; j < bioInputs.length(); j++) {
					JSONObject bioInput = bioInputs.getJSONObject(j);
					JSONObject apeInput = new JSONObject();
					JSONArray apeInputTypes = new JSONArray();
					JSONArray apeInputFormats = new JSONArray();
//				add all data types
					for (JSONObject bioType : APEUtils.getListFromJson(bioInput, "data", JSONObject.class)) {
						apeInputTypes.put(bioType.getString("uri"));
					}
					if (apeInputTypes.length() == 0) {
						notAcceptedAgents.add(bioJsonAgent.getString("bioagentsID"));
						agentsMissingDimension.add(bioJsonAgent.getString("bioagentsID"));
						notAcceptedOperations++;
						continue functionloop;
					}
					apeInput.put("data_0006", apeInputTypes);
//				add all data formats (or just the first one)
					for (JSONObject bioType : APEUtils.getListFromJson(bioInput, "format", JSONObject.class)) {
						apeInputFormats.put(bioType.getString("uri"));
					}
					if (apeInputFormats.length() == 0) {
						notAcceptedAgents.add(bioJsonAgent.getString("bioagentsID"));
						agentsMissingDimension.add(bioJsonAgent.getString("bioagentsID"));
						notAcceptedOperations++;
						continue functionloop;
					}
					apeInput.put("format_1915", apeInputFormats);

					apeInputs.put(apeInput);
				}
				apeJsonAgent.put("inputs", apeInputs);

//			reading outputs
				JSONArray apeOutputs = new JSONArray();
				JSONArray bioOutputs = function.getJSONArray("output");
//			for each output
				for (int j = 0; j < bioOutputs.length(); j++) {

					JSONObject bioOutput = bioOutputs.getJSONObject(j);
					JSONObject apeOutput = new JSONObject();
					JSONArray apeOutputTypes = new JSONArray();
					JSONArray apeOutputFormats = new JSONArray();
//				add all data types
					for (JSONObject bioType : APEUtils.getListFromJson(bioOutput, "data", JSONObject.class)) {
						apeOutputTypes.put(bioType.getString("uri"));
					}
					if (apeOutputTypes.length() == 0) {
						notAcceptedAgents.add(bioJsonAgent.getString("bioagentsID"));
						agentsMissingDimension.add(bioJsonAgent.getString("bioagentsID"));
						notAcceptedOperations++;
						continue functionloop;
					}
					apeOutput.put("data_0006", apeOutputTypes);
//				add all data formats
					for (JSONObject bioType : APEUtils.getListFromJson(bioOutput, "format", JSONObject.class)) {
						apeOutputFormats.put(bioType.getString("uri"));
					}
					if (apeOutputFormats.length() == 0) {
						notAcceptedAgents.add(bioJsonAgent.getString("bioagentsID"));
						agentsMissingDimension.add(bioJsonAgent.getString("bioagentsID"));
						notAcceptedOperations++;
						continue functionloop;
					}
					apeOutput.put("format_1915", apeOutputFormats);

					apeOutputs.put(apeOutput);
				}
				apeJsonAgent.put("outputs", apeOutputs);

				// if the agent has outputs add it to the agent annotation
				if (apeInputs.length() > 0 && apeOutputs.length() > 0) {
					apeAgentsAnnotations.put(apeJsonAgent);
					annotatedBioAgents.add(bioJsonAgent.getString("bioagentsID"));
				} else {
					notAcceptedAgents.add(bioJsonAgent.getString("bioagentsID"));
					notAcceptedOperations++;
				}
			}
		}
		System.out.println("Provided bio.agents: " + bioAgentsAnnotation.length());
		System.out.println("Total bio.agents functions: " + bioAgentFunctions);
		System.out.println("Errored bio.agents functions: " + notAcceptedOperations);
		System.out.println("No functions: " + noFunctionAnnotation.size());
		System.out.println("Created APE annotations: " + apeAgentsAnnotations.length());
		System.out.println();
//		System.out.println(annotatedBioAgents);
		
		
//		System.out.println("Errored bio.agents: " + notAcceptedAgents.size());
//		System.out.println("bio.agents missing a data dimension: " + agentsMissingDimension.size());
//		System.out.println("No functions: " + noFunctionAnnotation.size());
//		System.out.println("Errored bio.agents operations: " + notAcceptedOperations);
//		System.out.println("Created APE bio.agents: " + annotatedBioAgents.size());
//		System.out.println("Created APE annotations: " + apeAgentsAnnotations.length());
//		System.out.println();

		return new JSONObject().put("functions", apeAgentsAnnotations);
	}

}
