import { useEffect, useState } from "react";
import axiosInstance from "../api/axiosInstance.js";
import Navbar from "../Components/Navbar.jsx";
import { useNavigate } from "react-router-dom";
import Footer from "../Components/Footer.jsx";

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
    <div className="min-h-screen bg-gray-100">
      <Navbar />

      <div className="flex justify-around items-start mt-10 px-10 gap-10">
        {/* LEFT SIDE – THANK YOU MESSAGE */}
        <div className="w-1/2 sticky top-36 self-start ">
          <button
            className="px-6 py-3 rounded-lg
             bg-green-600 text-white font-medium
             hover:bg-green-700
             transition"
            onClick={handleNavigate}
          >
            Continue Shopping
          </button>

          <h1 className="text-5xl font-semibold text-green-600 mt-10">
            🎉 Thank You for Your Order!
          </h1>

          <p className="text-gray-600 text-lg p-4 bg-green-50 border border-green-500 rounded-lg mt-6">
            Your order has been placed successfully. We’ll notify you once your
            items are shipped.
          </p>
        </div>

        {/* RIGHT SIDE – ORDER ITEM CARDS */}
        <div className="w-1/2  h-auto">
          <h2 className="text-2xl font-medium mb-4">🛒 Order Items</h2>

          {data.length > 0 ? (
            <div className="grid grid-cols-1 gap-4 mb-4">
              {data.map((item) => (
                <div
                  key={item.product_id}
                  className="flex gap-4 items-center p-5 rounded-xl
             bg-white border border-gray-100
             shadow-md hover:shadow-lg
             hover:-translate-y-0.5
             transition-all duration-300  border border-gray-200 "
                >
                  {/* PRODUCT IMAGE */}
                  <img
                    src={item.product_img}
                    alt={item.name}
                    className="w-30 h-28 object-cover rounded-md mx-8"
                  />

                  {/* PRODUCT DETAILS */}
                  <div className="flex-1 ml-2">
                    <h3 className="text-xl font-semibold">
                      {item.product_name}
                    </h3>

                    <p className="text-md text-gray-600 ">{item.description}</p>

                    <p className="text-md text-gray-600 mt-1">
                      Price: ₹{item.price_per_unit}
                    </p>

                    <p className="text-md text-gray-600 ">
                      Quantity: {item.quantity}
                    </p>

                    <p className="font-medium mt-2 text-green-700">
                      Total: ₹{item.total_price}
                    </p>

                    <p className="font-medium text-blue-700 ">
                      Order ID: {item.order_id}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-500">No order items found.</p>
          )}
        </div>
      </div>
      <footer  className="mt-20">
        <Footer />
      </footer>
    </div>
  );
}

export default Order;
