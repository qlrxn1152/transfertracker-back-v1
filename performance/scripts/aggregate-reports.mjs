import fs from 'node:fs';
import path from 'node:path';

const [manifestPath, outputDirectory, suite] = process.argv.slice(2);

if (!manifestPath || !outputDirectory || !suite) {
    throw new Error('Usage: node aggregate-reports.mjs <manifest.tsv> <output-directory> <suite>');
}

const readJson = (file) => {
    try {
        return JSON.parse(fs.readFileSync(file, 'utf8'));
    } catch (_) {
        return null;
    }
};

const metric = (summary, name, value) => summary?.metrics?.[name]?.values?.[value] ?? null;
const value = (number) => (Number.isFinite(number) ? number : null);

const lines = fs.readFileSync(manifestPath, 'utf8').trim().split('\n').filter(Boolean);
const entries = lines.slice(1).map((line) => {
    const [scenario, exitCodeText, reportDirectory] = line.split('\t');
    const summary = readJson(path.join(reportDirectory, 'summary.json'));
    const metadata = readJson(path.join(reportDirectory, 'metadata.json'));
    return {
        scenario,
        exitCode: Number(exitCodeText),
        reportDirectory,
        report: fs.existsSync(path.join(reportDirectory, 'report.html')) ? path.join(reportDirectory, 'report.html') : null,
        averageMs: value(metric(summary, 'http_req_duration', 'avg')),
        p95Ms: value(metric(summary, 'http_req_duration', 'p(95)')),
        p99Ms: value(metric(summary, 'http_req_duration', 'p(99)')),
        rps: value(metric(summary, 'http_reqs', 'rate')),
        httpFailureRate: value(metric(summary, 'http_req_failed', 'rate')),
        droppedIterations: value(metric(summary, 'dropped_iterations', 'count')),
        thresholdPassed: metadata?.result?.thresholdPassed ?? null,
        status: metadata?.result?.status ?? (Number(exitCodeText) === 0 ? 'completed' : 'unknown'),
    };
});

const result = {
    suite,
    generatedAt: new Date().toISOString(),
    results: entries,
    note: 'This aggregate reads only generated k6 artifacts. Null means that a child run did not produce the corresponding metric.',
};

fs.writeFileSync(path.join(outputDirectory, 'aggregate.json'), `${JSON.stringify(result, null, 2)}\n`);

const format = (number, decimals = 2) => number === null ? 'N/A' : number.toFixed(decimals);
const markdown = [
    `# VEYRO Performance Suite: ${suite}`,
    '',
    '| API scenario | avg (ms) | p95 (ms) | p99 (ms) | RPS | HTTP failures | Dropped | Threshold | Exit |',
    '| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- | ---: |',
    ...entries.map((entry) => `| ${entry.scenario} | ${format(entry.averageMs)} | ${format(entry.p95Ms)} | ${format(entry.p99Ms)} | ${format(entry.rps)} | ${entry.httpFailureRate === null ? 'N/A' : `${(entry.httpFailureRate * 100).toFixed(3)}%`} | ${format(entry.droppedIterations, 0)} | ${entry.thresholdPassed === null ? 'N/A' : entry.thresholdPassed ? 'PASSED' : 'FAILED'} | ${entry.exitCode} |`),
    '',
    'Individual report.html, summary.json, and metadata.json files remain in each run directory.',
    '',
];
fs.writeFileSync(path.join(outputDirectory, 'aggregate.md'), markdown.join('\n'));
