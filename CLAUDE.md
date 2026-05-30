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
- `BuildConfig.API_BASE_URL` = `http://10.0.2.2:8080` (localhost del backend desde el emulador).
- Build & Run sobre emulador o dispositivo.

## Arquitectura

### Backend (`backend/src/main/kotlin/com/controlfinanciero/`)
- `Application.kt` — entry point. `main()` usa `EngineMain` (config en `application.conf`).
  `Application.module()` inicializa DB y plugins en orden:
  `DatabaseFactory.init` → Serialization → CORS → StatusPages → Routing.
- `database/DatabaseFactory.kt` — HikariCP + Exposed, lee config de `application.conf`.
- `models/db/Tables.kt` — tablas Exposed (Categories, Transactions).
- `models/dto/DTOs.kt` — DTOs serializables de la API.
- `repositories/` — acceso a datos (`CategoryRepository`, `TransactionRepository`).
- `routes/` — endpoints REST. Se registran en `plugins/Routing.kt`.
- `services/MercadoPagoService.kt` — cliente Ktor hacia la API de Mercado Pago.
- `plugins/` — config de Ktor (CORS, Serialization, StatusPages, Routing).
- Config runtime: `src/main/resources/application.conf` (puerto, DB, Mercado Pago).

### Android (`android/app/src/main/kotlin/com/controlfinanciero/`)
- `MainActivity.kt` — host Compose.
- `data/api/ApiService.kt` — interfaz Retrofit (espejo de los endpoints del backend).
- `data/api/RetrofitClient.kt` — singleton Retrofit (OkHttp, timeouts 30s, JSON lenient).
- `data/models/Models.kt` — modelos compartidos + `ApiResponse<T>` wrapper.
- `ui/screens/` — `DashboardScreen`, `AddTransactionScreen`.
- `ui/viewmodels/DashboardViewModel.kt` — estado del dashboard.
- `ui/theme/Theme.kt` — Material3.

## API REST

Base: `http://localhost:8080`. Todas las respuestas envueltas en `ApiResponse<T>`.

| Método | Ruta                              | Descripción                                   |
|--------|-----------------------------------|-----------------------------------------------|
| GET    | `/api/categories`                 | Listar categorías (filtro `type`)             |
| POST   | `/api/categories`                 | Crear categoría                               |
| POST   | `/api/categories/seed`            | Crear categorías por defecto                  |
| GET    | `/api/transactions`               | Listar (filtros `type,categoryId,from,to,limit,offset`) |
| POST   | `/api/transactions`               | Crear transacción manual                      |
| DELETE | `/api/transactions/{id}`          | Eliminar transacción                          |
| GET    | `/api/dashboard`                  | Dashboard del mes actual                      |
| GET    | `/api/dashboard/monthly/{year}`   | Reporte mensual del año                       |
| POST   | `/api/mercadopago/sync`           | Sincronizar pagos (filtros `from,to,categoryId`) |

## Configuración (variables de entorno)

El backend lee de `application.conf` con override por env var:

| Variable                    | Default                                              |
|-----------------------------|------------------------------------------------------|
| `PORT`                      | `8080`                                                |
| `DATABASE_URL`              | `jdbc:postgresql://localhost:5432/control_financiero` |
| `DATABASE_USER`             | `postgres`                                             |
| `DATABASE_PASSWORD`         | `postgres`                                             |
| `MERCADOPAGO_ACCESS_TOKEN`  | (sin default — requerido para sync)                   |

Nunca commitear tokens reales. `.env` y `local.properties` están en `.gitignore`.

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
- ⚠️ **A verificar (lógica de negocio):** en `MercadoPagoService.syncPayments`, `operationType == "regular_payment"` se clasifica como **ingreso**. Según el rol de la cuenta MP esto puede estar invertido — validar con datos reales antes de confiar en el dashboard.
- ✅ Verificado por CI (2026-05-30): backend y Android compilan en limpio en GitHub Actions (`.github/workflows/ci.yml`).
- ✅ Resuelto (2026-05-30): bug **preexistente** en `Tables.kt` — la columna se llamaba `source`, que choca con `ColumnSet.source` de Exposed 0.57 y hacía que el backend **no compilara**. Propiedad renombrada a `sourceCol` (la columna en la DB sigue siendo `"source"`). ⚠️ Gotcha al agregar columnas Exposed: evitar nombres que existan en `ColumnSet`/`Table`.
- ✅ Resuelto (2026-05-30): OOM de R8/D8 en CI → `org.gradle.jvmargs=-Xmx4g` en `android/gradle.properties`.
- Roadmap (del README): auth JWT, gráficos Vico en la app, historial con filtros,
  notificaciones de gastos altos, export Excel/PDF, presupuestos por categoría,
  integración Brubank.

## Nota sobre el entorno de Claude Code

Si la sesión de Claude Code se abrió desde `C:\Users\Usuario\AndroidStudioProjects\Controlfinanciero`
(un template Android vacío y distinto), el código real está acá:
`C:\Users\Usuario\Apps\Control Financiero`. Trabajar siempre sobre esta carpeta.
