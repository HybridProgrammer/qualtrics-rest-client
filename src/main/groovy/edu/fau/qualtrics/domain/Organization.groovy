package edu.fau.qualtrics.domain

import edu.fau.qualtrics.services.ConfigurationManager
import edu.fau.qualtrics.services.HttpClient
import edu.fau.qualtrics.util.CacheStats
import edu.fau.qualtrics.util.RESTPaths
import org.apache.commons.configuration.CompositeConfiguration

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
    CacheStats cacheStats
    String token

    String baseUrl
    String httpStatus
    Date creationDate
    Date expirationDate
    String id
    String name
    Stats stats


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
        cacheStats = new CacheStats("qualtrics.organization.cache.flush.milliseconds")
        this.token = token ?: config.getString("qualtrics.token")
    }

    def getOrganizationJson() {
        hydrate()
        //stats = new Stats(data?.result?.stats)

        return data
    }

    def getStats() {
        hydrate()

        return stats
    }

    def getData() {
        hydrate()

        return data
    }

    String getHttpStatus() {
        hydrate()

        return httpStatus
    }

    Date getCreationDate() {
        hydrate()

        return creationDate
    }

    Date getExpirationDate() {
        hydrate()

        return expirationDate
    }

    String getId() {
        hydrate()

        return id
    }

    String getName() {
        hydrate()

        return name
    }

    def setStats(def stats) {
        this.stats = new Stats(stats)
    }

    def setCreationDate(String date) {
        if(!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.creationDate = calendar.getTime()
    }

    def setExpirationDate(String date) {
        if(!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.creationDate = calendar.getTime()
    }

    private void hydrateData(Map map) {
        metaClass.setProperties(this, map.findAll { key, value ->
            this.hasProperty(key)
        })
    }

    private void hydrate() {
        Date now = new Date()
        if(cacheStats.hasExpired()) {
            def path = paths.getPath("organizations", [":organizationId": organizationId])
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheStats.updateFlashCacheTime()

            if(data) {
                hydrateData(data?.result)
                this.httpStatus = data?.httpStatus
            }
        }


    }
}
