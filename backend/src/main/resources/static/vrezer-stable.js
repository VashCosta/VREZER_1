/* VREZER 3.0 — stable single-source frontend
 * GitHub Pages -> Render /extract -> Render /analyze -> one immutable dashboard object.
 * No browser AI provider calls, no embedded provider keys, no mock analysis fallback.
 */
(function () {
  'use strict';
  const $ = id => document.getElementById(id);
  const arr = v => Array.isArray(v) ? v : [];
  const obj = v => v && typeof v === 'object' && !Array.isArray(v) ? v : {};
  const val = (...v) => v.find(x => x !== undefined && x !== null && String(x).trim() !== '') || 'Not available';
  const txt = v => String(v === undefined || v === null ? '' : v);
  const esc = v => txt(v).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#039;');
  const list = v => arr(v).map(x => typeof x === 'object' ? val(x.name,x.title,x.role,x.skill,x.text) : txt(x)).filter(Boolean);
  let file = null, sampleText = '', data = null, charts = {};
  const APP = 'vrezer-single-source-v1';

  function api() {
    if (window.VREZER_API_URL) return String(window.VREZER_API_URL).replace(/\/$/,'');
    const h = location.hostname || '';
    if (h.indexOf('github.io') >= 0 || h.indexOf('vercel.app') >= 0) return 'https://vrezer-backend.onrender.com';
    if (h === 'localhost' || h === '127.0.0.1') return 'http://localhost:9000';
    return '';
  }
  function show(e){ if(e)e.classList.remove('hidden'); }
  function hide(){ Array.prototype.forEach.call(arguments,e=>e&&e.classList.add('hidden')); }
  function put(id,v){ const e=$(id); if(e)e.textContent=val(v); }
  function html(id,s){ const e=$(id); if(e)e.innerHTML=s; }
  function setTicker(s){ put('ticker',s); }

  function loadProgress(p,phase,msg){
    const n=Math.max(0,Math.min(100,Math.round(p)));
    const f=$('prog-fill'), q=$('prog-pct'), ph=$('load-phase'), lm=$('load-msg');
    if(f)f.style.width=n+'%'; if(q)q.textContent='VREZER AI ANALYSIS · '+n+'% COMPLETE';
    if(ph)ph.textContent=phase||'Processing'; if(lm)lm.textContent=msg||'';
  }
  function phase(i){
    ['ps-parse','ps-rag','ps-ai','ps-jobs','ps-render'].forEach(function(id,k){
      const e=$(id); if(!e)return; e.classList.toggle('done',k<i); e.classList.toggle('active',k===i);
    });
  }

  async function fetchJson(url,options,timeout){
    const c=new AbortController(); const t=setTimeout(()=>c.abort(),timeout||300000);
    try{
      const r=await fetch(url,Object.assign({},options,{signal:c.signal}));
      const j=await r.json().catch(()=>({}));
      if(!r.ok)throw new Error(j.error||j.message||('HTTP '+r.status));
      return j;
    }catch(e){
      if(e.name==='AbortError')throw new Error('The live VREZER backend did not respond in time. Render may be waking up; retry the analysis.');
      throw e;
    }finally{clearTimeout(t);}
  }

  async function sha(file){
    const b=await file.arrayBuffer(), h=await crypto.subtle.digest('SHA-256',b);
    return Array.from(new Uint8Array(h)).map(x=>x.toString(16).padStart(2,'0')).join('');
  }
  function cacheKey(h){return APP+':'+h;}
  function cacheRead(h){try{return JSON.parse(localStorage.getItem(cacheKey(h))||'null');}catch(e){return null;}}
  function cacheWrite(h,d){try{localStorage.setItem(cacheKey(h),JSON.stringify(d));}catch(e){}}

  async function extract(file){
    const base=api(); if(!base)throw new Error('VREZER backend URL is not configured.');
    const fd=new FormData(); fd.append('file',file);
    const r=await fetchJson(base+'/api/analyzer/extract',{method:'POST',body:fd},300000);
    if(r.status!=='SUCCESS'||!r.text)throw new Error(r.message||'Resume extraction failed.');
    return r.text;
  }
  async function analyze(text){
    const base=api(); if(!base)throw new Error('VREZER backend URL is not configured.');
    const r=await fetchJson(base+'/api/analyzer/analyze',{
      method:'POST',headers:{'Content-Type':'application/json'},
      body:JSON.stringify({resumeText:text,jobDescription:''})
    },300000);
    if(!r||typeof r!=='object'||r.error||r.status==='ERROR')throw new Error(r&&r.error||'Analysis service returned no usable dossier.');
    return r;
  }

  function sample(kind){
    const s={
      software:'Alex Mercer\nalex.mercer@email.com | +91 98765 43210 | linkedin.com/in/alex-mercer | github.com/alex-mercer\nLocation: Bengaluru, Karnataka, India\n\nPROFESSIONAL SUMMARY\nSenior Software Development Engineer with 4+ years of experience building scalable microservices, REST APIs and distributed cloud applications.\n\nTECHNICAL SKILLS\nJava, Spring Boot, Python, React, PostgreSQL, Docker, AWS, Kubernetes, Git, Linux, MySQL, Redis\n\nWORK EXPERIENCE\nSenior Software Engineer | Razorpay Technologies | 2022 - Present\nArchitected scalable payment processing microservices and reduced API latency by 40% using Redis and PostgreSQL optimization.\n\nPROJECTS\nDistributed E-Commerce Microservices Platform\nReal-time Analytics Dashboard\n\nEDUCATION\nB.Tech in Computer Science & Engineering | VTU Karnataka | 2016 - 2020',
      aiml:'Dr. Priya Sharma\npriya.sharma@aiml.org | +91 98765 12345 | linkedin.com/in/priya-sharma-ai | github.com/priya-ai\nLocation: Bengaluru, India\n\nPROFESSIONAL SUMMARY\nAI / ML Specialist and Data Scientist with 5+ years of experience in Deep Learning, NLP, Computer Vision, LLMs, RAG and MLOps.\n\nTECHNICAL SKILLS\nMachine Learning, Deep Learning, NLP, Computer Vision, Large Language Models, RAG, Python, PyTorch, TensorFlow, Scikit-learn, Pandas, NumPy, OpenCV, Hugging Face, LangChain, AWS, Docker, Kubernetes, MLflow\n\nEXPERIENCE\nLead AI Engineer | AI Labs | 2021 - Present\nDesigned deep learning systems and fine-tuned Transformer models for NLP analysis.\n\nEDUCATION\nM.Tech in Artificial Intelligence | IISc Bangalore | 2017 - 2019',
      cae:'Rohan Verma\nrohan.verma@engg.com | +91 98123 45678 | linkedin.com/in/rohan-verma-cae\nLocation: Pune, Maharashtra, India\n\nPROFESSIONAL SUMMARY\nMechanical CAE and Finite Element Analysis Engineer with 4+ years of experience in structural simulation, thermal analysis and CAD modeling.\n\nTECHNICAL SKILLS\nANSYS Workbench, FEA Analysis, Structural Analysis, Thermal Dynamics, Crashworthiness, Abaqus, SolidWorks, AutoCAD, CATIA V5, PTC Creo, GD&T, MATLAB, Python, C++\n\nEXPERIENCE\nSenior CAE Engineer | Tata Motors R&D | 2021 - Present\nPerformed non-linear structural and impact FEA simulations and optimized component weight while maintaining rigidity.\n\nEDUCATION\nB.E. in Mechanical Engineering | College of Engineering Pune | 2016 - 2020'
    };
    sampleText=s[kind]||s.software;
    file=new File([sampleText],kind+'_sample.txt',{type:'text/plain'});
    const fs=$('file-status'); if(fs){fs.style.display='block';fs.textContent='✓ Loaded sample profile: '+kind.toUpperCase();}
    const b=$('analyse-btn'); if(b)b.disabled=false;
    setTicker('Sample profile loaded — ready for live backend analysis');
  }

  function choose(f){
    if(!f)return;
    if(!/\.(pdf|docx?|txt)$/i.test(f.name||'')){error('Upload a PDF, DOCX, DOC or TXT resume.');return;}
    file=f; sampleText='';
    const fs=$('file-status'); if(fs){fs.style.display='block';fs.textContent='✓ Loaded: '+f.name+' ('+Math.round(f.size/1024)+' KB)';}
    const b=$('analyse-btn'); if(b)b.disabled=false;
    setTicker('Resume loaded — ready for the live backend analysis');
  }
  function error(m){
    hide($('loading-section')); show($('upload-section'));
    const e=$('file-status'); if(e){e.style.display='block';e.style.background='rgba(255,0,60,.15)';e.style.color='#ff4a7d';e.innerHTML='<strong>VREZER Pipeline:</strong> '+esc(m);}
  }

  async function run(){
    if(!file){error('Please select or drop a resume first.');return;}
    hide($('upload-section'),$('dashboard-section')); show($('loading-section'));
    try{
      loadProgress(8,'Phase 1 / 5','Reading one resume through the live VREZER backend…');phase(0);
      const h=await sha(file); let d=!sampleText?cacheRead(h):null;
      if(d){loadProgress(74,'Phase 4 / 5','Using the exact analysis saved for this resume hash…');phase(3);}
      else{
        let text=sampleText;
        if(!text){text=await extract(file);loadProgress(32,'Phase 2 / 5','Resume evidence extracted and kept as the single analysis input.');phase(1);}
        else{loadProgress(32,'Phase 2 / 5','Sample evidence prepared and kept as the single analysis input.');phase(1);}
        loadProgress(48,'Phase 3 / 5','Running the single production AI reasoning pipeline…');phase(2);
        d=await analyze(text); loadProgress(76,'Phase 4 / 5','Backend returned the complete dossier and live market fields…');phase(3);
        if(!sampleText)cacheWrite(h,d);
      }
      if(!d||typeof d!=='object')throw new Error('No analysis dossier was returned.');
      data=d; loadProgress(96,'Phase 5 / 5','Rendering every section from the exact same response object…');phase(4);
      renderAll(d); loadProgress(100,'Complete','Analysis complete. Dashboard sections are synchronized to one source.'); show($('dashboard-section')); hide($('loading-section'),$('upload-section')); setTicker('ANALYSIS COMPLETE · '+val(d.name)+' · '+val(d.role,d.careerDomain));
      window.scrollTo({top:0,behavior:'smooth'});
    }catch(e){console.error(e);error(e.message||'Analysis failed.');}
  }

  function chips(id,a){const e=$(id);if(!e)return;const x=list(a);e.innerHTML=x.length?x.map(v=>'<span class="t-chip">'+esc(v)+'</span>').join(''):'<span class="muted">Not available</span>';}
  function rows(id,a){const e=$(id);if(!e)return;const x=list(a);e.innerHTML=x.length?x.map(v=>'<div class="list-row">'+esc(v)+'</div>').join(''):'<div class="muted">Not available</div>';}
  function cardRows(a){return list(a).map(v=>'<div class="list-row">'+esc(v)+'</div>').join('')||'<div class="muted">Not available</div>';}
  function questions(id,a){const e=$(id);if(!e)return;const x=arr(a);e.innerHTML=x.length?x.map(q=>{const o=obj(q);return '<div class="question-card"><b>'+esc(val(o.question,q))+'</b><p>'+esc(val(o.modelAnswer,o.starAnswer,o.answer,'Not available'))+'</p></div>';}).join(''):'<div class="muted">No questions returned.</div>';}

  function renderIdentity(d){
    const role=val(d.role,d.targetJobRole,d.careerDomain), domain=val(d.careerDomain,d.primaryDomain), ats=d.atsScore;
    put('drc-name',val(d.name,d.candidateName));put('drc-role',role);put('drc-exp',val(d.experience,d.experienceLevel));put('drc-edu',d.education);put('drc-domain',domain);
    put('hm-ats',ats);put('hm-ai-score',val(d.recruiterInsights&&d.recruiterInsights.overallEmployabilityScore,ats));
    put('hm-domain',domain);put('hm-level',val(d.experienceLevel,d.careerLevel));put('hm-status','LIVE BACKEND');put('hm-confidence',d.confidenceScore);
    put('ai-prediction',role+' · '+domain);put('pred-conf-val',d.confidenceScore==null?'Not available':d.confidenceScore+'%');
    const b=$('pred-conf-bar-fill');if(b)b.style.width=(Number(d.confidenceScore)||0)+'%';
  }

  function chart(id,type,labels,values,opts){
    if(!$(id)||typeof Chart==='undefined'||!labels.length||!values.length)return;
    if(charts[id])try{charts[id].destroy();}catch(e){}
    const cfg={type:type,data:{labels:labels,datasets:[{data:values,backgroundColor:type==='radar'?'rgba(255,0,60,.15)':'#ff003c',borderColor:'#ff003c',borderWidth:type==='radar'?2:0,pointBackgroundColor:'#ff003c',borderRadius:6}]},options:opts||{plugins:{legend:{display:false}}}};
    charts[id]=new Chart($(id),cfg);
  }

  function renderOverview(d){
    const ats=Number(d.atsScore), det=obj(d.atsScoreDetails), ds=[['Formatting','formattingScore'],['Sections','sectionCompletenessScore'],['Keywords','keywordOptimizationScore'],['Achievements','achievementScore'],['Grammar','grammarScore']];
    put('ats-val',Number.isFinite(ats)?ats:'Not available');put('ats-label',d.atsScoreText);
    put('sal-val',val(d.expectedLpaRange,d.salaryMin&&d.salaryMax?d.salaryMin+' - '+d.salaryMax:'Not available'));put('sal-unit',d.salaryCurrency||'INR');put('sal-usd',d.salaryUsd);
    if(Number.isFinite(ats))chart('ats-chart','doughnut',['ATS','Gap'],[ats,100-ats],{cutout:'78%',plugins:{legend:{display:false}}});
    const lab=[],v=[];ds.forEach(x=>{if(Number.isFinite(Number(det[x[1]]))){lab.push(x[0]);v.push(Number(det[x[1]]));}});
    chart('bar-chart','bar',lab,v,{indexAxis:'y',scales:{x:{min:0,max:100}},plugins:{legend:{display:false}}});
    const si=arr(d.skillIntelligence&&d.skillIntelligence.technicalCompetencyChart), labs=si.slice(0,6).map(x=>val(x.skill)), vals=si.slice(0,6).map(x=>Number(x.score)||0);
    if(labs.length&&vals.length)chart('radar-chart','radar',labs,vals,{scales:{r:{min:0,max:100,ticks:{display:false}}},plugins:{legend:{display:false}}});
    const c=Number(d.confidenceScore);put('ov-conf-score-lbl',Number.isFinite(c)?c+'%':'Not available');put('ov-conf-level-lbl',d.confidenceLevel||(Number.isFinite(c)?(c>=80?'High':c>=60?'Medium':'Low'):'Not available'));put('ov-conf-explanation-text',d.confidenceExplanation);
    const cb=$('ov-confidence-bar-fill');if(cb)cb.style.width=(Number(c)||0)+'%';renderSwot(d.swot);
  }
  function renderSwot(s){
    s=obj(s);chips('swot-strengths',s.strengths);chips('swot-weaknesses',s.weaknesses);chips('swot-opps',s.opportunities);chips('swot-risks',s.risks||s.resumeGaps||s.improvements);
  }
  function renderProfile(d){
    put('profile-summary',d.professionalSummary);put('profile-domain',d.careerDomain);put('profile-secondary-domain',d.secondaryDomain);put('profile-level',val(d.experienceLevel,d.careerLevel));put('profile-industry',d.industry||d.careerDomain);
    chips('profile-strongest-skills',d.topSkills);chips('profile-transferable-skills',d.transferableSkills||d.softSkills);
    put('conf-score-lbl',d.confidenceScore==null?'Not available':d.confidenceScore+'%');put('conf-level-lbl',d.confidenceLevel);put('conf-explanation-text',d.confidenceExplanation);
    const b=$('confidence-bar-fill');if(b)b.style.width=(Number(d.confidenceScore)||0)+'%';
    const e=$('evidence-panel');if(e)e.innerHTML='<div><b>Name</b><span>'+esc(val(d.name))+'</span></div><div><b>Role</b><span>'+esc(val(d.role,d.careerDomain))+'</span></div><div><b>Domain</b><span>'+esc(val(d.careerDomain))+'</span></div><div><b>Experience</b><span>'+esc(val(d.experience,d.experienceLevel))+'</span></div><div><b>Detected skills</b><span>'+esc(arr(d.topSkills).length)+'</span></div>';
  }
  function renderAts(d){
    const det=obj(d.atsScoreDetails), e=$('score-cards-container');
    const fields=[['Formatting','formattingScore'],['Section completeness','sectionCompletenessScore'],['Keyword optimization','keywordOptimizationScore'],['Achievements','achievementScore'],['Grammar','grammarScore']];
    if(e)e.innerHTML=fields.map(x=>'<div class="score-card"><div class="score-card-label">'+esc(x[0])+'</div><strong>'+ (Number.isFinite(Number(det[x[1]]))?esc(Number(det[x[1]])+'%'):'Not available') +'</strong></div>').join('');
    put('formatting-analysis',det.formattingExplanation||det.formatting);put('section-completeness',det.sectionCompletenessExplanation||det.sectionCompleteness);put('missing-keywords',list(d.missingKeywords||d.skillGaps).join(', '));put('grammar-analysis',det.grammarExplanation||det.grammar);
    rows('red-flags-list',d.redFlags||d.red_flags);
  }
  function renderSkills(d){
    rows('gap-list',d.skillGaps||d.missingSkills);rows('strengths-full-list',d.topSkills);chips('emerging-skills',d.hiringTrends&&d.hiringTrends.emergingTechnologies);chips('domain-grid',d.topSkills);
    const t=$('tech-skills-bars'),items=arr(d.skillIntelligence&&d.skillIntelligence.technicalCompetencyChart);
    if(t)t.innerHTML=items.length?items.map(x=>'<div class="skill-bar-row"><span>'+esc(val(x.skill))+'</span><div class="skill-bar"><i style="width:'+Math.max(0,Math.min(100,Number(x.score)||0))+'%"></i></div><b>'+esc(Number(x.score)||0)+'%</b></div>').join(''):'<div class="muted">No technical competency chart returned.</div>';
    const s=$('soft-skills-bars'),soft=arr(d.softSkills||d.skillIntelligence&&d.skillIntelligence.softSkillsAnalysis);
    if(s)s.innerHTML=soft.length?soft.map(x=>{const o=obj(x),score=Number(o.score);return '<div class="skill-bar-row"><span>'+esc(val(o.skill,x))+'</span><div class="skill-bar"><i style="width:'+(Number.isFinite(score)?Math.max(0,Math.min(100,score)):0)+'%"></i></div><b>'+ (Number.isFinite(score)?score+'%':'Not available') +'</b></div>';}).join(''):'<div class="muted">Not available</div>';
  }

  function renderJobs(d){
    const jobs=arr(d.retrievedJobOpportunities);const e=$('job-cards-grid');
    if(e)e.innerHTML=jobs.length?jobs.map(j=>'<article class="job-card"><div class="job-top"><strong>'+esc(val(j.title,j.role))+'</strong><span>'+esc(val(j.source,'Live'))+'</span></div><h4>'+esc(val(j.name,j.company))+'</h4><div>'+esc(val(j.location))+'</div><div class="job-salary">'+esc(val(j.salary,j.salaryRange,'Salary not disclosed'))+'</div>'+(j.url?'<a class="job-apply-btn" href="'+esc(j.url)+'" target="_blank" rel="noopener">Apply / View Job</a>':'')+'</article>').join(''):'<div class="muted">No live job opportunities returned for this resume.</div>';
    put('jobs-source-notice',jobs.length?'Live jobs returned by the backend retrieval pipeline.':'No live job opportunities returned.');
    ['t1','t2','t3'].forEach(function(k){const x=obj(d[k]),m={t1:['t1-role','t1-company','t1-loc','t1-sal'],t2:['t2-role','t2-company','t2-loc','t2-sal'],t3:['t3-role','t3-company','t3-loc','t3-sal']}[k];put(m[0],val(x.role,x.title));put(m[1],val(x.company,x.name));put(m[2],val(x.city,x.location));put(m[3],val(x.salary,x.salaryRange,x.expectedLpaRange));});
  }
  function renderCompanies(d){
    const e=$('mi-company-grid'),c=arr(d.recommendedCompanies);
    if(e)e.innerHTML=c.length?c.map(x=>'<div class="mi-company-card"><h4>'+esc(val(x.name,x.company))+'</h4><div>'+esc(val(x.role,x.title))+'</div><small>'+esc(val(x.location,x.city))+'</small><strong>'+esc(val(x.salary,x.salaryRange,'Salary not disclosed'))+'</strong></div>').join(''):'<div class="muted">No matching companies returned by the live backend.</div>';
    html('mi-quick-sections',c.slice(0,8).map(x=>'<span class="t-chip">'+esc(val(x.name,x.company))+'</span>').join(''));put('mi-result-count',c.length+' live matches');
  }
  function renderMarket(d){
    const m=obj(d.hiringTrends);put('market-demand',m.domainDemand);put('market-growth',m.industryGrowthPercentage==null?'Not available':m.industryGrowthPercentage+'%');put('market-remote',m.remoteWorkAvailabilityPercentage==null?'Not available':m.remoteWorkAvailabilityPercentage+'%');chips('trending-tech',m.emergingTechnologies);chips('high-demand-skills',d.topSkills);put('market-outlook',m.futureOutlook);put('market-data-sources',list(m.dataSources).join(' · '));
  }
  function renderCareer(d){
    const cp=obj(d.careerPrediction);rows('best-roles-list',d.bestMatchingJobRoles||cp.suitableRoles);rows('alt-paths-list',d.alternativeCareerPaths||cp.alternativePaths);
    const tl=arr(d.careerGrowthTimeline),te=$('career-timeline');if(te)te.innerHTML=tl.length?tl.map(x=>'<div class="timeline-item"><b>'+esc(val(x.stage))+'</b><strong>'+esc(val(x.title))+'</strong><span>'+esc(val(x.expectedSalaryProgression))+'</span><p>'+esc(val(x.roadmapNotes))+'</p></div>').join(''):'<div class="muted">No timeline returned.</div>';
    rows('learning-roadmap',d.learningRoadmap||d.skillIntelligence&&d.skillIntelligence.aiLearningRoadmap);chips('cert-recommendations',d.certifications||d.recommendedCertifications);rows('weekly-plan',d.weeklyLearningPlan||d.personalizedWeeklyPlan);
  }
  function renderInterview(d){
    const x=obj(d.interviewPreparation);questions('technical-questions',x.technicalQuestions);questions('hr-questions',x.hrQuestions);questions('project-questions',x.projectDiscussionQuestions);rows('interview-tips-content',x.tips||x.interviewTips||d.nextBestActions);
    const s=Number(d.recruiterInsights&&d.recruiterInsights.overallEmployabilityScore||d.atsScore);put('irb-score',Number.isFinite(s)?s+'%':'Not available');chips('irb-tips',x.tips||d.nextBestActions);
  }
  function renderImprove(d){
    put('recruiter-feedback',d.recruiterFeedback||d.recruiterInsights&&d.recruiterInsights.communicationQuality);rows('improvements-list',d.improvements||d.nextBestActions);rows('missing-sections',d.missingSections);rows('missing-achievements',d.missingAchievements);rows('bullet-rewrites',d.bulletPointRewrites);put('cover-letter-box',d.coverLetter||d.coverLetterDraft||'No cover letter generated in this analysis.');
  }
  function renderAnalytics(d){
    chart('analytics-ats-chart','bar',['ATS Score'],[Number(d.atsScore)||0],{indexAxis:'y',scales:{x:{min:0,max:100}},plugins:{legend:{display:false}}});
    const sk=arr(d.topSkills).slice(0,8);chart('analytics-skills-chart','bar',sk,sk.map(()=>Number(d.atsScore)||0),{indexAxis:'y',scales:{x:{min:0,max:100}},plugins:{legend:{display:false}}});
    const rr=arr(d.bestMatchingJobRoles||d.careerPrediction&&d.careerPrediction.suitableRoles).slice(0,6),lab=rr.map(x=>typeof x==='object'?val(x.title,x.role):x),vv=rr.map(x=>Number(x&&x.matchPercentage||x&&x.score)||Number(d.atsScore)||0);chart('analytics-compare-chart','bar',lab,vv,{indexAxis:'y',scales:{x:{min:0,max:100}},plugins:{legend:{display:false}}});
  }
  function renderDebug(d){
    put('debug-query',d.generatedQuery||d.booleanQuery);put('debug-profile',JSON.stringify(d.profile||d.candidateProfile||{},null,2));put('debug-parsed',JSON.stringify(d.parsedResume||{},null,2));put('debug-ranking',JSON.stringify({tier1:d.tier1,tier2:d.tier2,tier3:d.tier3},null,2));put('debug-request',d.geminiRequest||d.aiRequest);put('debug-response',d.geminiResponse||d.aiResponse);put('debug-ats',JSON.stringify(d.atsScoreDetails||{},null,2));put('debug-json',JSON.stringify(d,null,2));
  }
  function renderAll(d){renderIdentity(d);renderOverview(d);renderProfile(d);renderAts(d);renderSkills(d);renderJobs(d);renderCompanies(d);renderMarket(d);renderCareer(d);renderInterview(d);renderImprove(d);renderAnalytics(d);renderDebug(d);renderDownloadCenter();}
  function renderDownloadCenter(){
    const e=$('download-center-grid');if(!e)return;e.innerHTML='<div class="export-card"><h4>Verified JSON</h4><p>Exact analysis object used by the dashboard.</p><button class="btn-browse" data-dl="json">Download JSON</button></div><div class="export-card"><h4>Executive TXT</h4><p>Same synchronized data in a text report.</p><button class="btn-browse" data-dl="txt">Download TXT</button></div><div class="export-card"><h4>Browser PDF</h4><p>Printable summary from the same analysis state.</p><button class="btn-browse" data-dl="pdf">Download PDF</button></div>';
    Array.prototype.forEach.call(e.querySelectorAll('[data-dl]'),b=>b.addEventListener('click',()=>download(b.dataset.dl)));
  }

  function report(){
    return ['VREZER AI CAREER INTELLIGENCE','================================','Candidate: '+val(data.name),'Role: '+val(data.role,data.targetJobRole),'Domain: '+val(data.careerDomain),'Experience: '+val(data.experience,data.experienceLevel),'ATS Score: '+val(data.atsScore),'Confidence: '+val(data.confidenceScore),'','Professional Summary',val(data.professionalSummary),'','Top Skills',list(data.topSkills).join(', ')||'Not available','','Skill Gaps',list(data.skillGaps||data.missingSkills).join(', ')||'Not available','','Live Jobs',arr(data.retrievedJobOpportunities).map(x=>val(x.title,x.role)+' | '+val(x.name,x.company)+' | '+val(x.location)+' | '+val(x.salary,'Salary not disclosed')).join('\n')||'None returned','','Next Best Actions',list(data.nextBestActions).join('\n')||'Not available'].join('\n');
  }
  function download(type){
    if(!data)return;
    const name=(val(data.name,'vrezer')||'vrezer').replace(/[^a-z0-9_-]+/gi,'_');
    if(type==='json')return save(new Blob([JSON.stringify(data,null,2)],{type:'application/json'}),name+'-analysis.json');
    if(type==='txt')return save(new Blob([report()],{type:'text/plain'}),name+'-report.txt');
    if(window.html2pdf){
      const n=document.createElement('div');n.style.background='#fff';n.style.color='#111';n.style.padding='32px';n.style.fontFamily='Arial';n.innerHTML='<h1>VREZER AI Career Intelligence</h1><h2>'+esc(val(data.name))+'</h2><p><b>Role:</b> '+esc(val(data.role,data.targetJobRole))+'</p><p><b>Domain:</b> '+esc(val(data.careerDomain))+'</p><p><b>ATS:</b> '+esc(val(data.atsScore))+'</p><p><b>Confidence:</b> '+esc(val(data.confidenceScore))+'</p><h3>Summary</h3><p>'+esc(val(data.professionalSummary))+'</p><h3>Skills</h3><p>'+esc(list(data.topSkills).join(', '))+'</p><h3>Skill Gaps</h3><p>'+esc(list(data.skillGaps||data.missingSkills).join(', '))+'</p><h3>Next Actions</h3><pre style="white-space:pre-wrap">'+esc(list(data.nextBestActions).join('\n'))+'</pre>';document.body.appendChild(n);return window.html2pdf().from(n).set({margin:.4,filename:name+'-report.pdf',html2canvas:{scale:1.2},jsPDF:{unit:'in',format:'a4'}}).save().finally(()=>n.remove());
    }
    window.print();
  }
  function save(blob,name){const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=name;document.body.appendChild(a);a.click();setTimeout(()=>{URL.revokeObjectURL(a.href);a.remove();},1000);}

  // UI wiring
  const fi=$('file-input'),dz=$('drop-zone'),ab=$('analyse-btn');
  if(fi)fi.addEventListener('change',e=>choose(e.target.files&&e.target.files[0]));
  if(dz){dz.addEventListener('dragover',e=>{e.preventDefault();dz.classList.add('over');});dz.addEventListener('dragleave',()=>dz.classList.remove('over'));dz.addEventListener('drop',e=>{e.preventDefault();dz.classList.remove('over');choose(e.dataTransfer.files&&e.dataTransfer.files[0]);});}
  const br=$('btn-browse-trigger');if(br)br.addEventListener('click',e=>{e.preventDefault();e.stopPropagation();fi&&fi.click();});
  if(ab)ab.addEventListener('click',run);
  const demo=$('btn-demo-trigger');if(demo)demo.addEventListener('click',()=>{sample('software');setTimeout(run,100);});
  Array.prototype.forEach.call(document.querySelectorAll('.sample-resume-btn'),b=>b.addEventListener('click',e=>{e.preventDefault();e.stopPropagation();sample(b.dataset.sample||'software');setTimeout(run,100);}));

  Array.prototype.forEach.call(document.querySelectorAll('.dtab'),b=>b.addEventListener('click',()=>{Array.prototype.forEach.call(document.querySelectorAll('.dtab'),x=>x.classList.remove('active'));Array.prototype.forEach.call(document.querySelectorAll('.tab-content'),x=>x.classList.add('hidden'));b.classList.add('active');show($(b.dataset.tab));}));
  Array.prototype.forEach.call(document.querySelectorAll('.stab'),b=>b.addEventListener('click',()=>{Array.prototype.forEach.call(document.querySelectorAll('.stab'),x=>x.classList.remove('active'));Array.prototype.forEach.call(document.querySelectorAll('.stab-content'),x=>x.classList.add('hidden'));b.classList.add('active');show($(b.dataset.subtab));}));
  const reset=$('reset-btn');if(reset)reset.addEventListener('click',()=>location.reload());
  const theme=$('theme-toggle');if(theme)theme.addEventListener('click',()=>{const dark=document.body.classList.contains('dark');document.body.classList.toggle('dark',!dark);document.body.classList.toggle('light',dark);const i=$('theme-icon');if(i)i.className=dark?'fa-solid fa-sun':'fa-solid fa-moon';});
  const ex=$('export-btn');if(ex)ex.addEventListener('click',()=>download('pdf'));
  ['btn-export-json','btn-export-brief','btn-export-pdf','btn-export-ats','btn-export-skills','btn-export-interview','btn-export-cover'].forEach(id=>{const e=$(id);if(e)e.addEventListener('click',()=>download(id.indexOf('json')>=0?'json':id==='btn-export-brief'||id==='btn-export-pdf'?'pdf':'txt'));});
  const cm=$('close-export-modal');if(cm)cm.addEventListener('click',()=>hide($('export-modal')));
  const clock=$('live-time');function tick(){if(clock)clock.textContent=new Date().toLocaleTimeString('en-IN',{hour:'2-digit',minute:'2-digit',second:'2-digit'});}tick();setInterval(tick,1000);
  setTicker('VREZER stable production frontend · one-source analysis pipeline');
})();