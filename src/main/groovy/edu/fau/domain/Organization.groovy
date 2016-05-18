package edu.fau.domain

import edu.fau.services.ConfigurationManager
import edu.fau.services.HttpClient
import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.lang.time.DateUtils

import static groovyx.net.http.Method.GET

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
 * Time: 10:25 PM
 *
 */
class Organization {
    RESTPaths paths
    HttpClient httpClient
    CompositeConfiguration config
    def data
    String organizationId
    Date flushCacheTime
    int flushCacheInMilliseconds
    String token

    Organization(String organizationId, String token = null) {
        try {
            config = ConfigurationManager.getConfig()
        }
        catch (Exception e) {
            e.printStackTrace()
            println "Error loading config: " + e.message
        }

//        config.getString("qualtrics.baseURL", "https://fau.qualtrics.com")
//        config.getString("qualtrics.token")
        paths = new RESTPaths()
        httpClient = new HttpClient(config.getString("qualtrics.baseURL", "https://fau.qualtrics.com"))
        this.organizationId = organizationId
        flushCacheInMilliseconds = config.getInt("qualtrics.organization.cache.flush.milliseconds", 1000)   // 1 second
        flushCacheTime = DateUtils.addMilliseconds(new Date(), flushCacheInMilliseconds * -1) // force flush on load
        this.token = token ?: config.getString("qualtrics.token")
    }

    def getOrganizationJson() {
        hydrate()

        return data
    }

    private void hydrate() {
        Date now = new Date()
        if(now.after(flushCacheTime)) {
            def path = paths.getPath("organizations", [":organizationId": organizationId])
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            flushCacheTime = DateUtils.addMilliseconds(new Date(), flushCacheInMilliseconds)
        }
    }
}
