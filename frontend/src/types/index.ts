export type UserRole = 'STUDENT' | 'RECRUITER' | 'ADMIN';

export interface User {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  profilePhoto?: string;
  emailVerified?: boolean;
}

export interface UserProfile {
  phone?: string;
  githubUrl?: string;
  linkedinUrl?: string;
  portfolioUrl?: string;
  targetCompany?: string;
  dreamRole?: string;
  experienceYears?: number;
  college?: string;
  degree?: string;
  cgpa?: number;
  bio?: string;
}

export interface ParsedResumeData {
  name: string;
  email: string;
  phone: string;
  linkedin: string;
  github: string;
  skills: string[];
  education: Array<{ degree: string; college: string; cgpa: string; years: string }>;
  projects: Array<{ title: string; tech: string; description: string }>;
  experience: Array<{ company: string; role: string; duration: string; description: string }>;
}

export interface ScoreExplanationDetails {
  score: number;
  confidence: string;
  explanation: string;
  citations?: string[];
  keywordOptimizationScore?: number;
  formattingScore?: number;
  sectionCompletenessScore?: number;
  achievementScore?: number;
}

export interface LiveJobPosting {
  name: string;
  title: string;
  location: string;
  salary: string;
  url: string;
  source: string;
  requiredSkills?: string;
}

export interface RecommendedCompany {
  name: string;
  tier?: string;
  category: string;
  companyCategory?: string;
  role?: string;
  title?: string;
  expectedLpaRange?: string;
  salary?: string;
  salaryRange?: string;
  hiringProbabilityPercentage?: number;
  matchScore?: number;
  aiMatchPercentage?: number;
  confidenceScore?: number;
  requiredSkills?: string[] | string;
  skillMatchPercentage?: number;
  explanation?: string;
  companyLogo?: string;
  faviconFallback?: string;
  industry?: string;
  growthStage?: string;
  headquarters?: string;
  indianOffices?: string[];
  hiringLocations?: string[];
  openRoles?: string[];
  workMode?: string;
  workModel?: string;
  experienceRequired?: string;
  companySize?: string;
  companyOverview?: string;
  careersPageUrl?: string;
  applicationUrl?: string;
  location?: string;
  url?: string;
  retrievalSource?: string;
}

export interface HiringLocationHub {
  city: string;
  country: string;
  isIndia: boolean;
  latitude: number;
  longitude: number;
  demandLevel: string;
  estimatedSalaryRange: string;
  remoteOpportunitiesPercentage: number;
  costOfLivingIndicator: string;
  topIndustries: string[];
  topHiringCompanies: string[];
  trendingSkills: string[];
  visaFriendliness: string;
  whyLocationSuitsCandidate: string;
}

export interface SwotAnalysis {
  strengths: string[];
  weaknesses: string[];
  opportunities: string[];
  improvements: string[];
  missingSkills: string[];
  resumeGaps: string[];
}

export interface LearningRoadmapStage {
  stage: string;
  priority: string;
  topic: string;
  learningTime: string;
  recommendedCertifications: string[];
  freeResources: string[];
  paidResources: string[];
  expectedCareerImpact: string;
}

export interface BulletPointRewrite {
  original: string;
  aiRewritten: string;
  impactScore: number;
  reasoning: string;
}

export interface TechnicalInterviewQuestion {
  question: string;
  contextFromResume?: string;
  modelAnswer: string;
}

export interface BehavioralInterviewQuestion {
  question: string;
  starAnswer: string;
}

export interface AiAnalysisResult {
  name: string;
  email: string;
  phone: string;
  linkedin: string;
  github: string;
  portfolio: string;
  role: string;
  careerDomain: string;
  careerLevel: string;
  experience: string;
  education: string;
  cgpa: string;
  confidenceScore: number;
  profileStrength: number;
  professionalSummary: string;
  strategicForecast: string;
  aiModelUsed?: string;
  atsScore: number;
  atsScoreText: string;
  atsScoreDetails?: ScoreExplanationDetails;
  resumeQualityScoreDetails?: ScoreExplanationDetails;
  technicalSkillsScoreDetails?: ScoreExplanationDetails;
  communicationScoreDetails?: ScoreExplanationDetails;
  projectQualityScoreDetails?: ScoreExplanationDetails;
  portfolioReadinessScoreDetails?: ScoreExplanationDetails;
  recruiterReadinessScoreDetails?: ScoreExplanationDetails;
  careerReadinessScoreDetails?: ScoreExplanationDetails;
  topSkills: string[];
  softSkills: string[];
  programmingLanguages: string[];
  toolsAndTechnologies: string[];
  projects: string[];
  internships: string[];
  achievements: string[];
  certifications: string[];
  bestMatchingJobRoles: Array<{
    title: string;
    matchPercentage: number;
    hiringConfidence: string;
    explanation: string;
    supportedSkills: string[];
    supportedProjects: string[];
    educationFit: string;
  }>;
  swot: SwotAnalysis;
  bestHiringLocations: HiringLocationHub[];
  recommendedCompanies: RecommendedCompany[];
  retrievedJobOpportunities?: LiveJobPosting[];
  marketIntelligenceNotice?: string;
  skillIntelligence?: {
    detectedSkills: string[];
    strongestSkills: string[];
    missingSkills: string[];
    technicalCompetencyChart: Array<{ skill: string; score: number; marketDemand: string }>;
    softSkillsAnalysis: Array<{ skill: string; score: number; explanation: string }>;
    aiLearningRoadmap: LearningRoadmapStage[];
  };
  resumeImprovement?: {
    keywordOptimizationSuggestions: string[];
    weakBulletPoints: BulletPointRewrite[];
    recruiterStyleFeedback?: string;
  };
  interviewPreparation?: {
    technicalQuestions: TechnicalInterviewQuestion[];
    hrQuestions: Array<{ question: string; modelAnswer: string }>;
    projectDiscussionQuestions?: Array<{ question: string; modelAnswer: string }>;
    behavioralQuestions: BehavioralInterviewQuestion[];
  };
  nextBestActions: string[];
}
