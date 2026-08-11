import React, { useState } from 'react';
import { Bot, X, Send, Sparkles, User as UserIcon } from 'lucide-react';
import { api } from '../../lib/api';
import { useResumeContext } from '../../context/ResumeContext';

interface Message {
  id: string;
  sender: 'user' | 'bot';
  text: string;
}

export const AiChatbotWidget: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [input, setInput] = useState('');
  const { aiAnalysisResult, apiKey } = useResumeContext();
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      sender: 'bot',
      text: '👋 Hello! I am your **VREZER AI Coach**. Ask me anything about your current resume ATS score, skill gaps, company recommendations, or career roadmaps!'
    }
  ]);
  const [loading, setLoading] = useState(false);

  const handleSend = async (promptText?: string) => {
    const textToSend = promptText || input;
    if (!textToSend.trim()) return;
    const userMsg: Message = { id: Date.now().toString(), sender: 'user', text: textToSend };
    setMessages(prev => [...prev, userMsg]);
    if (!promptText) setInput('');
    setLoading(true);
    try {
      const activeKey = apiKey || localStorage.getItem('vrezerApiKey') || '';
      const headers: Record<string, string> = {};
      if (activeKey) {
        headers['X-GEMINI-API-KEY'] = activeKey;
      }
      const response = await api.post('/api/chat/ask', {
        prompt: textToSend,
        apiKey: activeKey,
        candidateContext: aiAnalysisResult && Object.keys(aiAnalysisResult).length > 0 ? aiAnalysisResult : null
      }, { headers });
      const botReply = response.data?.data?.reply || 'I am ready to help with your career acceleration!';
      setMessages(prev => [...prev, { id: (Date.now() + 1).toString(), sender: 'bot', text: botReply }]);
    } catch (error) {
      setMessages(prev => [
        ...prev,
        {
          id: (Date.now() + 1).toString(),
          sender: 'bot',
          text: '💡 **Career Tip**: To optimize your resume for ATS scanners, use standard section headers, clean text bullet points, and highlight metrics like percentage improvements and scale.'
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const quickPrompts = [
    "Improve my Resume Score",
    "ATS Scanner Tips",
    "Am I Ready for Google?",
    "Suggest High-Impact Projects"
  ];

  return (
    <div className="fixed bottom-6 right-6 z-50">
      {!isOpen ? (
        <button
          onClick={() => setIsOpen(true)}
          className="flex items-center gap-3 px-4 py-3 rounded-2xl bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 text-white shadow-2xl shadow-blue-500/40 hover:scale-105 transition-all duration-300 group border border-blue-400/30"
        >
          <div className="p-1.5 rounded-xl bg-white/10 group-hover:rotate-12 transition-transform">
            <Bot className="w-5 h-5 text-white" />
          </div>
          <span className="font-bold text-sm tracking-wide">Career Coach AI</span>
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
        </button>
      ) : (
        <div className="w-96 bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col h-[520px] transition-all">
          
          {/* Header */}
          <div className="p-4 bg-slate-950 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white shadow-md">
                <Sparkles className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-sm text-white">Career Intelligence Assistant</h3>
                <span className="text-[10px] text-emerald-400 flex items-center gap-1 font-medium">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span> Online & Heuristic Engine Active
                </span>
              </div>
            </div>
            <button
              onClick={() => setIsOpen(false)}
              className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Quick Prompts */}
          <div className="px-3 py-2 bg-slate-950/50 border-b border-slate-800/60 flex items-center gap-1.5 overflow-x-auto no-scrollbar">
            {quickPrompts.map((p, idx) => (
              <button
                key={idx}
                onClick={() => handleSend(p)}
                className="whitespace-nowrap px-2.5 py-1 rounded-lg bg-slate-800/70 hover:bg-blue-600/30 text-[11px] text-slate-300 hover:text-blue-300 border border-slate-700/50 transition"
              >
                {p}
              </button>
            ))}
          </div>

          {/* Messages Area */}
          <div className="flex-1 p-4 overflow-y-auto space-y-3 bg-slate-900/80">
            {messages.map((m) => (
              <div
                key={m.id}
                className={`flex gap-2.5 ${m.sender === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                {m.sender === 'bot' && (
                  <div className="w-7 h-7 rounded-lg bg-blue-600/20 border border-blue-500/30 flex items-center justify-center shrink-0 text-blue-400">
                    <Bot className="w-4 h-4" />
                  </div>
                )}
                <div
                  className={`max-w-[80%] p-3 rounded-2xl text-xs leading-relaxed ${
                    m.sender === 'user'
                      ? 'bg-blue-600 text-white rounded-br-none shadow-md'
                      : 'bg-slate-800 text-slate-200 border border-slate-700/60 rounded-bl-none'
                  }`}
                >
                  <p className="whitespace-pre-line">{m.text}</p>
                </div>
                {m.sender === 'user' && (
                  <div className="w-7 h-7 rounded-lg bg-slate-800 border border-slate-700 flex items-center justify-center shrink-0 text-slate-400">
                    <UserIcon className="w-4 h-4" />
                  </div>
                )}
              </div>
            ))}
            {loading && (
              <div className="flex items-center gap-2 text-xs text-slate-400">
                <Sparkles className="w-4 h-4 animate-spin text-blue-400" />
                <span>Career Intelligence AI is thinking...</span>
              </div>
            )}
          </div>

          {/* Input Box */}
          <div className="p-3 bg-slate-950 border-t border-slate-800 flex items-center gap-2">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSend()}
              placeholder="Ask anything about your career..."
              className="flex-1 bg-slate-900 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
            />
            <button
              onClick={() => handleSend()}
              className="p-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white shadow-md transition"
            >
              <Send className="w-4 h-4" />
            </button>
          </div>

        </div>
      )}
    </div>
  );
};
