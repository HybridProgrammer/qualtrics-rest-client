package edu.fau.qualtrics.domain

/**
 * Created by jason on 5/18/16.
 */
class Survey {
    String id
    String name
    String ownerId
    String organizationId
    Date creationDate
    Date lastModified
    Boolean isActive
    Questions questions
    ExportColumnMap exportColumnMap
    Blocks blocks
    Flow flow
    EmbeddedData embeddedData
    ResponseCounts responseCounts

    Survey(Map map) {
        hydrateData(map)
    }

    def setcreationDate(Date date) {
        this.creationDate = date
    }
    def setCreationDate(String date) {
        if(!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.creationDate = calendar.getTime()
    }

    def setLastModifiedDate(Date date) {
        this.lastModified = date
    }
    def setLastModifiedDate(String date) {
        if(!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.lastModified = calendar.getTime()
    }
    def getLastModifiedDate() {
        this.lastModified
    }

    def setLastModified(Date date) {
        this.lastModified = date
    }
    def setLastModified(String date) {
        if(!date || date == "null") return

        final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(date)
        this.lastModified = calendar.getTime()
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
        return "Survey{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", creationDate=" + creationDate +
                ", lastModified=" + lastModified +
                ", isActive=" + isActive +
                ",\r\n\t questions=" + questions +
                ",\r\n\t exportColumnMap=" + exportColumnMap +
                ",\r\n\t blocks=" + blocks +
                ",\r\n\t flow=" + flow +
                ",\r\n\t embeddedData=" + embeddedData +
                ",\r\n\t responseCounts=" + responseCounts +
                '}';
    }
}
