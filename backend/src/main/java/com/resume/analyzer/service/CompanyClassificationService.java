package com.resume.analyzer.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * CompanyClassificationService — Classifies retrieved companies into 24+ categories,
 * industry sectors, growth stages, headquarters, Indian office hubs, and enriches
 * rich per-company metadata (logo, careers links, salary, open roles, AI fit explanation).
 */
@Service
public class CompanyClassificationService {

    // ── Known Category Databases & Keywords ─────────────────────────────────

    private static final Map<String, String[]> CATEGORY_RULES = new LinkedHashMap<>();

    static {
        CATEGORY_RULES.put("Indian MNC", new String[]{
                "tcs", "tata consultancy", "infosys", "wipro", "hcl", "hcltech", "tech mahindra",
                "ltimindtree", "mindtree", "l&t", "larsen", "reliance", "jio", "tata motors",
                "tata steel", "mahindra", "godrej", "bharti airtel", "airtel", "titan", "asian paints"
        });

        CATEGORY_RULES.put("Global MNC", new String[]{
                "google", "alphabet", "microsoft", "amazon", "meta", "facebook", "apple", "ibm",
                "accenture", "deloitte", "pwc", "ey", "kpmg", "mckinsey", "bain", "bcg", "oracle",
                "cisco", "intel", "sap", "salesforce", "adobe", "nvidia", "qualcomm", "samsung",
                "sony", "siemens", "bosch", "general electric", "ge", "capgemini", "cognizant"
        });

        CATEGORY_RULES.put("Indian IT Services", new String[]{
                "tcs", "infosys", "wipro", "hcl", "hcltech", "tech mahindra", "ltimindtree",
                "cognizant", "mphasis", "persistent", "coforge", "birlasoft", "hexaware", "cyient",
                "zensar", "kpit", "tata elxsi"
        });

        CATEGORY_RULES.put("Product-Based Company", new String[]{
                "adobe", "atlassian", "microsoft", "google", "apple", "meta", "uber", "airbnb",
                "intuit", "linkedin", "figma", "canva", "stripe", "dropbox", "slack", "zoom",
                "booking", "expedia", "hubspot", "snowflake", "datadog", "postman"
        });

        CATEGORY_RULES.put("SaaS Company", new String[]{
                "salesforce", "hubspot", "snowflake", "datadog", "freshworks", "postman", "zoho",
                "notion", "figma", "asana", "monday.com", "zendesk", "intercom", "toast", "confluent",
                "servicenow", "workday", "box", "twilio", "sendgrid", "klaviyo"
        });

        CATEGORY_RULES.put("AI & Machine Learning Company", new String[]{
                "openai", "anthropic", "nvidia", "databricks", "midjourney", "hugging face",
                "scale ai", "cohere", "mistral", "glean", "c3.ai", "jasper", "copy.ai",
                "runway", "synthesia", "element ai", "abacus.ai", "deepmind"
        });

        CATEGORY_RULES.put("Cloud & DevOps Company", new String[]{
                "aws", "amazon web services", "hashicorp", "docker", "cloudflare", "datadog",
                "red hat", "digitalocean", "elastic", "gitlab", "kubernetes", "nutanix",
                "fastly", "akamai", "mongodb", "redis", "dynatrace", "new relic"
        });

        CATEGORY_RULES.put("Cybersecurity Company", new String[]{
                "palo alto", "crowdstrike", "zscaler", "cloudflare", "fortinet", "sentinelone",
                "okta", "wiz", "snyk", "darktrace", "checkpoint", "proofpoint", "cyberark",
                "tenable", "rapid7", "qualys", "trend micro"
        });

        CATEGORY_RULES.put("FinTech Company", new String[]{
                "stripe", "razorpay", "cred", "phonepe", "paytm", "block", "square", "plaid",
                "robinhood", "revolut", "adyen", "brex", "pine labs", "groww", "zerodha",
                "wise", "affirm", "klarna", "chime", "bill.com", "ramp"
        });

        CATEGORY_RULES.put("HealthTech Company", new String[]{
                "pharmeasy", "practo", "cure.fit", "cult.fit", "veeva", "teladoc", "oscar health",
                "innovaccer", "netmeds", "healthifyme", "1mg", "tata 1mg", "flat iron", "doximity",
                "goodrx", "cerner", "epic systems"
        });

        CATEGORY_RULES.put("EdTech Company", new String[]{
                "coursera", "udemy", "physicswallah", "physics wallah", "unacademy", "eruditus",
                "duolingo", "khan academy", "skillshare", "upgrad", "simplilearn", "lead school",
                "classplus", "quizlet", "chegg", "byju"
        });

        CATEGORY_RULES.put("E-Commerce Company", new String[]{
                "flipkart", "amazon", "meesho", "shopify", "etsy", "ebay", "myntra", "nykaa",
                "bigbasket", "instamart", "blinkit", "zepto", "ajio", "wayfair", "chewy"
        });

        CATEGORY_RULES.put("Unicorn", new String[]{
                "zepto", "swiggy", "zomato", "razorpay", "postman", "ola", "inmobi", "physicswallah",
                "cred", "phonepe", "groww", "zerodha", "meesho", "dream11", "lenskart",
                "pine labs", "mpl", "cars24", "nobroker", "policybazaar", "delhivery"
        });

        CATEGORY_RULES.put("Startup", new String[]{
                "wellfound", "yc", "y combinator", "stealth", "funded", "seed", "series a",
                "series b", "early-stage", "early stage", "scale-up"
        });

        CATEGORY_RULES.put("High-Growth Company", new String[]{
                "zepto", "blinkit", "zomato", "swiggy", "postman", "razorpay", "cred", "groww",
                "datadog", "snowflake", "cloudflare", "figma", "notion", "mistral"
        });

        CATEGORY_RULES.put("Government Organization", new String[]{
                "isro", "drdo", "nic", "c-dac", "cdac", "bel", "bhel", "ntpc", "ongc", "iocl",
                "gail", "hal", "barc", "npci", "rbi", "sebi", "crisil", "c-dot"
        });

        CATEGORY_RULES.put("Consulting Firm", new String[]{
                "mckinsey", "bcg", "bain", "deloitte", "pwc", "ey", "kpmg", "accenture", "mercer",
                "oliver wyman", "booz allen", "alvarez & marsal", "slalom"
        });

        CATEGORY_RULES.put("Manufacturing Company", new String[]{
                "tata motors", "l&t", "larsen & toubro", "siemens", "bosch", "general electric",
                "schneider electric", "caterpillar", "cummins", "abb", "honeywell", "emerson"
        });

        CATEGORY_RULES.put("Telecom Company", new String[]{
                "jio", "reliance jio", "airtel", "bharti airtel", "qualcomm", "cisco", "nokia",
                "ericsson", "vodafone idea", "at&t", "verizon", "t-mobile", "ciena"
        });

        CATEGORY_RULES.put("Automotive Company", new String[]{
                "tesla", "tata motors", "mahindra", "rivian", "hyundai", "bmw", "mercedes-benz",
                "ather energy", "ather", "ola electric", "hero motocorp", "tvsmotors"
        });

        CATEGORY_RULES.put("Semiconductor Company", new String[]{
                "nvidia", "intel", "amd", "qualcomm", "mediatek", "tsmc", "texas instruments",
                "broadcom", "applied materials", "micron", "arm", "analog devices", "nxp"
        });

        CATEGORY_RULES.put("Gaming Company", new String[]{
                "electronic arts", "ea", "ubisoft", "epic games", "dream11", "nazara", "krafton",
                "roblox", "riot games", "unity", "take-two", "zynga", "supercell"
        });

        CATEGORY_RULES.put("Digital Marketing Agency", new String[]{
                "dentsu", "ogilvy", "publicis", "hubspot", "wpp", "omnicom", "ipg", "webfx",
                "schbang", "foxymoron", "social beat", "dentsu webchutney"
        });

        CATEGORY_RULES.put("Media & Content Company", new String[]{
                "netflix", "spotify", "disney", "warner bros", "paramount", "sony", "pocket fm",
                "audible", "zee", "star india", "times internet", "dailyhunt"
        });
    }

    // HQ and India Office Knowledge Map
    private static final Map<String, CompanyInfo> KNOWN_COMPANIES = new HashMap<>();

    static class CompanyInfo {
        String name;
        String category;
        String industry;
        String growthStage;
        String headquarters;
        List<String> indianOffices;
        String domain;
        String overview;
        String careersUrl;

        CompanyInfo(String name, String category, String industry, String growthStage,
                    String headquarters, List<String> indianOffices, String domain,
                    String overview, String careersUrl) {
            this.name = name;
            this.category = category;
            this.industry = industry;
            this.growthStage = growthStage;
            this.headquarters = headquarters;
            this.indianOffices = indianOffices;
            this.domain = domain;
            this.overview = overview;
            this.careersUrl = careersUrl;
        }
    }

    static {
        KNOWN_COMPANIES.put("google", new CompanyInfo(
                "Google", "Global MNC", "Artificial Intelligence & Search", "Public Enterprise",
                "Mountain View, CA, USA", List.of("Bengaluru", "Hyderabad", "Gurugram", "Mumbai"),
                "google.com", "Global technology leader in AI, cloud computing, search, hardware, and software innovation.",
                "https://careers.google.com"
        ));
        KNOWN_COMPANIES.put("microsoft", new CompanyInfo(
                "Microsoft", "Global MNC", "Cloud & Enterprise Software", "Public Enterprise",
                "Redmond, WA, USA", List.of("Bengaluru", "Hyderabad", "Noida", "Mumbai"),
                "microsoft.com", "Empowering individuals and organizations through Azure cloud, AI, developer tools, and productivity software.",
                "https://careers.microsoft.com"
        ));
        KNOWN_COMPANIES.put("amazon", new CompanyInfo(
                "Amazon", "Global MNC", "Cloud Computing (AWS) & E-Commerce", "Public Enterprise",
                "Seattle, WA, USA", List.of("Bengaluru", "Hyderabad", "Chennai", "Gurugram", "Pune"),
                "amazon.com", "Global powerhouse in cloud infrastructure (AWS), e-commerce logistics, AI, and digital streaming.",
                "https://amazon.jobs"
        ));
        KNOWN_COMPANIES.put("tcs", new CompanyInfo(
                "TCS (Tata Consultancy Services)", "Indian MNC", "IT Services & Enterprise Transformation", "Public Enterprise",
                "Mumbai, Maharashtra, India", List.of("Bengaluru", "Hyderabad", "Pune", "Chennai", "Mumbai", "Noida", "Kolkata"),
                "tcs.com", "India's largest IT services and consulting organization driving digital transformation worldwide.",
                "https://www.tcs.com/careers"
        ));
        KNOWN_COMPANIES.put("infosys", new CompanyInfo(
                "Infosys", "Indian MNC", "IT Services & Cloud Consulting", "Public Enterprise",
                "Bengaluru, Karnataka, India", List.of("Bengaluru", "Hyderabad", "Pune", "Chennai", "Mysuru", "Gurugram"),
                "infosys.com", "Global leader in next-generation digital services and consulting across enterprise technology.",
                "https://www.infosys.com/careers"
        ));
        KNOWN_COMPANIES.put("wipro", new CompanyInfo(
                "Wipro", "Indian IT Services", "IT Services & Cybersecurity Solutions", "Public Enterprise",
                "Bengaluru, Karnataka, India", List.of("Bengaluru", "Hyderabad", "Pune", "Chennai", "Gurugram", "Kolkata"),
                "wipro.com", "Leading global information technology, consulting, and business process services corporation.",
                "https://careers.wipro.com"
        ));
        KNOWN_COMPANIES.put("adobe", new CompanyInfo(
                "Adobe", "Product-Based Company", "Creative Software & Digital Media", "Public Enterprise",
                "San Jose, CA, USA", List.of("Bengaluru", "Noida"),
                "adobe.com", "Pioneer in creative tools, digital document experiences, and AI-driven marketing technology.",
                "https://adobe.com/careers"
        ));
        KNOWN_COMPANIES.put("stripe", new CompanyInfo(
                "Stripe", "FinTech Company", "Financial Infrastructure & Payments", "Unicorn / Late Stage",
                "San Francisco, CA, USA", List.of("Bengaluru"),
                "stripe.com", "Financial infrastructure platform building economic tools for modern online businesses.",
                "https://stripe.com/jobs"
        ));
        KNOWN_COMPANIES.put("razorpay", new CompanyInfo(
                "Razorpay", "FinTech Company", "Payments & Neo-Banking", "Unicorn / Late Stage",
                "Bengaluru, Karnataka, India", List.of("Bengaluru", "Mumbai", "Gurugram"),
                "razorpay.com", "India's leading full-stack financial services and payment gateway platform for businesses.",
                "https://razorpay.com/jobs"
        ));
        KNOWN_COMPANIES.put("postman", new CompanyInfo(
                "Postman", "SaaS Company", "API Development & Developer Tools", "Unicorn / Late Stage",
                "San Francisco, CA, USA", List.of("Bengaluru"),
                "postman.com", "World-leading API platform used by over 30 million developers to build and manage APIs.",
                "https://www.postman.com/careers"
        ));
        KNOWN_COMPANIES.put("nvidia", new CompanyInfo(
                "Nvidia", "AI & Machine Learning Company", "GPU Architecture & AI Supercomputing", "Public Enterprise",
                "Santa Clara, CA, USA", List.of("Bengaluru", "Hyderabad", "Pune"),
                "nvidia.com", "World leader in GPU computing, AI hardware accelerators, and CUDA accelerated software.",
                "https://www.nvidia.com/en-us/about-nvidia/careers"
        ));
        KNOWN_COMPANIES.put("swiggy", new CompanyInfo(
                "Swiggy", "Unicorn", "On-Demand Delivery & Tech Logistics", "Public Enterprise",
                "Bengaluru, Karnataka, India", List.of("Bengaluru", "Gurugram", "Mumbai", "Hyderabad"),
                "swiggy.com", "Leading Indian on-demand food, grocery, and quick-commerce tech platform.",
                "https://careers.swiggy.com"
        ));
        KNOWN_COMPANIES.put("zomato", new CompanyInfo(
                "Zomato", "Unicorn", "Food Tech & Quick Commerce (Blinkit)", "Public Enterprise",
                "Gurugram, Haryana, India", List.of("Gurugram", "Bengaluru", "Mumbai"),
                "zomato.com", "India's prominent dining, delivery, and quick commerce technology enterprise.",
                "https://zomato.com/careers"
        ));
        KNOWN_COMPANIES.put("zepto", new CompanyInfo(
                "Zepto", "Unicorn", "Quick Commerce & Hyperlocal Logistics", "Unicorn / Late Stage",
                "Mumbai, Maharashtra, India", List.of("Mumbai", "Bengaluru", "Gurugram"),
                "zepto.com", "India's fastest growing 10-minute quick-commerce delivery platform.",
                "https://www.zeptonow.com/careers"
        ));
    }

    /**
     * Enriches a retrieved company record with canonical category, logo, industry,
     * HQ, Indian offices, open roles, skills, and AI match score.
     */
    public Map<String, Object> enrichCompanyData(Map<String, Object> inputCompany, String candidateDomain, List<String> candidateSkills) {
        Map<String, Object> comp = new LinkedHashMap<>(inputCompany);

        String rawName = String.valueOf(comp.getOrDefault("name", comp.getOrDefault("company", "Tech Company"))).trim();
        String rawRole = String.valueOf(comp.getOrDefault("role", comp.getOrDefault("title", "Role unavailable"))).trim();
        String rawLocation = String.valueOf(comp.getOrDefault("location", "Location unavailable")).trim();
        // Preserve salary when provided by the live job; otherwise mark as undisclosed
        String rawSalary = comp.containsKey("salary") && comp.get("salary") != null && !String.valueOf(comp.get("salary")).isBlank()
            ? String.valueOf(comp.get("salary")).trim()
            : "Salary not disclosed";
        String rawSkills = String.valueOf(comp.getOrDefault("requiredSkills", "")).trim();
        String rawUrl = String.valueOf(comp.getOrDefault("url", comp.getOrDefault("applicationUrl", "")));

        String nameLower = rawName.toLowerCase();

        // 1. Lookup Known Info or Classify Heuristically
        CompanyInfo known = findKnownCompany(nameLower);

        String companyCategory = (known != null) ? known.category : inferCategoryFromNameAndTitle(rawName, rawRole);
        String industry = (known != null) ? known.industry : inferIndustry(rawName, rawRole, candidateDomain);
        String growthStage = (known != null) ? known.growthStage : inferGrowthStage(rawName, companyCategory);
        String headquarters = (known != null) ? known.headquarters : inferHeadquarters(rawName, rawLocation);
        List<String> indianOffices = (known != null) ? known.indianOffices : inferIndianOffices(rawLocation);
        String domain = (known != null) ? known.domain : sanitizeDomain(rawName);
        String overview = (known != null) ? known.overview : generateOverview(rawName, companyCategory, industry);
        String careersUrl = (known != null && known.careersUrl != null && !known.careersUrl.isEmpty()) 
                ? known.careersUrl 
                : "https://www." + domain + "/careers";

        // Logo URL Generation (Clearbit with Google Favicon Fallback)
        String logoUrl = "https://logo.clearbit.com/" + domain;
        String faviconFallback = "https://www.google.com/s2/favicons?domain=" + domain + "&sz=128";

        // Application URL
        String applicationUrl = (!rawUrl.isEmpty()) 
                ? rawUrl 
                : "https://www.google.com/search?q=" + URLEncoderUtil.encode(rawName + " " + rawRole + " careers jobs");

        // Open Roles Array
        List<String> openRoles = parseListOrSingle(comp.get("openRoles"), rawRole);

        // Required Skills Array
        List<String> requiredSkills = parseListOrSingle(comp.get("requiredSkills"), rawSkills);

        // Work Mode Heuristic
        String workMode = inferWorkMode(rawLocation);

        // Experience Level
        String experienceRequired = inferExperience(rawRole);

        // Company Size
        String companySize = inferCompanySize(companyCategory, growthStage);

        // Calculate AI Match % and Confidence
        int matchScore = comp.containsKey("matchScore")
            ? parseInteger(comp.get("matchScore"), calculateMatchScore(candidateSkills, requiredSkills))
            : calculateMatchScore(candidateSkills, requiredSkills);

        int confidenceScore = comp.containsKey("confidenceScore")
            ? parseInteger(comp.get("confidenceScore"), matchScore > 0 ? Math.max(30, Math.min(95, matchScore)) : 0)
            : (matchScore > 0 ? Math.max(30, Math.min(95, matchScore)) : 0);

        String explanation = comp.containsKey("explanation") 
                ? String.valueOf(comp.get("explanation")) 
                : generateExplanation(rawName, rawRole, candidateDomain, candidateSkills, requiredSkills);

        // Populate Standard Output Map
        comp.put("name", rawName);
        comp.put("company", rawName);
        comp.put("companyLogo", logoUrl);
        comp.put("faviconFallback", faviconFallback);
        comp.put("companyCategory", companyCategory);
        comp.put("category", companyCategory);
        comp.put("industry", industry);
        comp.put("growthStage", growthStage);
        comp.put("headquarters", headquarters);
        comp.put("indianOffices", indianOffices);
        comp.put("hiringLocations", parseHiringLocations(rawLocation, indianOffices));
        comp.put("openRoles", openRoles);
        comp.put("title", rawRole);
        comp.put("role", rawRole);
        comp.put("requiredSkills", requiredSkills);
        comp.put("workMode", workMode);
        comp.put("experienceRequired", experienceRequired);
        comp.put("companySize", companySize);
        comp.put("salary", rawSalary);
        comp.put("companyOverview", overview);
        comp.put("careersPageUrl", careersUrl);
        comp.put("applicationUrl", applicationUrl);
        comp.put("url", applicationUrl);
        comp.put("matchScore", matchScore);
        comp.put("aiMatchPercentage", matchScore);
        comp.put("confidenceScore", confidenceScore);
        comp.put("explanation", explanation);

        return comp;
    }

    private CompanyInfo findKnownCompany(String nameLower) {
        for (Map.Entry<String, CompanyInfo> entry : KNOWN_COMPANIES.entrySet()) {
            if (nameLower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String inferCategoryFromNameAndTitle(String companyName, String roleTitle) {
        String combined = (companyName + " " + roleTitle).toLowerCase();

        for (Map.Entry<String, String[]> entry : CATEGORY_RULES.entrySet()) {
            String category = entry.getKey();
            for (String kw : entry.getValue()) {
                if (combined.contains(kw)) {
                    return category;
                }
            }
        }

        if (combined.contains("saas") || combined.contains("cloud")) return "SaaS Company";
        if (combined.contains("ai") || combined.contains("machine learning") || combined.contains("data")) return "AI & Machine Learning Company";
        if (combined.contains("fintech") || combined.contains("pay") || combined.contains("bank")) return "FinTech Company";
        if (combined.contains("health") || combined.contains("bio")) return "HealthTech Company";
        if (combined.contains("edtech") || combined.contains("learn")) return "EdTech Company";

        return "Product-Based Company";
    }

    private String inferIndustry(String companyName, String roleTitle, String candidateDomain) {
        String text = (companyName + " " + roleTitle + " " + (candidateDomain != null ? candidateDomain : "")).toLowerCase();
        if (text.contains("ai") || text.contains("ml") || text.contains("intelligence")) return "Artificial Intelligence & Data";
        if (text.contains("fintech") || text.contains("pay") || text.contains("finance")) return "Financial Technology & Payments";
        if (text.contains("cloud") || text.contains("devops") || text.contains("infrastructure")) return "Cloud & DevOps Infrastructure";
        if (text.contains("cyber") || text.contains("security")) return "Cybersecurity & Information Assurance";
        if (text.contains("health") || text.contains("medical")) return "Healthcare & BioTech";
        if (text.contains("commerce") || text.contains("retail")) return "E-Commerce & Digital Retail";
        if (text.contains("auto") || text.contains("vehicle") || text.contains("motor")) return "Automotive & Electric Vehicles";
        if (text.contains("semiconductor") || text.contains("hardware")) return "Semiconductor & Hardware Engineering";
        if (text.contains("game") || text.contains("gaming")) return "Gaming & Interactive Entertainment";
        if (text.contains("marketing") || text.contains("ad")) return "Digital Marketing & AdTech";

        return "Software Engineering & Enterprise Tech";
    }

    private String inferGrowthStage(String companyName, String category) {
        if ("Global MNC".equals(category) || "Indian MNC".equals(category) || "Government Organization".equals(category)) {
            return "Public Enterprise";
        }
        if ("Unicorn".equals(category)) {
            return "Unicorn / Late Stage";
        }
        if ("Startup".equals(category)) {
            return "Growth Stage (Series A-C)";
        }
        return "High-Growth Scaling Employer";
    }

    private String inferHeadquarters(String companyName, String location) {
        String locLow = location.toLowerCase();
        if (locLow.contains("bengaluru") || locLow.contains("bangalore")) return "Bengaluru, Karnataka, India";
        if (locLow.contains("hyderabad")) return "Hyderabad, Telangana, India";
        if (locLow.contains("pune")) return "Pune, Maharashtra, India";
        if (locLow.contains("mumbai")) return "Mumbai, Maharashtra, India";
        if (locLow.contains("delhi") || locLow.contains("gurugram") || locLow.contains("noida")) return "Gurugram, Haryana, India";
        if (locLow.contains("chennai")) return "Chennai, Tamil Nadu, India";

        return "Global HQ / Remote Hub";
    }

    private List<String> inferIndianOffices(String location) {
        List<String> offices = new ArrayList<>();
        String locLow = location.toLowerCase();
        if (locLow.contains("bengaluru") || locLow.contains("bangalore")) offices.add("Bengaluru");
        if (locLow.contains("hyderabad")) offices.add("Hyderabad");
        if (locLow.contains("pune")) offices.add("Pune");
        if (locLow.contains("delhi") || locLow.contains("gurugram") || locLow.contains("noida")) offices.add("Gurugram");
        if (locLow.contains("mumbai")) offices.add("Mumbai");
        if (locLow.contains("chennai")) offices.add("Chennai");

        if (offices.isEmpty()) {
            offices = List.of("Bengaluru", "Hyderabad", "Pune", "Gurugram");
        }
        return offices;
    }

    private String sanitizeDomain(String companyName) {
        String clean = companyName.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .replace("inc", "")
                .replace("corp", "")
                .replace("ltd", "")
                .replace("technologies", "")
                .replace("services", "")
                .trim();
        return clean.isEmpty() ? "techcompany.com" : clean + ".com";
    }

    private String generateOverview(String companyName, String category, String industry) {
        return companyName + " is a premier " + category + " specializing in " + industry +
                ". Known for cutting-edge engineering practices, scalable infrastructure, and strong employee growth initiatives.";
    }

    private List<String> parseHiringLocations(String locationStr, List<String> defaultIndianOffices) {
        if (locationStr == null || locationStr.isEmpty() || locationStr.equalsIgnoreCase("Remote / Global")) {
            List<String> list = new ArrayList<>(defaultIndianOffices.subList(0, Math.min(2, defaultIndianOffices.size())));
            list.add("Remote");
            return list;
        }
        return List.of(locationStr.split("[,/|]"));
    }

    private String inferWorkMode(String locationStr) {
        String low = locationStr.toLowerCase();
        if (low.contains("remote")) return "Remote";
        if (low.contains("hybrid")) return "Hybrid";
        return "Onsite / Hybrid";
    }

    private String inferExperience(String roleTitle) {
        String low = roleTitle.toLowerCase();
        if (low.contains("fresher") || low.contains("intern") || low.contains("graduate") || low.contains("junior")) {
            return "Fresher / Junior (0-2 yrs)";
        }
        if (low.contains("senior") || low.contains("lead") || low.contains("principal") || low.contains("architect")) {
            return "Senior / Lead (5+ yrs)";
        }
        return "Mid-Level (2-5 yrs)";
    }

    private String inferCompanySize(String category, String growthStage) {
        if (category.contains("MNC") || category.contains("IT Services") || growthStage.contains("Enterprise")) {
            return "Enterprise (5000+ employees)";
        }
        if (category.contains("Unicorn") || category.contains("Product") || category.contains("SaaS")) {
            return "Large (500-5000 employees)";
        }
        return "Mid-Size / Scale-Up (50-500 employees)";
    }

    private int calculateMatchScore(List<String> candidateSkills, List<String> requiredSkills) {
        if (candidateSkills == null || candidateSkills.isEmpty() || requiredSkills == null || requiredSkills.isEmpty()) {
            return 0;
        }

        long matches = candidateSkills.stream()
                .filter(cs -> cs != null && !cs.isBlank())
                .filter(cs -> requiredSkills.stream().anyMatch(rs -> rs != null && !rs.isBlank()
                        && (rs.toLowerCase().contains(cs.toLowerCase()) || cs.toLowerCase().contains(rs.toLowerCase()))))
                .count();

        double ratio = (double) matches / Math.max(1, requiredSkills.size());
        return Math.max(0, Math.min(100, (int) Math.round(ratio * 100.0)));
    }

    private String generateExplanation(String companyName, String roleTitle, String candidateDomain, List<String> candidateSkills, List<String> requiredSkills) {
        String skillText = (candidateSkills != null && !candidateSkills.isEmpty())
                ? String.join(", ", candidateSkills.subList(0, Math.min(3, candidateSkills.size())))
                : "core technical competencies";

        return companyName + " is actively recruiting for " + roleTitle + ". The candidate's background in " +
                (candidateDomain != null ? candidateDomain : "engineering") + " and proficiency in " + skillText + " strongly align with their open job requirements.";
    }

    @SuppressWarnings("unchecked")
    private List<String> parseListOrSingle(Object raw, String fallbackText) {
        if (raw instanceof List) {
            return (List<String>) raw;
        }
        if (raw instanceof String && !((String) raw).isEmpty()) {
            return List.of(((String) raw).split("[,|;]"));
        }
        return List.of(fallbackText.split("[,|;]"));
    }

    /**
     * Builds dynamic Tier 1 (85-100), Tier 2 (70-84), and Tier 3 (55-69) target company trajectory objects 
     * strictly derived from actual matched live jobs.
     */
    public Map<String, Map<String, String>> buildTierTrajectory(List<Map<String, String>> liveJobs, String domain, List<String> candidateSkills, String careerLevel, int atsScore) {
        Map<String, Map<String, String>> trajectory = new LinkedHashMap<>();
        if (liveJobs == null || liveJobs.isEmpty()) return trajectory;

        int tier = 1;
        for (Map<String, String> job : liveJobs) {
            if (tier > 3) break;
            String company = job.getOrDefault("name", job.getOrDefault("company", "")).trim();
            String role = job.getOrDefault("title", "").trim();
            if (company.isEmpty() || role.isEmpty()) continue;

            Map<String, String> card = new LinkedHashMap<>();
            card.put("company", company);
            card.put("role", role);
            card.put("city", job.getOrDefault("location", ""));
            String salary = job.getOrDefault("salary", "").trim();
            card.put("salary", salary.isEmpty() ? "Salary not disclosed" : salary);
            card.put("expectedLpaRange", salary.isEmpty() ? "Salary not disclosed" : salary);
            card.put("url", job.getOrDefault("url", ""));
            card.put("matchScore", job.getOrDefault("matchScore", ""));
            card.put("source", job.getOrDefault("source", "Verified Live API"));
            trajectory.put("tier" + tier, card);
            tier++;
        }
        return trajectory;
    }

    public List<Map<String, Object>> generateSkillTargetedCompanies(List<Map<String, String>> liveJobs, String domain, List<String> candidateSkills, String careerLevel, int atsScore) {
        Map<String, Map<String, Object>> compMap = new LinkedHashMap<>();
        String domainClean = (domain != null && !domain.trim().isEmpty()) ? domain.trim() : "Software Engineering";
        List<String> skillsClean = (candidateSkills != null) ? candidateSkills : List.of();

        // 1. Process Live API Jobs if available
        if (liveJobs != null && !liveJobs.isEmpty()) {
            for (Map<String, String> job : liveJobs) {
                String cName = job.getOrDefault("name", job.getOrDefault("company", "")).trim();
                if (cName.isEmpty() || compMap.containsKey(cName.toLowerCase())) continue;

                Map<String, Object> input = new LinkedHashMap<>();
                input.put("name", cName);
                input.put("title", job.getOrDefault("title", domainClean + " Specialist"));
                input.put("location", job.getOrDefault("location", "India / Global Remote"));
                input.put("salary", job.getOrDefault("salary", "Competitive Market Rate"));
                input.put("url", job.getOrDefault("url", ""));
                input.put("source", job.getOrDefault("source", "Live Market API"));
                input.put("requiredSkills", job.getOrDefault("requiredSkills", ""));

                if (job.containsKey("matchScore")) {
                    input.put("matchScore", job.get("matchScore"));
                } else if (job.containsKey("similarityScore")) {
                    try {
                        input.put("matchScore", (int) Math.round(Double.parseDouble(job.get("similarityScore"))));
                    } catch (Exception ignored) {}
                }

                Map<String, Object> enriched = enrichCompanyData(input, domainClean, skillsClean);
                compMap.put(cName.toLowerCase(), enriched);
                if (compMap.size() >= 12) break;
            }
        }

        // No live jobs means no company recommendations. Never fabricate companies or salaries.\n        return new ArrayList<>(compMap.values());
    }

    /**
     * Domain-targeted company catalog covering Indian MNCs, Indian Tech Companies/Unicorns,
     * Startups, and Global MNCs across 8+ major career domains.
     */
    private List<Map<String, String>> getDomainCuratedCompanies(String domain, List<String> skills, String careerLevel) {
        String dLow = domain.toLowerCase();
        String skillContext = !skills.isEmpty() ? String.join(", ", skills.subList(0, Math.min(4, skills.size()))) : "Domain Competencies";
        List<Map<String, String>> list = new ArrayList<>();

        // Removed curated domain company lists to avoid fabricating recommendations and salaries.
        // Company recommendations must come exclusively from verified live job postings.
        return list;
    }

    public String categorizeCompany(String name) {
        if (name == null) return "Employer";
        String lower = name.toLowerCase();
        if (lower.contains("google") || lower.contains("microsoft") || lower.contains("amazon") || lower.contains("apple") || lower.contains("meta") || lower.contains("netflix") || lower.contains("adobe") || lower.contains("uber")) {
            return "Global Technology Employer";
        } else if (lower.contains("flipkart") || lower.contains("swiggy") || lower.contains("zomato") || lower.contains("razorpay") || lower.contains("atlassian") || lower.contains("stripe") || lower.contains("cred") || lower.contains("postman") || lower.contains("zepto")) {
            return "Product Market Employer";
        } else if (lower.contains("tcs") || lower.contains("infosys") || lower.contains("wipro") || lower.contains("accenture") || lower.contains("cognizant") || lower.contains("hcl") || lower.contains("l&t")) {
            return "Global IT Services Employer";
        }
        return "Enterprise Product Employer";
    }

    public List<Map<String, Object>> generateSkillTargetedCompanies(String domain, List<String> candidateSkills, String careerLevel, int atsScore) {
        return generateSkillTargetedCompanies(null, domain, candidateSkills, careerLevel, atsScore);
    }

    private int parseInteger(Object val, int defaultVal) {
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    static class URLEncoderUtil {
        static String encode(String s) {
            try {
                return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                return s;
            }
        }
    }
}

