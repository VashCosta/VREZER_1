import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useResumeContext } from '../../context/ResumeContext';
import { ApiKeyModal } from '../ui/ApiKeyModal';
import {
  Sparkles, Sun, Moon, LogOut, Shield, ChevronDown, Bell, Clock, Key, FileText, Target, Map, HelpCircle, LayoutDashboard, Briefcase, Settings
} from 'lucide-react';
import { UserRole } from '../../types';

export const Navbar: React.FC = () => {
  const { user, logout, switchRole } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const { apiKey, setApiKey } = useResumeContext();
  const [showRoleMenu, setShowRoleMenu] = useState(false);
  const [showApiKeyModal, setShowApiKeyModal] = useState(false);
  const [timeString, setTimeString] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setTimeString(now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true }));
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  return (
    <>
      <header className="sticky top-0 z-40 bg-[#0b0f19]/90 backdrop-blur-xl border-b border-slate-800/80 px-4 lg:px-8 py-3 transition-colors">
        <div className="flex items-center justify-between max-w-7xl mx-auto">
          
          {/* Brand Logo & Platform Title */}
          <div className="flex items-center gap-6">
            <Link to="/" className="flex items-center gap-2.5 group">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-red-600 via-rose-600 to-red-400 p-0.5 shadow-lg shadow-red-500/30 group-hover:scale-105 transition-transform">
                <div className="w-full h-full bg-black rounded-[10px] flex items-center justify-center">
                  <Sparkles className="w-5 h-5 text-red-500 group-hover:rotate-12 transition-transform" />
                </div>
              </div>
              <div className="flex flex-col">
                <span className="font-black text-xl tracking-widest bg-clip-text text-transparent bg-gradient-to-r from-white via-red-200 to-red-500">
                  V R E Z E R
                </span>
                <span className="text-[10px] font-bold text-red-400/80 tracking-wider uppercase -mt-0.5">
                  AI Career Intelligence
                </span>
              </div>
            </Link>

            {/* Quick Navigation Links */}
            <nav className="hidden xl:flex items-center gap-1 pl-6 border-l border-slate-800/80 text-xs">
              <Link
                to="/dashboard"
                className={`px-3 py-1.5 rounded-lg transition font-medium flex items-center gap-1.5 ${
                  location.pathname === '/dashboard'
                    ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <LayoutDashboard className="w-3.5 h-3.5 text-indigo-400" /> Dashboard
              </Link>
              <Link
                to="/ats-analyzer"
                className={`px-3 py-1.5 rounded-lg transition font-medium flex items-center gap-1.5 ${
                  location.pathname === '/ats-analyzer'
                    ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <FileText className="w-3.5 h-3.5 text-cyan-400" /> ATS Auditor
              </Link>
              <Link
                to="/jd-matcher"
                className={`px-3 py-1.5 rounded-lg transition font-medium flex items-center gap-1.5 ${
                  location.pathname === '/jd-matcher'
                    ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <Target className="w-3.5 h-3.5 text-violet-400" /> JD Matcher
              </Link>
              <Link
                to="/career-roadmap"
                className={`px-3 py-1.5 rounded-lg transition font-medium flex items-center gap-1.5 ${
                  location.pathname === '/career-roadmap'
                    ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <Map className="w-3.5 h-3.5 text-emerald-400" /> Roadmap
              </Link>
              <Link
                to="/ai-interview"
                className={`px-3 py-1.5 rounded-lg transition font-medium flex items-center gap-1.5 ${
                  location.pathname === '/ai-interview'
                    ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <HelpCircle className="w-3.5 h-3.5 text-amber-400" /> Mock Studio
              </Link>
            </nav>
          </div>

          {/* Center API Key Status Badge */}
          <div className="hidden md:flex items-center gap-2">
            <button
              onClick={() => setShowApiKeyModal(true)}
              className={`px-3.5 py-1.5 rounded-xl border text-xs font-mono font-semibold flex items-center gap-2 transition shadow-sm ${
                apiKey
                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20'
                  : 'bg-amber-500/10 border-amber-500/30 text-amber-300 hover:bg-amber-500/20'
              }`}
            >
              <Key className="w-3.5 h-3.5 text-amber-300" />
              <span>{apiKey ? 'AI ENGINE: CONNECTED' : 'SET GEMINI API KEY'}</span>
            </button>
          </div>

          {/* Right Actions: Clock, Role & User Avatar */}
          <div className="flex items-center gap-3">
            
            {/* Live Digital Clock */}
            <div className="hidden sm:flex px-3 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800 text-slate-300 text-xs font-mono items-center gap-2 shadow-inner">
              <Clock className="w-3.5 h-3.5 text-indigo-400" />
              <span>{timeString || '08:21:43 PM'}</span>
            </div>
            
            {/* Role Switcher Pill */}
            {user && (
              <div className="relative">
                <button
                  onClick={() => setShowRoleMenu(!showRoleMenu)}
                  className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 text-xs font-semibold text-slate-300 hover:text-white hover:border-indigo-500/40 transition shadow-sm"
                >
                  <Shield className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Role: <strong className="text-indigo-400">{user.role}</strong></span>
                  <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
                </button>

                {showRoleMenu && (
                  <div className="absolute right-0 mt-2 w-52 bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-1.5 z-50 backdrop-blur-xl">
                    <div className="px-3 py-2 text-[10px] font-bold text-slate-400 uppercase tracking-wider">Switch Ecosystem Role</div>
                    {(['STUDENT', 'RECRUITER', 'ADMIN'] as UserRole[]).map((role) => (
                      <button
                        key={role}
                        onClick={() => {
                          switchRole(role);
                          setShowRoleMenu(false);
                          if (role === 'RECRUITER') navigate('/recruiter');
                          else if (role === 'ADMIN') navigate('/admin');
                          else navigate('/dashboard');
                        }}
                        className={`w-full text-left px-3 py-2 text-xs font-medium rounded-xl flex items-center justify-between transition ${
                          user.role === role ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30' : 'text-slate-300 hover:bg-slate-800/60'
                        }`}
                      >
                        <span>{role === 'STUDENT' ? '🎓 Student / Jobseeker' : role === 'RECRUITER' ? '💼 Recruiter Suite' : '⚙️ System Admin'}</span>
                        {user.role === role && <span className="w-2 h-2 rounded-full bg-indigo-400 shadow-[0_0_8px_#818cf8]"></span>}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Dark / Light Toggle */}
            <button
              onClick={toggleTheme}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition"
              title="Toggle theme"
            >
              {isDark ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-slate-400" />}
            </button>

            {/* User Profile */}
            {user ? (
              <div className="flex items-center gap-2 pl-2 border-l border-slate-800">
                <img
                  src={user.profilePhoto || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80'}
                  alt={user.name}
                  className="w-8 h-8 rounded-full border border-indigo-500/40 object-cover"
                />
                <button
                  onClick={logout}
                  className="p-2 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-rose-400 transition"
                  title="Logout"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <Link
                to="/login"
                className="px-4 py-2 text-xs font-semibold rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white shadow-lg shadow-indigo-500/25 border border-indigo-500/30 transition"
              >
                Sign In
              </Link>
            )}

          </div>
        </div>
      </header>

      <ApiKeyModal
        isOpen={showApiKeyModal}
        onClose={() => setShowApiKeyModal(false)}
        apiKey={apiKey}
        onSaveKey={(newKey) => setApiKey(newKey)}
      />
    </>
  );
};

