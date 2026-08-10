import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';

export interface ResumeHistoryItem {
  id: string;
  name: string;
  date: string;
  result: any;
  rawText: string;
  fileName?: string;
}

interface ResumeContextType {
  aiAnalysisResult: any;
  setAiAnalysisResult: (data: any) => void;
  rawText: string;
  setRawText: (text: string) => void;
  apiKey: string;
  setApiKey: (key: string) => void;
  resumeHistory: ResumeHistoryItem[];
  addResumeToHistory: (name: string, result: any, text: string, fileName?: string) => void;
  removeResumeFromHistory: (id: string) => void;
  clearAiAnalysisResult: () => void;
}

const ResumeContext = createContext<ResumeContextType | undefined>(undefined);

export const ResumeProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [aiAnalysisResult, setAiAnalysisResultState] = useState<any>(() => {
    const saved = localStorage.getItem('vrezerAiResult');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        return null;
      }
    }
    return null;
  });

  const [rawText, setRawTextState] = useState<string>(() => {
    return localStorage.getItem('vrezerRawText') || '';
  });

  const [apiKey, setApiKeyState] = useState<string>(() => {
    return localStorage.getItem('vrezerApiKey') || '';
  });

  const [resumeHistory, setResumeHistoryState] = useState<ResumeHistoryItem[]>(() => {
    const saved = localStorage.getItem('vrezerHistory');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        return [];
      }
    }
    return [];
  });

  const setApiKey = (key: string) => {
    setApiKeyState(key);
    if (key) {
      localStorage.setItem('vrezerApiKey', key);
    } else {
      localStorage.removeItem('vrezerApiKey');
    }
  };

  const setAiAnalysisResult = (data: any) => {
    setAiAnalysisResultState(data);
    if (data) {
      localStorage.setItem('vrezerAiResult', JSON.stringify(data));
    } else {
      localStorage.removeItem('vrezerAiResult');
    }
  };

  const setRawText = (text: string) => {
    setRawTextState(text);
    if (text) {
      localStorage.setItem('vrezerRawText', text);
    } else {
      localStorage.removeItem('vrezerRawText');
    }
  };

  const addResumeToHistory = (name: string, result: any, text: string, fileName?: string) => {
    const newItem: ResumeHistoryItem = {
      id: 'res_' + Date.now(),
      name: name || result?.name || 'Resume Version',
      date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
      result,
      rawText: text,
      fileName: fileName || 'Resume.pdf'
    };
    const updated = [newItem, ...resumeHistory.filter(h => h.id !== newItem.id)];
    setResumeHistoryState(updated);
    localStorage.setItem('vrezerHistory', JSON.stringify(updated));
  };

  const removeResumeFromHistory = (id: string) => {
    const updated = resumeHistory.filter(h => h.id !== id);
    setResumeHistoryState(updated);
    localStorage.setItem('vrezerHistory', JSON.stringify(updated));
  };

  const clearAiAnalysisResult = () => {
    setAiAnalysisResult(null);
    setRawText('');
  };

  return (
    <ResumeContext.Provider value={{
      aiAnalysisResult,
      setAiAnalysisResult,
      rawText,
      setRawText,
      apiKey,
      setApiKey,
      resumeHistory,
      addResumeToHistory,
      removeResumeFromHistory,
      clearAiAnalysisResult
    }}>
      {children}
    </ResumeContext.Provider>
  );
};

export const useResumeContext = () => {
  const context = useContext(ResumeContext);
  if (context === undefined) {
    throw new Error('useResumeContext must be used within a ResumeProvider');
  }
  return context;
};
