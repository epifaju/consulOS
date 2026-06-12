package com.consulos.config.openapi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Métadonnées OpenAPI pour les contrôleurs existants (sans modifier leurs sources).
 */
public final class OpenApiOperationCatalog {

    private OpenApiOperationCatalog() {
    }

    public static final Map<String, String> TAG_DESCRIPTIONS = Map.ofEntries(
            Map.entry("01 — Authentification", "Connexion JWT RS256, refresh httpOnly, profil utilisateur"),
            Map.entry("02 — Usagers", "Référentiel usagers, recherche, doublons, dossiers liés"),
            Map.entry("02 — Usagers — RGPD", "Export portabilité des données personnelles"),
            Map.entry("03 — Dossiers", "Cycle de vie dossier, workflow, commentaires, historique"),
            Map.entry("03 — Types de service", "Catalogue des prestations consulaires"),
            Map.entry("04 — Rendez-vous", "Agenda, créneaux, disponibilités"),
            Map.entry("04 — Paramètres RDV", "Créneaux horaires et jours de fermeture"),
            Map.entry("05 — Documents", "Upload MinIO, contrôle qualité, téléchargement"),
            Map.entry("06 — Encaissements", "Paiements, journal de caisse, reçus PDF"),
            Map.entry("06 — Encaissements — Exports", "Exports Excel du journal de caisse"),
            Map.entry("06 — Tarifs", "Grille tarifaire par type de service"),
            Map.entry("07 — Tableau de bord", "KPIs, graphiques, alertes, charge agents"),
            Map.entry("08 — Administration", "Utilisateurs, rôles, paramètres organisation"),
            Map.entry("08 — Audit", "Journal d'audit et export conformité")
    );

    private static final Map<String, OperationMeta> OPERATIONS = new LinkedHashMap<>();

    static {
        auth();
        usagers();
        dossiers();
        rendezvous();
        documents();
        encaissements();
        dashboard();
        admin();
        audit();
    }

    public static Optional<OperationMeta> resolve(String handlerKey) {
        return Optional.ofNullable(OPERATIONS.get(handlerKey));
    }

    private static void put(String key, String tag, String summary, String description, boolean isPublic) {
        OPERATIONS.put(key, new OperationMeta(tag, summary, description, isPublic));
    }

    private static void auth() {
        put("AuthController#login", "01 — Authentification",
                "Connexion utilisateur",
                "Authentifie par email/mot de passe. Retourne un access token JWT et pose un refresh token httpOnly.",
                true);
        put("AuthController#refresh", "01 — Authentification",
                "Rafraîchir le token",
                "Émet un nouvel access token à partir du refresh token httpOnly (cookie).",
                true);
        put("AuthController#logout", "01 — Authentification",
                "Déconnexion",
                "Invalide le refresh token et supprime le cookie.",
                false);
        put("AuthController#forgotPassword", "01 — Authentification",
                "Mot de passe oublié",
                "Envoie un email de réinitialisation si l'adresse existe (réponse uniforme).",
                true);
        put("AuthController#resetPassword", "01 — Authentification",
                "Réinitialiser le mot de passe",
                "Applique un nouveau mot de passe via token de réinitialisation.",
                true);
        put("AuthController#me", "01 — Authentification",
                "Profil courant",
                "Retourne le profil de l'utilisateur authentifié (rôle, organisation).",
                false);
    }

    private static void usagers() {
        put("UsagerController#list", "02 — Usagers",
                "Lister les usagers",
                "Recherche paginée multi-critères (paramètre `q`). Rôles : AGENT, SUPERVISEUR, CAISSIER, ADMIN.",
                false);
        put("UsagerController#create", "02 — Usagers",
                "Créer un usager",
                "Crée une fiche usager dans l'organisation courante. Rôles : AGENT, ADMIN.",
                false);
        put("UsagerController#getById", "02 — Usagers",
                "Détail usager",
                "Retourne la fiche complète d'un usager par UUID.",
                false);
        put("UsagerController#update", "02 — Usagers",
                "Modifier un usager",
                "Met à jour les informations d'un usager existant.",
                false);
        put("UsagerController#delete", "02 — Usagers",
                "Désactiver un usager",
                "Suppression logique (soft delete) de la fiche usager.",
                false);
        put("UsagerController#search", "02 — Usagers",
                "Recherche rapide",
                "Recherche full-text pour autocomplétion (min. 2 caractères).",
                false);
        put("UsagerController#doublons", "02 — Usagers",
                "Détecter les doublons",
                "Retourne les groupes de fiches potentiellement en double.",
                false);
        put("UsagerController#dossiers", "02 — Usagers",
                "Dossiers d'un usager",
                "Liste paginée des dossiers rattachés à un usager.",
                false);
        put("UsagerRgpdExportController#exportRgpd", "02 — Usagers — RGPD",
                "Exporter les données RGPD",
                "Télécharge un export JSON (portabilité) : profil, dossiers, RDV, encaissements, documents.",
                false);
    }

    private static void dossiers() {
        put("DossierController#list", "03 — Dossiers",
                "Lister les dossiers",
                "Liste paginée avec filtres statut, type, agent, usager, priorité, dates.",
                false);
        put("DossierController#create", "03 — Dossiers",
                "Créer un dossier",
                "Ouvre un nouveau dossier pour un usager et un type de service.",
                false);
        put("DossierController#stats", "03 — Dossiers",
                "Statistiques dossiers",
                "Compteurs par statut pour l'organisation.",
                false);
        put("DossierController#getById", "03 — Dossiers",
                "Détail dossier",
                "Retourne le dossier avec checklist, commentaires et métadonnées.",
                false);
        put("DossierController#update", "03 — Dossiers",
                "Modifier un dossier",
                "Met à jour les champs éditables du dossier.",
                false);
        put("DossierController#transition", "03 — Dossiers",
                "Transition de statut",
                "Applique une transition workflow (ex. BROUILLON → SOUMIS).",
                false);
        put("DossierController#historique", "03 — Dossiers",
                "Historique du dossier",
                "Journal des changements de statut et actions.",
                false);
        put("DossierController#addCommentaire", "03 — Dossiers",
                "Ajouter un commentaire",
                "Ajoute un commentaire interne au dossier.",
                false);
        put("TypeServiceController#list", "03 — Types de service",
                "Lister les types de service",
                "Catalogue des prestations disponibles pour la création de dossiers.",
                false);
    }

    private static void rendezvous() {
        put("RdvController#list", "04 — Rendez-vous",
                "Lister les rendez-vous",
                "Liste paginée avec filtres date, agent, statut.",
                false);
        put("RdvController#create", "04 — Rendez-vous",
                "Créer un rendez-vous",
                "Planifie un RDV lié à un usager et optionnellement un dossier.",
                false);
        put("RdvController#disponibilites", "04 — Rendez-vous",
                "Créneaux disponibles",
                "Retourne les créneaux libres pour un agent et une date.",
                false);
        put("RdvController#agenda", "04 — Rendez-vous",
                "Agenda",
                "Vue agenda sur une plage de dates.",
                false);
        put("RdvController#getById", "04 — Rendez-vous",
                "Détail rendez-vous",
                "Retourne un RDV par identifiant.",
                false);
        put("RdvController#update", "04 — Rendez-vous",
                "Modifier un rendez-vous",
                "Met à jour date, motif ou agent du RDV.",
                false);
        put("RdvController#updateStatut", "04 — Rendez-vous",
                "Changer le statut",
                "Confirme, annule ou marque absent un rendez-vous.",
                false);
        put("ParametresController#listCreneaux", "04 — Paramètres RDV",
                "Lister les créneaux",
                "Créneaux horaires configurés pour la prise de RDV.",
                false);
        put("ParametresController#createCreneau", "04 — Paramètres RDV",
                "Créer un créneau",
                "Ajoute un créneau horaire récurrent.",
                false);
        put("ParametresController#updateCreneau", "04 — Paramètres RDV",
                "Modifier un créneau",
                "Met à jour un créneau existant.",
                false);
        put("ParametresController#listJoursFermeture", "04 — Paramètres RDV",
                "Jours de fermeture",
                "Liste des jours sans prise de RDV.",
                false);
        put("ParametresController#createJourFermeture", "04 — Paramètres RDV",
                "Ajouter un jour de fermeture",
                "Bloque une date pour les RDV.",
                false);
        put("ParametresController#updateJourFermeture", "04 — Paramètres RDV",
                "Modifier un jour de fermeture",
                "Met à jour un jour de fermeture existant.",
                false);
    }

    private static void documents() {
        put("DocumentController#upload", "05 — Documents",
                "Uploader un document",
                "Upload multipart vers MinIO, rattaché à un dossier/usager.",
                false);
        put("DocumentController#getById", "05 — Documents",
                "Métadonnées document",
                "Retourne les métadonnées d'un document.",
                false);
        put("DocumentController#getDownloadUrl", "05 — Documents",
                "URL de téléchargement",
                "Génère une URL présignée MinIO pour téléchargement sécurisé.",
                false);
        put("DocumentController#updateStatutControle", "05 — Documents",
                "Contrôle qualité",
                "Valide ou rejette un document (contrôle qualité).",
                false);
        put("DocumentController#delete", "05 — Documents",
                "Supprimer un document",
                "Suppression logique du document.",
                false);
        put("DossierDocumentController#listByDossier", "05 — Documents",
                "Documents d'un dossier",
                "Liste les pièces jointes actives d'un dossier.",
                false);
    }

    private static void encaissements() {
        put("EncaissementController#list", "06 — Encaissements",
                "Lister les encaissements",
                "Liste paginée avec filtres statut, mode, caissier, dates.",
                false);
        put("EncaissementController#create", "06 — Encaissements",
                "Créer un encaissement",
                "Ouvre un encaissement pour un usager/dossier.",
                false);
        put("EncaissementController#journalCaisse", "06 — Encaissements",
                "Journal de caisse",
                "Synthèse des opérations pour une date donnée.",
                false);
        put("EncaissementController#getById", "06 — Encaissements",
                "Détail encaissement",
                "Retourne un encaissement avec lignes et paiements.",
                false);
        put("EncaissementController#enregistrerPaiement", "06 — Encaissements",
                "Enregistrer un paiement",
                "Enregistre le règlement (espèces, CB, etc.).",
                false);
        put("EncaissementController#getRecuPdf", "06 — Encaissements",
                "Télécharger le reçu PDF",
                "Génère le reçu PDF iText pour un encaissement payé.",
                false);
        put("EncaissementExportController#exportExcel", "06 — Encaissements — Exports",
                "Exporter le journal Excel",
                "Télécharge le journal de caisse au format .xlsx pour une plage de dates.",
                false);
        put("TarifController#list", "06 — Tarifs",
                "Lister les tarifs",
                "Grille tarifaire active par type de service.",
                false);
        put("TarifController#create", "06 — Tarifs",
                "Créer un tarif",
                "Ajoute un tarif pour une prestation.",
                false);
        put("TarifController#update", "06 — Tarifs",
                "Modifier un tarif",
                "Met à jour le montant ou la devise d'un tarif.",
                false);
    }

    private static void dashboard() {
        put("DashboardController#stats", "07 — Tableau de bord",
                "KPIs principaux",
                "Indicateurs : dossiers ouverts, RDV du jour, recettes, alertes.",
                false);
        put("DashboardController#dossiersChart", "07 — Tableau de bord",
                "Graphique dossiers",
                "Série temporelle des dossiers créés.",
                false);
        put("DashboardController#recettesChart", "07 — Tableau de bord",
                "Graphique recettes",
                "Série temporelle des encaissements.",
                false);
        put("DashboardController#alerts", "07 — Tableau de bord",
                "Alertes actives",
                "Liste des alertes métier non résolues.",
                false);
        put("DashboardController#agentsCharge", "07 — Tableau de bord",
                "Charge des agents",
                "Répartition des dossiers ouverts par agent.",
                false);
    }

    private static void admin() {
        put("AdminController#listUsers", "08 — Administration",
                "Lister les utilisateurs",
                "Liste paginée des comptes de l'organisation.",
                false);
        put("AdminController#getUser", "08 — Administration",
                "Détail utilisateur",
                "Retourne un compte utilisateur par UUID.",
                false);
        put("AdminController#createUser", "08 — Administration",
                "Créer un utilisateur",
                "Crée un compte avec rôle et mot de passe temporaire.",
                false);
        put("AdminController#updateUser", "08 — Administration",
                "Modifier un utilisateur",
                "Met à jour nom, email, rôle d'un compte.",
                false);
        put("AdminController#toggleActif", "08 — Administration",
                "Activer / désactiver",
                "Bascule le statut actif d'un compte.",
                false);
        put("AdminController#resetPassword", "08 — Administration",
                "Réinitialiser le mot de passe",
                "Génère un mot de passe temporaire pour un utilisateur.",
                false);
        put("AdminController#deleteUser", "08 — Administration",
                "Supprimer un utilisateur",
                "Suppression logique du compte (sauf soi-même).",
                false);
        put("AdminController#listRoles", "08 — Administration",
                "Lister les rôles",
                "Catalogue des rôles et permissions.",
                false);
        put("AdminController#getOrganisation", "08 — Administration",
                "Paramètres organisation",
                "Retourne les paramètres du consulat courant.",
                false);
        put("AdminController#updateOrganisation", "08 — Administration",
                "Modifier l'organisation",
                "Met à jour nom, fuseau, coordonnées du consulat.",
                false);
    }

    private static void audit() {
        put("AuditController#list", "08 — Audit",
                "Journal d'audit",
                "Liste paginée des événements audités (filtres date, utilisateur, entité).",
                false);
        put("AuditController#export", "08 — Audit",
                "Exporter le journal",
                "Export CSV/JSON du journal d'audit pour conformité.",
                false);
    }

    public record OperationMeta(String tag, String summary, String description, boolean publicEndpoint) {
    }
}
