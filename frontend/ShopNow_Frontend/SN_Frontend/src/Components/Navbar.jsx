import logo from '../Assets/logo.jpg';
import CartIcon from './CartIcon.jsx';
import Profile from './Profile.jsx';
import { useNavigate} from 'react-router-dom';
import axiosInstance from '../api/axiosInstance.js'
import {toast} from 'react-toastify';


function Navbar({count}) {

    const navigate = useNavigate();

    const handleLogout = async () => {
           try{
             const response = await axiosInstance.post('/auth/logout', {}, {
                withCredentials: true
             });

            console.log(response.data);
             if(response.status === 200) {
                toast.success("Logout Successful");
                 navigate('/login');  
             }
           } catch (error) {
                toast.error("Logout failed. Please try again.");
                console.error("error", error.response?.data || error.message);
           }
    }

    const handleDashboardNavigation = () => {
      if(window.location.pathname !== '/dashboard') {
        navigate('/dashboard');
      }
    }

  return (
    <nav className="sticky top-0 z-50">
    <div className="flex bg-[#ADEFD1] w-full pt-3 pb-3 px-4 lg:px-15 flex-row justify-between items-center">
        <div className="flex items-center">
            <img src={logo} alt="Logo" className="w-12 h-12 rounded-full object-contain mr-2" onClick={() => handleDashboardNavigation()}/>
            <p className="text-xl lg:text-3xl font-bold">ShopNow!</p>
        </div>
        <div className="flex items-center gap-6">
            <CartIcon count={count} />
            <Profile handleLogout={handleLogout} />
        </div>
    </div>
</nav>
  )
}

export default Navbar