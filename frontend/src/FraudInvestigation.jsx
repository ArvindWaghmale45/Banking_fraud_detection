import { useCallback, useEffect, useMemo, useState } from "react";
import axios from "axios";
import { Download, Filter, RefreshCw, ShieldAlert } from "lucide-react";

const ALERTS_API_URL = "http://localhost:8080/api/fraud-alerts";
const STATUSES = ["OPEN", "REVIEWING", "RESOLVED", "DISMISSED"];
const STATUS_FILTERS = ["ALL", ...STATUSES];

const csvValue = (value) => `"${String(value ?? "").replaceAll('"', '""')}"`;

function FraudInvestigation({ session }) {
  const [alerts, setAlerts] = useState([]);
  const [summary, setSummary] = useState(null);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [statusDrafts, setStatusDrafts] = useState({});
  const [notesDrafts, setNotesDrafts] = useState({});
  const [expandedId, setExpandedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const requestConfig = useMemo(() => ({
    headers: { Authorization: `Bearer ${session.token}` },
  }), [session.token]);

  const fetchInvestigations = useCallback(async (signal) => {
    const alertUrl = statusFilter === "ALL"
      ? `${ALERTS_API_URL}?size=100`
      : `${ALERTS_API_URL}?status=${statusFilter}&size=100`;
    const [alertResponse, summaryResponse] = await Promise.all([
      axios.get(alertUrl, { ...requestConfig, signal }),
      axios.get(`${ALERTS_API_URL}/summary`, { ...requestConfig, signal }),
    ]);
    return { alerts: alertResponse.data.content, summary: summaryResponse.data };
  }, [requestConfig, statusFilter]);

  const applyInvestigations = useCallback(({ alerts: nextAlerts, summary: nextSummary }) => {
    setAlerts(nextAlerts);
    setSummary(nextSummary);
    setStatusDrafts((current) => {
      const next = { ...current };
      nextAlerts.forEach((alert) => {
        if (!next[alert.id]) next[alert.id] = alert.status;
      });
      return next;
    });
    setNotesDrafts((current) => {
      const next = { ...current };
      nextAlerts.forEach((alert) => {
        if (next[alert.id] === undefined) next[alert.id] = alert.reviewNotes || "";
      });
      return next;
    });
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    fetchInvestigations(controller.signal)
      .then((data) => {
        applyInvestigations(data);
        setError("");
      })
      .catch((requestError) => {
        if (controller.signal.aborted) return;
        setError(
          requestError.response?.data?.message
            || requestError.response?.data?.detail
            || "Could not load investigations. Check the backend connection and sign-in session.",
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [applyInvestigations, fetchInvestigations]);

  const loadInvestigations = async () => {
    setLoading(true);
    setError("");
    try {
      applyInvestigations(await fetchInvestigations());
    } catch (requestError) {
      setError(
        requestError.response?.data?.message
          || requestError.response?.data?.detail
          || "Could not load investigations. Check the backend connection and sign-in session.",
      );
    } finally {
      setLoading(false);
    }
  };

  const saveReview = async (alertId) => {
    setSavingId(alertId);
    setError("");
    setNotice("");
    try {
      await axios.patch(
        `${ALERTS_API_URL}/${alertId}/status`,
        {
          status: statusDrafts[alertId],
          reviewNotes: notesDrafts[alertId] || "",
        },
        requestConfig,
      );
      setNotice(`Investigation #${alertId} updated.`);
      await loadInvestigations();
    } catch (requestError) {
      setError(
        requestError.response?.data?.message
          || requestError.response?.data?.detail
          || "Could not save this investigation update.",
      );
    } finally {
      setSavingId(null);
    }
  };

  const exportReport = () => {
    const header = [
      "Alert ID",
      "Transaction ID",
      "Prediction ID",
      "Severity",
      "Status",
      "Transaction Amount",
      "Created At",
      "Reviewed At",
      "Review Notes",
    ];
    const rows = alerts.map((alert) => [
      alert.id,
      alert.transactionId,
      alert.predictionId,
      alert.severity,
      alert.status,
      alert.transactionAmount,
      alert.createdAt,
      alert.reviewedAt,
      alert.reviewNotes,
    ]);
    const csv = [header, ...rows].map((row) => row.map(csvValue).join(",")).join("\r\n");
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = "fraud-investigations.csv";
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <section className="investigation-workspace" aria-labelledby="investigation-heading">
      <div className="investigation-header">
        <div>
          <p className="eyebrow">ALERT OPERATIONS / CASE REVIEW</p>
          <h2 id="investigation-heading">Fraud investigations</h2>
          <p>Review flagged transactions, record outcomes, and export the currently visible cases.</p>
        </div>
        <div className="investigation-actions">
          <button type="button" className="secondary-button" onClick={exportReport} disabled={alerts.length === 0}>
            <Download size={15} /> Export visible
          </button>
          <button type="button" className="secondary-button" onClick={loadInvestigations} disabled={loading}>
            <RefreshCw size={15} /> Refresh
          </button>
        </div>
      </div>

      {error && <p className="feedback error" role="alert">{error}</p>}
      {notice && <p className="feedback success" role="status">{notice}</p>}

      <div className="investigation-summary-grid">
        <article><span>Open</span><strong>{summary?.totalOpen ?? "—"}</strong></article>
        <article><span>Reviewing</span><strong>{summary?.totalReviewing ?? "—"}</strong></article>
        <article><span>Resolved</span><strong>{summary?.totalResolved ?? "—"}</strong></article>
        <article><span>High / critical</span><strong>{summary ? Number(summary.high) + Number(summary.critical) : "—"}</strong></article>
      </div>

      <div className="investigation-list-panel">
        <div className="investigation-list-header">
          <div>
            <h3>Flagged transactions</h3>
            <p>Cases are loaded from the backend; no sample alert data is shown.</p>
          </div>
          <label className="investigation-filter">
            <Filter size={15} />
            <span className="sr-only">Filter investigations by status</span>
            <select
              value={statusFilter}
              onChange={(event) => {
                setLoading(true);
                setStatusFilter(event.target.value);
              }}
            >
              {STATUS_FILTERS.map((status) => (
                <option key={status} value={status}>{status === "ALL" ? "All statuses" : status}</option>
              ))}
            </select>
          </label>
        </div>

        {loading ? (
          <p className="empty-state">Loading investigations...</p>
        ) : alerts.length === 0 ? (
          <div className="investigation-empty">
            <ShieldAlert size={25} />
            <strong>No flagged transactions yet</strong>
            <span>Once scored transaction predictions create alerts, they will appear here.</span>
          </div>
        ) : (
          <div className="investigation-list">
            {alerts.map((alert) => (
              <article className="investigation-card" key={alert.id}>
                <div className="investigation-card-main">
                  <div className="investigation-risk-icon"><ShieldAlert size={18} /></div>
                  <div className="investigation-case">
                    <strong>Alert #{alert.id} · Transaction #{alert.transactionId}</strong>
                    <span>
                      {alert.severity} risk · {new Date(alert.createdAt).toLocaleString()}
                    </span>
                  </div>
                  <div className="investigation-amount">
                    <span>Amount</span>
                    <strong>{alert.transactionAmount == null ? "—" : Number(alert.transactionAmount).toFixed(2)}</strong>
                  </div>
                  <span className={`status-pill status-${alert.status.toLowerCase()}`}>{alert.status}</span>
                  <button
                    className="text-button"
                    type="button"
                    aria-expanded={expandedId === alert.id}
                    onClick={() => setExpandedId(expandedId === alert.id ? null : alert.id)}
                  >
                    {expandedId === alert.id ? "Hide details" : "Review"}
                  </button>
                </div>
                {expandedId === alert.id && (
                  <div className="investigation-review">
                    <div className="investigation-detail-grid">
                      <span>Prediction ID <strong>{alert.predictionId}</strong></span>
                      <span>Created <strong>{new Date(alert.createdAt).toLocaleString()}</strong></span>
                      <span>Last reviewed <strong>{alert.reviewedAt ? new Date(alert.reviewedAt).toLocaleString() : "Not reviewed"}</strong></span>
                    </div>
                    <label>
                      Investigation notes
                      <textarea
                        maxLength={2000}
                        value={notesDrafts[alert.id] || ""}
                        onChange={(event) => setNotesDrafts((current) => ({
                          ...current,
                          [alert.id]: event.target.value,
                        }))}
                        placeholder="Record the review findings"
                        rows={3}
                      />
                    </label>
                    <div className="investigation-review-actions">
                      <label>
                        Case status
                        <select
                          value={statusDrafts[alert.id] || alert.status}
                          onChange={(event) => setStatusDrafts((current) => ({
                            ...current,
                            [alert.id]: event.target.value,
                          }))}
                        >
                          {STATUSES.map((status) => <option key={status}>{status}</option>)}
                        </select>
                      </label>
                      <button
                        className="primary-button"
                        type="button"
                        disabled={savingId === alert.id}
                        onClick={() => saveReview(alert.id)}
                      >
                        {savingId === alert.id ? "Saving..." : "Save review"}
                      </button>
                    </div>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

export default FraudInvestigation;
