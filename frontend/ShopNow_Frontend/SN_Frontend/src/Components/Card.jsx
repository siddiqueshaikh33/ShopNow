function Card({ product, onAction }) {
  const isOutOfStock = !product.stock || product.stock <= 0;

  return (
    <div className="w-full h-full flex flex-col justify-between rounded-2xl border border-slate-200/90 bg-white shadow-sm hover:shadow-md transition-all duration-300 overflow-hidden group">
      {/* Product Image Stage */}
      <div className="relative bg-slate-50/80 p-4 h-48 w-full flex items-center justify-center border-b border-slate-100 overflow-hidden">
        <img
          src={product.product_img}
          alt={product.name}
          className="h-40 w-full object-contain transition-transform duration-300 group-hover:scale-105"
          loading="lazy"
        />
        <span
          className={`absolute top-3 right-3 text-[11px] font-semibold px-2.5 py-0.5 rounded-full border shadow-2xs ${
            !isOutOfStock
              ? "bg-emerald-50 text-emerald-800 border-emerald-200"
              : "bg-slate-100 text-slate-500 border-slate-200"
          }`}
        >
          {!isOutOfStock ? `${product.stock} in stock` : "Out of stock"}
        </span>
      </div>

      {/* Product Content Details */}
      <div className="p-4 flex flex-col flex-grow justify-between gap-3">
        <div>
          <h3
            className="text-sm sm:text-base font-semibold text-slate-900 line-clamp-2 h-11 leading-snug"
            title={product.name}
          >
            {product.name}
          </h3>
          <p className="mt-1 text-xs text-slate-500 line-clamp-2 h-8 leading-relaxed">
            {product.description || "Authentic quality selection from our catalog."}
          </p>
        </div>

        <div className="mt-auto pt-3 border-t border-slate-100 flex items-center justify-between gap-3">
          <div>
            <span className="text-[10px] uppercase tracking-wider text-slate-400 font-semibold block">
              Price
            </span>
            <span className="text-lg font-bold text-slate-900">
              ${product.price}
            </span>
          </div>

          <button
            type="button"
            disabled={isOutOfStock}
            onClick={() => onAction(product)}
            className="px-4 py-2.5 text-xs font-semibold rounded-xl transition-all duration-200 shadow-xs disabled:opacity-40 disabled:cursor-not-allowed bg-slate-900 text-white hover:bg-[#062828] active:scale-[0.98] cursor-pointer whitespace-nowrap"
            style={{
              borderBottom: "2px solid #ADEFD1",
            }}
          >
            {isOutOfStock ? "Out of Stock" : "Add to Cart"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default Card;
