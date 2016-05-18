package edu.fau.services

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator

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
 * Time: 10:38 PM
 *
 */
class HttpClient {
    def http
    def success

    HttpClient(String baseUrl) {
        http = new HTTPBuilder(baseUrl)

        http.handler.failure = { resp ->
            println "Unexpected failure: ${resp.statusLine}"
        }

        http.handler.success = { HttpResponseDecorator resp, reader ->
//    println("Response: ${reader}")
            return reader
        }

        success = { HttpResponseDecorator resp, reader ->
//    println("Response: ${reader}")
            return reader
        }
    }
}
