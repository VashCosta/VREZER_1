import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, UserRole } from '../types';

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (token: string, user: User) => void;
  logout: () => void;
  isAuthenticated: boolean;
  switchRole: (role: UserRole) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('careerforge_token'));
  const [user, setUser] = useState<User | null>(() => {
    const savedUser = localStorage.getItem('careerforge_user');
    if (savedUser) {
      try { return JSON.parse(savedUser); } catch (e) { return null; }
    }
    // Default demo user for instant out-of-the-box exploration
    return {
      id: 1,
      name: 'Alex Johnson',
      email: 'alex.johnson@careerforge.io',
      role: 'STUDENT',
      profilePhoto: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80',
      emailVerified: true
    };
  });

  useEffect(() => {
    if (token) {
      localStorage.setItem('careerforge_token', token);
    } else {
      localStorage.removeItem('careerforge_token');
    }
  }, [token]);

  useEffect(() => {
    if (user) {
      localStorage.setItem('careerforge_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('careerforge_user');
    }
  }, [user]);

  const login = (newToken: string, newUser: User) => {
    setToken(newToken);
    setUser(newUser);
  };

  const logout = () => {
    setToken(null);
    setUser(null);
  };

  const switchRole = (newRole: UserRole) => {
    if (user) {
      const updated = { ...user, role: newRole };
      setUser(updated);
    }
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token || !!user, switchRole }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
