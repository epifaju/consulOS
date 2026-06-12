export function UsagersTableSkeleton() {
  return (
    <div className="space-y-3" data-testid="usagers-skeleton">
      {Array.from({ length: 8 }).map((_, index) => (
        <div
          key={index}
          className="h-12 animate-pulse rounded-md bg-muted"
        />
      ))}
    </div>
  )
}

export default UsagersTableSkeleton
