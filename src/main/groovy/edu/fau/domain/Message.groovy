package edu.fau.domain

/**
 * Created by jason on 5/19/16.
 */
class Message {
    String fromEmail
    String replyToEmail
    String fromName
    String subject

    Message(Map map) {
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
        return "Message{" +
                "fromEmail='" + fromEmail + '\'' +
                ", replyToEmail='" + replyToEmail + '\'' +
                ", fromName='" + fromName + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }
}
