import React, { useContext, useEffect, useRef, useState} from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import axiosInstance from '../api/axiosInstance';
import { Link } from 'react-router-dom';
import Typed from 'typed.js';
import { UserContext } from '../Context/UserProvider';
import { FaEye, FaEyeSlash } from "react-icons/fa";

function Login() {

      const [Username, setUsername] = useState('');
      const [password, setPassword] = useState('');
      const [showPassword, setShowPassword] = useState(false);
      const navigate = useNavigate();
      const {setUser} = useContext(UserContext);
      const el = useRef(null);
      const name = "ShopNow!";
    
      useEffect(() => {
        const typed = new Typed(el.current, {
          strings: [
`One step closer\n to becoming a part\n of <span class="bg-gradient-to-r from-[#FFFFFF] via-[#F1F5F9] to-[#FFFFFF] bg-clip-text text-transparent font-extrabold drop-shadow-md">${name}</span>`
          ],
          typeSpeed: 80,
          loop: true,
          cursorChar: ""
        });
    
        return () => typed.destroy();
      }, []);


   const validateForm = () => {
    if (!Username.trim()) {
      toast.error("Username is required");
      return false;
    } 

    if(/^\d+$/.test(Username)) {
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
   }   
    
   const verifyUser = async (e) => {
    e.preventDefault();
    // Add your login logic here
    if (!validateForm()) {
      return;
    }
    
    try{
     const response = await axiosInstance.post('auth/login', 
      {
        "username": Username,
        "password": password
      },
      {withCredentials: true}
    );

    if (response.data.message === "Login successful") {
      toast.success("Login Successful");
      setUser(response.data);
      console.log('Login successful:', response.data.username);
      navigate('/dashboard');
    } 
  } catch (error) {
      console.error('Login error:', error);
      const errorMassage = error.response?.data?.message;
      console.log("Error message:", errorMassage);
      errorMassage? toast.error(errorMassage) : toast.error('Something went wrong during login! Please try again.');
  }
   }
    
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
      <div className="w-full sm:w-[380px] bg-white rounded-xl  shadow-xl p-2  lg:pt-4">
        <h2 className="text-3xl font-bold text-center mb-4">
          Login
        </h2>
<form className='flex flex-col bg-white p-5 lg:p-5 pb-2 w-auto lg:w-92 relative gap-1' onSubmit={(e) => verifyUser(e)}>
          
          <label htmlFor="username" className='absolute top-2 left-7 pr-1 pl-1 bg-white text-black font-semibold'>Username</label>
          <input type="text" placeholder='Enter the Username' id="username" className='border p-3 rounded-md mb-4' onChange={(e) => setUsername(e.target.value)} />
         
          <div className="flex flex-col gap-1 relative mb-2">
        <label htmlFor="password" className="text-black absolute top-[-0.80rem]
 left-2 pr-1 pl-1 bg-white font-semibold">
          Password
        </label>
        <input
          type= {showPassword ? "text" : "password"}
          id="password"
          placeholder="Enter the password"
          className="border p-3 rounded-md pr-12"
          required 
          onChange={(e) => setPassword(e.target.value)}
        />
        <button
          type="button"
          className="absolute right-4 top-4 text-lg text-blue-600 font-bold" onClick={() => setShowPassword(!showPassword)}
        > {showPassword ? <FaEye/> : <FaEyeSlash/>}
        </button>
      </div>
          <button type="submit" className='bg-blue-600  h-12 mt-1 rounded-xl font-semibold text-white text-xl hover:bg-blue-500'>Log In</button>
        </form>
        <p className="text-center p-2 text-md ">
          Don't have an account?{" "}
          <Link to="/" className="text-blue-600 font-semibold hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  )
}

export default Login