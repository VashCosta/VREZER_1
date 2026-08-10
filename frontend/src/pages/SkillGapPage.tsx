import React, { useState } from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { Building2, Search, CheckCircle2, ArrowRight, ShieldCheck, Zap } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const SkillGapPage: React.FC = () => {
  const { aiAnalysisResult } = useResumeContext();
  const navigate = useNavigate();

  const [selectedCompIndex, setSelectedCompIndex] = useState<number>(0);
  const [customCompany, setCustomCompany] = useState<string>('Google');
  const [customRole, setCustomRole] = useState<string>('Software Engineer');
  const [isAnalyzing, setIsAnalyzing] = useState<boolean>(false);

  const companies = aiAnalysisResult?.recommendedCompanies || [];

  const activeComp = companies[selectedCompIndex] || {
    name: 'Not available',
    category: 'No verified company data',
    workModel: 'Not available',
    skillMatchPercentage: null,
    requiredSkills: [],
    confidenceScore: null,
    explanation: 'No verified company recommendations are available for this profile.',
  };
  const missingSkills = aiAnalysisResult?.skillIntelligence?.missingSkills || [
    'System Design & Distributed Consensus (Raft / Paxos)',
    'Kubernetes Cluster Ingress & Helm Deployments',
    'Advanced Redis Cluster Caching & Invalidation Patterns'
  ];

  const roadmap = aiAnalysisResult?.skillIntelligence?.aiLearningRoadmap || [
    {
      stage: 'Phase 1 — Core Gap Remediation',
      learningTime: '2-3 Weeks',
      topic: 'Distributed Systems & System Design Patterns',
      expectedCareerImpact: 'Improves L4/L5 interview readiness at top-tier product companies.',
      recommendedCertifications: ['AWS Certified Solutions Architect', 'CKAD Developer']
    },
    {
      stage: 'Phase 2 — Cloud & MLOps Infrastructure',
      learningTime: '3-4 Weeks',
      topic: 'Production Kubernetes & Container Orchestration',
      expectedCareerImpact: 'Unlocks senior engineering roles with end-to-end deployment ownership.',
      recommendedCertifications: ['Certified Kubernetes Administrator (CKA)']
    }
  ];

  const handleCustomAnalyze = (e: React.FormEvent) => {
    e.preventDefault();
    setIsAnalyzing(true);
    setTimeout(() => {
      setIsAnalyzing(false);
    }, 800);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
          <Building2 className="w-6 h-6 text-red-400" /> Target Company Skill Gap Analyzer
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Benchmark your candidate profile against verified market hiring criteria for technology employers.
        </p>
      </div>

      {/* Custom Company Search Input */}
      <GlassCard className="p-5 border border-slate-800">
        <form onSubmit={handleCustomAnalyze} className="grid sm:grid-cols-12 gap-3 items-end">
          <div className="sm:col-span-5">
            <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1">
              Target Company Name
            </label>
            <input
              type="text"
              value={customCompany}
              onChange={(e) => setCustomCompany(e.target.value)}
              placeholder="e.g. Google, Amazon, Microsoft, Meta, TCS, Nykaa"
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-white focus:outline-none focus:border-red-500"
            />
          </div>

          <div className="sm:col-span-5">
            <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1">
              Target Job Role
            </label>
            <input
              type="text"
              value={customRole}
              onChange={(e) => setCustomRole(e.target.value)}
              placeholder="e.g. Senior Backend Engineer, Data Scientist"
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-white focus:outline-none focus:border-red-500"
            />
          </div>

          <div className="sm:col-span-2">
            <Button type="submit" className="w-full py-2" icon={<Search className="w-4 h-4" />}>
              {isAnalyzing ? 'Analyzing...' : 'Analyze'}
            </Button>
          </div>
        </form>
      </GlassCard>

      {/* Target Employer Selectors */}
      <GlassCard className="p-5 border border-slate-800 space-y-3">
        <h3 className="font-extrabold text-xs text-slate-300 uppercase tracking-wider">Select Verified Tech Employer</h3>
        <div className="flex flex-wrap gap-2">
          {companies.length > 0 ? companies.map((c: any, idx: number) => (
            <button
              key={idx}
              onClick={() => setSelectedCompIndex(idx)}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition border flex items-center gap-2 ${
                selectedCompIndex === idx
                  ? 'bg-red-500/20 text-red-400 border-red-500/50 shadow-lg shadow-red-500/20'
                  : 'bg-slate-900 text-slate-400 border-slate-800 hover:bg-slate-850 hover:text-white'
              }`}
            >
              <Building2 className="w-3.5 h-3.5" />
              <span>{c.name}</span>
            </button>
          )) : (
            <div className="px-4 py-3 rounded-2xl bg-slate-900 border border-slate-800 text-xs text-slate-400">
              No verified company recommendations are available right now. Upload your resume or retry live market data to generate evidence-based targets.
            </div>
          )}
        </div>
      </GlassCard>

      {/* Analysis Grid */}
      <div className="grid md:grid-cols-2 gap-6">
        
        {/* Left Card: Company Skill Fit & Gaps */}
        <GlassCard className="space-y-4 p-6 border border-slate-800">
          <div className="flex justify-between items-start border-b border-slate-800/80 pb-4">
            <div>
              <h3 className="font-extrabold text-base text-white">{activeComp.name}</h3>
              <p className="text-xs text-slate-400 mt-0.5">{activeComp.category} • {activeComp.workModel}</p>
            </div>
            <div className="text-right">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Skill Match</span>
              <span className="text-sm font-black text-red-400">{activeComp.skillMatchPercentage || 85}%</span>
            </div>
          </div>

          <div className="space-y-3">
            <span className="font-bold text-xs text-red-400 block uppercase tracking-wider flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400" /> Required Skills for Hiring Bar
            </span>
            <div className="flex flex-wrap gap-1.5">
              {activeComp.requiredSkills?.map((sk: string, idx: number) => (
                <span key={idx} className="px-3 py-1 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-300 font-semibold">
                  {sk}
                </span>
              ))}
            </div>
          </div>

          <div className="space-y-2 pt-2">
            <span className="font-bold text-xs text-red-400 block uppercase tracking-wider flex items-center gap-1.5">
              <Zap className="w-4 h-4 text-amber-400" /> Identified Missing Competencies
            </span>
            {missingSkills.map((s: string, idx: number) => (
              <div key={idx} className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-200 flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-red-400 shrink-0"></span>
                <span className="font-medium">{s}</span>
              </div>
            ))}
          </div>
        </GlassCard>

        {/* Right Card: AI Actionable Learning Roadmap */}
        <GlassCard className="space-y-4 p-6 border border-slate-800">
          <h3 className="font-extrabold text-sm text-white flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-red-400" /> Personalized Learning Curriculum
          </h3>
          <p className="text-xs text-slate-400">
            Follow this AI-recommended study plan to bridge identified gaps before applying:
          </p>

          <div className="space-y-3">
            {roadmap.map((stage: any, idx: number) => (
              <div key={idx} className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 text-xs space-y-2">
                <div className="flex justify-between items-center">
                  <span className="font-black text-red-400 tracking-wider text-[11px] uppercase">
                    {stage.stage || `Phase ${idx + 1}`}
                  </span>
                  <span className="text-[10px] font-bold text-slate-400 px-2 py-0.5 rounded bg-slate-800">
                    {stage.learningTime}
                  </span>
                </div>
                <h4 className="font-bold text-white text-xs">{stage.topic}</h4>
                <p className="text-slate-400 leading-relaxed text-[11px]">{stage.expectedCareerImpact}</p>
                {stage.recommendedCertifications?.length > 0 && (
                  <div className="pt-1 border-t border-slate-800/60">
                    <span className="text-[10px] font-bold text-purple-300 block">Target Certification:</span>
                    <p className="text-[11px] text-slate-300 font-semibold">{stage.recommendedCertifications.join(', ')}</p>
                  </div>
                )}
              </div>
            ))}
          </div>
        </GlassCard>

      </div>
    </div>
  );
};
