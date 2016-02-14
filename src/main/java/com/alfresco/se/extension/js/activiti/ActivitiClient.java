package com.alfresco.se.extension.js.activiti;

import java.io.IOException;
import java.util.Iterator;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.processor.BaseProcessorExtension;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.mozilla.javascript.ScriptableObject;
import org.springframework.extensions.webscripts.json.JSONUtils;

import com.alfresco.se.extension.js.http.HttpRequest;

/**
 * 
 * @author RFernandes
 *
 */
public class ActivitiClient extends BaseProcessorExtension
{
	private static Log logger = LogFactory.getLog(ActivitiClient.class);

	private String activitiEndpoint;
	private String user;
	private String password;
	private String alfrescoActivitiRepositoryId = "alfresco-1";

	public ScriptableObject startProcess(String processName, String name, ScriptableObject scriptableObject)
	        throws JSONException, IOException
	{
		return startDocumentProcess(processName, name, scriptableObject, null, null);
	}

	public ScriptableObject startDocumentProcess(String processName, String name, ScriptableObject scriptableObject,
	        String documentPropertyName, ScriptNode document) throws JSONException, IOException
	{
		String documentId = null;
		if (document != null && documentPropertyName != null)
		{
			documentId = postContent(document);
		}
		return start(processName, name, scriptableObject, documentPropertyName, documentId);
	}

	@SuppressWarnings("unchecked")
	private ScriptableObject start(String processName, String name, ScriptableObject scriptableObject,
	        String documentPropertyName, String documentId) throws IOException, JSONException
	{
		JSONObject json = new JSONObject();
		JSONUtils jsonUtils = new JSONUtils();
		org.json.simple.JSONObject values = jsonUtils.toJSONObject(scriptableObject);

		if (documentPropertyName != null)
		{
			values.put(documentPropertyName,documentId);
		}

		json.put("name", name);
		json.put("values", values);
		json.put("processDefinitionId", getProcessDefinitionId(processName));
		HttpRequest request = new HttpRequest();
		String httpUrl = activitiEndpoint + "/api/enterprise/process-instances";

		logger.debug("URL:" + httpUrl);
		logger.debug("Paylod:" + json.toString());

		String response = request.post(httpUrl, json.toString(), "application/json", user, password);
		return jsonUtils.toObject(response);
	}

	private String postContent(ScriptNode document) throws IOException, JSONException
	{
		return postContent(document.getName(), document.getId(), document.getSiteShortName(), document.getMimetype());
	}

	private String postContent(String documentName, String documentId, String siteShortName, String mimeType)
	        throws IOException, JSONException
	{
		HttpRequest request = new HttpRequest();
		JSONObject json = new JSONObject();
		JSONUtils jsonUtils = new JSONUtils();
		json.put("name", documentName);
		json.put("link", true);
		json.put("source", alfrescoActivitiRepositoryId);
		json.put("sourceId", documentId + "@" + siteShortName);
		json.put("mimeType", mimeType);
		String response = request.post(activitiEndpoint + "/api/enterprise/content", json.toString(),
		        "application/json", user, password);
		org.json.simple.JSONObject answer = jsonUtils.toJSONObject(jsonUtils.toObject(response));
		return String.valueOf(answer.get("id"));
	}

	@SuppressWarnings("unchecked")
	private String getProcessDefinitionId(String processName) throws IOException, JSONException
	{
		HttpRequest request = new HttpRequest();
		JSONUtils jsonUtils = new JSONUtils();
		String response = request.get(activitiEndpoint + "/api/enterprise/process-definitions", user, password);
		org.json.simple.JSONObject json = jsonUtils.toJSONObject(jsonUtils.toObject(response));
		JSONArray data = (JSONArray) json.get("data");
		Iterator<org.json.simple.JSONObject> iter = data.iterator();
		while (iter.hasNext())
		{
			org.json.simple.JSONObject definition = iter.next();
			if (definition.get("name").equals(processName))
			{
				return (String) definition.get("id");
			}
		}

		return null;
	}

	public void setActivitiEndpoint(String activitiEndpoint)
	{
		this.activitiEndpoint = activitiEndpoint;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public void setAlfrescoActivitiRepositoryId(String alfrescoActivitiRepositoryId)
	{
		this.alfrescoActivitiRepositoryId = alfrescoActivitiRepositoryId;
	}

	public static void main(String[] args)
	{
		ActivitiClient client = new ActivitiClient();
		JSONUtils jsonUtils = new JSONUtils();
		try
		{
			client.setActivitiEndpoint("http://192.168.99.223:9090/activiti-app");
			client.setUser("rui");
			client.setPassword("rui");
			ScriptableObject response = client.startProcess("FOI-1", "test",
			        jsonUtils.toObject("{\"name\":\"xpto test\"}"));
			System.out.println(jsonUtils.toJSONString(response));

			System.out.println("----------");

			String documentId = client.postContent("Lung Cancer Form.docx", "35b0a09b-bee1-4b1e-b922-b8af269f3153",
			        "dwp-foi", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
			System.out.println("content posted id=" + documentId);
			response = client.start("FOI-1", "test", jsonUtils.toObject("{\"name\":\"xpto test\"}"), "documents",
			        documentId);
			System.out.println(jsonUtils.toJSONString(response));

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
