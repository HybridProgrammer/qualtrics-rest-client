package edu.fau.domain

import edu.fau.services.ConfigurationManager
import edu.fau.services.HttpClient
import org.apache.commons.configuration.CompositeConfiguration

import static groovyx.net.http.Method.GET

/**
 * Created by jason on 5/18/16.
 */
class LibraryMessages {
    RESTPaths paths
    HttpClient httpClient
    String httpStatus
    CompositeConfiguration config
    def data
    CacheStats cacheLibraryMessages = new CacheStats()
    CacheStats cacheLibraryMessage = new CacheStats()
    String token
    String nextPage
    String libraryId
    def params = [:]

    def libraryMessages = []
    def libraryMessage

    LibraryMessages(String libraryId, def params = [:], String token = null) {
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
        this.libraryId = libraryId
        this.params = params
    }

    def index = 0
    Iterator iterator() {
        index = 0
        if(libraryMessages.size() > cacheLibraryMessages.maxObjects || cacheLibraryMessages.hasExpired()) {
            libraryMessages.clear()
        }
        nextPage = null
        return [hasNext: {
            index < libraryMessages.size() || (!nextPage && index == libraryMessages.size() && index == 0 && hydrateLibraryMessages(true)) || (nextPage && hydrateLibraryMessages(true))
        }, next: {
            if(index >= libraryMessages.size()) {
                index = 0
            }

            libraryMessages[index++]

        }] as Iterator
    }

    def getLibraryMessage(String libraryId) {
        hydrateLibraryMessage(libraryId)

        return libraryMessage
    }

    private void hydrateLibraryMessageData(Map map) {
        LibraryMessage libraryMessage = new LibraryMessage(map)
        this.libraryMessage = libraryMessage
    }

    private void hydrateLibraryMessage(String libraryMessage) {
        if (cacheLibraryMessage.hasExpired()) {
            def path = paths.getPath("libraryMessage.get", [":libraryMessage": libraryMessage])
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheLibraryMessage.updateFlashCacheTime()

            if (data) {
                hydrateLibraryMessageData(data?.result)
                this.httpStatus = data?.httpStatus
            }
        }
    }

    private void hydreateLibraryMessagesData(def map) {
        map?.elements.each {
            LibraryMessage libraryMessage = new LibraryMessage(it)
            libraryMessages.add(libraryMessage)
        }
    }

    private int hydrateLibraryMessages(boolean forceFlush) {
        if(cacheLibraryMessages.hasExpired() || forceFlush) {
            if(libraryMessages.size() > cacheLibraryMessages.maxObjects || cacheLibraryMessages.hasExpired()) {
                libraryMessages.clear()
            }
            def path
            def query = [:]
            if(nextPage) {
                URL url = new URL(nextPage)
                path = url.getPath()
                query = convert(url.getQuery())
            }
            else {
                path = paths.getPath("libraryMessages.get").toString().replace(":libraryId", libraryId)
            }
            //query.put("libraryId", libraryId)
            params.each {
                query.put(it.key, it.value)
            }
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                uri.query = query
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheLibraryMessages.updateFlashCacheTime()

            hydreateLibraryMessagesData(data?.result)
            nextPage = data?.result?.nextPage
            int i =0
        }

        return libraryMessages.size()
    }

    public static Map<String, String> convert(String str) {
        String[] tokens = str.split("&|=");
        Map<String, String> map = new HashMap<>();
        for (int i=0; i<tokens.length-1; ) map.put(tokens[i++], tokens[i++]);
        return map;
    }
}
