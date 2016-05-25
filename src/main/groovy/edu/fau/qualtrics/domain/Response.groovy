package edu.fau.qualtrics.domain

import edu.fau.qualtrics.services.ConfigurationManager
import edu.fau.qualtrics.services.HttpClient
import edu.fau.qualtrics.util.RESTPaths
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST

/**
 * Created by jason on 5/25/16.
 */
class Response {
    public static final int ONE_SECOND_IN_MILLISECONDS = 1000
    public static final int FIVE_MINUTES_IN_MILLISECONDS = 300000
    final int BUFFER = 2048;
    public static final String TEMP_DIRECTORY_NAME = "QualtricsLibrary"
    RESTPaths paths
    HttpClient httpClient
    String httpStatus
    CompositeConfiguration config
    def data
    def params = [:]
    String token
    String surveyId
    Boolean operationTimedOut
    Integer operationPercentage
    String downloadFileURI
    def json

    Response(String surveyId, def params = [:], String token = null) {
        try {
            config = ConfigurationManager.getConfig()
        }
        catch (Exception e) {
            e.printStackTrace()
            println "Error loading config: " + e.message
        }

        paths = new RESTPaths()
        httpClient = new HttpClient(config.getString("qualtrics.baseURL", paths.baseUrl))
        this.token = token ?: config.getString("qualtrics.token")
        this.surveyId = surveyId
        this.params = params
    }

    def getJson() {
        String filePath = createTempDirectory().absolutePath + "/" + UUID.randomUUID() + ".zip"
        if (exportZip(ExportTypes.JSON, filePath)) {
            FileInputStream fis = new FileInputStream(filePath);

            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;
            // while there are entries I process them
            if ((entry = zis.getNextEntry()) != null) {
//                System.out.println("entry: " + entry.getName() + ", " + entry.getSize());
                // consume all the data from this entry
                json = new JsonSlurper().parse(zis)
//                while (zis.available() > 0) {
//                    zis.read();
//                }
                // I could close the entry, but getNextEntry does it automatically
                // zis.closeEntry()
            }
            fis.close()
            File file = new File(filePath)
            file.delete()
        }

        return json
    }

    /**
     *
     * @param format
     * @param filePath
     * @param millisecondsTimeout - defaults to 5 minutes, 300000 milliseconds
     * @return Boolean
     */
    def exportZip(String format, String filePath, Long millisecondsTimeout = FIVE_MINUTES_IN_MILLISECONDS) {
        operationTimedOut = false
        def path
        path = paths.getPath("response.post")
        def builder = new JsonBuilder()
        def root = builder {
            delegate.surveyId surveyId
            delegate.format format
        }
        Long startTime = System.currentTimeMillis()
        data = httpClient.http.request(POST) { req ->
            uri.path = path
            requestContentType = groovyx.net.http.ContentType.JSON
            headers['X-API-TOKEN'] = token
            body = builder.toString()

            response.success = httpClient.success
        }

        if (data && data?.meta?.httpStatus == "200 - OK") {
            String responseExportId = data?.result?.id

            Long elapsedTime = (System.currentTimeMillis() - startTime)
            while (getPercentComplete(responseExportId) != 100 && elapsedTime < millisecondsTimeout) {
                System.sleep(ONE_SECOND_IN_MILLISECONDS)
                elapsedTime = (System.currentTimeMillis() - startTime)
            }

            if (elapsedTime > millisecondsTimeout) {
                operationTimedOut = true
                return false
            }

            path = paths.getPath("response.get").toString().replace(":responseExportId", responseExportId)
            data = httpClient.http.request(GET) { req ->
                uri.path = path
                contentType = groovyx.net.http.ContentType.BINARY
                headers['X-API-TOKEN'] = token

                response.success = { resp, inputStream ->
                    File destination = new File(filePath)
                    FileUtils.copyInputStreamToFile(inputStream, destination)
                }
            }

            return true
        }

        return false
    }

    def getPercentComplete(String responseExportId) {
        def path = paths.getPath("response.progress").toString().replace(":responseExportId", responseExportId)
        data = httpClient.http.request(GET) { req ->
            uri.path = path
            headers['X-API-TOKEN'] = token

            response.success = httpClient.success
        }

        if (data && data?.meta?.httpStatus == "200 - OK") {
            if (data?.result?.percentComplete == null) {
                throw new Exception("Qualtrics response wasn't formatted correctly. Excepted format: https://api.qualtrics.com/docs/get-response-export-1")
            }
            operationPercentage = data?.result?.percentComplete
            downloadFileURI = data?.result?.file
            return operationPercentage
        }
    }

    private File createTempDirectory()
            throws IOException {
        final File temp;

        temp = File.createTempFile(TEMP_DIRECTORY_NAME, Long.toString(System.nanoTime()));

        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return (temp);
    }
}
