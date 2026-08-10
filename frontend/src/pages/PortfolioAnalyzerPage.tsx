import React from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { Code, Github, Linkedin, Sparkles, CheckCircle2, AlertTriangle, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const PortfolioAnalyzerPage: React.FC = () => {
  const { aiAnalysisResult } = useResumeContext();
  const navigate = useNavigate();

  if (!aiAnalysisResult || aiAnalysisResult.error || aiAnalysisResult.status === 'FAILED') {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] text-center p-6 space-y-4">
        <div className="inline-flex p-4 rounded-full bg-red-500/10 text-red-400 border border-red-500/20 mb-2">
          <Code className="w-12 h-12 animate-pulse" />
        </div>
        <h2 className="text-xl font-extrabold text-white">No Active Resume Analysis Found</h2>
        <p className="text-xs text-slate-400 max-w-md leading-relaxed">
          Please upload and parse your resume first in the Resume Parser module. 
          The AI will then extract your GitHub/LinkedIn links and perform a dynamic portfolio readiness audit.
        </p>
        <Button onClick={() => navigate('/upload')} icon={<ArrowRight className="w-4 h-4" />}>
          Go to Resume Parser
        </Button>
      </div>
    );
  }

  const result = aiAnalysisResult;
  const github = result.github || 'Not detected in resume';
  const linkedin = result.linkedin || 'Not detected in resume';
  
  const insights = result.recruiterInsights || {};
  const gitScore = insights.githubReadiness || 0;
  const linkScore = insights.linkedinReadiness || 0;
  const portScore = insights.portfolioReadiness || 0;

  const actions = result.nextBestActions || [];
  const improvements = result.resumeImprovement?.weakBulletPoints || [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
          <Code className="w-6 h-6 text-red-400" /> Portfolio & LinkedIn Health Auditor
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Scans and analyzes your online developer presence and professional networks extracted from your resume.
        </p>
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        
        {/* Profile Links & Status */}
        <GlassCard className="p-6 space-y-4">
          <h3 className="font-bold text-sm text-white border-b border-slate-800 pb-2">Profile Links Extracted</h3>
          
          <div className="space-y-3">
            <div className="p-3 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Github className="w-4 h-4 text-purple-400" />
                <span className="text-xs text-slate-300 font-mono text-[11px] truncate max-w-xs">{github}</span>
              </div>
              <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                github.includes('github.com') ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'
              }`}>
                {github.includes('github.com') ? 'LINK FOUND' : 'MISSING'}
              </span>
            </div>

            <div className="p-3 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Linkedin className="w-4 h-4 text-blue-400" />
                <span className="text-xs text-slate-300 font-mono text-[11px] truncate max-w-xs">{linkedin}</span>
              </div>
              <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                linkedin.includes('linkedin.com') ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'
              }`}>
                {linkedin.includes('linkedin.com') ? 'LINK FOUND' : 'MISSING'}
              </span>
            </div>
          </div>

          <div className="pt-2 text-xs text-slate-400">
            <span className="font-bold text-slate-300">Recruiter Feedback Note:</span>
            <p className="mt-1 leading-relaxed">{result.resumeImprovement?.recruiterStyleFeedback || 'Ensure your profile URLs are clear and clickable.'}</p>
          </div>
        </GlassCard>

        {/* Dynamic Readiness Scores */}
        <GlassCard className="p-6 space-y-4">
          <h3 className="font-bold text-sm text-white border-b border-slate-800 pb-2">Online Presence Readiness Score</h3>
          
          <div className="space-y-4">
            <div className="space-y-1.5">
              <div className="flex justify-between text-xs font-bold">
                <span className="text-slate-400">GitHub Profile Readiness</span>
                <span className="text-white">{gitScore}%</span>
              </div>
              <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                <div className="h-full bg-red-500 animate-pulse" style={{ width: `${gitScore}%` }} />
              </div>
            </div>

            <div className="space-y-1.5">
              <div className="flex justify-between text-xs font-bold">
                <span className="text-slate-400">LinkedIn Profile Alignment</span>
                <span className="text-white">{linkScore}%</span>
              </div>
              <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                <div className="h-full bg-red-500 animate-pulse" style={{ width: `${linkScore}%` }} />
              </div>
            </div>

            <div className="space-y-1.5">
              <div className="flex justify-between text-xs font-bold">
                <span className="text-slate-400">Portfolio Readiness Rating</span>
                <span className="text-white">{portScore}%</span>
              </div>
              <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                <div className="h-full bg-red-500 animate-pulse" style={{ width: `${portScore}%` }} />
              </div>
            </div>
          </div>
        </GlassCard>

      </div>

      {/* Recommended Next Actions */}
      <div className="grid md:grid-cols-2 gap-6">
        
        <GlassCard className="p-6 space-y-4">
          <h3 className="font-bold text-sm text-white flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Action Items to Improve Profiles
          </h3>
          <div className="space-y-3">
            {actions.map((item: string, idx: number) => (
              <div key={idx} className="p-3 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-350 leading-relaxed flex items-start gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-red-500 mt-1.5 shrink-0" />
                <span>{item}</span>
              </div>
            ))}
          </div>
        </GlassCard>

        <GlassCard className="p-6 space-y-4">
          <h3 className="font-bold text-sm text-white flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-amber-400" /> AI-Rewritten Project Bullet Points
          </h3>
          <p className="text-xs text-slate-400">
            Replacing generic descriptions with quantified metrics on your GitHub/portfolio can increase interview callbacks by up to 40%:
          </p>
          <div className="space-y-3">
            {improvements.map((bullet: any, idx: number) => (
              <div key={idx} className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 space-y-2 text-xs">
                <div>
                  <span className="text-[10px] font-bold text-slate-500 block uppercase tracking-wider">Original:</span>
                  <p className="text-slate-400">{bullet.original}</p>
                </div>
                <div className="pt-1.5 border-t border-slate-800/60">
                  <span className="text-[10px] font-bold text-emerald-400 block uppercase tracking-wider">AI Rewritten (Quantified):</span>
                  <p className="text-slate-200">{bullet.aiRewritten}</p>
                </div>
              </div>
            ))}
          </div>
        </GlassCard>

      </div>
    </div>
  );
};
