import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { AppShell } from '@/components/layout/AppShell'
import { ProtectedRoute } from '@/router/ProtectedRoute'

const PortalAccessPage = lazy(() => import('@/pages/portal/PortalAccessPage'))
const PortalOuvrirPage = lazy(() => import('@/pages/portal/PortalOuvrirPage'))
const PortalDossierPage = lazy(() => import('@/pages/portal/PortalDossierPage'))
const LoginPage = lazy(() => import('@/pages/auth/LoginPage'))
const ForgotPasswordPage = lazy(() => import('@/pages/auth/ForgotPasswordPage'))
const ResetPasswordPage = lazy(() => import('@/pages/auth/ResetPasswordPage'))
const DashboardPage = lazy(() => import('@/pages/dashboard/DashboardPage'))
const UsagersPage = lazy(() => import('@/pages/usagers/UsagersPage'))
const UsagerDetailPage = lazy(() => import('@/pages/usagers/UsagerDetailPage'))
const UsagerFormPage = lazy(() => import('@/pages/usagers/UsagerFormPage'))
const DossiersPage = lazy(() => import('@/pages/dossiers/DossiersPage'))
const DossierCreatePage = lazy(() => import('@/pages/dossiers/DossierCreatePage'))
const DossierDetailPage = lazy(() => import('@/pages/dossiers/DossierDetailPage'))
const AgendaPage = lazy(() => import('@/pages/rendezvous/AgendaPage'))
const DocumentsPage = lazy(() => import('@/pages/DocumentsPage'))
const CaissePage = lazy(() => import('@/pages/encaissement/CaissePage'))
const PaiementEnLignePage = lazy(() => import('@/pages/encaissement/PaiementEnLignePage'))
const ReportingPage = lazy(() => import('@/pages/reporting/ReportingPage'))
const RecuPage = lazy(() => import('@/pages/encaissement/RecuPage'))
const AdminLayout = lazy(() => import('@/pages/administration/AdminLayout'))
const UsersPage = lazy(() => import('@/pages/administration/UsersPage'))
const RolesPage = lazy(() => import('@/pages/administration/RolesPage'))
const ParametresPage = lazy(() => import('@/pages/administration/ParametresPage'))
const AuditLogsPage = lazy(() => import('@/pages/administration/AuditLogsPage'))

function PageLoader() {
  const { t } = useTranslation('common')
  return (
    <div className="flex h-64 items-center justify-center text-muted-foreground">
      {t('loading')}
    </div>
  )
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/portal" element={<PortalAccessPage />} />
          <Route path="/portal/ouvrir" element={<PortalOuvrirPage />} />
          <Route path="/portal/dossier" element={<PortalDossierPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route index element={<DashboardPage />} />
              <Route path="usagers" element={<UsagersPage />} />
              <Route element={<ProtectedRoute roles={['ROLE_AGENT', 'ROLE_ADMIN']} />}>
                <Route path="usagers/new" element={<UsagerFormPage />} />
                <Route path="usagers/:id/edit" element={<UsagerFormPage />} />
              </Route>
              <Route path="usagers/:id" element={<UsagerDetailPage />} />
              <Route path="dossiers" element={<DossiersPage />} />
              <Route path="dossiers/new" element={<DossierCreatePage />} />
              <Route path="dossiers/:id" element={<DossierDetailPage />} />
              <Route path="rendezvous" element={<AgendaPage />} />
              <Route path="documents" element={<DocumentsPage />} />
              <Route path="caisse" element={<CaissePage />} />
              <Route path="caisse/paiement-en-ligne" element={<PaiementEnLignePage />} />
              <Route path="caisse/recu/:id" element={<RecuPage />} />
              <Route path="reporting" element={<ReportingPage />} />
              <Route element={<ProtectedRoute roles={['ROLE_ADMIN']} />}>
                <Route path="administration" element={<AdminLayout />}>
                  <Route index element={<Navigate to="users" replace />} />
                  <Route path="users" element={<UsersPage />} />
                  <Route path="roles" element={<RolesPage />} />
                  <Route path="parametres" element={<ParametresPage />} />
                  <Route path="audit" element={<AuditLogsPage />} />
                </Route>
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}

export default AppRouter
