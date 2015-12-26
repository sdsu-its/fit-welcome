FIT Center Welcome Console
===========================

The ITS FIT (Faculty Instructional Technology) Center is a great place for San Diego State University Faculty to learn and use Learning Technologies, like OMR machines and classroom response tools.

Tracking why Faculty come to the FIT Center can help to improve the services and support they receive. This is where the FIT Welcome Console can help. Running a web-enabled device, like a desktop or tablet, guests can check in, providing statical tracking, as well as notifications in certain cases.

We also use [Acuity Scheduling](https://acuityscheduling.com/) to manage ParScore appointments. FIT Welcome integrates with Acuity to streamline the check-in process for faculty who have already made appointments. Faculty can also schedule appointments directly from within the Welcome Console.

FIT Welcome also includes various staff tools, including...
- Clock In/Out capabilities for Hourly Employees
- Reporting Tools for User and Staff Activity


## Setup
FIT Welcome is written primarily in Java and is run using the TomCat framework. A MySQL DB is used to store user information, as well as log events and clock in/out times.

FIT Welcome also uses [Key Server](https://github.com/sdsu-its/key-server) to access credentials for various tools and services (DataBase, APIs, Email, etc.)


### DB Config
To setup the various tables that FIT Welcome uses, run the code below in the Database that will be used with FIT Welcome.

All Primary Users (Faculty, TAs, etc.) should be in the __bbusers__ Table.
__Clock__ is used to store Clock In/Out events.
__Events__ stores all check-in information, as well as any additional information that is relevant to that check-in.
__Quotes__ stores a list of quotes that are displayed on the confirmation pages. These quotes can be anything you like! (A good source for quotes is [Brainy Quote](http://www.brainyquote.com/)).
The __Staff__ stores information for all staff users.

__Important Note:__ information in the __Staff__ table has priority over information in the __bbusers__ table; this is done to allow normal users to be changed to staff users without the need to remove them from the primary users table.

```
CREATE TABLE bbusers
(
    id INT(11) PRIMARY KEY NOT NULL,
    first_name TEXT,
    last_name TEXT,
    email TEXT
);
CREATE TABLE clock
(
    id INT(9),
    time_in TIMESTAMP,
    time_out TIMESTAMP DEFAULT '0000-00-00 00:00:00' NOT NULL
);
CREATE TABLE events
(
    TIMESTAMP TIMESTAMP DEFAULT 'CURRENT_TIMESTAMP' NOT NULL,
    redid INT(9),
    action TEXT,
    params TEXT
);
CREATE TABLE quotes
(
    id INT(11) PRIMARY KEY NOT NULL,
    text TEXT,
    author TEXT
);
CREATE TABLE staff
(
    id INT(11) PRIMARY KEY NOT NULL,
    first_name TEXT,
    last_name TEXT,
    email TEXT,
    clockable TINYINT(1),
    admin TINYINT(1),
    instructional_designer TINYINT(1)
);
```


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
