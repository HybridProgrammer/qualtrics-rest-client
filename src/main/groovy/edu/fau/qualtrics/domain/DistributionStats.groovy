package edu.fau.qualtrics.domain

/**
 * Created by jason on 5/19/16.
 */
class DistributionStats {
    int sent
    int failed
    int started
    int bounced
    int opened
    int skipped
    int finished
    int complaints
    int blocked

    DistributionStats(Map map) {
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
        return "DistributionStats{" +
                "sent=" + sent +
                ", failed=" + failed +
                ", started=" + started +
                ", bounced=" + bounced +
                ", opened=" + opened +
                ", skipped=" + skipped +
                ", finished=" + finished +
                ", complaints=" + complaints +
                ", blocked=" + blocked +
                '}';
    }
}
