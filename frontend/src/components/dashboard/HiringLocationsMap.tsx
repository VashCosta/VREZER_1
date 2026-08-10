import React, { useState } from 'react';
import { MapPin, Globe, Building, DollarSign, Briefcase, Sparkles, Navigation, ShieldCheck } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

export interface LocationItem {
  city: string;
  country: string;
  isIndia?: boolean;
  latitude?: number;
  longitude?: number;
  demandLevel?: string; // Extreme, High, Moderate
  estimatedSalaryRange?: string;
  remoteOpportunitiesPercentage?: number;
  costOfLivingIndicator?: string; // Low, Moderate, High, Very High
  topIndustries?: string[];
  topHiringCompanies?: string[];
  trendingSkills?: string[];
  visaFriendliness?: string;
  whyLocationSuitsCandidate?: string;
}

interface HiringLocationsMapProps {
  locations?: LocationItem[];
  careerDomain?: string;
}

export const HiringLocationsMap: React.FC<HiringLocationsMapProps> = ({ locations = [], careerDomain = "Software Systems" }) => {
  const [filter, setFilter] = useState<'all' | 'india' | 'abroad'>('all');
  const [selectedLocation, setSelectedLocation] = useState<LocationItem | null>(locations[0] || null);

  const activeLocations = (locations || []).filter(loc => {
    if (filter === 'india') return loc.isIndia;
    if (filter === 'abroad') return !loc.isIndia;
    return true;
  });

  const activeSelected = selectedLocation || activeLocations[0] || null;

  if (!locations || locations.length === 0) {
    return (
      <div className="glass-card rounded-3xl p-8 border border-white/10 relative overflow-hidden text-center space-y-4">
        <div className="flex items-center justify-center gap-2 text-xs font-mono text-red-400 font-bold uppercase tracking-widest">
          <Globe className="w-4 h-4" /> Global Market Intelligence
        </div>
        <h2 className="text-xl font-black text-white tracking-tight">
          Hiring Locations for <span className="gradient-text">{careerDomain}</span>
        </h2>
        <p className="text-xs text-slate-400 max-w-md mx-auto leading-relaxed">
          No verified hiring location data available from live market retrieval for this candidate's specific stack.
          Locations are populated only when live job boards provide verified geographic postings.
        </p>
      </div>
    );
  }

  return (
    <div className="glass-card rounded-3xl p-6 border border-white/10 relative overflow-hidden space-y-6">
      {/* Header & Filter Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-white/10 pb-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-red-400 font-bold uppercase tracking-widest">
            <Globe className="w-4 h-4" /> Global Market Intelligence
          </div>
          <h2 className="text-xl font-black text-white tracking-tight mt-0.5">
            Best Hiring Locations for <span className="gradient-text">{careerDomain}</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Dynamic location benchmarks based on your skills, experience, and 2026 industry demand.
          </p>
        </div>

        {/* Filter Tabs */}
        <div className="flex items-center bg-black/60 border border-white/10 rounded-xl p-1 shrink-0 font-mono text-xs">
          <button
            onClick={() => setFilter('all')}
            className={`px-3 py-1.5 rounded-lg transition ${filter === 'all' ? 'bg-red-600 text-white font-bold' : 'text-slate-400 hover:text-white'}`}
          >
            All Locations
          </button>
          <button
            onClick={() => setFilter('india')}
            className={`px-3 py-1.5 rounded-lg transition ${filter === 'india' ? 'bg-red-600 text-white font-bold' : 'text-slate-400 hover:text-white'}`}
          >
            🇮🇳 India Hubs
          </button>
          <button
            onClick={() => setFilter('abroad')}
            className={`px-3 py-1.5 rounded-lg transition ${filter === 'abroad' ? 'bg-red-600 text-white font-bold' : 'text-slate-400 hover:text-white'}`}
          >
            🌐 Abroad Hubs
          </button>
        </div>
      </div>

      {/* Grid Layout: Interactive Map Canvas + Location Details */}
      <div className="grid lg:grid-cols-12 gap-6">
        
        {/* Visual Map Selector Canvas */}
        <div className="lg:col-span-7 bg-[#040407] rounded-2xl border border-red-500/20 p-5 relative min-h-[340px] flex flex-col justify-between cyber-grid-pattern overflow-hidden">
          <div className="flex justify-between items-center text-[10px] font-mono text-slate-400 uppercase tracking-widest z-10">
            <span className="flex items-center gap-1.5 text-red-400 font-bold">
              <Navigation className="w-3.5 h-3.5 animate-pulse" /> Live Market Grid
            </span>
            <span>{activeLocations.length} Targeted Cities</span>
          </div>

          {/* Location Pins Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 my-4 z-10">
            {activeLocations.map((loc, idx) => {
              const isSelected = activeSelected.city === loc.city;
              return (
                <motion.div
                  key={idx}
                  whileHover={{ scale: 1.02 }}
                  onClick={() => setSelectedLocation(loc)}
                  className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-gradient-to-r from-red-950/70 to-black border-red-500 shadow-[0_0_20px_rgba(255,0,60,0.3)]'
                      : 'bg-black/40 border-white/10 hover:border-red-500/40 hover:bg-slate-900/40'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center gap-2">
                      <MapPin className={`w-4 h-4 ${isSelected ? 'text-red-400 animate-bounce' : 'text-slate-400'}`} />
                      <span className="font-bold text-sm text-white">{loc.city}</span>
                    </div>
                    <span className="px-2 py-0.5 text-[9px] font-mono font-bold rounded-full bg-red-500/20 text-red-300 border border-red-500/30">
                      {loc.demandLevel || 'High'}
                    </span>
                  </div>

                  <div className="text-xs font-mono font-bold text-slate-300 mt-2 flex items-center justify-between">
                    <span>{loc.estimatedSalaryRange || 'Not available'}</span>
                    <span className="text-[10px] text-slate-400 font-normal">{loc.isIndia ? '🇮🇳 India' : '🌐 Abroad'}</span>
                  </div>
                </motion.div>
              );
            })}
          </div>

          <div className="text-[10px] font-mono text-slate-500 z-10 flex items-center justify-between border-t border-white/5 pt-2">
            <span>Click any location to inspect demand, companies, and visa friendliness</span>
            <span className="text-red-400 font-bold">2026 Market Calibrated</span>
          </div>
        </div>

        {/* Location Details Panel */}
        <div className="lg:col-span-5 bg-gradient-to-b from-[#120610] to-[#060307] rounded-2xl border border-white/10 p-5 space-y-4 flex flex-col justify-between">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeSelected.city}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.2 }}
              className="space-y-4"
            >
              <div className="border-b border-white/10 pb-3">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-mono text-red-400 font-bold uppercase tracking-widest">
                    RECOMMENDED HIRING HUB
                  </span>
                  <span className="px-2.5 py-0.5 text-[10px] font-mono font-bold rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    {activeSelected.remoteOpportunitiesPercentage}% Remote Jobs
                  </span>
                </div>
                <h3 className="text-2xl font-black text-white mt-1">{activeSelected.city}</h3>
                <p className="text-xs text-slate-400">{activeSelected.country}</p>
              </div>

              {/* Key Metrics */}
              <div className="grid grid-cols-2 gap-3 text-xs font-mono">
                <div className="p-3 rounded-xl bg-black/40 border border-white/5">
                  <span className="text-slate-400 text-[10px] block">ESTIMATED COMPENSATION</span>
                  <strong className="text-white text-sm font-black mt-0.5 block">{activeSelected.estimatedSalaryRange || 'Not available'}</strong>
                </div>
                <div className="p-3 rounded-xl bg-black/40 border border-white/5">
                  <span className="text-slate-400 text-[10px] block">COST OF LIVING</span>
                  <strong className="text-amber-400 text-sm font-black mt-0.5 block">{activeSelected.costOfLivingIndicator}</strong>
                </div>
              </div>

              {/* Why Location Suits Candidate */}
              <div className="p-3 rounded-xl bg-red-950/20 border border-red-500/20">
                <span className="text-[10px] font-bold text-red-400 uppercase tracking-widest flex items-center gap-1 mb-1">
                  <Sparkles className="w-3 h-3" /> Why It Suits You
                </span>
                <p className="text-xs text-slate-300 leading-relaxed">
                  {activeSelected.whyLocationSuitsCandidate}
                </p>
              </div>

              {/* Hiring Companies & Industries */}
              <div className="space-y-2 text-xs">
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">Top Hiring Companies:</span>
                  <div className="flex flex-wrap gap-1.5">
                    {activeSelected.topHiringCompanies?.map((co, i) => (
                      <span key={i} className="px-2.5 py-1 rounded-md bg-white/5 text-slate-200 border border-white/10 text-[11px]">
                        {co}
                      </span>
                    ))}
                  </div>
                </div>

                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">Visa & Sponsorship:</span>
                  <span className="text-xs font-mono text-emerald-400 font-semibold flex items-center gap-1">
                    <ShieldCheck className="w-4 h-4" /> {activeSelected.visaFriendliness}
                  </span>
                </div>
              </div>
            </motion.div>
          </AnimatePresence>
        </div>

      </div>
    </div>
  );
};
