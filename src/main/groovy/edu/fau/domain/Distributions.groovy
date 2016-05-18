package edu.fau.domain

import edu.fau.services.ConfigurationManager
import edu.fau.services.HttpClient
import org.apache.commons.configuration.CompositeConfiguration

import static groovyx.net.http.Method.GET

/**
 * Created by jason on 5/18/16.
 */
class Distributions {
    RESTPaths paths
    HttpClient httpClient
    String httpStatus
    CompositeConfiguration config
    def data
    CacheStats cacheDistributions = new CacheStats()
    CacheStats cacheUser = new CacheStats()
    String token
    String nextPage

    def distributions = []
    def distribution

    Distributions(String token = null) {
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
    }

    def index = 0
    Iterator iterator() {
        index = 0
        distributions.clear()
        return [hasNext: {
            index < distributions.size() || nextPage || (!nextPage && index == distributions.size() && index == 0)
        }, next: {
            if(index >= distributions.size()) {
                if(nextPage || (index == distributions.size() && index == 0)) {
                    distributions.clear()
                    hydrateDistributions(true)
                    index = 0
                }
                else {
                    return null
                }
            }

            distributions[index++]

        }] as Iterator
    }

    def getDistribution(String distributionId) {
        hydrateDistribution(distributionId)

        return distribution
    }

    private void hydrateDistributionData(Map map) {
        Distribution distribution = new Distribution(map)
        this.distribution = distribution
    }

    private void hydrateDistribution(String distributionId) {
        if (cacheUser.hasExpired()) {
            def path = paths.getPath("distribution.get", [":distributionId": distributionId])
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheUser.updateFlashCacheTime()

            if (data) {
                hydrateDistributionData(data?.result)
                this.httpStatus = data?.httpStatus
            }
        }
    }

    private void hydreateDistributionsData(def map) {
        map?.elements.each {
            Distribution distribution = new Distribution(it)
            distributions.add(distribution)
        }
    }

    private void hydrateDistributions(boolean forceFlush) {
        if(cacheDistributions.hasExpired() || forceFlush) {
            def path
            def query
            if(nextPage) {
                URL url = new URL(nextPage)
                path = url.getPath()
                query = convert(url.getQuery())
            }
            else {
                path = paths.getPath("distribution.list")
            }
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                uri.query = query
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheDistributions.updateFlashCacheTime()

            hydreateDistributionsData(data?.result)
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
}
