import React, { useState, useEffect, useRef } from 'react';
import { ShieldCheck, ChevronDown, ChevronUp, Sparkles, CheckCircle2 } from 'lucide-react';
import { motion, AnimatePresence, useInView } from 'framer-motion';

interface ScoreDetail {
  score: number;
  confidence?: string;
  explanation?: string;
  citations?: string[];
  keywordOptimizationScore?: number;
  formattingScore?: number;
  sectionCompletenessScore?: number;
  achievementScore?: number;
}

interface ExplainableScoreCardProps {
  title: string;
  score: number;
  detail?: ScoreDetail;
  icon?: React.ReactNode;
  accentColor?: string;
}

function getScoreColor(score: number): { stroke: string; glow: string; badge: string; text: string } {
  if (score >= 85) return {
    stroke: '#10b981',
    glow: 'rgba(16,185,129,0.5)',
    badge: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
    text: 'text-emerald-400'
  };
  if (score >= 70) return {
    stroke: '#f59e0b',
    glow: 'rgba(245,158,11,0.5)',
    badge: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
    text: 'text-amber-400'
  };
  return {
    stroke: '#ef4444',
    glow: 'rgba(239,68,68,0.5)',
    badge: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
    text: 'text-rose-400'
  };
}

export const ExplainableScoreCard: React.FC<ExplainableScoreCardProps> = ({
  title, score, detail, icon
}) => {
  const [expanded, setExpanded] = useState(false);
  const [animatedScore, setAnimatedScore] = useState(0);
  const ref = useRef<HTMLDivElement>(null);
  const isInView = useInView(ref, { once: true, amount: 0.3 });

  const colors = getScoreColor(score);
  const confidence = detail?.confidence || (score >= 85 ? 'High' : score >= 70 ? 'Medium' : 'Low');
  const explanation = detail?.explanation || 'Calculated based on detected skills, keywords, formatting, and experience calibration in your uploaded resume.';
  const citations = detail?.citations || [];

  // Animate score counter on view
  useEffect(() => {
    if (!isInView) return;
    let start = 0;
    const duration = 1400;
    const step = 16;
    const increment = score / (duration / step);
    const timer = setInterval(() => {
      start += increment;
      if (start >= score) {
        setAnimatedScore(score);
        clearInterval(timer);
      } else {
        setAnimatedScore(Math.floor(start));
      }
    }, step);
    return () => clearInterval(timer);
  }, [isInView, score]);

  const circumference = 2 * Math.PI * 42;
  const strokeDasharray = `${(animatedScore / 100) * circumference} ${circumference}`;

  return (
    <motion.div
      ref={ref}
      whileHover={{ y: -5, scale: 1.01 }}
      transition={{ type: 'spring', stiffness: 300, damping: 20 }}
      className={`relative bg-gradient-to-br from-[#0a0810]/95 to-[#060408]/95 rounded-2xl p-5 border transition-all duration-300 cursor-pointer overflow-hidden group ${
        expanded
          ? 'border-[#C1121F]/50 shadow-[0_0_30px_rgba(193,18,31,0.2)]'
          : 'border-white/[0.08] hover:border-[#C1121F]/30'
      }`}
      onClick={() => setExpanded(!expanded)}
    >
      {/* Background glow on hover */}
      <div
        className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none rounded-2xl"
        style={{ background: `radial-gradient(circle at 50% 50%, ${colors.glow.replace('0.5', '0.06')}, transparent 70%)` }}
      />

      <div className="relative z-10">
        <div className="flex items-center justify-between">
          {/* Left: Icon + Title */}
          <div className="flex items-center gap-3">
            <div
              className="w-10 h-10 rounded-xl flex items-center justify-center transition-all duration-300 group-hover:scale-110"
              style={{ background: `${colors.stroke}15`, boxShadow: expanded ? `0 0 15px ${colors.glow}` : 'none' }}
            >
              {icon || <ShieldCheck className="w-5 h-5" style={{ color: colors.stroke }} />}
            </div>
            <div>
              <h3 className="text-xs font-bold text-slate-200 uppercase tracking-wider leading-tight">{title}</h3>
              <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded-full border mt-1 inline-block ${colors.badge}`}>
                {confidence} Confidence
              </span>
            </div>
          </div>

          {/* Right: Animated SVG Ring */}
          <div className="relative w-16 h-16 flex items-center justify-center">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
              {/* Background track */}
              <circle
                cx="50" cy="50" r="42"
                fill="none"
                stroke="rgba(255,255,255,0.06)"
                strokeWidth="7"
              />
              {/* Animated fill */}
              <circle
                cx="50" cy="50" r="42"
                fill="none"
                stroke={colors.stroke}
                strokeWidth="7"
                strokeLinecap="round"
                strokeDasharray={isInView ? strokeDasharray : `0 ${circumference}`}
                style={{
                  transition: 'stroke-dasharray 1.4s cubic-bezier(0.16, 1, 0.3, 1) 0.2s',
                  filter: `drop-shadow(0 0 6px ${colors.glow})`,
                }}
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-lg font-black text-white leading-none">{animatedScore}</span>
              <span className="text-[8px] text-slate-500 font-mono">/100</span>
            </div>
          </div>
        </div>

        {/* Expand toggle */}
        <div className="flex items-center justify-between pt-3 mt-3 border-t border-white/[0.05] text-[10px] text-slate-500 font-mono">
          <span>{expanded ? 'Hide AI breakdown' : 'View AI explanation & citations'}</span>
          <motion.div animate={{ rotate: expanded ? 180 : 0 }} transition={{ duration: 0.3 }}>
            <ChevronDown className="w-4 h-4 text-slate-400" />
          </motion.div>
        </div>
      </div>

      {/* Expanded Panel */}
      <AnimatePresence>
        {expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
            className="overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="pt-4 mt-4 border-t border-white/[0.06] space-y-3 relative z-10">
              {/* AI Rationale */}
              <div>
                <h4 className="text-[10px] font-bold uppercase tracking-widest flex items-center gap-1.5 mb-1.5"
                  style={{ color: colors.stroke }}>
                  <Sparkles className="w-3.5 h-3.5" /> AI Score Rationale
                </h4>
                <p className="text-[11px] text-slate-300 leading-relaxed bg-black/40 p-3 rounded-xl border border-white/[0.06] font-sans">
                  {explanation}
                </p>
              </div>

              {/* Sub-scores breakdown */}
              {detail?.keywordOptimizationScore !== undefined && (
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { label: 'Keywords', val: detail.keywordOptimizationScore },
                    { label: 'Formatting', val: detail.formattingScore },
                    { label: 'Sections', val: detail.sectionCompletenessScore },
                    { label: 'Achievements', val: detail.achievementScore },
                  ].filter(x => x.val !== undefined).map((item, i) => (
                    <div key={i} className="p-2 bg-slate-900/60 rounded-xl border border-white/[0.05]">
                      <span className="text-[9px] text-slate-500 font-mono uppercase">{item.label}</span>
                      <div className="flex items-center gap-2 mt-1">
                        <div className="flex-1 h-1 rounded-full bg-white/[0.06] overflow-hidden">
                          <motion.div
                            className="h-full rounded-full"
                            style={{ background: colors.stroke }}
                            initial={{ width: 0 }}
                            animate={{ width: `${item.val}%` }}
                            transition={{ duration: 1, delay: i * 0.1, ease: [0.16, 1, 0.3, 1] }}
                          />
                        </div>
                        <span className="text-[10px] font-bold text-white">{item.val}%</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Citations */}
              {citations.length > 0 && (
                <div>
                  <h5 className="text-[9px] font-bold text-slate-400 uppercase tracking-wider mb-1.5">
                    Resume Citations:
                  </h5>
                  <ul className="space-y-1">
                    {citations.map((cite, idx) => (
                      <li key={idx} className="text-[11px] text-slate-300 bg-[#C1121F]/5 border border-[#C1121F]/15 p-2 rounded-lg flex items-start gap-1.5">
                        <CheckCircle2 className="w-3 h-3 text-[#C1121F] shrink-0 mt-0.5" />
                        <span>"{cite}"</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};
