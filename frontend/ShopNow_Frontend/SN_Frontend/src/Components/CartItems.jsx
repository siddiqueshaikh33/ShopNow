import { useState } from "react";
import { toast } from "react-toastify";
import axiosInstance from "../api/axiosInstance";

function CartItems({ item, updateCart, deleteCart }) {
  const [loading, setLoading] = useState(false);

  const isMaxStockReached =
    item.product_stock != null && item.quantity >= item.product_stock;

  const handleUpdateQuantity = async (e, action) => {
    e.preventDefault();
    e.stopPropagation();

    const newQuantity =
      action === "increase" ? item.quantity + 1 : item.quantity - 1;

    // Prevent invalid minimum quantity
    if (newQuantity < 1) {
      toast.warn("Minimum quantity is 1");
      return;
    }

    // Prevent exceeding available warehouse stock
    if (
      action === "increase" &&
      item.product_stock != null &&
      newQuantity > item.product_stock
    ) {
      toast.warn(
        `Cannot add more than available stock (${item.product_stock} available)`
      );
      return;
    }

    try {
      setLoading(true);
      updateCart(item.product_id, newQuantity); // Optimistic UI update
      await axiosInstance.put(
        "/cart/update",
        null,
        {
          params: {
            productId: item.product_id,
            quantity: newQuantity,
          },
          withCredentials: true,
        }
      );
    } catch (error) {
      updateCart(item.product_id, item.quantity); // Revert on failure
      const errorMsg =
        error.response?.data?.message ||
        (typeof error.response?.data === "string"
          ? error.response.data
          : "Failed to update quantity");
      toast.error(errorMsg);
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
      const errorMsg =
        error.response?.data?.message ||
        (typeof error.response?.data === "string"
          ? error.response.data
          : "Failed to remove item");
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full bg-white flex flex-col sm:flex-row items-center sm:items-center gap-4 sm:gap-6 p-4 sm:p-5 mb-4 rounded-2xl border border-slate-200/90 shadow-sm hover:shadow-md transition relative">
      {/* Remove Button */}
      <button
        type="button"
        className="absolute top-3 right-3 w-7 h-7 flex items-center justify-center rounded-full bg-slate-100 text-slate-400 hover:bg-rose-100 hover:text-rose-600 transition cursor-pointer"
        title="Remove item"
        onClick={(e) => handleDelete(e)}
        aria-label="Remove item"
      >
        ✕
      </button>

      {/* Product Image */}
      <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-xl overflow-hidden bg-slate-50 border border-slate-100 flex-shrink-0 p-2 flex items-center justify-center">
        <img
          src={item.product_img}
          alt={item.product_name}
          className="w-full h-full object-contain"
          onError={(e) => {
            e.currentTarget.style.display = "none";
          }}
        />
      </div>

      {/* Content */}
      <div className="flex flex-col flex-1 justify-between w-full h-full">
        {/* Product Info */}
        <div>
          <h3 className="text-base sm:text-lg font-bold text-slate-900 pr-8 sm:pr-0">
            {item.product_name}
          </h3>
          <p className="text-xs text-slate-500 line-clamp-2 mt-0.5">
            {item.product_description}
          </p>
          {item.product_stock != null && (
            <div className="mt-2 flex items-center gap-2">
              {isMaxStockReached ? (
                <span className="text-[11px] font-semibold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200">
                  Maximum stock reached ({item.product_stock} available)
                </span>
              ) : (
                <span className="text-[11px] text-slate-500 font-medium">
                  Available stock: {item.product_stock} units
                </span>
              )}
            </div>
          )}
        </div>

        {/* Bottom Section */}
        <div className="flex items-end justify-between mt-4 w-full">
          {/* Quantity Controls */}
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={loading || item.quantity <= 1}
              className="w-7 h-7 rounded-lg border border-slate-300 bg-slate-50 hover:bg-slate-200 text-slate-800 font-bold transition flex items-center justify-center cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed text-sm"
              onClick={(e) => handleUpdateQuantity(e, "decrease")}
              aria-label="Decrease quantity"
              title="Decrease quantity"
            >
              −
            </button>

            <span className="text-sm font-bold text-slate-900 w-6 text-center">
              {item.quantity}
            </span>

            <button
              type="button"
              disabled={loading || isMaxStockReached}
              className="w-7 h-7 rounded-lg border border-slate-300 bg-slate-50 hover:bg-slate-200 text-slate-800 font-bold transition flex items-center justify-center cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed text-sm"
              onClick={(e) => handleUpdateQuantity(e, "increase")}
              aria-label="Increase quantity"
              title={
                isMaxStockReached
                  ? `Cannot add more. Available stock: ${item.product_stock}`
                  : "Increase quantity"
              }
            >
              +
            </button>
          </div>

          {/* Price Info */}
          <div className="text-right">
            <p className="text-xs text-slate-400 font-medium">
              ₹ {item.price_per_unit.toFixed(2)} / item
            </p>
            <p className="text-base font-extrabold text-slate-900">
              ₹ {item.total_price.toFixed(2)}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CartItems;
