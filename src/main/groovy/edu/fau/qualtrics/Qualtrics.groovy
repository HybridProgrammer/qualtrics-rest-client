package edu.fau.qualtrics

import edu.fau.qualtrics.services.ConfigurationManager
import org.apache.commons.configuration.CompositeConfiguration


def cli = new CliBuilder(usage: "java -jar qualtrics-rest-client-all-0.1.jar [options]", header: "Options")
cli.h(longOpt: "help", "print this message")

def options = cli.parse(args)

CompositeConfiguration config
try {
    config = ConfigurationManager.addConfig(System.getProperty("user.home") + "/qualtrics.properties")
}
catch (Exception e) {
    e.printStackTrace()
    println "Error loading config: " + e.message
}

QualtricsProcessor processor = new QualtricsProcessor()

processor.run(cli, options)
