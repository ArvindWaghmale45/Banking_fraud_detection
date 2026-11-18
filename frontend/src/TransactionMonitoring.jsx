import { useMemo, useState } from "react";
import axios from "axios";
import { ScanSearch, Sparkles } from "lucide-react";

const FEATURE_NAMES = Array.from({ length: 28 }, (_, index) => `v${index + 1}`);

function sampleRowFromCsv(text, predictedClass) {
  const lines = text.trim().split(/\r?\n/);
  if (lines.length < 2) throw new Error("Sample CSV is empty.");
  const headers = lines[0].replaceAll('"', "").split(",");
  for (let index = 1; index < lines.length; index += 1) {
    const values = lines[index].replaceAll('"', "").split(",");
    const row = Object.fromEntries(headers.map((header, position) => [header, values[position]]));
    if (String(row.Predicted_Class) === String(predictedClass)) return row;
  }
  throw new Error(`No sample found for predicted class ${predictedClass}.`);
}

export default function TransactionMonitoring({ session }) {
  const [values, setValues] = useState({
    time: "160760",
    amount: "23",
    ...Object.fromEntries(FEATURE_NAMES.map((name) => [name, "0"])),
  });
  const [advanced, setAdvanced] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);

  const payload = useMemo(() => ({
    time: Number(values.time),
    amount: Number(values.amount),
    features: Object.fromEntries(
      FEATURE_NAMES.map((name) => [name, Number(values[name])]),
    ),
  }), [values]);

  const loadSample = async (predictedClass) => {
    setError("");
    try {
      const response = await fetch("/test_predictions_sample.csv");
      if (!response.ok) throw new Error("Could not load sample transactions.");
      const row = sampleRowFromCsv(await response.text(), predictedClass);
      setValues({
        time: row.Time,
        amount: row.Amount,
        ...Object.fromEntries(FEATURE_NAMES.map((name) => [name, row[name.toUpperCase()] || "0"])),
      });
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  const scoreTransaction = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setResult(null);
    try {
      const response = await axios.post(
        "http://localhost:8080/api/predictions/manual",
        payload,
        { headers: { Authorization: `Bearer ${session.token}` } },
      );
      setResult(response.data);
    } catch (requestError) {
      setError(
        requestError.response?.data?.detail
          || requestError.response?.data?.message
          || "Could not score this transaction.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="workspace-panel">
      <div className="section-heading">
        <div>
          <h2>Transaction monitoring</h2>
          <p>Score a transaction with the saved Random Forest model and create an investigation if suspicious.</p>
        </div>
        <ScanSearch size={24} />
      </div>

      {error && <p className="feedback error" role="alert">{error}</p>}

      <form onSubmit={scoreTransaction}>
        <div className="upload-form">
          <label className="file-picker">
            <span>Time offset</span>
            <input value={values.time} onChange={(event) => setValues((current) => ({ ...current, time: event.target.value }))} type="number" min="0" required />
          </label>
          <label className="file-picker">
            <span>Amount</span>
            <input value={values.amount} onChange={(event) => setValues((current) => ({ ...current, amount: event.target.value }))} type="number" min="0" step="0.01" required />
          </label>
        </div>

        <div className="dataset-actions" style={{ marginTop: 16 }}>
          <button type="button" className="secondary-button" onClick={() => loadSample(1)}>
            <Sparkles size={15} /> Load sample suspicious
          </button>
          <button type="button" className="secondary-button" onClick={() => loadSample(0)}>
            Load sample genuine
          </button>
          <button type="button" className="text-button" onClick={() => setAdvanced((current) => !current)}>
            {advanced ? "Hide" : "Show"} advanced features
          </button>
        </div>

        {advanced && (
          <div className="executive-score-list" style={{ gridTemplateColumns: "repeat(4, minmax(0, 1fr))", marginTop: 16 }}>
            {FEATURE_NAMES.map((name) => (
              <label className="file-picker" key={name}>
                <span>{name.toUpperCase()}</span>
                <input
                  value={values[name]}
                  onChange={(event) => setValues((current) => ({ ...current, [name]: event.target.value }))}
                  type="number"
                  step="any"
                  required
                />
              </label>
            ))}
          </div>
        )}

        <button className="primary-button" disabled={loading} type="submit" style={{ marginTop: 20 }}>
          {loading ? "Scoring..." : "Score transaction"}
        </button>
      </form>

      {result && (
        <div className="executive-model-summary" style={{ marginTop: 20 }}>
          <div className="executive-model-title">
            <strong>{result.prediction}</strong>
            <span>Risk: {result.riskLevel}</span>
          </div>
          <div className="executive-score-list">
            <div><span>Fraud probability</span><strong>{(Number(result.fraudProbability) * 100).toFixed(4)}%</strong></div>
            <div><span>Threshold</span><strong>{(Number(result.threshold) * 100).toFixed(4)}%</strong></div>
            <div><span>Prediction ID</span><strong>{result.predictionId}</strong></div>
            <div><span>Alert ID</span><strong>{result.alertId ?? "None"}</strong></div>
          </div>
        </div>
      )}
    </section>
  );
}
