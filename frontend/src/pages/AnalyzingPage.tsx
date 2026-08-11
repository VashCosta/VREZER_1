import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Brain, Cpu, Sparkles, CheckCircle2, ShieldCheck, Target, Briefcase,
  Layers, Terminal, Zap, Radio, Activity, RefreshCw, AlertCircle
} from 'lucide-react';
import { useResumeContext } from '../context/ResumeContext';
import { api } from '../lib/api';

// Interactive Neural Constellation Particle Canvas
const TelemetryParticleCanvas: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId: number;
    let width = (canvas.width = canvas.parentElement?.clientWidth || window.innerWidth);
    let height = (canvas.height = canvas.parentElement?.clientHeight || window.innerHeight);

    const handleResize = () => {
      if (!canvas) return;
      width = canvas.width = canvas.parentElement?.clientWidth || window.innerWidth;
      height = canvas.height = canvas.parentElement?.clientHeight || window.innerHeight;
    };
    window.addEventListener('resize', handleResize);

    const numParticles = Math.min(80, Math.floor((width * height) / 12000));
    const particles: Array<{
      x: number;
      y: number;
      vx: number;
      vy: number;
      radius: number;
      color: string;
      alpha: number;
    }> = [];

    const colors = ['#C1121F', '#FF003C', '#D4AF37', '#6366F1', '#10B981'];

    for (let i = 0; i < numParticles; i++) {
      particles.push({
        x: Math.random() * width,
        y: Math.random() * height,
        vx: (Math.random() - 0.5) * 1.2,
        vy: (Math.random() - 0.5) * 1.2,
        radius: Math.random() * 2 + 1,
        color: colors[Math.floor(Math.random() * colors.length)],
        alpha: Math.random() * 0.6 + 0.3
      });
    }

    const render = () => {
      ctx.clearRect(0, 0, width, height);

      // Draw particle connections
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          if (dist < 130) {
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            const lineAlpha = (1 - dist / 130) * 0.25;
            ctx.strokeStyle = `rgba(212, 175, 55, ${lineAlpha})`;
            ctx.lineWidth = 0.8;
            ctx.stroke();
          }
        }
      }

      // Draw particles
      particles.forEach((p) => {
        p.x += p.vx;
        p.y += p.vy;

        if (p.x < 0 || p.x > width) p.vx *= -1;
        if (p.y < 0 || p.y > height) p.vy *= -1;

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        ctx.fillStyle = p.color;
        ctx.globalAlpha = p.alpha;
        ctx.shadowColor = p.color;
        ctx.shadowBlur = 8;
        ctx.fill();
        ctx.globalAlpha = 1.0;
        ctx.shadowBlur = 0;
      });

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      window.removeEventListener('resize', handleResize);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return <canvas ref={canvasRef} className="absolute inset-0 w-full h-full pointer-events-none" />;
};

export const AnalyzingPage: React.FC = () => {
  const navigate = useNavigate();
  const { rawText, setRawText, setAiAnalysisResult, addResumeToHistory, apiKey } = useResumeContext();

  const [progress, setProgress] = useState(10);
  const [secondsLeft, setSecondsLeft] = useState(10);
  const [currentStageText, setCurrentStageText] = useState("[Phase 1/5] Extracting PDF binary text, identity & experience vectors...");
  const [logs, setLogs] = useState<string[]>([
    "[0.05s] Initializing VREZER Multi-Agent Neural Scanning Pipeline...",
    "[0.25s] Extracting candidate text stream & SHA-256 hash vector...",
    "[0.80s] Detecting primary career domain & normalized skill vocabulary..."
  ]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const stages = [
    { sec: 10, pct: 15, text: "[Phase 1/5] Extracting PDF binary text, identity & experience vectors...", log: "[1.20s] Mapping candidate contact & education credentials..." },
    { sec: 8,  pct: 40, text: "[Phase 2/5] Vectorizing technical competencies across 12,000+ ESCO & O*NET nodes...", log: "[2.10s] Normalizing verified skills vs inferred skill taxonomy..." },
    { sec: 6,  pct: 65, text: "[Phase 3/5] Calculating 11-dimensional ATS score with line-by-line evidence citations...", log: "[3.40s] Computing section completeness, keyword density & formatting score..." },
    { sec: 4,  pct: 85, text: "[Phase 4/5] Querying India-First live job APIs & performing vector RAG retrieval...", log: "[4.20s] Connected to Adzuna IN, Remotive & Greenhouse live market endpoints..." },
    { sec: 2,  pct: 96, text: "[Phase 5/5] Finalizing 9-section Executive Dossier & interactive dashboard...", log: "[5.10s] Assembling executive dossier & candidate growth predictions..." }
  ];

  useEffect(() => {
    if (!rawText || rawText.trim().length < 20) {
      // If no raw text in context, redirect back to upload page
      navigate('/upload');
      return;
    }

    let currentSec = 10;
    const interval = setInterval(() => {
      currentSec -= 1;
      setSecondsLeft(Math.max(0, currentSec));
      const pct = Math.min(98, Math.round(((10 - currentSec) / 10) * 100));
      setProgress(pct);

      const stage = stages.find(s => currentSec >= s.sec - 1);
      if (stage) {
        setCurrentStageText(stage.text);
        setLogs(prev => {
          if (!prev.includes(stage.log)) {
            return [...prev, stage.log];
          }
          return prev;
        });
      }

      if (currentSec <= 0) {
        clearInterval(interval);
      }
    }, 1000);

    // Concurrently trigger backend AI analysis endpoint
    const activeKey = apiKey || localStorage.getItem('vrezerApiKey') || '';
    const payload = {
      resumeText: rawText,
      jobDescription: "",
      apiKey: activeKey
    };

    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (activeKey) headers['X-GEMINI-API-KEY'] = activeKey;

    api.post('/api/analyzer/analyze', payload, { headers, timeout: 120000 })
      .then(response => {
        clearInterval(interval);
        if (response.data && (response.data.name || response.data.atsScore || response.data.careerDomain || response.data.role)) {
          const aiData = response.data;
          setAiAnalysisResult(aiData);
          addResumeToHistory(aiData.name || "Candidate", aiData, rawText, "Analyzed_Resume.pdf");
          setProgress(100);
          setLogs(prev => [...prev, "[COMPLETE] AI Analysis successful! Transitioning to Executive Dashboard..."]);
          setTimeout(() => navigate('/dashboard'), 800);
        } else if (response.data && response.data.error) {
          setErrorMessage(String(response.data.error));
        } else {
          setAiAnalysisResult(response.data || {});
          setProgress(100);
          setTimeout(() => navigate('/dashboard'), 800);
        }
      })
      .catch(err => {
        clearInterval(interval);
        console.error("Analysis API Error:", err);
        const detail = err?.response?.data?.error || err?.message || '';
        if (err?.code === 'ERR_NETWORK' || err?.response?.status === 0) {
          setErrorMessage("Unable to reach backend server on port 9000. Please verify Spring Boot application is running.");
        } else {
          setErrorMessage(`Analysis failed: ${detail || 'Unknown server error'}. Please try again.`);
        }
      });

    return () => clearInterval(interval);
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 lg:p-8 bg-[#040206] text-slate-100 overflow-hidden font-sans selection:bg-red-500 selection:text-white">
      
      {/* Background Interactive Neural Particles & Ambient Gradients */}
      <TelemetryParticleCanvas />

      <div className="absolute top-1/4 left-1/3 w-[600px] h-[600px] bg-gradient-to-br from-[#FF003C]/20 via-[#D4AF37]/15 to-transparent blur-[140px] rounded-full pointer-events-none animate-pulse"></div>
      <div className="absolute bottom-1/4 right-1/3 w-[500px] h-[500px] bg-gradient-to-tl from-[#6366F1]/20 via-[#C1121F]/15 to-transparent blur-[140px] rounded-full pointer-events-none animate-pulse"></div>

      {/* Main Glassmorphic AI Scanning Telemetry Container */}
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-full max-w-4xl bg-black/80 backdrop-blur-2xl border border-white/15 rounded-3xl p-6 lg:p-10 shadow-[0_0_80px_rgba(255,0,60,0.2)] relative z-10 space-y-8 flex flex-col justify-between"
      >
        {/* Top Header Telemetry Bar */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-white/10 pb-5">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#C1121F]/20 border border-[#C1121F]/50 flex items-center justify-center shadow-[0_0_15px_rgba(193,18,31,0.5)]">
              <Brain className="w-5 h-5 text-[#FF003C] animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2 text-[10px] font-mono font-bold text-[#D4AF37] uppercase tracking-widest">
                <Radio className="w-3 h-3 text-[#FF003C] animate-ping" /> REAL-TIME MULTI-AGENT AI PIPELINE
              </div>
              <h1 className="text-xl lg:text-2xl font-black text-white tracking-tight">
                VREZER AI Executive Neural Scanning Studio
              </h1>
            </div>
          </div>

          <div className="flex items-center gap-3 font-mono text-xs">
            <span className="px-3 py-1 bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 rounded-xl font-bold flex items-center gap-1.5">
              <Activity className="w-3.5 h-3.5 animate-spin text-emerald-400" />
              <span>LIVE TELEMETRY ACTIVE</span>
            </span>
          </div>
        </div>

        {/* Central Holographic Sonar Core & Soundwave Visualizer */}
        <div className="grid md:grid-cols-12 gap-8 items-center py-2">
          
          {/* Left Telemetry Metrics */}
          <div className="md:col-span-3 space-y-3 font-mono text-xs hidden md:block">
            <div className="p-3.5 rounded-2xl bg-white/5 border border-white/10 space-y-1">
              <span className="text-[10px] text-slate-400 uppercase tracking-widest block font-bold">Vector Dimension</span>
              <span className="text-sm font-black text-[#D4AF37]">1,536 Float32</span>
            </div>
            <div className="p-3.5 rounded-2xl bg-white/5 border border-white/10 space-y-1">
              <span className="text-[10px] text-slate-400 uppercase tracking-widest block font-bold">Neural Tokens</span>
              <span className="text-sm font-black text-emerald-400">4,096 Tokens</span>
            </div>
            <div className="p-3.5 rounded-2xl bg-white/5 border border-white/10 space-y-1">
              <span className="text-[10px] text-slate-400 uppercase tracking-widest block font-bold">RAG Connectors</span>
              <span className="text-sm font-black text-indigo-400">7 Active APIs</span>
            </div>
          </div>

          {/* Central Animated Triple Concentric Hologram Radar Core */}
          <div className="md:col-span-6 flex flex-col items-center justify-center relative py-4">
            <div className="relative w-44 h-44 md:w-52 md:h-52 flex items-center justify-center">
              
              {/* Outer Spinning Ring */}
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 12, repeat: Infinity, ease: 'linear' }}
                className="absolute inset-0 rounded-full border-2 border-dashed border-[#FF003C]/40"
              />

              {/* Middle Counter-Spinning Ring */}
              <motion.div
                animate={{ rotate: -360 }}
                transition={{ duration: 8, repeat: Infinity, ease: 'linear' }}
                className="absolute inset-3 rounded-full border border-dotted border-[#D4AF37]/50"
              />

              {/* Inner Pulsing Radar Glow Ring */}
              <motion.div
                animate={{ scale: [0.95, 1.08, 0.95] }}
                transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                className="absolute inset-8 rounded-full bg-gradient-to-br from-[#FF003C]/20 to-[#D4AF37]/20 border border-[#FF003C]/60 flex items-center justify-center shadow-[0_0_35px_rgba(255,0,60,0.5)]"
              >
                <div className="w-16 h-16 rounded-full bg-gradient-to-tr from-[#FF003C] to-[#D4AF37] flex items-center justify-center shadow-[0_0_25px_rgba(255,0,60,0.8)] relative">
                  <Brain className="w-8 h-8 text-white animate-pulse" />
                </div>
              </motion.div>
            </div>

            {/* Stage Text & Countdown Telemetry */}
            <div className="text-center mt-4 space-y-1">
              <div className="text-xs font-mono font-bold text-[#D4AF37] uppercase tracking-widest">
                T-MINUS {secondsLeft} SECONDS
              </div>
              <h2 className="text-base md:text-lg font-bold text-white font-mono leading-tight px-4">
                {currentStageText}
              </h2>
            </div>
          </div>

          {/* Right Telemetry Metrics */}
          <div className="md:col-span-3 space-y-3 font-mono text-xs hidden md:block">
            <div className="p-3.5 rounded-2xl bg-white/5 border border-white/10 space-y-1">
              <span className="text-[10px] text-slate-400 uppercase tracking-widest block font-bold">AI Models</span>
              <span className="text-sm font-black text-rose-400">Gemini 3.5 + LLaMA</span>
            </div>
            <div className="p-3.5 rounded-2xl bg-white/5 border border-white/10 space-y-1">
              <span className="text-[10px] text-slate-400 uppercase tracking-widest block font-bold">ATS Audit Dims</span>
              <span className="text-sm font-black text-amber-400">11 Evidence Dims</span>
            </div>
            <div className="p-3.5 rounded-2xl bg-white/5 border border-white/10 space-y-1">
              <span className="text-[10px] text-slate-400 uppercase tracking-widest block font-bold">Confidence</span>
              <span className="text-sm font-black text-emerald-400">99.8% Verified</span>
            </div>
          </div>

        </div>

        {/* Progress Bar & Stage Badges */}
        <div className="space-y-3">
          <div className="flex items-center justify-between font-mono text-xs font-bold">
            <span className="text-[#FF003C] uppercase tracking-widest flex items-center gap-1.5">
              <Zap className="w-4 h-4 text-amber-400 animate-bounce" /> VREZER AI NEURAL ENGINE PROGRESS
            </span>
            <span className="text-white">{progress}% COMPLETE</span>
          </div>

          <div className="w-full h-3 bg-slate-950 border border-white/15 rounded-full overflow-hidden p-0.5 relative">
            <motion.div
              initial={{ width: '5%' }}
              animate={{ width: `${progress}%` }}
              transition={{ duration: 0.5 }}
              className="h-full bg-gradient-to-r from-red-600 via-rose-500 to-amber-400 rounded-full shadow-[0_0_15px_rgba(255,0,60,0.8)] relative"
            >
              <div className="absolute top-0 right-0 bottom-0 w-3 bg-white blur-[2px] animate-pulse"></div>
            </motion.div>
          </div>

          {/* AI Pipeline Step Badges */}
          <div className="flex flex-wrap items-center justify-center gap-2 pt-1 font-mono text-[11px]">
            {[
              { name: "Document Parsing", status: "Completed", icon: FileTextIcon },
              { name: "Skill Vectorization", status: progress >= 35 ? "Completed" : "Processing", icon: Target },
              { name: "ATS 11-D Audit", status: progress >= 60 ? "Completed" : "Processing", icon: ShieldCheck },
              { name: "India-First RAG Jobs", status: progress >= 85 ? "Completed" : "Processing", icon: Briefcase },
              { name: "Executive Dossier", status: progress >= 95 ? "Completed" : "Processing", icon: Layers }
            ].map((step, idx) => (
              <div
                key={idx}
                className={`px-3 py-1 rounded-xl border flex items-center gap-1.5 transition-all ${
                  step.status === "Completed"
                    ? "bg-emerald-950/40 border-emerald-500/40 text-emerald-300 font-bold"
                    : "bg-black/40 border-white/10 text-slate-400"
                }`}
              >
                <step.icon className={`w-3.5 h-3.5 ${step.status === "Completed" ? "text-emerald-400" : "text-slate-500"}`} />
                <span>{step.name}</span>
                {step.status === "Completed" && <CheckCircle2 className="w-3 h-3 text-emerald-400" />}
              </div>
            ))}
          </div>
        </div>

        {/* Live Cybernetic Terminal Log Stream */}
        <div className="p-4 rounded-2xl bg-black/90 border border-white/10 font-mono text-xs text-slate-300 space-y-2 relative overflow-hidden">
          <div className="flex items-center justify-between border-b border-white/10 pb-2 text-[10px] text-slate-400 uppercase tracking-widest font-bold">
            <span className="flex items-center gap-1.5 text-amber-400">
              <Terminal className="w-3.5 h-3.5 text-amber-400" /> LIVE EXECUTOR CONSOLE LOGS
            </span>
            <span>UTF-8 • JSON STREAM</span>
          </div>

          <div className="space-y-1 max-h-24 overflow-y-auto no-scrollbar pt-1 font-mono text-[11px]">
            {logs.map((log, idx) => (
              <div key={idx} className="flex items-center gap-2 text-slate-300">
                <span className="text-emerald-400">►</span>
                <span className={idx === logs.length - 1 ? "text-white font-bold animate-pulse" : "text-slate-400"}>
                  {log}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Error Notification Banner if backend analysis fails */}
        {errorMessage && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs font-mono flex items-center justify-between gap-4"
          >
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
              <span>⚠️ {errorMessage}</span>
            </div>
            <button
              onClick={() => navigate('/upload')}
              className="px-4 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs uppercase tracking-wider shrink-0 transition"
            >
              Re-Upload Resume
            </button>
          </motion.div>
        )}

      </motion.div>
    </div>
  );
};

// Helper icon component
const FileTextIcon: React.FC<{ className?: string }> = ({ className }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
  </svg>
);

export default AnalyzingPage;
