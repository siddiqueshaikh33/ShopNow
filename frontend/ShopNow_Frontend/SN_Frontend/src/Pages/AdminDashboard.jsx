import React, { useState } from "react";
import Navbar from "../Components/Navbar";
import DashboardCard from "../Components/AdminDashboardCard";
import PopupModal from "../Components/PopupModal";
import { toast } from "react-toastify";
import axios from "axios";
import Footer from "../Components/Footer";

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
          buttonText="View Orders"
          title="Manage Customer Orders"
          subtitle="Activity : Order Management"
          onClick={() => setActivePopup("VIEW_ORDERS")}
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
          buttonText="Update Stock"
          title="Update Inventory Stock"
          subtitle="Activity : Inventory Management"
          onClick={() => setActivePopup("UPDATE_STOCK")}
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
        <PopupModal title="Add Product" onClose={closePopup}>
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
        <PopupModal title="Delete Product" onClose={closePopup}>
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

      {/* Popup 3: View Orders */}
      {activePopup === "VIEW_ORDERS" && (
        <PopupModal title="Orders" onClose={closePopup}>
          <p className="text-gray-600">Orders table/list here...</p>
        </PopupModal>
      )}

      <div>
      <Footer />
      </div>
    </div>
  );
}

export default AdminDashboard;
