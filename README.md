# Asmitha Explores · Order Tracker

A small, single-user order management web app for **Asmitha Explores** — a business that sells
kids' educational flash cards, banners and books that are customised and printed on demand.

Built with **Java 17 · Spring Boot 3 · Spring MVC · Spring Data JPA · Thymeleaf · Bootstrap 5**.

---

## Features

| Area | What you get |
|------|--------------|
| **Dashboard** (`/`) | Orders per status, orders not yet delivered **with an "Open for" ageing column in red**, a **To print / restock** to-do list with a one-click **Ordered** button, orders & revenue this month |
| **Customers** (`/customers`) | List with search by name/mobile, computed order count, add/edit, delete (blocked when the customer has orders), detail page with full order history |
| **Inventory** (`/products`) | Catalogue mirrored from the storefront (name, **category**, price), **out-of-stock rows highlighted in red** with an "Out of stock" badge, add/edit/delete |
| **Orders** (`/orders`) | Create with **"add new customer" as the default** (switch off to pick an existing one), product picker grouped by category, multiple products per order, filters by status/customer/date range, coloured status badges |
| **Order editing** | While an order is still `NEW` you can change customer, date, products, quantities and notes — stock is reversed and re-applied automatically. After that only notes/tracking id can be edited |
| **Stock handling** | Creating an order reduces `quantityInHand`; when stock is short the order is still accepted and the shortfall is added to `quantityToBeOrdered` with a warning |
| **Status flow** | Forward-only: `NEW → PRINTING_IN_PROGRESS → PACKED → SHIPPED → DELIVERED`, tracking id mandatory for `SHIPPED` |
| **WhatsApp** | "Copy tracking message" button on shipped/delivered orders creates a ready-to-paste message |

---

## Profiles

| Profile | File | Database | Used for |
|---------|------|----------|----------|
| `local` (default) | `application-local.properties` | **PostgreSQL** on localhost | Running & testing on your laptop |
| `prod` | `application-prod.properties` | **Supabase** (PostgreSQL) | Production deployment |
| `test` | `src/test/resources/application.properties` | In-memory H2 (PostgreSQL mode) | `mvn test` (applied automatically) |

`application.properties` keeps only the settings shared by every profile; the active profile
defaults to `local`. Local and production run the **same database engine**, so there are no
surprises on deploy. H2 is a test-scope dependency only, so `mvn test` needs no database.

---

## Run it locally

**1. Start PostgreSQL and create the database** (one time, pick either option):

```bash
docker compose up -d                 # uses the bundled docker-compose.yml

# ...or a native install
brew services start postgresql@16
createdb asmitha_explores
```

Defaults expected by the `local` profile: host `localhost`, port `5432`, database
`asmitha_explores`, user `postgres`, password `postgres`. Override any of them with
`LOCAL_DB_HOST` / `LOCAL_DB_PORT` / `LOCAL_DB_NAME` / `LOCAL_DB_USER` / `LOCAL_DB_PASSWORD`
(see [`.env.example`](.env.example)). Tables are created automatically on first start.

**2. Run the app:**

```bash
mvn spring-boot:run
```

Then open **<http://localhost:8080/>** in a browser (works well on a phone too).

Other useful commands:

```bash
mvn clean package      # build the jar
mvn test               # run the unit tests (in-memory H2, no database needed)
java -jar target/order-tracker-1.0.0.jar
```

Local profile specifics: SQL schema auto-updated (`ddl-auto=update`), Thymeleaf cache off,
debug logging on, demo data seeded when the database is empty.

### Seed data

On the very first start (empty database) a `CommandLineRunner` loads the **real Asmitha Explores
catalogue** — 29 products with their storefront names, categories and prices
(Flash Cards, Banners, Learning Books, Story Books) — plus 3 sample customers and 3 sample orders
so the screens are not empty. Stock quantities are placeholders: correct them on the Inventory
screen. Turn seeding off with `app.seed-data=false` (already off in `prod`).

---

## Run in production (Supabase)

### Step 1 — create the tables (no data)

Two options, both create **only the 4 tables + keys + indexes**, never any rows:

**a) SQL script (recommended, nothing to run locally)**
Open Supabase → **SQL Editor**, paste [`src/main/resources/db/schema-postgres.sql`](src/main/resources/db/schema-postgres.sql)
and run it. The script is idempotent (`create table if not exists`, guarded constraints), so
re-running it is harmless. From a terminal it works too:

```bash
psql "postgresql://postgres.<project-ref>:<password>@<host>:5432/postgres" \
     -f src/main/resources/db/schema-postgres.sql
```

**b) Let Hibernate create them** — start the app once with the `prod` profile. `app.seed-data`
is `false` in production, so the demo catalogue is **not** inserted:

```bash
SPRING_PROFILES_ACTIVE=prod DB_DDL_AUTO=update mvn spring-boot:run
```

After the tables exist, switch to `DB_DDL_AUTO=validate` so the app never changes the schema
again — it will simply refuse to start if the schema and the code disagree.

### Step 1b — load the product catalogue (optional)

[`src/main/resources/db/seed-products.sql`](src/main/resources/db/seed-products.sql) inserts the
29 storefront products (name, category, price) with **stock at 0** — no customers, no orders.
It skips products whose name already exists, so it is safe to re-run when new products launch:

```bash
set -a && source .env && set +a
PGPASSWORD="$SUPABASE_DB_PASSWORD" PGSSLMODE=require \
  psql -h "$SUPABASE_DB_HOST" -p 5432 -U "$SUPABASE_DB_USER" -d "$SUPABASE_DB_NAME" \
       --no-password --pset pager=off -f src/main/resources/db/seed-products.sql
```

### Step 2 — configure and run

1. In Supabase open **Project Settings → Database → Connection pooling** and copy the host, port,
   database name, user (`postgres.<project-ref>`) and your database password.

2. Set the environment variables — see [`.env.example`](.env.example):

   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export SUPABASE_DB_HOST=aws-0-ap-south-1.pooler.supabase.com
   export SUPABASE_DB_PORT=6543          # 6543 = transaction pooler, 5432 = session/direct
   export SUPABASE_DB_NAME=postgres
   export SUPABASE_DB_USER=postgres.abcdefghijklmnop
   export SUPABASE_DB_PASSWORD='your-database-password'
   ```

3. Start the app:

   ```bash
   mvn clean package
   java -jar target/order-tracker-1.0.0.jar
   ```

   or, without exporting the profile:

   ```bash
   java -jar target/order-tracker-1.0.0.jar --spring.profiles.active=prod
   mvn spring-boot:run -Dspring-boot.run.profiles=prod          # from sources
   ```

The resulting JDBC URL is

```
jdbc:postgresql://${SUPABASE_DB_HOST}:${SUPABASE_DB_PORT}/${SUPABASE_DB_NAME}?sslmode=require&prepareThreshold=0
```

`sslmode=require` because Supabase only accepts TLS, and `prepareThreshold=0` because the
transaction pooler (PgBouncer) does not support server-side prepared statements.

Production profile specifics:

* No secrets in the repo — every value comes from an environment variable
* Hikari pool capped at 5 connections (Supabase pooler limits); tune with `DB_POOL_SIZE`
* `ddl-auto=update` creates the tables on the first deploy — set `DB_DDL_AUTO=validate`
  once the schema is stable
* Demo seeding off, H2 console off, template cache on, stack traces never shown to the user
* `server.forward-headers-strategy=native` keeps links on HTTPS behind a proxy
* `PORT` is honoured, which is what Railway / Render / Fly.io inject

### Plain PostgreSQL instead of Supabase?

The same `prod` profile works — point the variables at your own server and (optionally) override
the whole URL:

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/asmitha_explores
export SUPABASE_DB_USER=postgres
export SUPABASE_DB_PASSWORD=postgres
```

The PostgreSQL driver is already on the classpath — no `pom.xml` change needed.

---

## Configuration

| Property | Env variable | Default | Meaning |
|----------|--------------|---------|---------|
| `app.seed-data` | — | `true` (local) / `false` (prod) | Seed demo data when the database is empty |
| `app.shop-name` | — | `Asmitha Explores` | Shop name used in messages |
| `server.port` | `PORT` | `8080` | HTTP port |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | `local` | Active profile |
| `spring.jpa.hibernate.ddl-auto` | `DB_DDL_AUTO` | `update` | Schema handling in `prod` |
| — | `DB_POOL_SIZE` | `5` | Hikari max pool size in `prod` |

---

## Project structure

```
pom.xml
docker-compose.yml               local PostgreSQL for development
.env.example                     all environment variables, documented

src/main/java/com/asmithaexplores/ordertracker
├── OrderTrackerApplication.java
├── config/      AppProperties, DataSeeder (real storefront catalogue)
├── controller/  DashboardController, CustomerController, ProductController,
│                OrderController, GlobalExceptionHandler
├── dto/         CustomerForm/View, ProductForm/View, OrderForm, OrderItemForm,
│                OrderView, OrderItemView, OrderEditForm, OrderFilter,
│                OrderCreationResult, DashboardView
├── entity/      Customer, Product, Order, OrderItem, OrderStatus
├── exception/   ResourceNotFoundException, BusinessRuleException
├── repository/  CustomerRepository, ProductRepository, OrderRepository, OrderItemRepository
└── service/     CustomerService, ProductService, OrderService, DashboardService

src/main/resources
├── application.properties          common settings (active profile = local by default)
├── application-local.properties    local PostgreSQL, seed data, dev friendly
├── application-prod.properties     Supabase PostgreSQL, hardened
├── db/schema-postgres.sql          tables-only DDL for Supabase / PostgreSQL
└── templates/   dashboard.html, error.html, fragments/layout.html,
                 customers/{list,form,detail}.html,
                 products/{list,form}.html,
                 orders/{list,form,detail}.html

src/test/resources
└── application.properties          in-memory H2 used by mvn test

src/test/java/com/asmithaexplores/ordertracker
├── OrderTrackerApplicationTests.java
└── service/OrderServiceTest.java
```

---

## Tests

`OrderServiceTest` (JUnit 5 + Mockito) covers stock deduction, shortfall handling, duplicate line
merging, forward-only status transitions, the mandatory tracking id for shipping, and stock
restoration on delete.

```bash
mvn test
```
