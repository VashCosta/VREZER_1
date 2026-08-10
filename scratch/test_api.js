async function test() {
    try {
        const res = await fetch('http://localhost:7000/api/analyzer/analyze', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ resumeText: 'Dhanush Ragavendar. Java Spring Boot Developer with 3 years experience in REST API and PostgreSQL.' })
        });
        const data = await res.json();
        console.log('STATUS CODE:', res.status);
        console.log('AI_STATUS:', data.AI_STATUS);
        console.log('NAME:', data.name);
        console.log('ROLE:', data.role);
        console.log('ATS SCORE:', data.atsScore);
        console.log('JOBS RETRIEVED:', data.retrievedJobOpportunities ? data.retrievedJobOpportunities.length : 0);
    } catch (e) {
        console.error('Fetch error:', e);
    }
}
test();
