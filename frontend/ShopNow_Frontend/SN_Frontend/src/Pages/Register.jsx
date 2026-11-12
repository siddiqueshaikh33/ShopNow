import { useRef, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Link } from "react-router-dom";
import Typed from "typed.js";
import axiosInstance from "../api/axiosInstance";
import { toast } from "react-toastify";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { FcGoogle } from "react-icons/fc";

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
        }
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
px-4 lg:px-20 py-6 sm:py-10 gap-8
bg-gradient-to-r from-[#ADEFD1] via-[#7FD8BE] to-[#ADEFD1]
bg-[length:400%_400%] animate-gradient"
    >
      {/* Left Side - Typed Text */}
      <div className="w-full lg:w-1/2 flex justify-center lg:justify-start min-h-[120px] sm:min-h-[180px] lg:min-h-[240px]">
        <span
          ref={el}
          className="text-2xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 whitespace-pre-line text-center lg:text-left leading-tight drop-shadow-xs"
        />
      </div>

      {/* Right Side - Register Card */}
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-100 p-6 sm:p-10 relative z-10 mb-8 sm:mb-0">
        <div className="text-center mb-6">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Create an Account
          </h2>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Sign up to start shopping on ShopNow
          </p>
        </div>

        <form
          className="flex flex-col gap-4"
          onSubmit={(e) => registerUser(e)}
        >
          <div className="space-y-1.5">
            <label
              htmlFor="username"
              className="block text-xs font-semibold text-slate-700"
            >
              Username
            </label>
            <input
              type="text"
              placeholder="Choose a username"
              id="username"
              value={username}
              className="w-full px-4 py-3 rounded-xl border border-slate-300 bg-slate-50/50 text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white transition"
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          <div className="space-y-1.5">
            <label
              htmlFor="email"
              className="block text-xs font-semibold text-slate-700"
            >
              Email Address
            </label>
            <input
              type="email"
              placeholder="name@example.com"
              id="email"
              value={email}
              className="w-full px-4 py-3 rounded-xl border border-slate-300 bg-slate-50/50 text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white transition"
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="space-y-1.5">
            <label
              htmlFor="password"
              className="block text-xs font-semibold text-slate-700"
            >
              Password
            </label>
            <div className="relative">
              <input
                type={showPassword ? "text" : "password"}
                id="password"
                placeholder="Minimum 8 characters"
                value={password}
                className="w-full px-4 py-3 pr-12 rounded-xl border border-slate-300 bg-slate-50/50 text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white transition"
                required
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type="button"
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700 transition cursor-pointer p-1"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <FaEye size={16} /> : <FaEyeSlash size={16} />}
              </button>
            </div>
          </div>

          <div className="space-y-1.5">
            <label
              htmlFor="role"
              className="block text-xs font-semibold text-slate-700"
            >
              Account Type
            </label>
            <select
              name="role"
              id="role"
              value={role}
              className="w-full px-4 py-3 rounded-xl border border-slate-300 bg-slate-50/50 text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white transition"
              onChange={(e) => setRole(e.target.value)}
            >
              <option value="CUSTOMER">Customer</option>
            </select>
          </div>

          <button
            type="submit"
            className="w-full h-12 mt-2 rounded-xl font-bold text-sm text-white transition-all duration-200 shadow-xs active:scale-[0.98] bg-slate-900 hover:bg-[#062828] cursor-pointer"
            style={{
              borderBottom: "2px solid #ADEFD1",
            }}
          >
            Create Account
          </button>

          <div className="flex items-center my-2 justify-center gap-3">
            <div className="h-px bg-slate-200 flex-grow" />
            <span className="text-xs font-semibold text-slate-400">OR</span>
            <div className="h-px bg-slate-200 flex-grow" />
          </div>

          <button
            type="button"
            onClick={() =>
              (window.location.href =
                "http://localhost:8080/oauth2/authorization/google")
            }
            className="w-full h-11 px-4 rounded-xl border border-slate-300 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-xs flex items-center justify-center gap-2.5 transition shadow-2xs cursor-pointer"
          >
            <FcGoogle size={18} />
            <span>Continue with Google</span>
          </button>
        </form>

        <p className="text-center text-xs text-slate-600 mt-6 pt-4 border-t border-slate-100">
          Already have an account?{" "}
          <Link to="/login" className="text-slate-900 font-bold hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Register;
