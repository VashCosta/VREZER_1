import React, { useState } from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { DollarSign, Sparkles, TrendingUp, MapPin, Building2, Zap, BarChart3, Award, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const SalaryPredictorPage: React.FC = () => {
  const { aiAnalysisResult } = useResumeContext();
  const navigate = useNavigate();

  const [role, setRole] = useState<string>(aiAnalysisResult?.role || 'Senior Software Engineer');
  const [domain, setDomain] = useState<string>(aiAnalysisResult?.careerDomain || 'Backend Development');
  const [expYears, setExpYears] = useState<number>(aiAnalysisResult?.yearsOfExperience || 4);
  const [location, setLocation] = useState<string>('Bengaluru');

  const salaryEvidence = aiAnalysisResult?.candidateSalaryEstimate;
  const computeSalaryPrediction = () => {
    if (salaryEvidence && salaryEvidence.status === 'OK' && salaryEvidence.median != null) {
      const minLpa = salaryEvidence.min;
      const maxLpa = salaryEvidence.max;
      const medianLpa = salaryEvidence.median;
      return { minLpa, medianLpa, maxLpa };
    }
    return null;
  };

  const sal = computeSalaryPrediction();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
          <DollarSign className="w-6 h-6 text-red-400" /> AI Compensation & Salary Predictor
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Predict market-accurate salary bands based on industrial experience, tech stack demand, education pedigree, and city location benchmarks.
        </p>
      </div>

      {/* Interactive Predictor Inputs */}
      <GlassCard className="p-6 border border-slate-800 space-y-4">
        <h3 className="font-extrabold text-xs text-red-400 uppercase tracking-wider">Salary Prediction Controls</h3>
        
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
          <div>
            <label className="block text-slate-300 font-semibold mb-1">Target Role</label>
            <input
              type="text"
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-red-500"
            />
          </div>

          <div>
            <label className="block text-slate-300 font-semibold mb-1">Career Domain</label>
            <select
              value={domain}
              onChange={(e) => setDomain(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-red-500"
            >
              <option value="Backend Development">Backend Development</option>
              <option value="AI & Machine Learning">AI & Machine Learning</option>
              <option value="Full Stack Development">Full Stack Development</option>
              <option value="Cloud & DevOps">Cloud & DevOps</option>
              <option value="Data Science & Analytics">Data Science & Analytics</option>
              <option value="Digital Marketing">Digital Marketing</option>
              <option value="Finance & Accounting">Finance & Accounting</option>
              <option value="UI/UX Design">UI/UX Design</option>
            </select>
          </div>

          <div>
            <label className="block text-slate-300 font-semibold mb-1">Experience: {expYears} Years</label>
            <input
              type="range"
              min="0"
              max="15"
              step="1"
              value={expYears}
              onChange={(e) => setExpYears(parseInt(e.target.value))}
              className="w-full h-2 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-red-500 mt-2"
            />
          </div>

          <div>
            <label className="block text-slate-300 font-semibold mb-1">Primary Location</label>
            <select
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-red-500"
            >
              <option value="Bengaluru">Bengaluru / Bangalore</option>
              <option value="Hyderabad">Hyderabad</option>
              <option value="Pune">Pune</option>
              <option value="Mumbai">Mumbai</option>
              <option value="Gurgaon">Gurgaon / Delhi NCR</option>
              <option value="Remote">Remote (Global USD)</option>
            </select>
          </div>
        </div>

      </GlassCard>

      {/* Salary Meter Display */}
      <div className="grid md:grid-cols-12 gap-6 items-start">
        
        {/* Main Salary Card (7 cols) */}
        <GlassCard className="md:col-span-7 p-6 space-y-6 border border-slate-800">
          <div className="flex justify-between items-start border-b border-slate-800/80 pb-4">
            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block">Predicted Salary Band</span>
              <h2 className="text-xl font-extrabold text-white mt-0.5">{role}</h2>
              <p className="text-xs text-slate-400">{domain} • {location} • {expYears} Years Exp</p>
            </div>
            <span className="px-3.5 py-1.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-xs font-bold text-emerald-400">
              AI Market Calibrated
            </span>
          </div>

          <div className="text-center py-4 space-y-2">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-widest block">Evidence-Based Salary Estimate</span>
            {sal ? (
              <>
                <div className="text-4xl sm:text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-red-400 via-amber-300 to-emerald-400 tracking-tight">
                  ₹{sal.medianLpa} LPA
                </div>
                <p className="text-xs text-slate-400">
                  USD equivalent: <span className="text-white font-bold">Available only with a live exchange-rate source</span>
                </p>
              </>
            ) : (
              <p className="text-sm text-slate-300">
                Salary estimates are available only with verified live market evidence from resume analysis.
              </p>
            )}
          </div>

          {/* Visual Range Bar */}
          <div className="space-y-2 pt-2">
            {sal ? (
              <>
                <div className="flex justify-between text-xs font-bold text-slate-300">
                  <span className="text-red-400">Min: ₹{sal.minLpa} LPA</span>
                  <span className="text-amber-300">Median: ₹{sal.medianLpa} LPA</span>
                  <span className="text-emerald-400">Max: ₹{sal.maxLpa} LPA</span>
                </div>
                <div className="w-full h-3 rounded-full bg-slate-900 overflow-hidden border border-slate-800 relative">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-red-500 via-amber-400 to-emerald-500"
                    style={{ width: '100%' }}
                  />
                </div>
              </>
            ) : (
              <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-4 text-xs text-slate-300">
                Salary range details are not available without verified salary evidence.
              </div>
            )}
          </div>
        </GlassCard>

        {/* Breakdown & Market Context (5 cols) */}
        <GlassCard className="md:col-span-5 p-6 space-y-4 border border-slate-800">
          <h3 className="font-extrabold text-xs text-red-400 uppercase tracking-wider flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-emerald-400" /> Market Compensation Drivers
          </h3>

          <div className="space-y-2.5 text-xs">
            <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
              <span className="text-slate-400">Domain Demand Multiplier</span>
              <span className="font-bold text-amber-300">Evidence-based market data</span>
            </div>

            <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
              <span className="text-slate-400">Education Credentials</span>
              <span className="font-bold text-slate-400">
                Verified evidence only
              </span>
            </div>

            <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
              <span className="text-slate-400">Location Tier</span>
              <span className="font-bold text-white">{location} Hub</span>
            </div>
          </div>
        </GlassCard>

      </div>
    </div>
  );
};
