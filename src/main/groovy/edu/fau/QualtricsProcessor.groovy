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
//        Organization organization = new Organization("fau")
//        (0..10).each {
//            println organization.getOrganizationJson()
////            println organization.stats
//        }
////        Users users = new Users()
////        (0..10).each {
////            println users.getUserToken("UR_2u7WmfdCEWLDB5z")
////        }

//        Users users = new Users()

//        users.findAll {it.divisionId == 'DV_cCH5INYYqODTYix' }.each {
//            println it
//        }

//        User user = (User) users.findAll {it.username == 'jloiacon' && it.firstName=='Joseph'}.first()
//        println user
//
//        def user2 = users.getUser(user.id)
//        println user2

//        def time1 =elapsedTime {
//            users.each {
//                println it
//            }
//        }
//
////
//        System.sleep(10000)
////
//        def time2 =elapsedTime {
//            users.each {
//                println it
//            }
//        }
//
//        println time1
//        println time2
//
        Survey survey
        Surveys surveys = new Surveys()
        surveys.each {
            println it
        }

        survey = surveys.find {it.name == "Test"}


        println "Distributions for " + survey
        Distributions distributions = new Distributions(survey.id, [distributionRequestType: "Invite"])
        distributions.each {
            println it
        }



    }


    def elapsedTime(Closure closure) {
        def timeStart = new Date()
        closure()
        def timeStop = new Date()
        TimeCategory.minus(timeStop, timeStart)
    }


}
