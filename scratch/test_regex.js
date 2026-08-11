const skill = 'C++';
const escaped = skill.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
console.log('escaped:', JSON.stringify(escaped));
const pattern = `\\b${escaped}\\b`;
console.log('pattern:', JSON.stringify(pattern));
try {
    const r = new RegExp(pattern, 'i');
    console.log('regex ok:', r.toString());
    console.log('test C++:', r.test('C++ Developer'));
} catch(e) {
    console.log('REGEX ERROR:', e.message);
}

// Test the template literal version as it appears in script.js
const skill2 = 'C++';
const regex2 = new RegExp(`\\b${skill2.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, 'i');
console.log('regex2:', regex2.toString());
