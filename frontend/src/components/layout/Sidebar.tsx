import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { motion, AnimatePresence } from 'framer-motion';
import {
  LayoutDashboard, FileText, ShieldCheck, Target, Building2, Map, Users, HelpCircle,
  DollarSign, Code, FileCheck, Linkedin, Briefcase, Settings, Cpu, Wifi,
  ChevronLeft, ChevronRight, Zap, BarChart3
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { user } = useAuth();
  const role = user?.role || 'STUDENT';
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  const studentLinks = [
    { to: '/dashboard', label: 'Overview Dashboard', icon: LayoutDashboard, color: '#C1121F', badge: null },
    { to: '/upload', label: 'Resume ATS Parser', icon: FileText, color: '#D4AF37', badge: 'NEW' },
    { to: '/ats-analyzer', label: 'Formatting & Keywords', icon: ShieldCheck, color: '#10b981', badge: null },
    { to: '/jd-matcher', label: 'Job Description Matcher', icon: Target, color: '#6366f1', badge: null },
    { to: '/skill-gap', label: 'Company Skill Gap', icon: Building2, color: '#8b5cf6', badge: null },
    { to: '/career-roadmap', label: 'Career Roadmap', icon: Map, color: '#06b6d4', badge: null },
    { to: '/ai-interview', label: 'AI Mock Interview', icon: HelpCircle, color: '#f59e0b', badge: 'AI' },
    { to: '/salary-predictor', label: 'Salary Intelligence', icon: DollarSign, color: '#D4AF37', badge: null },
    { to: '/portfolio-analyzer', label: 'GitHub & Portfolio', icon: Code, color: '#ec4899', badge: null },
    { to: '/project-recommender', label: 'Project Recommender', icon: Cpu, color: '#10b981', badge: null },
    { to: '/resume-builder', label: 'Live Resume Builder', icon: FileCheck, color: '#6366f1', badge: null },
    { to: '/cover-letter', label: 'Cover Letter AI', icon: Briefcase, color: '#f59e0b', badge: null },
    { to: '/linkedin-optimizer', label: 'LinkedIn Optimizer', icon: Linkedin, color: '#0ea5e9', badge: null },
    { to: '/live-jobs', label: 'Live Job Market', icon: Wifi, color: '#C1121F', badge: 'LIVE' },
  ];

  const recruiterLinks = [
    { to: '/recruiter', label: 'Candidate Search & Rank', icon: Users, color: '#6366f1', badge: null },
    { to: '/upload', label: 'Batch Resume Upload', icon: FileText, color: '#D4AF37', badge: null },
    { to: '/recruiter', label: 'Shortlist Reports', icon: ShieldCheck, color: '#10b981', badge: null },
  ];

  const adminLinks = [
    { to: '/admin', label: 'Platform Analytics', icon: BarChart3, color: '#C1121F', badge: null },
    { to: '/admin', label: 'User Directory & Logs', icon: Users, color: '#6366f1', badge: null },
    { to: '/admin', label: 'System AI Control', icon: Settings, color: '#D4AF37', badge: null },
  ];

  const activeLinks = role === 'RECRUITER' ? recruiterLinks : role === 'ADMIN' ? adminLinks : studentLinks;

  return (
    <motion.aside
      animate={{ width: collapsed ? 64 : 256 }}
      transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
      className="relative bg-[#060409]/95 backdrop-blur-2xl border-r border-white/[0.06] min-h-[calc(100vh-61px)] flex flex-col justify-between shrink-0 hidden md:flex overflow-hidden"
    >
      {/* Ambient glow */}
      <div className="absolute top-0 left-0 w-full h-48 bg-gradient-to-b from-[#C1121F]/5 to-transparent pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-full h-32 bg-gradient-to-t from-[#D4AF37]/4 to-transparent pointer-events-none" />

      {/* Collapse toggle */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="absolute -right-3 top-6 z-20 w-6 h-6 rounded-full bg-[#0a060f] border border-[#C1121F]/40 flex items-center justify-center text-[#C1121F] hover:bg-[#C1121F]/10 transition-all shadow-lg hover:scale-110"
      >
        {collapsed ? <ChevronRight className="w-3 h-3" /> : <ChevronLeft className="w-3 h-3" />}
      </button>

      <div className="flex-1 overflow-y-auto no-scrollbar py-4">
        {/* Header label */}
        <AnimatePresence>
          {!collapsed && (
            <motion.div
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -10 }}
              className="px-4 py-2 mb-3"
            >
              <div className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#C1121F] shadow-[0_0_6px_#C1121F]" />
                <span className="text-[9px] font-black text-[#C1121F]/80 uppercase tracking-[0.2em] font-mono">
                  {role} MODULES
                </span>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        <nav className={`space-y-0.5 ${collapsed ? 'px-2' : 'px-3'}`}>
          {activeLinks.map((link, idx) => {
            const Icon = link.icon;
            const isActive = location.pathname === link.to;

            return (
              <NavLink
                key={`${link.to}-${idx}`}
                to={link.to}
                className="block"
              >
                {({ isActive: navActive }) => (
                  <motion.div
                    whileHover={{ x: collapsed ? 0 : 3 }}
                    whileTap={{ scale: 0.97 }}
                    className={`relative flex items-center gap-3 rounded-xl transition-all duration-200 group ${
                      collapsed ? 'justify-center p-2.5' : 'px-3 py-2.5'
                    } ${
                      navActive
                        ? 'bg-gradient-to-r from-[#C1121F]/15 to-[#D4AF37]/5 text-white border-l-2 border-[#C1121F]'
                        : 'text-slate-400 hover:text-white hover:bg-white/[0.05]'
                    }`}
                    style={navActive ? { paddingLeft: collapsed ? undefined : '10px' } : {}}
                  >
                    {/* Active indicator dot */}
                    {navActive && collapsed && (
                      <div className="absolute left-0 top-1/2 -translate-y-1/2 w-0.5 h-6 bg-[#C1121F] rounded-r" />
                    )}

                    {/* Icon */}
                    <div
                      className={`shrink-0 w-7 h-7 rounded-lg flex items-center justify-center transition-all duration-200 ${
                        navActive
                          ? 'shadow-lg'
                          : 'group-hover:scale-110'
                      }`}
                      style={{
                        background: navActive ? `${link.color}20` : 'transparent',
                        boxShadow: navActive ? `0 0 12px ${link.color}40` : 'none',
                      }}
                    >
                      <Icon
                        className="w-4 h-4"
                        style={{ color: navActive ? link.color : undefined }}
                      />
                    </div>

                    {/* Label */}
                    <AnimatePresence>
                      {!collapsed && (
                        <motion.span
                          initial={{ opacity: 0, width: 0 }}
                          animate={{ opacity: 1, width: 'auto' }}
                          exit={{ opacity: 0, width: 0 }}
                          className="text-xs font-semibold whitespace-nowrap overflow-hidden flex-1"
                        >
                          {link.label}
                        </motion.span>
                      )}
                    </AnimatePresence>

                    {/* Badge */}
                    {link.badge && !collapsed && (
                      <span
                        className={`text-[9px] font-black px-1.5 py-0.5 rounded-full uppercase tracking-wider ${
                          link.badge === 'LIVE'
                            ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                            : link.badge === 'AI'
                            ? 'bg-[#C1121F]/15 text-[#C1121F] border border-[#C1121F]/30'
                            : 'bg-[#D4AF37]/15 text-[#D4AF37] border border-[#D4AF37]/30'
                        }`}
                      >
                        {link.badge}
                      </span>
                    )}
                  </motion.div>
                )}
              </NavLink>
            );
          })}
        </nav>
      </div>

      {/* Pro Badge */}
      <div className={`${collapsed ? 'px-2 pb-4' : 'px-3 pb-4'}`}>
        <AnimatePresence>
          {!collapsed ? (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 10 }}
              className="p-4 rounded-2xl bg-gradient-to-br from-[#0c0508]/80 via-[#080510]/80 to-[#080510]/80 border border-[#C1121F]/20 text-center relative overflow-hidden"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-[#C1121F]/5 to-[#D4AF37]/3 pointer-events-none" />
              <div className="relative z-10">
                <div className="inline-flex p-2 rounded-xl bg-[#C1121F]/10 border border-[#C1121F]/20 mb-2">
                  <Zap className="w-4 h-4 text-[#C1121F] animate-pulse" />
                </div>
                <h4 className="text-xs font-bold text-white">VREZER AI</h4>
                <p className="text-[10px] text-slate-400 mt-0.5">Gemini + Llama Powered</p>
                <div className="flex items-center justify-center gap-1 mt-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 shadow-[0_0_5px_#10b981]" />
                  <span className="text-[10px] text-emerald-400 font-bold">All Systems Live</span>
                </div>
              </div>
            </motion.div>
          ) : (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex justify-center"
            >
              <div className="w-8 h-8 rounded-xl bg-[#C1121F]/10 border border-[#C1121F]/20 flex items-center justify-center">
                <Zap className="w-4 h-4 text-[#C1121F] animate-pulse" />
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.aside>
  );
};
