import Navbar from "../Components/Navbar";
import Card from "../Components/Card";
import { UserContext } from "../Context/UserProvider";
import { useContext, useEffect, useRef, useState } from "react";
import axiosInstance from "../api/axiosInstance";
import { toast } from "react-toastify";
import CardSkeleton from "../Components/CardSkeleton";
import { LayoutGrid } from "lucide-react";
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
  const isFetchingRef = useRef(false);

  // Fetch products whenever page or categoryName changes
  useEffect(() => {
    let isMounted = true;
    const fetchProducts = async () => {
      // Prevent duplicate in-flight requests
      if (isFetchingRef.current) return;
      isFetchingRef.current = true;

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
          setProducts((prev) => {
            if (page === 0) return newProducts;
            const existingIds = new Set(prev.map((item) => item.product_id));
            const uniqueIncoming = newProducts.filter(
              (item) => !existingIds.has(item.product_id)
            );
            return [...prev, ...uniqueIncoming];
          });
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
        isFetchingRef.current = false;
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
        if (
          entries[0].isIntersecting &&
          hasMore &&
          !isFetchingRef.current &&
          !loadingMore &&
          !error
        ) {
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
    isFetchingRef.current = false;
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
    if (!user?.username) {
      toast.info("Please sign in to add items to your cart.");
      return;
    }
    if (!product.stock || product.stock <= 0) {
      toast.warn("This item is currently out of stock.");
      return;
    }
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
      const errorMessage =
        error.response?.data?.message ||
        (typeof error.response?.data === "string"
          ? error.response.data
          : "Failed to add to cart");
      toast.error(errorMessage);
    }
  };

  const categories = [
    { name: "All Categories", isAll: true },
    { name: "Shirts", img: Shirt },
    { name: "Pants", img: Pants },
    { name: "Mobiles", img: Phones },
    { name: "Accessories", img: Accessories },
    { name: "Mobile Accessories", img: MobileAccessories },
  ];

  return (
    <div className="flex flex-col min-h-screen bg-[#F8FAFC]">
      <Navbar count={count} logoSrc={logo} title="ShopNow!" bgColor="#ADEFD1" />

      {/* Category Navigation Bar */}
      <div className="max-w-7xl mx-auto w-full px-4 sm:px-6 pt-6 pb-2">
        {loading && page === 0 ? (
          <div className="rounded-2xl bg-white border border-slate-200/80 shadow-sm animate-pulse p-6">
            <div className="h-6 w-48 bg-slate-200 mb-6 rounded"></div>
            <div className="flex gap-4 sm:gap-6 overflow-x-auto justify-start sm:justify-center">
              {[...Array(6)].map((_, i) => (
                <div
                  key={i}
                  className="flex flex-col items-center gap-2 flex-shrink-0"
                >
                  <div className="h-16 w-16 sm:h-20 sm:w-20 bg-slate-200 rounded-full" />
                  <div className="h-3 w-14 bg-slate-200 rounded" />
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="bg-white border border-slate-200/90 shadow-sm rounded-2xl p-5 sm:p-6 mb-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4 pb-3 border-b border-slate-100">
              <div>
                <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
                  Curated Collections
                </h2>
                <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
                  {categoryName
                    ? `Showing ${categoryName} catalog`
                    : "Select a department or browse all featured products"}
                </p>
              </div>

              {categoryName && (
                <button
                  type="button"
                  onClick={() => handleCategoryClick("")}
                  className="self-start sm:self-auto text-xs font-semibold px-3 py-1.5 rounded-lg text-slate-700 bg-slate-100 hover:bg-slate-200 transition cursor-pointer"
                >
                  Clear Category Filter
                </button>
              )}
            </div>

            <div className="flex gap-4 sm:gap-6 overflow-x-auto py-2 px-1 justify-start sm:justify-center">
              {categories.map((cat) => {
                const isActive = cat.isAll ? categoryName === "" : categoryName === cat.name;
                return (
                  <button
                    key={cat.name}
                    type="button"
                    className={`flex flex-col justify-center items-center flex-shrink-0 text-center transition-all duration-200 p-2.5 rounded-2xl cursor-pointer ${
                      isActive
                        ? "bg-[#ADEFD1]/35 ring-2 ring-slate-900 shadow-xs"
                        : "hover:bg-slate-50"
                    }`}
                    onClick={() => handleCategoryClick(cat.isAll ? "" : cat.name)}
                  >
                    {cat.isAll ? (
                      <div
                        className={`h-16 w-16 sm:h-20 sm:w-20 rounded-full flex items-center justify-center mb-2 transition-transform duration-200 p-0.5 border-2 ${
                          isActive
                            ? "border-slate-900 bg-slate-900 text-[#ADEFD1] scale-105"
                            : "border-slate-200 bg-slate-100 text-slate-700 hover:bg-slate-200"
                        }`}
                      >
                        <LayoutGrid className="w-7 h-7 sm:w-8 sm:h-8" />
                      </div>
                    ) : (
                      <img
                        src={cat.img}
                        alt={cat.name}
                        className={`h-16 w-16 sm:h-20 sm:w-20 rounded-full object-cover mb-2 transition-transform duration-200 p-0.5 border-2 ${
                          isActive
                            ? "border-slate-900 scale-105"
                            : "border-slate-200"
                        }`}
                      />
                    )}
                    <span
                      className={`text-xs sm:text-sm font-semibold tracking-tight ${
                        isActive ? "text-slate-950 font-bold" : "text-slate-700"
                      }`}
                    >
                      {cat.name}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* Catalog Grid */}
      <main className="max-w-7xl mx-auto w-full px-4 sm:px-6 py-4 flex-grow">
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6 w-full items-stretch">
          {loading && page === 0
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
        {!loading && products.length > 0 && hasMore && (
          <div ref={sentinelRef} className="h-4 w-full" />
        )}

        {/* Loading More Spinner */}
        {loadingMore && (
          <div className="flex justify-center items-center py-8 w-full">
            <div className="animate-spin rounded-full h-7 w-7 border-2 border-slate-300 border-t-slate-900" />
            <span className="ml-3 text-slate-600 font-medium text-xs">
              Loading more products...
            </span>
          </div>
        )}

        {/* Error / Retry Banner for Infinite Scroll */}
        {error && !loading && (
          <div className="max-w-md mx-auto my-8 p-6 bg-white border border-red-200 rounded-2xl shadow-sm text-center">
            <p className="text-red-600 text-xs font-semibold mb-3">{error}</p>
            <button
              type="button"
              onClick={() => {
                setError(null);
                setPage((prev) => (prev === 0 ? 0 : prev + 1));
              }}
              className="px-4 py-2 bg-slate-900 hover:bg-[#062828] text-white text-xs font-semibold rounded-xl transition shadow-xs cursor-pointer"
              style={{ borderBottom: "2px solid #ADEFD1" }}
            >
              Retry Loading Products
            </button>
          </div>
        )}

        {/* End of Catalog Banner */}
        {!loading && !hasMore && products.length > 0 && (
          <div className="text-center py-10 w-full">
            <span className="inline-block px-4 py-1.5 rounded-full bg-slate-200/60 text-slate-600 text-xs font-medium">
              You have reached the end of the catalog.
            </span>
          </div>
        )}

        {/* Empty State */}
        {!loading && products.length === 0 && (
          <div className="max-w-md mx-auto my-16 p-8 bg-white border border-slate-200/90 rounded-2xl shadow-sm text-center">
            <h3 className="text-base font-bold text-slate-900 mb-1">
              No products found
            </h3>
            <p className="text-xs text-slate-500 mb-5">
              No active listings are currently available in this department.
            </p>
            {categoryName && (
              <button
                type="button"
                onClick={() => handleCategoryClick("")}
                className="px-4 py-2 bg-slate-900 hover:bg-[#062828] text-white text-xs font-semibold rounded-xl transition shadow-xs cursor-pointer"
                style={{ borderBottom: "2px solid #ADEFD1" }}
              >
                View All Products
              </button>
            )}
          </div>
        )}
      </main>

      <footer className="mt-16">
        <Footer />
      </footer>
    </div>
  );
}

export default Dashboard;
