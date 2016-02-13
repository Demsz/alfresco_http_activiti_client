# alfresco_http_activiti_client

This extension to the repository adds 2 javascript root objects:
- http
- activiti

## http
This js root object allows to call remote HTTP API. In particular it has the following public methods:
- public String get(String urlString)
- public String get(String urlString, String user, String password)
- public String post(String urlString, String content,String contentType, String user, String password)
- public String execute(String urlString, String content,String contentType, String httpMethod, String user, String password)

The content variable on methods before refers to the payload of the request.
The content type would be for example for JSON REST API "application/json".
The httpMethod would be for those cases not covered by the GET and POST methods before (example: PUT, DELETE, etc).
In case the arguments don't apply to your call (in general this may happen for content, contentType, user, password) just pass null.

## activiti
This js root object allows to start workflows on a remote instance of Activiti Enterprise standalone. In particular it has the following public methods:
- public ScriptableObject startProcess(String processName, String name, ScriptableObject scriptableObject)

We leverage the process name (for example 'my workflow') and not the process id (for example 'my workflow:12:12593') cause we find it in general much more useful and adapted to the real usage, where javascript rules should remain unchanged even if our workflow suffers modifications for example. So internally the implementation searches for the actual process definition deployed with the given process name. the first one found is the one used when calling the start of the workflow.
The name variable is just the name you want to use for your process instance.
The ScriptableObject argument would be just any json containing the values for your workflow start form or that should be passed as variable for your workflow when starting. It will match the values property of the final payload passed to the Activiti Enterprise REST API.
The ScriptableObject returned by the method would correspond again to a json object containing the response received from Activiti and including details about the process instance started. For example:

{"tenantId": "tenant_1", "processDefinitionVersion": 12, "startedBy": {"id": 1000, "lastName": "Fernandes", "email": null, "firstName": "Rui", "externalId": "rui"}, "startFormDefined": false, "businessKey": null, "processDefinitionCategory": "http:\/\/www.activiti.org\/processdef", "ended": null, "variables": [], "id": "12612", "processDefinitionDescription": null, "processDefinitionKey": "FOI-1", "processDefinitionDeploymentId": "12580", "name": "test", "started": "2016-02-13T02:03:57.932+0000", "processDefinitionName": "FOI-1", "graphicalNotationDefined": true, "processDefinitionId": "FOI-1:12:12593"}



