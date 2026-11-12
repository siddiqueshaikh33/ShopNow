import { useEffect, useState } from "react";
import axiosInstance from "../api/axiosInstance.js";
import Navbar from "../Components/Navbar.jsx";
import { useNavigate } from "react-router-dom";
import Footer from "../Components/Footer.jsx";
import BackNav from "../Components/BackNav.jsx";

function Order() {
  const [data, setData] = useState([]);

  const navigate = useNavigate();

  useEffect(() => {
    const fetchData = async () => {
      console.log("Fetching order data...");
      try {
        const response = await axiosInstance.get("/order/list", {
          withCredentials: true,
        });

        // SAFE ACCESS
        const products = response?.data?.orders?.products || [];
        setData(products);

        console.log("Order products:", products);
      } catch (error) {
        console.error("Error fetching order data:", error);
      }
    };

    fetchData();
  }, []);

  const handleNavigate = () => {
    navigate("/dashboard");
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC]">
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-6 sm:py-8">
        <BackNav label="Back to Catalog" to="/dashboard" className="mb-4" />

        <div className="flex flex-col lg:flex-row justify-between items-start mt-2 gap-8 lg:gap-12">
          {/* LEFT SIDE – ORDER CONFIRMATION SUMMARY */}
          <div className="w-full lg:w-5/12 lg:sticky lg:top-28 self-start">
            <div className="bg-white border border-slate-200/90 rounded-3xl p-6 sm:p-8 shadow-sm">
              <div
                className="w-12 h-12 rounded-2xl flex items-center justify-center font-bold text-lg mb-4 text-slate-900 shadow-2xs"
                style={{ backgroundColor: "#ADEFD1" }}
              >
                ✓
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
                Order Confirmed!
              </h1>
              <p className="text-slate-600 text-sm mt-2 leading-relaxed">
                Your order has been placed successfully. Your purchase is recorded and your items are being prepared for dispatch.
              </p>

              <button
                type="button"
                className="mt-6 w-full py-3.5 px-6 rounded-xl bg-slate-900 hover:bg-[#062828] text-white font-bold text-sm transition-all duration-200 shadow-sm active:scale-[0.98] cursor-pointer"
                style={{ borderBottom: "2px solid #ADEFD1" }}
                onClick={handleNavigate}
              >
                Continue Shopping
              </button>
            </div>
          </div>

          {/* RIGHT SIDE – ORDER ITEM CARDS */}
          <div className="w-full lg:w-7/12 h-auto">
            <h2 className="text-xl font-bold text-slate-900 tracking-tight mb-4">
              Ordered Items
            </h2>

            {data.length > 0 ? (
              <div className="space-y-4 mb-4">
                {data.map((item) => (
                  <div
                    key={`${item.order_id}-${item.product_id}`}
                    className="flex flex-col sm:flex-row gap-4 items-start sm:items-center p-4 sm:p-5 rounded-2xl bg-white border border-slate-200/90 shadow-sm hover:shadow-md transition-all duration-200"
                  >
                    {/* PRODUCT IMAGE */}
                    <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-xl bg-slate-50 border border-slate-100 p-2 flex items-center justify-center flex-shrink-0 mx-auto sm:mx-0">
                      <img
                        src={item.product_img}
                        alt={item.product_name || "Product"}
                        className="w-full h-full object-contain"
                        onError={(e) => (e.currentTarget.style.display = "none")}
                      />
                    </div>

                    {/* PRODUCT DETAILS */}
                    <div className="flex-1 w-full flex flex-col justify-between">
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1">
                        <h3 className="text-base font-bold text-slate-900 tracking-tight">
                          {item.product_name}
                        </h3>
                        <span className="self-start sm:self-auto text-[10px] font-mono font-semibold px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 border border-slate-200">
                          ID: {item.order_id}
                        </span>
                      </div>

                      {item.description && (
                        <p className="text-xs text-slate-500 line-clamp-1 mt-0.5">
                          {item.description}
                        </p>
                      )}

                      <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between flex-wrap gap-2">
                        <div className="flex items-center gap-2.5 text-xs text-slate-500 font-medium">
                          <span>₹ {Number(item.price_per_unit || 0).toFixed(2)} × {item.quantity}</span>
                          <span className="text-emerald-700 font-semibold text-[11px] bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                            Confirmed
                          </span>
                        </div>

                        <div className="text-right">
                          <span className="text-[10px] uppercase tracking-wider text-slate-400 font-medium block">
                            Total
                          </span>
                          <span className="text-base font-extrabold text-slate-900">
                            ₹ {Number(item.total_price || 0).toFixed(2)}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="bg-white border border-slate-200/90 rounded-2xl p-8 text-center">
                <p className="text-slate-500 text-sm font-medium">No order items found.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      <footer className="mt-20">
        <Footer />
      </footer>
    </div>
  );
}

export default Order;
