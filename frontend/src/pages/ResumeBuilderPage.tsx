import React, { useState, useEffect } from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { FileCheck, Download, Sparkles, Plus, Trash2, Layout, Palette, RefreshCw } from 'lucide-react';

export const ResumeBuilderPage: React.FC = () => {
  const { aiAnalysisResult } = useResumeContext();

  const [activeTemplate, setActiveTemplate] = useState<'modern' | 'executive' | 'minimal' | 'corporate' | 'creative'>('modern');
  const [themeColor, setThemeColor] = useState('#C1121F');

  const [name, setName] = useState('Aarav Sharma');
  const [role, setRole] = useState('Senior Software Engineer (SDE-2)');
  const [email, setEmail] = useState('aarav.sharma@tech.io');
  const [phone, setPhone] = useState('+91 98765 43210');
  const [location, setLocation] = useState('Bengaluru, India');
  const [linkedin, setLinkedin] = useState('linkedin.com/in/aaravsharma');
  const [github, setGithub] = useState('github.com/aaravsharma');
  const [summary, setSummary] = useState('Results-driven Senior Software Engineer with 5+ years of experience building high-concurrency microservices, distributed cloud architectures, and real-time data pipelines with Java, Spring Boot, React, and PostgreSQL.');
  const [skills, setSkills] = useState('Java 21, Spring Boot 3, React 19, TypeScript, PostgreSQL, Redis, Docker, Kubernetes, AWS, Microservices');

  const [experiences, setExperiences] = useState([
    {
      role: 'Senior Backend Engineer',
      company: 'TechCorp Solutions',
      period: 'Jan 2022 - Present',
      details: 'Architected microservices handling 15,000 requests/sec. Reduced API latency by 45% using Redis caching and PostgreSQL query tuning.'
    },
    {
      role: 'Software Development Engineer',
      company: 'CloudScale Systems',
      period: 'Jul 2019 - Dec 2021',
      details: 'Built REST APIs and event-driven data pipelines using Spring Boot and Apache Kafka.'
    }
  ]);

  const [education, setEducation] = useState([
    {
      degree: 'B.Tech in Computer Science & Engineering',
      institution: 'IIT Delhi',
      year: '2015 - 2019',
      score: 'CGPA: 8.9 / 10'
    }
  ]);

  const [projects, setProjects] = useState([
    {
      title: 'VREZER AI Resume Intelligence System',
      tech: 'Java 21, Spring Boot, React, Groq LLaMA, RAG',
      desc: 'Built an AI-powered resume analyzer providing ATS scoring, live job matching, and company skill gap analysis.'
    }
  ]);

  // Sync with uploaded resume context if available
  useEffect(() => {
    if (aiAnalysisResult && !aiAnalysisResult.error && aiAnalysisResult.status !== 'FAILED') {
      const res = aiAnalysisResult;
      if (res.name) setName(res.name);
      if (res.role) setRole(res.role);
      if (res.email) setEmail(res.email);
      if (res.phone) setPhone(res.phone);
      if (res.topSkills && res.topSkills.length > 0) setSkills(res.topSkills.join(', '));
      if (res.professionalSummary) setSummary(res.professionalSummary);

      if (res.experienceList && res.experienceList.length > 0) {
        setExperiences(res.experienceList.map((e: any) => ({
          role: e.role || 'Software Engineer',
          company: e.company || 'Tech Company',
          period: e.duration || e.dates || '2021 - Present',
          details: e.description || 'Developed high-performance software systems.'
        })));
      }

      if (res.education && Array.isArray(res.education) && res.education.length > 0) {
        setEducation(res.education.map((e: any) => ({
          degree: e.degree || 'Degree',
          institution: e.institution || 'University',
          year: e.years || '2019',
          score: e.cgpa ? `CGPA: ${e.cgpa}` : ''
        })));
      }

      if (res.projects && Array.isArray(res.projects) && res.projects.length > 0) {
        setProjects(res.projects.map((p: any) => ({
          title: p.title || 'Project',
          tech: p.tech || '',
          desc: p.description || ''
        })));
      }
    }
  }, [aiAnalysisResult]);

  const handleExportPdf = () => {
    window.print();
  };

  const addExperience = () => {
    setExperiences([...experiences, { role: '', company: '', period: '', details: '' }]);
  };

  const removeExperience = (index: number) => {
    setExperiences(experiences.filter((_, i) => i !== index));
  };

  const addProject = () => {
    setProjects([...projects, { title: '', tech: '', desc: '' }]);
  };

  const removeProject = (index: number) => {
    setProjects(projects.filter((_, i) => i !== index));
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
            <FileCheck className="w-6 h-6 text-red-400" /> Multi-Template Live Resume Builder
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Edit, choose from 5 professional ATS templates, customize theme accents, and export clean PDF resumes.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button onClick={handleExportPdf} icon={<Download className="w-4 h-4" />}>
            Export PDF Resume
          </Button>
        </div>
      </div>

      {/* Control Bar: Templates & Colors */}
      <GlassCard className="p-4 flex flex-wrap items-center justify-between gap-4 border border-slate-800">
        {/* Template Switcher */}
        <div className="flex items-center gap-2">
          <Layout className="w-4 h-4 text-slate-400" />
          <span className="text-xs font-bold text-slate-300 mr-1">Template:</span>
          {(['modern', 'executive', 'minimal', 'corporate', 'creative'] as const).map((tmpl) => (
            <button
              key={tmpl}
              onClick={() => setActiveTemplate(tmpl)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold capitalize transition border ${
                activeTemplate === tmpl
                  ? 'bg-red-500/20 text-red-400 border-red-500/50 shadow-md shadow-red-500/10'
                  : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-white'
              }`}
            >
              {tmpl}
            </button>
          ))}
        </div>

        {/* Color Palette Switcher */}
        <div className="flex items-center gap-2">
          <Palette className="w-4 h-4 text-slate-400" />
          <span className="text-xs font-bold text-slate-300 mr-1">Accent:</span>
          {[
            { name: 'VREZER Red', hex: '#C1121F' },
            { name: 'VREZER Gold', hex: '#D4AF37' },
            { name: 'Electric Indigo', hex: '#6366f1' },
            { name: 'Emerald', hex: '#10b981' },
            { name: 'Dark Slate', hex: '#334155' }
          ].map((color) => (
            <button
              key={color.hex}
              onClick={() => setThemeColor(color.hex)}
              title={color.name}
              className={`w-6 h-6 rounded-full border-2 transition ${
                themeColor === color.hex ? 'border-white scale-110 shadow-lg' : 'border-transparent hover:scale-105'
              }`}
              style={{ backgroundColor: color.hex }}
            />
          ))}
        </div>
      </GlassCard>

      {/* Editor Grid */}
      <div className="grid lg:grid-cols-12 gap-6 items-start">
        
        {/* Left Column: Form Editor (5 cols) */}
        <div className="lg:col-span-5 space-y-4 print:hidden">
          
          <GlassCard className="p-5 space-y-3 border border-slate-800">
            <h3 className="font-extrabold text-xs text-red-400 uppercase tracking-wider">Candidate Identity</h3>
            
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <label className="block text-[11px] text-slate-400 mb-1 font-semibold">Full Name</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-white text-xs focus:border-red-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-[11px] text-slate-400 mb-1 font-semibold">Target Job Role</label>
                <input
                  type="text"
                  value={role}
                  onChange={(e) => setRole(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-white text-xs focus:border-red-500 focus:outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <label className="block text-[11px] text-slate-400 mb-1 font-semibold">Email Address</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-white text-xs focus:border-red-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-[11px] text-slate-400 mb-1 font-semibold">Phone Number</label>
                <input
                  type="text"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-white text-xs focus:border-red-500 focus:outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] text-slate-400 mb-1 font-semibold">Location</label>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-white text-xs focus:border-red-500 focus:outline-none"
              />
            </div>
          </GlassCard>

          <GlassCard className="p-5 space-y-3 border border-slate-800">
            <h3 className="font-extrabold text-xs text-red-400 uppercase tracking-wider">Summary & Skills</h3>
            <div>
              <label className="block text-[11px] text-slate-400 mb-1 font-semibold">Professional Summary</label>
              <textarea
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                rows={3}
                className="w-full bg-slate-900 border border-slate-800 rounded-lg p-2.5 text-xs text-white focus:border-red-500 focus:outline-none resize-none"
              />
            </div>
            <div>
              <label className="block text-[11px] text-slate-400 mb-1 font-semibold">Skills (Comma Separated)</label>
              <input
                type="text"
                value={skills}
                onChange={(e) => setSkills(e.target.value)}
                className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-white text-xs focus:border-red-500 focus:outline-none"
              />
            </div>
          </GlassCard>

          {/* Work Experience Form */}
          <GlassCard className="p-5 space-y-3 border border-slate-800">
            <div className="flex items-center justify-between">
              <h3 className="font-extrabold text-xs text-red-400 uppercase tracking-wider">Work Experience</h3>
              <button
                onClick={addExperience}
                className="p-1 rounded bg-slate-800 text-slate-300 hover:text-white text-xs flex items-center gap-1 font-semibold"
              >
                <Plus className="w-3.5 h-3.5" /> Add
              </button>
            </div>

            {experiences.map((exp, idx) => (
              <div key={idx} className="p-3 bg-slate-950/60 rounded-xl border border-slate-800 space-y-2 relative group">
                <button
                  onClick={() => removeExperience(idx)}
                  className="absolute top-2 right-2 text-slate-500 hover:text-rose-400 transition"
                  title="Remove"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="text"
                    placeholder="Role Title"
                    value={exp.role}
                    onChange={(e) => {
                      const copy = [...experiences];
                      copy[idx].role = e.target.value;
                      setExperiences(copy);
                    }}
                    className="bg-slate-900 border border-slate-800 rounded px-2 py-1 text-xs text-white"
                  />
                  <input
                    type="text"
                    placeholder="Company Name"
                    value={exp.company}
                    onChange={(e) => {
                      const copy = [...experiences];
                      copy[idx].company = e.target.value;
                      setExperiences(copy);
                    }}
                    className="bg-slate-900 border border-slate-800 rounded px-2 py-1 text-xs text-white"
                  />
                </div>
                <input
                  type="text"
                  placeholder="Employment Dates (e.g. Jan 2022 - Present)"
                  value={exp.period}
                  onChange={(e) => {
                    const copy = [...experiences];
                    copy[idx].period = e.target.value;
                    setExperiences(copy);
                  }}
                  className="w-full bg-slate-900 border border-slate-800 rounded px-2 py-1 text-xs text-white"
                />
                <textarea
                  placeholder="Key Responsibilities & Impact"
                  value={exp.details}
                  onChange={(e) => {
                    const copy = [...experiences];
                    copy[idx].details = e.target.value;
                    setExperiences(copy);
                  }}
                  rows={2}
                  className="w-full bg-slate-900 border border-slate-800 rounded p-2 text-xs text-white resize-none"
                />
              </div>
            ))}
          </GlassCard>

        </div>

        {/* Right Column: Live Printable Resume Preview (7 cols) */}
        <div className="lg:col-span-7">
          <div className="bg-white text-slate-900 p-8 sm:p-10 rounded-2xl shadow-2xl space-y-6 border border-slate-200 min-h-[750px] font-sans">
            
            {/* TEMPLATE 1: MODERN GLASS (VREZER SIGNATURE) */}
            {activeTemplate === 'modern' && (
              <div className="space-y-5">
                <div className="border-b-2 pb-4 flex justify-between items-start" style={{ borderColor: themeColor }}>
                  <div>
                    <h1 className="text-3xl font-black tracking-tight" style={{ color: themeColor }}>{name}</h1>
                    <p className="text-sm font-bold text-slate-700 mt-0.5">{role}</p>
                  </div>
                  <div className="text-right text-[11px] text-slate-600 space-y-0.5 font-medium">
                    <p>{email}</p>
                    <p>{phone}</p>
                    <p>{location}</p>
                  </div>
                </div>

                <div>
                  <h2 className="text-xs font-black uppercase tracking-widest border-b pb-1 mb-2" style={{ color: themeColor, borderColor: `${themeColor}40` }}>
                    Professional Summary
                  </h2>
                  <p className="text-xs text-slate-700 leading-relaxed font-normal">{summary}</p>
                </div>

                <div>
                  <h2 className="text-xs font-black uppercase tracking-widest border-b pb-1 mb-2" style={{ color: themeColor, borderColor: `${themeColor}40` }}>
                    Technical Core Competencies
                  </h2>
                  <div className="flex flex-wrap gap-1.5">
                    {skills.split(',').map((s, idx) => (
                      <span key={idx} className="px-2.5 py-1 rounded bg-slate-100 border border-slate-200 text-slate-800 text-[11px] font-bold">
                        {s.trim()}
                      </span>
                    ))}
                  </div>
                </div>

                <div>
                  <h2 className="text-xs font-black uppercase tracking-widest border-b pb-1 mb-3" style={{ color: themeColor, borderColor: `${themeColor}40` }}>
                    Work Experience
                  </h2>
                  <div className="space-y-3">
                    {experiences.map((exp, idx) => (
                      <div key={idx} className="space-y-1">
                        <div className="flex justify-between items-center text-xs">
                          <span className="font-extrabold text-slate-900">{exp.role} <span className="font-normal text-slate-500">• {exp.company}</span></span>
                          <span className="text-[11px] font-bold text-slate-500">{exp.period}</span>
                        </div>
                        <p className="text-xs text-slate-700 leading-relaxed">{exp.details}</p>
                      </div>
                    ))}
                  </div>
                </div>

                {education.length > 0 && (
                  <div>
                    <h2 className="text-xs font-black uppercase tracking-widest border-b pb-1 mb-2" style={{ color: themeColor, borderColor: `${themeColor}40` }}>
                      Education & Qualifications
                    </h2>
                    {education.map((edu, idx) => (
                      <div key={idx} className="flex justify-between text-xs text-slate-800 font-semibold">
                        <span>{edu.degree} — <span className="text-slate-600 font-normal">{edu.institution}</span></span>
                        <span className="text-slate-500 font-bold">{edu.year} {edu.score && `(${edu.score})`}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TEMPLATE 2: EXECUTIVE SERIF */}
            {activeTemplate === 'executive' && (
              <div className="space-y-5 font-serif">
                <div className="text-center border-b pb-4 border-slate-300">
                  <h1 className="text-3xl font-normal tracking-wide uppercase" style={{ color: themeColor }}>{name}</h1>
                  <p className="text-xs italic text-slate-600 mt-1 font-sans">{role}</p>
                  <p className="text-[11px] font-sans text-slate-500 mt-1">{email} | {phone} | {location}</p>
                </div>

                <div>
                  <h2 className="text-xs font-bold uppercase tracking-widest border-b pb-1 mb-2 font-sans" style={{ color: themeColor }}>
                    Executive Profile
                  </h2>
                  <p className="text-xs text-slate-800 leading-relaxed italic">{summary}</p>
                </div>

                <div>
                  <h2 className="text-xs font-bold uppercase tracking-widest border-b pb-1 mb-2 font-sans" style={{ color: themeColor }}>
                    Core Expertise
                  </h2>
                  <p className="text-xs text-slate-800 font-sans font-medium">{skills}</p>
                </div>

                <div>
                  <h2 className="text-xs font-bold uppercase tracking-widest border-b pb-1 mb-3 font-sans" style={{ color: themeColor }}>
                    Professional History
                  </h2>
                  <div className="space-y-3 font-sans">
                    {experiences.map((exp, idx) => (
                      <div key={idx} className="space-y-1">
                        <div className="flex justify-between items-center text-xs">
                          <span className="font-bold text-slate-900">{exp.role} <span className="italic font-normal text-slate-600">at {exp.company}</span></span>
                          <span className="text-[11px] text-slate-500">{exp.period}</span>
                        </div>
                        <p className="text-xs text-slate-700 leading-relaxed">{exp.details}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* TEMPLATE 3: MINIMALIST TECH */}
            {activeTemplate === 'minimal' && (
              <div className="space-y-5 font-mono">
                <div className="border-l-4 pl-4" style={{ borderColor: themeColor }}>
                  <h1 className="text-2xl font-bold">{name}</h1>
                  <p className="text-xs font-bold text-slate-600 mt-0.5">{role}</p>
                  <p className="text-[10px] text-slate-500 mt-1">// {email} • {phone} • {location}</p>
                </div>

                <div className="font-sans">
                  <h2 className="text-xs font-bold uppercase tracking-wider mb-1" style={{ color: themeColor }}>Summary</h2>
                  <p className="text-xs text-slate-700 leading-relaxed">{summary}</p>
                </div>

                <div className="font-sans">
                  <h2 className="text-xs font-bold uppercase tracking-wider mb-2" style={{ color: themeColor }}>Stack</h2>
                  <div className="flex flex-wrap gap-1">
                    {skills.split(',').map((s, idx) => (
                      <span key={idx} className="px-2 py-0.5 rounded bg-slate-100 text-slate-800 text-[10px] font-mono">
                        {s.trim()}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="font-sans">
                  <h2 className="text-xs font-bold uppercase tracking-wider mb-2" style={{ color: themeColor }}>Experience</h2>
                  <div className="space-y-3">
                    {experiences.map((exp, idx) => (
                      <div key={idx} className="space-y-0.5">
                        <div className="flex justify-between text-xs font-bold text-slate-900">
                          <span>{exp.role} @ {exp.company}</span>
                          <span className="text-[10px] text-slate-500 font-normal">{exp.period}</span>
                        </div>
                        <p className="text-xs text-slate-700">{exp.details}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* TEMPLATE 4 & 5 FALLBACKS */}
            {(activeTemplate === 'corporate' || activeTemplate === 'creative') && (
              <div className="space-y-5">
                <div className="p-4 rounded-xl text-white" style={{ backgroundColor: themeColor }}>
                  <h1 className="text-2xl font-extrabold">{name}</h1>
                  <p className="text-xs font-semibold text-white/90">{role}</p>
                  <p className="text-[11px] text-white/80 mt-1">{email} • {phone} • {location}</p>
                </div>
                <div>
                  <h2 className="text-xs font-bold uppercase tracking-wider text-slate-900 border-b pb-1 mb-2">Summary</h2>
                  <p className="text-xs text-slate-700 leading-relaxed">{summary}</p>
                </div>
                <div>
                  <h2 className="text-xs font-bold uppercase tracking-wider text-slate-900 border-b pb-1 mb-2">Skills</h2>
                  <p className="text-xs text-slate-800 font-medium">{skills}</p>
                </div>
                <div>
                  <h2 className="text-xs font-bold uppercase tracking-wider text-slate-900 border-b pb-1 mb-2">Experience</h2>
                  {experiences.map((exp, idx) => (
                    <div key={idx} className="mb-2 text-xs">
                      <div className="font-bold">{exp.role} — {exp.company}</div>
                      <p className="text-slate-600">{exp.details}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

          </div>
        </div>

      </div>
    </div>
  );
};
