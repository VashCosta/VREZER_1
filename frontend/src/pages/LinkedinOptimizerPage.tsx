import React from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { Linkedin, Sparkles, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const LinkedinOptimizerPage: React.FC = () => {
  const { aiAnalysisResult } = useResumeContext();
  const navigate = useNavigate();

  if (!aiAnalysisResult || aiAnalysisResult.error || aiAnalysisResult.status === 'FAILED') {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] text-center p-6 space-y-4">
        <div className="inline-flex p-4 rounded-full bg-red-500/10 text-red-400 border border-red-500/20 mb-2">
          <Linkedin className="w-12 h-12 animate-pulse" />
        </div>
        <h2 className="text-xl font-extrabold text-white">No Active Resume Analysis Found</h2>
        <p className="text-xs text-slate-400 max-w-md leading-relaxed">
          Please upload and parse your resume first in the Resume Parser module. 
          The AI will then generate optimized headlines and About sections customized to your profile.
        </p>
        <Button onClick={() => navigate('/upload')} icon={<ArrowRight className="w-4 h-4" />}>
          Go to Resume Parser
        </Button>
      </div>
    );
  }

  const res = aiAnalysisResult;
  const role = res.role || 'Professional Developer';
  const domain = res.careerDomain || 'Software Systems';
  const skills = res.topSkills || [];
  const projects = res.projects || [];

  const headline = `${role} | Specializing in ${domain} | Expert in ${skills.slice(0, 5).join(' • ')}`;
  
  const about = `Passionate ${role} specializing in ${domain}. Demonstrated technical proficiency in ${skills.join(', ')}.

🚀 Proven project execution experience:
${projects.map((p: string) => `• ${p}`).join('\n')}

Always eager to connect and collaborate on scaling technologies and resolving complex engineering bottlenecks!`;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
          <Linkedin className="w-6 h-6 text-red-400" /> LinkedIn Profile Optimizer
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Generate optimized profile headlines, high-converting About sections, and skill tags.
        </p>
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        <GlassCard className="space-y-4 p-6">
          <h3 className="font-bold text-sm text-white">Recommended Profile Headline</h3>
          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 text-xs text-red-400 font-semibold leading-relaxed">
            {headline}
          </div>
          <p className="text-[10px] text-slate-500 italic">
            Tip: Keep your headline keyword-rich to maximize your visibility in recruiter search queries.
          </p>
        </GlassCard>

        <GlassCard className="space-y-4 p-6">
          <h3 className="font-bold text-sm text-white">Recommended About Section</h3>
          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-350 leading-relaxed whitespace-pre-line font-mono">
            {about}
          </div>
        </GlassCard>
      </div>

      {/* Suggested Skills to tag */}
      <GlassCard className="p-6 space-y-3">
        <h3 className="font-bold text-sm text-white">Suggested Skills to Add on LinkedIn</h3>
        <div className="flex flex-wrap gap-2">
          {skills.map((s: string, idx: number) => (
            <span key={idx} className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-300 font-semibold">
              {s}
            </span>
          ))}
        </div>
      </GlassCard>
    </div>
  );
};
