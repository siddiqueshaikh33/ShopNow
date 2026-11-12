import React, { useContext, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import axiosInstance from "../api/axiosInstance";
import { Link } from "react-router-dom";
import Typed from "typed.js";
import { UserContext } from "../Context/UserProvider";
import { FaEye, FaEyeSlash } from "react-icons/fa";

function Login() {
  const [Username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const navigate = useNavigate();
  const { setUser } = useContext(UserContext);
  const el = useRef(null);
  const name = "ShopNow!";

  useEffect(() => {
    if (cooldown <= 0) return;
    const interval = setInterval(() => {
      setCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, [cooldown]);

  useEffect(() => {
    const typed = new Typed(el.current, {
      strings: [
        `One step closer\n to becoming a part\n of <span class="bg-gradient-to-r from-[#FFFFFF] via-[#F1F5F9] to-[#FFFFFF] bg-clip-text text-transparent font-extrabold drop-shadow-md">${name}</span>`,
      ],
      typeSpeed: 80,
      loop: true,
      cursorChar: "",
    });

    return () => typed.destroy();
  }, []);

  const validateForm = () => {
    if (!Username.trim()) {
      toast.error("Username is required");
      return false;
    }

    if (/^\d+$/.test(Username)) {
      toast.error("Username cannot contain special characters");
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

    return true;
  };

  const verifyUser = async (e) => {
    e.preventDefault();
    if (cooldown > 0) {
      toast.error(`Please wait ${cooldown} seconds before trying again.`);
      return;
    }
    if (!validateForm()) {
      return;
    }

    try {
      const response = await axiosInstance.post(
        "auth/login",
        {
          username: Username,
          password: password,
        },
        { withCredentials: true }
      );

      if (response.data.message === "Login successful") {
        toast.success("Login Successful");
        setUser(response.data);
        console.log("Login successful:", response.data.username);
        if (response.data.role === "ADMIN") {
          navigate("/admindashboard");
        } else {
          navigate("/dashboard");
        }
      }
    } catch (error) {
      console.error("Login error:", error);
      if (error.response?.status === 429) {
        const seconds = error.retryAfter || 60;
        setCooldown(seconds);
        toast.error(`Too many attempts. Please wait ${seconds} seconds.`);
        return;
      }
      const errorMassage = error.response?.data?.message;
      console.log("Error message:", errorMassage);
      errorMassage
        ? toast.error(errorMassage)
        : toast.error("Something went wrong during login! Please try again.");
    }
  };

  return (
    <div
      className="min-h-screen w-full flex flex-col lg:flex-row 
items-center justify-center lg:justify-around 
px-4 sm:px-8 lg:px-20 py-12 gap-8 sm:gap-12
bg-gradient-to-r from-[#ADEFD1] via-[#7FD8BE] to-[#ADEFD1]
bg-[length:400%_400%] animate-gradient"
    >
      {/* Left Side - Typed Text */}
      <div className="w-full lg:w-1/2 flex justify-center lg:justify-start min-h-[100px] sm:min-h-[180px] lg:min-h-[240px]">
        <span
          ref={el}
          className="text-2xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 whitespace-pre-line text-center lg:text-left leading-tight drop-shadow-xs"
        />
      </div>

      {/* Right Side - Login Card */}
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-100 p-6 sm:p-10 relative z-10">
        <div className="text-center mb-6">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Welcome Back
          </h2>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Sign in to access your cart and account
          </p>
        </div>

        <form
          className="flex flex-col gap-4"
          onSubmit={(e) => verifyUser(e)}
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
              placeholder="Enter your username"
              id="username"
              value={Username}
              className="w-full px-4 py-3 rounded-xl border border-slate-300 bg-slate-50/50 text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white transition"
              onChange={(e) => setUsername(e.target.value)}
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
                placeholder="Enter your password"
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

          {cooldown > 0 && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-xl text-xs text-center font-medium">
              Too many attempts. Please try again in <span className="font-bold">{cooldown}s</span>.
            </div>
          )}

          <button
            type="submit"
            disabled={cooldown > 0}
            className={`w-full h-12 mt-2 rounded-xl font-bold text-sm text-white transition-all duration-200 shadow-xs active:scale-[0.98] ${
              cooldown > 0
                ? "bg-slate-400 cursor-not-allowed"
                : "bg-slate-900 hover:bg-[#062828] cursor-pointer"
            }`}
            style={{
              borderBottom: cooldown > 0 ? "none" : "2px solid #ADEFD1",
            }}
          >
            {cooldown > 0 ? `Please Wait (${cooldown}s)` : "Sign In"}
          </button>
        </form>

        <p className="text-center text-xs text-slate-600 mt-6 pt-4 border-t border-slate-100">
          Don't have an account?{" "}
          <Link to="/" className="text-slate-900 font-bold hover:underline">
            Register now
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Login;
