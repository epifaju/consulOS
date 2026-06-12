import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import frCommon from '@/locales/fr/common.json'
import frAuth from '@/locales/fr/auth.json'
import frUsagers from '@/locales/fr/usagers.json'
import frDossiers from '@/locales/fr/dossiers.json'
import frRendezvous from '@/locales/fr/rendezvous.json'
import frDocuments from '@/locales/fr/documents.json'
import frEncaissements from '@/locales/fr/encaissements.json'
import frDashboard from '@/locales/fr/dashboard.json'
import frAdmin from '@/locales/fr/admin.json'
import enCommon from '@/locales/en/common.json'
import enAuth from '@/locales/en/auth.json'
import enUsagers from '@/locales/en/usagers.json'
import enDossiers from '@/locales/en/dossiers.json'
import enRendezvous from '@/locales/en/rendezvous.json'
import enDocuments from '@/locales/en/documents.json'
import enEncaissements from '@/locales/en/encaissements.json'
import enDashboard from '@/locales/en/dashboard.json'
import enAdmin from '@/locales/en/admin.json'
import ptCommon from '@/locales/pt/common.json'
import ptAuth from '@/locales/pt/auth.json'
import ptUsagers from '@/locales/pt/usagers.json'
import ptDossiers from '@/locales/pt/dossiers.json'
import ptRendezvous from '@/locales/pt/rendezvous.json'
import ptDocuments from '@/locales/pt/documents.json'
import ptEncaissements from '@/locales/pt/encaissements.json'
import ptDashboard from '@/locales/pt/dashboard.json'
import ptAdmin from '@/locales/pt/admin.json'

const namespaces = [
  'common',
  'auth',
  'usagers',
  'dossiers',
  'rendezvous',
  'documents',
  'encaissements',
  'dashboard',
  'admin',
] as const

void i18n.use(initReactI18next).init({
  lng: 'fr',
  fallbackLng: 'fr',
  ns: namespaces,
  defaultNS: 'common',
  interpolation: {
    escapeValue: false,
  },
  resources: {
    fr: {
      common: frCommon,
      auth: frAuth,
      usagers: frUsagers,
      dossiers: frDossiers,
      rendezvous: frRendezvous,
      documents: frDocuments,
      encaissements: frEncaissements,
      dashboard: frDashboard,
      admin: frAdmin,
    },
    en: {
      common: enCommon,
      auth: enAuth,
      usagers: enUsagers,
      dossiers: enDossiers,
      rendezvous: enRendezvous,
      documents: enDocuments,
      encaissements: enEncaissements,
      dashboard: enDashboard,
      admin: enAdmin,
    },
    pt: {
      common: ptCommon,
      auth: ptAuth,
      usagers: ptUsagers,
      dossiers: ptDossiers,
      rendezvous: ptRendezvous,
      documents: ptDocuments,
      encaissements: ptEncaissements,
      dashboard: ptDashboard,
      admin: ptAdmin,
    },
  },
})

export default i18n
