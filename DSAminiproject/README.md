# Smart Parking Management System

A Java Swing + MySQL mini-project demonstrating JDBC, FIFO waiting allocation,
a recent-exit stack, a vehicle lookup `HashMap`, and linked-list history records.

## Requirements

- JDK 25 or newer (Java 25 is the current LTS baseline)
- MySQL 8.0+
- Maven (for building and running with the MySQL Connector/J and BCrypt dependencies)

## Database setup

Run `database/smart_parking.sql` in MySQL Workbench or the MySQL command line.
The application connects using these environment variables (defaults shown):

| Variable | Default |
| --- | --- |
| `PARKING_DB_URL` | `jdbc:mysql://localhost:3306/smart_parking?serverTimezone=UTC` |
| `PARKING_DB_USER` | `root` |
| `PARKING_DB_PASSWORD` | empty |
| `PARKING_ADMIN_USERNAME` | not set |
| `PARKING_ADMIN_PASSWORD` | not set |

Create the first account using **Create account** on the login screen, or set
`PARKING_ADMIN_USERNAME` and `PARKING_ADMIN_PASSWORD` to seed an initial account.
New passwords are stored as BCrypt hashes. Existing PBKDF2 administrator
passwords remain valid and are upgraded to BCrypt after a successful sign-in.
Choose a strong password and keep environment variables private.

PowerShell example for the current terminal:

```powershell
$env:PARKING_DB_PASSWORD = "your-mysql-password"
$env:PARKING_ADMIN_USERNAME = "admin"
$env:PARKING_ADMIN_PASSWORD = "choose-a-strong-password"
mvn clean compile exec:java -Dexec.mainClass=smartparking.Main
```

To run from VS Code, open this folder and run the same Maven command in its
integrated terminal. The project is configured with `src` as its Java source
directory; the MySQL driver is resolved by Maven.

### Open the desktop app later

Start the local MySQL server, open PowerShell in this project folder, and run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-smart-parking.ps1
```

The first launch prompts only for the **MySQL root/server password** (not your
Smart Parking sign-in). The launcher stores the password encrypted for your
Windows account (outside the project), so later launches do not ask for it
again. Keep the MySQL service running while using the app. A **Smart Parking**
desktop shortcut can be used for one-click launch after the first-time setup.

## Features

- Admin login and password visibility toggle
- Self-service account creation with unique username/email checks and BCrypt
- Login by username or email; optional remembered username (never the password)
- Dashboard metrics and parking-slot overview
- Vehicle entry with automatic slot assignment
- FIFO waiting list when the lot is full; next queued vehicle is assigned
  automatically when a slot is released
- Vehicle exit, hourly fee calculation (CAR 50, BIKE 20, OTHER 40 per hour),
  and slot release
- Searchable parking history and daily/revenue/occupancy reports
- Recent exits held in a LIFO stack; current vehicle lookup cached in a
  `HashMap`; history results maintained in a linked list

Fees are rounded up to the next hour, with a one-hour minimum. All amounts are
in the application's local currency units.
