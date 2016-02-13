package com.alfresco.se.extension.js.activiti;

import java.io.IOException;
import java.util.Iterator;

import org.alfresco.repo.processor.BaseProcessorExtension;
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

	private String activitiEndpoint;
	private String user;
	private String password;

	public ScriptableObject startProcess(String processName, String name, ScriptableObject scriptableObject) throws JSONException, IOException
	{
		JSONObject json = new JSONObject();
		JSONUtils jsonUtils = new JSONUtils();
		json.put("processDefinitionId", getProcessDefinitionId(processName));
		json.put("name", name);
		json.put("values", jsonUtils.toJSONObject(scriptableObject));
		HttpRequest request = new HttpRequest();
		String response = request.post(activitiEndpoint + "/api/enterprise/process-instances", json.toString(),
		        "application/json", user, password);
		return jsonUtils.toObject(response);
	}

	@SuppressWarnings("unchecked")
	private String getProcessDefinitionId(String processName) throws IOException, JSONException
	{
		HttpRequest request = new HttpRequest();
		JSONUtils jsonUtils = new JSONUtils();
		String response=request.get(activitiEndpoint+"/api/enterprise/process-definitions", user, password);
		org.json.simple.JSONObject json=jsonUtils.toJSONObject(jsonUtils.toObject(response));
		JSONArray data=(JSONArray)json.get("data");
		Iterator<org.json.simple.JSONObject> iter=data.iterator();
		while(iter.hasNext()){
			org.json.simple.JSONObject definition=iter.next();
			if(definition.get("name").equals(processName)){
				return (String)definition.get("id");
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
	
	
	public static void main(String[] args){
		ActivitiClient client=new ActivitiClient();
		JSONUtils jsonUtils=new JSONUtils();
		try{
			client.setActivitiEndpoint("http://192.168.99.223:9090/activiti-app");
			client.setUser("rui");
			client.setPassword("rui");
			ScriptableObject response=client.startProcess("FOI-1","test", jsonUtils.toObject("{\"name\":\"xpto test\"}"));
			System.out.println(jsonUtils.toJSONString(response));
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
