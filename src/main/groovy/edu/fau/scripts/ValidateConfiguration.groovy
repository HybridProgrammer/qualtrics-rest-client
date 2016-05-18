package edu.fau.scripts

import edu.fau.services.ConfigurationManager
import org.apache.commons.configuration.CompositeConfiguration

/**
 * Created by jason on 1/26/16.
 */

CompositeConfiguration configuration = ConfigurationManager.getConfig()

assert configuration.getString("program.name", "empty"), "Qualtrics"
assert configuration.getInt("resources.id", 0), 1

println "Passed!"