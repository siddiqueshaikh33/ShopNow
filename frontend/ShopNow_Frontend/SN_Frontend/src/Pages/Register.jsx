import { useRef, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Link } from "react-router-dom";
import Typed from "typed.js";
import axiosInstance from "../api/axiosInstance";
import { toast } from "react-toastify";
import { FaEye, FaEyeSlash } from "react-icons/fa";

function Register() {
  const el = useRef(null);
  const name = "ShopNow!";

  //For Typed.js effect
  useEffect(() => {
    const typed = new Typed(el.current, {
      strings: [
        `Track your orders\nManage your cart\n Get the best deals\nWith  <span class="bg-gradient-to-r from-sky-400 via-indigo-500 to-pink-500 bg-clip-text text-transparent font-extrabold">${name}</span>`,
      ],
      typeSpeed: 80,
      loop: true,
      cursorChar: "",
    });

    return () => typed.destroy();
  }, []);

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("CUSTOMER");
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const navigate = useNavigate();

  const validateForm = () => {
    if (!username.trim()) {
      toast.error("Username is required");
      return false;
    }

    if (username.isNumber) {
      toast.error("Username cannot be a number");
      return false;
    }

    if (/^\d+$/.test(username)) {
      toast.error("Username cannot be only numbers");
      return false;
    }

    if (username.length < 3 || username.length > 20) {
      toast.error("Username must be between 3 and 20 characters");
      return false;
    }

    if (!email.trim()) {
      toast.error("Email is required");
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      toast.error("Please enter a valid email address");
      return false;
    }

    if (!password) {
      toast.error("Password is required");
      return false;
    }

    if (password.length < 8) {
      toast.error("Password must be at least 8 characters long");
      return false;
    }

    if (!role) {
      toast.error("Please select a role");
      return false;
    }

    return true;
  };

  //Function to handle user registration
  const registerUser = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      const response = await axiosInstance.post(
        "users/register",
        {
          username: username,
          email: email,
          password: password,
          role: role,
        },
        {
          headers: {
            "Content-Type": "application/json",
          },
        },
      );

      if (response.status === 200) {
        toast.success(response.data.message);
        navigate("/login");
      }
    } catch (error) {
      console.error("Registration error:", error);
      const errorMassage =
        error.response?.data?.Error ||
        error.message ||
        "Registration failed. Please try again.";
      console.log("Error message:", errorMassage);
      toast.error(errorMassage);
    }
  };

  return (
    <div
      className="min-h-screen w-full flex flex-col lg:flex-row 
items-center justify-center lg:justify-around 
px-4 lg:px-20 gap-8
bg-gradient-to-r from-[#ADEFD1] via-[#7FD8BE] to-[#ADEFD1]
bg-[length:400%_400%] animate-gradient"
    >
      {/* Left Side - Typed Text */}
      <div className="w-full lg:w-1/2 flex justify-center lg:justify-start min-h-[120px] sm:min-h-[200px] lg:min-h-[260px]">
        <span
          ref={el}
          className="text-xl sm:text-3xl lg:text-6xl font-bold text-black whitespace-pre-line text-center lg:text-left"
          style={{ fontFamily: "Inter, sans-serif" }}
        />
      </div>

      {/* Right Side - Register Form */}
      <div className="w-full sm:w-[380px] bg-white rounded-xl shadow-xl p-2 lg:p-2 lg:pt-4">
        <h2 className="text-3xl font-bold text-center mb-4">Register</h2>
        <form
          className="flex flex-col bg-white p-5 lg:p-5 pb-2 w-auto lg:w-92 relative"
          onSubmit={(e) => {
            registerUser(e);
          }}
        >
          <label
            htmlFor="username"
            className="absolute top-2 left-7 pr-1 pl-1 bg-white text-black font-semibold"
          >
            Username
          </label>
          <input
            type="text"
            placeholder="Enter the Username"
            id="username"
            className="border p-3 rounded-md mb-4"
            onChange={(e) => setUsername(e.target.value)}
          />
          <label
            htmlFor="email"
            className="absolute top-18 left-7 pr-1 pl-1 bg-white text-black font-semibold"
          >
            Email
          </label>
          <input
            type="email"
            placeholder="Enter the Email"
            id="email"
            className="border p-3 rounded-md mb-4"
            onChange={(e) => setEmail(e.target.value)}
          />
          <div className="flex flex-col gap-1 relative mb-4">
            <label
              htmlFor="password"
              className="text-black absolute top-[-0.80rem]
          left-2 pr-1 pl-1 bg-white font-semibold"
            >
              Password
            </label>
            <input
              type={showPassword ? "text" : "password"}
              id="password"
              placeholder="Enter the password"
              className="border p-3 rounded-md pr-12"
              required
              onChange={(e) => setPassword(e.target.value)}
            />
            <button
              type="button"
              className="absolute right-4 top-4 text-lg text-blue-600 font-bold"
              onClick={() => setShowPassword(!showPassword)}
            >
              {" "}
              {showPassword ? <FaEye /> : <FaEyeSlash />}
            </button>
          </div>
          <label
            htmlFor="role"
            className="absolute top-51 left-7 pr-1 pl-1 bg-white text-black font-semibold"
          >
            Role
          </label>
          <select
            name="role"
            placeholder="Select your role"
            id="role"
            className="border p-3 rounded-md mb-4"
            onChange={(e) => setRole(e.target.value)}
          >
            <option value="CUSTOMER">Customer</option>
            <option value="ADMIN">Admin</option>
          </select>
          <button
            type="submit"
            className="bg-blue-600  h-12 mt-1 rounded-xl font-semibold text-white text-xl hover:bg-blue-500"
          >
            Sign Up
          </button>
        </form>
        <p className="text-center mt-1 p-2 text-md">
          Already have an account?{" "}
          <Link
            to="/login"
            className="text-blue-600 font-semibold hover:underline"
          >
            Login
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Register;
