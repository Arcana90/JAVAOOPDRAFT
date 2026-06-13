# Pass Slip Backend — IntelliJ IDEA Setup

Pure Java 21 backend. No framework. Runs with an H2 in-memory database —
no MySQL, no external services, no configuration needed.

---

## What you need (install these first)

| Tool | Version | Link |
|------|---------|------|
| JDK  | 21+     | https://adoptium.net → download **Temurin 21 LTS** |
| IntelliJ IDEA | any edition | https://www.jetbrains.com/idea/download — Community (free) is enough |

Maven is **bundled inside IntelliJ** — you do not need to install it separately.

---

## Step 1 — Open the project

1. Launch IntelliJ IDEA.
2. On the Welcome screen click **Open** (or `File → Open` if a project is already open).
3. Navigate to and select the **`passslip-intellij`** folder. Click **OK**.
4. IntelliJ detects `pom.xml` and asks:
   > *"Maven build scripts found. Load?"*

   Click **Load** (or **Trust Project** if it asks about trust).

5. Wait for the bottom status bar to finish **"Resolving dependencies…"** and **"Indexing…"**
   (takes ~30–60 seconds on first open while Maven downloads H2 and JUnit from the internet).

---

## Step 2 — Set the JDK (first time only)

If IntelliJ shows a **"Project JDK is not defined"** banner at the top:

1. Click the banner or go to `File → Project Structure` (`Ctrl+Alt+Shift+S`).
2. Under **Project → SDK**, click the dropdown and select **21** (or **Add SDK → JDK**
   and point it at your JDK 21 installation folder).
3. Click **OK**.

---

## Step 3 — Run

The run configuration **"Run AppBootstrap"** is pre-loaded.

Click the **green ▶ triangle** in the top-right toolbar and select **Run AppBootstrap**.

Or press `Shift+F10`.

Output appears in the **Run** panel at the bottom.

### Expected output

```
[INFO] Connection pool ready.
[INFO] Database schema initialized and seed data loaded.

─── Demo: Issue pass slip for EMP-001 ───────────────
[INFO] ✓ SUCCESS  slip=a1b2c3d4-...  employee=EMP-001  timeOut=2024-...
[INFO] [EVENT] PassSlipIssued → slip=a1b2...  employee=EMP-001  at=2024-...

─── Demo: Re-issue for same employee (must be blocked) ─
[WARN] ✗ BLOCKED: Employee [EMP-001] already has an active pass slip (status: OUT)...

─── Demo: Validation failure (empty destination) ────
[WARN] ✗ VALIDATION FAILURE: [Destination must not be empty.]

─── Demo: Time-In for slip a1b2c3d4... ─────────────
[INFO] ✓ SUCCESS  slip=a1b2c3d4  employee=EMP-001  duration=0h 0m  timeIn=2024-...
[INFO] [EVENT] EmployeeReturned → slip=a1b2...  duration=0h 0m  at=2024-...

─── Demo: Re-process Time-In on RETURNED slip (must fail) ─
[WARN] ✗ VALIDATION FAILURE: Pass slip [...] cannot be resolved because it is in state [RETURNED]...

═══════════════════════════════════════════════════
  Demo complete. All lifecycle rules enforced.
═══════════════════════════════════════════════════
```

---

## Step 4 — Run Tests

**Option A — Run all tests at once:**

Click the green ▶ triangle → select **All Tests**.
Or press `Ctrl+Shift+F10` with a test file open.

**Option B — Run a single test class:**

Open any `*Test.java` file, click the green ▶ in the gutter next to the class name.

**Option C — Maven terminal:**

Open `View → Tool Windows → Terminal` and run:
```bash
mvn test
```

### Tests included

| Test class | What it covers |
|-----------|----------------|
| `DurationCalculatorTest` | Duration formatting, edge cases, null guards |
| `PassSlipValidatorTest` | All validation rules and length bounds |
| `EventPublisherTest` | Fan-out, failing listener isolation, unregister |
| `PassSlipPipelineIntegrationTest` | Full end-to-end pipeline against H2 (9 ordered scenarios) |

---

## Switching to MySQL for production

**1.** Replace the H2 dependency in `pom.xml`:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.3.0</version>
</dependency>
```

**2.** Update the three constants at the top of `AppBootstrap.java`:
```java
private static final String JDBC_URL = "jdbc:mysql://localhost:3306/passslip_db";
private static final String DB_USER  = "your_db_user";
private static final String DB_PASS  = "your_db_password";
```

**3.** Run the DDL from `DatabaseInitializer.java` on your MySQL server as a one-time
migration, then remove the `DatabaseInitializer.initialize(setupConn)` call from
`AppBootstrap.main()`.

---

## Project layout

```
passslip-intellij/
├── pom.xml                               Maven build (H2 + JUnit 5 + Mockito)
├── .idea/
│   ├── compiler.xml                      Java 21 bytecode target
│   ├── encodings.xml                     UTF-8 for all sources
│   ├── misc.xml                          Project SDK = JDK 21
│   └── runConfigurations/
│       ├── Run_AppBootstrap.xml          ← pre-built ▶ Run config
│       └── All_Tests.xml                 ← pre-built ▶ Test config
└── src/
    ├── main/java/backend/
    │   ├── app/
    │   │   ├── AppBootstrap.java         ← main() entry point
    │   │   └── DatabaseInitializer.java  H2 DDL + seed data
    │   ├── db/
    │   │   └── ConnectionPoolManager.java
    │   ├── employee/
    │   │   └── EmployeeStatus.java       AVAILABLE → OUT → RETURNED
    │   ├── events/
    │   │   ├── BackendEventListener.java
    │   │   ├── EventPublisher.java
    │   │   ├── PassSlipIssuedEvent.java
    │   │   └── EmployeeReturnedEvent.java
    │   ├── passslip/
    │   │   ├── PassSlipDTO.java
    │   │   ├── PassSlipValidator.java
    │   │   ├── PassSlipRepository.java
    │   │   ├── PassSlipService.java
    │   │   └── PassSlipController.java
    │   ├── shared/
    │   │   └── DurationCalculator.java
    │   └── timein/
    │       ├── TimeInValidator.java
    │       ├── ReturnStatusUpdater.java
    │       ├── TimeInService.java
    │       └── TimeInController.java
    └── test/java/backend/
        ├── events/   EventPublisherTest.java
        ├── passslip/ PassSlipValidatorTest.java
        │             PassSlipPipelineIntegrationTest.java
        ├── shared/   DurationCalculatorTest.java
        └── timein/   (add yours here)
```
