package edu.sdsu.its.fit_welcome.alerts;

import edu.sdsu.its.fit_welcome.alerts.Models.Staff;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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
     * @param path {@link String} File Path of file to read in
     * @return {@link String} File contents as a String
     */
    String readFile(final String path) {
        Logger.getLogger(getClass()).debug(String.format("Reading file from path %s into memory", path));
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * Make Alert Message HTML
     *
     * @param firstName {@link String} Recipient's First Name
     * @return {@link String} Message HTML
     */
    private String makeAlertMessage(final String firstName) {
        String message;
        Timestamp timestamp = new Timestamp(new java.util.Date().getTime());

        message = this.readFile("no_clock_out_email_template.html")
                .replace("{{ first }}", firstName)
                .replace("{{date}}", new SimpleDateFormat("E, MMMM dd, yyyy").format(timestamp))
                .replace("{{ generated_on_date_footer }}", timestamp.toString());

        return message;
    }


    /**
     * Send Alert Email.
     *
     * @param staff {@link Staff} Staff
     * @return {@link SendEmail} Instance of SendEmail
     */
    public SendEmail emailNotification(final Staff staff) {
        mEmail.setHostName(Vault.getParam("fit_email", "host"));
        final String port = Vault.getParam("fit_email", "port");
        assert port != null;
        mEmail.setSmtpPort(Integer.parseInt(port));
        mEmail.setAuthenticator(new DefaultAuthenticator(Vault.getParam("fit_email", "username"), Vault.getParam("fit_email", "password")));
        mEmail.setSSLOnConnect(Boolean.parseBoolean(Vault.getParam("fit_email", "ssl")));
        try {
            mEmail.setFrom(Vault.getParam("fit_email", "from_email"), Vault.getParam("fit_email", "from_name"));
            mEmail.setSubject("[ITS FIT Center] Notice of Non-Clock Out");
            mEmail.setHtmlMsg(makeAlertMessage(staff.firstName));

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
