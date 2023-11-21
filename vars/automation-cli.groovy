def performGetResourceID(Closure c, String id, String error) {
    /**
    * This method give a closure, call it
    **/
    def (code, body) = c.call()
    json = utils.readJson(body)
    resourceID = getResourceID(json, id)
    if (resourceID == -1 || code == 404) {
        currentBuild.result = 'FAILURE'
        throw new Exception(error)
    }
    return [code, resourceID]
}

def headers() {
    /**
    * This method prepare headere list
    **/
    return 
        [
            "Authorization" : "Bearer " + "${env.TOWER_AUTH_TOKEN}",
            "Accept": "application/json",
            "User-Agent": "groovy-2.4.4",
            "Content-Type": "application/json"
        ]
}

def jobTemplateBody(inventoryID, projectID, jobTemplateName, jobTemplateDescription) {
    /**
    * This represent a body request for job_template creation API
    **/
    return """
        {
            "name": "$jobTemplateName",
            "description": "$jobTemplateDescription",
            "job_type": "run",
            "inventory": $inventoryID,
            "project": $projectID,
            "playbook": "${env.TOWER_JOB_TEMPLATE_PLAYBOOK_NAME}",
            "verbosity": 1,
            "become_enabled": true,
            "ask_variables_on_launch": true,
            "diff_mode": true
        }
    """
}

def jobTemplateLaunchBody(targetEnvironment, hostGroups, extraVars, composeBranch) {
    /**
    * This represent a body request for job_template launch API
    **/
    return """
        {
            "extra_vars": {
              "owner": "${env.TOWER_JOB_TEMPLATE_EXTRAVARS_OWNER}",
              "delivery_environment": "$targetEnvironment",
              "mvp_repository_url": "${env.COMPOSE_REPO_URL_SSH}",
              "mvp_repository_branch": "$composeBranch",
              "hosts_group": "${hostGroups}",
              "devops_script_url": "${env.DEVOPS_SCRIPT_REPO_URL_SSH}",
              "gitlab_token": "${env.GITLAB_AUTH_TOKEN}",
              "gitlab_registry": "${env.GITLAB_DOCKER_REGISTRY}",
              "gitlab_user": "${env.GITLAB_USER}",
              "env_dict_per_sector": "${extraVars.extraData.env_file}",
              "metadata": {
                "jenkins_build": "${BUILD_NUMBER}",
                "commit_author": "${extraVars.metadata.commit_author}",
                "deployed_services": "${extraVars.metadata.deployed_services}",
                "sectors_to_deploy": "${extraVars.metadata.sectors_to_deploy}"
              }
            }
        }
    """
}

def credentialBody(credentialID) {
    /**
    * This represent a body request to link credential with job_template 
    **/
    return """
        {
            "id": $credentialID
        }
    """
}

def getRequest(connection) {
    /**
    * This method perform a GET request 
    **/
    headers().keySet().each {
      connection.setRequestProperty(it, headers().get(it)) 
    }
    def (code, response) = [connection.responseCode, connection.inputStream.text]
    connection = null
    return [code, response]
}

def postRequest(connection, body) {
    /**
    * This method perform a POST request 
    **/
    headers().keySet().each {
        connection.setRequestProperty(it, headers().get(it)) 
    }
    connection.setRequestMethod("POST")
    connection.doOutput = true
    connection.outputStream.write(body.getBytes("UTF-8"))
    def (code, response) = [connection.responseCode, connection.inputStream.text]
    connection = null
    return [code, response]
}

def deletetRequest(connection) {
    /**
    * This method perform a DELETE request 
    **/
    headers().keySet().each {
      connection.setRequestProperty(it, headers().get(it)) 
    }
    connection.setRequestMethod("DELETE")
    def code = connection.responseCode
    connection = null
    return code
}

def getResourceID(list, resourceName) {
    /**
    * Retrieve a resource ID if exists, otherwise return -1
    **/
    resource = list.results.find {r ->
        r.name == resourceName
    }
    if (resource == null) {
        return -1
    }
    return resource.id
}

def getOrganizationList() {
    /**
     * This method get an Organization json object from Tower, by name-id
    **/
    url = env.TOWER_API_BASE_URL + "/organizations/"
    def connection = new URL(url).openConnection() as HttpURLConnection
    return getRequest(connection)
}

def getJobTemplateList() {
    /**
    * This method return a list of job_templates
    **/
    url = env.TOWER_API_BASE_URL + "/job_templates/"   
    def connection = new URL(url).openConnection() as HttpURLConnection
    return getRequest(connection)
}

def getProjectList() {
     /**
     * This method return a project list stored in ansible tower
     **/
    url = env.TOWER_API_BASE_URL + "/projects/"
    def connection = new URL(url).openConnection() as HttpURLConnection
    return getRequest(connection)
}

def getInventoryList() {
    /**
    * This method return a inventories list in stored in ansible tower
    **/
    url = env.TOWER_API_BASE_URL + "/inventories/"
    def connection = new URL(url).openConnection() as HttpURLConnection
    return getRequest(connection)
}

def postJobTemplate(body) {
    /**
    * This method create a Job template 
    **/
    url = env.TOWER_API_BASE_URL + "/job_templates/"
    def connection = new URL(url).openConnection() as HttpURLConnection
    return postRequest(connection, body)
}

def getJobTemplate(name) {
    /**
     * This method get an JobTemplate json object from Tower, by job id/name
    **/
    url = env.TOWER_API_BASE_URL + "/job_templates/" + name + "/"   
    def connection = new URL(url).openConnection() as HttpURLConnection
    return getRequest(connection)
}

def getCredentialList() {
    /**
    * This method return a credentials list stored in ansible tower
    **/
    url = env.TOWER_API_BASE_URL + "/credentials/"
    def connection = new URL(url).openConnection() as HttpURLConnection
    return getRequest(connection)
}

def linkCredentialToJobTemplate(body, jobTemplateID) {
    /**
    * This method link a credential to Job Template
    **/
    url = env.TOWER_API_BASE_URL + "/job_templates/" + jobTemplateID + "/" + "credentials/"
    def connection = new URL(url).openConnection() as HttpURLConnection
    return postRequest(connection, body)
}

def launchJobTemplate(id, body) {
    /**
    * This method launch a job_template with extra_vars
    **/
    url = env.TOWER_API_BASE_URL + "/job_templates/" + id + "/" + "launch/"
    def connection = new URL(url).openConnection() as HttpURLConnection
    return postRequest(connection, body)
}

def rollbackJobTemplateCreation(id) {
    /*
    * This method delete a job_template 
    **/
    url = env.TOWER_API_BASE_URL + "/job_templates/" + id + "/"
    def connection = new URL(url).openConnection() as HttpURLConnection
    return deletetRequest(connection)
}