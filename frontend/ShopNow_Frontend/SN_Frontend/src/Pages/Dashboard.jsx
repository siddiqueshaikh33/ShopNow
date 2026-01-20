import Navbar from "../Components/Navbar";
import Card from "../Components/Card";
import { UserContext } from "../Context/UserProvider";
import { useContext, useEffect, useState } from "react";
import axiosInstance from "../api/axiosInstance";
import { toast } from "react-toastify";
import CardSkeleton from "../Components/CardSkeleton";
import Shirt from "../Assets/shirt.jpg";
import Pants from "../Assets/pants.jpg";
import Phones from "../Assets/phones.jpg";
import Accessories from "../Assets/accesiores.jpg";
import MobileAccessories from "../Assets/Mobile_Accessiores.jpg";
import Footer from "../Components/Footer";

function Dashboard() {
  const [products, setProducts] = useState([]);
  const { user } = useContext(UserContext);
  const [loading, setLoading] = useState(true);
  const [categoryName, setCategoryName] = useState("");
  const [count, setCount] = useState(0);

  useEffect(() => {
    setLoading(true);
    const fetchData = async () => {
      try {
        const response = await axiosInstance.get("/products", {
          params: { categoryName },
          withCredentials: true,
        });
        if (response.status === 200) setProducts(response.data.product);
      } catch (error) {
        toast.error(error.response?.data?.error || "Failed to fetch products");
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [categoryName]);

  useEffect(() => {
    if (!user?.username) return;
    const fetchCartCount = async () => {
      try {
        const response = await axiosInstance.get("/cart/count", {
          params: { username: user.username },
          withCredentials: true,
        });
        if (response.status === 200) setCount(response.data.count);
      } catch (error) {
        console.error("errorrrrrrrr:", error);
      }
    };
    fetchCartCount();
  }, [user?.username]);

  const handleCardAction = async (product) => {
    if (!user?.username) return;
  
    const data = { username: user.username, product_id: product.product_id, quantity: 1 };
    try {
      const response = await axiosInstance.post("/cart/add", data, { withCredentials: true });
      setCount(response.data.count);
      toast.success("Product added to cart successfully!");
    } catch (error) {
      toast.error(error.response?.data?.error || "Failed to add to cart");
    }
  };

  const categories = [
    { name: "Shirts", img: Shirt },
    { name: "Pants", img: Pants },
    { name: "Mobiles", img: Phones },
    { name: "Accessories", img: Accessories },
    { name: "Mobile Accessories", img: MobileAccessories },
  ];

  return (
    <div className="flex flex-col min-h-screen bg-slate-100">
      <Navbar count={count} setCount={setCount} />

      {/* Category Banner */}
      <div className="p-4">
        {loading ? (
          <div className="rounded-2xl bg-gray-100 shadow-md animate-pulse p-4">
            <div className="h-10 w-3/4 bg-gray-200 mb-4 mx-auto rounded"></div>
            <div className="flex gap-4 overflow-x-auto justify-center">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="h-24 w-24 bg-gray-200 rounded-full flex-shrink-0"></div>
              ))}
            </div>
          </div>
        ) : (
          <div className="bg-white shadow-md rounded-xl p-4 mb-1 ">
            <h2 className="text-2xl font-bold mb-4 flex justify-center">Get A Great Deal</h2>
            <div className="flex gap-7 overflow-x-auto py-2 justify-center">
              {categories.map((cat) => (
                <button
                  key={cat.name}
                  className="flex flex-col justify-center items-center flex-shrink-0 text-center hover:scale-105 transform transition duration-300"
                  onClick={() => setCategoryName(cat.name)}
                >
                  <img src={cat.img} alt={cat.name} className="h-24 w-24 rounded-full object-cover mb-1" />
                  <span className="font-semibold">{cat.name}</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Products Grid */}
      <div className="flex flex-wrap justify-center gap-6 p-2 w-auto">
        {loading
          ? [...Array(15)].map((_, i) => <CardSkeleton key={i} />)
          : products.map((product) => <Card key={product.product_id} product={product} onAction={handleCardAction} />)}
      </div>
      <footer ><Footer /></footer>
    </div>
  );
}

export default Dashboard;

