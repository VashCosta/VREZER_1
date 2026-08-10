import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { Navbar } from './components/layout/Navbar';
import { Sidebar } from './components/layout/Sidebar';
import { ResumeProvider } from './context/ResumeContext';

// Pages
import { LandingPage } from './pages/LandingPage';
import { LoginPage } from './pages/AuthPages';
import { StudentDashboard } from './pages/StudentDashboard';
import { ResumeUploadPage } from './pages/ResumeUploadPage';
import { AtsAnalyzerPage } from './pages/AtsAnalyzerPage';
import { JdMatcherPage } from './pages/JdMatcherPage';
import { SkillGapPage } from './pages/SkillGapPage';
import { CareerRoadmapPage } from './pages/CareerRoadmapPage';
import { AiInterviewPage } from './pages/AiInterviewPage';
import { SalaryPredictorPage } from './pages/SalaryPredictorPage';
import { PortfolioAnalyzerPage } from './pages/PortfolioAnalyzerPage';
import { ProjectRecommenderPage } from './pages/ProjectRecommenderPage';
import { ResumeBuilderPage } from './pages/ResumeBuilderPage';
import { CoverLetterGeneratorPage } from './pages/CoverLetterGeneratorPage';
import { LinkedinOptimizerPage } from './pages/LinkedinOptimizerPage';
import { RecruiterDashboardPage } from './pages/RecruiterDashboardPage';
import { AdminPanelPage } from './pages/AdminPanelPage';
import { LiveJobsPage } from './pages/LiveJobsPage';

import { AnalyzingPage } from './pages/AnalyzingPage';

const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const location = useLocation();
  const isLandingPage = location.pathname === '/';

  return (
    <div className="min-h-screen bg-[#0b0f19] text-slate-100 flex flex-col font-sans selection:bg-indigo-500 selection:text-white">
      <Navbar />
      <div className="flex-1 flex w-full max-w-[1600px] mx-auto">
        {!isLandingPage && <Sidebar />}
        <main className={`flex-1 overflow-y-auto max-w-full ${isLandingPage ? 'p-0' : 'p-4 lg:p-8'}`}>
          {children}
        </main>
      </div>
    </div>
  );
};

export const AppContent: React.FC = () => {
  return (
    <ResumeProvider>
      <Routes>
        <Route path="/" element={<AppLayout><LandingPage /></AppLayout>} />
        <Route path="/login" element={<LoginPage />} />
        
        {/* Student App Routes */}
        <Route path="/dashboard" element={<AppLayout><StudentDashboard /></AppLayout>} />
        <Route path="/upload" element={<AppLayout><ResumeUploadPage /></AppLayout>} />
        <Route path="/analyzing" element={<AnalyzingPage />} />
        <Route path="/ats-analyzer" element={<AppLayout><AtsAnalyzerPage /></AppLayout>} />
        <Route path="/jd-matcher" element={<AppLayout><JdMatcherPage /></AppLayout>} />
        <Route path="/skill-gap" element={<AppLayout><SkillGapPage /></AppLayout>} />
        <Route path="/career-roadmap" element={<AppLayout><CareerRoadmapPage /></AppLayout>} />
        <Route path="/ai-interview" element={<AppLayout><AiInterviewPage /></AppLayout>} />
        <Route path="/salary-predictor" element={<AppLayout><SalaryPredictorPage /></AppLayout>} />
        <Route path="/portfolio-analyzer" element={<AppLayout><PortfolioAnalyzerPage /></AppLayout>} />
        <Route path="/project-recommender" element={<AppLayout><ProjectRecommenderPage /></AppLayout>} />
        <Route path="/resume-builder" element={<AppLayout><ResumeBuilderPage /></AppLayout>} />
        <Route path="/cover-letter" element={<AppLayout><CoverLetterGeneratorPage /></AppLayout>} />
        <Route path="/linkedin-optimizer" element={<AppLayout><LinkedinOptimizerPage /></AppLayout>} />
        <Route path="/live-jobs" element={<AppLayout><LiveJobsPage /></AppLayout>} />

        {/* Recruiter Routes */}
        <Route path="/recruiter" element={<AppLayout><RecruiterDashboardPage /></AppLayout>} />

        {/* Admin Routes */}
        <Route path="/admin" element={<AppLayout><AdminPanelPage /></AppLayout>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </ResumeProvider>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <ThemeProvider>
        <Router>
          <AppContent />
        </Router>
      </ThemeProvider>
    </AuthProvider>
  );
};

export default App;

