# ConsulOS — Guide démo locale

## Démarrage rapide

```powershell
cd c:\Users\epifa\cursor-workspace\eConsulOS
.\scripts\start-demo.ps1
```

Ou manuellement :

```powershell
.\scripts\start-dev-infra.ps1
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
cd consulos\consulos-backend
mvn spring-boot:run -DskipTests
# Nouveau terminal :
cd consulos\consulos-frontend
npm run dev
```

## URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 (ou 3002 si 3000 occupé) |
| API | http://localhost:8082/api/v1 |
| Health | http://localhost:8082/api/v1/actuator/health |
| Swagger | http://localhost:8082/api/v1/swagger-ui/index.html |
| MinIO | http://localhost:9001 (minioadmin / minioadmin123) |
| MailHog | http://localhost:8025 |

## Comptes de démonstration

Mot de passe pour tous : **`Admin1234!`**

| Rôle | E-mail | Après login |
|------|--------|-------------|
| ADMIN | admin@consulos.local | `/administration` |
| SUPERVISEUR | superviseur@consulos.local | Dashboard KPIs |
| AGENT | agent@consulos.local | Dashboard |
| CAISSIER | caissier@consulos.local | Caisse `/caisse` |

## Données de démo (migration V11)

| Usager | Numéro | Dossier |
|--------|--------|---------|
| Mamadou Camara | GNB-2024-00001 | DOS-2026-00001 (BROUILLON) |
| Aminata Camara | GNB-2024-00002 | DOS-2026-00002 (SOUMIS) |

Un rendez-vous est planifié pour Mamadou Camara (lendemain 10h00).

## Scénario démo (15 min)

1. **SUPERVISEUR** — Dashboard : KPIs, graphiques, alertes
2. **AGENT** — Usagers → créer usager → Dossier → transition SOUMIS
3. **AGENT** — Documents : upload PDF sur un dossier
4. **AGENT** — Rendez-vous : prendre un créneau
5. **CAISSIER** — Caisse → encaissement → télécharger reçu PDF
6. **ADMIN** — Administration → Journal d'audit

## Tests E2E

```powershell
cd consulos\consulos-frontend
npm run test:e2e
```

## Ports spécifiques Windows

- PostgreSQL Docker : **5433** (évite conflit avec PostgreSQL Windows sur 5432)
- Backend dev : **8082** (évite conflit avec autres apps sur 8080)
