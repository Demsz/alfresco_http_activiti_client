# alfresco_http_activiti_client

This extension to the repository adds 2 javascript root objects:

- http
- activiti

## Configuration
After applying the amp you need to set some configuration on the alfrescop-global.properties file if you intend to use the activiti root object (no configuration required if you just want to use the http root object):

```
activiti.client.extension.endpoint=http://<activiti-server>:<activiti-port>/activiti-app
activiti.client.extension.user=<activiti-user>
activiti.client.extension.password=<activiti-password>
```

## http
This js root object allows to call remote HTTP API. In particular it has the following public methods:
```java
- public String get(String urlString)
- public InputStream getStream(String urlString)
- public String get(String urlString, String user, String password)
- public InputStream getStream(String urlString, String name, String password) 
- public String post(String urlString, String content,String contentType, String user, String password)
- public InputStream postStream(String urlString, String content,String contentType, String name, String password)
- public String execute(String urlString, String content,String contentType, String httpMethod, String user, String password)
- public InputStream executeGetStream(String urlString, String content,String contentType, String httpMethod, String name, String password)
```

The content variable on methods before refers to the payload of the request.

The content type would be for example for JSON REST API "application/json".

The httpMethod would be for those cases not covered by the GET and POST methods before (example: PUT, DELETE, etc).
In case the arguments don't apply to your call (in general this may happen for content, contentType, user, password) just pass null.

## activiti
This js root object allows to start workflows on a remote instance of Activiti Enterprise standalone. In particular it has the following public methods:

```java
- public ScriptableObject startProcess(String processName, String name, ScriptableObject scriptableObject)
- public ScriptableObject startDocumentProcess(String processName, String name, ScriptableObject scriptableObject,
	        String[] documentPropertyNames, ScriptNode[] documents)
- public void saveLocalDocument(int activitiContentId, String documentName, ScriptNode parent,String mimeType)
```

We leverage the process name (for example 'my workflow') and not the process id (for example 'my workflow:12:12593') cause we find it in general much more useful and adapted to the real usage, where javascript rules should remain unchanged even if our workflow suffers modifications for example. So internally the implementation searches for the actual process definition deployed with the given process name. the first one found is the one used when calling the start of the workflow.

The name variable is just the name you want to use for your process instance.

The ScriptableObject argument would be just any json containing the values for your workflow start form or that should be passed as variable for your workflow when starting. It will match the values property of the final payload passed to the Activiti Enterprise REST API.

The ScriptableObject returned by the method would correspond again to a json object containing the response received from Activiti and including details about the process instance started. For example:

```javascript
{"tenantId": "tenant_1", "processDefinitionVersion": 12, "startedBy": {"id": 1000, "lastName": "Fernandes", "email": null, "firstName": "Rui", "externalId": "rui"}, "startFormDefined": false, "businessKey": null, "processDefinitionCategory": "http:\/\/www.activiti.org\/processdef", "ended": null, "variables": [], "id": "12612", "processDefinitionDescription": null, "processDefinitionKey": "FOI-1", "processDefinitionDeploymentId": "12580", "name": "test", "started": "2016-02-13T02:03:57.932+0000", "processDefinitionName": "FOI-1", "graphicalNotationDefined": true, "processDefinitionId": "FOI-1:12:12593"}
```

An example of a script using the activiti js root object:

```javascript
var obj=eval('({"name":"'+document.properties['foi:number']+'"})');
activiti.startProcess("FOI-1",document.name,obj);
```

In case of the second method allows to start a workflow for a certain document. This would correspond to the last document argument passed to the method. It also expects to be specified which field would correspond to the attched document on the workflow start form. This is specified by the argument documentPropertyName with the name of the field.

The final method is meant to save inside Alfresco a document generated and living in activiti under a certain content id. It's meant typically to be used for example in case of document manipulations happening in activiti (documents created from templates or from merges for example).

Example:

```javascript
var obj=eval('({"name":"'+document.properties['foi:number']+'"})');
activiti.startDocumentProcess("FOI-1",document.name,obj,["documents"],[document]);
```
