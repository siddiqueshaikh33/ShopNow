import React, { useState } from "react";
import Navbar from "../Components/Navbar";
import DashboardCard from "../Components/AdminDashboardCard";
import PopupModal from "../Components/PopupModal";
import { toast } from "react-toastify";
import axios from "axios";
import Footer from "../Components/Footer";
import { PieChart, Pie, Tooltip, Legend, Cell } from "recharts";



function AdminDashboard() {
  const [activePopup, setActivePopup] = useState(null);

  // Add Product states
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [imageUrl, setImageUrl] = useState("");

  const closePopup = () => setActivePopup(null);

  const handleSubmit = async (e) => {
    e.preventDefault();

    const productData = {
      name,
      description,
      price: parseFloat(price),
      stock: parseInt(stock),
      categoryId: parseInt(categoryId),
      imageUrl,
    };

    try {
      const response = await axios.post(
        "http://localhost:8080/admin/addProduct",
        productData,
        { withCredentials: true }
      );

      console.log("Product added successfully:", response.data);
      toast.success("Product added successfully");
      closePopup();
    } catch (error) {
      console.error("Error adding product:", error);
      toast.error("Failed to add product");
    } finally {
      setName("");
      setDescription("");
      setPrice("");
      setStock("");
      setCategoryId("");
      setImageUrl("");
    }
  };


  //delete product function
  const [product, setProduct] = useState(null);
  const [productId, setProductId] = useState("");
  const fetchProduct = async (e) => {
     console.log("Fetching product with ID:", productId);
    e.preventDefault();
    try {
      const response = await axios.get(
        `http://localhost:8080/admin/product/${productId}`,
        { withCredentials: true }
      );

      setProduct(response.data);
      console.log("Product fetched successfully:", response.data);
      toast.success("Product fetched successfully");
    } catch (error) {
      console.error("Error fetching product:", error);
      toast.error("Failed to fetch product");
    } 
  };

  const handleFormDeletion = async (e) => {
    e.preventDefault();
    try {

      const response = await axios.delete(
        `http://localhost:8080/admin/deleteproduct/${productId}`,
        { withCredentials: true }
      );
      console.log("Product deleted successfully:", response.data);
      toast.success("Product deleted successfully");
      closePopup();
    }
    catch (error) {
      console.error("Error deleting product:", error);
      toast.error("Failed to delete product");
    } finally{
      setProductId("");
      setProduct(null);
    }
  };

  // Modify User Details states
  const [userId, setUserId] = useState("");
  const [Username, setUsername] = useState("");
  const [Email, setEmail] = useState("");
  const [Role, setRole] = useState("");
  const [AuthProvider, setAuthProvider] = useState("");


  const fetchUser = async (e) => {
    e.preventDefault();
    try{
    const response = await axios.get(`http://localhost:8080/admin/getDetails/${userId}`, { withCredentials: true });
     setUsername(response.data.username);
     setEmail(response.data.email);
     setRole(response.data.role);
     setAuthProvider(response.data.provider);
      console.log("User fetched successfully:", response.data);
      toast.success("User fetched successfully");
    } catch (error) {
      toast.error(error.response?.data || "Failed to fetch user or User not found");
      console.error("Error fetching user:", error);
    }
  };

 const handleUserForm = async (e) => {
  e.preventDefault();

  try {
    const updatedUserData = {
      username: Username,
      email: Email,
      role: Role,
    };

    const response = await axios.put(
      `http://localhost:8080/admin/updateUser/${userId}`,
      updatedUserData,
      { withCredentials: true }
    );

    console.log("User updated successfully:", response.data);
    toast.success("User updated successfully");
    closePopup();
  } catch (error) {
    console.error("Error updating user:", error);
    toast.error(error.response?.data || "Failed to update user");
  }
};

// Monthly Business states

const [month, setMonth] = useState("");
const [year, setYear] = useState("");
const [totalAmount, setTotalAmount] = useState(0);
const [categoryData, setCategoryData] = useState([]);
const [showMonthlyChart, setShowMonthlyChart] = useState(false);

const COLORS = ["#1D4ED8", "#16A34A", "#D97706", "#DC2626", "#0F766E"];


const handleMBusinessForm = async (e) => {
  e.preventDefault();

  try {
    const response = await axios.get("http://localhost:8080/admin/analytics/month", {
      params: { month: parseInt(month), year: parseInt(year) },
      withCredentials: true,
    });

    toast.success("Monthly business data fetched successfully");

    setCategoryData(response.data.category_sales_count);
    setTotalAmount(response.data.total_amount);

    setShowMonthlyChart(true); // ✅ show pie chart in popup (replace form)
  } catch (error) {
    console.error("Error fetching monthly business data:", error);
    toast.error("Failed to fetch monthly business data");
  }
};


  return (
    <div>
      <Navbar />
      <h1 className="font-bold text-3xl m-6 ml-10">Admin Dashboard</h1>
      <div className="admin-dashboard-container grid grid-cols-2 gap-4 m-10 flex flex-wrap justify-center">
        {/* 1) Add Product */}
        <DashboardCard
          buttonText="+ Add Product"
          title="Create and Manage new Products"
          subtitle="Activity : Product Management"
          onClick={() => setActivePopup("ADD_PRODUCT")}
        />

        {/* 2) Add Category */}
        <DashboardCard
          buttonText="- Delete Product"
          title="View and Delete the existing Products"
          subtitle="Activity : Product Management"
          onClick={() => setActivePopup("DELETE_PRODUCT")}
        />

        {/* 3) View Orders */}
        <DashboardCard
          buttonText="Modify User Details"
          title="View and Manage User"
          subtitle="Activity : User Management"
          onClick={() => setActivePopup("MODIFY_USER_DETAILS")}
        />

        {/* 4) Manage Users */}
        <DashboardCard
          buttonText="Manage Users"
          title="Manage All Users"
          subtitle="Activity : User Management"
          onClick={() => setActivePopup("MANAGE_USERS")}
        />

        {/* 5) Stock Update */}
        <DashboardCard
          buttonText="Monthly Business"
          title="Monthly Business Overview"
          subtitle="Activity : Analytics"
          onClick={() => setActivePopup("MONTHLY_BUSINESS")}
        />

        {/* 6) Reports */}
        <DashboardCard
          buttonText="Reports"
          title="Check Sales Reports"
          subtitle="Activity : Analytics"
          onClick={() => setActivePopup("REPORTS")}
        />
      </div>

      {/* Popup 1: Add Product */}
      {activePopup === "ADD_PRODUCT" && (
        <PopupModal title="Add Product" onClose={() => {
            closePopup(),
            setName(""),
            setDescription(""),
            setPrice(""),   
            setStock(""),
            setCategoryId(""),
            setImageUrl("")
          }}>
          <form onSubmit={handleSubmit} className="space-y-3">
            <input
              type="text"
              placeholder="Enter Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full border border-gray-300 px-3 py-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />

            <input
              type="text"
              placeholder="Enter Description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full border border-gray-300 px-3 py-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />

            <input
              type="number"
              placeholder="Enter Price"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              className="w-full border border-gray-300 px-3 py-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />

            <input
              type="number"
              placeholder="Enter Stock"
              value={stock}
              onChange={(e) => setStock(e.target.value)}
              className="w-full border border-gray-300 px-3 py-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />

            <input
              type="number"
              placeholder="Enter Category ID"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              className="w-full border border-gray-300 px-3 py-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              min="0"
            />

            <input
              type="text"
              placeholder="Enter Image URL"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              className="w-full border border-gray-300 px-3 py-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />

            <div className="flex gap-3 pt-2 justify-end">
              <button
                type="submit"
                className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 transition-colors duration-300 ease-in-out"
              >
                Submit
              </button>
            </div>
          </form>
        </PopupModal>
      )}

      {/* Popup 2: Delete Product */}
      {activePopup === "DELETE_PRODUCT" && (
        <PopupModal title="Delete Product" onClose={() => {
          closePopup(),
          setProductId(""),
          setProduct(null);
        }}>
           <form onSubmit={handleFormDeletion} className="space-y-3">

            <div className="flex gap-3 pt-2 justify-center">
            <input type="text" placeholder="Enter Product ID to Delete" value={productId} className="w-full border border-gray-300 px-3 py-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
             onChange={(e) => setProductId(e.target.value)} name="productId"/>
            <button type="button" onClick={(e) => fetchProduct(e)} className="bg-green-400 hover:bg-green-300 rounded px-4 py-2">Fetch</button>
            </div>

            {
              product && (<div className="mt-4 p-4 border-2 border-green-400 rounded gap-2">
                <h3 className="font-bold text-lg mb-2">Product Details</h3>
                <p className="bg-green-100 rounded p-2 mb-2 font-bold"><span className="font-semibold pr-2">Name:</span> {product.product_name}</p>
                <p className="bg-green-100 rounded p-2 mb-2 font-bold"><span className="font-semibold pr-2">Category:</span> {product.category}</p>
                </div>)
            }
            <div className="flex justify-center">
            <button type="submit"  className="bg-red-600 text-white px-4 py-2 mt-4  rounded hover:bg-red-700 transition-colors duration-300 ease-in-out ">Delete Product</button>
           </div>
           </form>
        </PopupModal>
      )}

      
      {activePopup === "MODIFY_USER_DETAILS" && (
        <PopupModal title="View and Manage User" onClose={() => {
          closePopup()
          setUserId(""),
          setUsername(""),
          setEmail(""),
          setRole(""),
          setAuthProvider("")
        }}>
          <div className="flex  items-center justify-center">
           <label htmlFor="userId">User Id:</label>
           <input type="text" name="userId" id="userId" className="border p-2 ml-3 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={userId} onChange={(e) => setUserId(e.target.value)} />
           <button className="bg-green-500 hover:bg-green-400 p-2 ml-4 rounded w-20 text-white" onClick={(e) => fetchUser(e)}>Fetch</button>
           </div>
          <p className="text-red-500 font-semibold mt-2">* Data fetched will appear here</p>
            <form className="mt-4 space-y-4 relative" onSubmit={handleUserForm}>
              <label htmlFor="username" className="absolute top-[-14px] left-2 bg-white px-1 font-semibold">Username </label>
              <input
              type="text"
              id="username"
              placeholder="Enter Username"
              value={Username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full border border-gray-300 px-3 py-3 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
             <label htmlFor="email" className="absolute top-13 left-2 bg-white px-1 font-semibold">Email </label>
            <input
              type="text"
              id="email"
              placeholder="Enter Email"
              value={Email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full border border-gray-300 px-3 py-3 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />

            <label htmlFor="role" className="absolute top-30 left-2 bg-white px-1 font-semibold">Role </label>
            <input
              type="text"
              id="role"
              placeholder="Enter Role"
              value={Role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full border border-gray-300 px-3 py-3 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />

            <label htmlFor="authProvider" className="absolute top-46 left-2 bg-white px-1 font-semibold">Auth Provider </label>
            <input
              type="text"
              id="authProvider"
              disabled={true}
              placeholder="Enter Auth Provider"
              value={AuthProvider}
              onChange={(e) => setAuthProvider(e.target.value)}
              className="w-full border border-gray-300 px-3 py-3  rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <div className="flex gap-3 pt-2 justify-end">
              <button
                type="submit"
                className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 transition-colors duration-300 ease-in-out"
              >
                Submit
              </button>
            </div>
            </form>
        </PopupModal>    
      )}

       {activePopup === "MONTHLY_BUSINESS" && (
  <PopupModal
    title="Monthly Business"
    onClose={() => {
      closePopup();
      setMonth("");
      setYear("");
      setTotalAmount(0);
      setCategoryData([]);
      setShowMonthlyChart(false);
    }}
  >
    {!showMonthlyChart ? (
      <form
        className="flex flex-col mt-4 space-y-4 relative"
        onSubmit={handleMBusinessForm}
      >
        <label
          htmlFor="month"
          className="absolute top-[-14px] left-2 bg-white px-1 font-semibold"
        >
          Month
        </label>

        <input
          type="number"
          id="month"
          placeholder="Enter Month"
          className="w-full border border-gray-300 px-3 py-3 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
          min={1}
          max={12}
          value={month}
          onChange={(e) => setMonth(e.target.value)}
        />

        <label
          htmlFor="year"
          className="absolute top-13 left-2 bg-white px-1 font-semibold"
        >
          Year
        </label>

        <input
          type="number"
          id="year"
          placeholder="Enter Year"
          className="w-full border border-gray-300 px-3 py-3 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
          min={2000}
          max={2100}
          value={year}
          onChange={(e) => setYear(e.target.value)}
        />

        <div className="flex gap-3 pt-2 justify-end">
          <button
            type="submit"
            className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 transition-colors duration-300 ease-in-out"
          >
            Submit
          </button>
        </div>
      </form>
    ) : (
      <div className="mt-4 flex flex-col items-center">
        <h2 className="text-lg font-bold mb-2">Category Sales Count</h2>

        <p className="font-semibold mb-4">
          Total Amount: ₹{totalAmount}
        </p>

        <PieChart width={400} height={300}>
          <Pie
            data={categoryData.map((item) => ({
              name: item.category,
              value: item.categoryCount,
            }))}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="50%"
            outerRadius={100}
            label
          >
            {categoryData.map((entry, index) => (
              <Cell
                key={`cell-${index}`}
                fill={COLORS[index % COLORS.length]}
              />
            ))}
          </Pie>

          <Tooltip />
          <Legend />
        </PieChart>
      </div>
    )}
  </PopupModal>
)}


      <div>
      <Footer />
      </div>
    </div>
  );
}

export default AdminDashboard;
