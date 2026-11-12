import { useState, useEffect, useContext } from "react";
import Navbar from "../Components/Navbar";
import { UserContext } from "../Context/UserProvider";
import CartItems from "../Components/CartItems";
import { toast } from "react-toastify";
import axiosInstance from "../api/axiosInstance";
import { SpinnerDotted } from "spinners-react";
import { useNavigate } from "react-router-dom";
import Footer from "../Components/Footer";

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
        error.response?.data || "Failed to initiate payment";
      toast.error(
        typeof errorMessage === "string"
          ? errorMessage
          : "Failed to initiate payment"
      );
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
    <div className="min-h-screen bg-gray-100">
      <Navbar user={user ? user.username : "Guest"} />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-6 sm:py-8">
        <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-6 sm:mb-7">Shopping Cart</h1>

        <div className="flex flex-col lg:flex-row gap-8">
          <div className="flex-1">
            {loading ? (
              <div className="h-screen flex items-center justify-center">
                <SpinnerDotted
                  size={60}
                  thickness={100}
                  speed={50}
                  color="rgb(95, 249, 89)"
                />
              </div>
            ) : cartItems.length > 0 ? (
              <div className="space-y-4 mr-0 lg:mr-10">
                {cartItems.map((item) => (
                  <CartItems
                    key={item.product_id}
                    item={item}
                    updateCart={updateClickCart}
                    deleteCart={deleteClickCart} // WORKS
                  />
                ))}
              </div>
            ) : (
              <p className="text-gray-600 text-2xl sm:text-4xl flex font-semibold justify-center items-center h-57 text-center px-4">
                Your cart is empty.
              </p>
            )}
          </div>

          {/* RIGHT */}
          {!loading && cartItems.length > 0 && (
            <div className="w-full lg:w-1/3">
              <div className="bg-white rounded-xl shadow-sm border p-4 sm:p-6 lg:sticky lg:top-24">
                <h2 className="text-2xl font-semibold mb-6">Order Summary</h2>

                <div className="space-y-4 text-gray-700">
                  <div className="flex justify-between">
                    <span>Total Items</span>
                    <span className="font-medium">
                      {cartItems.reduce((sum, item) => sum + item.quantity, 0)}
                    </span>
                  </div>

                  <div className="flex justify-between">
                    <span>Subtotal</span>
                    <span>₹ {overAllTotal.toFixed(2)}</span>
                  </div>

                  <div className="flex justify-between">
                    <span>Delivery</span>
                    <span className="text-green-600 font-medium">FREE</span>
                  </div>
                </div>

                <div className="border-t my-5"></div>

                <div className="flex justify-between font-bold text-lg">
                  <span>Total Amount</span>
                  <span>₹ {overAllTotal.toFixed(2)}</span>
                </div>

                <button
                  className="w-full mt-6 bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700"
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
