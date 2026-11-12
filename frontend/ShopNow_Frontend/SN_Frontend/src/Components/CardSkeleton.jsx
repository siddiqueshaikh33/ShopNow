function CardSkeleton() {
  return (
    <div className="w-full h-full flex flex-col justify-between rounded-2xl border border-slate-200/90 bg-white shadow-sm overflow-hidden animate-pulse">
      {/* Image Stage Skeleton */}
      <div className="h-48 bg-slate-100 relative w-full" />

      {/* Content Skeleton */}
      <div className="p-4 space-y-3 flex-grow flex flex-col justify-between">
        <div className="space-y-2">
          <div className="h-4 w-3/4 bg-slate-200 rounded" />
          <div className="h-3 w-full bg-slate-100 rounded" />
          <div className="h-3 w-2/3 bg-slate-100 rounded" />
        </div>

        <div className="mt-auto pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
          <div className="space-y-1">
            <div className="h-2 w-8 bg-slate-100 rounded" />
            <div className="h-5 w-14 bg-slate-200 rounded" />
          </div>
          <div className="h-8 w-24 bg-slate-200 rounded-xl" />
        </div>
      </div>
    </div>
  );
}

export default CardSkeleton;
