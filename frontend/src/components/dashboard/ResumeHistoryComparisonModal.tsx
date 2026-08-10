import React, { useState } from 'react';
import { Layers, X, TrendingUp, CheckCircle2, ArrowRight, Sparkles, Award, ShieldCheck } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface ResumeHistoryComparisonModalProps {
  isOpen: boolean;
  onClose: () => void;
  historyList: Array<{ id: string; name: string; date: string; result: any; rawText: string }>;
  currentResult: any;
  onSelectVersion: (item: any) => void;
}

export const ResumeHistoryComparisonModal: React.FC<ResumeHistoryComparisonModalProps> = ({
  isOpen,
  onClose,
  historyList,
  currentResult,
  onSelectVersion
}) => {
  const [versionAId, setVersionAId] = useState<string>(historyList[0]?.id || 'v1');
  const [versionBId, setVersionBId] = useState<string>(historyList[1]?.id || historyList[0]?.id || 'v1');

  if (!isOpen) return null;

  const itemA = historyList.find(h => h.id === versionAId) || historyList[0] || { name: 'Current Dossier', result: currentResult };
  const itemB = historyList.find(h => h.id === versionBId) || { name: 'Latest Dossier', result: currentResult };

  const resA = itemA.result || currentResult || {};
  const resB = itemB.result || currentResult || {};

  const atsA = resA.atsScore ?? null;
  const atsB = resB.atsScore ?? null;
  const atsDelta = atsA != null && atsB != null ? atsB - atsA : null;

  const skillsA: string[] = resA.topSkills || [];
  const skillsB: string[] = resB.topSkills || [];

  const addedSkills = skillsB.filter(s => !skillsA.includes(s));
  const retainedSkills = skillsB.filter(s => skillsA.includes(s));

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-lg">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="w-full max-w-4xl glass-card rounded-3xl p-6 md:p-8 border border-white/10 shadow-[0_0_60px_rgba(255,0,60,0.2)] bg-[#0c121d] text-slate-100 space-y-6 max-h-[90vh] overflow-y-auto"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-red-600 to-rose-600 flex items-center justify-center text-white shadow-lg shadow-red-600/30">
                <Layers className="w-5 h-5 text-amber-300" />
              </div>
              <div>
                <h3 className="text-lg font-black uppercase tracking-wider text-white">Resume Version Comparison Matrix</h3>
                <p className="text-xs text-slate-400">Track ATS Score Evolution & Skill Progression Across Resume Drafts</p>
              </div>
            </div>
            <button onClick={onClose} className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition">
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Version Selectors */}
          <div className="grid md:grid-cols-2 gap-4">
            <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-2">
              <label className="text-[11px] font-mono font-bold text-slate-400 uppercase tracking-widest block">
                Baseline Version (Version A)
              </label>
              <select
                value={versionAId}
                onChange={(e) => setVersionAId(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-[#090d16] border border-white/15 text-xs font-mono text-white focus:outline-none focus:border-red-500"
              >
                {historyList.map(h => (
                  <option key={h.id} value={h.id}>
                    {h.name} — {h.date} (ATS: {h.result?.atsScore ?? 'Unavailable'}%)
                  </option>
                ))}
              </select>
            </div>

            <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/30 space-y-2">
              <label className="text-[11px] font-mono font-bold text-red-400 uppercase tracking-widest block">
                Comparison Target (Version B)
              </label>
              <select
                value={versionBId}
                onChange={(e) => setVersionBId(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-[#090d16] border border-red-500/40 text-xs font-mono text-white focus:outline-none focus:border-red-500"
              >
                {historyList.map(h => (
                  <option key={h.id} value={h.id}>
                    {h.name} — {h.date} (ATS: {h.result?.atsScore ?? 'Unavailable'}%)
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Key Metrics Comparison Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
            <div className="p-4 rounded-2xl bg-white/5 border border-white/10">
              <div className="text-[10px] font-mono font-bold text-slate-400 uppercase">Version A ATS</div>
              <div className="text-2xl font-black text-white mt-1">{atsA != null ? `${atsA}%` : 'Unavailable'}</div>
            </div>

            <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/30">
              <div className="text-[10px] font-mono font-bold text-red-400 uppercase">Version B ATS</div>
              <div className="text-2xl font-black text-white mt-1">{atsB != null ? `${atsB}%` : 'Unavailable'}</div>
            </div>

            <div className={`p-4 rounded-2xl border ${atsDelta != null && atsDelta >= 0 ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300' : 'bg-red-500/10 border-red-500/30 text-red-300'}`}>
              <div className="text-[10px] font-mono font-bold uppercase">ATS Score Delta</div>
              <div className="text-2xl font-black mt-1 flex items-center justify-center gap-1">
                <TrendingUp className="w-5 h-5" />
                {atsDelta != null ? (atsDelta >= 0 ? `+${atsDelta}%` : `${atsDelta}%`) : 'Unavailable'}
              </div>
            </div>

            <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-amber-300">
              <div className="text-[10px] font-mono font-bold uppercase">New Skills Added</div>
              <div className="text-2xl font-black mt-1">{addedSkills.length}</div>
            </div>
          </div>

          {/* Side-by-side Table */}
          <div className="border border-white/10 rounded-2xl overflow-hidden text-xs">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-white/5 text-slate-300 font-mono uppercase text-[10px] border-b border-white/10">
                  <th className="p-3">Metric / Section</th>
                  <th className="p-3">Version A ({itemA.name})</th>
                  <th className="p-3">Version B ({itemB.name})</th>
                  <th className="p-3">Improvement Analysis</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 font-sans">
                <tr>
                  <td className="p-3 font-mono font-bold text-slate-300">Target Role</td>
                  <td className="p-3 text-slate-400">{resA.role || 'Unavailable'}</td>
                  <td className="p-3 text-white font-bold">{resB.role || 'Unavailable'}</td>
                  <td className="p-3 text-emerald-400 font-bold">Updated alignment</td>
                </tr>
                <tr>
                  <td className="p-3 font-mono font-bold text-slate-300">Career Domain</td>
                  <td className="p-3 text-slate-400">{resA.careerDomain || 'Unavailable'}</td>
                  <td className="p-3 text-white font-bold">{resB.careerDomain || 'Unavailable'}</td>
                  <td className="p-3 text-slate-300">Domain focused</td>
                </tr>
                <tr>
                  <td className="p-3 font-mono font-bold text-slate-300">Technical Caliber</td>
                  <td className="p-3 text-slate-400">{resA.technicalSkillsScoreDetails?.score ?? 'Unavailable'}%</td>
                  <td className="p-3 text-white font-bold">{resB.technicalSkillsScoreDetails?.score ?? 'Unavailable'}%</td>
                  <td className="p-3 text-emerald-400 font-bold">+{resA.technicalSkillsScoreDetails?.score != null && resB.technicalSkillsScoreDetails?.score != null ? `${resB.technicalSkillsScoreDetails.score - resA.technicalSkillsScoreDetails.score}% Caliber` : 'Unavailable'}</td>
                </tr>
                <tr>
                  <td className="p-3 font-mono font-bold text-slate-300">Projects Count</td>
                  <td className="p-3 text-slate-400">{(resA.projects || []).length} Projects</td>
                  <td className="p-3 text-white font-bold">{(resB.projects || []).length} Projects</td>
                  <td className="p-3 text-slate-300">Detailed impact</td>
                </tr>
                <tr>
                  <td className="p-3 font-mono font-bold text-slate-300">New Detected Skills</td>
                  <td className="p-3 text-slate-400" colSpan={2}>
                    <div className="flex flex-wrap gap-1">
                      {addedSkills.length > 0 ? (
                        addedSkills.map((s, idx) => (
                          <span key={idx} className="px-2 py-0.5 rounded-lg bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-[10px]">
                            +{s}
                          </span>
                        ))
                      ) : (
                        <span className="text-slate-500">No new skills added in Version B</span>
                      )}
                    </div>
                  </td>
                  <td className="p-3 text-emerald-400 font-bold">{addedSkills.length} skills gained</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button
              onClick={() => {
                onSelectVersion(itemB);
                onClose();
              }}
              className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white font-bold text-xs uppercase tracking-widest shadow-lg shadow-red-600/30 flex items-center gap-2 transition"
            >
              <Sparkles className="w-4 h-4 text-amber-300" /> Switch Dashboard View to Version B
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
