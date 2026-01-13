import {useState, useEffect } from "react";
import { createContext } from "react";
import axiosInstance from "../api/axiosInstance";

export const UserContext = createContext();

const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);  

  return (
    <UserContext.Provider value={{ user, setUser, loading, setLoading }}>
      {children}
    </UserContext.Provider>
  );
};

export default UserProvider;
