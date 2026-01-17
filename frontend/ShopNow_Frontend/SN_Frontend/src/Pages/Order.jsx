import { useEffect, useState } from 'react';
import axiosInstance from '../api/axiosInstance.js';
import Navbar from '../Components/Navbar.jsx';

function Order() {
  const [data, setData] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      console.log("Fetching order data...");
      try {
        const response = await axiosInstance.get('/order/list', {
          withCredentials: true
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

  return (
    <>
      <Navbar />

      <div className="flex justify-around items-start mt-10 px-10 gap-10">

        {/* LEFT SIDE – THANK YOU MESSAGE */}
        <div className="w-1/2">
          <h1 className="text-2xl font-semibold text-green-600 mb-3">
            🎉 Thank You for Your Order!
          </h1>

          <p className="text-gray-600">
            Your order has been placed successfully.
            We’ll notify you once your items are shipped.
          </p>
        </div>

        {/* RIGHT SIDE – ORDER ITEM CARDS */}
        <div className="w-1/2">
          <h2 className="text-xl font-medium mb-4">
            🛒 Order Items
          </h2>

          {data.length > 0 ? (
            <div className="grid grid-cols-1 gap-4 ">
              {data.map((item) => (
                <div
                  key={item.product_id}
                  className="border rounded-lg p-4 shadow-md hover:shadow-lg transition flex gap-4 items-center bg-white"
                >
                  {/* PRODUCT IMAGE */}
                  <img
                    src={item.product_img}
                    alt={item.name}
                    className="w-24 h-24 object-cover rounded-md m-6"
                  />

                  {/* PRODUCT DETAILS */}
                  <div className="flex-1 ml-2">
                    <h3 className="text-lg font-semibold">
                      {item.product_name}
                    </h3>

                    <p className="text-sm text-gray-600">
                      {item.description}
                    </p>

                    <p className="text-sm text-gray-600 mt-1">
                      Price: ₹{item.price_per_unit}
                    </p>

                    <p className="text-sm text-gray-600">
                      Quantity: {item.quantity}
                    </p>

                    <p className="font-medium mt-2 text-green-700">
                      Total: ₹{item.total_price}
                    </p>

                    <p className="text-xs text-gray-400 mt-1">
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
    </>
  );
}

export default Order;

