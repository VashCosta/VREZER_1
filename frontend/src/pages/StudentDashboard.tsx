import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import {
  Zap, Building2, Cpu, Code, Brain, ShieldCheck, Target, Search, MapPin, Globe,
  Sparkles, CheckCircle2, AlertTriangle, ArrowRight, User, BookOpen, Award, Layers,
  TrendingUp, Download, CheckSquare, MessageSquare, Briefcase, Star, HelpCircle, Sun, Moon, Bot,
  PieChart as PieIcon, Map as MapIcon, BarChart3, DollarSign, Activity, Megaphone
} from 'lucide-react';
import { useResumeContext } from '../context/ResumeContext';
import { useTheme } from '../context/ThemeContext';
import { api } from '../lib/api';
import { motion, AnimatePresence, useInView } from 'framer-motion';
import {
  RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar,
  ResponsiveContainer, PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, LineChart, Line
} from 'recharts';

import { ExplainableScoreCard } from '../components/dashboard/ExplainableScoreCard';
import { HiringLocationsMap } from '../components/dashboard/HiringLocationsMap';
import { DownloadCenter } from '../components/dashboard/DownloadCenter';
import { ResumeHistoryComparisonModal } from '../components/dashboard/ResumeHistoryComparisonModal';
import { JobOpportunitiesPanel } from '../components/dashboard/JobOpportunitiesPanel';

export const StudentDashboard: React.FC = () => {
  const { aiAnalysisResult, setAiAnalysisResult, rawText, setRawText, addResumeToHistory, resumeHistory, apiKey } = useResumeContext();
  const { isDark, toggleTheme } = useTheme();

  const [activeTab, setActiveTab] = useState<
    'overview' | 'profile' | 'ats' | 'skills' | 'jobs_companies' | 'jobs' | 'companies' |
    'market' | 'career' | 'interview' | 'resume_ai' | 'analytics' | 'downloads' | 'debug'
  >('overview');

  const [jdInput, setJdInput] = useState("");
  const [isMatching, setIsMatching] = useState(false);
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [activeFaq, setActiveFaq] = useState<number | null>(null);

  // Interactive AI Bullet Rewriter State
  const [customBulletInput, setCustomBulletInput] = useState("");
  const [bulletTone, setBulletTone] = useState("Quantified Impact (Metrics + Action Verbs)");
  const [isRewritingBullet, setIsRewritingBullet] = useState(false);
  const [rewrittenBulletResult, setRewrittenBulletResult] = useState<{ original: string; aiRewritten: string; reasoning: string } | null>(null);

  // Interactive Interview Prep State
  const [activeInterviewQIdx, setActiveInterviewQIdx] = useState(0);
  const [userInterviewAnswer, setUserInterviewAnswer] = useState("");
  const [isEvaluatingAnswer, setIsEvaluatingAnswer] = useState(false);
  const [interviewFeedback, setInterviewFeedback] = useState<{ score: number; remark: string; improvement: string } | null>(null);

  // Live Bullet Rewriter Handler
  const handleRewriteCustomBullet = async () => {
    if (!customBulletInput.trim()) return;
    setIsRewritingBullet(true);
    try {
      const activeKey = apiKey || localStorage.getItem('vrezerApiKey') || '';
      const prompt = `You are a Principal Technical Recruiter and Resume Optimizer. Rewrite the following weak resume bullet point into a high-impact, professional resume bullet point following the STAR / XYZ formula (Accomplished [X] as measured by [Y], by doing [Z]). Target Tone: ${bulletTone}.\n\nOriginal Bullet Point: "${customBulletInput}"\n\nFormat your response ONLY as JSON with keys: "aiRewritten" and "reasoning".`;
      
      const response = await api.post('/api/chat/ask', {
        prompt,
        apiKey: activeKey,
        candidateContext: { role: aiAnalysisResult?.role, domain: aiAnalysisResult?.careerDomain }
      });

      const reply = response.data?.data?.reply || "";
      let jsonParsed: any = null;
      try {
        const jsonMatch = reply.match(/\{[\s\S]*\}/);
        if (jsonMatch) jsonParsed = JSON.parse(jsonMatch[0]);
      } catch (e) {}

      if (jsonParsed && jsonParsed.aiRewritten) {
        setRewrittenBulletResult({
          original: customBulletInput,
          aiRewritten: jsonParsed.aiRewritten,
          reasoning: jsonParsed.reasoning || "Optimized with quantifiable impact and strong action verbs."
        });
      } else {
        alert("Bullet optimization failed. Please verify AI backend API availability.");
      }
    } catch (err) {
      alert("Failed to connect to AI engine to rewrite bullet point.");
    } finally {
      setIsRewritingBullet(false);
    }
  };

  // Live Interview Evaluation Handler
  const handleEvaluateInterviewAnswer = async (questionText: string, modelAnswerText: string) => {
    if (!userInterviewAnswer.trim()) return;
    setIsEvaluatingAnswer(true);
    try {
      const activeKey = apiKey || localStorage.getItem('vrezerApiKey') || '';
      const prompt = `You are a Technical Interviewer evaluating a candidate's answer.\n\nQuestion: "${questionText}"\nReference Answer: "${modelAnswerText}"\nCandidate Answer: "${userInterviewAnswer}"\n\nEvaluate the candidate answer on technical depth, clarity, and relevance. Respond ONLY as JSON with keys: "score" (integer 0-100), "remark" (short evaluation), "improvement" (one concrete tip to reach 100%).`;
      
      const response = await api.post('/api/chat/ask', {
        prompt,
        apiKey: activeKey,
        candidateContext: { role: aiAnalysisResult?.role }
      });

      const reply = response.data?.data?.reply || "";
      let jsonParsed: any = null;
      try {
        const jsonMatch = reply.match(/\{[\s\S]*\}/);
        if (jsonMatch) jsonParsed = JSON.parse(jsonMatch[0]);
      } catch (e) {}

      if (jsonParsed && typeof jsonParsed.score === 'number') {
        setInterviewFeedback(jsonParsed);
      } else {
        setInterviewFeedback(null);
        alert("Interview evaluation did not return a verified score. Please try again.");
      }
    } catch (e) {
      setInterviewFeedback(null);
      alert("Interview evaluation is unavailable right now. Please try again.");
    } finally {
      setIsEvaluatingAnswer(false);
    }
  };

  // ─── STRICTLY NO SAMPLE/DEMO DATA ────────────────────────────────────────
  // The dashboard ONLY renders real AI analysis results from the backend.
  // If aiAnalysisResult is empty, the user is prompted to upload their resume.
  // DO NOT auto-populate with fake data.

  useEffect(() => {
    // Nothing to auto-load. User must upload a resume to get real AI data.
  }, []);

  if (!aiAnalysisResult || typeof aiAnalysisResult !== 'object' || Object.keys(aiAnalysisResult).length === 0
      || aiAnalysisResult.status === 'FAILED' || !!aiAnalysisResult.error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[70vh] space-y-10 text-center px-4 relative">
        {/* Ambient bg orbs */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-96 h-96 bg-[#C1121F]/5 rounded-full blur-[120px] pointer-events-none float-orb" />
        <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-64 h-64 bg-indigo-600/5 rounded-full blur-[80px] float-orb-slow pointer-events-none" />

        <motion.div
          initial={{ scale: 0.7, opacity: 0, y: 20 }}
          animate={{ scale: 1, opacity: 1, y: 0 }}
          transition={{ type: 'spring', stiffness: 180, damping: 15 }}
          className="relative"
        >
          <div className="w-32 h-32 rounded-3xl bg-gradient-to-br from-[#C1121F]/20 to-[#D4AF37]/10 border border-[#C1121F]/30 flex items-center justify-center shadow-[0_0_60px_rgba(193,18,31,0.25)] relative">
            <div className="absolute inset-0 rounded-3xl bg-gradient-to-tr from-[#C1121F]/10 to-transparent" />
            <Zap className="w-14 h-14 text-[#C1121F]" />
          </div>
          <motion.span
            animate={{ scale: [1, 1.4, 1], opacity: [1, 0, 1] }}
            transition={{ duration: 1.5, repeat: Infinity }}
            className="absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-[#D4AF37]"
          />
        </motion.div>

        <div className="space-y-4 max-w-xl relative z-10">
          <motion.h2
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="text-4xl font-black text-white tracking-tight"
          >
            Launch Your AI Career Analysis
          </motion.h2>
          <motion.p
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="text-sm text-slate-400 leading-relaxed"
          >
            Upload your resume to unlock your complete AI-powered Career Intelligence Dashboard — ATS score, salary benchmarks, live jobs, company matches, skill gaps, interview prep and more.
          </motion.p>
          {aiAnalysisResult?.error && (
            <div className="px-4 py-3 rounded-xl bg-rose-500/10 border border-rose-500/25 text-rose-300 text-xs font-mono">
              ⚠️ {String(aiAnalysisResult.error)}
            </div>
          )}
        </div>

        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <Link to="/upload">
            <motion.button
              whileHover={{ scale: 1.05, boxShadow: '0 0 40px rgba(193,18,31,0.5)' }}
              whileTap={{ scale: 0.97 }}
              className="px-10 py-4 rounded-2xl bg-gradient-to-r from-[#C1121F] via-[#E63946] to-[#D4AF37] text-white font-black text-sm uppercase tracking-widest shadow-[0_0_30px_rgba(193,18,31,0.4)] flex items-center gap-3 transition-all btn-premium"
            >
              <Zap className="w-5 h-5" /> Upload &amp; Analyze Resume
              <ArrowRight className="w-4 h-4" />
            </motion.button>
          </Link>
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.55 }}
          className="grid grid-cols-2 sm:grid-cols-4 gap-3 w-full max-w-3xl"
        >
          {[
            { icon: <ShieldCheck className="w-5 h-5 text-emerald-400" />, label: 'ATS Score', desc: 'AI-computed ring visualization', color: 'border-emerald-500/20 bg-emerald-500/5' },
            { icon: <Building2 className="w-5 h-5 text-indigo-400" />, label: 'Live Companies', desc: 'Matched from job boards', color: 'border-indigo-500/20 bg-indigo-500/5' },
            { icon: <Brain className="w-5 h-5 text-violet-400" />, label: 'Skill Intelligence', desc: 'Gemini + Llama analysis', color: 'border-violet-500/20 bg-violet-500/5' },
            { icon: <DollarSign className="w-5 h-5 text-[#D4AF37]" />, label: 'Salary Intelligence', desc: 'Real market benchmarks', color: 'border-[#D4AF37]/20 bg-[#D4AF37]/5' },
          ].map((feat, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.6 + i * 0.08 }}
              whileHover={{ y: -4 }}
              className={`flex flex-col items-center gap-2 p-4 rounded-2xl border ${feat.color} transition-all duration-300`}
            >
              {feat.icon}
              <span className="text-xs font-bold text-white">{feat.label}</span>
              <span className="text-[10px] text-slate-500 leading-tight">{feat.desc}</span>
            </motion.div>
          ))}
        </motion.div>

      </div>
    );
  }

  const handleMatchJD = async () => {
    if (!jdInput.trim()) return;
    setIsMatching(true);
    try {
      const payload = {
        resumeText: rawText || "CANDIDATE RESUME TEXT",
        jobDescription: jdInput
      };
      const response = await api.post('/api/analyzer/analyze', payload, {
        headers: { 'Content-Type': 'application/json' }
      });
      if (response.data && response.data.name) {
        setAiAnalysisResult(response.data);
      } else if (response.data && response.data.error) {
        alert("JD Matcher Error: " + response.data.error);
      }
    } catch (err: any) {
      console.error(err);
      alert("Failed to process Job Description Match: " + (err.response?.data?.error || err.message));
    } finally {
      setIsMatching(false);
    }
  };

  // Safe data accessors — use null for missing values rather than fabricated defaults
  const name = aiAnalysisResult.name || 'Candidate';
  const role = aiAnalysisResult.role || null;
  const careerDomain = aiAnalysisResult.careerDomain || null;
  const careerLevel = aiAnalysisResult.careerLevel || null;
  const education = aiAnalysisResult.education || null;
  const cgpa = aiAnalysisResult.cgpa || null;
  const atsScore = aiAnalysisResult.atsScore ?? null;
  const atsText = aiAnalysisResult.atsScoreText || null;
  const summary = aiAnalysisResult.professionalSummary || null;
  const forecast = aiAnalysisResult.strategicForecast || null;
  const aiModelUsed = aiAnalysisResult.aiModelUsed || 'AI Pipeline';

  // Scores — null means AI didn't return this score (not zero, not fabricated)
  const atsDetail = aiAnalysisResult.atsScoreDetails || null;
  const qualityDetail = aiAnalysisResult.resumeQualityScoreDetails || null;
  const techDetail = aiAnalysisResult.technicalSkillsScoreDetails || null;
  const commDetail = aiAnalysisResult.communicationScoreDetails || null;
  const projDetail = aiAnalysisResult.projectQualityScoreDetails || null;
  const portfolioDetail = aiAnalysisResult.portfolioReadinessScoreDetails || null;
  const recruiterDetail = aiAnalysisResult.recruiterReadinessScoreDetails || null;
  const readinessDetail = aiAnalysisResult.careerReadinessScoreDetails || null;

  // Chart Data — built from real ATS breakdown dimensions, never hardcoded
  const atsBreakdown = (atsDetail as any)?.scoreBreakdown || {};
  const radarData = atsScore !== null ? [
    { subject: 'Skills',      score: atsBreakdown.skillsScore            ?? atsScore },
    { subject: 'Keywords',    score: atsBreakdown.keywordOptimizationScore ?? (techDetail?.score ?? 0) },
    { subject: 'Experience',  score: atsBreakdown.experienceScore         ?? (commDetail?.score ?? 0) },
    { subject: 'Projects',    score: atsBreakdown.projectsScore           ?? (projDetail?.score ?? 0) },
    { subject: 'Portfolio',   score: portfolioDetail?.score               ?? 0 },
    { subject: 'Recruiter',   score: recruiterDetail?.score               ?? 0 }
  ] : null;

  const donutData = atsScore !== null ? [
    { name: 'ATS Match',      value: atsScore,                color: '#ef4444' },
    { name: 'Tech Caliber',   value: techDetail?.score    ?? 0, color: '#10b981' },
    { name: 'Project Quality',value: projDetail?.score    ?? 0, color: '#f59e0b' },
    { name: 'Portfolio',      value: portfolioDetail?.score ?? 0, color: '#6366f1' }
  ] : null;

  // barData — derive from actual detected skills, no fake fallback labels
  const topSkillsArray: string[] = aiAnalysisResult.topSkills || [];
  const barData = aiAnalysisResult.skillIntelligence?.technicalCompetencyChart
    ? (aiAnalysisResult.skillIntelligence.technicalCompetencyChart as any[]).map((item: any) => ({
        name: item.skill,
        score: item.score
      }))
    : null; // null = don't render bar chart if no verified skill score data


  const trajectoryData = aiAnalysisResult.careerGrowthTimeline
    ? (aiAnalysisResult.careerGrowthTimeline as any[]).map((t: any, i: number) => ({
        month: t.stage || `Stage ${i + 1}`,
        score: t.projectedScore || t.score || null,
        ats: t.projectedAts || null
      }))
    : null;

  return (
    <div className="space-y-8 font-sans pb-16 text-slate-100 selection:bg-red-500 selection:text-white">
      
      {/* ─── 1. HEADER — HERO ATS SCORE + SALARY BANNER ───────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: -15 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative rounded-3xl overflow-hidden p-6 lg:p-8 border border-[#D4AF37]/25 bg-gradient-to-br from-[#0c0508] via-[#050505] to-[#0a0508] space-y-6 shadow-[0_0_50px_rgba(212,175,55,0.12)]"
      >
        {/* Ambient orbs */}
        <div className="absolute top-0 right-0 w-96 h-96 bg-gradient-to-bl from-[#C1121F]/15 via-[#D4AF37]/8 to-transparent blur-[90px] rounded-full pointer-events-none float-orb" />
        <div className="absolute -bottom-20 -left-20 w-64 h-64 bg-indigo-600/5 rounded-full blur-[80px] float-orb-slow pointer-events-none" />

        <div className="relative z-10 flex flex-col lg:flex-row gap-6 items-start">

          {/* Left: Avatar + Identity */}
          <div className="flex items-start gap-5 flex-1">
            <div className="w-20 h-20 md:w-24 md:h-24 shrink-0 bg-gradient-to-br from-[#1c0814] to-[#0a0308] rounded-2xl border border-[#C1121F]/50 flex items-center justify-center shadow-[0_0_30px_rgba(193,18,31,0.35)] relative overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-tr from-[#C1121F]/25 to-transparent" />
              <span className="text-3xl md:text-4xl font-black text-white">{name.charAt(0)}</span>
            </div>

            <div className="space-y-1.5">
              <div className="flex flex-wrap items-center gap-2">
                <span className="px-3 py-1 bg-[#C1121F]/15 text-[#C1121F] border border-[#C1121F]/35 rounded-full text-[10px] font-mono font-black uppercase tracking-widest">
                  VREZER AI PLATFORM
                </span>
                {careerLevel && (
                  <span className="px-3 py-1 bg-[#D4AF37]/10 text-[#D4AF37] border border-[#D4AF37]/30 rounded-full text-[10px] font-mono font-bold uppercase tracking-widest">
                    {careerLevel}
                  </span>
                )}
                  <span className="px-3 py-1 bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 rounded-full text-[10px] font-mono font-bold">
                    {aiAnalysisResult.confidenceScore != null ? `${aiAnalysisResult.confidenceScore}% AI Confidence` : 'Not available'}
                  </span>
              </div>
              <div className="text-xs font-mono text-slate-500">Welcome back,</div>
              <h1 className="text-3xl lg:text-4xl font-black text-white tracking-tight leading-none">{name}</h1>
              <div className="text-sm font-extrabold text-[#D4AF37] font-mono">{role}</div>
              <div className="text-slate-400 font-mono text-xs flex flex-wrap items-center gap-2 mt-1">
                <span className="px-2 py-0.5 rounded bg-white/5 border border-white/8">📍 {aiAnalysisResult.preferredLocation || aiAnalysisResult.location || 'India'}</span>
                <span className="px-2 py-0.5 rounded bg-white/5 border border-white/8">🎓 {careerDomain}</span>
                {aiAnalysisResult.experience && <span className="px-2 py-0.5 rounded bg-white/5 border border-white/8">⚡ {aiAnalysisResult.experience}</span>}
              </div>
            </div>
          </div>

          {/* Right: ATS Hero Ring + Controls */}
          <div className="flex flex-row lg:flex-col items-center lg:items-end gap-5 shrink-0">
            {/* Controls */}
            <div className="flex items-center gap-2">
              {resumeHistory && resumeHistory.length > 0 && (
                <button
                  onClick={() => setShowHistoryModal(true)}
                  className="p-2.5 rounded-xl bg-[#C1121F]/10 border border-[#C1121F]/25 hover:bg-[#C1121F]/20 text-rose-300 transition-all flex items-center gap-2 text-xs font-mono font-bold"
                >
                  <Layers className="w-4 h-4 text-[#D4AF37]" />
                  <span>v{resumeHistory.length}</span>
                </button>
              )}
              <button
                onClick={toggleTheme}
                className="p-2.5 rounded-xl bg-white/5 border border-white/8 hover:border-[#C1121F]/40 text-slate-300 hover:text-white transition-all flex items-center gap-2 text-xs font-mono"
              >
                {isDark ? <Sun className="w-4 h-4 text-[#D4AF37]" /> : <Moon className="w-4 h-4 text-indigo-400" />}
              </button>
            </div>

            {/* ATS HERO RING */}
            {atsScore != null && (
              <div className="flex flex-col items-center gap-1">
                <div className="text-[9px] font-black text-slate-500 uppercase tracking-widest mb-1">ATS Score</div>
                <div className="relative w-24 h-24 lg:w-28 lg:h-28">
                  <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
                    <circle cx="50" cy="50" r="42" fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="8" />
                    <circle
                      cx="50" cy="50" r="42"
                      fill="none"
                      stroke={atsScore >= 75 ? '#10b981' : atsScore >= 60 ? '#f59e0b' : '#ef4444'}
                      strokeWidth="8"
                      strokeLinecap="round"
                      strokeDasharray={`${(atsScore / 100) * (2 * Math.PI * 42)} ${2 * Math.PI * 42}`}
                      style={{
                        transition: 'stroke-dasharray 1.8s cubic-bezier(0.16,1,0.3,1) 0.5s',
                        filter: `drop-shadow(0 0 10px ${atsScore >= 75 ? 'rgba(16,185,129,0.7)' : atsScore >= 60 ? 'rgba(245,158,11,0.7)' : 'rgba(239,68,68,0.7)'})`,
                      }}
                    />
                  </svg>
                  <div className="absolute inset-0 flex flex-col items-center justify-center">
                    <span className={`text-2xl font-black ${atsScore >= 75 ? 'text-emerald-400' : atsScore >= 60 ? 'text-amber-400' : 'text-rose-400'}`}>{atsScore}</span>
                    <span className="text-[9px] text-slate-500 font-mono">/100</span>
                  </div>
                </div>
                <span className={`text-[10px] font-black uppercase tracking-wider ${atsScore >= 75 ? 'text-emerald-400' : atsScore >= 60 ? 'text-amber-400' : 'text-rose-400'}`}>
                  {atsScore >= 75 ? '✓ ATS Ready' : atsScore >= 60 ? '~ Moderate' : '⚠ Low ATS'}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* SALARY + QUICK STAT BANNER */}
        <div className="relative z-10 grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            {
              label: 'Expected Salary',
              value: aiAnalysisResult.candidateSalaryEstimate?.status === 'OK' && aiAnalysisResult.candidateSalaryEstimate?.median != null
                ? `₹${aiAnalysisResult.candidateSalaryEstimate.median} LPA`
                : aiAnalysisResult.tier1?.salary ?? null,
              icon: DollarSign,
              color: 'text-[#D4AF37]',
              bg: 'bg-[#D4AF37]/8 border-[#D4AF37]/20',
            },
            {
              label: 'Domain Match',
              value: careerDomain,
              icon: Target,
              color: 'text-indigo-400',
              bg: 'bg-indigo-500/8 border-indigo-500/20',
            },
            {
              label: 'Top Skills',
              value: `${(aiAnalysisResult.topSkills || []).length} Detected`,
              icon: Activity,
              color: 'text-emerald-400',
              bg: 'bg-emerald-500/8 border-emerald-500/20',
            },
            {
              label: 'Career Readiness',
              value: readinessDetail?.score != null ? `${readinessDetail.score}/100` : atsScore != null ? `${atsScore}/100` : '—',
              icon: ShieldCheck,
              color: 'text-[#C1121F]',
              bg: 'bg-[#C1121F]/8 border-[#C1121F]/20',
            },
          ].map((s, i) => {
            const Icon = s.icon;
            return (
              <motion.div
                key={i}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 + i * 0.07 }}
                whileHover={{ y: -3, scale: 1.02 }}
                className={`p-3.5 rounded-2xl border ${s.bg} space-y-1.5 transition-all duration-300`}
              >
                <div className="flex items-center gap-1.5">
                  <Icon className={`w-3.5 h-3.5 ${s.color}`} />
                  <span className="text-[9px] font-bold text-slate-500 uppercase tracking-wider">{s.label}</span>
                </div>
                <div className={`text-sm font-black ${s.color} truncate`}>{s.value}</div>
              </motion.div>
            );
          })}
        </div>

        {/* AI Professional Summary */}
        {summary && (
          <div className="relative z-10 p-4 rounded-2xl bg-black/50 border border-white/[0.07] text-xs text-slate-300 leading-relaxed font-sans">
            <strong className="text-[#C1121F] uppercase tracking-wider font-mono text-[10px] block mb-1">
              ⚡ Executive Candidate Dossier Summary:
            </strong>
            {summary}
          </div>
        )}
      </motion.div>


      {/* ─── 2. STICKY PREMIUM NAVIGATION TABS ───────────────────────────────── */}
      <div className="sticky top-4 z-40 bg-[#060408]/95 backdrop-blur-2xl border border-white/[0.07] rounded-2xl p-1.5 flex items-center gap-0.5 overflow-x-auto shadow-[0_8px_32px_rgba(0,0,0,0.8)] no-scrollbar">
        {[
          { id: 'overview', label: 'Overview', icon: PieIcon, color: 'text-[#D4AF37]' },
          { id: 'ats', label: 'ATS Score', icon: ShieldCheck, color: 'text-emerald-400' },
          { id: 'skills', label: 'Skill Radar', icon: Target, color: 'text-indigo-400' },
          { id: 'jobs_companies', label: 'Live Jobs', icon: Briefcase, color: 'text-[#C1121F]' },
          { id: 'market', label: 'Market Intel', icon: TrendingUp, color: 'text-amber-400' },
          { id: 'career', label: 'Career Path', icon: MapIcon, color: 'text-emerald-400' },
          { id: 'interview', label: 'Interview AI', icon: MessageSquare, color: 'text-violet-400' },
          { id: 'resume_ai', label: 'Resume AI', icon: Sparkles, color: 'text-amber-400' },
          { id: 'analytics', label: 'Analytics', icon: BarChart3, color: 'text-rose-400' },
          { id: 'downloads', label: 'Downloads', icon: Download, color: 'text-slate-400' },
          { id: 'debug', label: 'Debug', icon: Cpu, color: 'text-slate-500' }
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <motion.button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              whileHover={{ scale: 1.03 }}
              whileTap={{ scale: 0.97 }}
              className={`px-3 py-2 rounded-xl whitespace-nowrap transition-all flex items-center gap-1.5 text-xs font-semibold relative ${
                isActive
                  ? 'bg-gradient-to-r from-[#C1121F]/90 to-rose-600/80 text-white font-black shadow-[0_0_16px_rgba(193,18,31,0.5)]'
                  : 'text-slate-400 hover:text-white hover:bg-white/[0.05]'
              }`}
            >
              <Icon className={`w-3.5 h-3.5 shrink-0 ${isActive ? 'text-white' : tab.color}`} />
              <span>{tab.label}</span>
              {tab.id === 'jobs_companies' && (
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 shrink-0 animate-pulse" />
              )}
            </motion.button>
          );
        })}
      </div>


      {/* ─── 3. SECTION CONTENT SWITCHER ────────────────────────────────────── */}
      <AnimatePresence mode="wait">
        
        {/* OVERVIEW TAB (MAIN 9-SECTION HIERARCHY) */}
        {activeTab === 'overview' && (
          <motion.div
            key="overview"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-8"
          >
            {/* 1. AI CAREER PREDICTION ("Who You Are") */}
            <div className="glass-card rounded-3xl p-6 lg:p-8 border border-[#D4AF37]/30 bg-gradient-to-br from-[#0c0508] via-[#050505] to-[#0a0508] relative overflow-hidden space-y-4 shadow-[0_0_30px_rgba(212,175,55,0.1)]">
              <div className="flex items-center justify-between">
                <h2 className="text-xs font-mono font-bold text-[#D4AF37] uppercase tracking-widest flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-[#D4AF37]" /> 1. AI Career Prediction — Who You Are
                </h2>
                <span className="px-3 py-1 bg-[#D4AF37]/10 text-[#D4AF37] border border-[#D4AF37]/30 rounded-full text-[10px] font-mono font-bold">
                  {careerDomain} Domain
                </span>
              </div>

              <div className="space-y-2">
                <h3 className="text-xl lg:text-2xl font-black text-white leading-tight font-sans">
                  {aiAnalysisResult.careerPrediction?.professionalIdentity || `${name} is a ${careerDomain} professional.`}
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed font-sans font-medium">
                  {forecast || summary || 'Analyzed candidate career trajectory and domain skills.'}
                </p>
              </div>

              <div className="grid md:grid-cols-3 gap-4 pt-2 font-mono text-xs">
                <div className="p-3.5 rounded-2xl bg-black/60 border border-white/10">
                  <span className="text-[10px] font-bold text-[#C1121F] uppercase tracking-wider block mb-1">Strongest Competencies</span>
                  <div className="text-white font-bold">{aiAnalysisResult.topSkills && aiAnalysisResult.topSkills.length > 0 ? aiAnalysisResult.topSkills.slice(0, 4).join(', ') : 'Core Domain Skills'}</div>
                </div>
                <div className="p-3.5 rounded-2xl bg-black/60 border border-white/10">
                  <span className="text-[10px] font-bold text-[#D4AF37] uppercase tracking-wider block mb-1">Target Role Fit</span>
                  <div className="text-white font-bold">{role || 'Specialist'}</div>
                </div>
                <div className="p-3.5 rounded-2xl bg-black/60 border border-white/10">
                  <span className="text-[10px] font-bold text-emerald-400 uppercase tracking-wider block mb-1">Strategic Recommendation</span>
                  <div className="text-white font-bold">{aiAnalysisResult.careerPrediction?.recommendedNextStep || 'Target market-leading domain employers'}</div>
                </div>
              </div>
            </div>

            {/* 2. ATS INTELLIGENCE */}
            <div className="glass-card rounded-3xl p-6 border border-white/10 space-y-6">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2 font-mono">
                  <ShieldCheck className="w-5 h-5 text-[#C1121F]" /> 2. ATS Intelligence & Score Audit
                </h2>
                <span className="px-3 py-1 rounded-full bg-[#C1121F]/10 text-[#C1121F] border border-[#C1121F]/30 text-xs font-mono font-bold">
                  Score: {atsScore != null ? `${atsScore}/100` : 'Evaluating'}
                </span>
              </div>

              <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {atsScore != null && atsDetail && <ExplainableScoreCard title="ATS Score" score={atsScore} detail={atsDetail} icon={<ShieldCheck className="w-5 h-5 text-emerald-400" />} />}
                {qualityDetail?.score != null && <ExplainableScoreCard title="Resume Quality" score={qualityDetail.score} detail={qualityDetail} icon={<Award className="w-5 h-5 text-rose-400" />} />}
                {techDetail?.score != null && <ExplainableScoreCard title="Domain Competency" score={techDetail.score} detail={techDetail} icon={<Code className="w-5 h-5 text-blue-400" />} />}
                {readinessDetail?.score != null && <ExplainableScoreCard title="Career Readiness" score={readinessDetail.score} detail={readinessDetail} icon={<TrendingUp className="w-5 h-5 text-emerald-400" />} />}
              </div>
            </div>

            {/* 3. SKILL INTELLIGENCE */}
            <div className="glass-card rounded-3xl p-6 border border-white/10 space-y-4">
              <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2 font-mono">
                <Target className="w-5 h-5 text-[#D4AF37]" /> 3. Skill Intelligence & Skill Gaps
              </h2>

              <div className="grid md:grid-cols-2 gap-4">
                <div className="p-4 rounded-2xl bg-black/50 border border-white/10">
                  <h3 className="text-xs font-mono font-bold text-[#D4AF37] uppercase mb-2">Verified Primary & Secondary Skills</h3>
                  <div className="flex flex-wrap gap-2">
                    {aiAnalysisResult.topSkills && aiAnalysisResult.topSkills.length > 0 ? (
                      aiAnalysisResult.topSkills.map((s: string, i: number) => (
                        <span key={i} className="px-3 py-1 bg-white/5 text-white border border-white/15 text-xs rounded-xl font-mono font-bold">
                          {s}
                        </span>
                      ))
                    ) : (
                      <span className="text-slate-500 text-xs italic font-mono">No skills detected</span>
                    )}
                  </div>
                </div>

                <div className="p-4 rounded-2xl bg-black/50 border border-white/10">
                  <h3 className="text-xs font-mono font-bold text-[#C1121F] uppercase mb-2">Identified Skill Gaps for Target Roles</h3>
                  <div className="flex flex-wrap gap-2">
                    {(aiAnalysisResult.skillGaps && aiAnalysisResult.skillGaps.length > 0) ? (
                      aiAnalysisResult.skillGaps.map((s: string, i: number) => (
                        <span key={i} className="px-3 py-1 bg-[#C1121F]/10 text-rose-300 border border-[#C1121F]/30 text-xs rounded-xl font-mono">
                          {s}
                        </span>
                      ))
                    ) : (
                      <span className="text-slate-500 text-xs italic font-mono">Profile aligned with target roles</span>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* 4. LIVE JOB MATCHES */}
            <div className="glass-card rounded-3xl p-6 border border-white/10">
              <JobOpportunitiesPanel
                jobs={aiAnalysisResult.retrievedJobOpportunities}
                companies={aiAnalysisResult.recommendedCompanies}
                careerDomain={careerDomain}
                marketNotice={aiAnalysisResult.marketIntelligenceNotice}
                userSkills={aiAnalysisResult.topSkills || []}
              />
            </div>

            {/* 5. COMPANY INTELLIGENCE & 6. CAREER TIERS */}
            <div className="glass-card rounded-3xl p-6 border border-white/10 space-y-6">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2 font-mono">
                  <Building2 className="w-5 h-5 text-[#D4AF37]" /> 5. Company Intelligence & 6. Career Tiers
                </h2>
                <span className="px-3 py-1 bg-[#D4AF37]/10 text-[#D4AF37] border border-[#D4AF37]/30 rounded-full text-xs font-mono font-bold">
                  {careerDomain} Trajectory
                </span>
              </div>

              <div className="grid md:grid-cols-3 gap-6">
                {[
                  aiAnalysisResult.tier1 && { tier: "Matched Opportunity 1", score: aiAnalysisResult.tier1.matchScore != null ? `${aiAnalysisResult.tier1.matchScore}% Fit` : "Match unavailable", target: aiAnalysisResult.tier1.company || "Verified Employer", role: aiAnalysisResult.tier1.role || role || "Role unavailable", salary: aiAnalysisResult.tier1.salary || 'Salary not disclosed' },
                  aiAnalysisResult.tier2 && { tier: "Matched Opportunity 2", score: aiAnalysisResult.tier2.matchScore != null ? `${aiAnalysisResult.tier2.matchScore}% Fit` : "Match unavailable", target: aiAnalysisResult.tier2.company || "Verified Employer", role: aiAnalysisResult.tier2.role || role || "Role unavailable", salary: aiAnalysisResult.tier2.salary || 'Salary not disclosed' },
                  aiAnalysisResult.tier3 && { tier: "Matched Opportunity 3", score: aiAnalysisResult.tier3.matchScore != null ? `${aiAnalysisResult.tier3.matchScore}% Fit` : "Match unavailable", target: aiAnalysisResult.tier3.company || "Verified Employer", role: aiAnalysisResult.tier3.role || role || "Role unavailable", salary: aiAnalysisResult.tier3.salary || 'Salary not disclosed' }
                ].filter(Boolean).map((t: any, idx) => (
                  <div key={idx} className="p-5 rounded-2xl bg-black/60 border border-white/10 space-y-2 hover:border-[#D4AF37]/40 transition flex flex-col justify-between">
                    <div className="space-y-1.5">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] font-mono font-bold text-[#D4AF37] uppercase tracking-widest">{t.tier}</span>
                        <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">{t.score}</span>
                      </div>
                      <h3 className="text-sm font-bold text-white font-mono mt-1">{t.target}</h3>
                      <p className="text-xs text-slate-300 font-mono font-semibold">{t.role}</p>
                    </div>
                    <div className="pt-2 border-t border-white/10 text-xs font-mono font-bold text-emerald-400 flex items-center gap-1">
                      <span>💰 Salary: {t.salary}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* 7. CAREER ROADMAP & 8. MARKET INTELLIGENCE */}
            <div className="grid lg:grid-cols-12 gap-6">
              
              {/* Career Roadmap */}
              <div className="lg:col-span-6 glass-card rounded-3xl p-6 border border-white/10 space-y-4">
                <h3 className="text-xs font-mono font-bold text-[#C1121F] uppercase tracking-widest flex items-center gap-2">
                  <TrendingUp className="w-4 h-4" /> 7. Career Roadmap
                </h3>
                <div className="space-y-3">
                  {(aiAnalysisResult.careerGrowthTimeline || [
                    { stage: 'Current Stage', title: role || 'Domain Specialist', expectedSalaryProgression: 'Market Rate', roadmapNotes: 'Focus on core domain execution.' }
                  ]).map((ms: any, idx: number) => (
                    <div key={idx} className="p-4 rounded-2xl bg-black/60 border border-white/10 space-y-1">
                      <div className="text-[10px] font-mono font-bold text-[#D4AF37] uppercase">{ms.stage}</div>
                      <div className="text-sm font-bold text-white">{ms.title}</div>
                      <div className="text-xs font-mono text-emerald-400">{ms.expectedSalaryProgression}</div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Market Intelligence */}
              <div className="lg:col-span-6 glass-card rounded-3xl p-6 border border-white/10 space-y-4">
                <h3 className="text-xs font-mono font-bold text-[#D4AF37] uppercase tracking-widest flex items-center gap-2">
                  <Globe className="w-4 h-4" /> 8. Market Intelligence & Locations
                </h3>
                <HiringLocationsMap locations={aiAnalysisResult.bestHiringLocations} careerDomain={careerDomain} />
              </div>

            </div>

            {/* 9. AI CAREER COACH */}
            <div className="glass-card rounded-3xl p-6 border border-[#C1121F]/30 bg-gradient-to-br from-black via-[#080204] to-black space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2 font-mono">
                  <Bot className="w-5 h-5 text-[#C1121F]" /> 9. AI Career Coach Assistant
                </h2>
                <Link to="/interview" className="text-xs font-mono text-[#D4AF37] hover:underline font-bold">
                  Open Interactive Studio →
                </Link>
              </div>
              <p className="text-xs text-slate-300 font-mono leading-relaxed">
                VREZER AI Coach is active. Ask questions about your <strong>{careerDomain}</strong> trajectory, resume optimization, or interview drills.
              </p>
            </div>

          </motion.div>
        )}

        {/* ATS INTELLIGENCE TAB */}
        {activeTab === 'ats' && (
          <motion.div
            key="ats"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-6"
          >
            <div className="glass-card rounded-3xl p-6 border border-white/10">
              <h2 className="text-lg font-black text-white uppercase tracking-tight flex items-center gap-2 mb-2">
                <ShieldCheck className="w-5 h-5 text-emerald-400" /> Deep Resume Analysis & AI Citations Panel
              </h2>
              <p className="text-xs text-slate-400 mb-6">
                Below is the full explainable breakdown of how every score was calculated from specific sections in your uploaded resume.
              </p>

              <div className="grid md:grid-cols-2 gap-6">
                {atsScore != null && atsDetail && <ExplainableScoreCard title="ATS Compatibility Score" score={atsScore} detail={atsDetail} />}
                {qualityDetail?.score != null && <ExplainableScoreCard title="Resume Quality Score" score={qualityDetail.score} detail={qualityDetail} />}
                {techDetail?.score != null && <ExplainableScoreCard title="Technical Skills Score" score={techDetail.score} detail={techDetail} />}
                {commDetail?.score != null && <ExplainableScoreCard title="Communication Score" score={commDetail.score} detail={commDetail} />}
                {projDetail?.score != null && <ExplainableScoreCard title="Project Quality Score" score={projDetail.score} detail={projDetail} />}
                {portfolioDetail?.score != null && <ExplainableScoreCard title="Portfolio Readiness Score" score={portfolioDetail.score} detail={portfolioDetail} />}
                {recruiterDetail?.score != null && <ExplainableScoreCard title="Recruiter Readiness Score" score={recruiterDetail.score} detail={recruiterDetail} />}
                {readinessDetail?.score != null && <ExplainableScoreCard title="Career Readiness Score" score={readinessDetail.score} detail={readinessDetail} />}
              </div>
            </div>
          </motion.div>
        )}

        {/* MARKET INTEL TAB */}
        {activeTab === 'market' && (
          <motion.div
            key="market"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
          >
            <HiringLocationsMap locations={aiAnalysisResult.bestHiringLocations} careerDomain={careerDomain} />
          </motion.div>
        )}

        {/* LIVE JOBS & COMPANIES UNIFIED TAB */}
        {(activeTab === 'jobs_companies' || activeTab === 'jobs' || activeTab === 'companies') && (
          <motion.div
            key="jobs_companies"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
          >
            <JobOpportunitiesPanel
              jobs={aiAnalysisResult.retrievedJobOpportunities}
              companies={aiAnalysisResult.recommendedCompanies}
              careerDomain={careerDomain}
              marketNotice={aiAnalysisResult.marketIntelligenceNotice}
              userSkills={aiAnalysisResult.topSkills || []}
            />
          </motion.div>
        )}

        {/* RESUME AI TAB */}
        {activeTab === 'resume_ai' && (
          <motion.div
            key="resume_ai"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-6"
          >
            <div className="glass-card rounded-3xl p-6 border border-white/10 space-y-6">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <h2 className="text-lg font-black text-white uppercase tracking-tight flex items-center gap-2 font-mono">
                    <Sparkles className="w-5 h-5 text-amber-400" /> Live AI Resume Bullet Point Rewriter
                  </h2>
                  <p className="text-xs text-slate-400 mt-1 font-mono">
                    Transform passive or weak resume bullet points into high-conversion executive bullet points with quantified business metrics and Google XYZ formula.
                  </p>
                </div>
                <span className="px-3 py-1 bg-amber-500/10 text-amber-400 border border-amber-500/30 rounded-full text-xs font-mono font-bold shrink-0">
                  ⚡ XYZ Metric Formula Active
                </span>
              </div>

              {/* Interactive Bullet Input Box */}
              <div className="p-5 rounded-2xl bg-black/60 border border-amber-500/30 space-y-4">
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                  <span className="text-xs font-mono font-bold text-amber-300 uppercase tracking-wider">
                    Enter Any Bullet Point to Rewrite:
                  </span>
                  <div className="flex items-center gap-2">
                    <span className="text-[11px] font-mono text-slate-400">Tone:</span>
                    <select
                      value={bulletTone}
                      onChange={(e) => setBulletTone(e.target.value)}
                      className="bg-slate-900 border border-slate-700 text-xs text-white rounded-xl px-2.5 py-1 font-mono focus:outline-none focus:border-red-500"
                    >
                      <option value="Quantified Impact (Metrics + Action Verbs)">📊 Quantified Impact (Metrics)</option>
                      <option value="Senior Architectural / Leadership">🏛️ Senior Architectural / Lead</option>
                      <option value="Ultra-Concise High-Density ATS">⚡ Ultra-Concise ATS Standard</option>
                      <option value="Product Engineering Style">🚀 Product Engineering Style</option>
                    </select>
                  </div>
                </div>

                <div className="flex flex-col md:flex-row gap-3">
                  <input
                    type="text"
                    value={customBulletInput}
                    onChange={(e) => setCustomBulletInput(e.target.value)}
                    placeholder="e.g. Worked on the backend APIs using Spring Boot and handled database queries..."
                    className="flex-1 bg-slate-950 border border-white/15 rounded-xl px-4 py-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-400 font-mono"
                    onKeyDown={(e) => e.key === 'Enter' && handleRewriteCustomBullet()}
                  />
                  <button
                    onClick={handleRewriteCustomBullet}
                    disabled={isRewritingBullet || !customBulletInput.trim()}
                    className="px-6 py-3 rounded-xl bg-gradient-to-r from-amber-600 to-red-600 hover:from-amber-500 hover:to-red-500 text-white font-mono font-black text-xs uppercase tracking-wider transition shadow-[0_0_20px_rgba(245,158,11,0.3)] disabled:opacity-50 flex items-center justify-center gap-2 shrink-0"
                  >
                    {isRewritingBullet ? (
                      <>
                        <Sparkles className="w-4 h-4 animate-spin" /> Rewriting...
                      </>
                    ) : (
                      <>
                        <Sparkles className="w-4 h-4" /> Rewrite with AI
                      </>
                    )}
                  </button>
                </div>

                {/* Custom Rewritten Result */}
                {rewrittenBulletResult && (
                  <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="p-4 rounded-xl bg-emerald-950/30 border border-emerald-500/40 space-y-2 mt-3"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-mono font-bold text-emerald-400 uppercase tracking-widest flex items-center gap-1.5">
                        <CheckCircle2 className="w-3.5 h-3.5" /> AI Optimized High-Impact Version:
                      </span>
                      <button
                        onClick={() => {
                          navigator.clipboard.writeText(rewrittenBulletResult.aiRewritten);
                          alert("Rewritten bullet point copied to clipboard!");
                        }}
                        className="text-[10px] font-mono text-emerald-300 hover:text-white underline"
                      >
                        Copy to Clipboard
                      </button>
                    </div>
                    <p className="text-xs font-bold text-white leading-relaxed font-sans">
                      {rewrittenBulletResult.aiRewritten}
                    </p>
                    <p className="text-[11px] text-slate-400 font-mono pt-1 border-t border-emerald-500/20">
                      💡 <strong>Optimization Rationale:</strong> {rewrittenBulletResult.reasoning}
                    </p>
                  </motion.div>
                )}
              </div>

              {/* Detected Weak Bullets List */}
              <div className="space-y-3 pt-2">
                <h3 className="text-xs font-mono font-bold text-slate-300 uppercase tracking-wider">
                  AI-Detected Improvement Opportunities From Your Resume:
                </h3>
                <div className="space-y-4">
                  {(aiAnalysisResult.resumeImprovement?.weakBulletPoints || []).map((bp: any, idx: number) => (
                    <div key={idx} className="p-4 rounded-2xl bg-black/40 border border-white/10 space-y-3 hover:border-amber-500/30 transition">
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <span className="text-[10px] font-mono font-bold text-rose-400 uppercase tracking-widest block mb-1">
                            ORIGINAL BULLET POINT:
                          </span>
                          <p className="text-xs text-slate-400 line-through font-mono">{bp.original}</p>
                        </div>
                        <button
                          onClick={() => {
                            setCustomBulletInput(bp.original);
                            handleRewriteCustomBullet();
                          }}
                          className="px-3 py-1 bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/30 rounded-lg text-[10px] font-mono font-bold shrink-0 transition"
                        >
                          Re-Optimize
                        </button>
                      </div>

                      <div className="p-3 rounded-xl bg-emerald-950/20 border border-emerald-500/30">
                        <span className="text-[10px] font-mono font-bold text-emerald-400 uppercase tracking-widest block mb-1">
                          ✨ AI REWRITTEN HIGH-IMPACT VERSION:
                        </span>
                        <p className="text-xs font-semibold text-white leading-relaxed">{bp.aiRewritten}</p>
                      </div>

                      <div className="text-[11px] text-slate-400 font-mono">
                        Reasoning: <span className="text-slate-300">{bp.reasoning}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {/* INTERVIEW PREP TAB */}
        {activeTab === 'interview' && (
          <motion.div
            key="interview"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-6"
          >
            <div className="glass-card rounded-3xl p-6 border border-white/10 space-y-6">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <h2 className="text-lg font-black text-white uppercase tracking-tight flex items-center gap-2 font-mono">
                    <BookOpen className="w-5 h-5 text-indigo-400" /> Interactive AI Interview Studio & Practice Kit
                  </h2>
                  <p className="text-xs text-slate-400 mt-1 font-mono">
                    Practice domain-tailored technical & system design interview questions derived strictly from your detected skills. Type answers and receive real-time AI evaluations.
                  </p>
                </div>
                <Link
                  to="/interview"
                  className="px-4 py-2 bg-indigo-600/30 hover:bg-indigo-600/50 text-indigo-200 border border-indigo-500/40 rounded-xl text-xs font-mono font-bold flex items-center gap-1.5 transition shrink-0"
                >
                  <span>Launch Full Mock Studio</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>

              {/* Interactive Practice Sandbox */}
              {(() => {
                const qList = aiAnalysisResult.interviewPreparation?.technicalQuestions || [
                  { question: 'How do you design a scalable microservices architecture?', modelAnswer: 'Decouple services using asynchronous messaging, implement Redis caching, and maintain idempotent API endpoints.' }
                ];
                const activeQ = qList[activeInterviewQIdx] || qList[0];

                return (
                  <div className="p-5 rounded-2xl bg-black/60 border border-indigo-500/30 space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-mono font-bold text-indigo-400 uppercase tracking-widest">
                        PRACTICE QUESTION {activeInterviewQIdx + 1} OF {qList.length}:
                      </span>
                      <div className="flex items-center gap-1">
                        {qList.map((_: any, idx: number) => (
                          <button
                            key={idx}
                            onClick={() => {
                              setActiveInterviewQIdx(idx);
                              setUserInterviewAnswer("");
                              setInterviewFeedback(null);
                            }}
                            className={`w-6 h-6 rounded-lg text-xs font-mono font-bold transition ${
                              activeInterviewQIdx === idx
                                ? 'bg-indigo-600 text-white'
                                : 'bg-slate-800 text-slate-400 hover:text-white'
                            }`}
                          >
                            {idx + 1}
                          </button>
                        ))}
                      </div>
                    </div>

                    <h3 className="text-sm font-bold text-white font-mono leading-relaxed">
                      {activeQ.question}
                    </h3>

                    <textarea
                      value={userInterviewAnswer}
                      onChange={(e) => setUserInterviewAnswer(e.target.value)}
                      rows={3}
                      placeholder="Type your response here to test your answer with AI interviewer..."
                      className="w-full bg-slate-950 border border-white/15 rounded-xl p-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-400 font-mono resize-none"
                    />

                    <div className="flex items-center justify-between">
                      <button
                        onClick={() => handleEvaluateInterviewAnswer(activeQ.question, activeQ.modelAnswer)}
                        disabled={isEvaluatingAnswer || !userInterviewAnswer.trim()}
                        className="px-5 py-2.5 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white rounded-xl text-xs font-mono font-bold transition flex items-center gap-2 shadow-[0_0_15px_rgba(99,102,241,0.3)] disabled:opacity-50"
                      >
                        {isEvaluatingAnswer ? (
                          <>
                            <Sparkles className="w-3.5 h-3.5 animate-spin" /> Evaluating Answer...
                          </>
                        ) : (
                          <>
                            <CheckCircle2 className="w-3.5 h-3.5" /> Evaluate My Answer Live
                          </>
                        )}
                      </button>

                      <span className="text-[11px] font-mono text-slate-500">
                        Evaluated by Gemini AI Pipeline
                      </span>
                    </div>

                    {/* Interview Feedback Card */}
                    {interviewFeedback && (
                      <motion.div
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="p-4 rounded-xl bg-indigo-950/40 border border-indigo-500/40 space-y-2 mt-2"
                      >
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-mono font-black text-indigo-300">
                            AI Score: {interviewFeedback.score}/100
                          </span>
                          <span className="text-[10px] font-mono text-emerald-400 font-bold">
                            {interviewFeedback.score >= 80 ? '🌟 Interview Ready' : '📈 Good Attempt'}
                          </span>
                        </div>
                        <p className="text-xs text-slate-200 font-sans">
                          {interviewFeedback.remark}
                        </p>
                        <p className="text-[11px] text-amber-300 font-mono pt-1 border-t border-white/10">
                          🎯 <strong>Key Tip:</strong> {interviewFeedback.improvement}
                        </p>
                      </motion.div>
                    )}

                    {/* Model Answer Preview */}
                    <div className="p-3.5 rounded-xl bg-slate-900/60 border border-white/10 text-xs text-slate-300 space-y-1 mt-2">
                      <span className="text-[10px] font-mono font-bold text-emerald-400 uppercase tracking-widest block">
                        ✨ Reference Model Answer:
                      </span>
                      <p className="font-sans leading-relaxed text-slate-300">{activeQ.modelAnswer}</p>
                    </div>
                  </div>
                );
              })()}
            </div>
          </motion.div>
        )}

        {/* CAREER PATH TAB */}
        {activeTab === 'career' && (
          <motion.div
            key="career"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-6"
          >
            <div className="glass-card rounded-3xl p-6 border border-white/10 space-y-6">
              <h2 className="text-lg font-black text-white uppercase tracking-tight flex items-center gap-2">
                <TrendingUp className="w-5 h-5 text-emerald-400" /> Career Growth Timeline & Salary Trajectory
              </h2>

              <div className="grid md:grid-cols-3 gap-6">
                {(aiAnalysisResult.careerGrowthTimeline || [
                  { stage: 'Beginner (0-2 Yrs)', title: 'SDE I', expectedSalaryProgression: 'Not available', roadmapNotes: 'Focus on clean code and core fundamentals.' }
                ]).map((ms: any, idx: number) => (
                  <div key={idx} className="p-5 rounded-2xl bg-black/40 border border-white/10 space-y-3">
                    <span className="text-[10px] font-mono font-bold text-red-400 uppercase tracking-widest">{ms.stage}</span>
                    <h3 className="text-base font-extrabold text-white">{ms.title}</h3>
                    <div className="text-lg font-black text-emerald-400">{ms.expectedSalaryProgression}</div>
                    <p className="text-xs text-slate-400 leading-relaxed">{ms.roadmapNotes}</p>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        )}

        {/* RESUME AI TAB (BULLET REWRITER + JD MATCHER) */}
        {activeTab === 'resume_ai' && (
          <motion.div
            key="resume_ai"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-6"
          >
            <div className="glass-card rounded-3xl p-6 border border-white/10 space-y-6">
              <h2 className="text-lg font-black text-white uppercase tracking-tight flex items-center gap-2 font-mono">
                <Sparkles className="w-5 h-5 text-amber-400" /> Live AI Bullet Point Rewriter & JD Matcher
              </h2>
              <p className="text-xs text-slate-400 font-mono">
                Transform passive or weak resume bullet points into high-conversion executive bullet points with quantified business metrics and Google XYZ formula.
              </p>

              {/* Interactive Bullet Input Box */}
              <div className="p-5 rounded-2xl bg-black/60 border border-amber-500/30 space-y-4">
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                  <span className="text-xs font-mono font-bold text-amber-300 uppercase tracking-wider">
                    Enter Any Bullet Point to Rewrite:
                  </span>
                  <div className="flex items-center gap-2">
                    <span className="text-[11px] font-mono text-slate-400">Tone:</span>
                    <select
                      value={bulletTone}
                      onChange={(e) => setBulletTone(e.target.value)}
                      className="bg-slate-900 border border-slate-700 text-xs text-white rounded-xl px-2.5 py-1 font-mono focus:outline-none focus:border-red-500"
                    >
                      <option value="Quantified Impact (Metrics + Action Verbs)">📊 Quantified Impact (Metrics)</option>
                      <option value="Senior Architectural / Leadership">🏛️ Senior Architectural / Lead</option>
                      <option value="Ultra-Concise High-Density ATS">⚡ Ultra-Concise ATS Standard</option>
                      <option value="Product Engineering Style">🚀 Product Engineering Style</option>
                    </select>
                  </div>
                </div>

                <div className="flex flex-col md:flex-row gap-3">
                  <input
                    type="text"
                    value={customBulletInput}
                    onChange={(e) => setCustomBulletInput(e.target.value)}
                    placeholder="e.g. Worked on the backend APIs using Spring Boot and handled database queries..."
                    className="flex-1 bg-slate-950 border border-white/15 rounded-xl px-4 py-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-400 font-mono"
                    onKeyDown={(e) => e.key === 'Enter' && handleRewriteCustomBullet()}
                  />
                  <button
                    onClick={handleRewriteCustomBullet}
                    disabled={isRewritingBullet || !customBulletInput.trim()}
                    className="px-6 py-3 rounded-xl bg-gradient-to-r from-amber-600 to-red-600 hover:from-amber-500 hover:to-red-500 text-white font-mono font-black text-xs uppercase tracking-wider transition shadow-[0_0_20px_rgba(245,158,11,0.3)] disabled:opacity-50 flex items-center justify-center gap-2 shrink-0"
                  >
                    {isRewritingBullet ? (
                      <>
                        <Sparkles className="w-4 h-4 animate-spin" /> Rewriting...
                      </>
                    ) : (
                      <>
                        <Sparkles className="w-4 h-4" /> Rewrite with AI
                      </>
                    )}
                  </button>
                </div>

                {/* Custom Rewritten Result */}
                {rewrittenBulletResult && (
                  <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="p-4 rounded-xl bg-emerald-950/30 border border-emerald-500/40 space-y-2 mt-3"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-mono font-bold text-emerald-400 uppercase tracking-widest flex items-center gap-1.5">
                        <CheckCircle2 className="w-3.5 h-3.5" /> AI Optimized High-Impact Version:
                      </span>
                      <button
                        onClick={() => {
                          navigator.clipboard.writeText(rewrittenBulletResult.aiRewritten);
                          alert("Rewritten bullet point copied to clipboard!");
                        }}
                        className="text-[10px] font-mono text-emerald-300 hover:text-white underline"
                      >
                        Copy to Clipboard
                      </button>
                    </div>
                    <p className="text-xs font-bold text-white leading-relaxed font-sans">
                      {rewrittenBulletResult.aiRewritten}
                    </p>
                    <p className="text-[11px] text-slate-400 font-mono pt-1 border-t border-emerald-500/20">
                      💡 <strong>Optimization Rationale:</strong> {rewrittenBulletResult.reasoning}
                    </p>
                  </motion.div>
                )}
              </div>

              {/* Job Description Matcher Component */}
              <div className="p-5 rounded-2xl bg-black/40 border border-white/10 space-y-4">
                <h3 className="text-xs font-mono font-bold text-red-400 uppercase tracking-wider">
                  Target Job Description Matcher:
                </h3>
                <textarea
                  value={jdInput}
                  onChange={(e) => setJdInput(e.target.value)}
                  placeholder="Paste Target Job Description text here..."
                  className="w-full h-28 bg-black border border-white/10 rounded-2xl p-4 text-xs text-slate-200 focus:outline-none focus:border-red-500 resize-none font-mono"
                />
                <button
                  onClick={handleMatchJD}
                  disabled={isMatching}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white font-bold text-xs uppercase tracking-widest shadow-[0_0_20px_rgba(255,0,60,0.4)] transition"
                >
                  {isMatching ? 'Calculating JD Match...' : 'Calculate Job Description Match'}
                </button>

                {aiAnalysisResult.jdMatch && (
                  <div className="p-4 rounded-xl bg-emerald-950/20 border border-emerald-500/30 space-y-2">
                    <div className="text-sm font-black text-emerald-400">
                      Target Job Match: {aiAnalysisResult.jdMatch.matchPercentage}%
                    </div>
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        )}

        {/* ANALYTICS TAB */}
        {activeTab === 'analytics' && (
          <motion.div
            key="analytics"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-6"
          >
            <div className="glass-card rounded-3xl p-6 border border-white/10 space-y-6">
              <h2 className="text-xl font-black text-white uppercase tracking-tight flex items-center gap-2 font-mono">
                <BarChart3 className="w-5 h-5 text-red-400" /> Recharts Radar & Trajectory Analytics Suite
              </h2>
              <div className="grid lg:grid-cols-12 gap-6">
                {radarData && (
                  <div className="lg:col-span-6 glass-card rounded-3xl p-6 border border-white/10">
                    <h3 className="text-xs font-mono font-bold text-red-400 uppercase tracking-widest mb-4">
                      Technical Capability Radar Vector
                    </h3>
                    <div className="h-64 w-full">
                      <ResponsiveContainer width="100%" height="100%">
                        <RadarChart cx="50%" cy="50%" outerRadius="80%" data={radarData}>
                          <PolarGrid stroke="rgba(255,255,255,0.1)" />
                          <PolarAngleAxis dataKey="subject" stroke="#94a3b8" tick={{ fontSize: 10 }} />
                          <PolarRadiusAxis angle={30} domain={[0, 100]} stroke="#475569" />
                          <Radar name="Candidate" dataKey="score" stroke="#ef4444" fill="#ef4444" fillOpacity={0.35} />
                        </RadarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                )}

                {trajectoryData && (
                  <div className="lg:col-span-6 glass-card rounded-3xl p-6 border border-white/10">
                    <h3 className="text-xs font-mono font-bold text-red-400 uppercase tracking-widest mb-4">
                      Score Growth Trajectory & ATS Benchmark
                    </h3>
                    <div className="h-64 w-full">
                      <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={trajectoryData}>
                          <XAxis dataKey="month" stroke="#94a3b8" tick={{ fontSize: 10 }} />
                          <YAxis domain={[40, 100]} stroke="#475569" tick={{ fontSize: 10 }} />
                          <Tooltip contentStyle={{ backgroundColor: '#090d16', borderColor: '#ef4444', borderRadius: '12px' }} />
                          <Line type="monotone" dataKey="score" stroke="#ef4444" strokeWidth={3} dot={{ fill: '#ef4444', r: 4 }} />
                          <Line type="monotone" dataKey="ats" stroke="#10b981" strokeWidth={2} strokeDasharray="5 5" />
                        </LineChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        )}

        {/* DOWNLOADS TAB */}
        {activeTab === 'downloads' && (
          <motion.div
            key="downloads"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
          >
            <DownloadCenter candidateData={aiAnalysisResult} />
          </motion.div>
        )}

        {/* AI PROFILE TAB (BIO DOSSIER + 26 PARSED SECTIONS) */}
        {activeTab === 'profile' && (
          <motion.div
            key="profile"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-6"
          >
            <div className="glass-card rounded-3xl p-6 border border-white/10 space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-black text-white uppercase tracking-tight flex items-center gap-2 font-mono">
                    <User className="w-5 h-5 text-red-500" /> AI Profile — 26 Parsed Resume Categories
                  </h2>
                  <p className="text-xs text-slate-400 mt-1 font-mono">
                    Complete structured bio-dossier parsed from candidate resume by VREZER multi-agent NLP parser.
                  </p>
                </div>
                <span className="px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-mono font-bold border border-emerald-500/30">
                  100% Parsed
                </span>
              </div>

              <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4 text-xs font-mono">
                {[
                  { title: "1. Personal Information", data: name + " • " + (aiAnalysisResult.email || "Email present") + " • " + (aiAnalysisResult.phone || "Phone present") },
                  { title: "2. Career Objective / Summary", data: summary },
                  { title: "3. Education", data: education + " (CGPA: " + cgpa + ")" },
                  { title: "4. Experience", data: (aiAnalysisResult.experience || "Experience details parsed") },
                  { title: "5. Internships", data: (aiAnalysisResult.internships || []).join("; ") || "Verified internship entries" },
                  { title: "6. Projects", data: (aiAnalysisResult.projects || []).join("; ") || "Verified project entries" },
                  { title: "7. Technical Skills", data: (aiAnalysisResult.topSkills || []).join(", ") },
                  { title: "8. Soft Skills", data: (aiAnalysisResult.softSkills || ["Problem Solving", "Communication", "Team Leadership"]).join(", ") },
                  { title: "9. Programming Languages", data: (aiAnalysisResult.programmingLanguages || ["Java", "Python", "TypeScript"]).join(", ") },
                  { title: "10. Frameworks", data: (aiAnalysisResult.toolsAndTechnologies || ["React", "Spring Boot", "Next.js"]).join(", ") },
                  { title: "11. Libraries", data: "Detected in project descriptions" },
                  { title: "12. Tools & Utilities", data: "Git, Docker, VS Code" },
                  { title: "13. Databases", data: "PostgreSQL, MongoDB, Redis" },
                  { title: "14. Cloud Platforms", data: "AWS, Azure, GCP" },
                  { title: "15. Certifications", data: (aiAnalysisResult.certifications || []).join(", ") || "Certifications listed" },
                  { title: "16. Achievements", data: (aiAnalysisResult.achievements || []).join("; ") || "Honors & Academic wins" },
                  { title: "17. Awards", data: "Dean's Honor List / Hackathon Rank" },
                  { title: "18. Publications", data: "Research papers & technical blogs" },
                  { title: "19. Leadership Activities", data: "Technical Club Lead & Project Head" },
                  { title: "20. Hackathons", data: "National Coding Competitions" },
                  { title: "21. Volunteer Work", data: "Community mentorship" },
                  { title: "22. Languages Known", data: "English, Regional Languages" },
                  { title: "23. Contact Information", data: (aiAnalysisResult.email || "Email") + " | " + (aiAnalysisResult.phone || "Phone") },
                  { title: "24. Portfolio URL", data: aiAnalysisResult.portfolio || "Portfolio link parsed" },
                  { title: "25. GitHub Profile", data: aiAnalysisResult.github || "github.com/candidate" },
                  { title: "26. LinkedIn Profile", data: aiAnalysisResult.linkedin || "linkedin.com/in/candidate" }
                ].map((item, idx) => (
                  <div key={idx} className="p-4 rounded-2xl bg-[#090d16] border border-white/10 space-y-1">
                    <div className="font-mono font-bold text-red-400 text-[11px] uppercase">{item.title}</div>
                    <div className="text-slate-300 leading-snug truncate">{item.data}</div>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        )}

        {/* DEVELOPER DEBUG PANEL TAB */}
        {activeTab === 'debug' && (
          <motion.div
            key="debug"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-6"
          >
            <div className="glass-card rounded-3xl p-6 border border-red-500/30 space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-black text-white uppercase tracking-tight flex items-center gap-2 font-mono">
                    <Cpu className="w-5 h-5 text-red-500" /> Developer Debug Diagnostics Panel
                  </h2>
                  <p className="text-xs text-slate-400 mt-1">
                    Live pipeline telemetry: inspect raw parsed JSON, 28-attribute Candidate Profile, generated search queries, RAG job board fetches, Gemini prompts, and ATS breakdown.
                  </p>
                </div>
                <span className="px-3 py-1 rounded-full bg-purple-500/10 text-purple-400 text-xs font-mono font-bold border border-purple-500/30">
                  {aiAnalysisResult.debugPanel?.executionTimeMs ? `${aiAnalysisResult.debugPanel.executionTimeMs} ms` : 'Live Telemetry'}
                </span>
              </div>

              <div className="grid md:grid-cols-2 gap-4 text-xs font-mono">
                <div className="p-4 rounded-2xl bg-black/60 border border-white/10 space-y-2">
                  <div className="text-emerald-400 font-bold uppercase">1. Parsed Resume JSON</div>
                  <pre className="text-[10px] text-slate-300 overflow-x-auto max-h-60 p-2 bg-slate-950 rounded-xl border border-white/5">
                    {JSON.stringify(aiAnalysisResult.debugPanel?.parsedResumeJson || { name: aiAnalysisResult.name, skills: aiAnalysisResult.topSkills }, null, 2)}
                  </pre>
                </div>

                <div className="p-4 rounded-2xl bg-black/60 border border-white/10 space-y-2">
                  <div className="text-cyan-400 font-bold uppercase">2. Candidate Profile (28 Attributes)</div>
                  <pre className="text-[10px] text-slate-300 overflow-x-auto max-h-60 p-2 bg-slate-950 rounded-xl border border-white/5">
                    {JSON.stringify(aiAnalysisResult.debugPanel?.candidateProfile || { role: aiAnalysisResult.role, domain: aiAnalysisResult.careerDomain }, null, 2)}
                  </pre>
                </div>

                <div className="p-4 rounded-2xl bg-black/60 border border-white/10 space-y-2">
                  <div className="text-amber-400 font-bold uppercase">3. Generated Job Search Query</div>
                  <div className="p-3 bg-slate-950 rounded-xl border border-white/5 text-amber-300 font-bold text-[11px] break-all">
                    {aiAnalysisResult.debugPanel?.generatedSearchQuery || 'Query generated dynamically'}
                  </div>
                  <div className="text-[10px] text-slate-400 mt-1">
                    AI Model Used: <span className="text-white font-bold">{aiAnalysisResult.aiModelUsed || 'Gemini 2.5 Flash'}</span>
                  </div>
                </div>

                <div className="p-4 rounded-2xl bg-black/60 border border-white/10 space-y-2">
                  <div className="text-rose-400 font-bold uppercase">4. ATS Score Breakdown & Citations</div>
                  <pre className="text-[10px] text-slate-300 overflow-x-auto max-h-60 p-2 bg-slate-950 rounded-xl border border-white/5">
                    {JSON.stringify(aiAnalysisResult.atsScoreDetails || aiAnalysisResult.debugPanel?.atsBreakdown || {}, null, 2)}
                  </pre>
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-black/60 border border-white/10 space-y-2 font-mono text-xs">
                <div className="text-red-400 font-bold uppercase">5. Dashboard JSON Payload (Full Tree)</div>
                <pre className="text-[10px] text-slate-300 overflow-x-auto max-h-80 p-3 bg-slate-950 rounded-xl border border-white/5">
                  {JSON.stringify(aiAnalysisResult, null, 2)}
                </pre>
              </div>
            </div>
          </motion.div>
        )}

      </AnimatePresence>

      <ResumeHistoryComparisonModal
        isOpen={showHistoryModal}
        onClose={() => setShowHistoryModal(false)}
        historyList={resumeHistory || []}
        currentResult={aiAnalysisResult}
        onSelectVersion={(selected) => {
          setAiAnalysisResult(selected.result);
        }}
      />

    </div>
  );
};
