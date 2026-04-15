# TesisV3 — Sistema de Monitoreo de Salud

Aplicación Android de dos módulos para el monitoreo remoto de pacientes. El módulo **`app`** (móvil) permite a pacientes, médicos y cuidadores gestionar signos vitales, medicamentos y citas médicas. El módulo **`wearable`** (reloj Galaxy Watch) captura datos biométricos en tiempo real y los transmite al móvil.

---

## Tabla de contenidos

1. [Descripción general](#descripción-general)
2. [Arquitectura](#arquitectura)
3. [Estructura de carpetas](#estructura-de-carpetas)
4. [Módulo `app`](#módulo-app)
5. [Módulo `wearable`](#módulo-wearable)
6. [Backend y servicios externos](#backend-y-servicios-externos)
7. [Base de datos local](#base-de-datos-local)
8. [Pruebas unitarias](#pruebas-unitarias)
9. [Configuración y build](#configuración-y-build)

---

## Descripción general

| Módulo | Plataforma | Propósito |
|--------|-----------|-----------|
| `app` | Android (minSdk 30) | Panel principal, gestión de medicamentos, calendario de citas, roles Doctor/Cuidador/Paciente |
| `wearable` | Wear OS / Galaxy Watch | Captura de ECG, SpO₂ y frecuencia cardíaca mediante Samsung Health Sensor API |

El sistema distingue tres roles de usuario:

- **Paciente** — visualiza sus propios signos vitales, medicamentos y citas.
- **Doctor** — gestiona una lista de pacientes asignados, crea citas y medicamentos.
- **Cuidador** — supervisa a los pacientes a su cargo con permisos similares al Doctor.

---

## Arquitectura - Desafios

### ¿Por qué se adoptó esta arquitectura?

La versión propuesta inicialmete tenía todo el código de red, estado y lógica de negocio directamente dentro de las `Activity`. Esto generaba tres problemas concretos:

| Problema | Consecuencia |
|----------|-------------|
| Estado en `Activity` | Al rotar la pantalla, todos los datos de red se perdían y las llamadas HTTP se repetían innecesariamente |
| `readStream()` duplicado en 11 archivos | Un bug en la lectura del stream requería 11 correcciones simultáneas |
| Sin separación de responsabilidades | Imposible hacer pruebas unitarias sobre la lógica de negocio |

### Solución: MVVM + Clean Packages

Se adoptó el patrón **MVVM (Model-View-ViewModel)** de Android Jetpack combinado con una organización de paquetes por capa de responsabilidad:

```
Vista (Activity + Composables)
    ↕  observa estado / delega eventos
ViewModel (AndroidViewModel)
    ↕  hace llamadas de red / transforma datos
Red / Datos (network/, data/, iot/)
```

**Beneficios concretos obtenidos:**

- **Sobrevive rotaciones de pantalla**: el `ViewModel` vive más que la `Activity`. Los datos de red no se vuelven a pedir al girar el dispositivo.
- **`readStream()` una sola vez**: centralizado en `network/NetworkUtils.kt` y usado por todos.
- **Pruebas sin Android**: las funciones puras del `companion object` de los ViewModels se pueden testear con JUnit sin emulador.
- **Separación clara**: cada capa conoce solo lo que necesita conocer.

### Diagrama de capas (módulo `app`)

```
┌─────────────────────────────────────────────┐
│                  ui/                         │  Jetpack Compose + Activities
│  LoginActivity · DashboardActivity · ...     │  Solo UI: observan estado y delegan eventos
└────────────────────┬────────────────────────┘
                     │ by viewModels()
┌────────────────────▼────────────────────────┐
│               viewmodel/                     │  AndroidViewModel
│  DashboardViewModel · CalendarViewModel      │  Estado (mutableStateOf), coroutines,
│  CareViewModel                               │  lógica de negocio y formato
└──────┬─────────────────────────┬────────────┘
       │                         │
┌──────▼──────┐         ┌────────▼───────────┐
│  network/   │         │      data/          │
│ ApiConstants│         │  Room Database      │
│ NetworkUtils│         │  DAOs + Entities    │
│ AuthApi     │         └────────────────────┘
└──────┬──────┘
       │
┌──────▼──────┐   ┌──────────────┐
│    iot/     │   │   worker/    │
│ AzureIoT   │   │ WorkManager  │
│ MQTT/HTTP  │   │ background   │
└────────────┘   └──────────────┘
```

---

## Estructura de carpetas

### Módulo `app`

```
app/src/main/java/com/example/tesisv3/
│
├── MyApplication.kt              ← Punto de entrada: init de seguridad, WorkManager
├── AppContextHolder.kt           ← Singleton del contexto de aplicación
├── PatientSession.kt             ← Sesión global: tokens, usuario activo, patientId
├── UserRole.kt                   ← Enum UserRole + extensiones isDoctor/isPatient/isCaretaker
├── MyFirebaseMessagingService.kt ← Recepción de notificaciones push (FCM)
│
├── ui/                           ← Todas las pantallas (mismo paquete → se ven entre sí)
│   ├── AppBottomNav.kt           ← Barra de navegación inferior compartida + WatchStatusIcon
│   ├── RoleDrawerContent.kt      ← Drawer lateral con menú adaptado a cada rol
│   ├── LoginActivity.kt          ← Autenticación + JWT
│   ├── DashboardActivity.kt      ← Signos vitales en tiempo real, steps, recordatorios
│   ├── CalendarActivity.kt       ← Calendario de citas médicas (crear/editar/eliminar)
│   ├── CareActivity.kt           ← Gestión de medicamentos activos
│   ├── ReportsActivity.kt        ← Reportes y analíticas del paciente
│   ├── NotificationsActivity.kt  ← Historial de notificaciones recibidas
│   ├── SettingsActivity.kt       ← Configuración de la app (IoT transport, etc.)
│   ├── GroupsActivity.kt         ← Vista de grupos / relaciones del paciente
│   ├── MainActivity.kt           ← Splash / router inicial
│   ├── DoctorPatientsActivity.kt       ─┐
│   ├── DoctorPatientsListActivity.kt    ├─ Panel del médico
│   ├── DoctorPatientProfileActivity.kt ─┘
│   ├── CaretakerNoPatientsActivity.kt  ← Vista vacía para cuidador sin pacientes
│   ├── ForgotPasswordActivity.kt  ─┐
│   ├── VerifyResetCodeActivity.kt  ├─ Flujo de recuperación de contraseña
│   ├── ResetPasswordActivity.kt   ─┘
│   ├── PatientRegistrationActivity.kt    ─┐
│   ├── DoctorRegistrationActivity.kt     ├─ Registro por rol
│   └── CaretakerRegistrationActivity.kt ─┘
│
├── viewmodel/                    ← AndroidViewModels: estado + lógica de negocio
│   ├── DashboardViewModel.kt     ← Vitales, steps, medicamentos del día, recordatorios
│   ├── CalendarViewModel.kt      ← Citas médicas + lista de pacientes
│   └── CareViewModel.kt          ← Medicamentos CRUD + lista de pacientes
│
├── network/                      ← Capa de red (sin Android framework, testeable)
│   ├── ApiConstants.kt           ← URLs base de los microservicios backend
│   ├── NetworkUtils.kt           ← readStream() y buildJsonBody() compartidos
│   └── AuthApi.kt                ← Llamadas de autenticación (forgot/reset password)
│
├── data/                         ← Persistencia local con Room
│   ├── AppDatabase.kt            ← Base de datos (v4), migraciones incluidas
│   ├── MedicationEntity.kt / MedicationDao.kt
│   ├── MedicationLogEntity.kt / MedicationLogDao.kt
│   ├── NotificationEntity.kt / NotificationDao.kt
│   └── AppointmentEntity.kt / AppointmentDao.kt
│
├── iot/                          ← Capa de comunicación con Azure IoT Hub
│   ├── AzureIotClient.kt         ← Envío de mensajes al Hub (HTTPS / MQTT)
│   ├── DeviceRegistrationManager.kt ← Registro y UUID del dispositivo
│   ├── IotSettings.kt            ← Configuración de transporte (SharedPreferences)
│   ├── IotTransport.kt           ← Interfaz común de transporte
│   ├── HttpTransport.kt          ← Implementación HTTPS
│   └── MqttTransport.kt          ← Implementación MQTT / MQTT-WS
│
└── worker/                       ← Tareas en segundo plano (WorkManager)
    ├── MedicationReminderWorker.kt ← Notificaciones de medicamentos según horario
    └── DailyResetWorker.kt         ← Reinicio de estados a medianoche
```

### Módulo `wearable`

```
wearable/src/main/java/com/example/tesisv3/
│
├── ui/
│   └── activities/
│       ├── MainActivity.kt       ← Pantalla principal: permisos, conexión, envío de datos
│       ├── DetailsActivity.java  ← Vista de detalle de métricas
│       └── Main2Activity.java    ← Vista alternativa de sensores
│
├── device/
│   ├── listeners/
│   │   ├── BaseListener.java     ← Base abstracta de listeners de sensores
│   │   ├── EcgListener.java      ← Captura de ECG (Samsung Health Sensor API)
│   │   ├── HeartRateListener.java ← Captura de frecuencia cardíaca
│   │   └── SpO2Listener.java     ← Captura de saturación de oxígeno
│   └── managers/
│       ├── ConnectionManager.java   ← Gestión del canal Wear Data API → móvil
│       └── ConnectionObserver.java  ← Callbacks de estado de conexión
│
├── domain/
│   └── entities/
│       ├── EcgData.java
│       ├── HeartRateData.java / HeartRateStatus.java
│       ├── SpO2Status.java
│       ├── TrackerDataNotifier.java   ← Publisher del patrón Observer
│       ├── TrackerDataObserver.java   ← Interfaz Observer para datos de sensores
│       └── WearData.java             ← Wrapper unificado de métricas
│
└── data/                         ← (reservado para implementación futura)
    ├── datasource/
    ├── models/
    └── repository/
```

---

## Módulo `app`

### Roles y flujo de navegación

```
Login
  ├─ PATIENT  → DashboardActivity (vitales propios)
  ├─ DOCTOR   → DoctorPatientsActivity (panel de pacientes)
  └─ CARETAKER → DoctorPatientsActivity (pacientes a cargo)
```

Todos los roles comparten la barra inferior (`AppBottomNav`) y el drawer lateral (`RoleDrawerContent`), pero los ítems se adaptan dinámicamente al rol del usuario en sesión (`PatientSession.currentUser`).

### Estado de sesión

`PatientSession` es un `object` Kotlin (singleton) que persiste en memoria durante el ciclo de vida de la app:

```kotlin
object PatientSession {
    var patientId: String       // ID del paciente activo
    var currentUser: UserProfile?  // Perfil del usuario logueado
    var accessToken: String?    // JWT de acceso
    var refreshToken: String?
    var resetCode: String?      // Código temporal de reset de contraseña
}
```

### ViewModels

Cada pantalla principal tiene su propio `AndroidViewModel`. El estado se declara con `mutableStateOf` para que Compose recomponga automáticamente:

```kotlin
class CareViewModel(app: Application) : AndroidViewModel(app) {
    var medications by mutableStateOf<List<ApiMedication>>(emptyList())
        private set
    // ...
    init { loadData() }   // carga automática al crear el ViewModel
}
```

Los datos de red se obtienen en `Dispatchers.IO` dentro de `viewModelScope`, por lo que no bloquean el hilo principal y sobreviven rotaciones de pantalla.

### IoT Hub

`AzureIotClient` soporta tres protocolos de transporte, seleccionables en `SettingsActivity`:

| Protocolo | Clase | Uso recomendado |
|-----------|-------|----------------|
| HTTPS | `HttpTransport` | Redes corporativas restrictivas |
| MQTT | `MqttTransport` | Conexión directa, baja latencia |
| MQTT sobre WebSocket | `MqttTransport` (WS) | Redes con firewall que bloquean el puerto 8883 |

La cadena de conexión se inyecta en tiempo de compilación desde `local.properties` (nunca en el repositorio):

```
# local.properties
azureIotConnectionString=HostName=TU_HUB.azure-devices.net;DeviceId=TU_DEVICE;SharedAccessKey=TU_KEY
HostName=TU_HUB.azure-devices.net
```

---

## Módulo `wearable`

El reloj usa la **Samsung Health Sensor API v1.4.1** para acceder a los sensores de salud del Galaxy Watch. Los datos se envían al móvil a través de la **Wear Data API** de Google.

### Flujo de datos

```
Sensor (ECG / HR / SpO₂)
    ↓ SensorListener
TrackerDataNotifier  (patrón Observer)
    ↓ notifica a todos los TrackerDataObserver registrados
MainActivity
    ↓ Wear Data API (/wear/json)
DashboardActivity (app móvil)
    ↓ onMessageReceived()
DashboardViewModel.onWearableMessageReceived()
    ↓ actualiza mutableStateOf → Compose recompone
```

### Paths de mensajería Wear

| Path | Dirección | Contenido |
|------|-----------|-----------|
| `/APP_OPEN_WEARABLE_PAYLOAD` | Móvil → Reloj | Handshake de apertura |
| `/wear/json` | Reloj → Móvil | JSON con métricas biométricas |
| `/message-item-received` | Reloj → Móvil | Confirmación de recepción |

### Permisos especiales (Samsung)

El módulo requiere permisos específicos del fabricante además de los estándar de Android Health:

```xml
<uses-permission android:name="com.samsung.android.wear.permission.ECG" />
<uses-permission android:name="com.samsung.android.hardware.sensormanager.permission.READ_ADDITIONAL_HEALTH_DATA" />
```

---

## Backend y servicios externos

El backend está desplegado en **Azure Container Apps** (región Central US). Sigue una arquitectura de microservicios:

| Servicio | Responsabilidad |
|----------|----------------|
| `user-service` | Autenticación JWT, registro de usuarios, relaciones Doctor-Paciente-Cuidador |
| `calendar-service` | CRUD de citas médicas y medicamentos |
| `analytics-service` | Historial de signos vitales y reportes |

La comunicación es REST + JSON. Todos los endpoints requieren el header:
```
Authorization: Bearer <accessToken>
```

La función `readStream()` en `NetworkUtils.kt` es la única implementación de lectura de respuestas HTTP en todo el proyecto — centralizada y testeada.

---

## Base de datos local

Room database versión 4, archivo `tesisv3.db`:

| Entidad | Descripción |
|---------|-------------|
| `MedicationEntity` | Medicamentos guardados localmente para acceso offline |
| `MedicationLogEntity` | Registro histórico de tomas (migración v1→v2) |
| `NotificationEntity` | Notificaciones recibidas del backend (migración v2→v3) |
| `AppointmentEntity` | Citas médicas sincronizadas (migración v3→v4) |

Los workers de `WorkManager` acceden directamente a la base de datos mediante los DAOs:

- **`MedicationReminderWorker`** — lanza notificaciones locales según el horario de cada medicamento.
- **`DailyResetWorker`** — se ejecuta a medianoche; limpia estados temporales y sincroniza con el backend.

---

## Pruebas unitarias

El proyecto incluye **115 pruebas unitarias** que corren en la JVM (sin emulador):

```
./gradlew :app:testDebugUnitTest
```

| Clase de test | Tests | Qué cubre |
|---------------|-------|-----------|
| `UserRoleTest` | 27 | `UserRole.from()`, extensiones `isDoctor/isPatient/isCaretaker` en `UserProfile` nullable |
| `NetworkUtilsTest` | 18 | `buildJsonBody()` (inyección JSON, nulos, unicode), `readStream()` (null, streams rotos) |
| `CalendarViewModelTest` | 10 | `parseUtcMillis()`: ISO 8601 válidos, fallback en entrada inválida, orden temporal |
| `DashboardViewModelTest` | 60 | `isMedForToday`, `isTimeDue`, `formatMedTime`, `buildReminderTimeLabel`, `formatRecurrence`, `formatAppointmentTimeLabel`, `formatVitalsTimestamp`, `buildWearablePayload` |

Las pruebas están completamente desacopladas del framework Android. La configuración `isReturnDefaultValues = true` en `testOptions` garantiza que las clases de Android (como `android.util.Log`) retornen valores por defecto en lugar de lanzar excepciones.

---

## Configuración y build

### Requisitos

- Android Studio Hedgehog o superior
- JDK 17
- Galaxy Watch con Samsung Health Sensor API (para el módulo wearable)
- Cuenta Azure con IoT Hub configurado

### Variables locales (no committear)

Crear el archivo `local.properties` en la raíz del proyecto:

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
azureIotConnectionString=HostName=TU_HUB.azure-devices.net;DeviceId=TU_DEVICE;SharedAccessKey=TU_KEY
HostName=TU_HUB.azure-devices.net
```

### Comandos principales

```bash
# Compilar app móvil (debug)
./gradlew :app:assembleDebug

# Compilar app wearable (debug)
./gradlew :wearable:assembleDebug

# Correr todas las pruebas unitarias
./gradlew :app:testDebugUnitTest

# Correr pruebas forzando re-ejecución (sin caché)
./gradlew :app:testDebugUnitTest --rerun-tasks

# Ver reporte HTML de pruebas
# app/build/reports/tests/testDebugUnitTest/index.html
```

### Stack tecnológico

| Categoría | Tecnología |
|-----------|-----------|
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM, AndroidViewModel, mutableStateOf |
| Navegación | Activity-based con Bottom Navigation + Drawer |
| Red | HttpURLConnection (sin Retrofit) + `readStream()` centralizado |
| Persistencia | Room 2.7 (SQLite) |
| Background | WorkManager 2.9 |
| IoT | Azure IoT Hub (HTTPS / MQTT / MQTT-WS) |
| Push | Firebase Cloud Messaging (FCM) |
| Wearable | Samsung Health Sensor API 1.4.1 + Wear Data API |
| Autenticación | JWT (access token + refresh token) |
| Testing | JUnit 4, `org.json:json`, `kotlinx-coroutines-test` |
| Build | Gradle 8.11.1, compileSdk 36, minSdk 30, JVM 17 |
