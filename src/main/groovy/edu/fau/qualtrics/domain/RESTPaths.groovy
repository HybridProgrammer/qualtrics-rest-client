package edu.fau.qualtrics.domain

import edu.fau.qualtrics.services.ConfigurationManager
import org.apache.commons.configuration.CompositeConfiguration

/**
 * Copyright 2013 Jason Heithoff
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * User: jason
 * Date: 5/17/16
 * Time: 10:27 PM
 *
 */
class RESTPaths {
    def baseUrl
    def basePath = "/API"
    def paths = [:]
    CompositeConfiguration config

    RESTPaths() {
        try {
            config = ConfigurationManager.getConfig()
        }
        catch (Exception e) {
            e.printStackTrace()
            println "Error loading config: " + e.message
        }

        baseUrl = config.getString("qualtrics.baseURL", "https://fau.qualtrics.com")

        paths["organizations"] = basePath + "/v3/organizations/:organizationId"
        paths["user.api.token"] = basePath + "/v3/users/:userId/apitoken"
        paths["user.list"] = basePath + "/v3/users"
        paths["user.get"] = basePath + "/v3/users/:userId"
        paths["distribution.list"] = basePath + "/v3/distributions"
        paths["distribution.get"] = basePath + "/v3/distributions/:distributionId"
        paths["distribution.post"] = basePath + "/v3/distributions"
        paths["libraryMessages.get"] = basePath + "/v3/libraries/:libraryId/messages"
        paths["survey.list"] = basePath + "/v3/surveys"
        paths["survey.get"] = basePath + "/v3/surveys/:surveyId"


    }

    def getPath(String key, HashMap<String, String> params) {
        if(!paths.containsKey(key)) throw new Exception("Unknown key given for path")

        def path = paths[key]
        params.each {
            path = path.toString().replace(it.key, it.value)
        }

        return path
    }

    def getPath(String key) {
        def path = paths[key]
        return path
    }
}
