# GitAway Hotel Reservation System

The **GitAway Hotel Reservation System** is a Java Swing-based desktop application designed for managing hotel reservations, rooms, guest information, and billing. It caters to two main roles: **System Administrators** (who oversee hotel operations, room statuses, guest registers, reservations, and billing) and **Customers/Guests** (who register, make reservations, and view their booking details). 

---

## About the System

This system addresses common hotel management challenges by digitalizing workflows such as room assignment, reservation tracking, billing calculation, and guest directory management.

- **Problem Solved**: Replaces manual ledger booking or scattered spreadsheets with a centralized, relational SQL database connected to an intuitive desktop GUI, reducing double-bookings and billing errors.
- **Intended Users**:
  - **Hotel Staff/Administrators**: Manage the daily operations, update room availability, record billing, and approve pending reservations.
  - **Guests**: Self-service platform to view room types, check pricing, create reservations, and track booking statuses.
  - **Scope**: Designed as a thick-client desktop application backed by a MySQL database, appropriate for a single-property management system or academic capstone project.

---

## Features

### 1. Authentication and Registration
- **Role-Based Routing**: Restricts application access depending on the logged-in role (Admin or Customer).
- **Guest Registration**: New guests can create accounts directly from the login window via a registration dialog.
- **Login Protection**: Tracks login attempts; disables input fields and locks the session if authentication fails repeatedly (maximum of 3 failed attempts).
- **Password Toggle**: Allows visibility toggle of password entry fields.
- **Validation**: Ensures fields are not left empty before submission.

### 2. Administrator Panel
The Admin Panel features a sidebar-driven interface containing five core management modules:
- **Dashboard**: Displays real-time hotel indicators, including:
  - Total Rooms (categorized by type: Single, Double, Deluxe, Suite).
  - Room Status overview (Available, Reserved, Occupied, Maintenance).
  - Total Guests registered.
  - Active and Pending Reservations.
  - Total Revenue generated.
- **Room Management**: 
  - List all rooms with details (Room Number, Type, Price per Night, Capacity, Status, and Description).
  - Add, edit, or archive (soft-delete) rooms.
  - Filter rooms by type and status.
- **Guest Management**:
  - View full name, email, phone number, address, and ID number of registered guests.
  - Add, edit, or archive (soft-delete) guest records.
  - Search guest lists dynamically.
- **Reservation Management**:
  - Central log of all reservations indicating check-in/check-out dates, total cost, and current status.
  - Update status (Pending, Confirmed, Checked In, Checked Out, Cancelled).
  - Confirm pending bookings, assign specific rooms, or cancel bookings.
  - Soft-delete (archive) reservation entries.
- **Billing Management**:
  - Automatically generates bills when reservations transition to `CHECKED_OUT`.
  - View room charges, edit additional charges, apply discounts, calculate tax (12%), and settle totals.
  - Settle bill statuses (`Generated`, `Pending Settlement`, `Settled`, `Cancelled`).

### 3. Customer Panel
A tailored interface dedicated to guest self-service:
- **Make Reservation**: 
  - Allows customers to select room type, input check-in/check-out dates, specify number of guests, and add notes.
  - Automatically calculates estimated total amount before booking.
  - Selects only available rooms matching the request.
- **View Reservations**: 
  - Displays a historical log of the customer's own bookings.
  - View individual booking statuses (Pending, Confirmed, Checked In, Checked Out, Cancelled).
  - Allows canceling a booking if it is still in `PENDING` or `CONFIRMED` state.

---

## Technologies Used

- **Programming Language**: Java (developed/tested using JDK 25)
- **GUI Framework**: Java Swing & AWT (Abstract Window Toolkit)
- **Database**: MySQL 8.x/9.x
- **Database Driver**: MySQL Connector/J (`mysql-connector-j-8.1.0.jar`)
- **Build & Launch Tools**: Apache Ant (`build.xml`) and Windows Command scripts (`run.bat`)

---

## Project Structure

```
gitaway-hotel-reservation-system/
│
├── build/                       # Compiled bytecode classes
├── lib/                         # Dependency libraries
│   └── mysql-connector-j-8.1.0.jar
│
├── src/                         # Source files
│   └── com/
│       ├── gitaway/
│       │   └── database/        # Database connection and initialization code
│       │
│       └── hotel/
│           ├── Main.java        # Main entry point of the application
│           ├── dao/             # Data Access Objects interface layer
│           ├── db/              # Deprecated db connector (replaced by gitaway.database)
│           ├── exception/       # Custom Exception types (ValidationException, AuthenticationException)
│           ├── model/           # Business models (User, Room, Guest, Reservation, Billing)
│           ├── service/         # Service layer executing business logic and rules
│           ├── test/            # Unit/Integration tests
│           ├── ui/              # Swing GUI panels and dialogs
│           └── util/            # Validation helpers and custom utility classes
│
├── test/                        # Integration testing files
├── build.xml                    # Ant build configuration file
├── run.bat                      # Windows batch launcher (checks environment, compiles, and runs)
└── schema.sql                   # SQL script for database creation, table schema, and sample data seeding
```

---

## Database

The application connects to a MySQL instance hosted locally using the following properties:
- **Database URL**: `jdbc:mysql://localhost:3306/hotel_reservation_db`
- **Database Name**: `hotel_reservation_db`
- **Tables**:
  - `users`: Credentials, user details, and roles (`ADMIN` or `CUSTOMER`).
  - `rooms`: Room info, type (`SINGLE`, `DOUBLE`, `DELUXE`, `SUITE`), rates, and status (`AVAILABLE`, `RESERVED`, `OCCUPIED`, `MAINTENANCE`).
  - `guests`: Directory of guest names, contact numbers, email, address, and ID credentials.
  - `reservations`: Reservation dates, assigned rooms, cost computations, and statuses.
  - `billing`: Invoice ledger tracking room charges, additional fees, taxes (12%), discounts, and settlement status.

*Note: Database credentials are coded inside [DatabaseConnection.java](file:///c:/Users/mejor/Desktop/Latest/gitaway-hotel-reservation-system/src/com/gitaway/database/DatabaseConnection.java). On launch, the system automatically checks for the existence of `hotel_reservation_db` and initializes the database tables and sample records using [schema.sql](file:///c:/Users/mejor/Desktop/Latest/gitaway-hotel-reservation-system/schema.sql) if they are missing.*

---

## Installation and Setup

### Prerequisites
1. **Java Development Kit (JDK)**: Ensure JDK 11 or higher (JDK 25 recommended) is installed and available in your environment `PATH`.
2. **MySQL Server**: Ensure MySQL Server is running locally on port `3306`.
3. **Database Configuration**:
   - Check the username and password in [DatabaseConnection.java](file:///c:/Users/mejor/Desktop/Latest/gitaway-hotel-reservation-system/src/com/gitaway/database/DatabaseConnection.java#L15-L16). Modify these lines to match your local MySQL configuration.
   - By default, the database connector uses `root` as user and `M@rd3n4tu$110206` as password.

### Launching the Application
Execute the batch script `run.bat` in the root folder. The script will perform the following actions:
1. Verify the existence of `lib\mysql-connector-j-8.1.0.jar`.
2. Check for Apache Ant in the system `PATH`. If present, compiles and runs via Ant.
3. If Ant is not installed, compiles manually using the `javac` compiler into the `build\classes` directory.
4. Auto-creates `hotel_reservation_db` and seeds tables and initial mock data.
5. Launches the Swing login screen (`com.hotel.Main`).

---

## Default Account

Upon database initialization, the system seeds default user accounts for testing:

| Username | Password | Role | Full Name |
| :--- | :--- | :--- | :--- |
| **admin** | `admin123` | `ADMIN` | System Administrator |
| **customer** | `customer123` | `CUSTOMER` | Juan Dela Cruz |
| **guest** | `guest123` | `CUSTOMER` | Guest User |

---

## System Workflow

1. **Start System**: Main entry compiles (if necessary) and establishes database connection, triggering automatic schema setup.
2. **Authentication**: Users must log in on the redesigned Split Login Screen.
3. **Admin Flow**:
   - Views the summary statistics on the Dashboard.
   - Manages room lists, registers guest names, schedules reservations, and handles checkout invoices.
4. **Customer Flow**:
   - Registers an account or logs in.
   - Submits booking requests based on check-in/out range and room type.
   - Views history or cancels bookings directly.
5. **Billing Cycle**:
   - Transitioning a reservation status to `CHECKED_OUT` triggers billing generation.
   - Admin settles payments under the Billing Management panel.

---

## Current Limitations

- **Hardcoded Configuration**: Database connection settings (URL, user, and password) are stored directly in Java source code rather than a configuration file or environment variables.
- **Locking Mechanisms**: Lack of advanced concurrency management (e.g., pessimistic/optimistic locking) to prevent two administrators from modifying the same record simultaneously at the UI layer.
- **Desktop Restriction**: Application must run as a thick-client desktop GUI and requires direct database access, limiting remote browser access.
- **No Secure Password Hashing**: Passwords are saved in plain text within the database, which is not suitable for production deployment.

---

## Future Enhancements

- **Configuration File Support**: Relocate database settings to a `.properties` or `.env` configuration file outside the source package.
- **Secure Authentication**: Integrate password hashing (e.g., bcrypt or Argon2) to secure stored credentials.
- **Export Capabilities**: Allow exporting dashboards, bills, and reservation reports to PDF or Excel formats.
- **Interactive Calendar View**: Add an interactive scheduling calendar to preview room bookings visually rather than using tabular lists.

---

## License

This project was developed for academic capstone purposes.
