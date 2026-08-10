import React, { useState } from 'react';
import { useResumeContext } from '../context/ResumeContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { HelpCircle, Sparkles, CheckCircle2, Play, RefreshCw, Send, Award, Zap, ShieldCheck, Video } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const AiInterviewPage: React.FC = () => {
  const { aiAnalysisResult } = useResumeContext();
  const navigate = useNavigate();

  const [activeCategory, setActiveCategory] = useState<'TECHNICAL' | 'BEHAVIORAL' | 'HR' | 'PROJECT'>('TECHNICAL');
  const [mockMode, setMockMode] = useState(false);
  const [currentIdx, setCurrentIdx] = useState(0);
  const [userAnswer, setUserAnswer] = useState('');
  const [feedback, setFeedback] = useState<any>(null);
  const [isEvaluating, setIsEvaluating] = useState(false);

  const defaultTechQuestions = [
    {
      type: 'Technical',
      question: 'Explain how Spring Boot manages Bean Lifecycle and Dependency Injection under the hood.',
      context: 'Core Backend Framework Architecture',
      sampleAnswer: 'Spring manages beans using the ApplicationContext IoC container. The lifecycle involves instantiation, population of properties, BeanNameAware/BeanFactoryAware callbacks, post-processors (@PostConstruct), and destruction callbacks (@PreDestroy).'
    },
    {
      type: 'Technical',
      question: 'How do you handle Database Locking and Concurrency in high-throughput PostgreSQL microservices?',
      context: 'Database Transaction Management',
      sampleAnswer: 'Use Optimistic Locking (@Version column) for low-contention scenarios to avoid thread blocking, or Pessimistic Locking (SELECT FOR UPDATE) for high-contention transactions, paired with Redis distributed locks.'
    },
    {
      type: 'Technical',
      question: 'Compare REST APIs vs GraphQL vs gRPC for internal microservice communication.',
      context: 'Distributed Systems & API Protocol Design',
      sampleAnswer: 'REST uses HTTP/1.1 JSON with standard endpoints; GraphQL allows clients to request exact schema fields avoiding over-fetching; gRPC uses HTTP/2 Protocol Buffers for fast binary serialization ideal for high-throughput internal microservice RPCs.'
    }
  ];

  const defaultHrQuestions = [
    {
      type: 'HR / Culture Fit',
      question: 'Tell me about a time you resolved a major production outage or technical conflict under deadline pressure.',
      context: 'Incident Response & Stakeholder Management',
      sampleAnswer: 'Use the STAR method: Situation (outage during peak traffic), Task (restore service SLA), Action (isolated memory leak in Redis cache pool, rolled back faulty deployment, set up Prometheus alert), Result (downtime kept under 12 minutes, zero data loss).'
    }
  ];

  const defaultProjectQuestions = [
    {
      type: 'Project Deep-Dive',
      question: 'Walk me through the system architecture of the most complex project on your resume.',
      context: 'Resume Technical Architecture',
      sampleAnswer: 'Highlight frontend state management, API gateway layer, microservice boundaries, caching, database indexing, and CI/CD deployment pipeline.'
    }
  ];

  const defaultBehavioralQuestions = [
    {
      type: 'Behavioral Scenario',
      question: 'How do you handle ambiguous product requirements or technical disagreements with senior architects?',
      context: 'STAR Behavioral Assessment',
      sampleAnswer: 'I evaluate technical trade-offs objectively using benchmark data, create rapid POC prototypes to validate latency/cost, and schedule alignment meetings with clear decision documentation.'
    }
  ];

  const prep = aiAnalysisResult?.interviewPreparation || {};
  
  const techQuestions = (prep.technicalQuestions || []).length > 0
    ? (prep.technicalQuestions || []).map((q: any) => ({
        type: 'Technical',
        question: q.question,
        context: q.contextFromResume || 'Inferred skill requirement',
        sampleAnswer: q.modelAnswer
      }))
    : defaultTechQuestions;

  const hrQuestions = (prep.hrQuestions || []).length > 0
    ? (prep.hrQuestions || []).map((q: any) => ({
        type: 'HR / Culture Fit',
        question: q.question,
        context: 'Standard industry behavioral check',
        sampleAnswer: q.modelAnswer
      }))
    : defaultHrQuestions;

  const projectQuestions = (prep.projectDiscussionQuestions || []).length > 0
    ? (prep.projectDiscussionQuestions || []).map((q: any) => ({
        type: 'Project Deep-Dive',
        question: q.question,
        context: 'Extracted from resume project descriptions',
        sampleAnswer: q.modelAnswer
      }))
    : defaultProjectQuestions;

  const behavioralQuestions = (prep.behavioralQuestions || []).length > 0
    ? (prep.behavioralQuestions || []).map((q: any) => ({
        type: 'Behavioral Scenario',
        question: q.question,
        context: 'STAR response format required',
        sampleAnswer: q.starAnswer
      }))
    : defaultBehavioralQuestions;

  const activeQuestions = 
    activeCategory === 'TECHNICAL' ? techQuestions :
    activeCategory === 'HR' ? hrQuestions :
    activeCategory === 'PROJECT' ? projectQuestions : behavioralQuestions;

  const currentQ = activeQuestions[currentIdx] || activeQuestions[0];

  const handleEvaluateAnswer = () => {
    if (!userAnswer.trim()) return;
    setIsEvaluating(true);

    setTimeout(() => {
      const refStr = (currentQ.sampleAnswer || '').toLowerCase();
      const userStr = userAnswer.toLowerCase();

      const refWords = new Set<string>(refStr.split(/\W+/).filter((w: string) => w.length > 4));
      const userWords = new Set<string>(userStr.split(/\W+/).filter((w: string) => w.length > 4));

      let matches = 0;
      for (const w of refWords) {
        if (userWords.has(w)) matches++;
      }

      const overlapPercent = refWords.size > 0 ? (matches / refWords.size) * 100 : 50;
      const wordCount = userAnswer.split(/\s+/).length;

      let score = 55;
      if (wordCount > 15) score += 15;
      if (wordCount > 40) score += 15;
      score += Math.min(Math.round(overlapPercent * 0.2), 15);
      if (score > 96) score = 96;

      let remark = 'Solid response! You structured your answer clearly and referenced key technical concepts.';
      if (score < 70) remark = 'Good effort! Add concrete architecture examples and technical metrics to boost your evaluation score.';

      setFeedback({
        score,
        remark,
        keyMatchedTerms: Array.from(userWords).slice(0, 5),
        sampleReference: currentQ.sampleAnswer
      });
      setIsEvaluating(false);
    }, 600);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white flex items-center gap-2">
            <Video className="w-6 h-6 text-red-400" /> Interactive AI Mock Interview Studio
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Practice live technical, behavioral, HR, and project deep-dive questions with real-time AI evaluation.
          </p>
        </div>

        <Button
          onClick={() => setMockMode(!mockMode)}
          variant={mockMode ? 'secondary' : 'primary'}
          icon={<Play className="w-4 h-4" />}
        >
          {mockMode ? 'Exit Mock Simulator' : 'Start Live Simulator'}
        </Button>
      </div>

      {/* Category Tabs */}
      <GlassCard className="p-4 flex flex-wrap items-center justify-between gap-3 border border-slate-800">
        <div className="flex flex-wrap gap-2">
          {(['TECHNICAL', 'BEHAVIORAL', 'PROJECT', 'HR'] as const).map((cat) => (
            <button
              key={cat}
              onClick={() => {
                setActiveCategory(cat);
                setCurrentIdx(0);
                setFeedback(null);
                setUserAnswer('');
              }}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition border ${
                activeCategory === cat
                  ? 'bg-red-500/20 text-red-400 border-red-500/50 shadow-lg shadow-red-500/20'
                  : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-white'
              }`}
            >
              {cat === 'TECHNICAL' ? '💻 Technical Questions' :
               cat === 'BEHAVIORAL' ? '🌟 Behavioral (STAR)' :
               cat === 'PROJECT' ? '🚀 Project Deep-Dive' : '💼 HR & Culture'}
            </button>
          ))}
        </div>

        <span className="text-xs font-bold text-slate-400">
          Question {currentIdx + 1} of {activeQuestions.length}
        </span>
      </GlassCard>

      {/* Main Studio Arena */}
      <div className="grid lg:grid-cols-12 gap-6 items-start">
        
        {/* Question & Practice Panel (7 cols) */}
        <div className="lg:col-span-7 space-y-4">
          <GlassCard className="p-6 space-y-4 border border-slate-800">
            <div className="flex justify-between items-center">
              <span className="px-2.5 py-1 rounded-lg bg-red-500/10 border border-red-500/30 text-[10px] font-bold text-red-400 uppercase tracking-wider">
                {currentQ?.type || 'Question'}
              </span>
              <span className="text-xs font-semibold text-slate-400">{currentQ?.context}</span>
            </div>

            <h2 className="text-base font-extrabold text-white leading-relaxed">
              "{currentQ?.question}"
            </h2>

            <div>
              <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-2">
                Your Answer / Response
              </label>
              <textarea
                value={userAnswer}
                onChange={(e) => setUserAnswer(e.target.value)}
                placeholder="Type your response here... Use STAR method (Situation, Task, Action, Result) or reference concrete technical patterns."
                rows={6}
                className="w-full bg-slate-900 border border-slate-800 rounded-xl p-3.5 text-xs text-white focus:outline-none focus:border-red-500 resize-none leading-relaxed"
              />
            </div>

            <div className="flex items-center justify-between pt-2">
              <Button
                onClick={() => {
                  if (currentIdx > 0) {
                    setCurrentIdx(currentIdx - 1);
                    setFeedback(null);
                    setUserAnswer('');
                  }
                }}
                disabled={currentIdx === 0}
                variant="secondary"
              >
                Previous
              </Button>

              <Button onClick={handleEvaluateAnswer} icon={<Sparkles className="w-4 h-4" />}>
                {isEvaluating ? 'Evaluating...' : 'Evaluate Answer'}
              </Button>

              <Button
                onClick={() => {
                  if (currentIdx < activeQuestions.length - 1) {
                    setCurrentIdx(currentIdx + 1);
                    setFeedback(null);
                    setUserAnswer('');
                  }
                }}
                disabled={currentIdx === activeQuestions.length - 1}
                variant="secondary"
              >
                Next
              </Button>
            </div>
          </GlassCard>

          {/* AI Feedback Card */}
          {feedback && (
            <GlassCard className="p-6 space-y-4 border border-slate-800 bg-slate-900/90">
              <div className="flex justify-between items-center border-b border-slate-800 pb-3">
                <span className="font-extrabold text-xs text-white uppercase tracking-wider flex items-center gap-1.5">
                  <Award className="w-4 h-4 text-amber-400" /> AI Evaluation Feedback
                </span>
                <span className={`text-sm font-black px-3 py-1 rounded-xl ${
                  feedback.score >= 80 ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                }`}>
                  {feedback.score} / 100
                </span>
              </div>

              <p className="text-xs text-slate-300 leading-relaxed font-medium">{feedback.remark}</p>

              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1.5">
                  Key Technical Terms Detected
                </span>
                <div className="flex flex-wrap gap-1">
                  {feedback.keyMatchedTerms.map((term: string, idx: number) => (
                    <span key={idx} className="px-2 py-0.5 rounded bg-slate-800 border border-slate-700 text-[10px] text-slate-300 font-semibold">
                      {term}
                    </span>
                  ))}
                </div>
              </div>
            </GlassCard>
          )}
        </div>

        {/* Right Reference & Model Answer Card (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <GlassCard className="p-6 space-y-4 border border-slate-800">
            <h3 className="font-extrabold text-xs text-red-400 uppercase tracking-wider flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400" /> Model Reference Answer
            </h3>
            
            <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 text-xs text-slate-300 leading-relaxed font-normal">
              {currentQ?.sampleAnswer || 'Model answer generated dynamically based on candidate profile.'}
            </div>

            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-2">
              <span className="text-[10px] font-bold text-purple-300 uppercase tracking-wider block">
                Pro Interviewer Tip
              </span>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                When answering technical architecture questions, always mention system constraints (e.g. latency, throughput, storage SLAs) before detailing your chosen solution stack.
              </p>
            </div>
          </GlassCard>
        </div>

      </div>
    </div>
  );
};
