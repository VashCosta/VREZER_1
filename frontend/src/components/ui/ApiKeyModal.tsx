import React, { useState } from 'react';
import { Key, CheckCircle2, AlertTriangle, X, Sparkles, ShieldCheck, RefreshCw } from 'lucide-react';
import axios from 'axios';
import { motion, AnimatePresence } from 'framer-motion';

interface ApiKeyModalProps {
  isOpen: boolean;
  onClose: () => void;
  apiKey: string;
  onSaveKey: (key: string) => void;
}

export const ApiKeyModal: React.FC<ApiKeyModalProps> = ({ isOpen, onClose, apiKey, onSaveKey }) => {
  const [inputKey, setInputKey] = useState(apiKey || '');
  const [validating, setValidating] = useState(false);
  const [status, setStatus] = useState<{ valid?: boolean; message?: string; model?: string } | null>(null);

  if (!isOpen) return null;

  const handleTestKey = async () => {
    if (!inputKey.trim()) {
      setStatus({ valid: false, message: 'Please enter a valid API key string.' });
      return;
    }
    setValidating(true);
    setStatus(null);
    try {
      const res = await axios.post('/api/analyzer/validate-key', { apiKey: inputKey.trim() });
      if (res.data) {
        setStatus({
          valid: res.data.valid,
          message: res.data.message || 'Key validated successfully!',
          model: res.data.model || 'gemini-1.5-flash'
        });
      }
    } catch (err: any) {
      setStatus({
        valid: false,
        message: 'Validation failed: ' + (err.response?.data?.message || err.message)
      });
    } finally {
      setValidating(false);
    }
  };

  const handleSave = () => {
    onSaveKey(inputKey.trim());
    onClose();
  };

  const handleClear = () => {
    setInputKey('');
    onSaveKey('');
    setStatus(null);
  };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="w-full max-w-lg glass-card rounded-3xl p-6 md:p-8 border border-white/10 shadow-[0_0_50px_rgba(255,0,60,0.25)] relative overflow-hidden bg-[#0d131f] text-slate-100 space-y-6"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-red-600 to-rose-600 flex items-center justify-center text-white shadow-lg shadow-red-600/30">
                <Key className="w-5 h-5 text-amber-300" />
              </div>
              <div>
                <h3 className="text-lg font-black uppercase tracking-wider text-white">AI Engine Key Setup</h3>
                <p className="text-xs text-slate-400">Configure Google Gemini, OpenAI, or Groq API Key</p>
              </div>
            </div>
            <button onClick={onClose} className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition">
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="space-y-4">
            <div>
              <label className="text-xs font-mono font-bold text-slate-300 uppercase tracking-wider block mb-1">
                Your API Key
              </label>
              <div className="relative">
                <input
                  type="password"
                  value={inputKey}
                  onChange={(e) => setInputKey(e.target.value)}
                  placeholder="Paste your key (AIzaSy... or sk-...)"
                  className="w-full px-4 py-3 rounded-2xl bg-[#090d16] border border-white/15 focus:border-red-500 text-sm font-mono text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-red-500 transition"
                />
              </div>
              <p className="text-[11px] text-slate-400 mt-1.5 flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                Keys are stored locally in your browser session and sent directly to Google Gemini APIs.
              </p>
            </div>

            {status && (
              <motion.div
                initial={{ opacity: 0, y: -5 }}
                animate={{ opacity: 1, y: 0 }}
                className={`p-4 rounded-2xl border text-xs font-sans flex items-start gap-3 ${
                  status.valid
                    ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                    : 'bg-red-500/10 border-red-500/30 text-red-300'
                }`}
              >
                {status.valid ? <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-400 mt-0.5" /> : <AlertTriangle className="w-5 h-5 shrink-0 text-red-400 mt-0.5" />}
                <div>
                  <div className="font-bold">{status.message}</div>
                  {status.model && <div className="text-[10px] font-mono opacity-80 mt-0.5">Model: {status.model}</div>}
                </div>
              </motion.div>
            )}

            <div className="flex flex-wrap gap-2 pt-2">
              <button
                type="button"
                onClick={handleTestKey}
                disabled={validating}
                className="px-4 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-white font-bold text-xs uppercase tracking-wider border border-white/10 flex items-center gap-2 transition disabled:opacity-50"
              >
                {validating ? <RefreshCw className="w-4 h-4 animate-spin text-amber-400" /> : <Sparkles className="w-4 h-4 text-amber-300" />}
                Test & Validate Key
              </button>

              <button
                type="button"
                onClick={handleSave}
                className="flex-1 px-5 py-2.5 rounded-xl bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white font-bold text-xs uppercase tracking-widest shadow-lg shadow-red-600/30 flex items-center justify-center gap-2 transition"
              >
                Save & Apply Key
              </button>

              {inputKey && (
                <button
                  type="button"
                  onClick={handleClear}
                  className="px-3 py-2.5 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-400 font-bold text-xs uppercase tracking-wider transition"
                >
                  Clear Key
                </button>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
