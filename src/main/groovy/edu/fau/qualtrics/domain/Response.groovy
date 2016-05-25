package edu.fau.qualtrics.domain

import edu.fau.qualtrics.services.ConfigurationManager
import edu.fau.qualtrics.services.HttpClient
import edu.fau.qualtrics.util.RESTPaths
import groovy.json.JsonBuilder
import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.io.FileUtils

import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST

/**
 * Created by jason on 5/25/16.
 */
class Response {
    RESTPaths paths
    HttpClient httpClient
    String httpStatus
    CompositeConfiguration config
    def data
    def params = [:]
    String token
    String surveyId

    Response(String surveyId, def params = [:], String token = null) {
        try {
            config = ConfigurationManager.getConfig()
        }
        catch (Exception e) {
            e.printStackTrace()
            println "Error loading config: " + e.message
        }

        paths = new RESTPaths()
        httpClient = new HttpClient(config.getString("qualtrics.baseURL", paths.baseUrl))
        this.token = token ?: config.getString("qualtrics.token")
        this.surveyId = surveyId
        this.params = params
    }

    def exportZip(String format, String filePath) {
        def path
        path = paths.getPath("response.post")
        def builder = new JsonBuilder()
        def root = builder {
            delegate.surveyId surveyId
            delegate.format format
        }
        data = httpClient.http.request(POST) { req ->
            uri.path = path
            requestContentType = groovyx.net.http.ContentType.JSON
            headers['X-API-TOKEN'] = token
            body = builder.toString()

            response.success = httpClient.success
        }

        if(data && data?.meta?.httpStatus == "200 - OK") {
            String responseExportId = data?.result?.id

            path = paths.getPath("response.get").toString().replace(":responseExportId", responseExportId)
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                contentType = groovyx.net.http.ContentType.BINARY
                headers['X-API-TOKEN'] = token

                response.success = {resp, inputStream ->
                    File destination = new File(filePath)
                    FileUtils.copyInputStreamToFile(inputStream, destination)
                }
            }

        }

    }
}
