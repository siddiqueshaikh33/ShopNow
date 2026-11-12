import Navbar from "../Components/Navbar";
import Card from "../Components/Card";
import { UserContext } from "../Context/UserProvider";
import { useContext, useEffect, useRef, useState } from "react";
import axiosInstance from "../api/axiosInstance";
import { toast } from "react-toastify";
import CardSkeleton from "../Components/CardSkeleton";
import Shirt from "../assets/shirt.jpg";
import Pants from "../assets/pants.jpg";
import Phones from "../assets/Phones.jpg";
import Accessories from "../assets/accesiores.jpg";
import MobileAccessories from "../assets/Mobile_Accessiores.jpg";
import Footer from "../Components/Footer";
import logo from "../assets/logo.jpg";

function Dashboard() {
  const [products, setProducts] = useState([]);
  const { user } = useContext(UserContext);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState(null);
  const [categoryName, setCategoryName] = useState("");
  const [count, setCount] = useState(0);
  const sentinelRef = useRef(null);

  // Fetch products whenever page or categoryName changes
  useEffect(() => {
    let isMounted = true;
    const fetchProducts = async () => {
      if (page === 0) {
        setLoading(true);
        setError(null);
      } else {
        setLoadingMore(true);
        setError(null);
      }

      try {
        const response = await axiosInstance.get("/products", {
          params: {
            categoryName: categoryName || undefined,
            page: page,
            size: 12,
          },
          withCredentials: true,
        });

        if (isMounted && response.status === 200) {
          const newProducts = response.data.products || response.data.product || [];
          setProducts((prev) => (page === 0 ? newProducts : [...prev, ...newProducts]));
          setHasMore(response.data.hasMore ?? false);
          setError(null);
        }
      } catch (err) {
        if (isMounted) {
          const errorMsg =
            err.response?.data?.error ||
            err.response?.data?.message ||
            "Failed to fetch products";
          toast.error(errorMsg);
          setError(errorMsg);

          // Roll back page number so it does not drift forward on failure
          if (page > 0) {
            setPage((prev) => Math.max(0, prev - 1));
          }
        }
      } finally {
        if (isMounted) {
          setLoading(false);
          setLoadingMore(false);
        }
      }
    };

    fetchProducts();

    return () => {
      isMounted = false;
    };
  }, [categoryName, page]);

  // IntersectionObserver to auto-fetch next page when scrolling near bottom
  useEffect(() => {
    // Early exit if loading, loading more, reached catalog end, or an error occurred
    if (loading || loadingMore || !hasMore || error) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !loadingMore && !error) {
          setPage((prevPage) => prevPage + 1);
        }
      },
      {
        root: null,
        rootMargin: "200px",
        threshold: 0,
      }
    );

    const currentSentinel = sentinelRef.current;
    if (currentSentinel) {
      observer.observe(currentSentinel);
    }

    return () => {
      if (currentSentinel) {
        observer.unobserve(currentSentinel);
      }
    };
  }, [loading, loadingMore, hasMore, error]);

  const handleCategoryClick = (catName) => {
    if (categoryName === catName) {
      setCategoryName("");
    } else {
      setCategoryName(catName);
    }
    setProducts([]);
    setPage(0);
    setHasMore(true);
    setError(null);
  };

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
    console.log("Adding to cart:", product);
    const data = {
      username: user.username,
      product_id: product.product_id,
      quantity: 1,
    };
    try {
      const response = await axiosInstance.post("/cart/add", data, {
        withCredentials: true,
      });
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
      <Navbar count={count} logoSrc={logo} title="ShopNow!" bgColor="#ADEFD1" />

      {/* Category Banner */}
      <div className="p-4">
        {loading ? (
          <div className="rounded-2xl bg-gray-100 shadow-md animate-pulse p-4">
            <div className="h-10 w-3/4 bg-gray-200 mb-4 mx-auto rounded"></div>
            <div className="flex gap-4 overflow-x-auto justify-center">
              {[...Array(5)].map((_, i) => (
                <div
                  key={i}
                  className="h-24 w-24 bg-gray-200 rounded-full flex-shrink-0"
                ></div>
              ))}
            </div>
          </div>
        ) : (
          <div className="bg-white shadow-md rounded-xl p-4 mb-1 ">
            <h2 className="text-2xl font-bold mb-4 flex justify-center">
              Get A Great Deal
            </h2>
            <div className="flex gap-4 sm:gap-7 overflow-x-auto py-2 px-2 justify-start md:justify-center">
              {categories.map((cat) => (
                <button
                  key={cat.name}
                  className={`flex flex-col justify-center items-center flex-shrink-0 text-center hover:scale-105 transform transition duration-300 p-2 rounded-xl ${categoryName === cat.name ? "bg-blue-100 ring-2 ring-blue-500 shadow-sm" : ""
                    }`}
                  onClick={() => handleCategoryClick(cat.name)}
                >
                  <img
                    src={cat.img}
                    alt={cat.name}
                    className="h-20 w-20 sm:h-24 sm:w-24 rounded-full object-cover mb-1"
                  />
                  <span className="font-semibold text-sm sm:text-base">{cat.name}</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-wrap justify-center gap-4 sm:gap-6 p-2 sm:p-4 w-auto">
        {loading
          ? [...Array(12)].map((_, i) => <CardSkeleton key={i} />)
          : products.map((product) => (
            <Card
              key={product.product_id}
              product={product}
              onAction={handleCardAction}
            />
          ))}
      </div>

      {/* Invisible Sentinel for Infinite Scroll IntersectionObserver */}
      <div ref={sentinelRef} className="h-4 w-full" />

      {/* Loading More Spinner */}
      {loadingMore && (
        <div className="flex justify-center items-center py-6 w-full">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <span className="ml-3 text-gray-600 font-medium text-sm">
            Loading more products...
          </span>
        </div>
      )}

      {/* Error / Retry Banner for Infinite Scroll */}
      {error && !loading && (
        <div className="flex flex-col items-center justify-center py-6 w-full">
          <p className="text-red-500 text-sm font-medium mb-3">{error}</p>
          <button
            onClick={() => {
              setError(null);
              setPage((prev) => (prev === 0 ? 0 : prev + 1));
            }}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium rounded-lg transition"
          >
            Retry Loading Products
          </button>
        </div>
      )}

      {/* End of Catalog Banner */}
      {!loading && !hasMore && products.length > 0 && (
        <div className="text-center py-8 text-gray-500 font-medium text-sm w-full">
          You have reached the end of the catalog.
        </div>
      )}

      {/* Empty State */}
      {!loading && products.length === 0 && (
        <div className="text-center py-16 text-gray-500 font-medium text-lg w-full">
          No products found in this category.
        </div>
      )}

      <footer className="mt-20">
        <Footer />
      </footer>
    </div>
  );
}

export default Dashboard;
