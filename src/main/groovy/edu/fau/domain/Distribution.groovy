package edu.fau.domain

/**
 * Created by jason on 5/18/16.
 */
class Distribution {
    String id
    String parentDistributionId
    String ownerId

    Distribution(Map map) {
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
        return "Distribution{" +
                "id='" + id + '\'' +
                ", parentDistributionId='" + parentDistributionId + '\'' +
                ", ownerId='" + ownerId + '\'' +
                '}';
    }
}
