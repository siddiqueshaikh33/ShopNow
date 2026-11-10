function CardSkeleton() {
  return (
    <div className="w-[280px] sm:w-60 md:w-64 lg:w-72 max-w-full h-auto min-h-[380px] rounded-xl bg-white shadow-md overflow-hidden animate-pulse p-2">
      {/* Image skeleton */}
      <div className="h-50 bg-gray-200" />

      {/* Content skeleton */}
      <div className="p-4 space-y-3">
        <div className="h-4 w-3/4 bg-gray-200 rounded" />
        <div className="h-3 w-full bg-gray-200 rounded" />
        <div className="h-3 w-5/6 bg-gray-200 rounded" />

        <div className="h-8 w-24 bg-gray-200 rounded mt-4 flex items-center justify-center" />
      </div>
    </div>
  );
}

export default CardSkeleton;
