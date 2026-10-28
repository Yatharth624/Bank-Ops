import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, AuthResponse } from '../types';
import { apiClient } from '../api/client';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isAnalyst: boolean;
  login: (authData: AuthResponse) => void;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const savedToken = localStorage.getItem('bankops_token');
    const savedUser = localStorage.getItem('bankops_user');

    if (savedToken && savedUser) {
      try {
        setToken(savedToken);
        setUser(JSON.parse(savedUser));
      } catch (e) {
        localStorage.removeItem('bankops_token');
        localStorage.removeItem('bankops_user');
      }
    }
    setLoading(false);
  }, []);

  const login = (authData: AuthResponse) => {
    const userData: User = {
      id: authData.userId,
      username: authData.username,
      fullName: authData.fullName,
      email: authData.email,
      active: true,
      roles: authData.roles,
    };

    localStorage.setItem('bankops_token', authData.token);
    localStorage.setItem('bankops_user', JSON.stringify(userData));

    setToken(authData.token);
    setUser(userData);
  };

  const logout = () => {
    localStorage.removeItem('bankops_token');
    localStorage.removeItem('bankops_user');
    setToken(null);
    setUser(null);
    window.location.href = '/login';
  };

  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false;
  const isAnalyst = user?.roles.includes('ROLE_ANALYST') ?? false;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        isAdmin,
        isAnalyst,
        login,
        logout,
        loading,
      }}
    >
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
