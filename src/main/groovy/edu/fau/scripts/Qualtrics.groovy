package edu.fau.scripts


import edu.fau.services.ConfigurationManager
import groovy.json.JsonOutput
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import org.apache.commons.configuration.CompositeConfiguration

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.GET

/**
 * Created by jason on 1/11/16.s
 */

CompositeConfiguration config
try {
    config = ConfigurationManager.addConfig(System.getProperty("user.home") + "/qualtrics.properties")
}
catch (Exception e) {
    e.printStackTrace()
    println "Error loading config: " + e.message
    System.exit(-1)
}

println "Configuration successfully loaded."

def baseUrl = config.getString("qualtrics.baseURL", "https://fau.qualtrics.com")
def token = config.getString("qualtrics.token")

//def baseUrl = "https://localhost:8081"
def basePath = "/API"
def paths = [:]

//paths["login"] = basePath + "/api/PlatformServices/Login"
paths["organizations"] = basePath + "/v3/organizations/:organizationId"

def http = new HTTPBuilder(baseUrl)
//http.auth.basic(user,pass)

http.handler.failure = { resp ->
    println "Unexpected failure: ${resp.statusLine}"
}

http.handler.success = { HttpResponseDecorator resp, reader ->
//    println("Response: ${reader}")
    return reader
}

def success = { HttpResponseDecorator resp, reader ->
//    println("Response: ${reader}")
    return reader
}


//// Login
//def postBody = [
//        "UserName": user,
//        "Password": pass
//]
//data = http.request(POST) { req ->
//    uri.path = paths["login"]
//    requestContentType = JSON
////    headers['Authorization'] =
////            "Basic ${(user +":" + pass).bytes.encodeBase64().toString()}"
//    body = postBody
//
//
//    response.success = success
//}

//FamisOAuth2Metadata oAuth2 = new FamisOAuth2Metadata()
//println(data.Result)
//oAuth2.accessToken = data.Item["access_token"]
//oAuth2.refreshToken = data.Item["refresh_token"]
//oAuth2.tokenType = data.Item["token_type"]
//oAuth2.expiresIn = new Integer(data.Item["expires_in"].toString())
//oAuth2.expires = new Date().parse("EEE, dd MMM yyyy H:m:s z", data.Item[".expires"].toString())
//oAuth2.issued = new Date().parse("EEE, dd MMM yyyy H:m:s z", data.Item[".issued"].toString())

// Get Organizations

def path = paths["organizations"].toString().replace(":organizationId", "fau")
println path
data = http.request(GET) { req ->
    uri.path = path
    headers['X-API-TOKEN'] = token

    response.success = success
}

println (data)
//
//println(JsonOutput.toJson(data))
//
//PrintWriter out = new PrintWriter("properties.json")
//
//out.println(JsonOutput.toJson(data))
//out.flush()


////// Save Regions
////
//data = http.request(GET) { req ->
//    uri.path = paths["regions"]
//    requestContentType = JSON
//    headers['Authorization'] = oAuth2.tokenType + " " + oAuth2.accessToken
//
//    response.success = success
//}
//
//println(JsonOutput.toJson(data))
//
//PrintWriter out = new PrintWriter("regions.json")
//
//out.println(JsonOutput.toJson(data))
//out.flush()

//// Save PropertyTypes
//
//data = http.request(GET) { req ->
//    uri.path = paths["propertyType"]
//    requestContentType = JSON
//    headers['Authorization'] = oAuth2.tokenType + " " + oAuth2.accessToken
//
//    response.success = success
//}
//
//println(JsonOutput.toJson(data))
//
//PrintWriter out = new PrintWriter("propertytypes.json")
//
//out.println(JsonOutput.toJson(data))
//out.flush()


// Save Spaces

//data = http.request(GET) { req ->
//    uri.path = paths["spaces"]
//    requestContentType = JSON
//    headers['Authorization'] = oAuth2.tokenType + " " + oAuth2.accessToken
//
//    response.success = success
//}
//
//println(JsonOutput.toJson(data))
//
//PrintWriter out = new PrintWriter("spaces.json")
//
//out.println(JsonOutput.toJson(data))
//out.flush()


//println paths["property_region"]
//data = http.request(GET) { req ->
//    uri.path = paths["property_region"]
//    requestContentType = JSON
//    headers['Authorization'] = oAuth2.getAuthrorization()
//    query:[
//            "\$top": 100,
//            "\$skip": 0
//    ]
//
//    response.success = success
//}
//
////println (data)
//
//println(JsonOutput.toJson(data))
//
//PrintWriter out = new PrintWriter("property_region.json")
//
//out.println(JsonOutput.toJson(data))
//out.flush()