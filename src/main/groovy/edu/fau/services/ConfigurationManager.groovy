package edu.fau.services

import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.configuration.SystemConfiguration
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import org.apache.commons.io.IOUtils;

/**
 * Created by jason on 1/26/16.
 */
class ConfigurationManager {

    public static final String CONFIG_FILE_NAME = "configuration.properties"
    static ConfigurationManager configurationManagerInstance
    CompositeConfiguration compositeConfiguration
    List<String> userPaths = new ArrayList<>()

    static def CompositeConfiguration getConfig() {
        if(!configurationManagerInstance) {
            configurationManagerInstance = new ConfigurationManager()
        }

        return configurationManagerInstance.compositeConfiguration
    }

    /**
     * Should only be called once per config to add a new config
     * @param path
     * @return
     */
    static def CompositeConfiguration addConfig(String path) {
        CompositeConfiguration config
        if(!configurationManagerInstance) {
            config = getConfig()
        }

        List<String> paths = configurationManagerInstance.userPaths

        if(!paths.contains(path) && config != null) {
            File userFile = new File(path)
            if (userFile.exists()) {
                PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration(userFile)
                propertiesConfiguration.setReloadingStrategy(new FileChangedReloadingStrategy());
                // auto reload config file on change
                propertiesConfiguration.setAutoSave(true)
                // changes automatically update user file
                config.addConfiguration(propertiesConfiguration);
            }
            else {
                throw new Exception("Unable to locate configuration file: " + path)
            }
        }

        return config
    }

    private ConfigurationManager() {
        compositeConfigurations()
    }

    /**
     * We load config parameters in the following order
     * Jar file: CONFIG_FILE_NAME
     * Current Directory: CONFIG_FILE_NAME
     * System Properties:
     *  i.e. java -Dprogram.name=myApp.jar -jar myApp.jar
     *
     * @throws ConfigurationException
     */
    private def compositeConfigurations() throws ConfigurationException{
        compositeConfiguration = new CompositeConfiguration();
        File file = stream2file(getClass().getResourceAsStream("/" + CONFIG_FILE_NAME))
        if(file != null) {
            compositeConfiguration.addConfiguration(new PropertiesConfiguration(file))
        }

        File userFile = new File(CONFIG_FILE_NAME)
        if(userFile.exists()) {
            PropertiesConfiguration userPropertiesConfiguration = new PropertiesConfiguration(CONFIG_FILE_NAME)
            userPropertiesConfiguration.setReloadingStrategy(new FileChangedReloadingStrategy());   // auto reload config file on change
            userPropertiesConfiguration.setAutoSave(true)                                           // changes automatically update user file
            compositeConfiguration.addConfiguration(userPropertiesConfiguration);
        }

        compositeConfiguration.addConfiguration(new SystemConfiguration());



//        System.out.println(config.getString("configuration.first"));
//        System.out.println(config.getString("java.home"));
    }


    private File stream2file(InputStream inputStream) throws IOException {
        if(inputStream == null) {
            return null
        }
        final File tempFile = File.createTempFile("stream2file", ".tmp")
        tempFile.deleteOnExit()
        FileOutputStream out
        try {
            out = new FileOutputStream(tempFile)
            IOUtils.copy(inputStream, out)
        }
        finally {
            if(out != null) {
                out.close()
            }
        }

        return tempFile
    }
}
