import React, { useState, useCallback, useRef, useEffect } from 'react';
import { motion, AnimatePresence, useInView } from 'framer-motion';
import {
  Briefcase, MapPin, DollarSign, ExternalLink, Search, Filter,
  Zap, Wifi, Globe, Building2, RefreshCw, ChevronDown,
  Sparkles, Target, Clock, CheckCircle2, XCircle, Loader2, Check, AlertTriangle,
  TrendingUp, Star, ArrowUpRight, Code, Brain, Cpu, Megaphone, Database,
  BarChart3, Shield, Smartphone, Settings
} from 'lucide-react';
import axios from 'axios';
import { useResumeContext } from '../context/ResumeContext';

// ── Types ────────────────────────────────────────────────────────────────────

interface JobListing {
  name: string;
  title: string;
  location: string;
  salary: string;
  url: string;
  requiredSkills: string;
  source: string;
  explanation?: string;
  semanticScore?: string;
  matchPercentage?: string;
  similarityScore?: string;
  matchScore?: string;
  scope?: string;
  isIndia?: string;
  postedDate?: string;
  matchedSkills?: string;
  missingSkills?: string;
}

type ExperienceLevel = 'fresher' | 'junior' | 'mid-level' | 'senior' | 'lead';

// ── Domain Mapping ───────────────────────────────────────────────────────────

const DOMAIN_COLORS: Record<string, { bg: string; text: string; icon: React.FC<any>; glow: string }> = {
  'full stack': { bg: 'from-indigo-600/20 to-violet-600/15', text: 'text-indigo-300', icon: Code, glow: 'rgba(99,102,241,0.3)' },
  'software engineer': { bg: 'from-blue-600/20 to-cyan-600/15', text: 'text-blue-300', icon: Cpu, glow: 'rgba(59,130,246,0.3)' },
  'data scientist': { bg: 'from-purple-600/20 to-pink-600/15', text: 'text-purple-300', icon: BarChart3, glow: 'rgba(139,92,246,0.3)' },
  'ml engineer': { bg: 'from-violet-600/20 to-purple-600/15', text: 'text-violet-300', icon: Brain, glow: 'rgba(124,58,237,0.3)' },
  'digital marketing': { bg: 'from-pink-600/20 to-rose-600/15', text: 'text-pink-300', icon: Megaphone, glow: 'rgba(219,39,119,0.3)' },
  'devops': { bg: 'from-emerald-600/20 to-teal-600/15', text: 'text-emerald-300', icon: Settings, glow: 'rgba(16,185,129,0.3)' },
  'cybersecurity': { bg: 'from-red-600/20 to-orange-600/15', text: 'text-red-300', icon: Shield, glow: 'rgba(239,68,68,0.3)' },
  'android developer': { bg: 'from-green-600/20 to-emerald-600/15', text: 'text-green-300', icon: Smartphone, glow: 'rgba(34,197,94,0.3)' },
  'data analyst': { bg: 'from-cyan-600/20 to-blue-600/15', text: 'text-cyan-300', icon: Database, glow: 'rgba(6,182,212,0.3)' },
  'default': { bg: 'from-slate-600/20 to-slate-700/15', text: 'text-slate-300', icon: Briefcase, glow: 'rgba(99,102,241,0.25)' },
};

function getDomainStyle(title: string) {
  const t = title.toLowerCase();
  for (const [key, val] of Object.entries(DOMAIN_COLORS)) {
    if (t.includes(key)) return val;
  }
  return DOMAIN_COLORS['default'];
}

// ── Source Badge Colors ──────────────────────────────────────────────────────

const SOURCE_STYLES: Record<string, { bg: string; text: string; dot: string }> = {
  'adzuna live job api':     { bg: 'bg-blue-500/10',    text: 'text-blue-300',    dot: 'bg-blue-400'    },
  'remotive job api':        { bg: 'bg-green-500/10',   text: 'text-green-300',   dot: 'bg-green-400'   },
  'jsearch rapidapi':        { bg: 'bg-violet-500/10',  text: 'text-violet-300',  dot: 'bg-violet-400'  },
  'jooble live job api':     { bg: 'bg-amber-500/10',   text: 'text-amber-300',   dot: 'bg-amber-400'   },
  'greenhouse boards':       { bg: 'bg-emerald-500/10', text: 'text-emerald-300', dot: 'bg-emerald-400' },
  'lever boards':            { bg: 'bg-rose-500/10',    text: 'text-rose-300',    dot: 'bg-rose-400'    },
  'wellfound startup jobs':  { bg: 'bg-cyan-500/10',    text: 'text-cyan-300',    dot: 'bg-cyan-400'    },
  'rag market intelligence': { bg: 'bg-indigo-500/10',  text: 'text-indigo-300',  dot: 'bg-indigo-400'  },
};

function getSourceStyle(source: string) {
  return SOURCE_STYLES[source.toLowerCase()] ?? { bg: 'bg-slate-500/10', text: 'text-slate-300', dot: 'bg-slate-400' };
}

// ── Match Score Ring ─────────────────────────────────────────────────────────

const MatchRing: React.FC<{ score: number }> = ({ score }) => {
  const ref = useRef<SVGSVGElement>(null);
  const isInView = useInView(ref as any, { once: true });
  const color = score >= 75 ? '#10b981' : score >= 55 ? '#f59e0b' : '#ef4444';
  const circumference = 2 * Math.PI * 18;
  const label = score >= 75 ? 'Great' : score >= 55 ? 'Good' : 'Fair';

  return (
    <div className="flex flex-col items-center gap-0.5">
      <svg ref={ref} className="-rotate-90" width="44" height="44" viewBox="0 0 44 44">
        <circle cx="22" cy="22" r="18" fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="4" />
        <circle
          cx="22" cy="22" r="18"
          fill="none"
          stroke={color}
          strokeWidth="4"
          strokeLinecap="round"
          strokeDasharray={isInView ? `${(score / 100) * circumference} ${circumference}` : `0 ${circumference}`}
          style={{
            transition: 'stroke-dasharray 1.2s cubic-bezier(0.16,1,0.3,1) 0.3s',
            filter: `drop-shadow(0 0 4px ${color})`,
          }}
        />
        <text
          x="22" y="22"
          fill="white"
          fontSize="10"
          fontWeight="800"
          textAnchor="middle"
          dominantBaseline="central"
          transform="rotate(90, 22, 22)"
          style={{ fontFamily: 'Plus Jakarta Sans, sans-serif' }}
        >
          {score}
        </text>
      </svg>
      <span className="text-[9px] font-bold" style={{ color }}>{label}</span>
    </div>
  );
};

// ── Company Avatar ────────────────────────────────────────────────────────────

const CompanyAvatar: React.FC<{ name: string; style: any }> = ({ name, style }) => {
  const initials = name.split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase();
  return (
    <div
      className={`w-11 h-11 rounded-xl bg-gradient-to-br ${style.bg} border border-white/10 flex items-center justify-center shrink-0 font-black text-sm text-white`}
      style={{ boxShadow: `0 0 15px ${style.glow}` }}
    >
      {initials.slice(0, 2)}
    </div>
  );
};

// ── Job Card ─────────────────────────────────────────────────────────────────

const JobCard: React.FC<{ job: JobListing; index: number }> = ({ job, index }) => {
  const sourceStyle = getSourceStyle(job.source);
  const domainStyle = getDomainStyle(job.title);
  const DomainIcon = domainStyle.icon;

  const isIndia = job.isIndia === 'true' || job.scope === 'India'
    || ['india','bengaluru','hyderabad','pune','mumbai','gurugram','delhi','chennai','noida'].some(c => job.location.toLowerCase().includes(c));

  const matchedSkillsList = job.matchedSkills
    ? job.matchedSkills.split(',').map(s => s.trim()).filter(Boolean)
    : (job.requiredSkills ? job.requiredSkills.split(',').map(s => s.trim()).filter(Boolean).slice(0, 4) : []);

  const missingSkillsList = job.missingSkills && job.missingSkills !== 'None Critical'
    ? job.missingSkills.split(',').map(s => s.trim()).filter(Boolean).slice(0, 3)
    : [];

  const rawScore = job.matchScore ?? job.similarityScore ?? job.semanticScore ?? job.matchPercentage ?? '85';
  const scoreNum = Math.round(parseFloat(rawScore));

  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10, scale: 0.97 }}
      transition={{ duration: 0.4, delay: index * 0.05, ease: [0.16, 1, 0.3, 1] }}
      className="group relative bg-gradient-to-br from-[#09090f]/95 to-[#060608]/98 border border-slate-800/60 rounded-2xl p-5 job-card-hover overflow-hidden"
    >
      {/* Hover glow mesh */}
      <div className={`absolute inset-0 rounded-2xl bg-gradient-to-br ${domainStyle.bg} opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none`} />

      {/* India badge ribbon */}
      {isIndia && (
        <div className="absolute top-3 right-3 flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/25 text-[9px] font-bold text-amber-300">
          🇮🇳 India
        </div>
      )}

      <div className="relative z-10">
        {/* Header: Avatar + Title + Score */}
        <div className="flex items-start gap-3 mb-3">
          <CompanyAvatar name={job.name || 'Co'} style={domainStyle} />

          <div className="flex-1 min-w-0">
            <h3 className="text-sm font-bold text-white leading-tight truncate group-hover:text-white/90 transition-colors">
              {job.title}
            </h3>
            <div className="flex items-center gap-1.5 mt-0.5">
              <DomainIcon className={`w-3 h-3 ${domainStyle.text}`} />
              <p className="text-xs text-slate-400 truncate font-medium">{job.name}</p>
            </div>
          </div>

          {/* Match Score Ring */}
          <MatchRing score={scoreNum} />
        </div>

        {/* Meta row */}
        <div className="flex flex-wrap items-center gap-2 mb-3 text-xs text-slate-400">
          {job.location && (
            <span className="flex items-center gap-1">
              <MapPin className="w-3 h-3 text-indigo-400 shrink-0" />
              <span className="truncate max-w-[120px]">{job.location}</span>
            </span>
          )}
          {job.salary ? (
            <span className="flex items-center gap-1 text-emerald-400 font-bold">
              <DollarSign className="w-3 h-3" />
              {job.salary}
            </span>
          ) : (
            <span className="flex items-center gap-1 text-slate-500 font-medium">
              <DollarSign className="w-3 h-3" />
              Salary unavailable
            </span>
          )}
          {job.postedDate && (
            <span className="flex items-center gap-1 text-[10px] text-slate-500">
              <Clock className="w-3 h-3" />
              {job.postedDate}
            </span>
          )}
        </div>

        {/* Source badge */}
        <div className="flex items-center justify-between mb-3">
          <div className={`flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[9px] font-semibold ${sourceStyle.bg} ${sourceStyle.text}`}>
            <span className={`w-1.5 h-1.5 rounded-full ${sourceStyle.dot} animate-pulse`} />
            {job.source.replace(/ live job api| job api| boards| rapidapi/gi, '').trim()}
          </div>
          {!isIndia && (
            <span className="text-[9px] text-cyan-300 font-bold">🌍 Global</span>
          )}
        </div>

        {/* Matched Skills */}
        {matchedSkillsList.length > 0 && (
          <div className="flex flex-wrap gap-1 mb-2">
            {matchedSkillsList.slice(0, 5).map((skill, i) => (
              <span
                key={i}
                className="px-2 py-0.5 text-[10px] font-semibold rounded-md bg-emerald-950/40 text-emerald-300 border border-emerald-500/25 flex items-center gap-0.5"
              >
                <Check className="w-2.5 h-2.5" />
                {skill}
              </span>
            ))}
          </div>
        )}

        {/* Missing Skills */}
        {missingSkillsList.length > 0 && (
          <div className="flex flex-wrap gap-1 mb-3">
            {missingSkillsList.map((skill, i) => (
              <span
                key={i}
                className="px-2 py-0.5 text-[10px] font-medium rounded-md bg-rose-950/30 text-rose-300 border border-rose-500/20 flex items-center gap-0.5"
              >
                <AlertTriangle className="w-2.5 h-2.5" />
                {skill}
              </span>
            ))}
          </div>
        )}

        {/* AI Explanation */}
        {job.explanation && (
          <p className="text-[11px] text-slate-400 italic mb-3 line-clamp-2 leading-relaxed">{job.explanation}</p>
        )}

        {/* CTA */}
        <a
          href={job.url}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-xl bg-gradient-to-r from-indigo-600/25 to-violet-600/15 text-indigo-300 border border-indigo-500/30 hover:from-indigo-600/40 hover:to-violet-600/30 hover:text-indigo-200 hover:border-indigo-400/60 hover:shadow-lg hover:shadow-indigo-900/30 transition-all duration-250 btn-premium"
          onClick={e => e.stopPropagation()}
        >
          <ArrowUpRight className="w-3.5 h-3.5" />
          Apply Now
        </a>
      </div>
    </motion.div>
  );
};

// ── Domain Quick Select Chips ────────────────────────────────────────────────

const DOMAIN_CHIPS = [
  { label: 'Full Stack', icon: Code, color: 'text-indigo-300 border-indigo-500/30 bg-indigo-500/10 hover:bg-indigo-500/20' },
  { label: 'Digital Marketing', icon: Megaphone, color: 'text-pink-300 border-pink-500/30 bg-pink-500/10 hover:bg-pink-500/20' },
  { label: 'Data Scientist', icon: BarChart3, color: 'text-purple-300 border-purple-500/30 bg-purple-500/10 hover:bg-purple-500/20' },
  { label: 'ML Engineer', icon: Brain, color: 'text-violet-300 border-violet-500/30 bg-violet-500/10 hover:bg-violet-500/20' },
  { label: 'DevOps Engineer', icon: Settings, color: 'text-emerald-300 border-emerald-500/30 bg-emerald-500/10 hover:bg-emerald-500/20' },
  { label: 'Android Developer', icon: Smartphone, color: 'text-green-300 border-green-500/30 bg-green-500/10 hover:bg-green-500/20' },
  { label: 'Data Analyst', icon: Database, color: 'text-cyan-300 border-cyan-500/30 bg-cyan-500/10 hover:bg-cyan-500/20' },
  { label: 'Cybersecurity', icon: Shield, color: 'text-red-300 border-red-500/30 bg-red-500/10 hover:bg-red-500/20' },
];

// ── Empty State ──────────────────────────────────────────────────────────────

const EmptyState: React.FC<{ searched: boolean }> = ({ searched }) => (
  <motion.div
    initial={{ opacity: 0, scale: 0.95 }}
    animate={{ opacity: 1, scale: 1 }}
    className="col-span-full flex flex-col items-center justify-center py-24 text-center"
  >
    <div className="w-20 h-20 rounded-3xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center mb-5 shadow-[0_0_40px_rgba(99,102,241,0.15)]">
      <Briefcase className="w-9 h-9 text-indigo-400" />
    </div>
    <h3 className="text-lg font-bold text-white mb-2">
      {searched ? 'No matching jobs found' : 'Find Live Jobs Matched to Your Profile'}
    </h3>
    <p className="text-sm text-slate-500 max-w-xs leading-relaxed">
      {searched
        ? 'Try a different domain, location, or broaden your search.'
        : 'Enter your domain below or upload your resume to get AI-matched live job listings from 7+ real-time providers.'}
    </p>
  </motion.div>
);

// ── Experience Options ───────────────────────────────────────────────────────

const EXPERIENCE_OPTIONS: { value: ExperienceLevel; label: string }[] = [
  { value: 'fresher',   label: 'Fresher / Intern'   },
  { value: 'junior',   label: 'Junior (0–2 yrs)'   },
  { value: 'mid-level', label: 'Mid-Level (2–5 yrs)' },
  { value: 'senior',   label: 'Senior (5–8 yrs)'   },
  { value: 'lead',     label: 'Lead / Principal'   },
];

// ── Provider List ─────────────────────────────────────────────────────────────

const PROVIDERS = [
  { name: 'Adzuna', icon: Globe },
  { name: 'Remotive', icon: Wifi },
  { name: 'JSearch', icon: Zap },
  { name: 'Jooble', icon: Search },
  { name: 'Greenhouse', icon: Building2 },
  { name: 'Lever', icon: Briefcase },
  { name: 'Wellfound', icon: Sparkles },
];

// ── Main Page ────────────────────────────────────────────────────────────────

export const LiveJobsPage: React.FC = () => {
  const { aiAnalysisResult, rawText, apiKey: contextApiKey } = useResumeContext();

  const [resumeText,  setResumeText]  = useState('');
  const [domain,      setDomain]      = useState('');
  const [location,    setLocation]    = useState('');
  const [expLevel,    setExpLevel]    = useState<ExperienceLevel>('mid-level');
  const [apiKey,      setApiKey]      = useState('');
  const [jobs,        setJobs]        = useState<JobListing[]>([]);
  const [loading,     setLoading]     = useState(false);
  const [searched,    setSearched]    = useState(false);
  const [error,       setError]       = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [filterSource, setFilterSource] = useState('');

  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (rawText?.trim()) setResumeText(rawText.trim());
    if (aiAnalysisResult && typeof aiAnalysisResult === 'object') {
      if (aiAnalysisResult.careerDomain) setDomain(String(aiAnalysisResult.careerDomain));
      else if (aiAnalysisResult.role) setDomain(String(aiAnalysisResult.role));

      const lvl = String(aiAnalysisResult.careerLevel || '').toLowerCase();
      if (lvl.includes('fresher') || lvl.includes('intern')) setExpLevel('fresher');
      else if (lvl.includes('junior')) setExpLevel('junior');
      else if (lvl.includes('senior')) setExpLevel('senior');
      else if (lvl.includes('lead')) setExpLevel('lead');

      if (Array.isArray(aiAnalysisResult.retrievedJobOpportunities) && aiAnalysisResult.retrievedJobOpportunities.length > 0) {
        setJobs(aiAnalysisResult.retrievedJobOpportunities);
        setSearched(true);
      }
    }
  }, [rawText, aiAnalysisResult]);

  const filteredJobs = filterSource
    ? jobs.filter(j => j.source.toLowerCase().includes(filterSource.toLowerCase()))
    : jobs;

  const sources = [...new Set(jobs.map(j => j.source))];

  const handleSearch = useCallback(async () => {
    if (!resumeText.trim() && !domain.trim()) {
      setError('Please provide either resume text or a job domain to search.');
      return;
    }
    setError('');
    setLoading(true);
    setSearched(true);
    setJobs([]);

    if (abortRef.current) abortRef.current.abort();
    abortRef.current = new AbortController();

    try {
      const payload: Record<string, string> = {
        resumeText: resumeText.trim(),
        careerDomain: domain.trim() || 'Software Engineer',
        location: location.trim(),
        experienceLevel: expLevel,
      };
      if (apiKey.trim()) payload.apiKey = apiKey.trim();

      const res = await axios.post<JobListing[]>('/api/analyzer/jobs', payload, {
        signal: abortRef.current.signal,
        timeout: 45_000,
      });

      const data = Array.isArray(res.data) ? res.data : [];
      setJobs(data);
    } catch (err: unknown) {
      if (axios.isCancel(err)) return;
      const msg = (err as any)?.response?.data?.error ?? (err as any)?.message ?? 'Failed to fetch jobs.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [resumeText, domain, location, expLevel, apiKey]);

  // ── Render ──

  return (
    <div className="space-y-6 pb-16">

      {/* ── Hero Header ──────────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: -16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="relative rounded-3xl overflow-hidden p-8 bg-gradient-to-br from-[#07040f] via-[#050510] to-[#040508] border border-indigo-500/15"
      >
        {/* Background orbs */}
        <div className="absolute top-0 right-0 w-72 h-72 bg-indigo-500/8 rounded-full blur-[80px] float-orb pointer-events-none" />
        <div className="absolute bottom-0 left-0 w-48 h-48 bg-violet-500/6 rounded-full blur-[60px] float-orb-slow pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/25">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 live-badge" />
                <span className="text-[10px] font-black text-emerald-400 uppercase tracking-widest">Live Feed Active</span>
              </div>
              <span className="text-[10px] text-slate-500 font-mono">7 Providers · Real-time</span>
            </div>
            <h1 className="text-3xl md:text-4xl font-black text-white tracking-tight leading-tight">
              Live Job Market
              <span className="gradient-text-electric block text-2xl md:text-3xl">AI-Matched Opportunities</span>
            </h1>
            <p className="text-sm text-slate-400 mt-2 max-w-lg">
              VREZER AI analyzes your skills and matches you to live jobs from Adzuna, Remotive, JSearch, Jooble, Greenhouse, Lever & Wellfound — in real time.
            </p>
          </div>

          {/* Stats */}
          <div className="flex gap-4 shrink-0">
            {[
              { label: 'Providers', val: '7+', color: 'text-indigo-400' },
              { label: 'Jobs Fetched', val: jobs.length > 0 ? `${jobs.length}` : '∞', color: 'text-emerald-400' },
              { label: 'AI Matching', val: '100%', color: 'text-[#D4AF37]' },
            ].map(s => (
              <div key={s.label} className="text-center p-3 rounded-2xl bg-white/[0.04] border border-white/[0.07]">
                <div className={`text-xl font-black ${s.color}`}>{s.val}</div>
                <div className="text-[10px] text-slate-500 font-mono uppercase tracking-wider mt-0.5">{s.label}</div>
              </div>
            ))}
          </div>
        </div>
      </motion.div>

      {/* ── Domain Quick-Select Chips ─────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="flex flex-wrap gap-2"
      >
        <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest self-center mr-1">Quick Select:</span>
        {DOMAIN_CHIPS.map(chip => {
          const Icon = chip.icon;
          return (
            <button
              key={chip.label}
              onClick={() => setDomain(chip.label)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border transition-all duration-200 ${chip.color} ${domain === chip.label ? 'ring-1 ring-current scale-105' : 'hover:scale-105'}`}
            >
              <Icon className="w-3 h-3" />
              {chip.label}
            </button>
          );
        })}
      </motion.div>

      {/* ── Search Panel ─────────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15 }}
        className="bg-gradient-to-br from-[#09090f]/95 to-[#060608]/95 border border-slate-800/60 rounded-2xl p-6 space-y-5"
      >
        {/* Resume textarea */}
        <div>
          <label className="block text-xs font-bold text-slate-400 mb-2 uppercase tracking-wider">
            Resume Text <span className="text-slate-600 normal-case font-normal">(optional — powers AI semantic matching)</span>
          </label>
          <textarea
            id="live-jobs-resume-text"
            value={resumeText}
            onChange={e => setResumeText(e.target.value)}
            placeholder="Paste your resume here for AI-powered semantic job matching — Gemini & Llama analyze your skills and find the best matches…"
            rows={4}
            className="w-full px-4 py-3 text-sm bg-black/40 text-slate-200 placeholder-slate-600 border border-slate-700/40 rounded-xl resize-none focus:outline-none focus:border-indigo-500/50 focus:ring-1 focus:ring-indigo-500/20 transition-all font-mono"
          />
        </div>

        {/* Domain + Location row */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-bold text-slate-400 mb-2 uppercase tracking-wider">Job Domain / Role</label>
            <input
              id="live-jobs-domain"
              type="text"
              value={domain}
              onChange={e => setDomain(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              placeholder="e.g. Full Stack Developer, Digital Marketing, Data Scientist…"
              list="domain-suggestions-list"
              className="w-full px-4 py-2.5 text-sm bg-black/40 text-slate-200 placeholder-slate-600 border border-slate-700/40 rounded-xl focus:outline-none focus:border-indigo-500/50 focus:ring-1 focus:ring-indigo-500/20 transition-all"
            />
            <datalist id="domain-suggestions-list">
              {DOMAIN_CHIPS.map(d => <option key={d.label} value={d.label} />)}
            </datalist>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-400 mb-2 uppercase tracking-wider">Preferred Location</label>
            <div className="relative">
              <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
              <input
                id="live-jobs-location"
                type="text"
                value={location}
                onChange={e => setLocation(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSearch()}
                placeholder="e.g. Bengaluru, Remote, New York, Hyderabad…"
                className="w-full pl-9 pr-4 py-2.5 text-sm bg-black/40 text-slate-200 placeholder-slate-600 border border-slate-700/40 rounded-xl focus:outline-none focus:border-indigo-500/50 focus:ring-1 focus:ring-indigo-500/20 transition-all"
              />
            </div>
          </div>
        </div>

        {/* Advanced filters toggle */}
        <button
          onClick={() => setShowFilters(f => !f)}
          className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-300 transition-colors"
        >
          <Filter className="w-3.5 h-3.5" />
          Advanced Filters
          <motion.div animate={{ rotate: showFilters ? 180 : 0 }}>
            <ChevronDown className="w-3.5 h-3.5" />
          </motion.div>
        </button>

        <AnimatePresence>
          {showFilters && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="overflow-hidden"
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-1">
                <div>
                  <label className="block text-xs font-bold text-slate-400 mb-2 uppercase tracking-wider">Experience Level</label>
                  <select
                    value={expLevel}
                    onChange={e => setExpLevel(e.target.value as ExperienceLevel)}
                    className="w-full px-4 py-2.5 text-sm bg-black/40 text-slate-200 border border-slate-700/40 rounded-xl focus:outline-none focus:border-indigo-500/50 transition-all appearance-none"
                  >
                    {EXPERIENCE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-400 mb-2 uppercase tracking-wider">
                    Gemini API Key <span className="text-slate-600 normal-case font-normal">(semantic boost)</span>
                  </label>
                  <div className="relative">
                    <Sparkles className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-indigo-400" />
                    <input
                      type="password"
                      value={apiKey}
                      onChange={e => setApiKey(e.target.value)}
                      placeholder="AIza…"
                      className="w-full pl-9 pr-4 py-2.5 text-sm bg-black/40 text-slate-200 placeholder-slate-600 border border-slate-700/40 rounded-xl focus:outline-none focus:border-indigo-500/50 transition-all"
                    />
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Search CTA */}
        <div className="flex items-center gap-4">
          <motion.button
            id="live-jobs-search-btn"
            onClick={handleSearch}
            disabled={loading}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.97 }}
            className="flex items-center gap-2.5 px-7 py-3 text-sm font-black rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white disabled:opacity-60 disabled:cursor-not-allowed transition-all shadow-[0_0_25px_rgba(99,102,241,0.35)] btn-premium uppercase tracking-wide"
          >
            {loading ? (
              <><Loader2 className="w-4 h-4 animate-spin" /> Fetching Live Jobs…</>
            ) : (
              <><Search className="w-4 h-4" /> Search Live Jobs</>
            )}
          </motion.button>

          {jobs.length > 0 && (
            <div className="flex items-center gap-2 text-sm">
              <span className="text-emerald-400 font-black">{filteredJobs.length}</span>
              <span className="text-slate-500">of {jobs.length} listings</span>
            </div>
          )}
        </div>

        {/* Error */}
        {error && (
          <motion.div
            initial={{ opacity: 0, y: -5 }}
            animate={{ opacity: 1, y: 0 }}
            className="flex items-start gap-2 px-4 py-3 rounded-xl bg-rose-500/8 border border-rose-500/20 text-rose-300 text-sm"
          >
            <XCircle className="w-4 h-4 mt-0.5 shrink-0" />
            <span>{error}</span>
          </motion.div>
        )}
      </motion.div>

      {/* ── Source Filter Bar ─────────────────────────────────────────────────── */}
      <AnimatePresence>
        {searched && !loading && jobs.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 5 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="flex flex-wrap items-center gap-2 px-4 py-3 bg-black/40 border border-white/[0.06] rounded-xl text-xs"
          >
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
            <span className="text-slate-400 font-semibold mr-1">Live from:</span>
            {sources.map(src => {
              const style = getSourceStyle(src);
              return (
                <button
                  key={src}
                  onClick={() => setFilterSource(filterSource === src ? '' : src)}
                  className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full font-semibold transition-all ${style.bg} ${style.text} ${filterSource === src ? 'ring-1 ring-current scale-105' : 'hover:scale-105'}`}
                >
                  <span className={`w-1.5 h-1.5 rounded-full ${style.dot} animate-pulse`} />
                  {src.replace(/ live job api| job api| boards| rapidapi/gi, '').trim()}
                </button>
              );
            })}
            {filterSource && (
              <button
                onClick={() => setFilterSource('')}
                className="ml-auto text-slate-500 hover:text-slate-300 flex items-center gap-1 transition-colors"
              >
                <RefreshCw className="w-3 h-3" />
                Clear
              </button>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Jobs Grid ────────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <AnimatePresence mode="popLayout">
          {loading ? (
            Array.from({ length: 6 }).map((_, i) => (
              <motion.div
                key={`skel-${i}`}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ delay: i * 0.06 }}
                className="bg-[#09090f]/80 border border-slate-800/50 rounded-2xl p-5 space-y-4"
              >
                <div className="flex gap-3 items-center">
                  <div className="w-11 h-11 rounded-xl skeleton" />
                  <div className="flex-1 space-y-2">
                    <div className="h-4 skeleton rounded-lg w-3/4" />
                    <div className="h-3 skeleton rounded-lg w-1/2" />
                  </div>
                  <div className="w-11 h-11 skeleton rounded-full" />
                </div>
                <div className="h-3 skeleton rounded w-full" />
                <div className="flex gap-2">
                  {[1,2,3].map(j => <div key={j} className="h-5 w-16 skeleton rounded-md" />)}
                </div>
                <div className="h-8 w-28 skeleton rounded-xl" />
              </motion.div>
            ))
          ) : filteredJobs.length > 0 ? (
            filteredJobs.map((job, i) => (
              <JobCard key={`${job.name}-${job.title}-${i}`} job={job} index={i} />
            ))
          ) : (
            <EmptyState searched={searched} />
          )}
        </AnimatePresence>
      </div>

      {/* ── Provider Legends (pre-search) ────────────────────────────────────── */}
      {!searched && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.25 }}
          className="flex flex-wrap gap-2 justify-center pt-4"
        >
          {PROVIDERS.map(({ name, icon: Icon }) => (
            <div
              key={name}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/[0.04] border border-white/[0.07] text-xs text-slate-400 hover:text-slate-200 hover:bg-white/[0.07] transition-colors"
            >
              <Icon className="w-3 h-3 text-indigo-400" />
              {name}
            </div>
          ))}
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-indigo-500/8 border border-indigo-500/20 text-xs text-indigo-400">
            <TrendingUp className="w-3 h-3" />
            Real-time · Parallel fetch · Gemini+Llama ranked
          </div>
        </motion.div>
      )}
    </div>
  );
};

export default LiveJobsPage;
