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
    String httpStatus
    CompositeConfiguration config
    def data
    String userId
    CacheStats cacheAPIToken = new CacheStats()
    CacheStats cacheUsers = new CacheStats()
    CacheStats cacheUser = new CacheStats()
    String token
    String nextPage

    def users = []
    def user

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
        this.token = token ?: config.getString("qualtrics.token")
        if(cacheUsers.flushCacheInMilliseconds == 1000) {
            cacheUsers.flushCacheInMilliseconds = 600000  // 10 minutes
        }
    }

    def index = 0
    Iterator iterator() {
        index = 0
        if(users.size() > cacheUsers.maxObjects || cacheUsers.hasExpired()) {
            users.clear()
        }
        nextPage = null
        return [hasNext: {
            index < users.size() || (!nextPage && index == users.size() && index == 0 && hydrateUsers(true)) || (nextPage && hydrateUsers(true))
        }, next: {
            if(index >= users.size()) {
                index = 0
            }

            users[index++]

        }] as Iterator
    }

    def getUser(String userId) {
        hydrateUser(userId)

        return user
    }

    private void hydrateUserData(Map map) {
        User user = new User(map)
        this.user = user
    }

    private void hydrateUser(String userId) {
        if (cacheUser.hasExpired()) {
            def path = paths.getPath("user.get", [":userId": userId])
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheUser.updateFlashCacheTime()

            if (data) {
                hydrateUserData(data?.result)
                this.httpStatus = data?.httpStatus
            }
        }
    }

    private void hydreateUsersData(def map) {
        map?.elements.each {
            User user = new User(it)
            if(!users.find {it.id == user.id}) {
                users.add(user)
            }
        }
    }

    private int hydrateUsers(boolean forceFlush) {
        if(cacheUsers.hasExpired() || forceFlush) {
            if(users.size() > cacheUsers.maxObjects || cacheUsers.hasExpired()) {
                users.clear()
            }

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

        return users.size()
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
