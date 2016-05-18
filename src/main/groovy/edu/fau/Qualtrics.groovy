package edu.fau

import edu.fau.services.ConfigurationManager
import org.apache.commons.configuration.CompositeConfiguration


def cli = new CliBuilder(usage: "java -jar Qualtrics-all-1.0.jar [options]", header: "Options")
cli.h(longOpt: "help", "print this message")
cli.csv("generate a csv file for campus, buildings and rooms")
cli.l(longOpt: "link", args: 1, argName:"WorkdayId,famisId", "Change the link between Workday and FAMIS data. Must be used with -t, --type flag.")
cli.t(longOpt: "type", args: 1, argName:"campus, building, room", "Used to specify what kind of link to create.")
cli.v(longOpt: "validate", "Verifies all rooms match their equivalent building according to FAMIS")

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
