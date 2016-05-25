package edu.fau.qualtrics.domain

import edu.fau.qualtrics.services.ConfigurationManager
import edu.fau.qualtrics.services.HttpClient
import edu.fau.qualtrics.util.RESTPaths
import groovy.json.JsonBuilder
import groovy.transform.AutoClone
import org.apache.commons.configuration.CompositeConfiguration

import static groovyx.net.http.Method.POST

/**
 * Created by jason on 5/18/16.
 */
@AutoClone(excludes = "config")
class Distribution {
    String id
    String parentDistributionId
    String ownerId
    String organizationId
    String requestStatus
    String requestType
    Date sendDate
    Date createdDate
    Date modifiedDate
    Headers headers
    Recipients recipients
    Message message
    SurveyLink surveyLink
    DistributionStats stats

    RESTPaths paths
    HttpClient httpClient
    String httpStatus
    CompositeConfiguration config
    def data
    String token

    Distribution() {
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

    Distribution(Map map) {
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

        hydrateData(map)
    }

    def setCreatedDate(String date) {
        if (!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.createdDate = calendar.getTime()
    }

    def setCreatedDate(Date date) {
        this.createdDate = date
    }

    def setSendDate(String date) {
        if (!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.sendDate = calendar.getTime()
    }

    def setSendDate(Date date) {
        this.sendDate = date
    }

    def setModifiedDate(String date) {
        if (!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.modifiedDate = calendar.getTime()
    }

    def setModifiedDate(Date date) {
        this.modifiedDate = date
    }

    def setHeaders(def stats) {
        this.headers = new Headers(stats)
    }

    public Map asMap() {
        this.class.declaredFields.findAll { !it.synthetic }.collectEntries {
            [(it.name): this."$it.name"]
        }
    }

    private void hydrateData(Map map) {
        metaClass.setProperties(this, map.findAll { key, value ->
            this.hasProperty(key)
        })
    }

    def save() {
        def path = paths.getPath("distribution.post")
        def builder = new JsonBuilder()
        String sDate = this.sendDate.format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("Zulu"))
        def root = builder {
            surveyLink(
                "surveyId": this.surveyLink.surveyId,
                "expirationDate": surveyLink.expirationDate.format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("Zulu")),
                "linkType": this.surveyLink.linkType
            )
            header(
                "fromEmail": this.headers.fromEmail,
                "fromName": this.headers.fromName,
                "replyToEmail": this.headers.replyToEmail,
                "subject": this.headers.subject
            )
            message(
                "libraryId": this.message.libraryId,
                "messageId": this.message.messageId,
                    "messageText": this.message.messageText
            )
            recipients(
                "mailingListId": this.recipients.mailingListId,
                    "contactId": this.recipients.contactId
            )
            sendDate sDate
        }

        println builder.toPrettyString()


        data = httpClient.http.request(POST) { req ->
            uri.path = path
            requestContentType = groovyx.net.http.ContentType.JSON
            headers['X-API-TOKEN'] = token
            body = builder.toString()

            response.success = httpClient.success
        }

        if (data) {
            this.httpStatus = data?.httpStatus
        }

        return data
    }


    @Override
    public String toString() {
        return "Distribution{" +
                "id='" + id + '\'' +
                ", parentDistributionId='" + parentDistributionId + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", requestStatus='" + requestStatus + '\'' +
                ", requestType='" + requestType + '\'' +
                ", sendDate=" + sendDate +
                ", createdDate=" + createdDate +
                ", modifiedDate=" + modifiedDate +
                ",\r\n\t headers=" + headers +
                ",\r\n\t recipients=" + recipients +
                ",\r\n\t message=" + message +
                ",\r\n\t surveyLink=" + surveyLink +
                ",\r\n\t stats=" + stats +
                '\r\n}';
    }
}
