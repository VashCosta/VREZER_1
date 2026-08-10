import React, { useState, useRef, useEffect } from 'react';
import { motion, useScroll, useSpring, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import {
  Sparkles, ShieldCheck, FileText, Zap,
  RefreshCw, Building2, UploadCloud, CheckCircle2, X,
  ArrowRight, TrendingUp, Brain, Target, Users, Star,
  Map, HelpCircle, DollarSign, Code, FileCheck, Briefcase, Linkedin, Layers
} from 'lucide-react';
import { useResumeContext } from '../context/ResumeContext';
import axios from 'axios';

/* ─────────── HERO PARTICLE CANVAS (Indigo / Cyan / Violet) ─────────── */
const LiveParticleCanvas: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    let raf: number;
    let w = (canvas.width = window.innerWidth);
    let h = (canvas.height = window.innerHeight);

    const onResize = () => {
      w = canvas.width = window.innerWidth;
      h = canvas.height = window.innerHeight;
    };
    window.addEventListener('resize', onResize);

    /* particles */
    const N = Math.min(80, Math.floor(w / 16));
    const pts = Array.from({ length: N }, () => ({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
      r: Math.random() * 2 + 0.8,
      a: Math.random() * 0.5 + 0.2,
      color: Math.random() > 0.5 ? '#6366f1' : '#06b6d4',
    }));

    let mx = w / 2, my = h / 2;
    const onMove = (e: MouseEvent) => { mx = e.clientX; my = e.clientY; };
    window.addEventListener('mousemove', onMove);

    let t = 0;
    const draw = () => {
      t += 0.006;
      ctx.clearRect(0, 0, w, h);

      /* moving subtle grid */
      const offset = (t * 20) % 60;
      ctx.strokeStyle = 'rgba(99, 102, 241, 0.04)';
      ctx.lineWidth = 1;
      for (let x = -60 + (offset % 60); x < w; x += 60) {
        ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke();
      }
      for (let y = -60 + (offset % 60); y < h; y += 60) {
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
      }

      /* radial ambient glows */
      const grad1 = ctx.createRadialGradient(w * 0.3, h * 0.25, 0, w * 0.3, h * 0.25, w * 0.45);
      grad1.addColorStop(0, 'rgba(99, 102, 241, 0.12)');
      grad1.addColorStop(1, 'rgba(11, 15, 25, 0)');
      ctx.fillStyle = grad1;
      ctx.fillRect(0, 0, w, h);

      const grad2 = ctx.createRadialGradient(w * 0.75, h * 0.6, 0, w * 0.75, h * 0.6, w * 0.4);
      grad2.addColorStop(0, 'rgba(6, 182, 212, 0.08)');
      grad2.addColorStop(1, 'rgba(11, 15, 25, 0)');
      ctx.fillStyle = grad2;
      ctx.fillRect(0, 0, w, h);

      /* particles & connections */
      for (let i = 0; i < N; i++) {
        const p = pts[i];
        p.x = (p.x + p.vx + w) % w;
        p.y = (p.y + p.vy + h) % h;

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fillStyle = p.color;
        ctx.globalAlpha = p.a;
        ctx.fill();

        for (let j = i + 1; j < N; j++) {
          const q = pts[j];
          const dx = p.x - q.x, dy = p.y - q.y;
          const d = Math.sqrt(dx * dx + dy * dy);
          if (d < 130) {
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(q.x, q.y);
            ctx.strokeStyle = '#8b5cf6';
            ctx.globalAlpha = 0.15 * (1 - d / 130);
            ctx.lineWidth = 0.8;
            ctx.stroke();
          }
        }

        const mdx = p.x - mx, mdy = p.y - my;
        const md = Math.sqrt(mdx * mdx + mdy * mdy);
        if (md < 160) {
          ctx.beginPath();
          ctx.moveTo(p.x, p.y);
          ctx.lineTo(mx, my);
          ctx.strokeStyle = '#06b6d4';
          ctx.globalAlpha = 0.35 * (1 - md / 160);
          ctx.lineWidth = 1.2;
          ctx.stroke();
        }
      }
      ctx.globalAlpha = 1;

      raf = requestAnimationFrame(draw);
    };
    draw();

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', onResize);
      window.removeEventListener('mousemove', onMove);
    };
  }, []);

  return <canvas ref={canvasRef} className="fixed inset-0 pointer-events-none z-0" />;
};

/* ─────────── ANIMATED COUNTER ─────────── */
const AnimCounter: React.FC<{ to: number; suffix?: string; prefix?: string; duration?: number }> = ({ to, suffix = '', prefix = '', duration = 1800 }) => {
  const [val, setVal] = useState(0);
  useEffect(() => {
    let start = 0;
    const step = to / (duration / 16);
    const id = setInterval(() => {
      start = Math.min(start + step, to);
      setVal(Math.floor(start));
      if (start >= to) clearInterval(id);
    }, 16);
    return () => clearInterval(id);
  }, [to, duration]);
  return <>{prefix}{val.toLocaleString()}{suffix}</>;
};

/* ─────────── MAIN LANDING PAGE ─────────── */
export const LandingPage: React.FC = () => {
  const navigate = useNavigate();
  const { setAiAnalysisResult, setRawText, addResumeToHistory } = useResumeContext();

  const { scrollYProgress } = useScroll();
  const scaleX = useSpring(scrollYProgress, { stiffness: 120, damping: 30 });

  /* ── file state ── */
  const [file, setFile] = useState<File | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  /* ── analysis state ── */
  const [phase, setPhase] = useState<'idle' | 'loading' | 'done'>('idle');
  const [loadStep, setLoadStep] = useState(0);
  const [loadPct, setLoadPct] = useState(0);

  const PHASES = [
    'Parsing document structure & ATS compliance rules…',
    'Initializing Gemini AI neural career engine…',
    'Auditing font, margin, table & keyword formatting…',
    'Benchmarking against verified market skill taxonomies…',
    'Predicting salary compensation ranges (India & Global)…',
    'Generating interactive executive intelligence report…',
  ];

  /* ── drag handlers ── */
  const onDragEnter = (e: React.DragEvent) => { e.preventDefault(); setDragActive(true); };
  const onDragLeave = (e: React.DragEvent) => { e.preventDefault(); setDragActive(false); };
  const onDragOver  = (e: React.DragEvent) => { e.preventDefault(); };
  const onDrop = (e: React.DragEvent) => {
    e.preventDefault(); setDragActive(false);
    const f = e.dataTransfer.files[0];
    if (f) setFile(f);
  };
  const onFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files?.[0]) setFile(e.target.files[0]);
  };

  /* ── sample resume ── */
  const loadSample = () => {
    const text = `Sample Candidate Resume — Full-Stack Software Engineer
Java, Spring Boot, React, TypeScript, PostgreSQL, Docker, AWS
3+ Years | Computer Science Graduate
Projects: Delivered scalable backend APIs and improved application performance.
Skills: System Design, Backend Engineering, Cloud Architecture, Team Collaboration.
Education: Computer Science Degree. Certifications: Cloud / DevOps training.`;
    const blob = new Blob([text], { type: 'application/pdf' });
    setFile(new File([blob], 'Sample_Candidate_Resume.pdf', { type: 'application/pdf' }));
  };

  const buildInsufficientDataResult = (candidateName: string) => ({
    name: candidateName,
    role: 'Candidate',
    careerDomain: 'Not available',
    status: 'INSUFFICIENT_DATA',
    topSkills: [],
    missingSkills: [],
    recommendedCompanies: [],
    retrievedJobOpportunities: [],
    bestHiringLocations: [],
    careerGrowthTimeline: [],
    skillIntelligence: { missingSkills: [], aiLearningRoadmap: [] },
  });

  /* ── ANALYSE ── */
  const handleAnalyse = async () => {
    if (!file) {
      fileInputRef.current?.click();
      return;
    }
    setPhase('loading');
    setLoadStep(0);
    setLoadPct(0);

    let step = 0;
    const ticker = setInterval(() => {
      step = Math.min(step + 1, PHASES.length - 1);
      setLoadStep(step);
      setLoadPct(Math.round(((step + 1) / PHASES.length) * 100));
    }, 800);

    const rawName = file.name
      .replace(/\.[^/.]+$/, '')
      .replace(/[-_]/g, ' ')
      .replace(/resume|cv|final|2024|2025|2026/gi, '')
      .trim();
    const candidateName = rawName.split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join(' ') || 'Candidate';

    try {
      let extractedText = '';
      try {
        const fd = new FormData();
        fd.append('file', file);
        const exRes = await axios.post('/api/analyzer/extract', fd);

        extractedText = exRes.data?.text || '';
      } catch (_) {}

      if (!extractedText) {
        extractedText = await file.text().catch(() => candidateName + '\nJava Spring Boot React AWS Senior Engineer');
      }

      let result: any = null;
      try {
        const res = await axios.post('/api/analyzer/analyze',
          { resumeText: extractedText, jobDescription: '' },
          { headers: { 'Content-Type': 'application/json' }, timeout: 20000 }
        );
        if (res.data && typeof res.data === 'object' && (res.data.name || res.data.atsScore || res.data.role || res.data.candidateSalaryEstimate)) {
          result = {
            ...res.data,
            name: res.data.name || candidateName,
            role: res.data.role || 'Candidate',
            careerDomain: res.data.careerDomain || 'Not available',
          };
        }
      } catch (_) {}

      if (!result) result = buildInsufficientDataResult(candidateName);

      clearInterval(ticker);
      setLoadPct(100);
      setLoadStep(PHASES.length - 1);

      setRawText(extractedText);
      setAiAnalysisResult(result);
      addResumeToHistory(candidateName, result, extractedText, file.name);

      setTimeout(() => {
        setPhase('done');
        navigate('/dashboard');
      }, 600);

    } catch (err) {
      clearInterval(ticker);
      const fallbackResult = buildInsufficientDataResult(candidateName);
      setRawText('');
      setAiAnalysisResult(fallbackResult);
      addResumeToHistory(candidateName, fallbackResult, '', file.name);
      setTimeout(() => {
        setPhase('done');
        navigate('/dashboard');
      }, 600);
    }
  };

  return (
    <div className="relative min-h-screen bg-[#0b0f19] text-white overflow-x-hidden font-sans selection:bg-indigo-600 selection:text-white">

      {/* ─── LIVE PARTICLE CANVAS ─────────────────── */}
      <LiveParticleCanvas />

      {/* ─── SCROLL PROGRESS BAR ─────────────────── */}
      <motion.div
        style={{ scaleX }}
        className="fixed top-0 left-0 right-0 h-[3px] origin-left z-50 bg-gradient-to-r from-indigo-500 via-violet-400 to-cyan-400 shadow-[0_0_15px_rgba(99,102,241,0.5)]"
      />

      {/* ════════════════════════════════════════════
          HERO SECTION
      ════════════════════════════════════════════ */}
      <section className="relative z-10 min-h-screen flex flex-col items-center justify-center px-4 pb-16 pt-24 text-center max-w-7xl mx-auto">

        {/* LIVE BADGE */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="mb-6 inline-flex items-center gap-2.5 px-4 py-2 rounded-full border border-indigo-500/30 bg-indigo-950/40 backdrop-blur-md text-xs font-semibold tracking-wide text-indigo-300 shadow-lg shadow-indigo-500/10"
        >
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-cyan-400 opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-cyan-400" />
          </span>
          <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
          AI-Powered Employability & Career Intelligence Ecosystem
        </motion.div>

        {/* HEADLINE */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.1 }}
          className="max-w-4xl mx-auto space-y-4"
        >
          <h1 className="text-4xl sm:text-6xl md:text-7xl font-extrabold tracking-tight leading-[1.1]">
            Land Your Dream Job with <br className="hidden sm:inline" />
            <span className="bg-clip-text text-transparent bg-gradient-to-r from-indigo-400 via-violet-300 to-cyan-300">
              VREZER AI Intelligence
            </span>
          </h1>
          <p className="max-w-2xl mx-auto text-slate-300 text-base sm:text-lg leading-relaxed font-normal">
            Enterprise-grade ATS resume parsing, target company skill gap analysis (Google, Amazon, Meta), live mock interview practice, and personalized salary predictions.
          </p>
        </motion.div>

        {/* METRICS BAR */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.25 }}
          className="mt-8 grid grid-cols-2 sm:grid-cols-4 gap-3 max-w-3xl w-full mx-auto"
        >
          {[
            { icon: <Users className="w-4 h-4 text-indigo-400" />, label: 'Resumes Analyzed', to: 50000, suffix: '+' },
            { icon: <ShieldCheck className="w-4 h-4 text-cyan-400" />, label: 'ATS Match Accuracy', to: 99, suffix: '.4%' },
            { icon: <TrendingUp className="w-4 h-4 text-emerald-400" />, label: 'Interview Call Rate', to: 3, suffix: '.2x' },
            { icon: <Brain className="w-4 h-4 text-violet-400" />, label: 'AI Intelligence Models', to: 100, suffix: '%' },
          ].map((s, i) => (
            <div key={i} className="p-3.5 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-xl flex flex-col items-center gap-1 shadow-md">
              <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-400">
                {s.icon} {s.label}
              </div>
              <span className="text-xl font-bold text-white font-mono">
                <AnimCounter to={s.to} suffix={s.suffix} duration={2000} />
              </span>
            </div>
          ))}
        </motion.div>

        {/* ══════════════════════════════════════════
            UPLOAD CARD CONTAINER
        ══════════════════════════════════════════ */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.35 }}
          className="mt-10 w-full max-w-2xl mx-auto"
        >
          <div
            onDragEnter={onDragEnter}
            onDragLeave={onDragLeave}
            onDragOver={onDragOver}
            onDrop={onDrop}
            className={`relative rounded-3xl transition-all duration-300 cursor-pointer ${
              dragActive
                ? 'scale-[1.02] ring-2 ring-indigo-500 shadow-2xl shadow-indigo-500/30'
                : 'shadow-2xl hover:shadow-indigo-500/20'
            }`}
            onClick={() => !file && fileInputRef.current?.click()}
          >
            <div className="relative overflow-hidden rounded-3xl border border-slate-800 bg-slate-900/70 backdrop-blur-xl p-8 sm:p-10">
              
              {/* Top ambient glow inside card */}
              <div className="absolute -top-24 left-1/2 -translate-x-1/2 w-96 h-48 bg-indigo-600/15 rounded-full blur-3xl pointer-events-none" />

              <div className="relative z-10 flex flex-col items-center gap-5">
                
                {/* Upload Icon Circle */}
                <div className={`w-20 h-20 rounded-2xl flex items-center justify-center transition-all duration-300 ${
                  file
                    ? 'bg-emerald-500/15 border border-emerald-500/40 text-emerald-400 shadow-lg shadow-emerald-500/20'
                    : 'bg-indigo-600/10 border border-indigo-500/30 text-indigo-400 hover:bg-indigo-600/20 shadow-lg shadow-indigo-500/10'
                }`}>
                  {file
                    ? <CheckCircle2 className="w-10 h-10 text-emerald-400" />
                    : <UploadCloud className="w-10 h-10 text-indigo-400" />
                  }
                </div>

                {/* Upload Instructions */}
                <div className="text-center">
                  {file ? (
                    <div className="space-y-2">
                      <p className="text-lg font-bold text-emerald-400">Resume Ready For Analysis ✓</p>
                      <div className="flex items-center gap-2 justify-center px-4 py-2 rounded-xl bg-slate-950/80 border border-emerald-500/30">
                        <FileText className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                        <span className="text-sm font-mono text-slate-200 truncate max-w-xs">{file.name}</span>
                        <span className="text-xs text-slate-400 ml-1">({(file.size / 1024).toFixed(0)} KB)</span>
                        <button
                          onClick={e => { e.stopPropagation(); setFile(null); }}
                          className="ml-2 p-1 rounded-lg hover:bg-rose-500/20 text-slate-400 hover:text-rose-400 transition"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="space-y-1.5">
                      <p className="text-xl sm:text-2xl font-bold text-white tracking-tight">
                        {dragActive ? 'Drop Your Resume File Now' : 'Drag & Drop Your Resume Here'}
                      </p>
                      <p className="text-slate-400 text-xs sm:text-sm font-medium">
                        Supports PDF, DOCX, DOC files up to 20MB
                      </p>
                    </div>
                  )}
                </div>

                {/* Buttons */}
                <div className="flex flex-wrap items-center justify-center gap-3 mt-1">
                  <button
                    onClick={e => { e.stopPropagation(); fileInputRef.current?.click(); }}
                    className="px-5 py-2.5 rounded-xl border border-slate-700 bg-slate-800/80 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition flex items-center gap-2"
                  >
                    <FileText className="w-4 h-4 text-indigo-400" />
                    {file ? 'Change File' : 'Browse Computer'}
                  </button>
                  <button
                    onClick={e => { e.stopPropagation(); loadSample(); }}
                    className="px-5 py-2.5 rounded-xl border border-indigo-500/30 bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-300 text-xs font-semibold transition flex items-center gap-2"
                  >
                    <Sparkles className="w-4 h-4 text-amber-300" />
                    Try Sample Resume
                  </button>
                </div>

              </div>
            </div>
          </div>

          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf,.docx,.doc"
            className="hidden"
            onChange={onFileInput}
          />

          {/* MAIN LAUNCH BUTTON */}
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={handleAnalyse}
            disabled={phase === 'loading'}
            className="mt-5 w-full py-4 px-8 rounded-2xl font-extrabold text-lg sm:text-xl tracking-wide uppercase text-white transition-all duration-300 relative overflow-hidden group shadow-xl shadow-indigo-600/30 border border-indigo-500/30 disabled:opacity-70 disabled:cursor-wait"
            style={{
              background: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #06b6d4 100%)',
            }}
          >
            <span className="relative flex items-center justify-center gap-3">
              {phase === 'loading'
                ? <><RefreshCw className="w-6 h-6 animate-spin" /> Processing Neural Analysis…</>
                : <><Zap className="w-6 h-6 text-amber-300 fill-amber-300" /> Launch AI Career Analysis ⚡</>
              }
            </span>
          </motion.button>

          {/* Quick Pillar Tags */}
          <div className="mt-4 flex flex-wrap justify-center gap-2">
            {[
              '🎯 ATS Score Audit', '💰 Salary Intelligence', '🏢 Target Company Match',
              '🗺️ Interactive Roadmap', '🎙️ AI Mock Interview', '⚙️ Recruiter Suite'
            ].map((tag, i) => (
              <span key={i} className="px-3 py-1 rounded-lg text-xs font-medium bg-slate-900/80 border border-slate-800 text-slate-400">
                {tag}
              </span>
            ))}
          </div>

        </motion.div>

      </section>

      {/* ════════════════════════════════════════════
          LOADING OVERLAY
      ════════════════════════════════════════════ */}
      <AnimatePresence>
        {phase === 'loading' && (
          <motion.div
            key="overlay"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-xl p-4"
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="w-full max-w-lg rounded-3xl border border-slate-800 bg-slate-900/95 backdrop-blur-2xl shadow-2xl p-8 text-center space-y-6 overflow-hidden"
            >
              <div className="w-16 h-16 rounded-2xl bg-indigo-600/20 border border-indigo-500/40 flex items-center justify-center mx-auto text-indigo-400 shadow-lg shadow-indigo-500/20">
                <Brain className="w-8 h-8 animate-pulse text-indigo-400" />
              </div>

              <div>
                <p className="text-xs font-mono font-bold text-indigo-400 uppercase tracking-widest mb-1">
                  Step {loadStep + 1} of {PHASES.length}
                </p>
                <h3 className="text-white font-bold text-lg">
                  {PHASES[loadStep]}
                </h3>
              </div>

              {/* Progress bar */}
              <div className="w-full bg-slate-950 rounded-full h-3 border border-slate-800 overflow-hidden">
                <motion.div
                  className="h-full rounded-full bg-gradient-to-r from-indigo-500 via-violet-500 to-cyan-400 shadow-[0_0_12px_#6366f1]"
                  initial={{ width: '0%' }}
                  animate={{ width: `${loadPct}%` }}
                  transition={{ duration: 0.6, ease: 'easeOut' }}
                />
              </div>

              <p className="text-slate-400 text-xs font-mono">
                VREZER AI NEURAL ENGINE · {loadPct}% COMPLETE
              </p>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ════════════════════════════════════════════
          SYSTEM PILLARS FEATURE GRID
      ════════════════════════════════════════════ */}
      <section className="relative z-10 py-20 px-4 max-w-7xl mx-auto border-t border-slate-800/80">
        
        <div className="text-center mb-14 space-y-3">
          <span className="text-xs font-mono font-bold text-indigo-400 uppercase tracking-widest px-3.5 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/20">
            Next-Gen AI Capabilities
          </span>
          <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
            7 Powerful Pillars of Career Intelligence
          </h2>
          <p className="text-slate-400 text-base max-w-xl mx-auto">
            Everything job-seekers, recruiters, and career advisors need in a unified AI ecosystem.
          </p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {[
            {
              icon: <ShieldCheck className="w-6 h-6 text-indigo-400" />,
              title: 'AI Resume & ATS Intelligence',
              desc: 'Deep PDF parsing with granular ATS score breakdown, formatting flaw flags (margin, table, icon, font issues), and missing keyword analysis.',
              link: '/ats-analyzer'
            },
            {
              icon: <Target className="w-6 h-6 text-cyan-400" />,
              title: 'Job Description Matcher',
              desc: 'Upload target job descriptions to compute instant semantic match scores, missing tech stack items, and custom bullet point recommendations.',
              link: '/jd-matcher'
            },
            {
              icon: <Building2 className="w-6 h-6 text-violet-400" />,
              title: 'Target Company Skill Gap',
              desc: 'Benchmark your profile against hiring bars of Google, Amazon, Meta, Microsoft, and Netflix with concrete learning actions.',
              link: '/skill-gap'
            },
            {
              icon: <Map className="w-6 h-6 text-emerald-400" />,
              title: 'Interactive Career Roadmap',
              desc: 'AI-generated step-by-step career path trajectory with recommended projects, certifications, and target milestones.',
              link: '/career-roadmap'
            },
            {
              icon: <HelpCircle className="w-6 h-6 text-amber-400" />,
              title: 'AI Mock Interview Studio',
              desc: 'Interactive real-time interview engine for Technical, Behavioral, and System Design practice with live feedback.',
              link: '/ai-interview'
            },
            {
              icon: <DollarSign className="w-6 h-6 text-rose-400" />,
              title: 'Compensation & Portfolio Intelligence',
              desc: 'Predict market salary ranges in India (LPA) & Global ($ USD) alongside GitHub and LinkedIn profile audit scores.',
              link: '/salary-predictor'
            },
          ].map((item, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.08 }}
              onClick={() => navigate(item.link)}
              className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-xl hover:border-indigo-500/40 hover:bg-slate-800/60 transition-all duration-300 cursor-pointer group shadow-lg flex flex-col justify-between"
            >
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center group-hover:scale-110 transition-transform">
                  {item.icon}
                </div>
                <h3 className="text-lg font-bold text-white group-hover:text-indigo-300 transition-colors">
                  {item.title}
                </h3>
                <p className="text-slate-400 text-xs sm:text-sm leading-relaxed">
                  {item.desc}
                </p>
              </div>
              <div className="mt-6 flex items-center gap-1.5 text-xs font-semibold text-indigo-400 group-hover:translate-x-1 transition-transform">
                Explore Module <ArrowRight className="w-3.5 h-3.5" />
              </div>
            </motion.div>
          ))}
        </div>

        {/* RECRUITER & ADMIN CALLOUT */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mt-16 p-8 sm:p-10 rounded-3xl bg-gradient-to-r from-indigo-950/60 via-slate-900 to-violet-950/40 border border-indigo-500/20 backdrop-blur-xl flex flex-col md:flex-row items-center justify-between gap-6 shadow-2xl"
        >
          <div className="space-y-2 text-left max-w-xl">
            <span className="text-[10px] font-bold text-indigo-300 uppercase tracking-widest">Enterprise Talent Suite</span>
            <h3 className="text-2xl font-bold text-white">Recruiter & Campus Placement Suite</h3>
            <p className="text-slate-300 text-sm">
              Batch multi-resume analysis, automated candidate skill ranking, shortlisted candidate matrix reports, and college administrative telemetry.
            </p>
          </div>
          <button
            onClick={() => navigate('/recruiter')}
            className="px-6 py-3.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs uppercase tracking-wider transition shadow-lg shadow-indigo-500/25 shrink-0 flex items-center gap-2"
          >
            <Users className="w-4 h-4" /> Open Recruiter Portal
          </button>
        </motion.div>

      </section>

    </div>
  );
};
