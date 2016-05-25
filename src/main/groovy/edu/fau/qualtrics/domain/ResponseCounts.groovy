package edu.fau.qualtrics.domain

/**
 * Created by jason on 5/18/16.
 */
class ResponseCounts {
    int auditable
    int generated
    int deleted

    ResponseCounts(Map map) {
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
        return "ResponseCounts{" +
                "auditable=" + auditable +
                ", generated=" + generated +
                ", deleted=" + deleted +
                '}';
    }
}
