import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuditLogsPage } from '@/pages/administration/AuditLogsPage'
import { auditService } from '@/services/auditService'
import type { AuditLog, PageResponse } from '@/types/audit'

const mockLog = {
  id: 'log-1',
  organisationId: 'org-1',
  userId: 'user-1',
  userNom: 'Diallo',
  userPrenom: 'Awa',
  action: 'DOSSIER_CREATE',
  entiteType: 'DOSSIER',
  entiteId: 'd1',
  ancienEtat: null,
  nouvelEtat: '{"id":"d1"}',
  ipAddress: '127.0.0.1',
  userAgent: 'JUnit',
  createdAt: '2026-09-09T10:00:00Z',
} satisfies AuditLog

const { logsState } = vi.hoisted(() => ({
  logsState: {
    data: null as PageResponse<AuditLog> | null,
    isLoading: false,
  },
}))

vi.mock('@/hooks/useAudit', () => ({
  useAuditLogs: () => logsState,
}))

vi.mock('@/services/auditService', () => ({
  auditService: {
    exportCsv: vi.fn(),
  },
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AuditLogsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AuditLogsPage', () => {
  beforeEach(() => {
    logsState.data = {
      content: [mockLog],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }
    vi.mocked(auditService.exportCsv).mockReset()
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:audit'),
      revokeObjectURL: vi.fn(),
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders audit logs', () => {
    renderPage()
    expect(screen.getByText(/Journal d'audit/i)).toBeInTheDocument()
    expect(screen.getByText(/Awa Diallo/)).toBeInTheDocument()
    expect(screen.getByText('DOSSIER_CREATE')).toBeInTheDocument()
    expect(screen.getByText('DOSSIER')).toBeInTheDocument()
    expect(screen.getByText('127.0.0.1')).toBeInTheDocument()
  })

  it('exposes action filter', () => {
    renderPage()
    expect(screen.getByPlaceholderText(/Filtrer par action/i)).toBeInTheDocument()
  })

  it('expands row to show etats json', async () => {
    const user = userEvent.setup()
    renderPage()
    const expand = document.querySelector('svg.lucide-chevron-right')?.closest('button')
    expect(expand).toBeTruthy()
    await user.click(expand!)
    expect(screen.getByText(/nouvelEtat|"nouveau"/i)).toBeInTheDocument()
  })

  it('exports csv', async () => {
    vi.mocked(auditService.exportCsv).mockResolvedValue({ data: new Blob(['csv']) } as never)
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('button', { name: /Exporter CSV/i }))
    expect(auditService.exportCsv).toHaveBeenCalled()
  })
})
