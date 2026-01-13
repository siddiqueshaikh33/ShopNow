import Navbar from "../Components/Navbar";
import Card from "../Components/Card";
import { UserContext } from "../Context/UserProvider";
import { useContext, useEffect, useState } from "react";
import axiosInstance from "../api/axiosInstance";
import { toast } from "react-toastify";
import CardSkeleton from "../Components/Cardskeleton";
import Shirt from "../Assets/shirt.jpg";
import Pants from "../Assets/pants.jpg";
import Phones from "../Assets/phones.jpg";
import accesiores from "../Assets/accesiores.jpg";
import MobileAccessiores from "../Assets/Mobile_Accessiores.jpg";

function Dashboard() {
  const [products, setProducts] = useState([]);
  const {user} = useContext(UserContext);
  const [loading, setLoading] = useState(true);
  const [categoryName, setCategoryName] = useState("");
  const [count, setCount] = useState(0);

  useEffect(() => {
    setLoading(true);
    const fetchData = async () => {
      try {
        const response = await axiosInstance.get("/products", {
          params: { categoryName: categoryName },
          withCredentials: true,
        });
        if (response.status === 200) {
          setProducts(response.data.product);
          console.log("Fetched data:", response.data);
        }
      } catch (error) {
        console.error("Error fetching data:", error);
        toast.error(error.response?.data?.error || "Failed to fetch products");
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [categoryName]);

  useEffect(() => {
    // Placeholder for future cart functionality
    if (!user) return;
    const fetchCartCount = async () => {
      // Logic to fetch cart item count can be added here
      try {
        console.log("Fetching cart count for user:", user);
        const response = await axiosInstance.get("/cart/count", {
          params: { username: user.username },
          withCredentials: true,
        });
        if (response.status === 200) {
          console.log("Cart count fetched:", response.data);
          setCount(response.data.count);
        }
      } catch (error) {
        console.error("Error fetching cart count:", error);
      }
    };
    fetchCartCount();
  }, [user]);

  const handleCardAction = async (product) => {
    console.log("Product added to cart:", product);
    const data = {
      username: user ? user.username : "Guest",
      product_id: product.product_id,
      quantity: 1,
    };
    try {
      const response = await axiosInstance.post("/cart/add", data, {
        withCredentials: true,
      });
      console.log(product.product_id);
      setCount(response.data.count);
      console.log("Add to cart response:", response.data.count);
      toast.success("Product added to cart successfully!");
    } catch (error) {
      console.error("Error adding to cart:", error);
      toast.error(error.response?.data?.error || "Failed to add to cart");
    }
  };

  return (
    <div className="flex flex-col ">
      <Navbar
        user={user ? user.username : "Guest"}
        setCount={setCount}
        count={count}
      />
      <div className=" flex flex-1 p-4 flex-col bg-slate-100">
        {loading ? (
          <div className="h-56 ml-44 mr-44 rounded-2xl bg-gray-100 shadow-md overflow-hidden animate-pulse">
            <div className="bg-gray-200 h-10 w-50 m-4"></div>
            <div className="flex pl-4 gap-20 justify-around overflow-x-auto">
              {[...Array(5)].map((_, index) => (
                <div
                  key={index}
                  className="bg-gray-200 h-27 w-27 rounded-full object-cover mb-1"
                ></div>
              ))}
            </div>
          </div>
        ) : (
          <div className="h-56 ml-44 mr-44 rounded-xl bg-white shadow-sm border-1 border-black hover:shadow-lg transition flex flex-col ">
            <h2 className="text-2xl font-bold p-5">Get A Great Deal</h2>
            <div className="flex pl-4 gap-20 justify-around overflow-x-auto">
              <button
                className="group text-md font-semibold flex items-center flex-col"
                value={"Shirts"}
                onClick={(e) => {
                  e.stopPropagation();
                  setCategoryName("Shirts");
                }}
              >
                <img
                  src={Shirt}
                  alt="Shirt"
                  className="h-27 w-27 rounded-full object-cover transition-transform duration-300 group-hover:scale-105 mb-1"
                />
                Shirts
              </button>

              <button
                className="group text-md font-semibold flex items-center flex-col"
                value={"Pants"}
                onClick={(e) => {
                  e.stopPropagation();
                  setCategoryName("Pants");
                }}
              >
                <img
                  src={Pants}
                  alt="Pants"
                  className="h-27 w-27 rounded-full object-cover transition-transform duration-300 group-hover:scale-105 mb-1"
                />
                Pants
              </button>

              <button
                className="group text-md font-semibold flex items-center flex-col"
                value={"Mobiles"}
                onClick={(e) => {
                  e.stopPropagation();
                  setCategoryName("Mobiles");
                }}
              >
                <img
                  src={Phones}
                  alt="Phones"
                  className="h-27 w-27 rounded-full object-cover transition-transform duration-300 group-hover:scale-105 mb-1"
                />
                Mobiles
              </button>

              <button
                className="group text-md font-semibold flex items-center flex-col"
                value={"Accessories"}
                onClick={(e) => {
                  e.stopPropagation();
                  setCategoryName("Accessories");
                }}
              >
                <img
                  src={accesiores}
                  alt="Accessories"
                  className="h-27 w-27 rounded-full object-cover transition-transform duration-300 group-hover:scale-105 mb-1"
                />
                Accessories
              </button>

              <button
                className="group text-md font-semibold flex items-center flex-col"
                value={"Mobile Accessories"}
                onClick={(e) => {
                  e.stopPropagation();
                  setCategoryName("Mobile Accessories");
                }}
              >
                <img
                  src={MobileAccessiores}
                  alt="Mobile Accessories"
                  className="h-27 w-27 rounded-full object-cover transition-transform duration-300 group-hover:scale-105 mb-1"
                />
                Mobile Accessories
              </button>
            </div>
          </div>
        )}
        <div className="flex flex-row m-6 gap-4 flex-wrap  justify-center">
          {loading
            ? [...Array(8)].map((_, index) => <CardSkeleton key={index} />)
            : products.map((product) => (
                <Card
                  key={product.product_id}
                  product={product}
                  onAction={handleCardAction}
                />
              ))}
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
