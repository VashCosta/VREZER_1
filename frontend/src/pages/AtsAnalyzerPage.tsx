import React from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { ShieldCheck, AlertTriangle, CheckCircle2, ListFilter, ArrowRight, Sparkles, HelpCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const AtsAnalyzerPage: React.FC = () => {
  const { aiAnalysisResult } = useResumeContext();
  const navigate = useNavigate();

  if (!aiAnalysisResult || aiAnalysisResult.error || aiAnalysisResult.status === 'FAILED') {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] text-center p-6 space-y-4">
        <div className="inline-flex p-4 rounded-full bg-red-500/10 text-red-400 border border-red-500/20 mb-2">
          <ShieldCheck className="w-12 h-12 animate-pulse" />
        </div>
        <h2 className="text-xl font-extrabold text-white">No Active Resume Analysis Found</h2>
        <p className="text-xs text-slate-400 max-w-md leading-relaxed">
          Please upload and parse your resume first in the Resume Parser module. 
          The AI will then generate a detailed ATS compatibility score, formatting audit, and keyword optimization breakdown.
        </p>
        <Button onClick={() => navigate('/upload')} icon={<ArrowRight className="w-4 h-4" />}>
          Go to Resume Parser
        </Button>
      </div>
    );
  }

  const result = aiAnalysisResult;
  const atsScore = result.atsScore || 0;
  const atsScoreText = result.atsScoreText || 'NEEDS IMPROVEMENT';
  
  const scoreDetails = result.atsScoreDetails || {};
  const kwScore = scoreDetails.keywordOptimizationScore ?? null;
  const fmtScore = scoreDetails.formattingScore ?? null;
  const secScore = scoreDetails.sectionCompletenessScore ?? null;
  const achScore = scoreDetails.achievementScore ?? null;
  const explanation = scoreDetails.explanation || 'No AI explanation available for this analysis.';
  const citations = scoreDetails.citations || [];
  const confidence = scoreDetails.confidence || null;

  const improvements = result.swot?.improvements || [];
  const suggestions = result.resumeImprovement?.formattingRecommendations || [];
  const keywordSugg = result.resumeImprovement?.keywordOptimizationSuggestions || [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
          <ShieldCheck className="w-6 h-6 text-red-400" /> AI ATS Formatting & Keyword Auditor
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Displays ATS compatibility score, formatting recommendations, and keyword suggestions extracted dynamically by the AI.
        </p>
      </div>

      <div className="grid md:grid-cols-3 gap-6">
        
        {/* ATS Score Radial Card */}
        <GlassCard className="text-center p-8 flex flex-col items-center justify-center">
          <div className="w-36 h-36 rounded-full border-8 border-red-500/20 border-t-red-500 flex items-center justify-center relative shadow-[0_0_15px_rgba(239,68,68,0.1)]">
            <span className="text-4xl font-black text-white">{atsScore}%</span>
          </div>
          <h3 className="font-bold text-sm text-white mt-4">ATS Compatibility Score</h3>
          <span className={`mt-2 px-3 py-1 rounded-full text-[10px] font-black tracking-wider uppercase ${
            atsScore >= 85 ? 'bg-emerald-500/20 text-emerald-400' :
            atsScore >= 70 ? 'bg-amber-500/20 text-amber-400' : 'bg-red-500/20 text-red-400'
          }`}>
            {atsScoreText}
          </span>
          <p className="text-[11px] text-slate-400 mt-3 italic text-center px-2">
            Confidence: {confidence || 'N/A — upgrade to AI API key for full confidence metrics'}
          </p>
        </GlassCard>

        {/* Score Breakdown Metrics */}
        <GlassCard className="md:col-span-2 space-y-4 p-6">
          <h3 className="font-bold text-sm text-white">Measurable Characteristics Scorecard</h3>
          <div className="grid grid-cols-2 gap-4">
            
            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Keyword Density</span>
              {kwScore !== null ? (
                <div className="flex items-center gap-2">
                  <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                    <div className="h-full bg-red-500" style={{ width: `${kwScore}%` }} />
                  </div>
                  <span className="text-xs font-extrabold text-white shrink-0">{kwScore}%</span>
                </div>
              ) : <span className="text-xs text-slate-500 italic">N/A — requires AI API key</span>}
            </div>

            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Formatting Quality</span>
              {fmtScore !== null ? (
                <div className="flex items-center gap-2">
                  <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                    <div className="h-full bg-red-500" style={{ width: `${fmtScore}%` }} />
                  </div>
                  <span className="text-xs font-extrabold text-white shrink-0">{fmtScore}%</span>
                </div>
              ) : <span className="text-xs text-slate-500 italic">N/A — requires AI API key</span>}
            </div>

            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Section Completeness</span>
              {secScore !== null ? (
                <div className="flex items-center gap-2">
                  <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                    <div className="h-full bg-red-500" style={{ width: `${secScore}%` }} />
                  </div>
                  <span className="text-xs font-extrabold text-white shrink-0">{secScore}%</span>
                </div>
              ) : <span className="text-xs text-slate-500 italic">N/A — requires AI API key</span>}
            </div>

            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Quantified Achievements</span>
              {achScore !== null ? (
                <div className="flex items-center gap-2">
                  <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                    <div className="h-full bg-red-500" style={{ width: `${achScore}%` }} />
                  </div>
                  <span className="text-xs font-extrabold text-white shrink-0">{achScore}%</span>
                </div>
              ) : <span className="text-xs text-slate-500 italic">N/A — requires AI API key</span>}
            </div>

          </div>

          <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 mt-4 space-y-2">
            <span className="text-xs font-bold text-red-400 block">AI Evaluation Reasoning</span>
            <p className="text-xs text-slate-300 leading-relaxed">{explanation}</p>
          </div>
        </GlassCard>

      </div>

      <div className="grid md:grid-cols-2 gap-6">
        
        {/* ATS Formatting & Section Fixes */}
        <GlassCard className="p-6 space-y-4">
          <h3 className="font-bold text-sm text-white flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-400" /> Structure & Formatting Suggestions
          </h3>
          <div className="space-y-2.5">
            {suggestions.length > 0 ? (
              suggestions.map((s: any, idx: number) => (
                <div key={idx} className="p-3 rounded-xl bg-slate-900/60 border border-slate-800/80 text-xs text-slate-200 flex items-start gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-red-500 mt-1.5 shrink-0" />
                  <span>{s}</span>
                </div>
              ))
            ) : (
              improvements.map((s: any, idx: number) => (
                <div key={idx} className="p-3 rounded-xl bg-slate-900/60 border border-slate-800/80 text-xs text-slate-200 flex items-start gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-red-500 mt-1.5 shrink-0" />
                  <span>{s}</span>
                </div>
              ))
            )}
          </div>
        </GlassCard>

        {/* Keyword Suggestions */}
        <GlassCard className="p-6 space-y-4">
          <h3 className="font-bold text-sm text-white flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-purple-400" /> Target Keyword Optimization
          </h3>
          <p className="text-xs text-slate-400">
            Including these industry-standard keywords related to your profile will boost your ATS compatibility score:
          </p>
          <div className="flex flex-wrap gap-2">
            {keywordSugg.length > 0 ? (
              keywordSugg.map((kw: any, idx: number) => (
                <span key={idx} className="px-3 py-1.5 rounded-xl bg-purple-950/40 text-purple-300 border border-purple-800/40 text-xs font-semibold">
                  + {kw}
                </span>
              ))
            ) : (
              <span className="text-xs text-slate-500">No specific keyword enhancements detected. Ensure your technical skills list is fully detailed.</span>
            )}
          </div>
        </GlassCard>

      </div>
    </div>
  );
};
