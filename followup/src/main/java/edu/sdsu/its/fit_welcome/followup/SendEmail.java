package edu.sdsu.its.fit_welcome.followup;


import edu.sdsu.its.fit_welcome.followup.Models.Event;
import edu.sdsu.its.fit_welcome.followup.Models.User;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Scanner;

/**
 * Send Email to user.
 *
 * @author Tom Paulus
 *         Created on 9/25/15.
 */
public class SendEmail {
    final HtmlEmail mEmail = new HtmlEmail();

    /**
     * Read file from Local File System
     * - Used to read in template files.
     *
     * @return {@link String} File contents as a String
     * @param path {@link String} File Path of file to read in
     */
    String readFile(final String path) {
        Logger.getLogger(getClass()).debug(String.format("Reading file from path %s into memory", path));
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * Make Survey Message HTML
     *
     * @return {@link String} Message HTML
     * @param firstName {@link String} Recipient's First Name
     * @param email {@link String} Recipient's Email - For Unsubscribe
     * @param date {@link String} Visit Date
     * @param eventID {@link int} ID associated with their visit
     */
    private String makeSurveyMessage(final String firstName, final String email, final String date, final int eventID) {
        String message;
        Timestamp timestamp = new Timestamp(new java.util.Date().getTime());

        final String link = Vault.getParam("fit_welcome", "followup_survey_link");
        final String max = Vault.getParam("fit_welcome", "followup_max");
        final String uLink = Vault.getParam("fit_welcome", "followup_unsubscribe");

        assert link != null;
        assert max != null;
        assert uLink != null;

        message = this.readFile("survey_email_template.html")
                .replace("{{ first }}", firstName)
                .replace("{{ date }}", date)
                .replace("{{ survey_link }}", link)
                .replace("{{ event_id }}", Integer.toString(eventID))
                .replace("{{ frequency }}", max)
                .replace("{{ generated_on_date_footer }}", timestamp.toString())
                .replace("{{ unsubscribe_link }}", uLink)
                .replace("{{ email }}", email);

        return message;
    }


    /**
     * Send Survey Email.
     *
     * @return {@link SendEmail} Instance of SendEmail
     * @param user {@link User} Guest
     */
    public SendEmail emailNotification(final User user, final Event event) {
        mEmail.setHostName(Vault.getParam("fit_email", "host"));
        final String port = Vault.getParam("fit_email", "port");
        assert port != null;
        mEmail.setSmtpPort(Integer.parseInt(port));
        mEmail.setAuthenticator(new DefaultAuthenticator(Vault.getParam("fit_email", "username"), Vault.getParam("fit_email", "password")));
        mEmail.setSSLOnConnect(Boolean.parseBoolean(Vault.getParam("fit_email", "ssl")));
        try {
            mEmail.setFrom(Vault.getParam("fit_email", "from_email"), Vault.getParam("fit_email", "from_name"));
            mEmail.setSubject("[ITS FIT Center] Service Feedback");
            mEmail.setHtmlMsg(makeSurveyMessage(user.firstName, user.email, event.date.toString("EEEE, MMMM dd, yyyy"), event.ID));

        } catch (EmailException e) {
            Logger.getLogger(getClass()).error("Problem Making Email", e);
        }

        return this;
    }


    /**
     * Send email to requester.
     *
     * @param to_email {@link String}
     */
    public void send(final String to_email) {
        try {
            Logger.getLogger(getClass()).info(String.format("Sending Email Message TO: %s", to_email));
            mEmail.addTo(to_email);
            mEmail.send();
        } catch (EmailException e) {
            Logger.getLogger(getClass()).error("Problem Sending Email", e);
        }
    }
}
