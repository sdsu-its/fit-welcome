package edu.sdsu.its.fit_welcome;

import edu.sdsu.its.fit_welcome.Models.User;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Generate Non-Static HTML Pages
 *
 * @author Tom Paulus
 *         Created on 12/11/15.
 */
public class Pages {
    public static final String WELCOME = "public/welcome.html";
    public static final String MEET_ID = "public/id_select.html";
    public static final String CONFIRMATION = "public/conf.html";
    public static final String APPT_FOUND = "public/appt_found.html";
    public static final String STAFF_WELCOME = "public/staff.html";
    public static final String STAFF_CONFIRMATION = "public/staff_conf.html";
    public static final String SCHEDULE = "public/schedule.html";

    public static final String ADMIN = "admin/index.html";
    public static final String REPORT_DATE_PICKER = "admin/report_date.html";
    public static final String REPORT_CONF = "admin/report_conf.html";
    public static final String ADMIN_CONF = "admin/conf.html";
    public static final String MANUAL_TIME = "admin/manual_time.html";
    public static final String NEW_USER = "admin/new_staff.html";

    public static final String UNSUBSCRIBE = "followup/unsubscribe.html";
    public static final String SUBSCRIBE = "followup/subscribe.html";

    public static final String FORBIDDEN = "403.html";
    public static final String NOT_FOUND = "404.html";

    public static String makePage(final String pageLocation, final HashMap<String, String> fillParams) {
        Logger.getLogger(Pages.class).debug(String.format("Merging Params %s into %s", fillParams.keySet().toString(), pageLocation));

        return merge(readFile(pageLocation), fillParams);

    }

    private static String merge(final String mergeString, final HashMap<String, String> mergeParams) {
        String mergedString = mergeString;
        for (String name : mergeParams.keySet()) {
            mergedString = mergedString.replace(String.format("{{%s}}", name), mergeParams.get(name));
        }

        return mergedString;
    }

    private static String readFile(final String path) {
        InputStream inputStream = Pages.class.getClassLoader().getResourceAsStream(path);
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    public static String arrayToList(final User[] users) {
        String html = "";

        for (User user : users) {
            html += String.format("<option value=\"%d\">%s, %s</option>\n", user.id, user.lastName, user.firstName);
        }

        return html;
    }

    public static String arrayToButtons(final User[] users) {
        String html = "";

        for (int u = 0; u < users.length; u += 2) {
            html += " <tr>\n";
            for (int i = 0; i < 2; i++) {
                if (users.length > u + i) {
                    html += String.format("<td>\n" +
                            "                            <button class=\"panelButton\" type=\"submit\" name=\"host\" value=\"%s\">%s\n" +
                            "                            </button>\n" +
                            "                        </td>", users[u + i].id, users[u + i].firstName);
                }
            }
            html += " </tr>\n";
        }

        return html;
    }
}
