package edu.fau

import edu.fau.domain.*
import edu.fau.services.*
//import edu.fau.util.FamisToWorkdayMapper
import groovy.time.TimeCategory
import groovyx.gpars.GParsPool
import org.apache.commons.configuration.CompositeConfiguration

import java.util.regex.Pattern

/**
 * Created by jason on 4/1/16.
 */
class QualtricsProcessor {



    def run(def cli, def options) {
        Organization organization = new Organization("fau")
        (0..10).each {
            println organization.getOrganizationJson()
        }
//        Users users = new Users()
//        (0..10).each {
//            println users.getUserToken("UR_2u7WmfdCEWLDB5z")
//        }

    }


    def elapsedTime(Closure closure) {
        def timeStart = new Date()
        closure()
        def timeStop = new Date()
        TimeCategory.minus(timeStop, timeStart)
    }


}
