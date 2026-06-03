# CLAUDE.md

Guía para trabajar en este repositorio. Leer antes de tocar código.

## Qué es esto

**Control Financiero**: app de registro financiero personal con integración a Mercado Pago.
Monorepo con dos módulos Gradle independientes (no hay build raíz que los agrupe):

```
control-financiero/
├── backend/   # Servidor REST Kotlin + Ktor 3.0 + PostgreSQL (Exposed ORM)
└── android/   # App Android Jetpack Compose + Material3
```

Ambos comparten el package base `com.controlfinanciero`, pero son proyectos Gradle
separados: se abren/compilan por separado.

## Stack

| Componente      | Backend                          | Android                                  |
|-----------------|----------------------------------|------------------------------------------|
| Lenguaje        | Kotlin 2.0.21 (JVM 17)           | Kotlin 2.0.21 (JVM 17)                   |
| Framework       | Ktor 3.0.3 (Netty)               | Jetpack Compose + Material3              |
| Datos           | Exposed 0.57.0 + PostgreSQL + HikariCP | Retrofit 2.11 + kotlinx.serialization |
| Gráficos        | —                                | Vico Charts 2.0.1                        |
| HTTP saliente   | Ktor Client CIO (a Mercado Pago) | OkHttp + logging-interceptor             |
| Serialización   | kotlinx-serialization-json       | kotlinx-serialization-json 1.7.3         |

- Android SDK: `compileSdk`/`targetSdk` = 35, `minSdk` = 26. AGP 8.7.3.
- `applicationId` / `namespace` Android = `com.controlfinanciero`.

## Build & Run

> Cada módulo tiene su **Gradle wrapper** (`gradlew` / `gradlew.bat` / `gradle/wrapper/`)
> fijado a **Gradle 8.11.1** (compatible con AGP 8.7.3 y Kotlin 2.0.21). El wrapper
> descarga Gradle solo en la primera corrida.
> Nota: este entorno de desarrollo **no** tiene `gradle`, `java` ni Android SDK en el PATH,
> así que la compilación se hace desde el IDE o una máquina con JDK 17 instalado.

### Backend
```bash
# Requisitos: JDK 17, PostgreSQL corriendo, base "control_financiero" creada
cd backend
./gradlew run            # (requiere wrapper) — levanta Ktor en :8080
./gradlew build          # compila + tests
./gradlew test           # solo tests
# Seed inicial de categorías:
curl -X POST http://localhost:8080/api/categories/seed
```

### Android
- Abrir la carpeta `android/` en Android Studio (NO la raíz).
- `BuildConfig.API_BASE_URL` depende del **product flavor** (dimensión `target`, en `app/build.gradle.kts`):
  - `emulator` → `http://10.0.2.2:8080` (alias del localhost de la PC desde el emulador).
  - `device` → `http://<device.api.host>:8080` para un **celular físico**. La IP se lee de
    `local.properties` (clave `device.api.host`, no versionado) con fallback `192.168.101.75`.
- Elegir la variante en el panel **Build Variants** de AS (`:app` → `deviceDebug` o `emulatorDebug`).
- Para el celu físico: backend corriendo, misma red Wi-Fi, y abrir el puerto 8080 en el
  firewall de Windows (`New-NetFirewallRule ... -LocalPort 8080 -Action Allow`).
- Build & Run sobre emulador o dispositivo.

## Arquitectura

### Backend (`backend/src/main/kotlin/com/controlfinanciero/`)
- `Application.kt` — entry point. `main()` usa `EngineMain` (config en `application.conf`).
  `Application.module()` inicializa DB y plugins en orden:
  `DatabaseFactory.init` → Serialization → CORS → **Security (JWT)** → StatusPages → Routing.
- `database/DatabaseFactory.kt` — HikariCP + Exposed, lee config de `application.conf`.
- `models/db/Tables.kt` — tablas Exposed (`Households`, `Users`, `Categories`, `Transactions`, `Accounts`, `RecurringTransactions`, `RecurringOccurrences`, `Investments`, `Budgets`, `SavingsGoals`). `Users` tiene `household_id` nullable; `Categories`/`Transactions`/`RecurringTransactions` tienen `user_id`. `RecurringOccurrences` son los vencimientos (pendiente/pagado) de cada fijo. `Investments` son inversiones (carga manual) por usuario. `Accounts` son cuentas/billeteras; `Transactions` tiene `account_id` nullable (saldo de cuenta = inicial + ingresos − egresos). `Budgets` son límites mensuales de gasto por categoría. `SavingsGoals` son metas de ahorro (carga manual, standalone): objetivo + ahorrado + fecha límite opcional. ⚠️ **Toda tabla nueva hay que agregarla al `SchemaUtils.createMissingTablesAndColumns(...)` de `DatabaseFactory.init`** o la tabla no se crea (500 "relation does not exist").
- `models/dto/DTOs.kt` + `AuthDTOs.kt` — DTOs serializables de la API.
- `repositories/` — `UserRepository`, `CategoryRepository`, `TransactionRepository`, `RecurringTransactionRepository`, `HouseholdRepository`, `InvestmentRepository`, `AccountRepository`, `BudgetRepository`, `SavingsGoalRepository`. Las escrituras filtran por `userId`; las lecturas de transacciones usan el **alcance del hogar** (`HouseholdRepository.memberIds`). `RecurringTransactionRepository` genera los vencimientos del mes por frecuencia (idempotente); al marcar un vencimiento `pagado` crea la `Transaction` real, y al volverlo a `pendiente` la borra.
- `routes/` — endpoints REST. Se registran en `plugins/Routing.kt`: `authRoutes` es público, el resto va bajo `authenticate(JWT_AUTH)`.
- `services/MercadoPagoService.kt` — cliente Ktor hacia la API de Mercado Pago.
- `plugins/Security.kt` — JWT HMAC256: `configureSecurity()`, `generateJwtToken()`, `ApplicationCall.userId()` (extrae el userId del token).
- `plugins/` — config de Ktor (CORS, Serialization, Security, StatusPages, Routing).
- Config runtime: `src/main/resources/application.conf` (puerto, DB, Mercado Pago, JWT).

### Android (`android/app/src/main/kotlin/com/controlfinanciero/`)
- `MainActivity.kt` — host Compose + gating de auth (spinner / `AuthScreen` / app).
- `data/api/ApiService.kt` — interfaz Retrofit (espejo de los endpoints del backend).
- `data/api/RetrofitClient.kt` — singleton Retrofit; interceptor agrega `Bearer` y, ante 401, fuerza logout.
- `data/auth/` — `SessionManager` (DataStore) + `AuthTokenProvider` (token en memoria que lee el interceptor).
- `data/models/Models.kt` — modelos compartidos + `ApiResponse<T>` wrapper.
- `ui/screens/` — `DashboardScreen`, `AddTransactionScreen`, `AuthScreen`, `RecurringScreen` (fijos con frecuencia + vencimientos pendiente/pagado), `HouseholdScreen` (hogar compartido), `InvestmentsScreen` (inversiones), `AccountsScreen` (cuentas/billeteras), `BudgetsScreen` (presupuestos), `SavingsGoalsScreen` (metas de ahorro), `ReportsScreen` (gráficos Vico), `SettingsScreen` (perfil + import CSV de MP), `PremiumScreen` (placeholder). Se navegan desde el **menú lateral** (`ModalNavigationDrawer`); el dashboard tiene la hamburguesa que lo abre. Avatar de iniciales en `ui/components/UserAvatar.kt`.
- `ui/viewmodels/` — `DashboardViewModel`, `AuthViewModel` (login/registro/logout/perfil), `RecurringViewModel`, `HouseholdViewModel`, `InvestmentViewModel`, `AccountViewModel`, `BudgetViewModel`, `SavingsGoalViewModel`.
- `ui/theme/Theme.kt` — Material3.

## API REST

Base: `http://localhost:8080`. Todas las respuestas envueltas en `ApiResponse<T>`.
Todas las rutas excepto `register`/`login` requieren `Authorization: Bearer <token>` y filtran por usuario.

| Método | Ruta                              | Descripción                                   |
|--------|-----------------------------------|-----------------------------------------------|
| POST   | `/api/auth/register`              | Crear cuenta (`email,password,name?`) → `{ token, user }` (siembra categorías) |
| POST   | `/api/auth/login`                 | Login → `{ token, user }`                     |
| GET    | `/api/auth/me`                    | Usuario autenticado                           |
| PUT    | `/api/auth/me`                    | Editar perfil (campo `name`)                  |
| POST   | `/api/mercadopago/token`          | Guardar token MP del usuario                  |
| GET    | `/api/categories`                 | Listar categorías (filtro `type`)             |
| POST   | `/api/categories`                 | Crear categoría                               |
| POST   | `/api/categories/seed`            | Crear categorías por defecto                  |
| GET    | `/api/transactions`               | Listar (filtros `type,categoryId,from,to,limit,offset`) |
| POST   | `/api/transactions`               | Crear transacción manual (`accountId` opcional) |
| POST   | `/api/transactions/import`        | Importar CSV de Mercado Pago (`csv,accountId?,onlyPurchases`) |
| DELETE | `/api/transactions/{id}`          | Eliminar transacción                          |
| GET    | `/api/dashboard`                  | Dashboard del mes actual (genera los vencimientos del mes) |
| GET    | `/api/dashboard/monthly/{year}`   | Reporte mensual del año                       |
| POST   | `/api/mercadopago/sync`           | Sincronizar pagos (filtros `from,to,categoryId`) |
| GET    | `/api/recurring`                  | Listar fijos con sus vencimientos del mes (pendiente/pagado) |
| POST   | `/api/recurring`                  | Crear fijo (`amount,description,type,categoryId,frequency,anchorDate`) |
| POST   | `/api/recurring/occurrences/{id}/pay`   | Marcar vencimiento pagado (crea la transacción) |
| POST   | `/api/recurring/occurrences/{id}/unpay` | Volver vencimiento a pendiente (borra la transacción) |
| DELETE | `/api/recurring/{id}`             | Eliminar fijo                                 |
| GET    | `/api/household`                  | Hogar actual del usuario (miembros + código)  |
| POST   | `/api/household`                  | Crear hogar → genera `inviteCode`             |
| POST   | `/api/household/join`             | Unirse a un hogar por `inviteCode`            |
| POST   | `/api/household/leave`            | Salir del hogar                               |
| GET    | `/api/investments`                | Resumen de inversiones (totales + lista)      |
| POST   | `/api/investments`                | Crear inversión (`name,type,amountInvested,currentValue?`) |
| PUT    | `/api/investments/{id}`           | Editar inversión                              |
| DELETE | `/api/investments/{id}`           | Eliminar inversión                            |
| GET    | `/api/accounts`                   | Resumen de cuentas (patrimonio total + saldos) |
| POST   | `/api/accounts`                   | Crear cuenta (`name,type,initialBalance`)     |
| PUT    | `/api/accounts/{id}`              | Editar cuenta                                 |
| DELETE | `/api/accounts/{id}`              | Eliminar cuenta (desvincula sus transacciones) |
| GET    | `/api/budgets`                    | Presupuestos con gastado del mes (% usado, excedido) |
| POST   | `/api/budgets`                    | Crear/actualizar presupuesto de categoría (`categoryId,monthlyLimit`) |
| PUT    | `/api/budgets/{id}`               | Editar límite del presupuesto                 |
| DELETE | `/api/budgets/{id}`               | Eliminar presupuesto                          |
| GET    | `/api/savings-goals`              | Metas de ahorro (totales + progreso por meta) |
| POST   | `/api/savings-goals`              | Crear meta (`name,targetAmount,deadline?,initialAmount?`) |
| PUT    | `/api/savings-goals/{id}`         | Editar meta (nombre/objetivo/fecha)           |
| POST   | `/api/savings-goals/{id}/contribute` | Aportar (`amount>0`) o retirar (`amount<0`) |
| DELETE | `/api/savings-goals/{id}`         | Eliminar meta                                 |

**Vista compartida:** si el usuario pertenece a un hogar, `transactions`, `dashboard` y
`monthly` agregan los datos de **todos los miembros** (`HouseholdRepository.memberIds`). Las
escrituras y el borrado siguen siendo por dueño.

## Configuración (variables de entorno)

El backend lee de `application.conf` con override por env var:

| Variable                    | Default                                              |
|-----------------------------|------------------------------------------------------|
| `PORT`                      | `8080`                                                |
| `DATABASE_URL`              | `jdbc:postgresql://localhost:5432/control_financiero` |
| `DATABASE_USER`             | `postgres`                                             |
| `DATABASE_PASSWORD`         | `postgres`                                             |
| `MERCADOPAGO_ACCESS_TOKEN`  | (fallback global; cada usuario puede setear el suyo)  |
| `APP_ENV`                   | `development` (cualquier otro valor = "prod": activa el fail-fast del JWT) |
| `JWT_SECRET`                | `dev-secret-cambiar-en-produccion` (⚠️ con `APP_ENV≠development` el server **no arranca** si sigue el default) |
| `JWT_ISSUER` / `JWT_AUDIENCE` | `control-financiero` / `control-financiero-app`     |
| `JWT_VALIDITY_MS`           | `604800000` (7 días)                                  |

Nunca commitear tokens reales. `.env` y `local.properties` están en `.gitignore`.
Generar un secret fuerte (≥ 32 chars): `openssl rand -base64 48`. En prod, setear `APP_ENV=production` + `JWT_SECRET`.

## Convenciones

- Kotlin: la fuente vive en `src/main/kotlin/...` (no `java/`).
- Cualquier endpoint nuevo en el backend debe reflejarse en `ApiService.kt` del Android.
- Cualquier campo nuevo de DTO debe sincronizarse entre `models/dto/DTOs.kt` (backend) y
  `data/models/Models.kt` (Android), respetando los nombres JSON.
- Commits en español está bien; el repo es `veridian-ware/control-financiero`.

## Problemas conocidos / pendientes

- ✅ Resuelto (2026-05-30): agregado el Gradle wrapper 8.11.1 a ambos módulos.
- ✅ Resuelto (2026-05-30): `android/settings.gradle.kts` corregido a `dependencyResolutionManagement`.
- ✅ Resuelto (2026-05-30): agregado `android/app/proguard-rules.pro` (faltaba; rompía el build de release).
- ✅ Resuelto (2026-05-30): `MercadoPagoService` parseaba mal fechas con offset negativo (AR `-03:00`); ahora usa `OffsetDateTime`.
- ✅ Resuelto (2026-05-30): `getDashboard` ahora ordena por fecha DESC antes de `take(10)` (transacciones recientes).
- ✅ Resuelto (2026-05-31): `MercadoPagoService.syncPayments` ya no adivina ingreso/egreso por `operation_type`. Consulta `GET /users/me` una vez y clasifica por `collector_id`: si la cuenta cobró → **ingreso**, si no → **egreso** (`classify()`). Si `/users/me` falla, cae a la heurística previa.
- ⚠️ **A verificar (alcance del endpoint):** el caso de uso real es una **cuenta personal de pago** (servicios/compras → egresos). Pero `/v1/payments/search` es *collector-scoped*: lista pagos que la cuenta **cobró**, no necesariamente los que **pagó**. Riesgo: con una cuenta pagadora, la sync puede devolver pocos/ningún gasto. Validar con un token real; si no aparecen los gastos, hace falta otra fuente de datos (movimientos de cuenta MP), que es un cambio mayor.
- ✅ Verificado por CI (2026-05-30): backend y Android compilan en limpio en GitHub Actions (`.github/workflows/ci.yml`).
- ✅ Resuelto (2026-05-30): bug **preexistente** en `Tables.kt` — la columna se llamaba `source`, que choca con `ColumnSet.source` de Exposed 0.57 y hacía que el backend **no compilara**. Propiedad renombrada a `sourceCol` (la columna en la DB sigue siendo `"source"`). ⚠️ Gotcha al agregar columnas Exposed: evitar nombres que existan en `ColumnSet`/`Table`.
- ✅ Resuelto (2026-05-30): OOM de R8/D8 en CI → `org.gradle.jvmargs=-Xmx4g` en `android/gradle.properties`.
- ✅ Resuelto (2026-05-31): guard de `JWT_SECRET` — con `APP_ENV≠development` el backend hace fail-fast si sigue el secret por defecto; en dev solo advierte (`plugins/Security.kt`).
- ✅ Agregado (2026-05-31): **ingresos/egresos recurrentes** (`recurring_transactions`). Reemplazo práctico de "integrar Brubank" (que no tiene API pública oficial).
- ✅ Rediseñado (2026-06-02): los fijos ahora tienen **frecuencia** (`semanal`/`quincenal`/`mensual`) y generan **vencimientos** (`recurring_occurrences`) con estado **pendiente/pagado**. Ya **no se auto-materializan**: el dashboard solo cuenta lo marcado como pagado (marcar pagado crea la `Transaction`; despagar la borra). Se eliminaron `POST /api/recurring/run` y `materializeDue`. ⚠️ La generación es del **mes en curso** (meses no abiertos no generan ocurrencias). ⚠️ Gotcha: `recurring_occurrences.transaction_id` tiene FK a `transactions`, así que al despagar hay que **soltar la FK (set null) antes** de borrar la transacción. Y el `from` del dashboard ahora trunca a medianoche (arrastraba nanosegundos y no contaba transacciones fechadas a las 00:00).
- ✅ Agregado (2026-05-31): **hogar compartido** — dos personas (ej: pareja) comparten ingresos y gastos. Modelo elegido: visibilidad compartida vía `households` + `users.household_id`; cada transacción mantiene su dueño. ⚠️ Limitaciones MVP a evaluar: el dashboard agrupa "por categoría" por `categoryId`, así que categorías homónimas de distintos usuarios aparecen separadas; `getById`/`delete` siguen restringidos al dueño.
- ✅ Agregado (2026-05-31): **product flavors `emulator`/`device`** (dimensión `target`) para correr en celular físico sin editar la URL cada vez. `device` lee la IP de la PC de `local.properties` (`device.api.host`, gitignored) con fallback. ⚠️ Gotchas: la IP de DHCP puede cambiar (actualizar `device.api.host`); el celu necesita mismo Wi-Fi + puerto 8080 abierto en el firewall; el CI ahora compila ambos flavors en `assembleDebug` (el flavor `device` usa el fallback porque no hay `local.properties` en CI).
- ✅ Agregado (2026-06-02): **gráficos Vico** (`ReportsScreen`): balance mensual (line chart) + gasto por categoría (column chart), con datos que el dashboard ya carga (frontend only, sin backend). ⚠️ Gotcha API Vico 2.0.1: los ejes se crean con `VerticalAxis.rememberStart()` / `HorizontalAxis.rememberBottom()` — las clases `VerticalAxis`/`HorizontalAxis` se importan de `com.patrykandpatrick.vico.core.cartesian.axis` y las extensiones `rememberStart`/`rememberBottom` de `com.patrykandpatrick.vico.compose.cartesian.axis`. NO existen `rememberStartAxis`/`rememberBottomAxis`. El resto (CartesianChartHost, rememberCartesianChart, rememberLine/ColumnCartesianLayer, CartesianChartModelProducer, runTransaction { lineSeries/columnSeries { series(...) } }) está en los paquetes esperados.
- ✅ Agregado (2026-06-02): **import de CSV de Mercado Pago** (`POST /api/transactions/import`, `services/MpCsvImporter.kt`). Reemplaza la idea del botón OAuth "Conectá tu MP": la API de MP **no** expone las compras de un consumidor (`/payments/search` es collector-scoped, ver nota de arriba), pero el CSV de "dinero en cuenta" sí las trae. Parser `;`-delimitado por nombre de columna (no por posición); monto con signo (negativo→egreso, positivo→ingreso); `onlyPurchases` filtra por `BUSINESS_UNIT="Mercado Pago"` (descarta transferencias/retiros); dedup idempotente por `externalId="mpcsv_<SOURCE_ID>"` y `source="mercadopago_csv"`; categoría "Mercado Pago" auto-creada por tipo. UI: `SettingsScreen` → botón que abre el file picker (SAF `GetContent`), lee el CSV y lo postea. Smoke-test OK (2 importadas / 1 filtrada / 0 errores; reenvío 0/3/0 por dedup). ⚠️ El CSV no trae el nombre del comercio, solo el canal (`SUB_UNIT`: Wallet/QR/Checkouts) → la descripción queda "Mercado Pago · <canal>".
- ✅ Agregado (2026-06-03): **metas de ahorro** (`savings_goals`, `SavingsGoalRepository`, `SavingsGoalsScreen`). Carga manual y **standalone** (como inversiones, no toca transacciones ni cuentas): objetivo + ahorrado + fecha límite opcional. `contribute` suma (aporte) o resta (retiro) sobre el ahorrado, sin bajar de 0; el progreso (`progressPct` capeado 0-100, `remaining`, `reached`) se calcula al vuelo. Smoke-test OK (alta 20%, aporte/retiro, superar objetivo→pct 100/reached, update recalcula). ⚠️ Gotcha encontrado: olvidé registrar `SavingsGoals` en el `SchemaUtils` de `DatabaseFactory` → 500 "relation savings_goals does not exist". Toda tabla nueva va en esa lista.
- Roadmap (del README): auth JWT, gráficos Vico en la app, historial con filtros,
  notificaciones de gastos altos, export Excel/PDF, presupuestos por categoría,
  integración Brubank.

## Nota sobre el entorno de Claude Code

Si la sesión de Claude Code se abrió desde `C:\Users\Usuario\AndroidStudioProjects\Controlfinanciero`
(un template Android vacío y distinto), el código real está acá:
`C:\Users\Usuario\Apps\Control Financiero`. Trabajar siempre sobre esta carpeta.
