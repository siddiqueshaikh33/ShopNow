import CartIcon from "./CartIcon.jsx";
import Profile from "./Profile.jsx";
import { useNavigate } from "react-router-dom";
import axiosInstance from "../api/axiosInstance.js";
import { toast } from "react-toastify";

function Navbar({
  count = 0,
  logoSrc,
  title = "ShopNow!",
  dashboardPath = "/dashboard",
  loginPath = "/login",
  bgColor = "#ADEFD1",
  showCart = true,
  showProfile = true,
}) {
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      const response = await axiosInstance.post(
        "/auth/logout",
        {},
        { withCredentials: true }
      );
      if (response.status === 200) {
        toast.success("Logout Successful");
        navigate(loginPath);
      }
    } catch (error) {
      toast.error("Logout failed. Please try again.");
      console.error("error", error.response?.data || error.message);
    }
  };

  const handleDashboardNavigation = () => {
    if (window.location.pathname !== dashboardPath) {
      navigate(dashboardPath);
    }
  };

  return (
    <nav className="sticky top-0 z-50">
      <div
        className="flex w-full pt-3 pb-3 px-4 lg:px-15 flex-row justify-between items-center"
        style={{ backgroundColor: bgColor }}
      >
        <div className="flex items-center cursor-pointer" onClick={handleDashboardNavigation}>
          {logoSrc && (
            <img
              src={logoSrc}
              alt="Logo"
              className="w-12 h-12 rounded-full object-contain mr-2"
            />
          )}
          <p className="text-xl lg:text-3xl font-bold">{title}</p>
        </div>

        <div className="flex items-center gap-6">
          {showCart && <CartIcon count={count} />}
          {showProfile && <Profile handleLogout={handleLogout} />}
        </div>
      </div>
    </nav>
  );
}

export default Navbar;
