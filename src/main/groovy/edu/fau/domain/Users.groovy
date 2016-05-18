package edu.fau.domain

import edu.fau.services.ConfigurationManager
import edu.fau.services.HttpClient
import jdk.nashorn.internal.codegen.Splitter
import net.sf.json.JSON
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
class Users {
    RESTPaths paths
    HttpClient httpClient
    CompositeConfiguration config
    def data
    String userId
    CacheStats cacheAPIToken = new CacheStats()
    CacheStats cacheUsers = new CacheStats()
    String token
    String nextPage

    def users = []

    Users(String token = null) {
        try {
            config = ConfigurationManager.getConfig()
        }
        catch (Exception e) {
            e.printStackTrace()
            println "Error loading config: " + e.message
        }

        paths = new RESTPaths()
        httpClient = new HttpClient(config.getString("qualtrics.baseURL", "https://fau.qualtrics.com"))
        this.userId = userId
        this.token = token ?: config.getString("qualtrics.token")
    }

    def index = 0
    Iterator iterator() {
        index = 0
        return [hasNext: {
            index < users.size() || nextPage || (!nextPage && index == users.size() && index == 0)
        }, next: {
            if(index >= users.size()) {
                if(nextPage || (index == users.size() && index == 0)) {
                    users.clear()
                    hydrateUsers(true)
                    index = 0
                }
                else {
                    return null
                }
            }

            users[index++]

        }] as Iterator
    }

    private void hydreateUsersData(def map) {
        map?.elements.each {
            User user = new User(it)
            users.add(user)
        }
    }

    private void hydrateUsers(boolean forceFlush) {
        if(cacheUsers.hasExpired() || forceFlush) {
            def path
            def query
            if(nextPage) {
                URL url = new URL(nextPage)
                path = url.getPath()
                query = convert(url.getQuery())
            }
            else {
                path = paths.getPath("user.list")
            }
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                uri.query = query
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheUsers.updateFlashCacheTime()

            hydreateUsersData(data?.result)
            nextPage = data?.result?.nextPage
            int i =0
        }
    }

    public static Map<String, String> convert(String str) {
        String[] tokens = str.split("&|=");
        Map<String, String> map = new HashMap<>();
        for (int i=0; i<tokens.length-1; ) map.put(tokens[i++], tokens[i++]);
        return map;
    }

    def getUserToken(String userId) {
        if(this.userId != userId) {
            this.userId = userId
            hydrateUserAPIToken(true)
        }
        else {
            hydrateUserAPIToken(false)
        }


        return data?.result?.apiToken
    }

    private void hydrateUserAPIToken(boolean forceFlush) {
        if(cacheAPIToken.hasExpired() || forceFlush) {
            def path = paths.getPath("user.api.token", [":userId": userId])
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheAPIToken.updateFlashCacheTime()
        }
    }
}
