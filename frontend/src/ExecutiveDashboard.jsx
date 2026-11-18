import { Activity, AlertTriangle, ArrowRight, Database, ShieldCheck } from "lucide-react";

const formatNumber = (value) => Number(value || 0).toLocaleString();
const formatPercent = (value) => `${(value * 100).toFixed(2)}%`;

function ExecutiveDashboard({
  datasets,
  metric,
  alertSummary,
  loading,
  onOpenDatasets,
  onOpenPerformance,
  onOpenInvestigations,
}) {
  const uploadedRows = datasets.reduce((total, dataset) => total + Number(dataset.totalRecords || 0), 0);
  const classifiedRows = datasets.reduce(
    (total, dataset) => total + Number(dataset.fraudRecords || 0) + Number(dataset.genuineRecords || 0),
    0,
  );
  const fraudRows = datasets.reduce((total, dataset) => total + Number(dataset.fraudRecords || 0), 0);
  const openInvestigations = Number(alertSummary?.totalOpen || 0) + Number(alertSummary?.totalReviewing || 0);
  const fraudRate = classifiedRows > 0 ? fraudRows / classifiedRows : null;

  if (loading) {
    return <p className="empty-state">Loading live workspace data...</p>;
  }

  return (
    <section className="executive-dashboard" aria-labelledby="executive-heading">
      <div className="executive-heading">
        <div>
          <p className="eyebrow">RISK OPERATIONS / LIVE WORKSPACE</p>
          <h2 id="executive-heading">Executive overview</h2>
          <p>Current dataset and investigation totals. Scores are based on saved model evaluations.</p>
        </div>
        <span className="executive-model-tag">
          <Activity size={15} />
          {metric ? `Latest model · ${metric.modelName}` : "No evaluated model"}
        </span>
      </div>

      <div className="executive-stat-grid">
        <article className="executive-stat-card">
          <span><Database size={16} /> Dataset rows uploaded</span>
          <strong>{formatNumber(uploadedRows)}</strong>
          <small>{formatNumber(datasets.length)} dataset files in this workspace</small>
        </article>
        <article className="executive-stat-card">
          <span><ShieldCheck size={16} /> Transactions classified</span>
          <strong>{classifiedRows > 0 ? formatNumber(classifiedRows) : "—"}</strong>
          <small>{classifiedRows > 0 ? "Based on stored dataset classifications" : "No prediction results have been stored yet"}</small>
        </article>
        <article className="executive-stat-card">
          <span><AlertTriangle size={16} /> Fraud rate</span>
          <strong>{fraudRate === null ? "—" : formatPercent(fraudRate)}</strong>
          <small>{fraudRate === null ? "Available after transactions are scored" : `${formatNumber(fraudRows)} fraud cases recorded`}</small>
        </article>
        <article className="executive-stat-card">
          <span><Activity size={16} /> Open investigations</span>
          <strong>{alertSummary ? formatNumber(openInvestigations) : "—"}</strong>
          <small>
            {alertSummary
              ? `${formatNumber(alertSummary.totalResolved)} resolved · ${formatNumber(alertSummary.totalDismissed)} dismissed`
              : "Investigation summary unavailable"}
          </small>
        </article>
      </div>

      <div className="executive-content-grid">
        <article className="executive-panel">
          <div className="executive-panel-heading">
            <div>
              <h3>Recent datasets</h3>
              <p>Uploaded files and their processing state</p>
            </div>
            <button type="button" className="executive-link" onClick={onOpenDatasets}>
              Manage datasets <ArrowRight size={14} />
            </button>
          </div>
          {datasets.length === 0 ? (
            <p className="empty-state">No datasets uploaded yet.</p>
          ) : (
            <div className="executive-dataset-list">
              {datasets.slice(0, 5).map((dataset) => (
                <div className="executive-dataset-row" key={dataset.id}>
                  <div className="executive-dataset-icon"><Database size={16} /></div>
                  <div className="executive-dataset-name">
                    <strong>{dataset.fileName}</strong>
                    <span>{formatNumber(dataset.totalRecords)} rows · {new Date(dataset.uploadDate).toLocaleDateString()}</span>
                  </div>
                  <span className={`status-pill status-${dataset.status.toLowerCase()}`}>{dataset.status}</span>
                </div>
              ))}
            </div>
          )}
        </article>

        <article className="executive-panel">
          <div className="executive-panel-heading">
            <div>
              <h3>Model evaluation</h3>
              <p>Most recently saved held-out test results</p>
            </div>
            <button type="button" className="executive-link" onClick={onOpenPerformance}>
              View scores <ArrowRight size={14} />
            </button>
          </div>
          {metric ? (
            <div className="executive-model-summary">
              <div className="executive-model-title">
                <strong>{metric.modelName}</strong>
                <span>Version {metric.modelVersion}</span>
              </div>
              <div className="executive-score-list">
                <div><span>Precision</span><strong>{formatPercent(metric.precision)}</strong></div>
                <div><span>Fraud recall</span><strong>{formatPercent(metric.recall)}</strong></div>
                <div><span>F1 score</span><strong>{formatPercent(metric.f1Score)}</strong></div>
                <div><span>PR AUC</span><strong>{formatPercent(metric.prAuc)}</strong></div>
              </div>
              <small>Evaluated {new Date(metric.trainingDate).toLocaleString()}</small>
            </div>
          ) : (
            <p className="empty-state">Train and evaluate a model to see its real test results here.</p>
          )}
        </article>
      </div>

      <div className="executive-bottom-grid">
        <article className="executive-insight-card">
          <div>
            <p className="eyebrow">TRANSACTION ANALYTICS</p>
            <h3>Amount analysis and fraud trends</h3>
            <p>
              These charts will appear when scored transaction amounts and prediction timestamps are stored.
              Dataset upload totals are not presented as fraud detections.
            </p>
          </div>
          <button type="button" className="executive-link" onClick={onOpenInvestigations}>
            Open investigations <ArrowRight size={14} />
          </button>
        </article>
      </div>
    </section>
  );
}

export default ExecutiveDashboard;
