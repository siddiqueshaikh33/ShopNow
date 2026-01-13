import { ShoppingCart } from "lucide-react";
import { useNavigate } from "react-router-dom";

export default function CartIcon({count}) {
  
  const navigate = useNavigate();

  return (
    <div className="relative" onClick={() => navigate("/viewcart")}>
      <ShoppingCart className="w-7 h-7 m-1 text-gray-600 hover:text-green-600 cursor-pointer" />
      <span className="absolute top-0.5 -right-1 bg-red-500 text-white text-xs px-1.5 rounded-full">
        {count}
      </span>
    </div>
  );
}
