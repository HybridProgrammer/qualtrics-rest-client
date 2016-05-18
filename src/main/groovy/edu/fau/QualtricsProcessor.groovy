package edu.fau

import edu.fau.domain.*
import edu.fau.services.*
import edu.fau.util.FamisToWorkdayMapper
import groovy.time.TimeCategory
import groovyx.gpars.GParsPool
import org.apache.commons.configuration.CompositeConfiguration

import java.util.regex.Pattern

/**
 * Created by jason on 4/1/16.
 */
class FamisWdSyncProcessor {

    public static final int WORKDAY_ID = 0
    public static final int FAMIS_ID = 1
    Double peakMemory = 0.0

    Boolean markUnmatchedRegionsAsInactive
    Boolean markUnmatchedPropertiesAsInactive
    Boolean markUnmatchedSpacesAsInactive
    Boolean excludeInactiveCompuses
    Boolean excludeInactiveBuildings
    Boolean excludeInactiveRooms
    Boolean processRooms
    Boolean loadRegions
    Boolean processRegions
    Boolean processCampuses
    Boolean isInterative
    Boolean isInterativeCampusMapping

    CompositeConfiguration config

    AccruentClient accruentClient
    WorkdayClient workdayClient

    LogService logService
    CsvService csvService

    def run(def cli, def options) {
        if(evaulateRuntimeFlags(options, cli)) {
            return
        }

        printMemory()

        initializeConfiguration()
        loadData("default")

        printMemory()


        runComparisonRoomsThenBuildings()
        syncWDBuildingsAndRoomsfromFamisData()
        FamisToWorkdayMapper mapper = new FamisToWorkdayMapper()
        mapper.mapCampuses(workdayClient)
        addNewBuildingsAndRooms(workdayClient, accruentClient, logService)



        println ""
        println "The following buildings are not linked and some possible reasons why"
        workdayClient.locations.findAll { location ->
            if (location.isDirty == false) {
                GParsPool.withPool {
                    Integer nLinkedRooms = 0
                    nLinkedRooms = location.subordinateLocations.findAllParallel { wdRoom -> wdRoom.integrationFAMISId != null }.size()

                    println "Workday Building[" + location.id + "]: " + location.getName()
                    println "\tNumber of Rooms Linked: " + nLinkedRooms
                    println "\tNumber of rooms: " + location.subordinateLocations.size()
                    if (location.subordinateLocations.size() > 0) {
                        println "\tPercentage of rooms linked: " + (nLinkedRooms / location.subordinateLocations.size()).toString()
                    }

                }
            }

        }

        println("Building Comparison Results:")
        println("Match Count: " + workdayClient.locations.findAll { building -> building.integrationFAMISId }.size())
        println("No Match Count: " + workdayClient.locations.findAll { building -> !building.integrationFAMISId }.size())
        println("Dirty Count: " + workdayClient.getDirtyCount(WorkdayClient.BUILDING_LOCATION_TYPE))
        println ""
        println("Room Comparison Results:")
        println("Match Count: " + workdayClient.rooms.findAll { room -> room.integrationFAMISId }.size())
        println("No Match Count: " + workdayClient.rooms.findAll { room -> !room.integrationFAMISId }.size())
        println("Dirty Count: " + workdayClient.getDirtyCount(WorkdayClient.ROOM_LOCATION_TYPE))
        println ""
        println("Campus Comparison Results:")
        println("Match Count: " + workdayClient.campuses.findAll { campus -> campus.integrationFAMISId }.size())
        println("No Match Count: " + workdayClient.campuses.findAll { campus -> !campus.integrationFAMISId }.size())
        println("Dirty Count: " + workdayClient.getDirtyCount(WorkdayClient.CAMPUS_LOCATION_TYPE))
        Thread.sleep(5000)


        updateWorkday()

    }

    private void updateWorkday() {
        WDStats wdStats = pushChangesToWorkday()


        println ""
        println "Successful Building Updates: " + wdStats.totalSuccessfulBuildingUpdates
        println "Failed Building Updates: " + wdStats.totalFailledBuildingUpdates
        println "Successful Room Updates: " + wdStats.totalSuccessfulRoomUpdates
        println "Failed Room Updates: " + wdStats.totalFailledRoomUpdates
        println "Successful Campus Updates: " + wdStats.totalSuccessfulCampusUpdates
        println "Failed Campus Updates: " + wdStats.totalFailledCampusUpdates

        printMemory()
        println "Peak Memory: " + peakMemory
    }

    private boolean evaulateRuntimeFlags(options, cli) {
        if (options.h) {
            cli.usage()
            return true
        }

        if ((options.l && !options.t) || (!options.l && options.t) || (options.l && options.t &&  !options.l.toString().contains(","))) {
            println "Error: Must include both -l and -t flags when trying to link locations."
            cli.usage()
            return true
        }

        if (options.l && options.t && options.l.toString().contains(",")) {

            String[] settings = options.l.toString().split(",")
            println "Attempting to link Workday Id: " + settings[WORKDAY_ID] + " with FAMIS Id: " + settings[FAMIS_ID]
            printMemory()

            initializeConfiguration()
            loadData(options.t.toString())

            switch (options.t.toString()) {
                case "campus":
                    println "not implemented"
                    break
                case "building":
                    FamisProperty famisProperty = accruentClient.famisPropertyById.get(settings[FAMIS_ID])
                    WDLocation building = workdayClient.locationByLocationId.get(settings[WORKDAY_ID])
                    if(!famisProperty) {
                        println "Unable to locate building in FAMIS."
                        return true
                    }
                    if(!building) {
                        println "Unable to locate buildings in Workday."
                        return true
                    }

                    // first check to see if there exists another buildings with the same integrationFAMISId
                    def match = workdayClient.locations.find {b-> b.integrationFAMISId == famisProperty.Id.toString()}
                    if(match) {
                        // id will will have a conflict during update. So will prevent that conflect
                        match.id = match.id + "_moving"
                        updateWorkday()
                        swapIntegrationIds(match, famisProperty, building)
                        if(match.integrationFAMISId) {
                            compareAndUpdateWDBuilding(accruentClient.famisPropertyById.get(match.integrationFAMISId), match)
                        }
                        else {
                            println "Don't know what I should do with this property. No matching property in FAMIS. Marking as inactive to prevent user confusion. " + match
//                            match.isDirty = false //don't update it. Leave it in the broken state. We don't know what to name it too
                            match.inactive = true
                        }

                    }
                    else {
                        building.integrationFAMISId = famisProperty.Id.toString()
                    }

                    compareAndUpdateWDBuilding(famisProperty, building)
                    updateWorkday()

                    break
                case "room":
                    FamisSpace famisSpace = accruentClient.famisSpaceById.get(settings[FAMIS_ID])
                    WDLocation room = workdayClient.roomsByLocationId.get(settings[WORKDAY_ID])
                    if(!famisSpace) {
                        println "Unable to locate room in FAMIS."
                        return true
                    }
                    if(!room) {
                        println "Unable to locate room in Workday."
                        return true
                    }

                    // first check to see if there exists another buildings with the same integrationFAMISId
                    def match = workdayClient.locations.find {b-> b.integrationFAMISId == famisSpace.Id.toString()}
                    if(match) {
                        // id will will have a conflict during update. So will prevent that conflect
                        match.id = match.id + "_moving"
                        updateWorkday()
                        swapIntegrationIds(match, famisSpace, room)
                        if(match.integrationFAMISId) {
                            compareAndUpdateWDRooms(accruentClient.famisPropertyById, accruentClient.famisSpaceById.get(match.integrationFAMISId), match)
                        }
                        else {
                            println "Don't know what I should do with this room. No matching space in FAMIS. Marking as inactive to prevent user confusion. " + match
//                            match.isDirty = false //don't update it. Leave it in the broken state. We don't know what to name it too
                            match.inactive = true
                        }

                    }
                    else {
                        room.integrationFAMISId = famisSpace.Id.toString()
                    }

                    compareAndUpdateWDRooms(accruentClient.famisPropertyById, famisSpace, room)
                    updateWorkday()
                    break
            }

            printMemory()
            return true
        }

        if (options.csv) {
            println "Generating CSV files...."
            printMemory()

            initializeConfiguration()
            loadData("default")

            printMemory()
            generateCSV()

            return true
        }

        if(options.v) {
            printMemory()

            initializeConfiguration()
            loadData("default")

            printMemory()

            def linkedBuildings = workdayClient.locations.each {it.integrationFAMISId}
            linkedBuildings.each {building->
                def linkedRooms = building.subordinateLocations.findAll {it.integrationFAMISId}
                linkedRooms.each {room->
                    def space = accruentClient.famisSpaceById.get(room.integrationFAMISId)
                    if(!space.PropertyId.equals(building.integrationFAMISId)) {
                        logService.error("Workday Room: " + room.name + " Should not be linked to Building: " + building.name)
                        logService.error("Workday Building: " + building)
                        logService.error("Workday Room: " + room)
                        logService.error("FAMIS Room: " + space)
                    }
                }

            }

            return true
        }

        return false
    }

    private void swapIntegrationIds(WDLocation match, def famis, WDLocation building) {
        match.integrationFAMISId = building.integrationFAMISId
        building.integrationFAMISId = famis.Id.toString()
    }

    private void generateCSV() {
//Campuses
        if (accruentClient.famisRegionById.size() > 0) {
            csvService.write(config.getString("output.path") + "/famis_regions.csv", accruentClient.famisRegionById.values().first().fields, accruentClient.famisRegionById.values())
        }
        if (workdayClient.campuses.size() > 0) {
            csvService.write(config.getString("output.path") + "/workday_campuses.csv", workdayClient.campuses.first().fields, workdayClient.campusByLocationId.values())
        }
        if (accruentClient.famisPropertyById.size() > 0) {
            csvService.write(config.getString("output.path") + "/famis_properties.csv", accruentClient.famisPropertyById.values().first().fields, accruentClient.famisPropertyById.values())
        }
        if (workdayClient.locations.size() > 0) {
            csvService.write(config.getString("output.path") + "/workday_buildings.csv", workdayClient.locations.first().fields, workdayClient.locationByLocationId.values())
        }
        if (accruentClient.famisSpaceById.size() > 0) {
            csvService.write(config.getString("output.path") + "/famis_spaces.csv", accruentClient.famisSpaceById.values().first().fields, accruentClient.famisSpaceById.values())
        }
        if (workdayClient.rooms.size() > 0) {
            csvService.write(config.getString("output.path") + "/workday_rooms.csv", workdayClient.rooms.first().fields, workdayClient.roomsByLocationId.values())
        }

        println "CSV files have been generated. Check file timestamps to see if anything was updated."

        return
    }

    /**
     * Loads campus, building and rooms from Workday and FAMIS into memory
     * In the case of manual linking the hierarchy is campus, building then room
     * manual linking requires that the parent data type must also be loaded to properly configure superiorlocation data
     * Therefor, if you link buildings you must also load campus; if you link rooms you must also link buildings
     * @param override
     */
    private void loadData(String override) {
        accruentClient.isTesting = false
        if ((loadRegions && override == "default") || override == "building") {
            println "Downloading FAMIS regions"
            accruentClient.getRegions()

            println "Downloading Workday campuses"
            workdayClient.getCampuses(excludeInactiveCompuses)
        }

        if(override == "default" || override == "building" || override == "room") {
            println "Downloading FAMIS properties"
            accruentClient.getProperties()

            println "Downloading Workday buildings"
            workdayClient.getBuildings(excludeInactiveBuildings)
        }

        if ((processRooms && override == "default") || override == "room") {
            println "Downloading FAMIS spaces"
            accruentClient.getSpaces()

            // Get All rooms
            println "Downloading Workday rooms"
            workdayClient.getRooms(excludeInactiveRooms)
        }
        printMemory()
    }

/**
 * Log into both Workday and Accruent clients
 * @param config
 * @return
 */
    private void LogIn(CompositeConfiguration config) {
        accruentClient = new AccruentClient(config.getString("famis.user", "oit"), config.getString("famis.pass").toCharArray())
        workdayClient = new WorkdayClient(config.getString("workday.user"), config.getString("workday.pass").toCharArray())

        workdayClient.config = config
    }

/**
 * Already established linked regions in Workday are ignored and not touched in this comparison
 * We will attempt to link regions by name when their name matches exactly and a link doesn't already exist
 * If an exact match can't be found we will print possible candidate regions for matching later
 *  Deployment Notes:
 *      - Once we establish all links we will then assume
 *          - All unlinked regions in famis are new and create them in Workday if they are active
 *          - All unlinked regions in Workday will be marked as inactive. Set markUnmatchedRegionsAsInactive to true to enable
 * @param workdayClient
 * @param accruentClient
 * @param markUnmatchedRegionsAsInactive
 */
    private void runComparisonRegions(WorkdayClient workdayClient, AccruentClient accruentClient, boolean markUnmatchedRegionsAsInactive, boolean runInteractively = false) {
        println("Comparing data: ")
        println("")

        Integer matchesCount = 0
        Integer noMatchCount = 0
        for (int i = 0; i < workdayClient.campuses.size(); i++) {
            WDLocation campus = workdayClient.campuses[i]

            // Found linked campus
            if (campus.integrationFAMISId && accruentClient.famisRegionById.get(campus.integrationFAMISId)) {
                println campus.name + " is already linked to [" + campus.integrationFAMISId + "] " + accruentClient.famisRegionById.get(campus.integrationFAMISId).getDisplayName() + " will be ignored in comparison"
                matchesCount++
//            campus.linkNotes = "Already linked"
            } // found exact match
            else if (campus.equals(accruentClient.famisRegionByName.get(campus.name)) && campus.integrationFAMISId == null) {
                println campus.name + " matches [" + accruentClient.famisRegionByName.get(campus.name).Id + "] " + accruentClient.famisRegionByName.get(campus.name).getDisplayName()
                matchesCount++
                campus.integrationFAMISId = accruentClient.famisRegionByName.get(campus.name).Id
                campus.linkNotes = "Exact campus name match"
            } else { // no matches found; attempt to find close matches


                println "Workday campus: " + campus.name + " No match found in FAMIS data"
                List<FamisRegion> candidateRegions = accruentClient.findEligibleLocations(workdayClient, true)
                if (candidateRegions.size() == 0 && markUnmatchedRegionsAsInactive) {
                    campus.inactive = true
                } else if (runInteractively && candidateRegions.size() > 0) {
                    println 'Would you like to link a campus? Choose a number or s to skip or c to continue processing and disable interactive mode.'
                    Scanner scanner = new Scanner(System.in);
                    def response = scanner.nextLine();
//                def response = System.console().readLine 'Would you like to link a campus? Choose a number or c to continue.'
                    if (response.toLowerCase() != "c" && response.toLowerCase() != "s" && response.isNumber()) {
                        Integer choice = new Integer(response)
                        if (choice >= candidateRegions.size()) {
                            println "Not one of the available options. Continuing on."
                            noMatchCount++
                        } else {
                            FamisRegion region = candidateRegions[choice]
                            campus.integrationFAMISId = region.Id
                            campus.linkNotes = "User override. Manually Linked."
                            matchesCount++
                            println "Workday Location: " + campus.getName() + " is now linked to Famis Region " + region.getDisplayName()
                        }
                    } else if (response.toLowerCase() == "c") {
                        runInteractively = false
                    } else {
                        noMatchCount++
                    }

                } else {
                    noMatchCount++
                }
            }
        }

        println("Campus Comparison Results:")
        println("Match Count: " + matchesCount)
        println("No Match Count: " + noMatchCount)
        println("Dirty Count: " + workdayClient.getDirtyCount(WorkdayClient.CAMPUS_LOCATION_TYPE))

        Thread.sleep(5000)

        println ""


        tryToLinkUnLinkedRooms(workdayClient, accruentClient)
    }

/**
 * Already established linked properties in Workday are ignored and not touched in this comparison
 * We will attempt to link properties by name when their name matches exactly and a link doesn't already exist
 * If an exact match can't be found we will print possible candidate properties for matching later
 *  Deployment Notes:
 *      - Once we establish all links we will then assume
 *          - All unlinked properties in famis are new and create them in Workday if they are active
 *          - All unlinked properties in Workday will be marked as inactive. Set markUnmatchedPropertiesAsInactive to true to enable
 * @param workdayClient
 * @param accruentClient
 * @param markUnmatchedPropertiesAsInactive
 */
    private void runComparisonRoomsThenBuildings() {
        println("Comparing data: ")
        println("")

        Integer matchesCount = 0
        Integer noMatchCount = 0
        for (int i = 0; i < workdayClient.rooms.size(); i++) {
            WDLocation room = workdayClient.rooms[i]

            // Found linked location
            if (room.integrationFAMISId && accruentClient.famisSpaceById.get(room.integrationFAMISId)) {
//            println room.name + " is already linked to [" + room.integrationFAMISId + "] " + accruentClient.famisSpaceById.get(room.integrationFAMISId).getFormattedSpaceName() + " will be ignored in comparison"
                matchesCount++
//            room.linkNotes = "Already linked"
                if (room.superiorLocation && !room.superiorLocation.integrationFAMISId) {
                    tryToLinkBuilding(room, accruentClient)
                }
            } // found exact match; must match room and contain proper room name format: i.e. has buildingID_Room roomName
            else if (room.equals(accruentClient.famisSpaceByName.get(room.name), accruentClient.famisPropertyById)
                    && room.integrationFAMISId == null
                    && accruentClient.famisSpaceByName.get(room.name).getFormattedSpaceName(accruentClient.famisPropertyById).contains("_Room ")) {
//            println room.name + " matches [" + accruentClient.famisSpaceByName.get(room.name).Id + "] " + accruentClient.famisSpaceByName.get(room.name).getFormattedSpaceName()
                matchesCount++
                room.integrationFAMISId = accruentClient.famisSpaceByName.get(room.name).Id
                room.linkNotes = "Exact room name match"

                tryToLinkBuilding(room, accruentClient)

            } else { // no matches found; attempt to find close matches
                println "Workday room: " + room.name + " No match found in FAMIS data"
                WDLocation building = workdayClient.locationByLocationId.get(room.superiorId)
                if (!building && markUnmatchedPropertiesAsInactive) {
                    room.inactive = true
                }

                List<FamisSpace> candidateSpaces = accruentClient.findCloseMatches(room, WorkdayClient.ROOM_LOCATION_TYPE, workdayClient.locationByLocationId.get(room.superiorId), true)
                if (building && candidateSpaces.size() == 0 && markUnmatchedPropertiesAsInactive) {
                    room.inactive = true
                }

                noMatchCount++
            }
        }

        println("Room Comparison Results:")
        println("Match Count: " + matchesCount)
        println("No Match Count: " + noMatchCount)
        println("Dirty Count: " + workdayClient.getDirtyCount(WorkdayClient.ROOM_LOCATION_TYPE))

        println ""

        Thread.sleep(5000)

        matchesCount = 0
        noMatchCount = 0
        for (int i = 0; i < workdayClient.locations.size(); i++) {
            WDLocation location = workdayClient.locations[i]

            // Found linked location
            if (location.integrationFAMISId && accruentClient.famisPropertyById.get(location.integrationFAMISId)) {
                println location.name + " is already linked to [" + location.integrationFAMISId + "] " + accruentClient.famisPropertyById.get(location.integrationFAMISId).getFormattedBuildingName() + " will be ignored in comparison"
                matchesCount++
//            location.linkNotes = "Already linked"
                if (location.superiorLocation && !location.superiorLocation.integrationFAMISId) {
                    tryToLinkCampus(location, accruentClient)
                }
            } // found exact match
            else if (location.equals(accruentClient.famisPropertyByName.get(location.name)) && location.integrationFAMISId == null) {
                println location.name + " matches [" + accruentClient.famisPropertyByName.get(location.name).Id + "] " + accruentClient.famisPropertyByName.get(location.name).getFormattedBuildingName()
                matchesCount++
                location.integrationFAMISId = accruentClient.famisPropertyByName.get(location.name).Id
                location.linkNotes = "Exact building name match"

                tryToLinkCampus(location, accruentClient)
            } else { // no matches found; attempt to find close matches


                println "Workday location: " + location.name + " No match found in FAMIS data"
                List<FamisProperty> candidateProperties = accruentClient.findCloseMatches(location, WorkdayClient.BUILDING_LOCATION_TYPE, null, true)
                if (candidateProperties.size() == 0 && markUnmatchedPropertiesAsInactive) {
                    location.inactive = true
                } else if (isInterative && candidateProperties.size() > 0) {
                    println 'Would you like to link a building? Choose a number or s to skip or c to continue processing and disable interactive mode.'
                    Scanner scanner = new Scanner(System.in);
                    def response = scanner.nextLine();
//                def response = System.console().readLine 'Would you like to link a building? Choose a number or c to continue.'
                    if (response.toLowerCase() != "c" && response.toLowerCase() != "s" && response.isNumber()) {
                        Integer choice = new Integer(response)
                        if (choice >= candidateProperties.size()) {
                            println "Not one of the available options. Continuing on."
                            noMatchCount++
                        } else {
                            FamisProperty property = candidateProperties[choice]
                            location.integrationFAMISId = property.Id
                            location.linkNotes = "User override. Manually Linked."
                            matchesCount++
                            println "Workday Location: " + location.getName() + " is now linked to Famis Property " + property.getFormattedBuildingName()

                            tryToLinkCampus(location, accruentClient)
                        }
                    } else if (response.toLowerCase() == "c") {
                        isInterative = false
                    } else {
                        noMatchCount++
                    }

                } else {
                    noMatchCount++
                }
            }
        }

        println("Building Comparison Results:")
        println("Match Count: " + matchesCount)
        println("No Match Count: " + noMatchCount)
        println("Dirty Count: " + workdayClient.getDirtyCount(WorkdayClient.BUILDING_LOCATION_TYPE))

        Thread.sleep(5000)

        println ""


        tryToLinkUnLinkedRooms(workdayClient, accruentClient)
    }

/**
 * find rooms that contain the exact same room number in room name and building matches in both workday and famis will linked
 * i.e. 96_Room 101 will match famis.room="101" or famis.room="69_Room 101" but not famis.room="101k"
 * @param workdayClient
 * @param accruentClient
 */
    private void tryToLinkUnLinkedRooms(WorkdayClient workdayClient, AccruentClient accruentClient) {
        List<WDLocation> linkedBuildings = workdayClient.locations.findAll { building -> building.integrationFAMISId }
        List<FamisSpace> spaces = new ArrayList<FamisSpace>(accruentClient.famisSpaceById.values())

        linkedBuildings.each { building ->
            List<WDLocation> unlinkedRooms = building.subordinateLocations.findAll { room -> !room.integrationFAMISId }
            List<FamisSpace> famisPropertySpaces = spaces.findAll { space -> space.PropertyId == building.integrationFAMISId }

            // Look for possiible linkable rooms. Rooms that contain the same building and room number. i.e. 101 will match 101 not 101k
            unlinkedRooms.each { room ->
                List<FamisSpace> matches = new ArrayList<>()
                if (room.name.contains("_Room ")) { // Format is buildingId_Room room#
                    String[] values = room.name.split("_Room ")
                    String value = ""
                    if (values.size() > 1) {
                        value = values[1]
                    } else {
                        println "The workd has ended!. Please see tryToLinkUnlinkedRooms method in Workday.groovy. room.name: " + room.name
                    }
                    Pattern pattern = Pattern.compile(value + "\$")
                    matches = famisPropertySpaces.findAll { space -> space.getName().matches(pattern) }
                } else { //try to match just the room.name
                    matches = famisPropertySpaces.findAll { space -> space.getName() == room.name }
                }

                if (matches.size() > 1) {
                    println "To many possible matches. Unable to link room"
                    matches.each {
                        println "\tWD Room[" + room.id + "] could be: famis.space[" + it.Id + "] " + it.getName()
                    }
                } else if (matches.size() == 1) { // Link it!
                    room.integrationFAMISId = matches.first().Id
                    println "WD Room[" + room.id + "] is now linked to famis.space[" + matches.first().Id + "] " + matches.first().getName()
                }
            }


        }


    }

    private void tryToLinkCampus(WDLocation building, AccruentClient accruentClient) {
//    Double linkedBuildingCountThreshold = 0.95 // 95% of the spaces have links
        Double linkedBuildingCountThreshold = 0.95 // 95% of the spaces have links

        //Check if building is linked when #property hits threshold
        if (building.superiorLocation && !building.superiorLocation.integrationFAMISId) {
            Integer nLinkedBuildings = building.superiorLocation.subordinateLocations.findAll { wdBuilding -> wdBuilding.integrationFAMISId != null }.size()

            if (building.integrationFAMISId && building.superiorLocation.subordinateLocations.size() > 0 && (nLinkedBuildings / building.superiorLocation.subordinateLocations.size()) > linkedBuildingCountThreshold) {
                FamisProperty famisProperty = accruentClient.famisPropertyById.get(building.integrationFAMISId)
                FamisPropertyRegion propertyRegion = accruentClient.famisPropertyRegionByPropertyId.get(famisProperty.Id)
                if (propertyRegion) {
                    FamisRegion region = accruentClient.famisRegionById.get(propertyRegion.RegionId)
                    building.superiorLocation.integrationFAMISId = region.Id
                    println "Workday Location: " + building.superiorLocation.getName() + " is now linked to Famis Region " + region.getName()
                    building.superiorLocation.linkNotes = "Auto Linked based on building link percentage: " + (nLinkedBuildings / building.superiorLocation.subordinateLocations.size()).toString()
                }
            }
        }
    }

    private void tryToLinkBuilding(WDLocation room, AccruentClient accruentClient) {
//    Double linkedRoomCountThreshold = 0.95 // 95% of the spaces have links
        Double linkedRoomCountThreshold = 0.25 // 95% of the spaces have links

        //Check if building is linked when #spaces hits threshold
        if (room.superiorLocation && !room.superiorLocation.integrationFAMISId) {
            GParsPool.withPool {
                Integer nLinkedRooms = room.superiorLocation.subordinateLocations.findAllParallel { wdRoom -> wdRoom.integrationFAMISId != null }.size()

                if (room.superiorLocation.subordinateLocations.size() > 0 && (nLinkedRooms / room.superiorLocation.subordinateLocations.size()) > linkedRoomCountThreshold) {
                    FamisProperty property = accruentClient.famisPropertyById.get(accruentClient.famisSpaceByName.get(room.name).PropertyId)
                    room.superiorLocation.integrationFAMISId = property.Id
                    println "Workday Location: " + room.superiorLocation.getName() + " is now linked to Famis Property " + property.getFormattedBuildingName()
                    room.superiorLocation.linkNotes = "Auto Linked based on room link percentage: " + (nLinkedRooms / room.superiorLocation.subordinateLocations.size()).toString()
                }
            }
        }
    }

/**
 * Already established linked properties in Workday are ignored and not touched in this comparison
 * We will attempt to link properties by name when their name matches exactly and a link doesn't already exist
 * If an exact match can't be found we will print possible candidate properties for matching later
 *  Deployment Notes:
 *      - Once we establish all links we will then assume
 *          - All unlinked properties in famis are new and create them in Workday if they are active
 *          - All unlinked properties in Workday will be marked as inactive. Set markUnmatchedPropertiesAsInactive to true to enable
 * @param workdayClient
 * @param accruentClient
 * @param markUnmatchedPropertiesAsInactive
 */
    private void runComparisonBuildingsThenRooms(WorkdayClient workdayClient, AccruentClient accruentClient, boolean markUnmatchedPropertiesAsInactive, boolean runInteractively = false) {
        println("Comparing data: ")
        println("")

        Integer matchesCount = 0
        Integer noMatchCount = 0
        for (int i = 0; i < workdayClient.locations.size(); i++) {
            WDLocation location = workdayClient.locations[i]

            // Found linked location
            if (location.integrationFAMISId && accruentClient.famisPropertyById.get(location.integrationFAMISId)) {
                println location.name + " is already linked to [" + location.integrationFAMISId + "] " + accruentClient.famisPropertyById.get(location.integrationFAMISId).getFormattedBuildingName() + " will be ignored in comparison"
                matchesCount++

            } // found exact match
            else if (location.equals(accruentClient.famisPropertyByName.get(location.name)) && location.integrationFAMISId == null) {
                println location.name + " matches [" + accruentClient.famisPropertyByName.get(location.name).Id + "] " + accruentClient.famisPropertyByName.get(location.name).getFormattedBuildingName()
                matchesCount++
                location.integrationFAMISId = accruentClient.famisPropertyByName.get(location.name).Id
            } else { // no matches found; attempt to find close matches
                println "Workday location: " + location.name + " No match found in FAMIS data"
                List<FamisProperty> candidateProperties = accruentClient.findCloseMatches(location, WorkdayClient.BUILDING_LOCATION_TYPE, null, true)
                if (candidateProperties.size() == 0 && markUnmatchedPropertiesAsInactive) {
                    location.inactive = true
                } else if (runInteractively && candidateProperties.size() > 0) {
                    println 'Would you like to link a building? Choose a number or s to skip or c to continue processing and disable interactive mode.'
                    Scanner scanner = new Scanner(System.in);
                    def response = scanner.nextLine();
//                def response = System.console().readLine 'Would you like to link a building? Choose a number or c to continue.'
                    if (response.toLowerCase() != "c" && response.toLowerCase() != "s" && response.isNumber()) {
                        Integer choice = new Integer(response)
                        if (choice >= candidateProperties.size()) {
                            println "Not one of the available options. Continuing on."
                            noMatchCount++
                        } else {
                            FamisProperty property = candidateProperties[choice]
                            location.integrationFAMISId = property.Id
                            matchesCount++
                            println "Workday Location: " + location.getName() + " is now linked to Famis Property " + property.getFormattedBuildingName()
                        }
                    } else if (response.toLowerCase() == "c") {
                        runInteractively = false
                    } else {
                        noMatchCount++
                    }

                } else {
                    noMatchCount++
                }
            }
        }

        println("Building Comparison Results:")
        println("Match Count: " + matchesCount)
        println("No Match Count: " + noMatchCount)
        println("Dirty Count: " + workdayClient.getDirtyCount(WorkdayClient.BUILDING_LOCATION_TYPE))

        Thread.sleep(5000)

        println ""

        matchesCount = 0
        noMatchCount = 0

        for (int i = 0; i < workdayClient.rooms.size(); i++) {
            WDLocation room = workdayClient.rooms[i]

            // Found linked location
            if (room.integrationFAMISId && accruentClient.famisSpaceById.get(room.integrationFAMISId)) {
//            println room.name + " is already linked to [" + room.integrationFAMISId + "] " + accruentClient.famisSpaceById.get(room.integrationFAMISId).getFormattedSpaceName() + " will be ignored in comparison"
                matchesCount++

            } // found exact match; must match room and building
            else if (room.equals(accruentClient.famisSpaceByName.get(room.name), accruentClient.famisPropertyById)
                    && room.integrationFAMISId == null
                    && WorkdayClient.isRoomLinkedToBuilding(room, workdayClient, accruentClient)) {
//            println room.name + " matches [" + accruentClient.famisSpaceByName.get(room.name).Id + "] " + accruentClient.famisSpaceByName.get(room.name).getFormattedSpaceName()
                matchesCount++
                room.integrationFAMISId = accruentClient.famisSpaceByName.get(room.name).Id
            } else { // no matches found; attempt to find close matches
                println "Workday room: " + room.name + " No match found in FAMIS data"
                WDLocation building = workdayClient.locationByLocationId.get(room.superiorId)
                if (!building && markUnmatchedPropertiesAsInactive) {
                    room.inactive = true
                }

                List<FamisSpace> candidateSpaces = accruentClient.findCloseMatches(room, WorkdayClient.ROOM_LOCATION_TYPE, workdayClient.locationByLocationId.get(room.superiorId), true)
                if (building && candidateSpaces.size() == 0 && markUnmatchedPropertiesAsInactive) {
                    room.inactive = true
                }

                noMatchCount++
            }
        }

        println("Room Comparison Results:")
        println("Match Count: " + matchesCount)
        println("No Match Count: " + noMatchCount)
        println("Dirty Count: " + workdayClient.getDirtyCount(WorkdayClient.ROOM_LOCATION_TYPE))

    }


    private WDStats pushChangesToWorkday() {
        println ""
        println "Processing Updates for Workday:"
        WDStats wdStats = new WDStats()
        workdayClient.locations.each {
            if (it.isDirty && it.isNew == false) {
                if (workdayClient.updateLocation(it)) {
                    if (it.integrationFAMISId) {
                        // Mark property as proccessed
                        FamisProperty famisProperty = accruentClient.famisPropertyById.get(it.integrationFAMISId)
                        famisProperty.isProccessed = true
                    }

                    println "\t" + it.name + " has been updated in Workday"
                    println "Response: " + workdayClient.responseText
                    wdStats.totalSuccessfulBuildingUpdates++
                } else {
                    println "\t" + it.name + " failed to update in Workday"
                    println "\t" + workdayClient.responseText
                    wdStats.totalFailledBuildingUpdates++
                }
//        System.exit(0)
            } else if (it.isDirty && it.isNew == true && config.getBoolean("add.new.buildings")) {
                if (workdayClient.addLocation(it)) {
                    // Mark property as proccessed
                    FamisProperty famisProperty = accruentClient.famisPropertyById.get(it.integrationFAMISId)
                    famisProperty.isProccessed = true

                    println "\t" + it.name + " has been added to Workday"
                    println "\t" + workdayClient.responseText
                    wdStats.totalSuccessfulBuildingUpdates++
                } else {
                    println "\t" + it.name + " failed to add to Workday"
                    println "\t" + workdayClient.responseText
                    wdStats.totalFailledBuildingUpdates++
                }
            }
        }

        workdayClient.rooms.each {
            if (it.isDirty && it.isNew == false) {
                if (workdayClient.updateLocation(it)) {
                    if (it.integrationFAMISId) {
                        // Mark space as proccessed
                        FamisSpace famisSpace = accruentClient.famisSpaceById.get(it.integrationFAMISId)
                        famisSpace.isProccessed = true
                    }

                    println "\t" + it.name + " has been updated in Workday"
                    println "Response: " + workdayClient.responseText
                    wdStats.totalSuccessfulRoomUpdates++
                } else {
                    println "\t" + it.name + " failed to update in Workday"
                    println "\t" + workdayClient.responseText
                    wdStats.totalFailledRoomUpdates++
                }
//        System.exit(0)
            } else if (it.isDirty && it.isNew == true && config.getBoolean("add.new.rooms")) {
                if (workdayClient.addLocation(it)) {
                    // Mark space as proccessed
                    FamisSpace famisSpace = accruentClient.famisSpaceById.get(it.integrationFAMISId)
                    famisSpace.isProccessed = true

                    println "\t" + it.name + " has been added to Workday"
                    println "Response: " + workdayClient.responseText
                    wdStats.totalSuccessfulRoomUpdates++
                } else {
                    println "\t" + it.name + " failed to add to Workday"
                    println "Response: " + workdayClient.responseText
                    wdStats.totalSuccessfulRoomUpdates++
                }
            }
        }

        workdayClient.campuses.each {
            if (it.isDirty && it.isNew == false) {
                if (workdayClient.updateLocation(it)) {
                    if (it.integrationFAMISId) {
                        // Mark space as proccessed
                        FamisRegion famisRegion = accruentClient.famisRegionById.get(it.integrationFAMISId)
                        famisRegion.isProccessed = true
                    }

                    println "\t" + it.name + " has been updated in Workday"
                    println "Response: " + workdayClient.responseText
                    wdStats.totalSuccessfulCampusUpdates++
                } else {
                    println "\t" + it.name + " failed to update in Workday"
                    println "\t" + workdayClient.responseText
                    wdStats.totalFailledCampusUpdates++
                }
//        System.exit(0)
            } else if (it.isDirty && it.isNew == true && config.getBoolean("add.new.campuses")) {
                if (workdayClient.addLocation(it)) {
                    // Mark space as proccessed
                    FamisRegion famisRegion = accruentClient.famisRegionById.get(it.integrationFAMISId)
                    famisRegion.isProccessed = true

                    println "\t" + it.name + " has been added to Workday"
                    println "Response: " + workdayClient.responseText
                    wdStats.totalSuccessfulCampusUpdates++
                } else {
                    println "\t" + it.name + " failed to add to Workday"
                    println "Response: " + workdayClient.responseText
                    wdStats.totalSuccessfulCampusUpdates++
                }
            }
        }
        return wdStats
    }

/**
 * Sync building information including spaces
 * @param wdLocations
 * @param famisProperties
 */
    def syncWDBuildingsAndRoomsfromFamisData() {
        List<WDLocation> wdBuildings = workdayClient.locations
        List<WDLocation> wdRooms = workdayClient.rooms
        HashMap<String, FamisProperty> famisProperties = accruentClient.famisPropertyById
        HashMap<String, FamisSpace> famisSpaces = accruentClient.famisSpaceById
        for (int i = 0; i < wdBuildings.size(); i++) {
            if (!wdBuildings[i].integrationFAMISId) {
                continue
            }

            WDLocation wdLocation = wdBuildings[i]
            FamisProperty famisProperty = famisProperties.get(wdLocation.integrationFAMISId)

            if (!famisProperty) { // Property was deleted from famis instead of marking it as inactive. Weird
                logService.error("Workday property is mapped to an integrationFAMISId which has a famisProperty which is null.  wdLocation.integrationFAMISId: " + wdLocation.integrationFAMISId, true)
                continue
            }
            compareAndUpdateWDBuilding(famisProperty, wdLocation)
        }

        for (int i = 0; i < wdRooms.size(); i++) {
            if (!wdRooms[i].integrationFAMISId) {
                continue
            }

            WDLocation wdLocation = wdRooms[i]
            FamisSpace famisSpace = famisSpaces.get(wdLocation.integrationFAMISId)

            if (!famisSpace) { // Space was deleted from famis instead of marking it as inactive. Weird
                logService.error("Workday property is mapped to an integrationFAMISId which has a famisSpace which is null.  wdLocation.integrationFAMISId: " + wdLocation.integrationFAMISId, true)
                continue
            }
            compareAndUpdateWDRooms(famisProperties, famisSpace, wdLocation)
        }
    }

/**
 * New rooms are defined as rooms that have buildings in Workday local data but don't have a Workday record present in the cloud
 * New buildings are defined as buildings that have campuses in Workday local data but don't have a Workday record present in the cloud
 * @param workdayClient
 * @param accruentClient
 * @param logService
 */
    def addNewBuildingsAndRooms(WorkdayClient workdayClient, AccruentClient accruentClient, LogService logService) {
        markFamisCampusesThatExistInWorkday(workdayClient, accruentClient)
        markFamisPropertiesThatExistInWorkday(workdayClient, accruentClient)
        markFamisSpacesThatExistInWorkday(workdayClient, accruentClient)

        def newBuildingsMap = accruentClient.famisPropertyById.findAll { map ->
            FamisProperty property = map.value
            FamisRegion region = accruentClient.famisPropertyRegionList.find { pr -> pr.PropertyId == property.Id && pr.region.Type == "Site" }?.region
            WDLocation campus = workdayClient.campuses.find { c -> c.integrationFAMISId == region?.Id?.toString() }

            !property.existsInWorkday && campus
        }

        addNewBuildingsToWorkday(newBuildingsMap, accruentClient, workdayClient)

        def newRoomsMap = accruentClient.famisSpaceById.findAll { map ->
            !map.value.existsInWorkday && accruentClient.famisPropertyById.get(map.value.PropertyId) && accruentClient.famisPropertyById.get(map.value.PropertyId).existsInWorkday
        }

        addNewRoomsToWorkday(newRoomsMap, accruentClient, workdayClient)
    }

    private ArrayList<FamisSpace> addNewRoomsToWorkday(Map<String, FamisSpace> newRoomsMap, accruentClient, WorkdayClient workdayClient) {
        newRoomsMap.values().each { room ->
            // We can only add rooms that have existing building in Workday
            if(workdayClient.buildingByIntegrationId.containsKey(room.PropertyId)) {
                WDLocation location = new WDLocation()
                location.isNew = true
                location.integrationFAMISId = room.Id
                location.superiorLocation = workdayClient.buildingByIntegrationId.get(room.PropertyId)
                location.superiorDescriptor = location.superiorLocation?.descriptor
                location.superiorId = location.superiorLocation?.id
                location.superiorWid = location.superiorLocation?.wid

                compareAndUpdateWDRooms(accruentClient.famisPropertyById, room, location)
                workdayClient.rooms.add(location)
            }

        }
    }

    private ArrayList<FamisSpace> addNewBuildingsToWorkday(Map<String, FamisProperty> newBuilingsMap, accruentClient, WorkdayClient workdayClient) {
        newBuilingsMap.values().each { building ->
            // We can only add rooms that have existing campuses in Workday
//            if(workdayClient.campusByIntegrationId.containsKey(building.PropertyId)) {
            FamisRegion region = accruentClient.famisPropertyRegionList.find { pr -> pr.PropertyId == building.Id && pr.region.Type == "Site" }?.region
            WDLocation campus = workdayClient.campuses.find { c -> c.integrationFAMISId == region?.Id?.toString() }
            if(campus) {
                WDLocation location = new WDLocation()
                location.isNew = true
                location.integrationFAMISId = building.Id
                location.superiorLocation = campus
                location.superiorDescriptor = location.superiorLocation?.descriptor
                location.superiorId = location.superiorLocation?.id
                location.superiorWid = location.superiorLocation?.wid

                compareAndUpdateWDBuilding(building, location)
                workdayClient.locations.add(location)
            }

        }
    }

    private List<WDLocation> markFamisSpacesThatExistInWorkday(WorkdayClient workdayClient, accruentClient) {
        workdayClient.rooms.each {
            WDLocation room = it
            FamisSpace space = accruentClient.famisSpaceById.get(room.integrationFAMISId)

            if (space) {
                space.existsInWorkday = true
            }
        }
    }

    private List<WDLocation> markFamisPropertiesThatExistInWorkday(WorkdayClient workdayClient, accruentClient) {
        workdayClient.locations.each {
            WDLocation building = it
            FamisProperty property = accruentClient.famisPropertyById.get(building.integrationFAMISId)

            if (property) {
                property.existsInWorkday = true
            }
        }
    }

    private List<WDLocation> markFamisCampusesThatExistInWorkday(WorkdayClient workdayClient, accruentClient) {
        workdayClient.campuses.each {
            WDLocation campus = it
            FamisRegion region = accruentClient.famisRegionById.get(campus.integrationFAMISId)

            if (region) {
                region.existsInWorkday = true
            }
        }
    }

/**
 * Contains the mapping between Workday and Famis buildings
 * Please see https://kb.fau.edu:8444/display/FS/FAMIS+Inbound+Workday+Integration for detailed mapping rules
 * @param famisProperty
 * @param wdLocation
 */
    private void compareAndUpdateWDBuilding(FamisProperty famisProperty, WDLocation wdLocation) {
        FamisToWorkdayMapper mapper = new FamisToWorkdayMapper()
        mapper.mapBuilding(famisProperty, wdLocation)
        mapper.mapBuildingSuperLocation(famisProperty, accruentClient, wdLocation, workdayClient, logService)
    }

/**
 * Contains the mapping between Workday and Famis spaces
 * Please see https://kb.fau.edu:8444/display/FS/FAMIS+Inbound+Workday+Integration for detailed mapping rules
 * @param famisSpace
 * @param wdLocation
 */
    private void compareAndUpdateWDRooms(HashMap<String, FamisProperty> famisPropertyById, FamisSpace famisSpace, WDLocation wdLocation) {
        FamisToWorkdayMapper mapper = new FamisToWorkdayMapper()
        mapper.mapRoom(famisPropertyById, famisSpace, wdLocation)
    }

    def printMemory() {
        int mb = 1024 * 1024;

        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();

        System.out.println("##### Heap utilization statistics [MB] #####");

        //Print used memory
        System.out.println("Used Memory:"
                + (runtime.totalMemory() - runtime.freeMemory()) / mb);

        //Print free memory
        System.out.println("Free Memory:"
                + runtime.freeMemory() / mb);

        //Print total available memory
        System.out.println("Total Memory:" + runtime.totalMemory() / mb);

        if ((runtime.totalMemory() / mb) > peakMemory) {
            peakMemory = (runtime.totalMemory() / mb)
        }

        //Print Maximum available memory
        System.out.println("Max Memory:" + runtime.maxMemory() / mb);
    }

    private CompositeConfiguration initializeConfiguration() {
        config
        try {
            config = ConfigurationManager.addConfig(System.getProperty("user.home") + "/workdaysync.properties")
        }
        catch (Exception e) {
            e.printStackTrace()
            println "Error loading config: " + e.message
            System.exit(1)
        }

        if(config) {
            println "Configuration successfully loaded."
        }
        else {
            println("Failed to load configuration.")
            System.exit(1)
        }


        logService = new LogService(this.class)
        csvService = new CsvService()

        markUnmatchedRegionsAsInactive = config.getBoolean("markUnmatchedRegionsAsInactive")
        markUnmatchedPropertiesAsInactive = config.getBoolean("markUnmatchedPropertiesAsInactive")
        markUnmatchedSpacesAsInactive = config.getBoolean("markUnmatchedSpacesAsInactive")
        excludeInactiveCompuses = config.getBoolean("excludeInactiveCompuses")
        excludeInactiveBuildings = config.getBoolean("excludeInactiveBuildings")
        excludeInactiveRooms = config.getBoolean("excludeInactiveRooms")
        processRooms = config.getBoolean("processRooms")
        loadRegions = config.getBoolean("loadRegions")
        processRegions = config.getBoolean("processRegions")
        processCampuses = config.getBoolean("processCampuses")
        this.isInterative = config.getBoolean("isInterative")
        isInterativeCampusMapping = config.getBoolean("isInterativeCampusMapping")

        if(processRegions && !loadRegions) {
            println("Configuration error: processRegions cannot be true when loadRegions is false")
            System.exit(1)
        }

        LogIn(config)

        return config
    }


    def elapsedTime(Closure closure) {
        def timeStart = new Date()
        closure()
        def timeStop = new Date()
        TimeCategory.minus(timeStop, timeStart)
    }


}
