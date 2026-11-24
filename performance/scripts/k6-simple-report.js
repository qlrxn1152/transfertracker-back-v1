export function simpleReport(data) {
    const metrics = data.metrics;

    const duration = metrics.http_req_duration.values;
    const requests = metrics.http_reqs.values;
    const failed = metrics.http_req_failed.values;

    const dropped =
        metrics.dropped_iterations?.values?.count ?? 0;

    const completed = requests.count;
    const total = completed + dropped;

    const completionRate = total > 0
        ? (completed / total) * 100
        : 0;

    const failedRate = failed.rate * 100;

    const passed =
        duration['p(95)'] < 300 &&
        failed.rate < 0.001 &&
        dropped === 0 &&
        metrics.checks?.values?.rate === 1;

    function formatTime(ms) {
        if (!Number.isFinite(ms)) {
            return '-';
        }

        return ms >= 1000
            ? `${(ms / 1000).toFixed(2)}s`
            : `${ms.toFixed(2)}ms`;
    }

    function formatNumber(value) {
        return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }

    function metricCard(label, value, description, color = '') {
        return `
            <div class="metric-card">
                <div class="metric-label">${label}</div>
                <div class="metric-value ${color}">
                    ${value}
                </div>
                <div class="metric-description">
                    ${description}
                </div>
            </div>
        `;
    }

    function responseBar(label, value) {
        const width = duration.max > 0
            ? (value / duration.max) * 100
            : 0;

        return `
            <div class="bar-row">
                <div class="bar-header">
                    <span>${label}</span>
                    <strong>${formatTime(value)}</strong>
                </div>

                <div class="bar-track">
                    <div
                        class="bar-fill"
                        style="width: ${width}%"
                    ></div>
                </div>
            </div>
        `;
    }

    return `
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">

    <meta
        name="viewport"
        content="width=device-width, initial-scale=1.0"
    >

    <title>VEYRO Performance Report</title>

    <style>
        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            padding: 40px 20px;
            background: #0f172a;
            color: #e2e8f0;
            font-family:
                -apple-system,
                BlinkMacSystemFont,
                "Segoe UI",
                sans-serif;
        }

        .container {
            max-width: 1100px;
            margin: 0 auto;
        }

        header {
            margin-bottom: 32px;
        }

        h1 {
            margin: 0 0 10px;
            font-size: 30px;
        }

        h2 {
            margin: 0 0 24px;
            font-size: 19px;
        }

        .subtitle {
            color: #94a3b8;
            font-size: 14px;
        }

        .status {
            display: inline-block;
            margin-top: 18px;
            padding: 8px 14px;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 700;
        }

        .status-pass {
            color: #4ade80;
            background: #14532d;
        }

        .status-fail {
            color: #fca5a5;
            background: #7f1d1d;
        }

        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 16px;
            margin-bottom: 24px;
        }

        .metric-card {
            padding: 24px;
            background: #1e293b;
            border: 1px solid #334155;
            border-radius: 12px;
        }

        .metric-label {
            color: #94a3b8;
            font-size: 14px;
            margin-bottom: 12px;
        }

        .metric-value {
            font-size: 30px;
            font-weight: 700;
        }

        .metric-description {
            margin-top: 10px;
            color: #94a3b8;
            font-size: 12px;
        }

        .success {
            color: #4ade80;
        }

        .danger {
            color: #f87171;
        }

        .section {
            padding: 28px;
            margin-bottom: 24px;
            background: #1e293b;
            border: 1px solid #334155;
            border-radius: 12px;
        }

        .bar-row {
            margin-bottom: 20px;
        }

        .bar-row:last-child {
            margin-bottom: 0;
        }

        .bar-header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
            font-size: 14px;
        }

        .bar-track {
            height: 12px;
            background: #334155;
            border-radius: 6px;
            overflow: hidden;
        }

        .bar-fill {
            height: 100%;
            background: #38bdf8;
            border-radius: 6px;
        }

        .request-bar {
            display: flex;
            height: 28px;
            border-radius: 6px;
            overflow: hidden;
            background: #334155;
        }

        .completed-bar {
            background: #22c55e;
        }

        .dropped-bar {
            background: #ef4444;
        }

        .legend {
            display: flex;
            justify-content: space-between;
            gap: 12px;
            margin-top: 16px;
            font-size: 14px;
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        th, td {
            padding: 14px 8px;
            text-align: left;
            border-bottom: 1px solid #334155;
        }

        th {
            color: #94a3b8;
            font-weight: 500;
            font-size: 13px;
        }

        td {
            font-size: 14px;
        }

        tr:last-child td {
            border-bottom: none;
        }

        footer {
            text-align: center;
            color: #64748b;
            font-size: 12px;
            margin-top: 30px;
        }

        @media (max-width: 768px) {
            .metrics-grid {
                grid-template-columns: repeat(2, 1fr);
            }

            .metric-value {
                font-size: 24px;
            }
        }

        @media (max-width: 480px) {
            .metrics-grid {
                grid-template-columns: 1fr;
            }

            .legend {
                flex-direction: column;
            }
        }
    </style>
</head>

<body>
<div class="container">

    <header>
        <h1>VEYRO Performance Report</h1>

        <div class="subtitle">
            GET /api/posts?page=0
        </div>

        <div class="status ${
            passed ? 'status-pass' : 'status-fail'
        }">
            ${passed ? 'PASS' : 'FAIL'}
        </div>
    </header>

    <div class="metrics-grid">

        ${metricCard(
            '평균 응답 시간',
            formatTime(duration.avg),
            'Average Response Time'
        )}

        ${metricCard(
            'p95 응답 시간',
            formatTime(duration['p(95)']),
            '목표: 300ms 미만',
            duration['p(95)'] < 300
                ? 'success'
                : 'danger'
        )}

        ${metricCard(
            'p99 응답 시간',
            formatTime(duration['p(99)']),
            '99% Percentile'
        )}

        ${metricCard(
            '실제 처리량',
            requests.rate.toFixed(2) + ' RPS',
            '전체 실행 시간 기준'
        )}

        ${metricCard(
            'HTTP 실패율',
            failedRate.toFixed(2) + '%',
            '실행된 HTTP 요청 기준',
            failed.rate < 0.001
                ? 'success'
                : 'danger'
        )}

        ${metricCard(
            'Dropped Iterations',
            formatNumber(dropped),
            '실행하지 못한 반복 수',
            dropped === 0
                ? 'success'
                : 'danger'
        )}

    </div>

    <div class="section">
        <h2>요청 처리 현황</h2>

        <p>
            전체 요청 시도 대상:
            <strong>${formatNumber(total)}건</strong>
        </p>

        <div class="request-bar">
            <div
                class="completed-bar"
                style="width: ${completionRate}%"
            ></div>

            <div
                class="dropped-bar"
                style="width: ${100 - completionRate}%"
            ></div>
        </div>

        <div class="legend">
            <span class="success">
                실행 요청:
                ${formatNumber(completed)}건
                (${completionRate.toFixed(1)}%)
            </span>

            <span class="danger">
                미실행:
                ${formatNumber(dropped)}건
                (${(100 - completionRate).toFixed(1)}%)
            </span>
        </div>
    </div>

    <div class="section">
        <h2>응답 시간 비교</h2>

        ${responseBar('최소', duration.min)}
        ${responseBar('평균', duration.avg)}
        ${responseBar('중앙값', duration.med)}
        ${responseBar('p90', duration['p(90)'])}
        ${responseBar('p95', duration['p(95)'])}
        ${responseBar('p99', duration['p(99)'])}
        ${responseBar('최대', duration.max)}
    </div>

    <div class="section">
        <h2>성능 기준 검증</h2>

        <table>
            <thead>
                <tr>
                    <th>항목</th>
                    <th>목표</th>
                    <th>측정값</th>
                    <th>결과</th>
                </tr>
            </thead>

            <tbody>
                <tr>
                    <td>p95 응답 시간</td>
                    <td>&lt; 300ms</td>
                    <td>${formatTime(duration['p(95)'])}</td>
                    <td class="${
                        duration['p(95)'] < 300
                            ? 'success'
                            : 'danger'
                    }">
                        ${
                            duration['p(95)'] < 300
                                ? 'PASS'
                                : 'FAIL'
                        }
                    </td>
                </tr>

                <tr>
                    <td>HTTP 실패율</td>
                    <td>&lt; 0.1%</td>
                    <td>${failedRate.toFixed(2)}%</td>
                    <td class="${
                        failed.rate < 0.001
                            ? 'success'
                            : 'danger'
                    }">
                        ${
                            failed.rate < 0.001
                                ? 'PASS'
                                : 'FAIL'
                        }
                    </td>
                </tr>

                <tr>
                    <td>Dropped Iterations</td>
                    <td>0</td>
                    <td>${formatNumber(dropped)}</td>
                    <td class="${
                        dropped === 0
                            ? 'success'
                            : 'danger'
                    }">
                        ${dropped === 0 ? 'PASS' : 'FAIL'}
                    </td>
                </tr>

                <tr>
                    <td>HTTP 200 검증</td>
                    <td>100%</td>
                    <td>
                        ${(
                            (metrics.checks?.values?.rate ?? 0)
                            * 100
                        ).toFixed(2)}%
                    </td>
                    <td class="${
                        metrics.checks?.values?.rate === 1
                            ? 'success'
                            : 'danger'
                    }">
                        ${
                            metrics.checks?.values?.rate === 1
                                ? 'PASS'
                                : 'FAIL'
                        }
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <footer>
        VEYRO Backend Performance Test
        · ${new Date().toISOString()}
    </footer>

</div>
</body>
</html>
    `;
}