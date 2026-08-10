import React, { useState } from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { Map, Sparkles, CheckCircle2, Clock, Search, ArrowRight, Zap, Trophy, ShieldCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const CareerRoadmapPage: React.FC = () => {
  const { aiAnalysisResult } = useResumeContext();
  const navigate = useNavigate();

  const [dreamJob, setDreamJob] = useState<string>('Senior Full Stack Architect');
  const [isGenerating, setIsGenerating] = useState<boolean>(false);

  const careerDomain = aiAnalysisResult?.careerDomain || 'Software Systems & Distributed Architecture';
  const role = aiAnalysisResult?.role || dreamJob;

  const defaultRoadmap = [
    {
      stage: 'Phase 1 — Foundations & Core Stack',
      learningTime: 'Month 1 - 2',
      topic: 'Java 21, Spring Boot Microservices & Data Structures',
      priority: 'CRITICAL',
      expectedCareerImpact: 'Build robust object-oriented API foundation and backend database mastery.',
      recommendedCertifications: ['Oracle Certified Professional Java SE 17'],
      freeResources: ['Java Brains YouTube', 'Spring Academy'],
      paidResources: ['Udemy Masterclass']
    },
    {
      stage: 'Phase 2 — Distributed Architecture',
      learningTime: 'Month 3 - 4',
      topic: 'Apache Kafka Event Streaming, Redis Cluster Caching & PostgreSQL Tuning',
      priority: 'HIGH',
      expectedCareerImpact: 'Prepares for high-concurrency systems design interviews at product companies.',
      recommendedCertifications: ['Confluent Certified Developer for Apache Kafka'],
      freeResources: ['Confluent Developer Docs'],
      paidResources: ['Educative System Design Grokking']
    },
    {
      stage: 'Phase 3 — Cloud Native & DevOps',
      learningTime: 'Month 5 - 6',
      topic: 'AWS Cloud (EC2, S3, RDS, ECS), Docker Containers & Kubernetes Orchestration',
      priority: 'HIGH',
      expectedCareerImpact: 'Enables end-to-end containerized cloud deployment and MLOps management.',
      recommendedCertifications: ['AWS Certified Solutions Architect Associate'],
      freeResources: ['AWS Skill Builder', 'KubeAcademy'],
      paidResources: ['A Cloud Guru']
    },
    {
      stage: 'Phase 4 — System Design & Leadership',
      learningTime: 'Month 7+',
      topic: 'High-Level System Design, Fault-Tolerant Consensus, & Tech Team Leadership',
      priority: 'ADVANCED',
      expectedCareerImpact: 'Qualifies for Senior (SDE-2 / Staff) roles and technical architecture leadership.',
      recommendedCertifications: ['AWS Certified DevOps Engineer Professional'],
      freeResources: ['System Design Primer GitHub'],
      paidResources: ['ByteByteGo System Design']
    }
  ];

  const roadmap = aiAnalysisResult?.skillIntelligence?.aiLearningRoadmap || defaultRoadmap;

  const defaultTimeline = [
    {
      stage: 'Entry / Junior (Years 0-2)',
      title: 'Associate Software Engineer',
      expectedSalaryProgression: 'Market Rate',
      roadmapNotes: 'Focus on writing clean, unit-tested code, understanding CI/CD, and delivering feature tickets.',
      recommendedProjects: ['REST API Service', 'CRUD Full Stack Portal']
    },
    {
      stage: 'Mid-Level (Years 2-5)',
      title: 'Senior Software Engineer (SDE-2)',
      expectedSalaryProgression: 'Market Rate',
      roadmapNotes: 'Own complete microservices, optimize database queries, mentor juniors, and conduct code reviews.',
      recommendedProjects: ['Distributed Event Cache', 'Microservice Auth Pipeline']
    },
    {
      stage: 'Senior / Lead (Years 5+)',
      title: 'Staff Engineer / Technical Architect',
      expectedSalaryProgression: 'Market Rate',
      roadmapNotes: 'Define system architecture across teams, select cloud infrastructure, and set engineering standards.',
      recommendedProjects: ['Multi-Region RAG Engine', 'High-Throughput Gateway']
    }
  ];

  const timeline = aiAnalysisResult?.careerGrowthTimeline || defaultTimeline;

  const handleGenerateCustomRoadmap = (e: React.FormEvent) => {
    e.preventDefault();
    setIsGenerating(true);
    setTimeout(() => {
      setIsGenerating(false);
    }, 800);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
          <Map className="w-6 h-6 text-red-400" /> Interactive Career Roadmap
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          AI-engineered multi-phase learning milestones and long-term career progression tracks tailored to your dream role.
        </p>
      </div>

      {/* Dream Job Search Input */}
      <GlassCard className="p-5 border border-slate-800">
        <form onSubmit={handleGenerateCustomRoadmap} className="flex flex-col sm:flex-row gap-3 items-end">
          <div className="flex-1">
            <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1">
              Target Career Role / Dream Job Title
            </label>
            <input
              type="text"
              value={dreamJob}
              onChange={(e) => setDreamJob(e.target.value)}
              placeholder="e.g. Senior Backend Engineer, AI Specialist, Product Manager"
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-white focus:outline-none focus:border-red-500"
            />
          </div>
          <Button type="submit" className="py-2 shrink-0" icon={<Sparkles className="w-4 h-4" />}>
            {isGenerating ? 'Generating Track...' : 'Generate Custom Roadmap'}
          </Button>
        </form>
      </GlassCard>

      {/* Target Domain Badge */}
      <GlassCard className="p-6 border border-slate-800">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block">Primary Target Domain</span>
            <h2 className="text-lg font-black text-white mt-0.5">{careerDomain}</h2>
            <p className="text-xs text-slate-400">Target Role: <span className="text-red-400 font-bold">{role}</span></p>
          </div>
          <span className="px-3.5 py-2 rounded-xl bg-red-500/10 border border-red-500/30 text-xs font-bold text-red-400 flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4" /> VREZER AI Verified Roadmap
          </span>
        </div>
      </GlassCard>

      {/* Learning Roadmap Milestones */}
      <div className="space-y-4">
        <h3 className="font-extrabold text-sm text-white px-1 flex items-center gap-2">
          <Trophy className="w-4 h-4 text-red-400" /> Phased Learning & Upskilling Milestones
        </h3>

        <div className="grid gap-4">
          {roadmap.map((m: any, idx: number) => (
            <GlassCard key={idx} hoverEffect className="p-5 flex flex-col md:flex-row items-start justify-between gap-4 border border-slate-800">
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 rounded-2xl bg-red-500/10 border border-red-500/30 flex items-center justify-center text-red-400 font-extrabold text-xs shrink-0 mt-0.5">
                  P{idx + 1}
                </div>
                <div className="space-y-1.5">
                  <div className="flex items-center gap-2">
                    <h4 className="font-bold text-white text-xs">{m.topic}</h4>
                    <span className="px-2 py-0.5 rounded bg-purple-950/50 text-purple-300 border border-purple-800/40 text-[9px] font-extrabold">
                      {m.priority || 'HIGH'}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 leading-relaxed">{m.expectedCareerImpact}</p>
                  
                  {m.recommendedCertifications?.length > 0 && (
                    <div className="text-[11px] text-slate-300 pt-1">
                      <span className="font-bold text-red-400">Target Certification:</span> {m.recommendedCertifications.join(', ')}
                    </div>
                  )}

                  {(m.freeResources?.length > 0 || m.paidResources?.length > 0) && (
                    <div className="flex flex-wrap gap-x-4 gap-y-1 pt-1.5 text-[10px] text-slate-400">
                      {m.freeResources?.length > 0 && (
                        <div>
                          <span className="font-bold text-slate-300">Free:</span> {m.freeResources.join(' | ')}
                        </div>
                      )}
                      {m.paidResources?.length > 0 && (
                        <div>
                          <span className="font-bold text-slate-300">Paid:</span> {m.paidResources.join(' | ')}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>

              <div className="flex flex-col items-end gap-1 shrink-0 self-end md:self-center text-right">
                <span className="text-[10px] text-slate-400 flex items-center gap-1 font-semibold">
                  <Clock className="w-3.5 h-3.5 text-red-400" /> {m.learningTime || '2 Months'}
                </span>
                <span className="px-2.5 py-1 rounded-full bg-slate-900 border border-slate-800 text-[10px] text-slate-300 mt-1 font-bold">
                  Phase {idx + 1}
                </span>
              </div>
            </GlassCard>
          ))}
        </div>
      </div>

      {/* Long-Term Career Timeline */}
      <div className="space-y-4 pt-2">
        <h3 className="font-extrabold text-sm text-white px-1 flex items-center gap-2">
          <Zap className="w-4 h-4 text-amber-400" /> Long-Term Career Progression & Compensation Ladder
        </h3>
        
        <div className="grid md:grid-cols-3 gap-6">
          {timeline.map((stage: any, idx: number) => (
            <GlassCard key={idx} className="p-5 flex flex-col justify-between space-y-3 border border-slate-800">
              <div>
                <div className="flex justify-between items-center border-b border-slate-800/80 pb-2 mb-2">
                  <span className="text-[10px] font-bold text-slate-400 tracking-wider uppercase">{stage.stage}</span>
                  <span className="text-[10px] font-extrabold text-red-400">{stage.expectedSalaryProgression}</span>
                </div>
                <h4 className="font-bold text-white text-xs">{stage.title}</h4>
                <p className="text-[11px] text-slate-400 mt-1 leading-relaxed">{stage.roadmapNotes}</p>
              </div>
              {stage.recommendedProjects?.length > 0 && (
                <div className="border-t border-slate-800/80 pt-2 space-y-1">
                  <span className="text-[9px] font-extrabold text-slate-400 uppercase tracking-wider block">Benchmark Projects</span>
                  <div className="flex flex-wrap gap-1">
                    {stage.recommendedProjects.map((p: string, pIdx: number) => (
                      <span key={pIdx} className="px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-[9px] text-slate-300 font-semibold">
                        {p}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </GlassCard>
          ))}
        </div>
      </div>

    </div>
  );
};
