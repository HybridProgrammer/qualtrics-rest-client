package edu.fau.qualtrics

import edu.fau.qualtrics.domain.*

//import edu.fau.util.FamisToWorkdayMapper
import groovy.time.TimeCategory

/**
 * Created by jason on 4/1/16.
 */
class QualtricsProcessor {



    def run(def cli, def options) {
        Organization organization = new Organization("fau")
        (0..10).each {
            println organization.getOrganizationJson()
//            println organization.stats
        }
        Users users = new Users()
        (0..10).each {
            println users.getUserToken("UR_2u7WmfdCEWLDB5z")
        }

//        Users users = new Users()
        users.each {
                println it
            }

        users.findAll {it.divisionId == 'DV_cCH5INYYqODTYix' }.each {
            println it
        }

        User user = (User) users.findAll {it.username == 'jheithof@fau.edu#fau' && it.firstName=='Jason'}.first()
        println user

        LibraryMessages libraryMessages = new LibraryMessages(user.id)
        LibraryMessage libraryMessage = libraryMessages.find {it.description == "test message"}
        libraryMessages.each {
            println it
        }

        def user2 = users.getUser(user.id)
        println user2

        def time1 =elapsedTime {
            users.each {
                println it
            }
        }

//
        System.sleep(10000)
//
        def time2 =elapsedTime {
            users.each {
                println it
            }
        }

        println time1
        println time2

        System.sleep(10000)

//
        Survey survey
        Surveys surveys = new Surveys()
        surveys.each {
            println it
        }

        survey = surveys.find {it.name == "Test"}
        survey = surveys.getSurvey(survey.id)
        println survey


        println "Distributions for " + survey
        Distributions distributions = new Distributions(survey.id, [distributionRequestType: "Invite"])
        Distribution distribution
        distributions.each {
            println it
            distribution = it
        }
//
//        distribution.headers.subject += "."
//        distribution.message.libraryId = user.id
//        distribution.message.messageId = libraryMessage.id
//        distribution.sendDate = DateUtils.addHours(new Date(), 2) // schedule 2 hours from now
//        distribution.surveyLink.expirationDate = (new Date() + 1) // expire 1 day from now
//        println distribution.save()

    }


    def elapsedTime(Closure closure) {
        def timeStart = new Date()
        closure()
        def timeStop = new Date()
        TimeCategory.minus(timeStop, timeStart)
    }


}