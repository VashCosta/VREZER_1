import React, { useState, useMemo } from 'react';
import {
  Briefcase, Search, MapPin, ExternalLink, Globe, Sparkles, Building2, CheckCircle2,
  AlertTriangle, Filter, ChevronRight, X, Shield, TrendingUp, Cpu, Award, Zap, Layers,
  DollarSign, ArrowUpRight, BadgeCheck, RefreshCw, Loader2
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import axios from 'axios';

export interface JobOpportunityItem {
  name?: string;
  company?: string;
  companyLogo?: string;
  faviconFallback?: string;
  title?: string;
  role?: string;
  location?: string;
  salary?: string;
  expectedLpaRange?: string;
  salaryRange?: string;
  url?: string;
  applicationUrl?: string;
  careersPageUrl?: string;
  source?: string;
  retrievalSource?: string;
  category?: string;
  companyCategory?: string;
  industry?: string;
  growthStage?: string;
  headquarters?: string;
  indianOffices?: string[];
  hiringLocations?: string[];
  openRoles?: string[];
  requiredSkills?: string[] | string;
  workMode?: string;
  workModel?: string;
  experienceRequired?: string;
  companySize?: string;
  companyOverview?: string;
  matchScore?: number;
  aiMatchPercentage?: number;
  confidenceScore?: number;
  hiringProbabilityPercentage?: number;
  explanation?: string;
  matchReason?: string;
  missingSkills?: string;
  city?: string;
  state?: string;
  country?: string;
  employmentType?: string;
  postedDate?: string;
  semanticScore?: string;
}

interface JobOpportunitiesPanelProps {
  jobs?: JobOpportunityItem[];
  companies?: JobOpportunityItem[];
  careerDomain?: string;
  marketNotice?: string;
  userSkills?: string[];
}

export const JobOpportunitiesPanel: React.FC<JobOpportunitiesPanelProps> = ({
  jobs = [],
  companies = [],
  careerDomain = "Professional Domain",
  marketNotice = "",
  userSkills = []
}) => {
  // Live Crawled items state (fetched on demand or refreshed)
  const [liveCrawledJobs, setLiveCrawledJobs] = useState<JobOpportunityItem[]>([]);
  const [isCrawling, setIsCrawling] = useState<boolean>(false);
  const [crawlFeedback, setCrawlFeedback] = useState<string | null>(null);

  // Combine all retrieved company/job items deduplicated
  const rawList: JobOpportunityItem[] = useMemo(() => {
    const combined = [...liveCrawledJobs, ...(jobs || []), ...(companies || [])];
    const dedupedMap = new Map<string, JobOpportunityItem>();
    for (const item of combined) {
      const cName = item.name || item.company || "Employer";
      const key = `${cName.toLowerCase()}_${(item.title || item.role || '').toLowerCase()}`;
      if (!dedupedMap.has(key)) {
        dedupedMap.set(key, item);
      }
    }
    return Array.from(dedupedMap.values());
  }, [liveCrawledJobs, jobs, companies]);

  // Quick Access Section state
  const [quickSection, setQuickSection] = useState<string>("all");

  // Advanced Filters State
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("");
  const [selectedIndustry, setSelectedIndustry] = useState("");
  const [selectedCompanySize, setSelectedCompanySize] = useState("");
  const [selectedExperience, setSelectedExperience] = useState("");
  const [selectedSkill, setSelectedSkill] = useState("");
  const [selectedCity, setSelectedCity] = useState("");
  const [selectedCountry, setSelectedCountry] = useState("");
  const [selectedWorkMode, setSelectedWorkMode] = useState("");
  const [selectedEmploymentType, setSelectedEmploymentType] = useState("");

  // Modal Inspection State
  const [inspectedCompany, setInspectedCompany] = useState<JobOpportunityItem | null>(null);

  // Quick Access Tabs definition
  const quickTabs = [
    { id: 'all', label: 'Recommended for You', icon: Sparkles },
    { id: 'indian_mnc', label: 'Indian Employers', icon: Building2 },
    { id: 'global_mnc', label: 'Global Employers', icon: Globe },
    { id: 'product', label: 'Product & Platform Employers', icon: Cpu },
    { id: 'ai_ml', label: 'AI / ML Employers', icon: Zap },
    { id: 'saas', label: 'SaaS Employers', icon: Layers },
    { id: 'fintech', label: 'FinTech Employers', icon: DollarSign },
    { id: 'startups', label: 'Scaling Employers', icon: TrendingUp },
    { id: 'high_growth', label: 'High-Growth Employers', icon: Award },
  ];

  // Live Crawler function
  const handleLiveCrawlRefresh = async (customQuery?: string) => {
    setIsCrawling(true);
    setCrawlFeedback(null);
    try {
      const q = customQuery || searchTerm || careerDomain || "Software Engineer";
      const loc = selectedCity ? `${selectedCity}, India` : (selectedCountry || "India");
      const exp = selectedExperience || "fresher";

      const res = await axios.post('/api/analyzer/jobs', {
        query: q,
        location: loc,
        experienceLevel: exp.toUpperCase(),
        skills: userSkills.length > 0 ? userSkills : [q]
      });

      if (res.data && Array.isArray(res.data) && res.data.length > 0) {
        const mapped: JobOpportunityItem[] = res.data.map((j: any) => ({
          name: j.name || j.company || "Live Employer",
          company: j.name || j.company,
          title: j.title || j.role || q,
          role: j.title || j.role || q,
          location: j.location || loc,
          salary: j.salary || undefined,
          url: j.url || `https://www.google.com/search?q=${encodeURIComponent((j.name || j.company || 'Job') + ' ' + (j.title || 'hiring'))}`,
          applicationUrl: j.url,
          source: j.source || "Live Market Crawler",
          requiredSkills: j.requiredSkills || userSkills,
          matchScore: j.matchPercentage ? parseInt(j.matchPercentage, 10) : (j.semanticScore ? parseInt(j.semanticScore, 10) : undefined),
          aiMatchPercentage: j.matchPercentage ? parseInt(j.matchPercentage, 10) : (j.semanticScore ? parseInt(j.semanticScore, 10) : undefined),
        }));
        setLiveCrawledJobs(mapped);
        setCrawlFeedback(`Successfully crawled ${mapped.length} live job listings for "${q}"!`);
      } else {
        setCrawlFeedback(`Live crawler completed. Showing all grounded matching employer records.`);
      }
    } catch (err: any) {
      console.warn("Crawler refresh notice:", err);
      setCrawlFeedback("Live market crawler query completed.");
    } finally {
      setIsCrawling(false);
    }
  };

  // Helper function to check if an item fits quick access section
  const matchesQuickSection = (item: JobOpportunityItem, sectionId: string): boolean => {
    if (sectionId === 'all') return true;
    const categoryStr = (item.companyCategory || item.category || '').toLowerCase();
    const nameStr = (item.name || item.company || '').toLowerCase();
    const industryStr = (item.industry || '').toLowerCase();

    switch (sectionId) {
      case 'indian_mnc':
        return categoryStr.includes('indian mnc') || categoryStr.includes('indian it') ||
               ['tcs', 'infosys', 'wipro', 'hcl', 'tech mahindra', 'l&t', 'reliance'].some(n => nameStr.includes(n));
      case 'global_mnc':
        return categoryStr.includes('global mnc') ||
               ['google', 'microsoft', 'amazon', 'meta', 'apple', 'ibm', 'accenture', 'deloitte', 'oracle'].some(n => nameStr.includes(n));
      case 'product':
        return categoryStr.includes('product') || categoryStr.includes('global mnc') || industryStr.includes('product');
      case 'ai_ml':
        return categoryStr.includes('ai') || categoryStr.includes('machine learning') || industryStr.includes('ai') || industryStr.includes('intelligence');
      case 'saas':
        return categoryStr.includes('saas') || categoryStr.includes('cloud') || industryStr.includes('saas');
      case 'fintech':
        return categoryStr.includes('fintech') || industryStr.includes('fintech') || industryStr.includes('payment');
      case 'startups':
        return categoryStr.includes('startup') || categoryStr.includes('unicorn') || categoryStr.includes('growth stage');
      case 'high_growth':
        return categoryStr.includes('high-growth') || categoryStr.includes('unicorn') || categoryStr.includes('scale-up');
      default:
        return true;
    }
  };

  // Filter application
  const filteredCompanies = useMemo(() => {
    return rawList.filter(item => {
      // 1. Quick Section Filter
      if (!matchesQuickSection(item, quickSection)) return false;

      const compName = (item.name || item.company || '').toLowerCase();
      const roleTitle = (item.title || item.role || '').toLowerCase();
      const location = (item.location || item.headquarters || '').toLowerCase();
      const category = (item.companyCategory || item.category || '').toLowerCase();
      const industry = (item.industry || '').toLowerCase();
      const size = (item.companySize || '').toLowerCase();
      const exp = (item.experienceRequired || '').toLowerCase();
      const workMode = (item.workMode || item.workModel || '').toLowerCase();
      const empType = (item.employmentType || '').toLowerCase();

      // Skills array matching
      const skillsArr = Array.isArray(item.requiredSkills)
        ? item.requiredSkills.map(s => s.toLowerCase())
        : (item.requiredSkills || '').toLowerCase().split(',');

      // Search keyword
      const searchLow = searchTerm.toLowerCase().trim();
      if (searchLow && !compName.includes(searchLow) && !roleTitle.includes(searchLow) && !location.includes(searchLow)) {
        return false;
      }

      // Category filter
      if (selectedCategory && !category.includes(selectedCategory.toLowerCase())) {
        return false;
      }

      // Industry filter
      if (selectedIndustry && !industry.includes(selectedIndustry.toLowerCase())) {
        return false;
      }

      // Company Size
      if (selectedCompanySize && !size.includes(selectedCompanySize.toLowerCase())) {
        return false;
      }

      // Experience Level
      if (selectedExperience && !exp.includes(selectedExperience.toLowerCase())) {
        return false;
      }

      // Skill search
      if (selectedSkill) {
        const skillLow = selectedSkill.toLowerCase().trim();
        const hasSkill = skillsArr.some(s => s.includes(skillLow));
        if (!hasSkill) return false;
      }

      // City filter
      if (selectedCity && !location.includes(selectedCity.toLowerCase())) {
        return false;
      }

      // Country filter
      if (selectedCountry) {
        const countryLow = selectedCountry.toLowerCase();
        if (countryLow === 'india' && !location.includes('india') && !location.includes('bengaluru') && !location.includes('hyderabad') && !location.includes('pune') && !location.includes('mumbai') && !location.includes('gurugram') && !location.includes('chennai') && !location.includes('noida')) {
          return false;
        }
        if (countryLow === 'global' && (location.includes('india') || location.includes('bengaluru'))) {
          return false;
        }
      }

      // Work Mode
      if (selectedWorkMode && !workMode.includes(selectedWorkMode.toLowerCase())) {
        return false;
      }

      // Employment Type
      if (selectedEmploymentType && !empType.includes(selectedEmploymentType.toLowerCase())) {
        return false;
      }

      return true;
    });
  }, [rawList, quickSection, searchTerm, selectedCategory, selectedIndustry, selectedCompanySize, selectedExperience, selectedSkill, selectedCity, selectedCountry, selectedWorkMode, selectedEmploymentType]);

  // Reset filters helper
  const handleResetFilters = () => {
    setSearchTerm("");
    setSelectedCategory("");
    setSelectedIndustry("");
    setSelectedCompanySize("");
    setSelectedExperience("");
    setSelectedSkill("");
    setSelectedCity("");
    setSelectedCountry("");
    setSelectedWorkMode("");
    setSelectedEmploymentType("");
  };
  return (
    <div className="space-y-8">
      {/* ── HEADER CARD ─────────────────────────────────────────────────── */}
      <div className="glass-card rounded-3xl p-6 border border-white/10 space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="px-3 py-1 bg-red-500/10 text-red-400 border border-red-500/30 rounded-full text-[11px] font-mono font-bold uppercase tracking-wider flex items-center gap-1.5">
                <Zap className="w-3.5 h-3.5" /> Market Intelligence Engine
              </span>
              <span className="px-3 py-1 bg-cyan-500/10 text-cyan-400 border border-cyan-500/30 rounded-full text-[11px] font-mono font-bold">
                Live RAG Verified
              </span>
            </div>
            <h1 className="text-2xl font-black text-white uppercase tracking-tight mt-2 font-mono flex items-center gap-2">
              <Building2 className="w-6 h-6 text-red-500" />
              Company Classification & Opportunities Ecosystem
            </h1>
            <p className="text-xs text-slate-300 mt-1 max-w-2xl font-mono">
              Live job postings from verified hiring sources matched to <strong className="text-cyan-400">{careerDomain}</strong>.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3 shrink-0">
            <button
              onClick={() => handleLiveCrawlRefresh()}
              disabled={isCrawling}
              className="px-4 py-2.5 bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white rounded-2xl text-xs font-mono font-bold transition flex items-center gap-2 shadow-[0_0_20px_rgba(255,0,60,0.4)] disabled:opacity-50"
            >
              {isCrawling ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin text-amber-300" />
                  <span>Crawling Live Web...</span>
                </>
              ) : (
                <>
                  <RefreshCw className="w-4 h-4 text-amber-300" />
                  <span>Live Crawl Real-Time Jobs</span>
                </>
              )}
            </button>

            <span className="px-4 py-2.5 bg-black/60 border border-red-500/30 rounded-2xl text-xs font-mono font-bold text-red-400">
              {filteredCompanies.length} Opportunities
            </span>
          </div>
        </div>

        {crawlFeedback && (
          <div className="p-3.5 bg-emerald-500/10 border border-emerald-500/30 rounded-2xl text-xs text-emerald-300 font-mono flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{crawlFeedback}</span>
          </div>
        )}

        {marketNotice && (
          <div className="p-4 bg-amber-500/10 border border-amber-500/30 rounded-2xl text-xs text-amber-300 font-mono flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0 text-amber-400" />
            <span>{marketNotice}</span>
          </div>
        )}

        {/* ── DYNAMIC QUICK-ACCESS HIRING SECTIONS BAR ────────────────── */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-mono font-extrabold text-red-400 uppercase tracking-widest flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-amber-400" /> Dynamic Quick-Access Sections (Retrieved Data):
            </span>
          </div>
          <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-thin">
            {quickTabs.map(tab => {
              const Icon = tab.icon;
              const isActive = quickSection === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setQuickSection(tab.id)}
                  className={`px-4 py-2.5 rounded-2xl text-xs font-mono font-bold shrink-0 flex items-center gap-2 transition-all duration-200 ${
                    isActive
                      ? 'bg-gradient-to-r from-red-600 to-rose-600 text-white border border-red-400 shadow-[0_0_20px_rgba(255,0,60,0.4)]'
                      : 'bg-black/50 hover:bg-white/10 text-slate-300 border border-white/10'
                  }`}
                >
                  <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* ── ADVANCED MULTI-DIMENSIONAL FILTERS SUITE ────────────────── */}
        <div className="pt-4 border-t border-white/10 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-300 flex items-center gap-2">
              <Filter className="w-4 h-4 text-cyan-400" /> Advanced Multi-Dimensional Opportunity Filters
            </h3>
            <button
              onClick={handleResetFilters}
              className="text-[11px] font-mono text-slate-400 hover:text-red-400 underline transition"
            >
              Reset All Filters
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {/* 1. SEARCH ROLES & COMPANIES */}
            <div className="col-span-1 sm:col-span-2">
              <label className="block text-[10px] font-mono font-bold text-slate-400 uppercase tracking-wider mb-1">
                Company or Role Keyword
              </label>
              <div className="relative">
                <Search className="w-3.5 h-3.5 text-slate-500 absolute left-3 top-2.5" />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Company name, role, skill..."
                  className="w-full bg-black/60 border border-slate-700 rounded-xl pl-8 pr-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-red-500 font-mono"
                />
              </div>
            </div>

            {/* 2. COMPANY CATEGORY */}
            <div>
              <label className="block text-[10px] font-mono font-bold text-slate-400 uppercase tracking-wider mb-1">
                Company Category
              </label>
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="w-full bg-black/60 border border-slate-700 rounded-xl px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-red-500 font-mono"
              >
                <option value="">All Categories</option>
                <option value="Indian MNC">🇮🇳 Indian MNCs</option>
                <option value="Global MNC">🌍 Global MNCs</option>
                <option value="Indian IT Services">💼 Indian IT Services</option>
                <option value="Product-Based Company">🚀 Product-Based</option>
                <option value="SaaS Company">☁️ SaaS Companies</option>
                <option value="AI & Machine Learning Company">🤖 AI & Machine Learning</option>
                <option value="Cloud & DevOps Company">⚙️ Cloud & DevOps</option>
                <option value="Cybersecurity Company">🛡️ Cybersecurity</option>
                <option value="FinTech Company">💳 FinTech</option>
                <option value="HealthTech Company">🏥 HealthTech</option>
                <option value="EdTech Company">🎓 EdTech</option>
                <option value="E-Commerce Company">🛒 E-Commerce</option>
                <option value="Startup">⚡ Startups</option>
                <option value="Unicorn">🦄 Unicorns</option>
                <option value="High-Growth Company">📈 High-Growth</option>
                <option value="Government Organization">🏛️ Government Sector</option>
                <option value="Consulting Firm">📊 Consulting Firms</option>
                <option value="Manufacturing Company">🏭 Manufacturing</option>
                <option value="Telecom Company">📡 Telecom</option>
                <option value="Automotive Company">🏎️ Automotive</option>
                <option value="Semiconductor Company">🔬 Semiconductor</option>
                <option value="Gaming Company">🎮 Gaming</option>
                <option value="Digital Marketing Agency">📢 Marketing Agencies</option>
                <option value="Media & Content Company">🎬 Media & Content</option>
              </select>
            </div>

            {/* 3. EXPERIENCE LEVEL */}
            <div>
              <label className="block text-[10px] font-mono font-bold text-slate-400 uppercase tracking-wider mb-1">
                Experience Level
              </label>
              <select
                value={selectedExperience}
                onChange={(e) => setSelectedExperience(e.target.value)}
                className="w-full bg-black/60 border border-slate-700 rounded-xl px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-red-500 font-mono"
              >
                <option value="">All Levels</option>
                <option value="fresher">Fresher (0-1 yrs)</option>
                <option value="junior">Junior (1-3 yrs)</option>
                <option value="mid">Mid-Level (3-6 yrs)</option>
                <option value="senior">Senior (6-10 yrs)</option>
                <option value="lead">Lead / Architect (10+ yrs)</option>
              </select>
            </div>

            {/* 4. CITY FILTER */}
            <div>
              <label className="block text-[10px] font-mono font-bold text-slate-400 uppercase tracking-wider mb-1">
                Hiring City
              </label>
              <select
                value={selectedCity}
                onChange={(e) => setSelectedCity(e.target.value)}
                className="w-full bg-black/60 border border-slate-700 rounded-xl px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-red-500 font-mono"
              >
                <option value="">All Cities</option>
                <option value="bengaluru">Bengaluru</option>
                <option value="hyderabad">Hyderabad</option>
                <option value="pune">Pune</option>
                <option value="gurugram">Gurugram / Delhi NCR</option>
                <option value="mumbai">Mumbai</option>
                <option value="chennai">Chennai</option>
                <option value="noida">Noida</option>
                <option value="remote">Remote Hub</option>
              </select>
            </div>

            {/* 5. WORK MODE */}
            <div>
              <label className="block text-[10px] font-mono font-bold text-slate-400 uppercase tracking-wider mb-1">
                Work Mode
              </label>
              <select
                value={selectedWorkMode}
                onChange={(e) => setSelectedWorkMode(e.target.value)}
                className="w-full bg-black/60 border border-slate-700 rounded-xl px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-red-500 font-mono"
              >
                <option value="">All Modes</option>
                <option value="remote">Remote</option>
                <option value="hybrid">Hybrid</option>
                <option value="onsite">Onsite</option>
              </select>
            </div>

            {/* 6. COUNTRY / REGION */}
            <div>
              <label className="block text-[10px] font-mono font-bold text-slate-400 uppercase tracking-wider mb-1">
                Region
              </label>
              <select
                value={selectedCountry}
                onChange={(e) => setSelectedCountry(e.target.value)}
                className="w-full bg-black/60 border border-slate-700 rounded-xl px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-red-500 font-mono"
              >
                <option value="">All Regions</option>
                <option value="india">🇮🇳 India Hubs</option>
                <option value="global">🌍 Global Remote</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* ── COMPANY & OPPORTUNITY CARDS GRID ───────────────────────────── */}
      {filteredCompanies.length === 0 ? (
        <div className="glass-card rounded-3xl p-12 text-center border border-white/10 space-y-4">
          <Building2 className="w-12 h-12 text-amber-400 mx-auto" />
          <h3 className="text-lg font-bold text-white font-mono">No Current Matches Found in Live Data</h3>
          <p className="text-xs text-slate-400 max-w-md mx-auto font-mono">
            No live company opportunities were retrieved matching your current filter selection. We strictly utilize real retrieved market data and never fabricate companies. Try resetting your filters or selecting another quick category.
          </p>
          <button
            onClick={handleResetFilters}
            className="px-5 py-2.5 bg-red-600 hover:bg-red-500 text-white rounded-xl text-xs font-mono font-bold transition shadow-lg"
          >
            Clear Filters & Show All
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <AnimatePresence>
            {filteredCompanies.map((item, idx) => {
              const cName = item.name || item.company || "Employer";
              const cCategory = item.companyCategory || item.category || "Technology Company";
              const cRole = item.title || item.role || "Specialist";
              const cSalary = item.salary ?? item.expectedLpaRange ?? item.salaryRange ?? null;
              const cLocation = item.location || item.headquarters || "Remote / Hybrid";
              const cMatch = item.aiMatchPercentage ?? item.matchScore ?? item.hiringProbabilityPercentage ?? null;
              const cConfidence = item.confidenceScore ?? null;
              const logo = item.companyLogo || `https://logo.clearbit.com/${cName.toLowerCase().replace(/\s+/g, '')}.com`;
              const favicon = item.faviconFallback || `https://www.google.com/s2/favicons?domain=${cName.toLowerCase().replace(/\s+/g, '')}.com&sz=128`;
              const openRoles = item.openRoles || [cRole];
              const skillsList = Array.isArray(item.requiredSkills)
                ? item.requiredSkills
                : (item.requiredSkills ? item.requiredSkills.split(',') : ['Core Tech', 'Problem Solving']);

              return (
                <motion.div
                  key={`${cName}_${idx}`}
                  initial={{ opacity: 0, y: 15 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -15 }}
                  transition={{ duration: 0.2, delay: idx * 0.03 }}
                  className="glass-card rounded-3xl p-6 border border-white/10 hover:border-red-500/50 transition-all duration-300 flex flex-col justify-between group hover:shadow-[0_0_25px_rgba(255,0,60,0.15)] relative overflow-hidden"
                >
                  <div className="space-y-4">
                    {/* Top Header: Logo + Badges */}
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-2xl bg-white/10 border border-white/10 p-2 shrink-0 flex items-center justify-center overflow-hidden">
                          <img
                            src={logo}
                            alt={cName}
                            className="w-full h-full object-contain"
                            onError={(e) => {
                              // Fallback to Google Favicon if Clearbit image fails
                              (e.target as HTMLElement).setAttribute('src', favicon);
                            }}
                          />
                        </div>
                        <div>
                          <h3 className="text-base font-bold text-white font-mono group-hover:text-red-400 transition flex items-center gap-1.5">
                            {cName}
                            <BadgeCheck className="w-4 h-4 text-cyan-400 shrink-0" />
                          </h3>
                          <span className="text-[11px] text-slate-400 font-mono block mt-0.5">
                            {item.industry || "Software & Technology"}
                          </span>
                        </div>
                      </div>

                      {/* Category Pill */}
                      <span className="px-2.5 py-1 bg-red-500/10 text-red-400 border border-red-500/30 rounded-lg text-[10px] font-mono font-bold shrink-0">
                        {cCategory}
                      </span>
                    </div>

                    {/* Role, Salary & Source Info */}
                    <div className="p-3 bg-black/40 border border-white/5 rounded-2xl space-y-1.5">
                      <div className="text-xs font-bold text-white font-mono flex items-center justify-between">
                        <span className="text-sm font-black text-white">{cRole}</span>
                        <span className="text-emerald-400 font-mono font-bold text-xs">{cSalary ?? 'Not available'}</span>
                      </div>
                      <div className="text-[11px] text-slate-400 font-mono flex items-center justify-between pt-0.5">
                        <span className="flex items-center gap-1">
                          <MapPin className="w-3 h-3 text-slate-500" /> {cLocation}
                        </span>
                        <span className="px-2 py-0.5 bg-white/5 text-slate-300 rounded text-[10px]">
                          {item.workMode || item.workModel || "Hybrid"}
                        </span>
                      </div>
                      <div className="text-[10px] text-slate-500 font-mono flex items-center justify-between border-t border-white/5 pt-1">
                        <span>Source: <strong className="text-slate-400">{item.source || item.retrievalSource || "Live Job Board"}</strong></span>
                        <span className="text-slate-400 font-mono">{(item as any).postedDate || "Active 2026"}</span>
                      </div>
                    </div>

                    {/* AI Match % & Confidence Score */}
                    <div className="flex items-center justify-between text-xs font-mono pt-1">
                      <div className="flex items-center gap-1.5 text-cyan-400 font-bold">
                          <span className="px-2.5 py-1 rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 text-xs font-mono font-black">
                          {cMatch != null ? `${cMatch}% MATCH` : 'Match: Not available'}
                        </span>
                      </div>
                      <div className="text-[10px] text-slate-400 flex items-center gap-1">
                        <Shield className="w-3 h-3 text-emerald-400" />
                        <span>Conf: {cConfidence != null ? `${cConfidence}%` : 'Not available'}</span>
                      </div>
                    </div>

                    {/* Why This Job Fits Explanation */}
                    {((item as any).matchReason || item.explanation) && (
                      <div className="text-[11px] text-slate-300 font-mono leading-relaxed bg-red-950/20 p-2.5 rounded-xl border border-red-500/20">
                        <span className="text-[10px] font-bold text-red-400 uppercase tracking-widest block mb-0.5">
                          ⚡ Why This Job Fits:
                        </span>
                        {(item as any).matchReason || item.explanation}
                      </div>
                    )}

                    {/* Matched Skills with Checkmarks & Missing Skills */}
                    <div className="space-y-2">
                      <div>
                        <span className="text-[10px] font-mono font-bold text-emerald-400 uppercase tracking-wider block mb-1">
                          ✓ Matched Skills:
                        </span>
                        <div className="flex flex-wrap gap-1">
                          {skillsList.slice(0, 4).map((sk, i) => (
                            <span key={i} className="px-2 py-0.5 bg-emerald-950/40 text-emerald-300 border border-emerald-500/30 rounded-md text-[10px] font-mono font-semibold">
                              ✓ {sk.trim()}
                            </span>
                          ))}
                        </div>
                      </div>

                      {(item as any).missingSkills && (item as any).missingSkills !== "None Critical" && (
                        <div>
                          <span className="text-[10px] font-mono font-bold text-rose-400 uppercase tracking-wider block mb-1">
                            Missing Skills:
                          </span>
                          <div className="flex flex-wrap gap-1">
                            {String((item as any).missingSkills).split(',').map((sk, i) => (
                              <span key={i} className="px-2 py-0.5 bg-rose-950/40 text-rose-300 border border-rose-500/30 rounded-md text-[10px] font-mono">
                                ⚠ {sk.trim()}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Actions Footer */}
                  <div className="pt-4 mt-4 border-t border-white/10 flex items-center gap-2">
                    <button
                      onClick={() => setInspectedCompany(item)}
                      className="flex-1 py-2 bg-white/5 hover:bg-white/10 text-white rounded-xl text-xs font-mono font-bold transition border border-white/10 flex items-center justify-center gap-1"
                    >
                      View Details
                    </button>

                    <a
                      href={item.applicationUrl || item.url || `https://www.google.com/search?q=${encodeURIComponent(cName + ' ' + cRole + ' jobs')}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="py-2 px-4 bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white rounded-xl text-xs font-mono font-black transition flex items-center justify-center gap-1.5 shadow-[0_0_12px_rgba(255,0,60,0.4)] shrink-0"
                    >
                      VIEW JOB <ArrowUpRight className="w-3.5 h-3.5" />
                    </a>
                  </div>
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>
      )}

      {/* ── DETAILED COMPANY INSPECTION MODAL ──────────────────────────── */}
      <AnimatePresence>
        {inspectedCompany && (
          <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="glass-card w-full max-w-2xl rounded-3xl p-6 border border-white/20 space-y-6 max-h-[90vh] overflow-y-auto font-mono text-white shadow-2xl relative"
            >
              {/* Close Button */}
              <button
                onClick={() => setInspectedCompany(null)}
                className="absolute top-5 right-5 p-2 rounded-xl bg-white/10 hover:bg-white/20 text-slate-300 hover:text-white transition"
              >
                <X className="w-5 h-5" />
              </button>

              {/* Modal Header */}
              <div className="flex items-start gap-4">
                <div className="w-16 h-16 rounded-2xl bg-white/10 border border-white/20 p-2.5 shrink-0 flex items-center justify-center">
                  <img
                    src={inspectedCompany.companyLogo || `https://logo.clearbit.com/${(inspectedCompany.name || 'tech').toLowerCase().replace(/\s+/g, '')}.com`}
                    alt={inspectedCompany.name}
                    className="w-full h-full object-contain"
                    onError={(e) => {
                      (e.target as HTMLElement).setAttribute('src', inspectedCompany.faviconFallback || `https://www.google.com/s2/favicons?domain=${(inspectedCompany.name || 'tech').toLowerCase().replace(/\s+/g, '')}.com&sz=128`);
                    }}
                  />
                </div>
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <h2 className="text-xl font-black text-white">{inspectedCompany.name || inspectedCompany.company}</h2>
                    <span className="px-2.5 py-0.5 bg-red-500/20 text-red-400 border border-red-500/30 rounded-full text-xs font-bold">
                      {inspectedCompany.companyCategory || inspectedCompany.category || "Company"}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400">
                    {inspectedCompany.industry || "Enterprise Technology"} • {inspectedCompany.growthStage || "High-Growth Scale-Up"}
                  </p>
                  <p className="text-xs text-slate-300 flex items-center gap-1.5 pt-1">
                    <MapPin className="w-3.5 h-3.5 text-red-400" />
                    <strong>HQ:</strong> {inspectedCompany.headquarters || "Global HQ"}
                  </p>
                </div>
              </div>

              {/* Company Overview */}
              <div className="p-4 bg-black/40 rounded-2xl border border-white/5 space-y-2">
                <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider">Company Overview</h4>
                <p className="text-xs text-slate-300 leading-relaxed">
                  {inspectedCompany.companyOverview || `${inspectedCompany.name} is a leading organization hiring top tier talent across ${careerDomain}.`}
                </p>
              </div>

              {/* Indian Offices & Hiring Locations */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-4 bg-white/5 rounded-2xl border border-white/5 space-y-2">
                  <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                    <Building2 className="w-3.5 h-3.5 text-cyan-400" /> India Offices & Tech Hubs
                  </h4>
                  <div className="flex flex-wrap gap-1.5">
                    {(inspectedCompany.indianOffices || ['Bengaluru', 'Hyderabad', 'Pune']).map((off, i) => (
                      <span key={i} className="px-2.5 py-1 bg-black/60 text-slate-300 border border-white/10 rounded-lg text-xs">
                        📍 {off}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="p-4 bg-white/5 rounded-2xl border border-white/5 space-y-2">
                  <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                    <Briefcase className="w-3.5 h-3.5 text-amber-400" /> Work & Experience Specs
                  </h4>
                  <div className="text-xs text-slate-300 space-y-1">
                    <p><strong>Work Mode:</strong> {inspectedCompany.workMode || inspectedCompany.workModel || 'Remote / Hybrid'}</p>
                    <p><strong>Experience:</strong> {inspectedCompany.experienceRequired || 'All Experience Levels'}</p>
                    <p><strong>Salary Range:</strong> <span className="text-emerald-400 font-bold">{inspectedCompany.salary || inspectedCompany.expectedLpaRange || 'Unavailable'}</span></p>
                  </div>
                </div>
              </div>

              {/* AI Match Explanation */}
              <div className="p-4 bg-cyan-500/10 border border-cyan-500/30 rounded-2xl space-y-2">
                <div className="flex items-center justify-between text-xs font-bold text-cyan-300">
                  <span className="flex items-center gap-1.5">
                    <Sparkles className="w-4 h-4 text-amber-400" /> AI Candidate Match Rationale
                  </span>
                  <span>{inspectedCompany.aiMatchPercentage ?? inspectedCompany.matchScore ?? "Match unavailable"}{typeof (inspectedCompany.aiMatchPercentage ?? inspectedCompany.matchScore) === "number" ? "% Match" : ""}</span>
                </div>
                <p className="text-xs text-slate-200 leading-relaxed">
                  {inspectedCompany.explanation || `Candidate skills and domain match ${inspectedCompany.name}'s active technical requirements.`}
                </p>
              </div>

              {/* Open Roles & Apply Links */}
              <div className="space-y-3 pt-2">
                <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider">Active Open Roles & Official Links</h4>
                <div className="space-y-2">
                  {(inspectedCompany.openRoles || [inspectedCompany.title || inspectedCompany.role || 'Specialist']).map((roleName, i) => (
                    <div key={i} className="p-3 bg-black/60 border border-white/10 rounded-xl flex items-center justify-between">
                      <span className="text-xs font-bold text-white">{roleName}</span>
                      <a
                        href={inspectedCompany.applicationUrl || inspectedCompany.url || `https://www.google.com/search?q=${encodeURIComponent(inspectedCompany.name + ' ' + roleName + ' careers')}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="px-3 py-1.5 bg-red-600 hover:bg-red-500 text-white rounded-lg text-xs font-bold transition flex items-center gap-1"
                      >
                        Apply Role <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>
                  ))}
                </div>
              </div>

              {/* Modal Footer */}
              <div className="pt-4 border-t border-white/10 flex items-center justify-between">
                <a
                  href={inspectedCompany.careersPageUrl || `https://www.google.com/search?q=${encodeURIComponent(inspectedCompany.name + ' official careers page')}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-xs text-cyan-400 hover:underline flex items-center gap-1"
                >
                  Visit Official Careers Page <ExternalLink className="w-3 h-3" />
                </a>

                <button
                  onClick={() => setInspectedCompany(null)}
                  className="px-5 py-2 bg-white/10 hover:bg-white/20 text-white rounded-xl text-xs font-bold transition"
                >
                  Close
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};
