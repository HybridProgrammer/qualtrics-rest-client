package edu.fau.qualtrics.domain

import edu.fau.qualtrics.services.ConfigurationManager
import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.lang.time.DateUtils

/**
 * Created by jason on 5/18/16.
 */
class CacheStats {
    Date flushCacheTime
    int flushCacheInMilliseconds
    int maxObjects
    CompositeConfiguration config

    CacheStats(String configPrefix = null) {
        try {
            config = ConfigurationManager.getConfig()
        }
        catch (Exception e) {
            e.printStackTrace()
            println "Error loading config: " + e.message
        }

        if(configPrefix) {
            flushCacheInMilliseconds = config.getInt(configPrefix + ".flush.milliseconds", 1000)   // 1 second
            maxObjects = config.getInt(configPrefix + ".max.objects", 1000)
        }
        else {
            flushCacheInMilliseconds = config.getInt("qualtrics.cache.flush.milliseconds", 1000)   // 1 second
            maxObjects = config.getInt("qualtrics.cache.max.objects", 1000)
        }
        flushCacheTime = DateUtils.addMilliseconds(new Date(), flushCacheInMilliseconds * -1) // force flush on load

    }

    def updateFlashCacheTime() {
        this.flushCacheTime = DateUtils.addMilliseconds(new Date(), flushCacheInMilliseconds)
    }

    def hasExpired() {
        Date now = new Date()
        return now.after(flushCacheTime)
    }
}
