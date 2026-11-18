import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  Activity,
  BadgeCheck,
  BrainCircuit,
  Gauge,
  ShieldAlert,
  Target,
  TrendingUp,
} from "lucide-react";

const metricCards = [
  { key: "accuracy", label: "Accuracy", icon: Gauge },
  { key: "precision", label: "Precision", icon: Target },
  { key: "recall", label: "Fraud recall", icon: ShieldAlert },
  { key: "f1Score", label: "F1 score", icon: Activity },
  { key: "rocAuc", label: "ROC AUC", icon: TrendingUp },
  { key: "prAuc", label: "PR AUC", icon: BadgeCheck },
];

const formatScore = (value) =>
  value == null ? "—" : `${(Number(value) * 100).toFixed(2)}%`;

function TrainingDashboard({ metric, loading, onRefresh }) {
  if (loading) {
    return (
      <section className="dashboard-loading">
        <div className="loading-spinner" />
        <p>Loading your fraud analytics...</p>
      </section>
    );
  }

  if (!metric) {
    return (
      <section className="dashboard-empty">
        <BrainCircuit size={34} />
        <h2>No model metrics available</h2>
        <p>
          Train and evaluate a model in your Jupyter Notebook, then import its
          metrics into the backend to see them here.
        </p>
        <button type="button" onClick={onRefresh}>Refresh metrics</button>
      </section>
    );
  }

  const chartData = [
    { name: "Accuracy", score: Number(metric.accuracy) },
    { name: "Precision", score: Number(metric.precision) },
    { name: "Recall", score: Number(metric.recall) },
    { name: "F1 score", score: Number(metric.f1Score) },
    { name: "ROC AUC", score: Number(metric.rocAuc) },
    { name: "PR AUC", score: Number(metric.prAuc) },
  ];

  return (
    <section className="professional-dashboard">
      <div className="dashboard-title-row">
        <div>
          <p className="eyebrow">OVERVIEW / MODEL ANALYTICS</p>
          <h2>Fraud detection dashboard</h2>
          <p className="dashboard-description">
            Model performance and evaluation metrics from your saved training run.
          </p>
        </div>
        <button className="dashboard-refresh" type="button" onClick={onRefresh}>
          <Activity size={15} /> Refresh
        </button>
      </div>

      <div className="model-summary">
        <div className="model-icon"><BrainCircuit size={23} /></div>
        <div className="model-info">
          <span>ACTIVE EVALUATED MODEL</span>
          <strong>{metric.modelName}</strong>
          <small>Version {metric.modelVersion}</small>
        </div>
        <div className="model-date">
          <span>LAST TRAINING DATE</span>
          <strong>
            {metric.trainingDate
              ? new Date(metric.trainingDate).toLocaleString()
              : "Not available"}
          </strong>
        </div>
        <span className="model-status"><i /> Metrics saved</span>
      </div>

      <div className="dashboard-section-heading">
        <div>
          <h3>Model performance</h3>
          <p>Evaluation scores from the held-out test dataset</p>
        </div>
      </div>

      <div className="professional-metric-grid">
        {metricCards.map(({ key, label, icon: Icon }) => (
          <article className="professional-metric-card" key={key}>
            <div className="metric-card-top">
              <span>{label}</span>
              <div className="metric-icon"><Icon size={18} /></div>
            </div>
            <strong>{formatScore(metric[key])}</strong>
            <div className="metric-progress">
              <span style={{ width: `${Math.max(0, Math.min(100, Number(metric[key]) * 100))}%` }} />
            </div>
            <small>Evaluation score</small>
          </article>
        ))}
      </div>

      <div className="professional-chart-grid">
        <article className="professional-chart-card">
          <div className="dashboard-section-heading">
            <div>
              <h3>Performance comparison</h3>
              <p>Comparison of the six saved evaluation metrics</p>
            </div>
          </div>
          <div className="professional-chart">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 12, right: 12, left: -18, bottom: 4 }}>
                <CartesianGrid stroke="#e9eeeb" vertical={false} />
                <XAxis dataKey="name" tick={{ fill: "#75847d", fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis domain={[0, 1]} tick={{ fill: "#75847d", fontSize: 11 }} axisLine={false} tickLine={false} />
                <Tooltip
                  formatter={(value) => formatScore(value)}
                  contentStyle={{ borderRadius: 10, border: "1px solid #e5ebe7" }}
                />
                <Bar dataKey="score" name="Score" fill="#24785b" radius={[6, 6, 0, 0]} maxBarSize={54} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </article>

        <article className="professional-insight-card">
          <div className="insight-icon"><ShieldAlert size={22} /></div>
          <p className="eyebrow">MODEL INSIGHT</p>
          <h3>Focus on fraud detection quality</h3>
          <p>
            Fraud recall measures how many actual fraud cases the model
            identifies. Precision measures how many flagged cases are actually
            fraud. Both matter when evaluating an imbalanced banking dataset.
          </p>
          <div className="insight-values">
            <div>
              <span>Fraud recall</span>
              <strong>{formatScore(metric.recall)}</strong>
            </div>
            <div>
              <span>Precision</span>
              <strong>{formatScore(metric.precision)}</strong>
            </div>
          </div>
        </article>
      </div>

      <p className="dashboard-disclaimer">
        These are saved model evaluation metrics, not live banking transactions.
        A high accuracy score alone does not establish reliable fraud detection.
      </p>
    </section>
  );
}

export default TrainingDashboard;