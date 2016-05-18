package edu.fau.domain

/**
 * Created by jason on 5/18/16.
 */
class User {

    String id
    String divisionId
    String username
    String firstName
    String lastName
    String userType
    String email
    String accountStatus

    User(Map map) {
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
        return "User{" +
                "id='" + id + '\'' +
                ", divisionId='" + divisionId + '\'' +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userType='" + userType + '\'' +
                ", email='" + email + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                '}';
    }
}
