package edu.fau.domain

/**
 * Created by jason on 5/18/16.
 */
class Survey {
    String id
    String name
    String ownerId
    Date lastModified
    Boolean isActive

    Survey(Map map) {
        hydrateData(map)
    }

    def setLastModified(String date) {
        if(date || date == "null") return

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
                ", lastModified=" + lastModified +
                ", isActive=" + isActive +
                '}';
    }
}
