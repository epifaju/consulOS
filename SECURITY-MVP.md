# ConsulOS — Revue sécurité MVP

Date : juin 2026 | Périmètre : Sprint 8 clôture MVP

---

## Synthèse

| Domaine | Statut | Action |
|---------|--------|--------|
| Auth JWT RS256 + refresh httpOnly | OK | — |
| `@PreAuthorize` sur endpoints | OK | Référence centralisée ajoutée |
| Isolation multi-tenant | OK | `organisation_id` filtré côté services |
| Secrets hors Git | OK | `.gitignore` + `scripts/generate-jwt-keys.ps1` |
| UI alignée sur rôles backend | OK | `rolePermissions.ts` + routes protégées |
| Catalogue permissions vs API | Écart documenté | Voir §2 |
| Rate limiting login | OK | 5 req / 15 min |
| CORS dev | OK | `localhost` + `127.0.0.1` |
| Audit AOP | OK | Actions sensibles journalisées |

---

## 1. Matrice rôles × endpoints (référence)

Source de vérité code : `MvpRoleAuthorizationReference.java` + annotations `@PreAuthorize`.

| Rôle | Usagers écriture | Dossiers écriture | Caisse | Dashboard KPIs | Admin | Export RGPD |
|------|------------------|-------------------|--------|----------------|-------|-------------|
| ADMIN | oui | oui | oui | oui | oui | oui |
| SUPERVISEUR | non | validation | lecture | oui | non | oui |
| AGENT | oui | oui | création | non | non | oui |
| CAISSIER | lecture | lecture | oui | recettes | non | non |
| ACCUEIL | lecture | lecture | non | non | non | non |

---

## 2. Écarts identifiés (RolePermissionsCatalog legacy)

Le fichier `RolePermissionsCatalog.java` (Phase 1) est **incomplet** par rapport aux `@PreAuthorize` réels :

| Écart | Détail | Risque | Mitigation MVP |
|-------|--------|--------|----------------|
| CAISSIER lit usagers | API autorise `GET /usagers` | Faible — nécessaire pour caisse | Documenté |
| SUPERVISEUR sans `usagers:write` | Cohérent métier | Aucun | UI masque Modifier |
| ACCUEIL sur search usagers | API `/usagers/search` | Faible | OK pour accueil |
| Audit réservé ADMIN | API `/audit/*` | Aucun | Conforme |

**Recommandation Phase 2** : remplacer `RolePermissionsCatalog` par une matrice unique alimentant RBAC frontend + tests de non-régression.

---

## 3. Secrets & configuration

- Ne jamais committer : `.env`, `secrets/*.pem`, keystores SSL
- Génération clés : `.\scripts\generate-jwt-keys.ps1`
- Production : variables d'environnement + vault (pas de defaults `consulos_dev_pass`)
- JWT dev embarqué (`jwt-dev/`) : ignoré par Git, fallback classpath uniquement

---

## 4. Contrôles frontend

- Routes formulaire usager : `ROLE_AGENT`, `ROLE_ADMIN` uniquement
- Boutons Modifier / Supprimer / Nouvel usager : masqués selon rôle
- Toast **403 ACCESS_DENIED** : message explicite via intercepteur axios
- Token access en mémoire Zustand ; refresh en cookie httpOnly

---

## 5. Checklist pré-production

- [ ] `SPRING_PROFILES_ACTIVE=prod` + SSL activé
- [ ] Mots de passe Docker / DB / Redis / MinIO uniques
- [ ] Rotation clés JWT + `JWT_*_KEY_PATH` en vault
- [ ] `show-sql: false`, logs INFO
- [ ] Revue OWASP ZAP ou équivalent sur `/api/v1`
- [ ] Vérifier `git log -- secrets/` vide

---

## 6. Actions correctives livrées (cette revue)

1. `MvpRoleAuthorizationReference.java` — matrice documentée en code
2. `rolePermissions.ts` — alignement UI
3. `apiErrors.ts` + intercepteur — feedback 403 utilisateur
4. `.gitignore` renforcé + script clés JWT
