import { useEffect, useState, useRef } from "react";
import logo from "../Assets/logo.jpg";
import { useContext } from "react";
import { UserContext } from "../Context/UserProvider.jsx";
import { useNavigate } from "react-router-dom";

function Profile({ handleLogout }) {
  const { user, loading } = useContext(UserContext);
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    const handler = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setOpen(false);
      }
    };

    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const navigateOrder = () => {
    navigate("/vieworder");
  };

  return (
    <div className="relative inline-block" ref={menuRef}>
      {/* Profile Button */}
      <div
        className="flex border rounded-full bg-white cursor-pointer"
        onClick={() => setOpen(!open)}
      >
        <img
          src={logo}
          alt="logo"
          className="w-8 h-8 rounded-full object-cover m-2"
        />
        <p className="m-2 p-1 text-black font-semibold">{loading ? "Loading..." : user ? user.username : "Guest"}</p>
      </div>

      {/* Dropdown */}
      {open && (
        <div className="absolute top-16 right-0 w-40 z-50">
          {/* Triangle */}
          <div className="absolute -top-2 right-18 w-4 h-4 bg-white border-l border-t rotate-45"></div>

          {/* Box */}
          <div className="bg-white border rounded shadow-md">
            <button
              className="w-full px-4 py-2 hover:bg-red-400 text-black border-b font-semibold"
              onClick={handleLogout}
            >
              Logout...
            </button>
             <button
              className="w-full px-4 py-2 hover:bg-blue-400 text-black font-semibold"
              onClick={navigateOrder}
            >
              Orders History
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default Profile;
