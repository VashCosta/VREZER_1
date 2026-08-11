import React, { useState } from 'react';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { Target, Sparkles, CheckCircle2, XCircle, ArrowRight, ShieldCheck, AlertTriangle, FileText, Info } from 'lucide-react';
import { api } from '../lib/api';
import { useResumeContext } from '../context/ResumeContext';
import { motion } from 'framer-motion';

export const JdMatcherPage: React.FC = () => {
  const { rawText, aiAnalysisResult, apiKey, setAiAnalysisResult } = useResumeContext();
  const [jdText, setJdText] = useState('');
  const [loading, setLoading] = useState(false);
  const [matchResult, setMatchResult] = useState<any>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleMatch = async () => {
    if (!jdText.trim()) {
      setErrorMsg('Please paste a job description text to perform comparison.');
      return;
    }

    const currentResumeText = rawText || (aiAnalysisResult ? JSON.stringify(aiAnalysisResult) : '');
    if (!currentResumeText || currentResumeText.trim().length < 15) {
      setErrorMsg('No active resume found. Please upload a resume first.');
      return;
    }

    setLoading(true);
    setErrorMsg(null);

    try {
      const payload = {
        resumeText: currentResumeText,
        jobDescription: jdText.trim(),
        apiKey: apiKey || ""
      };

      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      if (apiKey) {
        headers['X-GEMINI-API-KEY'] = apiKey;
      }

      const response = await api.post('/api/analyzer/jd-match', payload, { headers });

      if (response.data) {
        setMatchResult(response.data);
        // Also update shared dossier context with new job-specific analysis
        if (response.data.name) {
          setAiAnalysisResult(response.data);
        }
      }
    } catch (err: any) {
      console.error(err);
      setErrorMsg(err.response?.data?.message || err.message || 'Error processing Job Description Match.');
    } finally {
      setLoading(false);
    }
  };

  const matchPercentage = matchResult?.atsScore || matchResult?.bestMatchingJobRoles?.[0]?.matchPercentage || 0;
  const targetRole = matchResult?.role || 'Job Description Target Match';
  const matchedSkills: string[] = matchResult?.topSkills || [];
  const missingSkills: string[] = matchResult?.swot?.missingSkills || matchResult?.skillIntelligence?.missingSkills || [];
  const keywordSuggestions: string[] = matchResult?.resumeImprovement?.keywordOptimizationSuggestions || [];
  const citations: string[] = matchResult?.atsScoreDetails?.citations || [];

  return (
    <div className="space-y-6 font-sans pb-16">
      
      {/* Header */}
      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-xs font-mono font-bold text-indigo-400 uppercase tracking-widest mb-2">
          <Target className="w-3.5 h-3.5 text-indigo-400" />
          WORKFLOW 17 — RESUME VS JOB DESCRIPTION MATCH ENGINE
        </div>
        <h1 className="text-3xl font-black text-white uppercase tracking-tight font-mono">
          Job Description Match Analyzer
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Paste any job posting or requirement description. VREZER calculates exact keyword density match %, skill gaps, and section compatibility with complete explainability.
        </p>
      </div>

      <div className="grid md:grid-cols-12 gap-6">
        
        {/* Input JD Box */}
        <div className="md:col-span-5 space-y-4">
          <GlassCard className="p-6 space-y-4 border-indigo-500/30 bg-[#0a0d16]">
            <h3 className="font-bold text-sm text-white uppercase tracking-wider font-mono flex items-center gap-2">
              <FileText className="w-4 h-4 text-indigo-400" /> Target Job Description
            </h3>

            <textarea
              value={jdText}
              onChange={(e) => setJdText(e.target.value)}
              rows={12}
              placeholder="Paste complete Job Description text here (e.g. Seeking Senior Full Stack Engineer proficient in Spring Boot, React, Docker, PostgreSQL, and AWS microservices...)"
              className="w-full bg-[#050811] border border-white/15 rounded-2xl p-4 text-xs font-mono text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 resize-none transition"
            />

            {errorMsg && (
              <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-300 text-xs font-mono flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            <Button
              onClick={handleMatch}
              isLoading={loading}
              icon={<Sparkles className="w-4 h-4 text-amber-300" />}
              className="w-full bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 border-none shadow-lg shadow-indigo-600/30"
            >
              Analyze & Match Against Resume
            </Button>
          </GlassCard>
        </div>

        {/* Results Box */}
        <div className="md:col-span-7 space-y-4">
          <GlassCard className="p-6 space-y-6 border-white/10 bg-[#0a0d16] min-h-[450px]">
            <h3 className="font-bold text-sm text-white uppercase tracking-wider font-mono flex items-center justify-between">
              <span>Match Analytics & Explanation</span>
              {matchResult && (
                <span className="px-3 py-1 rounded-full bg-indigo-500/20 text-indigo-300 text-xs font-mono border border-indigo-500/30">
                  Confidence: {matchResult.atsScoreDetails?.confidence || 'High'}
                </span>
              )}
            </h3>

            {!matchResult ? (
              <div className="flex flex-col items-center justify-center py-20 text-slate-500 text-xs text-center space-y-3">
                <div className="w-16 h-16 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-slate-400">
                  <Target className="w-8 h-8 text-indigo-400 opacity-60" />
                </div>
                <div>
                  <div className="font-mono font-bold text-slate-300 uppercase">Ready for JD Comparison</div>
                  <div className="text-slate-400 mt-1 max-w-sm">
                    Paste a target job posting on the left to generate match percentages, missing keywords, and section-by-section analysis.
                  </div>
                </div>
              </div>
            ) : (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="space-y-6 text-xs font-sans"
              >
                {/* Score Card Banner */}
                <div className="p-5 rounded-2xl bg-gradient-to-r from-indigo-950/60 to-purple-950/40 border border-indigo-500/40 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-mono font-bold text-indigo-300 uppercase tracking-widest block">MATCH COMPATIBILITY</span>
                    <div className="text-4xl font-black text-white mt-0.5">{matchPercentage}%</div>
                    <div className="text-slate-400 text-xs mt-1">Target Role: <strong className="text-white">{targetRole}</strong></div>
                  </div>
                  <div className="text-right">
                    <span className="px-4 py-2 rounded-2xl bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 font-mono font-bold uppercase text-xs">
                      {matchPercentage >= 85 ? 'EXCELLENT FIT' : matchPercentage >= 70 ? 'GOOD FIT' : 'NEEDS KEYWORDS'}
                    </span>
                  </div>
                </div>

                {/* Matched vs Missing Skills */}
                <div className="grid md:grid-cols-2 gap-4">
                  <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 space-y-2">
                    <span className="font-mono font-bold text-emerald-400 uppercase text-[11px] block flex items-center gap-1.5">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Verified Matched Skills ({matchedSkills.length})
                    </span>
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {matchedSkills.map((s, idx) => (
                        <span key={idx} className="px-2.5 py-1 rounded-lg bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-[11px] font-mono">
                          {s}
                        </span>
                      ))}
                    </div>
                  </div>

                  <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/30 space-y-2">
                    <span className="font-mono font-bold text-rose-400 uppercase text-[11px] block flex items-center gap-1.5">
                      <XCircle className="w-4 h-4 text-rose-400" /> Missing Target Skills ({missingSkills.length})
                    </span>
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {missingSkills.map((s, idx) => (
                        <span key={idx} className="px-2.5 py-1 rounded-lg bg-rose-500/20 text-rose-300 border border-rose-500/30 text-[11px] font-mono">
                          {s}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Keyword Optimization Suggestions */}
                <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 space-y-2">
                  <span className="font-mono font-bold text-amber-300 uppercase text-[11px] block">
                    Recommended Keywords to Inject for ATS Pass
                  </span>
                  <ul className="space-y-1 text-slate-300 list-disc list-inside">
                    {keywordSuggestions.map((kw, idx) => (
                      <li key={idx} className="font-sans">{kw}</li>
                    ))}
                  </ul>
                </div>

                {/* Explainability Citation */}
                <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-1 text-slate-400 text-[11px]">
                  <div className="font-mono font-bold text-slate-200 uppercase flex items-center gap-1.5">
                    <Info className="w-3.5 h-3.5 text-indigo-400" /> How this match score was derived
                  </div>
                  <div>{matchResult.atsScoreDetails?.explanation || 'Calculated by comparing extracted technical skills, project tech stacks, and section depth against words in the job description.'}</div>
                </div>

              </motion.div>
            )}
          </GlassCard>
        </div>

      </div>
    </div>
  );
};
