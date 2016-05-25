package edu.fau.qualtrics.domain

import edu.fau.qualtrics.services.ConfigurationManager
import edu.fau.qualtrics.services.HttpClient
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
    CacheStats cacheDistribution = new CacheStats()
    String token
    String nextPage
    String surveyId
    def params = [:]

    def distributions = []
    def distribution

    Distributions(String surveyId, def params = [:], String token = null) {
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
        this.surveyId = surveyId
        this.params = params
    }

    def index = 0
    Iterator iterator() {
        index = 0
        if(distributions.size() > cacheDistributions.maxObjects || cacheDistributions.hasExpired()) {
            distributions.clear()
        }
        nextPage = null
        return [hasNext: {
            index < distributions.size() || (!nextPage && index == distributions.size() && index == 0 && hydrateDistributions(true)) || (nextPage && hydrateDistributions(true))
        }, next: {
            if(index >= distributions.size()) {
                index = 0
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
        if (cacheDistribution.hasExpired()) {
            def path = paths.getPath("distribution.get", [":distributionId": distributionId])
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheDistribution.updateFlashCacheTime()

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

    private int hydrateDistributions(boolean forceFlush) {
        if(cacheDistributions.hasExpired() || forceFlush) {
            if(distributions.size() > cacheDistributions.maxObjects || cacheDistributions.hasExpired()) {
                distributions.clear()
            }
            def path
            def query = [:]
            if(nextPage) {
                URL url = new URL(nextPage)
                path = url.getPath()
                query = convert(url.getQuery())
            }
            else {
                path = paths.getPath("distribution.list")
            }
            query.put("surveyId", surveyId)
            params.each {
                query.put(it.key, it.value)
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

        return distributions.size()
    }

    public static Map<String, String> convert(String str) {
        String[] tokens = str.split("&|=");
        Map<String, String> map = new HashMap<>();
        for (int i=0; i<tokens.length-1; ) map.put(tokens[i++], tokens[i++]);
        return map;
    }
}
