import React from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { Cpu, Sparkles, Database, Layers, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const ProjectRecommenderPage: React.FC = () => {
  const { aiAnalysisResult } = useResumeContext();
  const navigate = useNavigate();

  const staticProjects = [
    {
      title: 'Real-Time AI Talent Intelligence Platform',
      difficulty: 'Advanced',
      time: '3-4 Weeks',
      desc: 'Architect a high-performance web app with automated PDF/DOCX resume parsing, ATS format auditing, and target company skill gap matching.',
      tech: 'Spring Boot 3, React 19, TypeScript, PostgreSQL, Docker, Tailwind CSS',
      db: 'Users, Resumes, ResumeScores, AtsReports, JobMatches, SkillGaps',
      api: 'POST /api/resumes/upload, POST /api/ai/ats-audit, POST /api/ai/match-jd'
    },
    {
      title: 'Distributed Event-Driven Payment Gateway',
      difficulty: 'Hard',
      time: '4 Weeks',
      desc: 'Build an event-driven payment processing engine with idempotency keys, Kafka event streams, and Redis rate limiting.',
      tech: 'Spring Cloud, Apache Kafka, Redis, PostgreSQL Sharding, Docker',
      db: 'Accounts, Transactions, IdempotencyKeys, AuditLogs',
      api: 'POST /api/v1/payments/charge, GET /api/v1/transactions/{id}'
    }
  ];

  const hasAnalysis = aiAnalysisResult && !aiAnalysisResult.error && aiAnalysisResult.status !== 'FAILED';
  
  // Extract dynamic projects from timeline if available
  const dynamicProjects: string[] = [];
  if (hasAnalysis && aiAnalysisResult.careerGrowthTimeline) {
    aiAnalysisResult.careerGrowthTimeline.forEach((t: any) => {
      if (t.recommendedProjects) {
        t.recommendedProjects.forEach((p: string) => {
          if (!dynamicProjects.includes(p)) {
            dynamicProjects.push(p);
          }
        });
      }
    });
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
          <Cpu className="w-6 h-6 text-red-400" /> AI Resume-Boosting Project Blueprints
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Tailored production project blueprints complete with database schemas, modules, and API endpoint structures.
        </p>
      </div>

      {hasAnalysis && dynamicProjects.length > 0 && (
        <GlassCard className="p-6 space-y-3 border-red-500/20">
          <h3 className="font-bold text-sm text-white flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-purple-400" /> Recommended Projects for {aiAnalysisResult.careerDomain}
          </h3>
          <p className="text-xs text-slate-400">
            Building these projects will directly boost your matching weight for your target job roles:
          </p>
          <div className="grid md:grid-cols-2 gap-4">
            {dynamicProjects.map((p, idx) => (
              <div key={idx} className="p-4 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-200 flex items-start gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-red-500 mt-1.5 shrink-0" />
                <div>
                  <h4 className="font-bold text-white text-xs">{p}</h4>
                  <p className="text-[10px] text-slate-400 mt-1">Target Role Alignment: {aiAnalysisResult.role}</p>
                </div>
              </div>
            ))}
          </div>
        </GlassCard>
      )}

      {!hasAnalysis && (
        <GlassCard className="p-6 text-center space-y-3">
          <h3 className="font-bold text-xs text-slate-400 uppercase tracking-widest">Personalized Recommendations Inactive</h3>
          <p className="text-xs text-slate-500 max-w-md mx-auto leading-relaxed">
            Upload your resume in the Resume Parser to generate projects custom-tailored to your career domain.
          </p>
          <Button onClick={() => navigate('/upload')} size="sm" icon={<ArrowRight className="w-3.5 h-3.5" />}>
            Upload Resume
          </Button>
        </GlassCard>
      )}

      <div className="grid md:grid-cols-2 gap-6">
        {staticProjects.map((p, idx) => (
          <GlassCard key={idx} hoverEffect className="space-y-4 p-6">
            <div className="flex items-center justify-between">
              <span className="px-2.5 py-1 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-xs font-bold">
                {p.difficulty} • {p.time}
              </span>
            </div>

            <h3 className="text-base font-bold text-white">{p.title}</h3>
            <p className="text-xs text-slate-350 leading-relaxed">{p.desc}</p>

            <div className="p-3 rounded-xl bg-slate-900 border border-slate-800 space-y-1.5 text-xs">
              <div className="font-bold text-blue-400 flex items-center gap-1.5">
                <Layers className="w-3.5 h-3.5" /> Recommended Tech Stack
              </div>
              <p className="text-slate-400">{p.tech}</p>
            </div>

            <div className="p-3 rounded-xl bg-slate-900 border border-slate-800 space-y-1.5 text-xs">
              <div className="font-bold text-emerald-400 flex items-center gap-1.5">
                <Database className="w-3.5 h-3.5" /> Database Tables & API Endpoints
              </div>
              <p className="text-slate-400">Schema: {p.db}</p>
              <p className="text-slate-450 font-mono text-[10px]">Endpoints: {p.api}</p>
            </div>
          </GlassCard>
        ))}
      </div>
    </div>
  );
};
