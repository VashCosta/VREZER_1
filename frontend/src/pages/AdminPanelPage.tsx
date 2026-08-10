import React from 'react';
import { GlassCard } from '../components/ui/GlassCard';
import { LayoutDashboard, Users, ShieldCheck, Activity, Terminal, MessageSquare } from 'lucide-react';

export const AdminPanelPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
          <LayoutDashboard className="w-6 h-6 text-indigo-400" /> Platform Telemetry & Admin Control Panel
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Monitor active system metrics, user registrations, recruiter actions, system logs, and feedback.
        </p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <GlassCard>
          <span className="text-xs font-bold text-slate-400">Total Platform Users</span>
          <div className="text-3xl font-extrabold text-white mt-2">1,240</div>
        </GlassCard>
        <GlassCard>
          <span className="text-xs font-bold text-slate-400">Active Students</span>
          <div className="text-3xl font-extrabold text-blue-400 mt-2">980</div>
        </GlassCard>
        <GlassCard>
          <span className="text-xs font-bold text-slate-400">Enterprise Recruiters</span>
          <div className="text-3xl font-extrabold text-indigo-400 mt-2">140</div>
        </GlassCard>
        <GlassCard>
          <span className="text-xs font-bold text-slate-400">System Uptime</span>
          <div className="text-3xl font-extrabold text-emerald-400 mt-2">100%</div>
        </GlassCard>
      </div>

      <GlassCard className="space-y-3">
        <h3 className="font-bold text-sm text-white flex items-center gap-2">
          <Terminal className="w-4 h-4 text-emerald-400" /> System Telemetry Logs
        </h3>
        <div className="p-4 rounded-xl bg-slate-950 font-mono text-[11px] text-slate-300 space-y-2 border border-slate-800">
          <div className="text-emerald-400">[INFO] 2026-07-24 23:05:14 - FileParsingService: PDF stream parsed successfully (1.2MB).</div>
          <div className="text-blue-400">[INFO] 2026-07-24 23:05:15 - AiAnalysisService: ATS Audit generated for resume #101.</div>
          <div className="text-purple-400">[INFO] 2026-07-24 23:05:16 - SecurityConfig: Authenticated JWT token for user alex.johnson@careerforge.io.</div>
        </div>
      </GlassCard>
    </div>
  );
};
