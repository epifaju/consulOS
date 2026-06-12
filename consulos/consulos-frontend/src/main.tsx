import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@/styles/globals.css'
import { ConsulOSApp } from '@/ConsulOSApp'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConsulOSApp />
  </StrictMode>,
)
