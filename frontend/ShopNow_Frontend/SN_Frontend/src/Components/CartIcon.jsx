import { ShoppingCart } from "lucide-react";
import { useNavigate } from "react-router-dom";

export default function CartIcon({ count }) {
  const navigate = useNavigate();

  return (
    <button
      type="button"
      aria-label="View shopping cart"
      className="relative p-1 rounded-full hover:bg-black/5 transition cursor-pointer"
      onClick={() => navigate("/viewcart")}
    >
      <ShoppingCart className="w-6 h-6 sm:w-7 sm:h-7 text-slate-800 hover:text-slate-950 transition-colors" />
      {count > 0 && (
        <span
          className="absolute -top-1 -right-1 bg-red-600 text-white text-[10px] font-extrabold min-w-[18px] h-[18px] px-1 rounded-full flex items-center justify-center shadow-xs border border-white"
        >
          {count}
        </span>
      )}
    </button>
  );
}
