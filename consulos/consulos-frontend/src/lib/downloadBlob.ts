export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

export function parseFilenameFromDisposition(header: string | undefined, fallback: string): string {
  if (!header) {
    return fallback
  }
  const match = /filename="?([^";\n]+)"?/i.exec(header)
  return match?.[1]?.trim() ?? fallback
}
