FIT Center Welcome Console
===========================

The ITS FIT (Faculty Instructional Technology) Center is a great place for San Diego State University Faculty to learn and use Learning Technologies, like OMR machines and classroom response tools.

Tracking why Faculty come to the FIT Center can help to improve the services and support they receive. This is where the FIT Welcome Console can help. Running a web-enabled device, like a desktop or tablet, guests can check in, providing statical tracking, as well as notifications in certain cases.

We also use [Acuity Scheduling](https://acuityscheduling.com/) to manage ParScore appointments. FIT Welcome integrates with Acuity to streamline the check-in process for faculty who have already made appointments. Faculty can also schedule appointments directly from within the Welcome Console.

FIT Welcome also includes various staff tools, including...
- Clock In/Out capabilities for Hourly Employees
- Reporting Tools for User and Staff Activity


## Setup
FIT Welcome is written primarily in Java and is run using the TomCat framework to run the WebSites. A MySQL DB is used to store user information, as well as log events and clock in/out times.

__FollowUp__ is an additional optional module that is run independently as a Java Executable at the end of every day (or whenever you like) that sends a follow up email with a survey link to users who have visited within the last X days. There is also a param to limit the number of emails a user gets.
 - Param: `followup_freshness` - Within how many days to contact the users. For example a value of 3 would mean to contact those who have visited in the last 3 days.
 - Param: `followup_max` - The minimum number of days since the user was last emailed. For Example a value of 7 would mean that a user who visits every day would only get an email every 7 days.

 - ___Full Param List is below!___


FIT Welcome also uses [Key Server](https://github.com/sdsu-its/key-server) to access credentials for various tools and services (DataBase, APIs, Email, etc.)


### DB Config
To setup the various tables that FIT Welcome uses, run the code below in the Database that will be used with FIT Welcome.

All Primary Users (Faculty, TAs, etc.) should be in the __bbusers__ Table.
__Clock__ is used to store Clock In/Out events.
__Events__ stores all check-in information, as well as any additional information that is relevant to that check-in.
__Quotes__ stores a list of quotes that are displayed on the confirmation pages. These quotes can be anything you like! (A good source for quotes is [Brainy Quote](http://www.brainyquote.com/)).
The __Staff__ stores information for all staff users.
__Emails__ is used by the FollowUp module to track who was sent emails when.

__Important Note:__ information in the __Staff__ table has priority over information in the __bbusers__ table; this is done to allow normal users to be changed to staff users without the need to remove them from the primary users table.

```
CREATE TABLE bbusers
(
    id INT(9) PRIMARY KEY NOT NULL,
    first_name TEXT,
    last_name TEXT,
    email TEXT,
    send_emails TINYINT(1) DEFAULT '1'
);
CREATE TABLE clock
(
    id INT(9),
    time_in TIMESTAMP,
    time_out TIMESTAMP DEFAULT '0000-00-00 00:00:00' NOT NULL
);
CREATE TABLE events
(
    ID int(9) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    TIMESTAMP TIMESTAMP DEFAULT '0000-00-00 00:00:00' NOT NULL,
    redid INT(9),
    action TEXT,
    params TEXT
);
CREATE TABLE quotes
(
    id INT(9) PRIMARY KEY NOT NULL,
    text TEXT,
    author TEXT
);
CREATE TABLE staff
(
    id INT(9) PRIMARY KEY NOT NULL,
    first_name TEXT,
    last_name TEXT,
    email TEXT,
    clockable TINYINT(1),
    admin TINYINT(1),
    instructional_designer TINYINT(1)
);
CREATE TABLE email
(
    TIMESTAMP TIMESTAMP DEFAULT '0000-00-00 00:00:00' NOT NULL,
    ID int(9),
    TYPE TEXT
);
```

### KeyServer Setup
- `db-password` = Database Password
- `db-url` = jdbc:mysql://db_host:3306/ _replace db_host and possibly the port with your MySQL server info_
-	`db-user` = Database Username
- `followup_freshness` = The maximum amount of time since the user's last visit to email the user
-	`followup_max` = The minimum number of days between emails
-	`followup_survey_link` = Survey Link for the User, use the string `{{ event_id }}` to fill in the Event ID.
Example: [http://www.bing.com/images/search?q={{ event_id }}](http://www.bing.com/images/search?q={{ event_id }})
-	`followup_unsubscribe` = Unsubscribe Link, use `{{ email }}` to substitute the email.
Example:  [http://your_domian_with_context/pages/followup/unsubscribe?e={{ email }}](http://your_domian_with_context/pages/followup/unsubscribe?e={{ email }})

### Acuity Setup
One small addition needs to be made to the Acuity Scheduler. By default, the Client Scheduling page of Acuity is not setup to process multiple sessions in the same window. To fix this, we need to add a small snippet of code to the Confirmation Page. This can be done by enabling _Custom Conversion Tracking_ (Under Import/Export/Syncing).

Insert the following snippet. Be sure to replace `mypage` with the URL(without http or https) of the page that the scheduler iframe is embedded in.

```
var url = (window.location != window.parent.location)
    ? document.referrer
    : document.location;

if (url.indexOf('mypage') !=-1) {
    // Where mypage above is the URL, without http or https, of the page that the scheduler iframe is embedded in.
    // This prevents redirects if the page in embedded elsewhere.

    window.top.location = '../index.html';
}
```
