package edu.fau.domain

/**
 * Created by jason on 5/19/16.
 */
class Recipients {
    String mailingListId
    String contactId
    String libraryId
    String sampleId

    Recipients(Map map) {
        hydrateData(map)
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
        return "Recipients{" +
                "mailingListId='" + mailingListId + '\'' +
                ", contactId='" + contactId + '\'' +
                ", libraryId='" + libraryId + '\'' +
                ", sampleId='" + sampleId + '\'' +
                '}';
    }
}
