import React from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard,
  ArrowLeftRight,
  AlertTriangle,
  History,
  Sliders,
  Users,
  LogOut,
  ShieldCheck,
  Activity,
  Layers,
} from 'lucide-react';

export const Layout: React.FC = () => {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const correlationId = sessionStorage.getItem('bankops_correlation_id') || 'CORR-SYS';

  const navItems = [
    { name: 'Dashboard', path: '/', icon: <LayoutDashboard size={18} /> },
    { name: 'Transactions', path: '/transactions', icon: <ArrowLeftRight size={18} /> },
    { name: 'Exceptions Queue', path: '/exceptions', icon: <AlertTriangle size={18} /> },
    { name: 'Audit Trail', path: '/audit', icon: <History size={18} /> },
  ];

  const adminItems = [
    { name: 'Validation Rules', path: '/admin/rules', icon: <Sliders size={18} /> },
    { name: 'User Management', path: '/admin/users', icon: <Users size={18} /> },
  ];

  return (
    <div className="min-h-screen flex bg-slate-950 text-slate-100">
      {/* Sidebar Navigation */}
      <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between shrink-0">
        <div>
          {/* Brand Logo */}
          <div className="h-16 flex items-center gap-3 px-6 border-b border-slate-800 bg-slate-900/50">
            <div className="h-9 w-9 rounded-lg bg-gradient-to-tr from-sky-600 to-indigo-500 flex items-center justify-center text-white shadow-md shadow-sky-900/30">
              <ShieldCheck size={22} />
            </div>
            <div>
              <span className="font-bold text-lg tracking-tight bg-gradient-to-r from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
                BankOps
              </span>
              <span className="block text-[10px] text-sky-400 font-mono tracking-widest uppercase">
                Enterprise Core
              </span>
            </div>
          </div>

          {/* Navigation Links */}
          <div className="px-3 py-4 space-y-1">
            <div className="px-3 pb-2 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
              Operations
            </div>
            {navItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === '/'}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                  }`
                }
              >
                {item.icon}
                <span>{item.name}</span>
              </NavLink>
            ))}

            {isAdmin && (
              <>
                <div className="px-3 pt-6 pb-2 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                  Administration
                </div>
                {adminItems.map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                        isActive
                          ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                          : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                      }`
                    }
                  >
                    {item.icon}
                    <span>{item.name}</span>
                  </NavLink>
                ))}
              </>
            )}
          </div>
        </div>

        {/* User Card & Logout */}
        <div className="p-4 border-t border-slate-800 bg-slate-900/60">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-3 overflow-hidden">
              <div className="h-8 w-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center font-bold text-xs text-sky-400 shrink-0">
                {user?.fullName?.charAt(0) || 'U'}
              </div>
              <div className="truncate">
                <div className="text-xs font-semibold text-slate-200 truncate">{user?.fullName}</div>
                <div className="text-[10px] text-slate-400 truncate">@{user?.username}</div>
              </div>
            </div>
            <span
              className={`text-[10px] font-semibold px-2 py-0.5 rounded border uppercase ${
                isAdmin
                  ? 'bg-purple-950 text-purple-300 border-purple-800'
                  : 'bg-sky-950 text-sky-300 border-sky-800'
              }`}
            >
              {isAdmin ? 'ADMIN' : 'ANALYST'}
            </span>
          </div>

          <button
            onClick={logout}
            className="w-full flex items-center justify-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-rose-400 hover:bg-rose-950/30 border border-slate-800 hover:border-rose-900 transition-colors"
          >
            <LogOut size={14} />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        {/* Top Header */}
        <header className="h-16 px-8 bg-slate-900/70 backdrop-blur border-b border-slate-800 flex items-center justify-between sticky top-0 z-20">
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-2 text-xs font-mono px-2.5 py-1 rounded bg-slate-800/80 border border-slate-700/80 text-slate-300">
              <Activity size={13} className="text-emerald-400" />
              <span>Trace ID:</span>
              <span className="text-sky-400">{correlationId}</span>
            </span>
            <span className="text-xs text-slate-400 flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse"></span>
              Synthetic Sandbox Connected
            </span>
          </div>

          <div className="flex items-center gap-3">
            <span className="text-xs text-slate-400">Environment:</span>
            <span className="text-xs font-medium px-2 py-0.5 rounded bg-amber-950/80 border border-amber-800 text-amber-300 font-mono">
              STAGE-FINTECH-DEMO
            </span>
          </div>
        </header>

        {/* Page Container */}
        <main className="p-8 flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
