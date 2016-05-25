package edu.fau.qualtrics.domain

import edu.fau.qualtrics.services.ConfigurationManager
import edu.fau.qualtrics.services.HttpClient
import org.apache.commons.configuration.CompositeConfiguration

import static groovyx.net.http.Method.GET

/**
 * Created by jason on 5/18/16.
 */
class Surveys {
    RESTPaths paths
    HttpClient httpClient
    String httpStatus
    CompositeConfiguration config
    def data
    CacheStats cacheSurveys = new CacheStats()
    CacheStats cacheSurvey = new CacheStats()
    String token
    String nextPage

    def surveys = []
    def survey

    Surveys(String token = null) {
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
        if(surveys.size() > cacheSurveys.maxObjects || cacheSurveys.hasExpired()) {
            surveys.clear()
        }
        nextPage = null
        return [hasNext: {
            index < surveys.size() || (!nextPage && index == surveys.size() && index == 0 && hydrateSurveys(true)) || (nextPage && hydrateSurveys(true))
        }, next: {
            if(index >= surveys.size()) {
                index = 0
            }

            surveys[index++]

        }] as Iterator
    }

    def getSurvey(String surveyId) {
        hydrateSurvey(surveyId)

        return survey
    }

    private void hydrateSurveyData(Map map) {
        Survey survey = new Survey(map)
        this.survey = survey
    }

    private void hydrateSurvey(String surveyId) {
        if (cacheSurvey.hasExpired()) {
            def path = paths.getPath("survey.get", [":surveyId": surveyId])
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheSurvey.updateFlashCacheTime()

            if (data) {
                hydrateSurveyData(data?.result)
                this.httpStatus = data?.httpStatus
            }
        }
    }

    private void hydreateSurveysData(def map) {
        map?.elements.each {
            Survey survey = new Survey(it)
            surveys.add(survey)
        }
    }

    private int  hydrateSurveys(boolean forceFlush) {
        if(cacheSurveys.hasExpired() || forceFlush) {
            if(surveys.size() > cacheSurveys.maxObjects || cacheSurveys.hasExpired()) {
                surveys.clear()
            }
            def path
            def query
            if(nextPage) {
                URL url = new URL(nextPage)
                path = url.getPath()
                query = convert(url.getQuery())
            }
            else {
                path = paths.getPath("survey.list")
            }
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                uri.query = query
                headers['X-API-TOKEN'] = token

                response.success = httpClient.success
            }
            cacheSurveys.updateFlashCacheTime()

            hydreateSurveysData(data?.result)
            nextPage = data?.result?.nextPage
            int i =0
        }

        return surveys.size()
    }

    public static Map<String, String> convert(String str) {
        String[] tokens = str.split("&|=");
        Map<String, String> map = new HashMap<>();
        for (int i=0; i<tokens.length-1; ) map.put(tokens[i++], tokens[i++]);
        return map;
    }

}
