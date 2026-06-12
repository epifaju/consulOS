import { useTranslation } from 'react-i18next'

export function DocumentsPage() {
  const { t } = useTranslation('documents')
  return <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>
}

export default DocumentsPage
