package edu.fau.qualtrics.domain

/**
 * Created by jason on 5/19/16.
 */
class SurveyLink {
    String surveyId
    Date expirationDate
    String linkType

    SurveyLink(Map map) {
        hydrateData(map)
    }

    def setExpirationDate(String date) {
        if(!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.expirationDate = calendar.getTime()
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
        return "SurveyLink{" +
                "surveyId='" + surveyId + '\'' +
                ", expirationDate=" + expirationDate +
                ", linkType='" + linkType + '\'' +
                '}';
    }
}
