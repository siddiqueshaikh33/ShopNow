import { useState, useEffect, useContext } from "react";
import Navbar from "../Components/Navbar";
import { UserContext } from "../Context/UserProvider";
import CartItems from "../Components/CartItems";
import { toast } from "react-toastify";
import axiosInstance from "../api/axiosInstance";
import { SpinnerDotted } from "spinners-react";
import { useNavigate } from "react-router-dom";
import Footer from "../Components/Footer";
import BackNav from "../Components/BackNav";

function Cart() {
  const { user } = useContext(UserContext);

  const [cartItems, setCartItems] = useState([]);
  const [overAllTotal, setOverallTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [click, setClick] = useState(false);
  const navigate = useNavigate();

  const updateClickCart = (productId, quantity) => {
    setCartItems((prevItems) =>
      prevItems.map((item) =>
        item.product_id === productId
          ? {
              ...item,
              quantity: quantity,
              total_price: item.price_per_unit * quantity,
            }
          : item
      )
    );
  };

  const deleteClickCart = (productId) => {
    setCartItems((prevItems) =>
      prevItems.filter((item) => item.product_id != productId)
    );
  };

  const fetchCartItems = async () => {
    try {
      setLoading(true);
      const response = await axiosInstance.get("/cart/getitems", {
        withCredentials: true,
      });

      setCartItems(response.data.cart_items);
    } catch (error) {
      toast.error(error.response?.data?.error || "Failed to fetch cart items");
    } finally {
      setLoading(false);
    }
  };

  // call once when user changes
  useEffect(() => {
    if (user) {
      fetchCartItems();
    }
  }, [user]);

  useEffect(() => {
    const total = cartItems.reduce((sum, item) => sum + item.total_price, 0);
    setOverallTotal(total);
  }, [cartItems]);

  const handlePayment = async () => {
    if (click) return; // extra safety guard

    // Pre-checkout stock boundary verification
    const exceedsStockItem = cartItems.find(
      (item) => item.product_stock != null && item.quantity > item.product_stock
    );
    if (exceedsStockItem) {
      toast.error(
        `Quantity for "${exceedsStockItem.product_name}" exceeds available stock (${exceedsStockItem.product_stock} available). Please adjust your cart.`
      );
      return;
    }

    const outOfStockItem = cartItems.find(
      (item) => item.product_stock != null && item.product_stock <= 0
    );
    if (outOfStockItem) {
      toast.error(
        `"${outOfStockItem.product_name}" is currently out of stock. Please remove it from your cart.`
      );
      return;
    }

    setClick(true);
    await openRazorpay();
  };

  const openRazorpay = async () => {
    try {
      const orderResponse = await axiosInstance.post(
        "/payment/create",
        {
          items: cartItems.map((item) => ({
            productId: item.product_id,
            quantity: item.quantity,
          })),
        },
        { withCredentials: true }
      );

      const razorpayOrderId = orderResponse.data;

      const options = {
        key: import.meta.env.VITE_RAZORPAY_KEY_ID,
        amount: Math.round(overAllTotal * 100),
        currency: "INR",
        order_id: razorpayOrderId,

        handler: function (response) {
          sendPaymentDetailsToServer(response);
        },

        modal: {
          ondismiss: async function () {
            try {
              await axiosInstance.post(
                "/payment/cancel",
                { orderId: razorpayOrderId },
                { withCredentials: true }
              );
              toast.info("Payment cancelled. Reserved items have been released.");
            } catch (err) {
              console.error("Failed to cancel order on modal dismiss:", err);
            }
          },
        },

        prefill: {
          name: user.username,
          email: user.email,
        },

        theme: {
          color: "#2563eb",
        },
      };

      const rzp = new window.Razorpay(options);
      rzp.open();
      setClick(false);
    } catch (error) {
      const errorMessage =
        error.response?.data?.message ||
        (typeof error.response?.data === "string"
          ? error.response.data
          : "Failed to initiate payment");
      toast.error(errorMessage);
      setClick(false);
    }
  };

  const sendPaymentDetailsToServer = async (paymentResponse) => {
    try {
      const verifyResponse = await axiosInstance.post(
        "/payment/verify",
        {
          razorpayPaymentId: paymentResponse.razorpay_payment_id,
          razorpayOrderId: paymentResponse.razorpay_order_id,
          razorpaySignature: paymentResponse.razorpay_signature,
        },
        { withCredentials: true }
      );
      console.log("Payment verified successfully");
      toast.success("Payment successful!");
      if (verifyResponse.status === 200) {
        // Payment verified on server, you can perform post-payment actions here
        console.log("Order completed:", verifyResponse.data);
        navigate("/vieworder");
      }
      // Optionally, you can clear the cart or redirect the user
      setCartItems([]);
    } catch {
      toast.error("Failed to verify payment");
    } finally {
      setClick(false);
    }
  };
  return (
    <div className="min-h-screen bg-[#F8FAFC]">
      <Navbar user={user ? user.username : "Guest"} />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-6 sm:py-8">
        <BackNav to="/dashboard" label="Back to Catalog" className="mb-4" />

        <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-slate-900 tracking-tight mb-6 sm:mb-8">
          Shopping Cart
        </h1>

        <div className="flex flex-col lg:flex-row gap-8">
          <div className="flex-1">
            {loading ? (
              <div className="h-64 flex items-center justify-center">
                <SpinnerDotted
                  size={60}
                  thickness={100}
                  speed={50}
                  color="#ADEFD1"
                />
              </div>
            ) : cartItems.length > 0 ? (
              <div className="space-y-4 mr-0 lg:mr-10">
                {cartItems.map((item) => (
                  <CartItems
                    key={item.product_id}
                    item={item}
                    updateCart={updateClickCart}
                    deleteCart={deleteClickCart}
                  />
                ))}
              </div>
            ) : (
              <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm p-12 text-center max-w-md mx-auto my-12">
                <h3 className="text-lg font-bold text-slate-900 mb-2">Your cart is empty</h3>
                <p className="text-xs text-slate-500 mb-6">
                  You haven't added any products to your cart yet.
                </p>
                <button
                  type="button"
                  onClick={() => navigate("/dashboard")}
                  className="px-5 py-2.5 bg-slate-900 hover:bg-[#062828] text-white text-xs font-semibold rounded-xl transition shadow-xs cursor-pointer"
                  style={{ borderBottom: "2px solid #ADEFD1" }}
                >
                  Browse Products
                </button>
              </div>
            )}
          </div>

          {/* RIGHT */}
          {!loading && cartItems.length > 0 && (
            <div className="w-full lg:w-1/3">
              <div className="bg-white rounded-2xl shadow-sm border border-slate-200/90 p-6 lg:sticky lg:top-24">
                <h2 className="text-xl font-bold text-slate-900 tracking-tight mb-6">
                  Order Summary
                </h2>

                <div className="space-y-4 text-sm text-slate-600">
                  <div className="flex justify-between">
                    <span>Total Items</span>
                    <span className="font-semibold text-slate-900">
                      {cartItems.reduce((sum, item) => sum + item.quantity, 0)}
                    </span>
                  </div>

                  <div className="flex justify-between">
                    <span>Subtotal</span>
                    <span className="font-semibold text-slate-900">₹ {overAllTotal.toFixed(2)}</span>
                  </div>

                  <div className="flex justify-between">
                    <span>Delivery</span>
                    <span className="text-emerald-700 font-bold text-xs uppercase bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                      FREE
                    </span>
                  </div>
                </div>

                <div className="border-t border-slate-100 my-5"></div>

                <div className="flex justify-between font-extrabold text-slate-900 text-lg">
                  <span>Total Amount</span>
                  <span>₹ {overAllTotal.toFixed(2)}</span>
                </div>

                <button
                  type="button"
                  className="w-full mt-6 bg-slate-900 hover:bg-[#062828] text-white py-3 rounded-xl font-bold text-sm transition-all duration-200 shadow-sm active:scale-[0.98] cursor-pointer disabled:opacity-50"
                  style={{ borderBottom: "2px solid #ADEFD1" }}
                  disabled={click}
                  onClick={handlePayment}
                >
                  {click ? "Processing..." : "Proceed to Pay"}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
      <footer>
        <Footer />
      </footer>
    </div>
  );
}

export default Cart;
