package edu.fau.domain

/**
 * Created by jason on 5/18/16.
 */
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


    Distribution() {

    }

    Distribution(Map map) {
        hydrateData(map)
    }

    def setCreatedDate(String date) {
        if(!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.createdDate = calendar.getTime()
    }

    def setSendDate(String date) {
        if(!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.sendDate = calendar.getTime()
    }

    def setModifiedDate(String date) {
        if(!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.modifiedDate = calendar.getTime()
    }

    def setHeaders(def stats) {
        this.headers = new Headers(stats)
    }

    public Map asMap() {
        this.class.declaredFields.findAll { !it.synthetic }.collectEntries {
            [ (it.name):this."$it.name" ]
        }
    }

    private void hydrateData(Map map) {
        metaClass.setProperties(this, map.findAll { key, value ->
            this.hasProperty(key)
        })
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
