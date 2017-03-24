# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [2.5.0] - 2017-03-24 -  _TBA_
### Added
 - The Users Table in the DB can now be synced with Blackboard to ensure an
 accurate dataset is available at all times. This replaces the update script
 that relied on a CSV file containing all of the user's information.

### Changed
 - Added `bb-api-user-sync` secret and added `bb-API-secret`, `bb-url`,
 `syncEnable`, and `syncFrequency` fields to main app secret.
 - DB Schema has been adapted to accommodate Bb Sync
```
CREATE TABLE dsk (
  `PK`   INT AUTO_INCREMENT PRIMARY KEY,
  `name` TEXT
);

INSERT INTO dsk (`name`) VALUES ('EXTERNAL');

CREATE TABLE department (
  `PK`   INT AUTO_INCREMENT PRIMARY KEY,
  `name` TEXT
);

ALTER TABLE users
  ADD COLUMN `dsk` INT,
  ADD FOREIGN KEY (`dsk`) REFERENCES dsk (`PK`),
  ADD COLUMN `department` INT,
  ADD FOREIGN KEY (`department`) REFERENCES department (`PK`),
  ADD COLUMN `updated` TIMESTAMP;

UPDATE users
SET dsk = (SELECT PK
           FROM dsk
           WHERE name = 'EXTERNAL');

ALTER TABLE events
  ADD FOREIGN KEY (`redid`) REFERENCES users (`id`);
```

## [2.4.1] - 2016-12-22 - _Kane_
### Changed
 - Switched the Live Dashboard from frequent polling to Server Sent Events (SSE). This reduced both server and DB load, as well as reduces network traffic.
 - Optimized Vault Token renewal. The token expiry time is tracked and the token is only renwed before it expires. This significantly improves load times.
 - UI improvements to Live dashboard.

### Fixed
- Issue [FW-6](http://morden.sdsu.edu:9000/issue/FW-6) Fixed Live Page styling which was broken when various CSS elements were moved to a commons file, `common.css`.

## [2.4.0] - 2016-12-14 - _Harper_
### Added
 - Different Kiosks can different displays via Locales. The locale is set via a URL param of the format `?locale=LOCALE`, and is
 saved in a cookie to ensure consistency after refreshes. Different App Logos, Titles, and Sitemaps can be defined for each locale.
 - Added option to filter locales when exporting events.

### Changed
 - Migrated JS Cookies Lib from a home-built version, to [js.cookie](https://github.com/js-cookie/js-cookie).
 - The DB schema for the events table has been changed to accommodate for Kiosk's Locale. Run the following SQL command in your DB to add the table, and set the locale for past events.
 ```
 ALTER TABLE events ADD COLUMN (locale TEXT);
 UPDATE events SET locale = '<LOCALE>';
 ```
### Fixed
 - Issue [FW-5](http://morden.sdsu.edu:9000/issue/FW-5) - Escaped Appointment Name in Appointment Found View.

## [2.3.1] - 2016-11-04 - _Pasco_
### Fixed
 - Issue [#33](https://bitbucket.org/sdsu-its/fit-welcome/issues/33/http-status-500) - Improved Logging for Errors in regard to the Database Connections.

## [2.3.0] - 2016-11-04 - _Rockcastle_
### Added
 - Added Vault Driver to manage sensitive keys, like passwords, database credentials, etc. With this, new Environment Variables are needed. For information on how to configure Vault and AppRoles, take a look at our Online Documentation: https://sdsu-its.gitbooks.io/vault/content/
 - New Environment Variables:
  + `VAULT_ADDR` - Vault Address (Ex. `https://127.0.0.1:8200`)
  + `VAULT_ROLE` - Vault AppRole ID
  + `VAULT_SECRET` - Vault AppRole Secret ID corresponding to the provided Role ID.

### Changed
 - All Params are now pulled from the Vault, not KeyServer. Tests and Accessors have been changed to reflect this.

### Removed
 - Because of the change to Vault, the KeyServer Environment Variables (`KSPATH` and `KSKEY`) are no needed for the operation of FIT Welcome. However, they still may be used for other older applications. See **Added** for the new Environment Variables.

### Fixed
 - Issue [#32](https://bitbucket.org/sdsu-its/fit-welcome/issues/32/staff-cannot-clock-in-if-fuzzy-name) - Staff Logins take priority over appointments. Issue was preventing staff from clocking in, if an appointment had a fuzzy match.

## [2.2.1] - 2016-10-24 - _Sangamon_
### Added
 - On Init Class to create a new Admin User and Dr-register the DB Driver on Shutdown

## [2.2.0] - 2016-09-30 - _Sanders_
### Added
- Appointment Matching based on University ID via custom appointment form in each appointment.

### Changed
- Appointment Map to add a button for "Blackboard Help" since it is an option that is frequently used and looked for by clients.
