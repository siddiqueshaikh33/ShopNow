import { useState } from "react";
import { toast } from "react-toastify";
import axiosInstance from "../api/axiosInstance";

function CartItems({ item, updateCart, deleteCart }) {
  const [loading, setLoading] = useState(false);

  const handleUpdateQuantity = async (e, action) => {
    e.preventDefault();
    e.stopPropagation();

    const newQuantity =
      action === "increase" ? item.quantity + 1 : item.quantity - 1;

    // Prevent invalid quantity
    if (newQuantity < 1) {
      toast.warn("Minimum quantity is 1");
      return;
    }

    try {
      setLoading(true);
      updateCart(item.product_id, newQuantity); // Update parent state
      await axiosInstance.put(
        "/cart/update",
        null, // IMPORTANT for @RequestParam
        {
          params: {
            productId: item.product_id,
            quantity: newQuantity,
          },
          withCredentials: true,
        }
      ); // refetch cart from parent
    } catch (error) {
      console.error(error);
      updateCart(item.product_id, item.quantity); // Revert on error
      toast.error(error.response?.data || "Failed to update quantity");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      setLoading(true);
      await axiosInstance.delete("/cart/delete", {
        params: { productId: item.product_id },
        withCredentials: true,
      });
      deleteCart(item.product_id); // Update parent state
    } catch (error) {
      console.error(error);
      toast.error(error.response?.data || "Failed to remove item");
    } finally {
      setLoading(false); // Remove item from parent state
    }
  };

  return (
    <div className="w-full bg-white flex flex-col sm:flex-row items-center sm:items-center gap-4 sm:gap-6 p-4 sm:p-5 mb-4 sm:mb-6 rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition relative">
      {/* Remove Button */}
      <button
        className="absolute top-3 right-3 w-8 h-8 flex items-center justify-center rounded-full bg-gray-100 text-gray-500 hover:bg-red-500 hover:text-white transition"
        title="Remove item"
        onClick={(e) => handleDelete(e)}
      >
        ✕
      </button>

      {/* Product Image */}
      <div className="w-28 h-28 sm:w-36 sm:h-36 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0">
        <img
          src={item.product_img}
          alt={item.product_name}
          className="w-full h-full object-cover"
          onError={(e) => (e.currentTarget.style.display = "none")}
        />
      </div>

      {/* Content */}
      <div className="flex flex-col flex-1 justify-between w-full h-full">
        {/* Product Info */}
        <div>
          <h3 className="text-lg sm:text-xl font-semibold text-gray-900 pr-8 sm:pr-0">
            {item.product_name}
          </h3>
          <p className="text-sm sm:text-md text-gray-600 line-clamp-2">
            {item.product_description}
          </p>
        </div>

        {/* Bottom Section */}
        <div className="flex items-end justify-between mt-3 w-full">
          {/* Quantity Controls */}
          <div className="flex items-center gap-3">
            <button
              disabled={loading}
              className="w-8 h-8 rounded-full border border-gray-300 bg-gray-100 hover:bg-green-200 text-lg font-semibold disabled:opacity-50"
              onClick={(e) => handleUpdateQuantity(e, "decrease")}
            >
              −
            </button>

            <span className="text-lg font-semibold">{item.quantity}</span>

            <button
              disabled={loading}
              className="w-8 h-8 rounded-full border border-gray-300 bg-gray-100 hover:bg-green-200 text-lg font-semibold disabled:opacity-50"
              onClick={(e) => handleUpdateQuantity(e, "increase")}
            >
              +
            </button>
          </div>

          {/* Price Info */}
          <div className="text-right">
            <p className="text-md text-gray-500">
              ₹ {item.price_per_unit.toFixed(2)} / item
            </p>
            <p className="text-lg font-bold text-blue-600">
              ₹ {item.total_price.toFixed(2)}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CartItems;
