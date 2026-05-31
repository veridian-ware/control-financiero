# Control Financiero

[![CI](https://github.com/veridian-ware/control-financiero/actions/workflows/ci.yml/badge.svg)](https://github.com/veridian-ware/control-financiero/actions/workflows/ci.yml)

Aplicación de registro financiero personal con integración a Mercado Pago.

## Arquitectura

```
control-financiero/
├── backend/          # Servidor Ktor + PostgreSQL
│   └── src/main/kotlin/com/controlfinanciero/
│       ├── Application.kt
│       ├── database/         # DatabaseFactory (HikariCP + Exposed)
│       ├── models/
│       │   ├── db/           # Tablas Exposed (Categories, Transactions)
│       │   └── dto/          # DTOs serializables
│       ├── repositories/     # Lógica de acceso a datos
│       ├── routes/           # Endpoints REST
│       ├── services/         # MercadoPagoService
│       └── plugins/          # Configuración Ktor
└── android/          # App Android con Jetpack Compose
    └── app/src/main/kotlin/com/controlfinanciero/
        ├── MainActivity.kt
        ├── data/
        │   ├── api/          # Retrofit + ApiService
        │   └── models/       # Modelos compartidos
        └── ui/
            ├── screens/      # DashboardScreen, AddTransactionScreen
            ├── viewmodels/   # DashboardViewModel
            └── theme/        # Material3 Theme
```

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Backend | Kotlin + Ktor 3.0 |
| Base de datos | PostgreSQL + Exposed ORM |
| Android | Jetpack Compose + Material3 |
| HTTP Client | Retrofit + kotlinx.serialization |
| Gráficos | Vico Charts |
| Fintech API | Mercado Pago REST API |

## Setup

### Backend

1. Instalar PostgreSQL y crear la base de datos:
   ```sql
   CREATE DATABASE control_financiero;
   ```

2. Configurar variables de entorno (o editar `application.conf`):
   ```bash
   export DATABASE_URL=jdbc:postgresql://localhost:5432/control_financiero
   export DATABASE_USER=postgres
   export DATABASE_PASSWORD=tu_password
   export MERCADOPAGO_ACCESS_TOKEN=tu_access_token
   export JWT_SECRET=un_secreto_largo_y_aleatorio   # generar con: openssl rand -base64 48
   export APP_ENV=production                         # en prod: fuerza un JWT_SECRET propio
   ```
   > En producción (`APP_ENV` distinto de `development`) el server **no arranca** si el
   > `JWT_SECRET` sigue siendo el de desarrollo por defecto.

3. Ejecutar:
   ```bash
   cd backend
   ./gradlew run
   ```

4. Seed de categorías por defecto:
   ```bash
   curl -X POST http://localhost:8080/api/categories/seed
   ```

### Android

1. Abrir la carpeta `android/` en Android Studio.
2. El `API_BASE_URL` apunta a `http://10.0.2.2:8080` (localhost del emulador).
3. Build & Run.

### Mercado Pago

1. Obtener access token en [mercadopago.com.ar/developers](https://www.mercadopago.com.ar/developers)
2. Configurar la variable `MERCADOPAGO_ACCESS_TOKEN`
3. Sincronizar desde la app o via API:
   ```bash
   curl -X POST http://localhost:8080/api/mercadopago/sync
   ```

## API Endpoints

Todas las rutas excepto `register` y `login` requieren `Authorization: Bearer <token>`.
Cada usuario solo ve sus propios datos (multi-tenant).

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/register` | Crear cuenta → `{ token, user }` |
| POST | `/api/auth/login` | Iniciar sesión → `{ token, user }` |
| GET | `/api/auth/me` | Usuario autenticado |
| POST | `/api/mercadopago/token` | Guardar el token de Mercado Pago del usuario |
| GET | `/api/categories` | Listar categorías |
| POST | `/api/categories` | Crear categoría |
| POST | `/api/categories/seed` | Crear categorías por defecto |
| GET | `/api/transactions` | Listar transacciones (filtros: type, categoryId, from, to) |
| POST | `/api/transactions` | Crear transacción manual |
| DELETE | `/api/transactions/{id}` | Eliminar transacción |
| GET | `/api/dashboard` | Dashboard del mes actual |
| GET | `/api/dashboard/monthly/{year}` | Reporte mensual anual |
| POST | `/api/mercadopago/sync` | Sincronizar pagos de Mercado Pago |
| GET/POST/DELETE | `/api/recurring` | Ingresos/egresos fijos mensuales |
| GET/POST | `/api/household` | Hogar compartido (crear, ver) |
| POST | `/api/household/join` · `/leave` | Unirse por código / salir |

## Próximas funcionalidades

- [x] Autenticación JWT (multi-tenant, datos por usuario)
- [x] Ingresos/egresos recurrentes (ej: haberes mensuales, se registran solos)
- [x] Hogar compartido (dos personas sincronizan ingresos y gastos)
- [ ] Gráficos mensuales en la app (Vico Charts)
- [ ] Pantalla de historial con filtros
- [ ] Notificaciones de gastos altos
- [ ] Export a Excel/PDF
- [ ] Presupuestos por categoría
- [ ] Integración Brubank — pendiente: sin API pública oficial; el sueldo se cubre con recurrentes
