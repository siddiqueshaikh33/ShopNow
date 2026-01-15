import {useState } from "react";
import { createContext } from "react";


export const UserContext = createContext();

const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);  
  console.log("UserProvider Rendered. User:", user);
  return (
    <UserContext.Provider value={{ user, setUser, loading, setLoading }}>
      {children}
    </UserContext.Provider>
  );
};

export default UserProvider;
