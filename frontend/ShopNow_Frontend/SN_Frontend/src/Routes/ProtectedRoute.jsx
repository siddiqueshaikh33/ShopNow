import { useContext, useEffect } from "react";
import { Navigate } from "react-router-dom";
import { SpinnerDotted } from "spinners-react";
import axiosInstance from "../api/axiosInstance";
import { UserContext } from "../Context/UserProvider";
import { toast } from "react-toastify";

function ProtectedRoute({ children }) {
  const { user, loading, setLoading, setUser } = useContext(UserContext);

  useEffect(() => {
    //Call /auth/me ONLY if user is not already present
    console.log("ProtectedRoute useEffect triggered. User:", user);
    if (user?.username) {
      setLoading(false);
      return;
    }

    const restoreSession = async () => {
      try {
        setLoading(true);
        const res = await axiosInstance.get("/auth/me", {
          withCredentials: true,
        });

        //  keep user shape consistent
        setUser(res.data);
        console.log("Session restored", res.data);
      } catch (error) {
        console.error("Failed to restore session", error.response?.data || error.message);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    restoreSession();
  }, [user, setLoading, setUser]);

  if (loading) {
    return (
      <div className="h-screen flex items-center justify-center">
        <SpinnerDotted
          size={40}
          thickness={100}
          speed={100}
          color="rgba(57, 172, 219, 1)"
        />
      </div>
    );
  }

  if (!user) {
    toast.error("Access Denied. Please log in.");
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

export default ProtectedRoute;
