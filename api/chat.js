module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-GEMINI-API-KEY');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const { prompt, candidateContext } = req.body || {};
  const name = candidateContext ? candidateContext.name : 'Candidate';
  const role = candidateContext ? candidateContext.role : 'Software Engineer';

  const reply = `Based on your VREZER profile for ${name} (${role}): Focus on quantifying project achievements and applying to matching roles listed in your dashboard!`;

  return res.status(200).json({
    status: 'SUCCESS',
    reply: reply,
    timestamp: new Date().toISOString()
  });
};
