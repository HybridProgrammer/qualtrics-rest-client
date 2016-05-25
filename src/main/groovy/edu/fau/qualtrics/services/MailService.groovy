package edu.fau.qualtrics.services

import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Created by jason on 2/9/16.
 */
class MailService {
    List<String> to = new ArrayList<>()
    String from = "DoNotReply@fau.edu"
    String host = "smtp.fau.edu"
    String htmlBody
    String textBody
    String subject
    Properties properties
    Boolean simulate = true

    MailService() {
        properties = new Properties()
        properties.setProperty("mail.smtp.host", host)
        subject = "Groovy Email"
        // https://support.e2ma.net/Resource_Center/Account_how-to/basic-format-for-your-own-html-emails
        // <img src='imageURL' alt="" border=0 width=1px height=1px>
        // email checker http://stackoverflow.com/questions/1018078/testing-html-email-rendering
        textBody = "Please read message by visiting http://tomcatpc.fau.edu/campaign/{campaign_id}/{hash}"
        htmlBody = """
<h1>This is actual message</h1>

<img src='http://tomcatpc.fau.edu/campaign/{campaign_id}/{hash}' alt="" border=0 width=1px height=1px>
"""
    }

    def setHost(String host) {
        this.host = host
        properties.setProperty("mail.smtp.host", host)
    }

    def send() {
        Session session = Session.getDefaultInstance(properties)

        try {
            MimeMessage message = new MimeMessage(session)
            message.setFrom(new InternetAddress(from))
            message.setSubject(subject)

            to.each {
                println("Attempting to send message to " + it)
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(it))
            }

            message.setContent(htmlBody, "text/html")

            if (!simulate) {
                Transport.send(message)
            } else {
                println "\tSimulation Mode: " + textBody
            }

            println("Sent message successfully... " + to)


        }
        catch (MessagingException mex) {
            mex.printStackTrace()
        }
    }
}
