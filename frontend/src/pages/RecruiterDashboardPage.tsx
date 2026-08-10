import React, { useState } from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { Users, Search, UploadCloud, CheckCircle2, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const RecruiterDashboardPage: React.FC = () => {
  const { resumeHistory, removeResumeFromHistory } = useResumeContext();
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');

  const filteredHistory = resumeHistory.filter(h => {
    const skillsStr = (h.result?.topSkills || []).join(' ').toLowerCase();
    const nameStr = (h.name || '').toLowerCase();
    const query = searchTerm.toLowerCase();
    return skillsStr.includes(query) || nameStr.includes(query);
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
            <Users className="w-6 h-6 text-red-400" /> Recruiter Candidate Intelligence Portal
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Browse and filter candidate resumes evaluated by the AI multi-agent pipeline in real-time.
          </p>
        </div>

        <Button onClick={() => navigate('/upload')} icon={<UploadCloud className="w-4 h-4" />}>
          Upload Candidates
        </Button>
      </div>

      {/* Search Bar */}
      <GlassCard className="p-4 flex items-center gap-3">
        <Search className="w-4 h-4 text-slate-500" />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Filter candidate database by name or skill (e.g. Spring Boot, React, DevOps)..."
          className="w-transparent text-xs text-white placeholder-slate-500 focus:outline-none flex-1"
        />
      </GlassCard>

      {/* Candidates Ranking Table */}
      <GlassCard className="overflow-x-auto p-0">
        {filteredHistory.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-slate-500 text-xs text-center space-y-3">
            <div className="w-16 h-16 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-slate-400">
              <Users className="w-8 h-8 text-red-500/80" />
            </div>
            <div>
              <div className="font-bold text-slate-350 uppercase">No Candidates Found</div>
              <p className="text-slate-500 mt-1 max-w-sm">
                No analyzed resumes are present matching the query. Please upload resumes via the parser.
              </p>
            </div>
          </div>
        ) : (
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-[10px] font-bold text-slate-400 uppercase tracking-widest bg-slate-950/60">
                <th className="p-4">Rank</th>
                <th className="p-4">Candidate Name</th>
                <th className="p-4">Career Domain</th>
                <th className="p-4">ATS Compatibility</th>
                <th className="p-4">Hiring Location</th>
                <th className="p-4">Top Skills</th>
                <th className="p-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-xs">
              {filteredHistory.map((h, idx) => {
                const res = h.result || {};
                const topHub = res.bestHiringLocations?.[0]?.city || 'Remote';
                return (
                  <tr key={h.id} className="hover:bg-slate-900/50 transition">
                    <td className="p-4 font-extrabold text-red-400">#{idx + 1}</td>
                    <td className="p-4 font-bold text-white">
                      {res.name || h.name}
                      <span className="block text-[10px] text-slate-400 font-normal">{res.email || 'no-email@domain.com'}</span>
                    </td>
                    <td className="p-4 text-slate-300 font-bold">{res.careerDomain || 'Software Systems'}</td>
                    <td className="p-4 font-extrabold text-emerald-400">{res.atsScore || 70}%</td>
                    <td className="p-4 text-slate-400">{topHub}</td>
                    <td className="p-4">
                      <div className="flex flex-wrap gap-1">
                        {(res.topSkills || []).slice(0, 3).map((s: string, sIdx: number) => (
                          <span key={sIdx} className="px-2 py-0.5 rounded bg-slate-800 text-[9px] text-slate-300 font-mono">
                            {s}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="p-4 text-right">
                      <Button size="sm" variant="outline" onClick={() => removeResumeFromHistory(h.id)}>
                        Remove
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </GlassCard>
    </div>
  );
};
