package com.alfresco.se.extension.js.activiti;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.processor.BaseProcessorExtension;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
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
	private ContentService contentService;

	public void setContentService(ContentService contentService)
	{
		this.contentService = contentService;
	}

	public ScriptableObject startProcess(String processName, String name, ScriptableObject scriptableObject)
	        throws JSONException, IOException
	{
		return startDocumentProcess(processName, name, scriptableObject, null, null);
	}

	public ScriptableObject startDocumentProcess(String processName, String name, ScriptableObject scriptableObject,
	        String[] documentPropertyNames, ScriptNode[] documents) throws JSONException, IOException
	{
		if(documents==null)
			documents=new ScriptNode[0];
		if(documentPropertyNames==null)
			documentPropertyNames=new String[0];
		
		String[] documentIds = new String[documents.length];
		for(int i=documents.length;i>0;i--){
			documentIds[i-1]=postContent(documents[i-1]);
		}
		return start(processName, name, scriptableObject, documentPropertyNames, documentIds);
	}

	@SuppressWarnings("unchecked")
	private ScriptableObject start(String processName, String name, ScriptableObject scriptableObject,
	        String[] documentPropertyNames, String[] documentIds) throws IOException, JSONException
	{
		JSONObject json = new JSONObject();
		JSONUtils jsonUtils = new JSONUtils();
		org.json.simple.JSONObject values = jsonUtils.toJSONObject(scriptableObject);

		for(int i=documentPropertyNames.length;i>0;i--){
			values.put(documentPropertyNames[i-1], documentIds[i-1]);
		}

		json.put("name", name);
		json.put("values", values);
		String processDefinitionId=getProcessDefinitionId(processName);
		if(processDefinitionId==null){
			throw new RuntimeException("No process found with name \""+processName+"\".");
		}
		json.put("processDefinitionId", processDefinitionId);
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
		json.put("link", false);
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
	
	
	public void saveLocalDocument(int activitiContentId, String documentName, ScriptNode parent,String mimeType) throws IOException{
		HttpRequest request = new HttpRequest();
		InputStream response = null;
		OutputStream outputStream = null;
		try
		{
			response = request.getStream(activitiEndpoint+"/api/enterprise/content/"+activitiContentId+"/raw",
			       user, password);
			ScriptNode doc=parent.createFile(documentName);
			ContentWriter writer=contentService.getWriter(doc.getNodeRef(),ContentModel.PROP_CONTENT,true);
			if(mimeType!=null)
				writer.setMimetype(mimeType);
			outputStream=writer.getContentOutputStream();

			int read = 0;
			byte[] bytes = new byte[1024];

			while ((read = response.read(bytes)) != -1)
			{
				outputStream.write(bytes, 0, read);
			}
		} finally
		{
			if (response != null)
				try
				{
					response.close();
				} catch (Exception e1)
				{
				}
			if (outputStream != null)
				try
				{
					outputStream.close();
				} catch (Exception e1)
				{
				}
		}
	}

	public static void main(String[] args)
	{
		ActivitiClient client = new ActivitiClient();
		client.setActivitiEndpoint("http://192.168.99.223:9090/activiti-app");
		client.setUser("rui");
		client.setPassword("rui");

		try
		{
			/*
			 * JSONUtils jsonUtils = new JSONUtils(); ScriptableObject response
			 * = client.startProcess("FOI-1", "test",
			 * jsonUtils.toObject("{\"name\":\"xpto test\"}"));
			 * System.out.println(jsonUtils.toJSONString(response));
			 * 
			 * System.out.println("----------");
			 * 
			 * String documentId = client.postContent("Lung Cancer Form.docx",
			 * "35b0a09b-bee1-4b1e-b922-b8af269f3153", "dwp-foi",
			 * "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
			 * ); System.out.println("content posted id=" + documentId);
			 * response = client.start("FOI-1", "test",
			 * jsonUtils.toObject("{\"name\":\"xpto test\"}"), "documents",
			 * documentId);
			 * System.out.println(jsonUtils.toJSONString(response));
			 */
			HttpRequest request = new HttpRequest();
			InputStream response = null;
			OutputStream outputStream = null;
			try
			{
				response = request.getStream("http://192.168.99.223:9090/activiti-app/api/enterprise/content/9024/raw",
				        "rui", "rui");

				outputStream = new FileOutputStream(new File("test.docx"));

				int read = 0;
				byte[] bytes = new byte[1024];

				while ((read = response.read(bytes)) != -1)
				{
					outputStream.write(bytes, 0, read);
				}
			} finally
			{
				if (response != null)
					try
					{
						response.close();
					} catch (Exception e1)
					{
					}
				if (outputStream != null)
					try
					{
						outputStream.close();
					} catch (Exception e1)
					{
					}
			}

			System.out.println("Done!");

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
