package edu.fau.domain

import edu.fau.services.ConfigurationManager
import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.lang.time.DateUtils

/**
 * Created by jason on 5/18/16.
 */
class CacheStats {
    Date flushCacheTime
    int flushCacheInMilliseconds
    CompositeConfiguration config

    CacheStats(String configName = null) {
        try {
            config = ConfigurationManager.getConfig()
        }
        catch (Exception e) {
            e.printStackTrace()
            println "Error loading config: " + e.message
        }

        if(configName) {
            flushCacheInMilliseconds = config.getInt(configName, 1000)   // 1 second
        }
        else {
            flushCacheInMilliseconds = config.getInt("qualtrics.cache.flush.milliseconds", 1000)   // 1 second
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
