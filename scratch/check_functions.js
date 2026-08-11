const fs = require('fs');
const content = fs.readFileSync('script.js', 'utf8');

const funcDefs = new Set();
const defMatches = content.matchAll(/(?:function\s+([a-zA-Z0-9_$]+)|(?:const|let|var)\s+([a-zA-Z0-9_$]+)\s*=\s*(?:function|\([^)]*\)\s*=>))/g);
for (const m of defMatches) {
    if (m[1]) funcDefs.add(m[1]);
    if (m[2]) funcDefs.add(m[2]);
}

const callMatches = content.matchAll(/\b([a-zA-Z0-9_$]+)\s*\(/g);
const candidates = new Set();
for (const m of callMatches) {
    const fn = m[1];
    if (!funcDefs.has(fn) && /^[a-z][a-zA-Z0-9_$]*$/.test(fn)) {
        candidates.add(fn);
    }
}

// Ignore common JS builtins/DOM methods
const known = new Set([
    'if', 'for', 'while', 'switch', 'catch', 'function', 'return', 'typeof',
    'log', 'error', 'warn', 'info', 'debug', 'dir',
    'getElementById', 'getElementsByClassName', 'querySelectorAll', 'querySelector', 'createElement', 'appendChild', 'removeChild', 'setAttribute', 'getAttribute', 'removeAttribute', 'classList', 'addEventListener', 'removeEventListener', 'preventDefault', 'stopPropagation', 'getBoundingClientRect', 'scrollTo', 'scrollIntoView',
    'min', 'max', 'round', 'floor', 'ceil', 'abs', 'random', 'sqrt', 'sin', 'cos', 'now',
    'json', 'text', 'arrayBuffer', 'blob',
    'indexOf', 'lastIndexOf', 'includes', 'startsWith', 'endsWith', 'slice', 'substring', 'substr', 'split', 'join', 'trim', 'replace', 'replaceAll', 'match', 'matchAll', 'test', 'exec', 'toLowerCase', 'toUpperCase', 'charAt', 'charCodeAt', 'padStart', 'padEnd', 'concat', 'repeat',
    'push', 'pop', 'shift', 'unshift', 'splice', 'sort', 'reverse', 'map', 'filter', 'forEach', 'reduce', 'some', 'every', 'find', 'findIndex', 'entries', 'keys', 'values', 'from', 'isArray',
    'setItem', 'getItem', 'removeItem', 'clear',
    'setInterval', 'clearInterval', 'setTimeout', 'clearTimeout',
    'parseInt', 'parseFloat', 'isNaN', 'isFinite', 'encodeURIComponent', 'decodeURIComponent',
    'fetch', 'alert', 'confirm', 'prompt', 'requestAnimationFrame', 'cancelAnimationFrame',
    'then', 'catch', 'finally', 'resolve', 'reject',
    'toString', 'valueOf', 'toFixed', 'toPrecision', 'toLocaleString', 'toISOString', 'toLocaleTimeString', 'toLocaleDateString',
    'getContext', 'beginPath', 'arc', 'fill', 'stroke', 'moveTo', 'lineTo', 'clearRect', 'save', 'restore', 'translate', 'rotate', 'scale', 'setTransform', 'fillText',
    'createObjectURL', 'revokeObjectURL', 'generateAsync', 'loadAsync', 'file',
    'getDocument', 'getPage', 'getTextContent', 'readAsText'
]);

const missing = [];
for (const fn of candidates) {
    if (!known.has(fn)) {
        missing.push(fn);
    }
}

console.log('UNRESOLVED USER FUNCTION CALLS:', missing);
