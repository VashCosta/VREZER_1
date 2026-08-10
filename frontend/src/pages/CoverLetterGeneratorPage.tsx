import React, { useState, useEffect } from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { Briefcase, Sparkles, Copy, Check, ArrowRight, Loader2, Download } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export const CoverLetterGeneratorPage: React.FC = () => {
  const { aiAnalysisResult, apiKey } = useResumeContext();
  const navigate = useNavigate();

  const [company, setCompany] = useState('Google');
  const [role, setRole] = useState('Software Engineer');
  const [tone, setTone] = useState('Executive & Impact-Focused');
  const [letter, setLetter] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (aiAnalysisResult && !aiAnalysisResult.error && aiAnalysisResult.status !== 'FAILED') {
      const res = aiAnalysisResult;
      const recommendedComp = res.recommendedCompanies?.[0]?.name || 'Google';
      setCompany(recommendedComp);
      setRole(res.role || 'Software Engineer');
      
      const candidateName = res.name || 'Candidate Professional';
      const skillsStr = (res.topSkills || []).slice(0, 4).join(', ');
      const projectTitle = res.projects?.[0] || 'AI Platform Development';

      const generated = `Dear Hiring Team at ${recommendedComp},\n\nI am writing to express my strong enthusiasm for the ${res.role || 'Software Engineer'} position at ${recommendedComp}. With a robust background in ${res.careerDomain || 'Software Systems'} and technical proficiency in ${skillsStr}, I am eager to contribute to your engineering excellence.\n\nIn my recent work, I spearheaded key development initiatives, including my work on "${projectTitle}". I focus on writing modular, optimized systems and aligning architectural decisions with business goals. My summary profile aligns with your target standards, and I am keen to deploy these capabilities at your team.\n\nThank you for your time and consideration. I look forward to discussing how my background fits your team's goals.\n\nSincerely,\n${candidateName}`;
      
      setLetter(generated);
    }
  }, [aiAnalysisResult]);

  if (!aiAnalysisResult || aiAnalysisResult.error || aiAnalysisResult.status === 'FAILED') {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] text-center p-6 space-y-4">
        <div className="inline-flex p-4 rounded-full bg-red-500/10 text-red-400 border border-red-500/20 mb-2">
          <Briefcase className="w-12 h-12 animate-pulse" />
        </div>
        <h2 className="text-xl font-extrabold text-white">No Active Resume Analysis Found</h2>
        <p className="text-xs text-slate-400 max-w-md leading-relaxed">
          Please upload and parse your resume first in the Resume Parser module. 
          The AI will then generate customized cover letters matching your skills and projects.
        </p>
        <Button onClick={() => navigate('/upload')} icon={<ArrowRight className="w-4 h-4" />}>
          Go to Resume Parser
        </Button>
      </div>
    );
  }

  const handleCompose = async () => {
    setIsGenerating(true);
    const res = aiAnalysisResult;
    const candidateName = res.name || 'Candidate Professional';
    const skillsStr = (res.topSkills || []).slice(0, 5).join(', ');
    const projectTitle = res.projects?.[0] || 'Technical Platform Project';

    try {
      const activeKey = apiKey || localStorage.getItem('vrezerApiKey') || '';
      const prompt = `Generate a compelling, professional cover letter tailored for:
Candidate Name: ${candidateName}
Target Company: ${company}
Target Role: ${role}
Candidate Skills: ${skillsStr}
Notable Project: ${projectTitle}
Desired Tone: ${tone}

Make it authentic, persuasive, well-structured (3-4 concise paragraphs), highlighting quantified achievements and passion for the target role.`;

      const response = await axios.post('/api/chat/ask', {
        prompt,
        apiKey: activeKey,
        candidateContext: { name: candidateName, role: res.role, domain: res.careerDomain, skills: res.topSkills }
      });

      const reply = response.data?.data?.reply;
      if (reply && reply.trim().length > 50) {
        setLetter(reply.trim());
      } else {
        throw new Error("Fallback to template");
      }
    } catch (e) {
      const generated = `Dear Hiring Team at ${company},\n\nI am writing to express my strong enthusiasm for the ${role} position at ${company}. With a robust background in ${res.careerDomain || 'Software Systems'} and technical proficiency in ${skillsStr}, I am eager to contribute to your engineering excellence.\n\nIn my recent work, I spearheaded key development initiatives, including my work on "${projectTitle}". I focus on writing modular, optimized systems and aligning architectural decisions with business goals. My summary profile aligns with your target standards, and I am keen to deploy these capabilities at your team.\n\nThank you for your time and consideration. I look forward to discussing how my background fits your team's goals.\n\nSincerely,\n${candidateName}`;
      setLetter(generated);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(letter);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadTxt = () => {
    const blob = new Blob([letter], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `Cover_Letter_${company.replace(/\s+/g, '_')}_${role.replace(/\s+/g, '_')}.txt`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
          <Briefcase className="w-6 h-6 text-red-400" /> AI Cover Letter Generator
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Generate tailored professional cover letters customized dynamically for your target company and role.
        </p>
      </div>

      <div className="grid md:grid-cols-3 gap-6">
        <GlassCard className="space-y-4 p-6">
          <h3 className="font-bold text-sm text-white">Target Job Details</h3>
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Company Name</label>
            <input
              type="text"
              value={company}
              onChange={(e) => setCompany(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-red-500"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Target Job Role</label>
            <input
              type="text"
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-red-500"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Writing Tone & Style</label>
            <select
              value={tone}
              onChange={(e) => setTone(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-red-500"
            >
              <option value="Executive & Impact-Focused">👔 Executive & Impact-Focused</option>
              <option value="Technical & Architecture-Oriented">⚙️ Technical & Deep Engineering</option>
              <option value="High-Growth Startup / Fast-Paced">🚀 High-Growth Startup / Visionary</option>
              <option value="Crisp, Concise & Direct">⚡ Crisp, Concise & Direct</option>
            </select>
          </div>
          <Button onClick={handleCompose} disabled={isGenerating} icon={isGenerating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}>
            {isGenerating ? 'Generating Letter...' : 'Generate with AI'}
          </Button>
        </GlassCard>

        <GlassCard className="md:col-span-2 space-y-4 p-6">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-sm text-white">Generated Cover Letter</h3>
            <div className="flex items-center gap-2">
              <Button size="sm" variant="outline" onClick={handleDownloadTxt} icon={<Download className="w-3.5 h-3.5" />}>
                Download .TXT
              </Button>
              <Button size="sm" variant="outline" onClick={handleCopy} icon={copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}>
                {copied ? 'Copied!' : 'Copy Text'}
              </Button>
            </div>
          </div>
          <textarea
            value={letter}
            onChange={(e) => setLetter(e.target.value)}
            rows={14}
            className="w-full bg-slate-900 border border-slate-850 rounded-xl p-4 text-xs text-slate-300 focus:outline-none font-mono leading-relaxed"
          />
        </GlassCard>
      </div>
    </div>
  );
};
