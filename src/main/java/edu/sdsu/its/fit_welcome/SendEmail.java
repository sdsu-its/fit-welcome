package edu.sdsu.its.fit_welcome;

import edu.sdsu.its.Vault;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Scanner;

/**
 * Send Email to user with Reports Attached.
 *
 * @author Tom Paulus
 *         Created on 9/25/15.
 */
public class SendEmail {
    final HtmlEmail mEmail = new HtmlEmail();
    private static final String emailHost = Vault.getParam("fit_email", "host");
    private static final String emailPort = Vault.getParam("fit_email", "port");
    private static final String emailUser = Vault.getParam("fit_email", "username");
    private static final String emailPass = Vault.getParam("fit_email", "password");
    private static final String emailFromAdd = Vault.getParam("fit_email", "from_email");
    private static final String emailFromName = Vault.getParam("fit_email", "from_name");

    /**
     * Read file from Local File System
     *  - Used to read in template files.
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
     * Generate HTML Message for Report Email.
     *
     * @param firstName {@link String} Recipient's first name
     * @return {@link String} HTML Message for Sending
     */
    private String makeFileMessage(final String firstName) {
        String message;
        Timestamp timestamp = new Timestamp(new java.util.Date().getTime());

        message = this.readFile("report_email_template.html")
                .replace("{{ first }}", firstName)
                .replace("{{ generated_on_date_footer }}", timestamp.toString());

        return message;
    }

    /**
     * Send Email with Reports (Usage, Timesheet, etc.) to the requester
     *
     * @param reportType {@link String} Name of the Report that was Run
     *                   For Subject line of Email
     * @param firstName  {@link String} Recipient's first name
     * @param files      {@link File[]} Report Files to attach to the Email
     * @return {@link SendEmail} Instance of SendEmail
     */
    public SendEmail emailFile(final String reportType, final String firstName, final File[] files) {
        for (File file : files) {
            EmailAttachment attachment = new EmailAttachment();
            attachment.setPath(file.getPath());
            attachment.setDisposition(EmailAttachment.ATTACHMENT);
            attachment.setName(file.getName());

            try {
                mEmail.attach(attachment);
            } catch (EmailException e) {
                Logger.getLogger(getClass()).error("Problem Attaching Report");
            }

        }

        mEmail.setHostName(emailHost);
        assert emailPort != null;
        mEmail.setSmtpPort(Integer.parseInt(emailPort));
        mEmail.setAuthenticator(new DefaultAuthenticator(emailUser, emailPass));
        mEmail.setSSLOnConnect(Boolean.parseBoolean(Vault.getParam("fit_email", "ssl")));
        try {
            mEmail.setFrom(emailFromAdd, emailFromName);
            mEmail.setSubject("[FIT WELCOME]  " + reportType);
            mEmail.setHtmlMsg(makeFileMessage(firstName));

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
