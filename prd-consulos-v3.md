# PRD — ConsulOS v3.0
### ERP Consulaire · Optimisé pour Cursor AI
> **Stack** : Spring Boot 3.3 · React 18 · PostgreSQL 16 · Docker Compose  
> **Cible** : Postes diplomatiques africains en Europe (B2G SaaS)  
> **Méthode** : Développement assisté Cursor AI — Blocs séquentiels A/B/C/D/E

---

## Table des matières

1. [Vision & Contexte Produit](#1-vision--contexte-produit)
2. [Stack Technique & Architecture](#2-stack-technique--architecture)
3. [Structure du Projet](#3-structure-du-projet)
4. [Modèle de Données & Migrations Flyway](#4-modèle-de-données--migrations-flyway)
5. [Modules MVP — Détail Fonctionnel & API](#5-modules-mvp--détail-fonctionnel--api)
6. [Sécurité & Conformité](#6-sécurité--conformité)
7. [Prompts Cursor AI — Blocs d'Implémentation](#7-prompts-cursor-ai--blocs-dimplémentation)
8. [Infrastructure & Déploiement](#8-infrastructure--déploiement)
9. [Roadmap & Priorités](#9-roadmap--priorités)
10. [Questions Ouvertes & Hypothèses](#10-questions-ouvertes--hypothèses)

---

## 1. Vision & Contexte Produit

### 1.1 Vision

ConsulOS est un ERP léger, modulaire et déployable rapidement pour les consulats et ambassades africains établis en Europe. Il centralise dans une seule interface web toutes les opérations consulaires quotidiennes : accueil des usagers, gestion des dossiers et demandes, rendez-vous, documents justificatifs, encaissements, tableaux de bord de pilotage et administration du poste.

L'objectif est de remplacer la fragmentation actuelle entre registres papier, tableurs Excel, messageries email et outils disparates par un système unique, traçable et exploitable — à l'échelle d'un poste isolé ou d'un réseau multi-consulats.

### 1.2 Problème métier

- Opérations dispersées : papier, tableurs, boîtes mail, agendas et fichiers locaux non contrôlés.
- Traitement ralenti, reporting difficile, risque élevé d'erreurs et de pertes d'information.
- Absence de traçabilité sur les actions sensibles (consultation / modification de données personnelles).
- Difficulté d'audit interne et de pilotage pour les responsables de poste.
- Besoin croissant de conformité RGPD et de souveraineté des données.

### 1.3 Positionnement produit

ConsulOS n'est ni un grand ERP étatique (trop lourd à déployer) ni un simple agenda de rendez-vous (trop limité). Il occupe le créneau intermédiaire : assez léger pour être adopté en quelques semaines par un consulat africain en Europe, assez structuré pour centraliser la gestion quotidienne et produire un reporting fiable, et assez modulaire pour s'étendre vers des fonctions avancées (e-paiement, OCR, intégrations nationales) au fil des phases.

### 1.4 Utilisateurs cibles

**Internes**

| Rôle | Description |
|---|---|
| Agent d'accueil | Enregistre les visiteurs, vérifie les rendez-vous, oriente les usagers |
| Agent métier | Traite les dossiers : état civil, visas, passeports, cartes consulaires, légalisations |
| Caissier / agent habilité | Enregistre les frais, reçus et moyens de paiement |
| Superviseur de service | Suit les files, retards, productivité et exceptions |
| Chef de chancellerie | Consulte les tableaux de bord et pilote la charge |
| Administrateur système | Gère comptes, rôles, paramètres, référentiels et sécurité |

**Externes**

- Usager diaspora : crée ou complète une demande, fournit des documents, suit son dossier.
- Représentant mandaté : agit pour un tiers selon les règles du consulat.

### 1.5 Modèle économique

- Abonnement annuel B2G par poste diplomatique ou par réseau multi-postes.
- Mise en service et paramétrage initial facturés séparément.
- Modules additionnels payants : SMS/email, paiements en ligne, stockage avancé, support premium, reporting centralisé multi-sites.

---

## 2. Stack Technique & Architecture

### 2.1 Vue d'ensemble de la stack

| Couche | Technologie | Justification |
|---|---|---|
| **Frontend** | React 18 + Vite 5 + TypeScript 5 | SPA rapide, typage fort, hot reload |
| **UI / Design** | Tailwind CSS 3 + shadcn/ui + Radix UI | Composants accessibles, personnalisables |
| **State** | Zustand 4 + TanStack Query 5 | État local léger + cache serveur intelligent |
| **Formulaires** | React Hook Form + Zod | Validation déclarative end-to-end |
| **Routing** | React Router v6 | SPA navigation, routes protégées par rôle |
| **PDF Client** | react-pdf + @react-pdf/renderer | Aperçu + génération reçus côté client |
| **Charts** | Recharts 2 | Tableaux de bord réactifs |
| **i18n** | i18next + react-i18next | FR / EN / PT multilingue |
| **Backend** | Spring Boot 3.3 + Java 21 | Virtual threads, dernières LTS |
| **Sécurité** | Spring Security + JWT RS256 | Tokens signés, refresh token rotation |
| **Persistance** | Spring Data JPA + Hibernate 6 | ORM mature, requêtes natives si besoin |
| **Migrations** | Flyway 10 | Versioning SQL traçable |
| **BDD** | PostgreSQL 16 | JSONB, row-level security, audit |
| **Cache** | Redis 7 (Lettuce) | Sessions, rate limiting, queue légère |
| **Fichiers** | MinIO (compatible S3) | Stockage objets self-hosted |
| **PDF Serveur** | iText 7 Community | Génération reçus et rapports |
| **Email** | Spring Mail + Mailgun/Brevo | Notifications transactionnelles |
| **SMS** | Twilio / OVH SMS (optionnel) | Confirmations RDV, alertes |
| **WebSocket** | Spring WebSocket + STOMP | Notifications temps réel (file d'attente) |
| **Conteneurs** | Docker + Docker Compose | Déploiement reproductible |
| **Reverse Proxy** | Nginx | SSL/TLS, routing, static files |
| **Monitoring** | Spring Actuator + Prometheus + Grafana | Métriques, alertes, tableaux de bord ops |
| **Logs** | Logback + ELK Stack (optionnel) | Centralisation et recherche de logs |
| **Tests Back** | JUnit 5 + Mockito + Testcontainers | Tests unitaires et d'intégration DB |
| **Tests Front** | Vitest + React Testing Library + Playwright | Unitaires + E2E |
| **API Docs** | SpringDoc OpenAPI 3 (Swagger UI) | Documentation auto-générée |
| **Qualité** | SonarLint + Checkstyle + ESLint + Prettier | Norme de code dans Cursor AI |

### 2.2 Patterns architecturaux

#### Backend — Règles non négociables pour Cursor AI

- **Multi-tenant** : isolation par `organisation_id` sur toutes les entités et requêtes.
- **RBAC** : annotations `@PreAuthorize("hasRole('...')")` sur tous les endpoints.
- **Réponses unifiées** : `ApiResponse<T>` et `PageResponse<T>` pour toutes les réponses REST.
- **Entité de base** : `BaseEntity` avec `id` (UUID), `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `organisationId`.
- **Exceptions métier** : `BusinessException(String errorCode, String message, HttpStatus status)` — jamais de `RuntimeException` brute.
- **Migrations** : Flyway — un fichier `V{N}__description.sql` par fonctionnalité.
- **Nouveau code = nouveaux fichiers uniquement**. Ne jamais modifier les services, contrôleurs ou entités existants.
- **Logging** : SLF4J + MDC avec `userId`, `organisationId`, `requestId` sur chaque requête.

#### Frontend — Règles non négociables pour Cursor AI

- Composants fonctionnels React avec TypeScript strict.
- Aucun appel API direct dans les composants — passer obligatoirement par TanStack Query.
- Zustand pour l'état global (auth, user, organisation) — pas de Context API pour l'état partagé.
- Zod pour la validation de tous les formulaires et les types API.
- Séparation stricte : `/pages`, `/components`, `/hooks`, `/services`, `/stores`, `/types`.
- Responsive first : grilles Tailwind, breakpoints `sm/md/lg/xl` définis dans `tailwind.config.ts`.
- Tokens d'accès stockés uniquement en mémoire (Zustand), refresh token en `httpOnly` cookie.

---

## 3. Structure du Projet

### 3.1 Arborescence backend

```
consulos-backend/
├── src/main/java/com/consulos/
│   ├── config/          # SecurityConfig, JwtConfig, CorsConfig, RedisConfig
│   ├── common/          # BaseEntity, ApiResponse, PageResponse, BusinessException
│   ├── auth/            # AuthController, AuthService, JwtService, RefreshTokenService
│   ├── user/            # UserController, UserService, UserRepository, UserEntity, UserDTO
│   ├── usager/          # UsagerController, UsagerService, UsagerRepository, UsagerEntity
│   ├── dossier/         # DossierController, DossierService, DossierRepository, DossierEntity
│   ├── rendezvous/      # RdvController, RdvService, RdvRepository, RdvEntity
│   ├── document/        # DocumentController, DocumentService, DocumentRepository
│   ├── encaissement/    # EncaissementController, EncaissementService, EncaissementRepository
│   ├── dashboard/       # DashboardController, DashboardQueryService
│   ├── audit/           # AuditLogEntity, AuditAspect, AuditRepository
│   ├── notification/    # NotificationController, NotificationService, WebSocketConfig
│   └── administration/  # AdminController, RoleService, OrganisationService
├── src/main/resources/
│   ├── db/migration/    # V1__init.sql, V2__users.sql, ... V8__audit.sql
│   └── application.yml  # Profils : dev, prod, test
└── src/test/            # Tests par module
```

### 3.2 Arborescence frontend

```
consulos-frontend/
├── src/
│   ├── pages/
│   │   ├── auth/           # Login, ForgotPassword
│   │   ├── accueil/        # AccueilPage, FilePage
│   │   ├── usagers/        # UsagersPage, UsagerDetailPage, UsagerCreatePage
│   │   ├── dossiers/       # DossiersPage, DossierDetailPage, DossierCreatePage
│   │   ├── rendezvous/     # AgendaPage, RdvDetailPage
│   │   ├── documents/      # DocumentsPage
│   │   ├── encaissement/   # CaissePage, RecuPage
│   │   ├── dashboard/      # DashboardPage
│   │   └── administration/ # UsersPage, RolesPage, ParametresPage
│   ├── components/
│   │   ├── ui/             # shadcn/ui re-exports
│   │   ├── layout/         # AppShell, Sidebar, Header, Breadcrumb
│   │   ├── shared/         # DataTable, SearchBar, StatusBadge, ConfirmDialog
│   │   └── [module]/       # Composants spécifiques par module
│   ├── hooks/              # useAuth, useOrganisation, useDossier, useRdv, ...
│   ├── services/           # apiClient.ts, authService.ts, usagerService.ts, ...
│   ├── stores/             # authStore.ts, uiStore.ts
│   ├── types/              # index.ts, api.ts, usager.ts, dossier.ts, ...
│   ├── lib/                # queryClient.ts, i18n.ts, utils.ts
│   └── router/             # AppRouter.tsx, ProtectedRoute.tsx, routes.ts
```

---

## 4. Modèle de Données & Migrations Flyway

### V1 — Organisations

```sql
-- V1__base_and_organisations.sql
CREATE TABLE organisations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(20) UNIQUE NOT NULL,
    nom             VARCHAR(200) NOT NULL,
    pays_code       VARCHAR(3) NOT NULL,       -- ISO 3166-1 alpha-3
    langue_defaut   VARCHAR(5) DEFAULT 'fr',
    devise_defaut   VARCHAR(3) DEFAULT 'EUR',
    actif           BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
```

### V2 — Users & Roles

```sql
-- V2__users_and_roles.sql
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50) UNIQUE NOT NULL,   -- ROLE_ADMIN, ROLE_AGENT, etc.
    libelle     VARCHAR(100) NOT NULL,
    permissions JSONB DEFAULT '[]'
);

CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id    UUID NOT NULL REFERENCES organisations(id),
    email              VARCHAR(255) UNIQUE NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    nom                VARCHAR(100) NOT NULL,
    prenom             VARCHAR(100) NOT NULL,
    role_id            UUID NOT NULL REFERENCES roles(id),
    actif              BOOLEAN DEFAULT true,
    derniere_connexion TIMESTAMPTZ,
    created_at         TIMESTAMPTZ DEFAULT NOW(),
    updated_at         TIMESTAMPTZ DEFAULT NOW(),
    created_by         UUID REFERENCES users(id)
);
```

### V3 — Usagers

```sql
-- V3__usagers.sql
CREATE TABLE usagers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    numero_dossier  VARCHAR(30) UNIQUE,         -- ORG-YYYYNNNNN
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    date_naissance  DATE,
    lieu_naissance  VARCHAR(200),
    nationalite     VARCHAR(3),                 -- ISO 3166-1 alpha-3
    type_usager     VARCHAR(30),                -- DIASPORA, MANDATAIRE, AUTRE
    telephone       VARCHAR(30),
    email           VARCHAR(255),
    adresse         TEXT,
    numero_document VARCHAR(50),
    type_document   VARCHAR(30),                -- PASSEPORT, CNI, TITRE_SEJOUR
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id)
);
```

### V4 — Dossiers & Workflow

```sql
-- V4__dossiers.sql
CREATE TABLE types_service (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    code            VARCHAR(50) NOT NULL,       -- PASSEPORT, ETAT_CIVIL, etc.
    libelle         VARCHAR(200) NOT NULL,
    delai_jours     INTEGER DEFAULT 30,
    tarif_defaut    DECIMAL(10,2),
    workflow_config JSONB DEFAULT '{}',         -- transitions autorisées
    checklist       JSONB DEFAULT '[]',         -- pièces requises
    actif           BOOLEAN DEFAULT true
);

CREATE TABLE dossiers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    reference       VARCHAR(30) UNIQUE NOT NULL, -- DOS-YYYY-NNNNN
    usager_id       UUID NOT NULL REFERENCES usagers(id),
    type_service_id UUID NOT NULL REFERENCES types_service(id),
    statut          VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
    -- BROUILLON | SOUMIS | INCOMPLET | EN_REVUE | VALIDE | REJETE | CLOTURE
    agent_id        UUID REFERENCES users(id),
    priorite        VARCHAR(20) DEFAULT 'NORMALE',
    date_soumission TIMESTAMPTZ,
    date_echeance   TIMESTAMPTZ,
    motif_rejet     TEXT,
    notes_internes  TEXT,
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id)
);

CREATE TABLE dossier_historique (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dossier_id  UUID NOT NULL REFERENCES dossiers(id),
    statut_de   VARCHAR(30),
    statut_vers VARCHAR(30) NOT NULL,
    commentaire TEXT,
    user_id     UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
```

### V5 — Rendez-vous

```sql
-- V5__rendezvous.sql
CREATE TABLE rendezvous (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    usager_id       UUID NOT NULL REFERENCES usagers(id),
    dossier_id      UUID REFERENCES dossiers(id),
    type_service_id UUID REFERENCES types_service(id),
    agent_id        UUID REFERENCES users(id),
    date_heure      TIMESTAMPTZ NOT NULL,
    duree_minutes   INTEGER DEFAULT 30,
    statut          VARCHAR(20) DEFAULT 'CONFIRME',
    -- CONFIRME | EN_ATTENTE | PRESENT | ABSENT | ANNULE | REPORTE
    motif           TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
```

### V6 — Documents

```sql
-- V6__documents.sql
CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    dossier_id      UUID REFERENCES dossiers(id),
    usager_id       UUID REFERENCES usagers(id),
    nom_fichier     VARCHAR(255) NOT NULL,
    type_document   VARCHAR(100) NOT NULL,
    mime_type       VARCHAR(100),
    taille_octets   BIGINT,
    chemin_stockage VARCHAR(500),               -- MinIO object key
    statut_controle VARCHAR(30) DEFAULT 'RECU',
    -- RECU | LISIBLE | COMPLET | A_CORRIGER | VALIDE
    version         INTEGER DEFAULT 1,
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id)
);
```

### V7 — Encaissements

```sql
-- V7__encaissements.sql
CREATE TABLE tarifs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    type_service_id UUID REFERENCES types_service(id),
    libelle         VARCHAR(200) NOT NULL,
    montant         DECIMAL(10,2) NOT NULL,
    devise          VARCHAR(3) DEFAULT 'EUR',
    actif           BOOLEAN DEFAULT true
);

CREATE TABLE encaissements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    dossier_id      UUID REFERENCES dossiers(id),
    usager_id       UUID NOT NULL REFERENCES usagers(id),
    numero_recu     VARCHAR(30) UNIQUE NOT NULL, -- REC-YYYY-NNNNN
    montant_total   DECIMAL(10,2) NOT NULL,
    montant_percu   DECIMAL(10,2) DEFAULT 0,
    devise          VARCHAR(3) DEFAULT 'EUR',
    mode_paiement   VARCHAR(30),    -- ESPECES | CARTE | VIREMENT | EXTERNE | EXONERE
    statut          VARCHAR(20) DEFAULT 'DU',
    -- DU | PARTIEL | PAYE | EXONERE | REMBOURSE
    date_paiement   TIMESTAMPTZ,
    caissier_id     UUID REFERENCES users(id),
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
```

### V8 — Audit

```sql
-- V8__audit.sql
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID REFERENCES organisations(id),
    user_id         UUID REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    entite_type     VARCHAR(100),
    entite_id       UUID,
    ancien_etat     JSONB,
    nouvel_etat     JSONB,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Index de performance
CREATE INDEX idx_dossiers_org_statut ON dossiers(organisation_id, statut);
CREATE INDEX idx_dossiers_usager     ON dossiers(usager_id);
CREATE INDEX idx_rdv_org_date        ON rendezvous(organisation_id, date_heure);
CREATE INDEX idx_audit_org_date      ON audit_logs(organisation_id, created_at DESC);
CREATE INDEX idx_usagers_org         ON usagers(organisation_id);
CREATE INDEX idx_usagers_fulltext    ON usagers
    USING GIN (to_tsvector('french', nom || ' ' || prenom || ' ' || coalesce(numero_document,'')));
```

---

## 5. Modules MVP — Détail Fonctionnel & API

### Vue d'ensemble des modules

| Bloc | Module | Endpoints principaux | Sprint | MoSCoW |
|---|---|---|---|---|
| M1 | Auth & Sécurité | POST /auth/login, /refresh, /logout | S1 | MUST |
| M2 | Référentiel Usagers | GET\|POST\|PUT /usagers, /usagers/{id} | S1 | MUST |
| M3 | Dossiers & Workflow | GET\|POST\|PUT /dossiers, /dossiers/{id}/transition | S2 | MUST |
| M4 | Rendez-vous | GET\|POST\|PUT /rendezvous, /slots, /disponibilites | S2 | MUST |
| M5 | Gestion Documentaire | POST /documents/upload, GET /documents/{id}/download | S3 | MUST |
| M6 | Encaissements & Reçus | GET\|POST /encaissements, /recu/{id}/pdf | S3 | MUST |
| M7 | Tableau de Bord | GET /dashboard/stats, /dashboard/alerts | S4 | MUST |
| M8 | Administration | CRUD /users, /roles, /organisation/params | S4 | MUST |
| M9 | Audit & Journalisation | GET /audit/logs, /audit/export | S4 | MUST |
| M10 | Notifications WebSocket | WS /ws, GET /notifications | S5 | SHOULD |
| M11 | Portail Usager Externe | Public API + espace usager | S6 | SHOULD |
| M12 | Exports & Reporting | GET /export/csv, /export/excel | S5 | SHOULD |

---

### 5.1 Auth & Sécurité (M1)

**Fonctionnalités**
- JWT RS256 avec access token (15 min) + refresh token rotation (7 jours).
- Rôles : `ROLE_ADMIN`, `ROLE_SUPERVISEUR`, `ROLE_AGENT`, `ROLE_CAISSIER`, `ROLE_ACCUEIL`.
- Réinitialisation de mot de passe par email (token signé, expiration 1h).
- Rate limiting sur `/auth/login` : 5 tentatives / 15 min par IP via Redis.

**Endpoints REST**
```
POST   /api/v1/auth/login           → { accessToken, refreshToken, user }
POST   /api/v1/auth/refresh          → { accessToken }
POST   /api/v1/auth/logout           → 204 No Content
POST   /api/v1/auth/forgot-password  → 200 (email envoyé)
POST   /api/v1/auth/reset-password   → 200
GET    /api/v1/auth/me               → UserProfileDTO
```

---

### 5.2 Référentiel Usagers (M2)

**Fonctionnalités**
- Fiche usager unique avec numéro auto-généré (`ORG-YYYYNNNNN`).
- Recherche multicritère full-text (nom, prénom, numéro document, email, téléphone).
- Détection des doublons potentiels (nom + date naissance + nationalité).
- Historique complet des demandes et passages.
- Export fiche usager en PDF (iText 7).

**Endpoints REST**
```
GET    /api/v1/usagers               → PageResponse<UsagerSummaryDTO>
POST   /api/v1/usagers               → UsagerDTO
GET    /api/v1/usagers/{id}          → UsagerDetailDTO (avec historique)
PUT    /api/v1/usagers/{id}          → UsagerDTO
DELETE /api/v1/usagers/{id}          → 204 (soft delete)
GET    /api/v1/usagers/search?q=     → List<UsagerSummaryDTO>
GET    /api/v1/usagers/doublons      → List<DoublonGroup>
GET    /api/v1/usagers/{id}/dossiers → List<DossierSummaryDTO>
```

---

### 5.3 Dossiers & Workflow (M3)

**Statuts et transitions autorisées**

| Statut | Transitions autorisées |
|---|---|
| `BROUILLON` | → SOUMIS (agent ou usager) |
| `SOUMIS` | → INCOMPLET (agent), → EN_REVUE (agent) |
| `INCOMPLET` | → SOUMIS (usager ou agent après correction) |
| `EN_REVUE` | → VALIDE (superviseur), → REJETE (superviseur), → INCOMPLET (agent) |
| `VALIDE` | → CLOTURE (agent ou auto après traitement) |
| `REJETE` | → CLOTURE (agent), → SOUMIS (réouverture avec motif) |
| `CLOTURE` | Terminal — consultation seule |

**Endpoints REST**
```
GET    /api/v1/dossiers                      → PageResponse<DossierSummaryDTO>
POST   /api/v1/dossiers                      → DossierDTO
GET    /api/v1/dossiers/{id}                 → DossierDetailDTO
PUT    /api/v1/dossiers/{id}                 → DossierDTO
POST   /api/v1/dossiers/{id}/transition      → { statut, motif } → DossierDTO
GET    /api/v1/dossiers/{id}/historique      → List<HistoriqueDTO>
POST   /api/v1/dossiers/{id}/commentaires    → CommentaireDTO
GET    /api/v1/dossiers/stats                → DossierStatsDTO
GET    /api/v1/types-service                 → List<TypeServiceDTO>
```

---

### 5.4 Rendez-vous (M4)

**Fonctionnalités**
- Paramétrage par service : créneaux, durées, quotas journaliers, jours de fermeture.
- Confirmation / report / annulation avec notification email/SMS (optionnel).
- Détection automatique des conflits de créneaux.
- Vue agenda agent (jour/semaine) et vue superviseur (tous agents).
- Liaison logique dossier ↔ rendez-vous.

**Endpoints REST**
```
GET    /api/v1/rendezvous                    → PageResponse<RdvDTO>
POST   /api/v1/rendezvous                    → RdvDTO
GET    /api/v1/rendezvous/{id}               → RdvDetailDTO
PUT    /api/v1/rendezvous/{id}               → RdvDTO
POST   /api/v1/rendezvous/{id}/statut        → { statut, motif }
GET    /api/v1/rendezvous/disponibilites     → List<CreneauDTO>
GET    /api/v1/rendezvous/agenda?date=&agent= → List<RdvAgendaDTO>
GET    /api/v1/parametres/creneaux           → List<ConfigCreneauDTO>
PUT    /api/v1/parametres/creneaux/{id}      → ConfigCreneauDTO
```

---

### 5.5 Gestion Documentaire (M5)

**Fonctionnalités**
- Téléversement sécurisé vers MinIO (S3) — validation MIME (PDF/JPEG/PNG/WEBP, max 10 Mo).
- Classement par type : `PIECE_IDENTITE`, `JUSTIFICATIF`, `PHOTO`, `FORMULAIRE`, `AUTRE`.
- Contrôle manuel : `RECU` → `LISIBLE` → `COMPLET` → `VALIDE` ou `A_CORRIGER`.
- Prévisualisation inline (react-pdf) + miniatures images.
- URLs de téléchargement pré-signées MinIO (expiration 15 min).
- Historique des versions et remplacement de document.

**Endpoints REST**
```
POST   /api/v1/documents/upload              → DocumentDTO (multipart/form-data)
GET    /api/v1/documents/{id}                → DocumentDetailDTO
GET    /api/v1/documents/{id}/download-url   → { url, expiresAt }
PUT    /api/v1/documents/{id}/statut-controle → { statut, notes }
DELETE /api/v1/documents/{id}                → 204
GET    /api/v1/dossiers/{dossierId}/documents → List<DocumentDTO>
```

---

### 5.6 Encaissements & Reçus (M6)

**Fonctionnalités**
- Référentiel de tarifs par service et par organisation.
- Paiement multi-mode : espèces, carte, virement, externe, exonéré.
- Numéro de reçu auto-généré (`REC-YYYY-NNNNN`) + PDF iText 7 (logo, QR code).
- Journal de caisse avec rapprochement de fin de journée.
- Exports Excel/CSV de la période sélectionnée.

**Endpoints REST**
```
GET    /api/v1/encaissements                 → PageResponse<EncaissementDTO>
POST   /api/v1/encaissements                 → EncaissementDTO
GET    /api/v1/encaissements/{id}            → EncaissementDetailDTO
GET    /api/v1/encaissements/{id}/recu/pdf   → PDF (application/pdf)
GET    /api/v1/encaissements/journal-caisse  → JournalCaisseDTO
GET    /api/v1/tarifs                        → List<TarifDTO>
POST   /api/v1/tarifs                        → TarifDTO
PUT    /api/v1/tarifs/{id}                   → TarifDTO
```

---

### 5.7 Tableau de Bord (M7)

**KPIs & Widgets**
- Volumes : dossiers créés / traités / en attente par type de service (jour / semaine / mois).
- Délais : temps moyen de traitement par service, dossiers dépassant l'échéance.
- Rendez-vous du jour : taux de présence, absences, retards.
- Caisse : recettes de la journée et de la période.
- Alertes : goulets d'étranglement, agents surchargés, dossiers bloqués > 7 jours.

**Endpoints REST**
```
GET    /api/v1/dashboard/stats          → DashboardStatsDTO
GET    /api/v1/dashboard/dossiers-chart → List<ChartPointDTO> (?periode=)
GET    /api/v1/dashboard/recettes-chart → List<ChartPointDTO>
GET    /api/v1/dashboard/alerts         → List<AlerteDTO>
GET    /api/v1/dashboard/agents-charge  → List<AgentChargeDTO>
```

---

## 6. Sécurité & Conformité

### 6.1 Authentification & Autorisation

- JWT RS256 : clé privée RSA-2048 générée au démarrage, jamais exposée.
- Access token : 15 minutes. Refresh token : 7 jours, stocké `httpOnly` cookie.
- Révocation des refresh tokens via Redis (blacklist sur logout).
- Isolation multi-tenant stricte : filtre Spring Security vérifiant `organisation_id` sur chaque requête.
- `@PreAuthorize` sur tous les endpoints — aucun endpoint non protégé hors `/auth/**` et `/public/**`.

### 6.2 Protection des données

- Chiffrement en transit : TLS 1.3 obligatoire (Nginx terminaison SSL).
- Chiffrement au repos : PostgreSQL tablespace chiffré ou disque chiffré par le fournisseur.
- Fichiers : MinIO bucket avec chiffrement SSE-S3, accès uniquement via URLs pré-signées (15 min).
- Mots de passe : BCrypt avec cost factor 12.
- Données sensibles masquées dans les logs (numéros de document, emails).

### 6.3 Audit Trail

- AOP Spring (`@Around`) sur toutes les méthodes annotées `@Auditable`.
- Champs capturés : `userId`, `organisationId`, `action`, `entiteType`, `entiteId`, `ancienEtat` (JSON), `nouvelEtat` (JSON), IP, userAgent, timestamp.
- Table `audit_logs` en append-only — aucune mise à jour ni suppression autorisée.
- Rétention configurable par l'organisation (défaut : 5 ans).

### 6.4 Conformité RGPD

- Hébergement en Europe (OVH, Hetzner ou équivalent).
- Droit à l'oubli : anonymisation des données usager sur demande (pseudonymisation, pas suppression physique).
- Export des données personnelles : `GET /api/v1/usagers/{id}/export-rgpd`.
- Consentement traçable, politique de conservation documentaire.

---

## 7. Prompts Cursor AI — Blocs d'Implémentation

> Copier-coller dans Cursor AI en mode Agent (`Ctrl+I` / `Cmd+I`).  
> Chaque bloc suit la structure canonique **A/B/C/D/E**.

---

### BLOC 0 — Initialisation du Projet

```
# BLOC 0 — INITIALISATION CONSULOS (Cursor AI Agent)

## A — Contexte & Contraintes
Tu initialises le projet ConsulOS from scratch.
- Backend : Spring Boot 3.3, Java 21, Maven, PostgreSQL 16, JWT RS256
- Frontend : React 18 + Vite 5, TypeScript strict, Tailwind CSS 3, shadcn/ui
- Règle absolue : tout nouveau code va dans de nouveaux fichiers. Ne jamais modifier
  les fichiers existants générés par Spring Initializr ou Vite.

## B — Backend à créer
1. Génère pom.xml avec dépendances :
   spring-boot-starter-web, spring-boot-starter-security, spring-boot-starter-data-jpa,
   spring-boot-starter-validation, spring-boot-starter-mail, spring-boot-starter-data-redis,
   spring-boot-starter-actuator, spring-boot-starter-websocket,
   jjwt-api 0.12.x, jjwt-impl, jjwt-jackson, flyway-core, postgresql, lombok, mapstruct,
   springdoc-openapi-starter-webmvc-ui 2.x, itext7-core 8.x, poi-ooxml 5.x,
   testcontainers, jacoco-maven-plugin.

2. Crée src/main/java/com/consulos/common/ avec :
   - BaseEntity.java (UUID id, createdAt, updatedAt, createdBy, updatedBy, organisationId)
   - ApiResponse.java (boolean success, T data, String message, String errorCode)
   - PageResponse.java (List<T> content, long totalElements, int totalPages, int page, int size)
   - BusinessException.java (String errorCode, String message, HttpStatus status)
   - GlobalExceptionHandler.java (@RestControllerAdvice)

3. Crée application.yml avec profils dev / prod / test.
4. Crée docker-compose.yml : postgres:16, redis:7-alpine, minio:latest, mailhog:latest.
5. Crée V1__init_organisations.sql, V2__users_roles.sql avec le DDL du PRD §4.

## C — Frontend à créer
1. Crée vite.config.ts avec alias @/ → src/, proxy /api → http://localhost:8080.
2. Crée tailwind.config.ts avec palette ConsulOS (primary #1B4F72, accent #2E86C1).
3. Installe et configure shadcn/ui (npx shadcn-ui@latest init).
4. Crée src/lib/queryClient.ts (TanStack Query v5, staleTime 5 min).
5. Crée src/stores/authStore.ts (Zustand : user, accessToken, isAuthenticated, login, logout).
6. Crée src/lib/apiClient.ts (Axios instance, intercepteur Bearer token, refresh automatique).
7. Crée src/router/AppRouter.tsx avec routes protégées par rôle.
8. Crée src/lib/i18n.ts avec locales fr/en/pt.

## D — Tests à créer
- ApplicationContextTest.java (smoke test Spring context)
- Vitest config + un test dummy App.test.tsx

## E — Validation
- mvn clean test -Dspring.profiles.active=test → BUILD SUCCESS
- npm run test → passed
- docker-compose up -d → tous les services healthy
```

---

### BLOC 1 — Auth & Sécurité JWT RS256

```
# BLOC 1 — AUTH & SÉCURITÉ (Cursor AI Agent)

## A — Contexte & Contraintes
Implémente l'authentification JWT RS256 complète pour ConsulOS.
Règle : créer uniquement de nouveaux fichiers dans src/main/java/com/consulos/auth/.
Ne jamais modifier SecurityConfig ni les entités existantes.

## B — Migration SQL
Crée V2__users_roles.sql avec :
- Table roles : id, code (ROLE_ADMIN|ROLE_SUPERVISEUR|ROLE_AGENT|ROLE_CAISSIER|ROLE_ACCUEIL),
  libelle, permissions JSONB
- Table users : id, organisation_id, email, password_hash, nom, prenom, role_id, actif,
  derniere_connexion, created_at, updated_at, created_by
- Données initiales : 5 rôles + 1 user admin de démo

## C — Backend à créer (auth/ package)
1. JwtKeyProvider.java — génère/charge la paire RSA-2048 depuis ${JWT_PRIVATE_KEY_PATH}.
2. JwtService.java — generateAccessToken(UserDetails, Duration 15min),
   generateRefreshToken(), validateToken(), extractClaims().
3. RefreshTokenService.java — stocke dans Redis (key: rt:{userId}, TTL 7j),
   rotate() renvoie nouveau pair, revoke() supprime de Redis.
4. SecurityConfig.java — JWT filter chain, CORS, CSRF désactivé, rate limiting sur
   /auth/login via Redis (5 req/15min par IP), BCryptPasswordEncoder cost=12.
5. AuthController.java — POST /login, /refresh, /logout, /forgot-password, /reset-password, GET /me.
6. AuthService.java — logique métier (authentification, vérification actif, mise à jour
   derniere_connexion).
7. UserDetailsServiceImpl.java — charge depuis UserRepository.
8. DTOs : LoginRequestDTO, LoginResponseDTO, RefreshRequestDTO, ForgotPasswordDTO,
   ResetPasswordDTO, UserProfileDTO.

## D — Frontend à créer
1. src/pages/auth/LoginPage.tsx — formulaire email/mdp, React Hook Form + Zod,
   gestion erreurs, redirect post-login selon rôle.
2. src/services/authService.ts — appels API login/refresh/logout.
3. Mise à jour authStore.ts — persistance en mémoire uniquement, refresh auto
   via intercepteur Axios.
4. src/router/ProtectedRoute.tsx — vérifie isAuthenticated + rôle autorisé, redirect /login.
5. LoginPage.test.tsx (Vitest + React Testing Library).

## E — Tests Backend
- AuthServiceTest.java (Mockito : login OK, login user inactif, mauvais mdp)
- JwtServiceTest.java (génération, validation, expiration)
- AuthControllerIT.java (Testcontainers : POST /auth/login → 200 avec token valide)
- Critère : mvn test → 100% pass, pas de régression sur ApplicationContextTest
```

---

### BLOC 2 — Référentiel Usagers

```
# BLOC 2 — RÉFÉRENTIEL USAGERS (Cursor AI Agent)

## A — Contexte & Contraintes
Implémente le module Usagers complet. Nouveaux fichiers uniquement dans usager/ package.
Tous les endpoints nécessitent @PreAuthorize. Isolation multi-tenant via organisation_id.

## B — Migration SQL
V3__usagers.sql : table usagers complète (voir PRD §4), index sur organisation_id,
index full-text GIN sur (nom || ' ' || prenom || ' ' || coalesce(numero_document,'')).

## C — Backend (usager/ package)
1. UsagerEntity.java étend BaseEntity.
2. UsagerRepository.java : findByOrganisationId (Page), searchFullText (Page),
   findPotentialDoublons (nom, dateNaissance, nationalite, organisationId).
3. UsagerMapper.java (MapStruct) : UsagerEntity ↔ UsagerDTO / UsagerDetailDTO / UsagerSummaryDTO.
4. UsagerService.java : create, update, softDelete, search, findById, findDoublons, getHistorique.
   Génération numéro : SELECT nextval('usager_seq') → ORG-YYYY-NNNNN.
5. UsagerController.java : tous les endpoints de la spec PRD §5.2.
   @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')") sur GET.
   @PreAuthorize("hasAnyRole('AGENT','ADMIN')") sur POST/PUT.
6. UsagerExportService.java : générer PDF fiche usager avec iText 7.

## D — Frontend (pages/usagers/)
1. UsagersPage.tsx : tableau paginé, barre de recherche globale, filtre type_usager.
2. UsagerDetailPage.tsx : onglets Informations / Dossiers / Documents / Historique.
3. UsagerCreatePage.tsx / UsagerEditPage.tsx : React Hook Form + Zod,
   alerte doublon potentiel à la saisie (debounce 500ms).
4. hooks/useUsagers.ts : useQuery list, useQuery detail, useMutation create/update/delete.
5. services/usagerService.ts : tous les appels API.
6. components/usagers/DoublonAlert.tsx : affiche les usagers similaires trouvés.

## E — Tests
- UsagerServiceTest.java : create (OK, doublon détecté), search, softDelete
- UsagerControllerIT.java : Testcontainers POST/GET/PUT /usagers
- UsagersPage.test.tsx : render, recherche, navigation vers détail
```

---

### BLOC 3 — Dossiers & Workflow

```
# BLOC 3 — DOSSIERS & WORKFLOW (Cursor AI Agent)

## A — Contexte & Contraintes
Implémente le moteur de dossiers avec workflow configurable par type de service.
Nouveaux fichiers uniquement dans dossier/ package.
La machine d'état est implémentée dans WorkflowEngine injectable, pas dans DossierService.

## B — Migration SQL
V4__dossiers.sql : tables types_service, dossiers, dossier_historique (voir PRD §4).
V4b : insérer 5 types de service de démo (PASSEPORT, ETAT_CIVIL, CARTE_CONSULAIRE,
LEGALISATION, PROCURATION) avec checklists JSON et tarifs.

## C — Backend (dossier/ package)
1. DossierEntity.java, TypeServiceEntity.java, DossierHistoriqueEntity.java.
2. WorkflowEngine.java — valide une transition (statutDe, statutVers, role) selon la
   config JSON de TypeServiceEntity.workflow_config. Lance BusinessException si invalide.
3. DossierService.java :
   - create() : génère référence DOS-YYYY-NNNNN, statut BROUILLON.
   - transition(dossierId, newStatut, motif) : appelle WorkflowEngine, enregistre historique.
   - assignerAgent(), ajouterCommentaire(), getHistorique(), getStats().
4. DossierController.java : tous les endpoints PRD §5.3.
   Droits : transition VALIDE/REJETE → ROLE_SUPERVISEUR seulement.
5. DossierMapper.java (MapStruct) : entités → DTOs.
6. Audit : @Auditable sur transition() et create().

## D — Frontend (pages/dossiers/)
1. DossiersPage.tsx : tableau filtrable par statut/service/agent/période, badges colorés.
2. DossierCreatePage.tsx : stepper 3 étapes (Usager → Type → Pièces jointes).
3. DossierDetailPage.tsx :
   - Header : référence, statut badge, usager, agent assigné.
   - Onglets : Informations / Documents / Historique / Commentaires.
   - Actions contextuelles selon rôle (boutons de transition).
4. components/dossiers/WorkflowTimeline.tsx : visualisation des étapes avec dates.
5. components/dossiers/ChecklistViewer.tsx : pièces requises avec statut coché.
6. hooks/useDossiers.ts, services/dossierService.ts.

## E — Tests
- WorkflowEngineTest.java : toutes les transitions valides et invalides
- DossierServiceTest.java : create, transition OK, transition non autorisée
- DossierControllerIT.java : Testcontainers flux complet BROUILLON → VALIDE
- DossierDetailPage.test.tsx : affichage onglets, actions selon rôle
```

---

### BLOC 4 — Rendez-vous

```
# BLOC 4 — RENDEZ-VOUS (Cursor AI Agent)

## A — Contexte & Contraintes
Implémente la gestion des rendez-vous avec calendrier et détection de conflits.
Nouveaux fichiers uniquement dans rendezvous/ package.

## B — Migration SQL
V5__rendezvous.sql : tables rendezvous, config_creneaux (service, jour_semaine,
heure_debut, heure_fin, duree_minutes, quota_max).

## C — Backend (rendezvous/ package)
1. RdvEntity.java, ConfigCreneauEntity.java.
2. DisponibiliteService.java : calcule les créneaux libres pour une date + service donnés,
   en soustrayant les RDV existants des slots configurés.
3. ConflitRdvException.java (extends BusinessException).
4. RdvService.java : create() vérifie dispo via DisponibiliteService, updateStatut(),
   getAgenda(agentId, dateDebut, dateFin).
5. RdvController.java : tous les endpoints PRD §5.4.
6. RdvNotificationService.java : envoie email confirmation/annulation/rappel via Spring Mail.

## D — Frontend (pages/rendezvous/)
1. AgendaPage.tsx : vue calendrier hebdomadaire, filtres par agent / service.
2. RdvCreateModal.tsx : sélection date → créneaux disponibles → usager → dossier → confirm.
3. RdvDetailPage.tsx : infos RDV, liaison dossier, boutons statut.
4. hooks/useRdv.ts, hooks/useDisponibilites.ts.
5. components/rendezvous/CreneauPicker.tsx : créneaux disponibles par demi-heure.

## E — Tests
- DisponibiliteServiceTest.java : créneaux libres, conflit, quota atteint
- RdvServiceTest.java : création OK, création en conflit, annulation
- AgendaPage.test.tsx : render agenda, sélection créneau
```

---

### BLOC 5 — Documents (MinIO)

```
# BLOC 5 — GESTION DOCUMENTAIRE (Cursor AI Agent)

## A — Contexte & Contraintes
Implémente le stockage et la gestion des documents via MinIO.
Nouveaux fichiers uniquement dans document/ package.
Les URLs de téléchargement sont TOUJOURS pré-signées (jamais d'URL directe MinIO exposée).

## B — Migration SQL
V6__documents.sql : table documents (voir PRD §4).

## C — Backend (document/ package)
1. MinioConfig.java : @Configuration, bean MinioClient depuis ${MINIO_ENDPOINT},
   ${MINIO_ACCESS_KEY}, ${MINIO_SECRET_KEY}, création du bucket consulos-docs si absent.
2. MinioStorageService.java : uploadFile(inputStream, fileName, contentType) → objectKey,
   generatePresignedDownloadUrl(objectKey, 15min), deleteFile(objectKey).
   Valide MIME (PDF/JPEG/PNG/WEBP uniquement) et taille (max 10 Mo) avant upload.
3. DocumentEntity.java, DocumentRepository.java, DocumentMapper.java.
4. DocumentService.java : upload(), updateStatutControle(), getDownloadUrl(), delete().
5. DocumentController.java : endpoints PRD §5.5. @Auditable sur upload et delete.

## D — Frontend
1. DocumentUploadZone.tsx : drag & drop (react-dropzone), prévisualisation, progression.
2. DocumentViewer.tsx : iframe react-pdf pour PDF, <img> pour images. Bouton télécharger.
3. DocumentList.tsx : tableau nom/type/statut/date/actions.
4. StatutControleBadge.tsx : badge coloré par statut de contrôle.
5. hooks/useDocuments.ts, services/documentService.ts.

## E — Tests
- MinioStorageServiceTest.java (Testcontainers MinIO) : upload, presignedUrl, delete
- DocumentServiceTest.java : upload OK, MIME invalide rejeté, size dépassée rejetée
- DocumentUploadZone.test.tsx : drag & drop, erreur taille
```

---

### BLOC 6 — Encaissements & Reçus PDF

```
# BLOC 6 — ENCAISSEMENTS & REÇUS (Cursor AI Agent)

## A — Contexte & Contraintes
Implémente la caisse consulaire avec génération de reçus PDF via iText 7.
Nouveaux fichiers uniquement dans encaissement/ package.

## B — Migration SQL
V7__encaissements.sql : tables tarifs, encaissements (voir PRD §4).
Séquence encaissement_seq pour numérotation REC-YYYY-NNNNN.

## C — Backend (encaissement/ package)
1. TarifEntity.java, EncaissementEntity.java, mappers.
2. TarifService.java : CRUD tarifs par organisation.
3. EncaissementService.java :
   - create() : calcule montant depuis tarifs, génère REC-YYYY-NNNNN, statut DU.
   - enregistrerPaiement(id, montantPercu, modePaiement) : PAYE si soldé, sinon PARTIEL.
   - getJournalCaisse(date, organisationId) → totaux par mode de paiement.
4. RecuPdfService.java : génère PDF avec iText 7 (logo organisation, numéro, usager,
   service, montant, mode paiement, date, signature caissier, QR code vérification).
5. EncaissementController.java : endpoints PRD §5.6.
   GET /{id}/recu/pdf → byte[] avec Content-Type: application/pdf.
6. ExportService.java : export Excel journal de caisse via Apache POI.

## D — Frontend (pages/encaissement/)
1. CaissePage.tsx : liste encaissements du jour + formulaire création rapide.
2. RecuPage.tsx : aperçu PDF (react-pdf) + bouton impression / téléchargement.
3. TarifsPage.tsx : tableau CRUD tarifs (admin seulement).
4. hooks/useEncaissements.ts, services/encaissementService.ts.

## E — Tests
- RecuPdfServiceTest.java : génère PDF valide (assert bytes non vides)
- EncaissementServiceTest.java : create, paiement partiel, paiement complet, exonération
- CaissePage.test.tsx : création encaissement, affichage journal
```

---

### BLOC 7 — Tableau de Bord & Audit

```
# BLOC 7 — TABLEAU DE BORD & AUDIT (Cursor AI Agent)

## A — Contexte & Contraintes
Implémente le tableau de bord opérationnel et le système d'audit transversal.
Audit : aspect AOP transversal dans audit/AuditAspect.java — nouveau fichier uniquement.
Dashboard : nouveaux fichiers dans dashboard/ package.

## B — Migration SQL
V8__audit.sql : table audit_logs (voir PRD §4). Contrainte : INSERT ONLY (no UPDATE/DELETE).
Vue matérialisée dashboard_stats_daily (rafraîchie toutes les heures via Spring Scheduler).

## C — Backend

### Audit (audit/ package)
1. @Auditable annotation (@Retention RUNTIME, @Target METHOD).
2. AuditAspect.java (@Aspect @Component) : @Around sur @Auditable, capture userId depuis
   SecurityContext, ancienEtat/nouvelEtat en JSON, IP depuis HttpServletRequest MDC.
3. AuditLogEntity.java, AuditRepository.java (append-only : pas de save(update) autorisé).
4. AuditController.java : GET /audit/logs (paginé, filtres), GET /audit/export (CSV).
   @PreAuthorize("hasRole('ADMIN')") sur tous.

### Dashboard (dashboard/ package)
1. DashboardQueryService.java : requêtes natives PostgreSQL (pas JPA) pour :
   - dossierStatsByPeriode(organisationId, debut, fin)
   - recettesParService(organisationId, debut, fin)
   - rdvTauxPresence(organisationId, date)
   - alertesActives(organisationId) → dossiers bloqués, agents surchargés
2. DashboardController.java : endpoints PRD §5.7.
3. DashboardScheduler.java : rafraîchit la vue matérialisée toutes les heures.

## D — Frontend

### Dashboard (pages/dashboard/)
1. DashboardPage.tsx : grille responsive (4 KPI cards + 2 charts Recharts + alertes).
2. KpiCard.tsx : icône + valeur + tendance (flèche + %).
3. DossiersBarChart.tsx, RecettesLineChart.tsx (Recharts).
4. AlertesPanel.tsx : liste alertes avec badge priorité + lien vers dossier.

### Administration (pages/administration/)
1. UsersPage.tsx : CRUD agents (table + modal création/édition).
2. RolesPage.tsx : visualisation permissions par rôle.
3. ParametresPage.tsx : langues, devises, services actifs, horaires, jours fériés.
4. AuditLogsPage.tsx : journal filtrable avec export CSV.

## E — Tests
- AuditAspectTest.java : vérifie enregistrement audit sur transition dossier
- DashboardQueryServiceTest.java (Testcontainers) : stats calculées correctement
- DashboardPage.test.tsx : render KPI, charts, alertes
```

---

## 8. Infrastructure & Déploiement

### 8.1 Docker Compose (développement)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: consulos
      POSTGRES_USER: consulos
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports: ["5432:5432"]

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    ports: ["6379:6379"]

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY}
    volumes:
      - minio_data:/data
    ports: ["9000:9000", "9001:9001"]

  mailhog:
    image: mailhog/mailhog
    ports: ["1025:1025", "8025:8025"]

  backend:
    build: ./consulos-backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/consulos
      SPRING_DATA_REDIS_HOST: redis
      MINIO_ENDPOINT: http://minio:9000
    depends_on: [postgres, redis, minio]
    ports: ["8080:8080"]

  frontend:
    build: ./consulos-frontend
    ports: ["3000:3000"]

  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    ports: ["80:80", "443:443"]
    depends_on: [backend, frontend]

volumes:
  postgres_data:
  minio_data:
```

### 8.2 Variables d'environnement obligatoires

| Variable | Exemple | Description |
|---|---|---|
| `JWT_PRIVATE_KEY_PATH` | `/secrets/rsa-private.pem` | Clé RSA-2048 pour JWT |
| `JWT_PUBLIC_KEY_PATH` | `/secrets/rsa-public.pem` | Clé publique correspondante |
| `POSTGRES_PASSWORD` | `changeme-prod!` | Mot de passe PostgreSQL |
| `REDIS_PASSWORD` | `changeme-redis!` | Mot de passe Redis |
| `MINIO_ACCESS_KEY` | `consulos-access` | Identifiant MinIO |
| `MINIO_SECRET_KEY` | `changeme-minio!` | Secret MinIO |
| `MAIL_HOST` | `smtp.brevo.com` | Serveur SMTP |
| `MAIL_USERNAME` | `api` | Login SMTP |
| `MAIL_PASSWORD` | `xxx` | Mot de passe SMTP |
| `APP_BASE_URL` | `https://consulos.exemple.com` | URL publique application |

### 8.3 Exigences non fonctionnelles

- Application web responsive, utilisable sur poste fixe et tablette.
- Temps de chargement cible < 2 secondes sur les vues courantes.
- TLS 1.3 obligatoire en production.
- Sauvegardes PostgreSQL : daily dump + WAL archiving.
- Monitoring : Spring Actuator `/health` + Prometheus scrape + alertes Grafana.
- Journalisation des erreurs centralisée (Logback → ELK ou fichiers rotatifs).

---

## 9. Roadmap & Priorités

### 9.1 Sprints MVP (8 sprints de 2 semaines)

| Sprint | Bloc | Livrables clés |
|---|---|---|
| S1 | Bloc 0 + 1 | Initialisation projet, Auth JWT RS256, Docker Compose, CI/CD basique |
| S2 | Bloc 2 | Référentiel Usagers complet, recherche full-text, détection doublons |
| S3 | Bloc 3 | Dossiers + WorkflowEngine, historique, commentaires, types de service |
| S4 | Bloc 4 | Rendez-vous, agenda, créneaux, notifications email/SMS |
| S5 | Bloc 5 | Documents MinIO, upload sécurisé, prévisualisation, contrôle qualité |
| S6 | Bloc 6 | Encaissements, tarifs, reçus PDF iText 7, journal de caisse |
| S7 | Bloc 7 | Tableau de bord, KPIs, alertes, Audit AOP, administration |
| S8 | Finalisation | Tests E2E Playwright, performance, sécurité, docs OpenAPI, démo client |

### 9.2 MoSCoW

**Must Have (MVP)**
- Fiche usager, création et suivi de dossier, rendez-vous liés aux dossiers, documents joints, encaissement simple, tableau de bord basique, rôles et audit.

**Should Have (Phase 2)**
- Notifications temps réel (WebSocket + STOMP), portail usager externe, exports Excel/CSV enrichis, détection de doublons avancée, modèles de documents et checklists par service.

**Could Have (Phase 3)**
- Paiement en ligne (Stripe / PayDunya), OCR assisté (Tesseract), multi-postes mutualisés, intégrations API partenaires, IA assistive.

**Won't Have Now**
- Biométrie, signature électronique qualifiée eIDAS, décision automatisée de visa, application mobile native, data warehouse ministériel complet.

### 9.3 Phases post-MVP

**Phase 2 — Operations**
- Notifications temps réel et file d'attente électronique.
- Portail usager limité (suivi dossier, dépôt documents, prise RDV).
- Exports standards et reporting mensuel.
- Application mobile PWA.

**Phase 3 — Expansion**
- Paiement en ligne intégré.
- OCR assisté — pré-remplissage formulaires depuis documents scannés.
- Multi-postes mutualisés — tableau de bord régional / central.
- Intégrations API partenaires (OFPRA, DGFiP, systèmes nationaux).

---

## 10. Questions Ouvertes & Hypothèses

### 10.1 Questions à trancher avant le Sprint 1

1. Services prioritaires au lancement : passeports, état civil, cartes consulaires, légalisations ou assistance ?
2. Instance par poste (on-premise) ou plateforme SaaS mutualisée multi-consulats ?
3. Profondeur documentaire MVP : combien de types de pièces, règles de rétention ?
4. Le module caisse doit-il produire un export comptable normalisé (CODA, FEC) ?
5. Quels contrôles légaux s'appliquent au stockage des pièces et à la durée de rétention dans le pays hôte ?
6. Langues actives dès le MVP : français uniquement ou FR + EN + PT simultanément ?

### 10.2 Hypothèses posées

- Les postes harmonisent a minima leurs catégories de services et leurs statuts de dossier.
- Les agents accepteront une solution légère si elle réduit réellement la charge manuelle.
- La grande majorité de la valeur peut être livrée sans intégration profonde aux systèmes ministériels.
- Les pièces justificatives sont majoritairement PDF ou image dans le MVP.
- L'hébergement OVH (RGPD, France) est acceptable pour les postes cibles.

### 10.3 Risques à surveiller

| Risque | Mitigation |
|---|---|
| Variabilité forte des règles entre consulats | `workflow_config` JSON paramétrable par poste et par type de service |
| Sensibilité juridique des données personnelles | Audit trail complet, conformité RGPD, chiffrement en transit et au repos |
| Frictions de conduite du changement | Formation, documentation utilisateur, support dédié à l'onboarding |
| Hétérogénéité de l'équipement informatique | Application web responsive, compatible tablettes, PWA en phase 2 |
| Référentiel tarifaire et procédural instable | Référentiel paramétrable par l'administrateur du poste, sans redéploiement |

---

*— Fin du PRD ConsulOS v3.0 — Cursor AI Ready —*
