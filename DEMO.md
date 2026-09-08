# ConsulOS — Guide démo client (MVP v0.1)



Durée cible : **15–20 minutes** | Public : décideurs + équipe consulaire



---



## Prérequis (5 min avant la démo)



```powershell

cd c:\Users\epifa\cursor-workspace\eConsulOS



# 1. Variables d'environnement (une fois)

copy .env.example .env



# 2. Clés JWT RS256 (dossier secrets/, non versionné)

.\scripts\generate-jwt-keys.ps1



# 3. Démarrage complet (infra Docker + backend + frontend)

.\scripts\start-demo.ps1

```



Vérifications :



| Check | URL / commande | Attendu |

|-------|----------------|---------|

| API | http://localhost:8082/api/v1/actuator/health | `UP` (ou 503 si Redis down — API utilisable) |

| Frontend | http://localhost:3000 | Page login ConsulOS |

| Swagger | http://localhost:8082/swagger-ui/index.html | Groupes 01-auth … 11-reporting |

| E2E (optionnel) | `cd consulos\consulos-frontend; npm run test:e2e` | 19/19 |



---



## URLs



| Service | URL |

|---------|-----|

| Frontend | http://localhost:3000 (ou port Vite affiché) |

| API | http://localhost:8082/api/v1 |

| Swagger | http://localhost:8082/swagger-ui/index.html |

| MinIO console | http://localhost:9001 (`minioadmin` / `minioadmin123`) |

| MailHog (ConsulOS) | http://localhost:8026 (SMTP `localhost:1026`) |



---



## Comptes de démonstration



Mot de passe pour tous : **`Admin1234!`**



| Rôle | E-mail | Redirection | Menu visible (extrait) | Droits clés démo |

|------|--------|-------------|------------------------|------------------|

| ADMIN | admin@consulos.local | Administration | Tout + Administration | Utilisateurs, audit, tarifs |

| SUPERVISEUR | superviseur@consulos.local | Dashboard KPIs | Dashboard, Usagers, Dossiers, Docs, RDV, Caisse | Lecture + validation, export RGPD |

| AGENT | agent@consulos.local | Dashboard (accueil) | Pas Caisse / Admin | Usagers, dossiers, documents, RDV |

| CAISSIER | caissier@consulos.local | **Caisse** | Pas RDV / Admin | Encaissements, reçus PDF, export Excel |

| ADMIN_RESEAU | reseau@consulos.local | **Réseau** (`/regional`) | Accueil + Réseau | TDB régional multi-postes (GNB-FR + SEN-FR) |



> **Notes rôles** :

> - Seuls **AGENT** et **ADMIN** peuvent créer/modifier un usager. Le superviseur voit les fiches mais pas le bouton « Modifier ».

> - Le menu latéral est **filtré par rôle** (plus d’entrée Administration pour non-admin).

> - L’agent n’a pas les KPIs superviseur : message d’accueil + accès via le menu.



---



## Données seed (migration V11)



| Usager | N° | Dossier | Statut |

|--------|-----|---------|--------|

| Mamadou Camara | GNB-2024-00001 | DOS-2026-00001 | BROUILLON |

| Aminata Camara | GNB-2024-00002 | DOS-2026-00002 | SOUMIS |



RDV planifié pour Mamadou Camara (lendemain 10h00).

> **Portail usager** : http://localhost:3010/portal — ex. `DOS-2026-00001` + email usager de la fiche.  
> Les mails **ne partent pas** vers Gmail/Yahoo en local : ouvrir **MailHog ConsulOS** → http://localhost:8026 (SMTP `MAIL_PORT=1026`, pas le Mailpit eConsulat sur 8025).



---



## Script de démo pas à pas



### Étape 1 — Vision superviseur (3 min)



**Compte** : `superviseur@consulos.local`



1. Se connecter → **Tableau de bord**

2. Montrer les **KPIs** : dossiers ouverts, RDV du jour, recettes

3. Ouvrir les **graphiques** dossiers / recettes

4. Consulter les **alertes actives** et la **charge agents**

5. Menu **Usagers** → ouvrir **Aminata Camara**

6. Onglets **Dossiers** / **Documents** / **Historique** (aperçu rapide)

7. Cliquer **Exporter RGPD** → téléchargement JSON (portabilité)

8. Cliquer **Fiche PDF** → téléchargement PDF (identité / contact)

9. *Montrer l'absence du bouton Modifier* (contrôle d'accès)

10. *Montrer l'absence du menu Administration* (RBAC UI)



**Message clé** : pilotage temps réel + conformité RGPD + menus adaptés au rôle.



---



### Étape 2 — Parcours agent : usager → dossier (4 min)



**Déconnexion** → **Compte** : `agent@consulos.local`



1. À l’arrivée : **Tableau de bord** avec message d’accueil (pas de KPIs superviseur)

2. **Usagers** → **Nouvel usager**

   - Nom : `Diallo`, Prénom : `Fatou`, Nationalité : `GIN`, Email + téléphone

   - Enregistrer (ignorer doublons si alerte)

3. Ouvrir la fiche créée → onglets **Dossiers**, **Documents**, **Historique**

4. **Dossiers** → **Nouveau dossier** pour cet usager (type passeport ou état civil)

5. Ouvrir le dossier **DOS-2026-00001** (seed Mamadou) ou le dossier créé

6. Onglet **Informations** → **Assigner** un agent

7. Cliquer **Soumettre** → transition `BROUILLON` → `SOUMIS`

8. Onglet **Historique** du dossier → timeline des transitions



**Message clé** : workflow métier traçable, multi-tenant, UI sans entrées Caisse/Admin.



---



### Étape 3 — Documents & rendez-vous (3 min)



Toujours en **agent** :



1. Sur un dossier → onglet **Documents** → **Uploader** un PDF de test

2. Revenir à la fiche **usager** → onglet **Documents** → le fichier apparaît (lié via dossier)

3. Menu **Documents** (page globale) → filtres type / statut / référence → retrouver le fichier

4. Menu **Rendez-vous** → **Agenda**

5. Créer un RDV pour un usager seed (créneau disponible)

6. Confirmer le RDV dans la liste



**Message clé** : pièces jointes MinIO + vue usager + liste globale + planification.



---



### Étape 4 — Caisse & recettes (4 min)



**Déconnexion** → **Compte** : `caissier@consulos.local`



1. Arrivée **directe sur Caisse** (pas de dashboard KPIs)

2. *Montrer le menu* : pas de RDV ni Administration

3. Rechercher `Aminata` → créer encaissement (ex. 50 €)

4. Cliquer le n° **REC-…** → page reçu

5. **Télécharger le PDF** (iText 7)

6. Dans le panneau **Journal de caisse** → **Exporter Excel**

7. Ouvrir le fichier `.xlsx` (vérifier en-tête)



**Message clé** : encaissement, reçu officiel, journal comptable exportable.



---



### Étape 5 — Administration & audit (3 min)



**Déconnexion** → **Compte** : `admin@consulos.local`



1. Arrivée sur **Administration** → **Utilisateurs** (liste des comptes démo)

2. **Journal d'audit** → filtrer les actions récentes (`USAGER_RGPD_EXPORT`, transitions dossier…)

3. **Swagger** (onglet navigateur) → montrer la doc API groupée par module



**Message clé** : gouvernance, traçabilité, API documentée pour intégrations futures.



---



### Étape 6 — Résilience auth (1 min, optionnel)



1. Recharger la page (F5) en étant connecté → session restaurée via cookie refresh

2. Montrer la déconnexion propre



---



## Points techniques à mentionner



- **Stack** : Spring Boot 3.3 / React 18 / PostgreSQL 16 / MinIO / Redis

- **Sécurité** : JWT RS256, refresh httpOnly, `@PreAuthorize` + **menu/routes FE filtrés par rôle**, audit AOP

- **Multi-tenant** : isolation `organisation_id` sur toutes les requêtes

- **Documents** : upload dossier, onglet usager, page globale filtrable

- **Notifications (Phase 2)** : cloche header + WebSocket STOMP (`/api/v1/ws`) — transition / assignation dossier

- **Portail usager (Phase 2)** : `/portal` — magic link (réf. + email) → suivi dossier, dépôt documents, prise RDV

- **Reporting (Phase 2)** : menu Reporting (superviseur/admin) — synthèse mensuelle + Excel / CSV

- **SMS RDV (Phase 2, optionnel)** : `CONSULOS_SMS_ENABLED=true` — confirmation / annulation / rappel en complément email (provider `log` ou `twilio`)

- **Paiement en ligne (Phase 3)** : `CONSULOS_PAYMENT_ENABLED=true` — simulateur `log` ou **Stripe Checkout** (`CONSULOS_PAYMENT_PROVIDER=stripe-sdk` + `STRIPE_SECRET_KEY` / webhook `POST /payments/webhook/stripe`).

- **OCR (Phase 3)** : `CONSULOS_OCR_ENABLED=true` — providers `log` | `structured-log` | **`tesseract`** (Tess4J). Modèles : `.\scripts\download-tessdata.ps1` puis `CONSULOS_OCR_TESSERACT_DATAPATH=…/tessdata`. Viewer → **Pré-remplir usager**.

- **Réseau multi-postes (Phase 3 Vague A–D)** : `CONSULOS_RESEAU_ENABLED=true` — login `reseau@consulos.local` / `Admin1234!` → menu **Réseau** (`/regional`) : KPIs, alertes, reporting Excel, **rattacher/détacher postes** (code org) + liste membres. APIs `/reseau/*`, `/regional/*`.

- **Tests** : 19 scénarios Playwright (auth, dossier, documents, caisse, exports, perf < 2 s)

- **Secrets** : clés dans `secrets/`, jamais commitées (`.gitignore`)



---



## Dépannage rapide



| Problème | Solution |

|----------|----------|

| Port 8082 occupé | Arrêter l'ancien Java ou `Get-NetTCPConnection -LocalPort 8082` |

| 403 sur modification usager | Se connecter en **agent** ou **admin** |

| Accès Administration refusé | Compte **admin** uniquement (route + menu protégés) |

| Toast 403 en caissier | Se déconnecter / reconnecter ; menu ne doit plus proposer RDV/Admin |

| Backend sans endpoints export | Redémarrer `mvn spring-boot:run` après compile |

| PostgreSQL refusé | `.\scripts\start-dev-infra.ps1` (Docker, port **5433**) |

| Rate limit login | Attendre 15 min ou redémarrer le backend |

| SMS RDV (optionnel) | Dans `.env` : `CONSULOS_SMS_ENABLED=true` + `CONSULOS_SMS_PROVIDER=log` → logs `[SMS-LOG]` ; Twilio : `provider=twilio` + SID/token/from |



---



## Tests automatisés



```powershell

cd consulos\consulos-frontend

npm run test:e2e

```



Couverture : auth, dashboard (superviseur KPIs + agent accueil), usager/dossier, encaissement PDF, exports RGPD/Excel, performance PRD.



---



## Ports Windows



- PostgreSQL Docker : **5433** (évite conflit 5432)

- Backend dev : **8082** (évite conflit 8080)

- Frontend E2E : **3010** (`npm run dev:e2e`)


