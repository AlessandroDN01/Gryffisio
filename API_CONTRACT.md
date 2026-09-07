# Contratto API — Gryffisio backend

Riferimento per lo sviluppo del frontend Angular che consumerà questo backend.
Non è pensato per essere ricreato da zero: quando crei il progetto Angular, copia
questo file nella root di quel progetto (Claude Code lo carica automaticamente se
si chiama `CLAUDE.md`, altrimenti tienilo come riferimento e citalo quando serve).

Questo documento descrive **il comportamento attuale** del backend, incluse le
incoerenze note — non è una specifica "ideale". Se qualcosa qui cambia lato
backend, questo file va riallineato.

## Base URL e CORS

- Dev: `http://localhost:8080`
- CORS già configurato per `http://localhost:4200` (`app.cors.allowed-origins` in
  `application.yaml`) — è **la porta di default di `ng serve`**, quindi in sviluppo
  locale non serve toccare nulla lato backend.
- Header consentiti: `Authorization`, `Content-Type`. Header esposto: `Authorization`.
  `allowCredentials = false` → niente cookie di sessione, solo header Bearer.

## Autenticazione

- `POST /api/auth/login` — pubblico. Body: `{ email, password }` → risposta
  `{ token }` (JWT).
- Nessun refresh token. Token scade dopo `app.jwt.ttl` (default 1h). Quando scade,
  il client deve rifare login — non c'è endpoint di refresh.
- `POST /api/auth/logout` — pubblico, no-op (204, non invalida nulla lato server:
  JWT stateless). Lato Angular basta scartare il token.
- Ogni richiesta autenticata: header `Authorization: Bearer <token>`.
- Claims nel JWT: `sub` (email), `uid` (id operatore, Integer), `roles` (array,
  es. `["ADMIN"]` o `["OPERATOR"]`). Utile per decodificare il token lato client
  (es. con `jwt-decode`) e sapere subito ruolo/id senza una chiamata aggiuntiva.
- Ruoli: `OPERATOR`, `ADMIN`. Non esiste self-registration: gli utenti li crea
  solo un ADMIN (vedi sotto).

## Formato errori

Tutte le risposte di errore hanno questa forma (`ErrorResponse`):

```json
{
  "timestamp": "2026-08-31T10:15:00",
  "status": 404,
  "error": "Not Found",
  "code": "Domain_not_found",
  "message": "Domain not found: 42",
  "path": "/api/domains/42"
}
```

`code` è stabile e pensato per essere usato in logica applicativa (es. switch nel
frontend), `message` è per il debug/log, non necessariamente da mostrare
all'utente così com'è. Status possibili: 400 (validazione o business rule), 401
(non autenticato), 403 (autenticato ma ruolo insufficiente), 404, 409 (conflitto,
es. email/nome duplicato), 500.

## Endpoint per risorsa

Legenda auth: **pub** = nessun token richiesto · **auth** = qualsiasi utente
loggato (OPERATOR o ADMIN) · **admin** = richiede ruolo ADMIN.

### Auth (`/api/auth`)
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| POST | `/login` | pub | `LoginRequest` → `LoginResponse` |
| POST | `/logout` | pub | — → 204 |

### Operatori (`/api/operators`) — utenti applicativi
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `/{id}` | pub | → `OperatorDto` |
| GET | `` | pub | → `OperatorDto[]` |
| POST | `` | **admin** | `CreateUserRequest` → `OperatorDto` (201) |
| PUT | `/{id}` | **admin** | `OperatorRequest` → `OperatorDto` |
| PUT | `/{id}/password` | **admin** | `ResetPasswordRequest` → `OperatorDto` |
| DELETE | `/{id}` | **admin** | → 204 |

`role` in `CreateUserRequest`/`OperatorRequest` è una stringa libera lato DTO ma
validata server-side contro l'enum (`"OPERATOR"` o `"ADMIN"`, case-insensitive) —
valore diverso → 400 `Invalid_role`.

### Progetti (`/api/projects`)
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `/{id}` | pub | → `ProjectDto` |
| GET | `` | pub | → `ProjectDto[]` |
| GET | `/name?name=` | pub | → `ProjectDto` *(da sistemare lato backend: dovrebbe diventare `?name=` sulla collection, per ora è un path fisso)* |
| GET | `/code?code=` | pub | → `ProjectDto` *(stesso discorso)* |
| POST | `` | **admin** | `ProjectRequest` → `ProjectDto` (201) |
| PUT | `/{id}` | **admin** | `ProjectRequest` → `ProjectDto` |
| DELETE | `/{id}` | **admin** | → 204 |

### Domini (`/api/domains`)
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `/{id}` | pub | → `DomainDto` |
| GET | `` | pub | → `DomainDto[]` |
| GET | `/name?name=` | pub | → `DomainDto` *(stesso discorso di projects/name)* |
| POST | `` | **admin** | `DomainRequest` → `DomainDto` (201) |
| PUT | `/{id}` | **admin** | `DomainRequest` → `DomainDto` |
| DELETE | `/{id}` | **admin** | → 204 |

### Attività (`/api/activities`) — gerarchia parent/child
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `/{id}` | pub | → `ActivityDto` |
| GET | `` | pub | → `ActivityDto[]` |
| GET | `/roots` | pub | → `ActivityDto[]` (solo attività senza parent) |
| GET | `/{parentId}/children` | pub | → `ActivityDto[]` |
| POST | `` | **admin** | `ActivityRequest` → `ActivityDto` (201) |
| PUT | `/{id}` | **admin** | `ActivityRequest` → `ActivityDto` |
| DELETE | `/{id}` | **admin** | → 204 |

`ActivityRequest.parentId` nullable = attività radice. Un'attività non può essere
parent di se stessa (400 `Activity_invalid_parent`).

### Dottori (`/api/doctors`)
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `/{id}` | pub | → `DoctorDto` |
| GET | `` | pub | → `DoctorDto[]` |
| GET | `/search?lastName=` | pub | → `DoctorDto[]` *(da sistemare: il segmento `/search` è superfluo, il filtro dovrebbe stare su `GET ''` con query param opzionale)* |
| POST | `` | **admin** | `DoctorRequest` → `DoctorDto` (201) |
| PUT | `/{id}` | **admin** | `DoctorRequest` → `DoctorDto` |
| DELETE | `/{id}` | **admin** | → 204 |

### Sessioni (`/api/sessions`)
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `/{id}` | pub | → `SessionDto` |
| GET | `` | pub | → `SessionDto[]` |
| POST | `` | **admin** | `SessionRequest` → `SessionDto` (201) |
| PUT | `/{id}` | **admin** | `SessionRequest` → `SessionDto` |
| DELETE | `/{id}` | **admin** | → 204 |

### Tipi soggetto (`/api/subject-types`)
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `/{id}` | pub | → `SubjectTypeDto` |
| GET | `` | pub | → `SubjectTypeDto[]` |
| POST | `` | **admin** | `SubjectTypeRequest` → `SubjectTypeDto` (201) |
| PUT | `/{id}` | **admin** | `SubjectTypeRequest` → `SubjectTypeDto` |
| DELETE | `/{id}` | **admin** | → 204 |

### Soggetti (`/api/subjects`)
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `/{id}` | pub | → `SubjectDto` |
| GET | `` | pub | → `SubjectDto[]` |
| GET | `/by-project/{projectId}` | pub | → `SubjectDto[]` *(da sistemare: dovrebbe essere `?projectId=` sulla collection)* |
| POST | `` | pub | `SubjectRequest` → `SubjectSaveResult` (201) |
| PUT | `/{id}` | **admin** | `SubjectRequest` → `SubjectSaveResult` |
| DELETE | `/{id}` | **admin** | → 204 |

**Attenzione (comportamento non ovvio):** `POST`/`PUT` su subject **non** fanno
deduplica. Se `code` esiste già per lo stesso `projectId`, il backend crea/aggiorna
comunque il record e restituisce `SubjectSaveResult.codeAlreadyExists = true` come
semplice segnale — sta al frontend decidere cosa fare (avviso, conferma, blocco).
Se non gestisci questo flag rischi soggetti duplicati silenziosi.

### Registrazioni (`/api/registrations`) — entità centrale
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `/{id}` | pub | → `RegistrationDto` |
| GET | `?projectId=&fromDate=&toDate=&operatorId=&activityId=&domainId=&page=&size=` | pub | → `PagedResponse<RegistrationDto>` |
| POST | `` | pub | `RegistrationRequest` → `RegistrationDto` (201) |
| PUT | `/{id}` | **admin** | `RegistrationRequest` → `RegistrationDto` |
| DELETE | `/{id}` | **admin** | → 204 |

`operatorIds` max 5 elementi (`@Size(max=5)`), `subjectIds`/`activityIds` non
vuoti. `PUT`/`DELETE` leggono l'id operatore dal claim `uid` del JWT per l'audit
log — **richiedono un token valido con quel claim**, non solo un utente generico.
La lista è paginata lato server: `page` è zero-based (default `0`) e `size` ha
default `5` e massimo `100`. I filtri sono tutti opzionali e cumulabili; `fromDate`
e `toDate` sono date ISO inclusive. I risultati sono ordinati per `activityDate`
decrescente e poi per id decrescente. Una pagina ha la forma
`{ items: RegistrationDto[], page: number, size: number, total: number, totalPages: number, hasNext: boolean }`.
Valori non numerici, paginazione non valida e `fromDate > toDate` restituiscono 400.

### Richieste di modifica (`/api/modification-requests`)
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `/{id}` | auth | → `ModificationRequestDto` |
| GET | `?status=` | auth | → `ModificationRequestDto[]` (status opzionale: `PENDING`/`APPROVED`/`REJECTED`, case-insensitive; senza filtro torna tutto; valore non valido → 400 `Invalid_status`) |
| POST | `` | pub | `ModificationRequestRequest` → `ModificationRequestDto` (201) |
| PUT | `/{id}` | **admin** | `ModificationRequestDecision` → `ModificationRequestDto` |

`PUT /{id}` è l'azione di approvazione/rifiuto (`decide`): `approve: true/false`,
`rejectionReason` obbligatorio solo lato UX se `approve = false` (non validato
server-side come NotBlank condizionale). Riservato ad ADMIN via `@PreAuthorize`
(fix applicato — in precedenza qualunque utente autenticato poteva chiamarlo).

### Audit log (`/api/audit-logs`) — sola lettura
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `` | auth | → `AuditLogDto[]` |
| GET | `/by-entity?entityName=&entityId=` | auth | → `AuditLogDto[]` *(da sistemare: `/by-entity` è superfluo, basterebbe il query param su `GET ''`)* |

### Dashboard / resoconto (`/api/dashboard`)
| Metodo | Path | Auth | Body → Risposta |
|---|---|---|---|
| GET | `?projectId=` | auth | → `DashboardDto` (`projectId` opzionale: se assente, `selectProjectRegistration` torna `null`) |

Questo è probabilmente l'endpoint principale per il resoconto finale che devi
costruire lato frontend — aggrega conteggi/ore per dominio e per operatore.

## DTO — forma esatta e validazioni

```ts
// --- Auth ---
LoginRequest        { email: string; password: string }               // email valida, entrambi obbligatori
LoginResponse        { token: string }

// --- Operator (= utente applicativo) ---
CreateUserRequest    { firstName: string; lastName: string; email: string; password: string; role: string }
// firstName/lastName max 50, email valida max 100
// password: >=12 caratteri, almeno 1 minuscola, 1 maiuscola, 1 cifra, 1 simbolo, non comune (es. "password123")
// role: "OPERATOR" | "ADMIN"
OperatorRequest       { firstName: string; lastName: string; email: string; role: string }   // update, NO password
ResetPasswordRequest  { newPassword: string }   // stesse regole di StrongPassword
OperatorDto           { id: number; firstName: string; lastName: string; email: string; role: string }

// --- Catalog semplici ---
ProjectDto      { id: number; name: string; code: string }
ProjectRequest  { name: string; code: string }              // name max 100, code max 20, entrambi univoci

DomainDto       { id: number; name: string; activityIds: number[]; activityNames: string[] }
DomainRequest   { name: string; activityIds: number[] }     // name max 100, univoco

ActivityDto     { id: number; name: string; parentId: number | null; parentName: string | null }
ActivityRequest { name: string; parentId: number | null }   // name max 100

DoctorDto       { id: number; firstName: string; lastName: string }
DoctorRequest   { firstName: string; lastName: string }     // entrambi max 50

SessionDto      { id: number; session: string }
SessionRequest  { session: string }                          // max 50, univoco

SubjectTypeDto     { id: number; type: string }
SubjectTypeRequest { type: string }                           // max 100

SubjectDto          { id: number; projectId: number; projectName: string; code: string; subjectTypeId: number; subjectTypeName: string }
SubjectRequest      { projectId: number; code: string; subjectTypeId: number }   // code max 50
SubjectSaveResult   { subject: SubjectDto; codeAlreadyExists: boolean }

// --- Registration (entità centrale) ---
RegistrationDto     {
    id: number; projectId: number; projectName: string;
    domainId: number; domainName: string;
    sessionId: number; sessionName: string;
    doctorId: number | null; doctorFullName: string | null;
    activityDate: string;          // ISO date "2026-08-31"
    durationMinutes: number;
    operatorIds: number[]; subjectIds: number[]; activityIds: number[];
    createdAt: string;             // ISO datetime
}
RegistrationRequest {
    projectId: number; domainId: number; sessionId: number; doctorId: number | null;
    activityDate: string; durationMinutes: number;
    operatorIds: number[];  // 1-5 elementi, obbligatorio
    subjectIds: number[];   // non vuoto
    activityIds: number[];  // non vuoto
}

// --- Modification request ---
ModificationRequestDto {
    id: number; registrationId: number; operatorId: number; operatorFullName: string;
    newActivityDate: string | null; newDurationMinutes: number | null;
    newSessionId: number | null; newDoctorId: number | null;
    reason: string; status: "PENDING" | "APPROVED" | "REJECTED";
    submittedAt: string; handledByAdminId: number | null; handledAt: string | null;
    rejectionReason: string | null;
}
ModificationRequestRequest {
    registrationId: number; operatorId: number;
    newActivityDate?: string; newDurationMinutes?: number; newSessionId?: number; newDoctorId?: number;
    reason: string;   // obbligatorio
}
ModificationRequestDecision { approve: boolean; rejectionReason?: string }

// --- Audit log ---
AuditLogDto { id: number; operatorIds: number[]; operatorNames: string[]; action: string; entityName: string; entityId: number; description: string | null; createdAt: string }

// --- Dashboard ---
DashboardDto        { totalRegistration: number; selectProjectRegistration: number | null; operatorMatrics: OperatorMatricsDto[]; domainMatrics: DomainMatricsDto[] }
OperatorMatricsDto  { operatorId: number; operatorName: string; registrationCount: number; totalHours: number }
DomainMatricsDto    { domainId: number; domainName: string; registrationCount: number; totalHours: number }

// --- Errori ---
ErrorResponse { timestamp: string; status: number; error: string; code: string; message: string; path: string }
```

## Cose da sapere prima di iniziare il frontend

1. **Paginazione registrazioni**: soltanto `GET /api/registrations` restituisce
   una pagina anziché un array. Invia `page` e `size` quando navighi tra le pagine
   e usa `items` per la lista visualizzata.
2. **Date**: `LocalDate` serializza come `"YYYY-MM-DD"`, `LocalDateTime` come ISO
   senza timezone (es. `"2026-08-31T10:15:00.123"`) — nessun timezone esplicito,
   trattale come locali.
3. **401 vs 403**: 401 = token assente/scaduto/invalido (redirect a login), 403 =
   token valido ma ruolo insufficiente (mostra un messaggio, non serve rifare
   login). Distinguili nell'interceptor HTTP Angular.
4. **`GlobalExceptionHandler`** copre già i casi comuni (400/401/403/404/409/500)
   con corpo JSON coerente (`ErrorResponse`) — puoi costruire un interceptor unico
   lato Angular che parsa sempre quella forma.
5. Endpoint marcati "da sistemare" sopra funzionano oggi così come descritti — se
   il backend viene aggiornato per usare query param al posto dei path fissi,
   questo file va aggiornato insieme al codice.