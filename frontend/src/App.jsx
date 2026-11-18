import { lazy, Suspense, useCallback, useEffect, useMemo, useState } from "react";
import axios from "axios";
import {
  BarChart3,
  Database,
  LayoutDashboard,
  LogOut,
  Search,
  ScanSearch,
  ShieldCheck,
} from "lucide-react";
import "./App.css";

const API_URL = "http://localhost:8080/api/auth";
const DATASET_API_URL = "http://localhost:8080/api/datasets";
const MODEL_METRICS_API_URL = "http://localhost:8080/api/model-metrics/latest";
const MODEL_METRICS_IMPORT_API_URL = "http://localhost:8080/api/model-metrics/import";
const FRAUD_ALERTS_API_URL = "http://localhost:8080/api/fraud-alerts";
const MODEL_TRAINING_API_URL = "http://localhost:8000/api/train";
const MAX_FILE_SIZE = 200 * 1024 * 1024;
const TrainingDashboard = lazy(() => import("./TrainingDashboard.jsx"));
const ExecutiveDashboard = lazy(() => import("./ExecutiveDashboard.jsx"));
const FraudInvestigation = lazy(() => import("./FraudInvestigation.jsx"));
const TransactionMonitoring = lazy(() => import("./TransactionMonitoring.jsx"));

function DatasetWorkspace({ session, onLogout }) {
  const [datasets, setDatasets] = useState([]);
  const [file, setFile] = useState(null);
  const [uploaded, setUploaded] = useState(false);
  const [uploadedDatasetId, setUploadedDatasetId] = useState(null);
  const [trainingComplete, setTrainingComplete] = useState(false);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [modelMetric, setModelMetric] = useState(null);
  const [alertSummary, setAlertSummary] = useState(null);
  const [metricLoading, setMetricLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [selectedDataset, setSelectedDataset] = useState(null);
  const [activePage, setActivePage] = useState("overview");

  const requestConfig = useMemo(() => ({
    headers: { Authorization: `Bearer ${session.token}` },
  }), [session.token]);

  const loadModelMetrics = useCallback(async () => {
    try {
      const { data } = await axios.get(MODEL_METRICS_API_URL, {
        headers: { Authorization: `Bearer ${session.token}` },
      });
      setModelMetric(data);
    } catch (error) {
      setModelMetric(null);
      if (error.response?.status !== 404) {
        console.error("Could not load model metrics:", error);
      }
    } finally {
      setMetricLoading(false);
    }
  }, [session.token]);

  const refreshModelMetrics = useCallback(() => {
    setMetricLoading(true);
    loadModelMetrics();
  }, [loadModelMetrics]);

  useEffect(() => {
    const controller = new AbortController();
    axios
      .get(MODEL_METRICS_API_URL, { ...requestConfig, signal: controller.signal })
      .then(({ data }) => {
        setModelMetric(data);
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        setModelMetric(null);
        if (error.response?.status !== 404) {
          console.error("Could not load model metrics:", error);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setMetricLoading(false);
      });

    return () => {
      controller.abort();
    };
  }, [requestConfig]);

  useEffect(() => {
    const controller = new AbortController();
    axios
      .get(`${FRAUD_ALERTS_API_URL}/summary`, { ...requestConfig, signal: controller.signal })
      .then(({ data }) => setAlertSummary(data))
      .catch((error) => {
        if (!controller.signal.aborted) {
          setAlertSummary(null);
          console.error("Could not load fraud investigation summary:", error);
        }
      });

    return () => controller.abort();
  }, [requestConfig]);

  const loadDatasets = useCallback(async () => {
    try {
      const { data } = await axios.get(`${DATASET_API_URL}?size=100`, {
        headers: { Authorization: `Bearer ${session.token}` },
      });
      setDatasets(data.content);
    } catch (error) {
      setMessage(
        error.response?.status === 401
          ? "Your session expired. Please sign in again."
          : "Could not load datasets. Check that the backend and database are available.",
      );
      setMessageType("error");
    } finally {
      setLoading(false);
    }
  }, [session.token]);

  useEffect(() => {
    let active = true;
    axios
      .get(`${DATASET_API_URL}?size=100`, {
        headers: { Authorization: `Bearer ${session.token}` },
      })
      .then(({ data }) => {
        if (active) setDatasets(data.content);
      })
      .catch((error) => {
        if (!active) return;
        setMessage(
          error.response?.status === 401
            ? "Your session expired. Please sign in again."
            : "Could not load datasets. Check that the backend and database are available.",
        );
        setMessageType("error");
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [session.token]);

  const uploadDataset = async (event) => {
    event.preventDefault();
    if (!file) {
      setMessage("Choose a CSV file first.");
      setMessageType("error");
      return;
    }
    if (!file.name.toLowerCase().endsWith(".csv")) {
      setMessage("Only CSV files can be uploaded.");
      setMessageType("error");
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      setMessage("The maximum file size is 200 MB.");
      setMessageType("error");
      return;
    }

    setUploading(true);
    setMessage("");
    setMessageType("");
    let datasetId = uploadedDatasetId;
    if (!uploaded || datasetId === null) {
      try {
        const datasetBody = new FormData();
        datasetBody.append("file", file);
        const { data: dataset } = await axios.post(DATASET_API_URL, datasetBody, requestConfig);
        datasetId = dataset.id;
        setUploadedDatasetId(datasetId);
        setUploaded(true);
        await loadDatasets();
      } catch (error) {
        setMessage(error.response?.data?.detail || "Dataset upload failed. Check the CSV format and try again.");
        setMessageType("error");
        setUploading(false);
        return;
      }
    }

    try {
      setMessage("Dataset uploaded. Training and evaluating the model...");
      const trainingBody = new FormData();
      trainingBody.append("file", file);
      const { data: training } = await axios.post(MODEL_TRAINING_API_URL, trainingBody);
      const { metrics } = training;
      await axios.post(MODEL_METRICS_IMPORT_API_URL, {
        model_name: training.model_name,
        accuracy: metrics.accuracy,
        precision: metrics.precision,
        recall: metrics.recall,
        f1_score: metrics.f1_score,
        roc_auc: metrics.roc_auc,
        pr_auc: metrics.pr_auc,
      }, requestConfig);
      setMessage("Model evaluated. Saving its held-out transaction predictions...");
      const { data: predictionCsv } = await axios.get(
        `http://localhost:8000/api/models/${training.model_id}/test-predictions`,
        { responseType: "blob" },
      );
      const predictionBody = new FormData();
      predictionBody.append(
        "file",
        new File([predictionCsv], `test-predictions-${training.model_id}.csv`, { type: "text/csv" }),
      );
      const { data: importedPredictions } = await axios.post(
        `${DATASET_API_URL}/${datasetId}/predictions/import`,
        predictionBody,
        requestConfig,
      );
      await loadModelMetrics();
      axios
        .get(`${FRAUD_ALERTS_API_URL}/summary`, requestConfig)
        .then(({ data }) => setAlertSummary(data))
        .catch((error) => console.error("Could not refresh fraud investigation summary:", error));
      setTrainingComplete(true);
      setMessage(
        `Detection complete: ${training.model_name} scored ${importedPredictions.totalImported.toLocaleString()} held-out transactions. ${importedPredictions.fraudAlertsCreated.toLocaleString()} were flagged; evaluation metrics were saved.`,
      );
      setMessageType("success");
      setActivePage("investigations");
    } catch (error) {
      const detail = error.response?.data?.detail || error.message;
      setMessage(`Dataset uploaded, but training or saving its metrics failed: ${detail}`);
      setMessageType("error");
    } finally {
      setUploading(false);
    }
  };

  const showDetails = async (id) => {
    if (selectedDataset?.id === id) {
      setSelectedDataset(null);
      return;
    }
    try {
      const { data } = await axios.get(`${DATASET_API_URL}/${id}`, requestConfig);
      setSelectedDataset(data);
    } catch (error) {
      setMessage(error.response?.data?.detail || "Could not load dataset details.");
      setMessageType("error");
    }
  };

  const deleteDataset = async (dataset) => {
    if (!window.confirm(`Delete "${dataset.fileName}" and its stored file? This cannot be undone.`)) {
      return;
    }
    try {
      await axios.delete(`${DATASET_API_URL}/${dataset.id}`, requestConfig);
      setDatasets((current) => current.filter((item) => item.id !== dataset.id));
      if (selectedDataset?.id === dataset.id) {
        setSelectedDataset(null);
      }
      setMessage("Dataset deleted.");
      setMessageType("success");
    } catch (error) {
      setMessage(error.response?.data?.detail || "Could not delete the dataset.");
      setMessageType("error");
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return `${bytes} bytes`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const pageTitles = {
    overview: "Overview",
    datasets: "Dataset Management",
    monitoring: "Transaction Monitoring",
    performance: "Model Performance",
    investigations: "Fraud Investigation",
  };

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="sidebar-logo"><ShieldCheck size={23} /></div>
          <div>
            <strong>Sentinel</strong>
            <span>FRAUD INTELLIGENCE</span>
          </div>
        </div>

        <div className="sidebar-label">WORKSPACE</div>
        <nav className="sidebar-nav" aria-label="Main navigation">
          <button
            aria-current={activePage === "overview" ? "page" : undefined}
            className={activePage === "overview" ? "nav-item active" : "nav-item"}
            onClick={() => setActivePage("overview")}
            type="button"
          >
            <LayoutDashboard size={18} /> Overview
          </button>
          <button
            aria-current={activePage === "datasets" ? "page" : undefined}
            className={activePage === "datasets" ? "nav-item active" : "nav-item"}
            onClick={() => setActivePage("datasets")}
            type="button"
          >
            <Database size={18} /> Datasets
          </button>
          <button
            aria-current={activePage === "monitoring" ? "page" : undefined}
            className={activePage === "monitoring" ? "nav-item active" : "nav-item"}
            onClick={() => setActivePage("monitoring")}
            type="button"
          >
            <ScanSearch size={18} /> Transaction Monitor
          </button>
          <button
            aria-current={activePage === "performance" ? "page" : undefined}
            className={activePage === "performance" ? "nav-item active" : "nav-item"}
            onClick={() => setActivePage("performance")}
            type="button"
          >
            <BarChart3 size={18} /> Model Performance
          </button>
          <button
            aria-current={activePage === "investigations" ? "page" : undefined}
            className={activePage === "investigations" ? "nav-item active" : "nav-item"}
            onClick={() => setActivePage("investigations")}
            type="button"
          >
            <Search size={18} /> Investigations
          </button>
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-user">
            <div className="user-avatar">
              {(session.user.name || "U").charAt(0).toUpperCase()}
            </div>
            <div className="sidebar-user-info">
              <strong>{session.user.name}</strong>
              <span>Analyst account</span>
            </div>
          </div>
          <button className="sidebar-logout" onClick={onLogout} type="button">
            <LogOut size={17} /> Sign out
          </button>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div>
            <p className="topbar-overline">BANKING FRAUD DETECTION SYSTEM</p>
            <h1>{pageTitles[activePage]}</h1>
          </div>
          <div className="topbar-right">
            <span className="system-status"><i /> Backend workspace</span>
            <div className="topbar-avatar">
              {(session.user.name || "U").charAt(0).toUpperCase()}
            </div>
          </div>
        </header>

        {message && (
          <p className={`feedback workspace-feedback ${messageType}`} role="status">
            {message}
          </p>
        )}

        {activePage === "overview" && (
          <Suspense fallback={<p className="empty-state">Loading executive dashboard...</p>}>
            <ExecutiveDashboard
              datasets={datasets}
              metric={modelMetric}
              alertSummary={alertSummary}
              loading={loading || metricLoading}
              onOpenDatasets={() => setActivePage("datasets")}
              onOpenPerformance={() => setActivePage("performance")}
              onOpenInvestigations={() => setActivePage("investigations")}
            />
          </Suspense>
        )}

        {activePage === "performance" && (
          <Suspense fallback={<p className="empty-state">Loading dashboard...</p>}>
            <TrainingDashboard
              metric={modelMetric}
              loading={metricLoading}
              onRefresh={refreshModelMetrics}
            />
          </Suspense>
        )}

        {activePage === "datasets" && (
          <>
            <section className="workspace-panel upload-panel" aria-labelledby="upload-heading">
              <div>
                <h2 id="upload-heading">Upload a dataset</h2>
                <p>Choose a labeled CSV with Time, V1–V28, Amount, and Class. Training currently supports up to 50,000 rows.</p>
              </div>
              <form className="upload-form" onSubmit={uploadDataset}>
                <label className="file-picker">
                  <span>{file ? file.name : "Choose CSV file"}</span>
                  <input
                    accept=".csv,text/csv"
                    onChange={(event) => {
                      setFile(event.target.files?.[0] || null);
                      setUploaded(false);
                      setUploadedDatasetId(null);
                      setTrainingComplete(false);
                    }}
                    type="file"
                  />
                </label>
                <button
                  className="primary-button upload-button"
                  disabled={uploading || !file || trainingComplete}
                  type="submit"
                >
                  {uploading
                    ? "Uploading and training..."
                    : trainingComplete
                      ? "Training complete"
                      : uploaded
                        ? "Retry training"
                        : "Upload & train"}
                </button>
              </form>
            </section>

            <section className="workspace-panel" aria-labelledby="datasets-heading">
              <div className="section-heading">
                <div>
                  <h2 id="datasets-heading">Your datasets</h2>
                  <p>View and manage datasets uploaded to your account.</p>
                </div>
                <span className="dataset-count">{datasets.length} shown</span>
              </div>

              {loading ? (
                <p className="empty-state">Loading your datasets...</p>
              ) : datasets.length === 0 ? (
                <p className="empty-state">No datasets yet. Upload a CSV file to get started.</p>
              ) : (
                <div className="dataset-list">
                  {datasets.map((dataset) => (
                    <article className="dataset-row" key={dataset.id}>
                      <div className="dataset-file-icon" aria-hidden="true">CSV</div>
                      <div className="dataset-main">
                        <strong>{dataset.fileName}</strong>
                        <span>
                          {formatFileSize(dataset.fileSize)} · {dataset.totalRecords.toLocaleString()} records
                        </span>
                        <span>Uploaded {new Date(dataset.uploadDate).toLocaleString()}</span>
                        {selectedDataset?.id === dataset.id && (
                          <div className="dataset-details">
                            <span>Processing status: {selectedDataset.status}</span>
                            <span>Detected fraud cases: {selectedDataset.fraudRecords.toLocaleString()}</span>
                            <span>Genuine cases: {selectedDataset.genuineRecords.toLocaleString()}</span>
                          </div>
                        )}
                      </div>
                      <span className={`status-pill status-${dataset.status.toLowerCase()}`}>
                        {dataset.status}
                      </span>
                      <div className="dataset-actions">
                        <button
                          className="text-button"
                          type="button"
                          onClick={() => showDetails(dataset.id)}
                        >
                          {selectedDataset?.id === dataset.id ? "Hide details" : "Details"}
                        </button>
                        <button
                          className="text-button delete-button"
                          type="button"
                          onClick={() => deleteDataset(dataset)}
                        >
                          Delete
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </>
        )}

        {activePage === "monitoring" && (
          <Suspense fallback={<p className="empty-state">Loading transaction monitoring...</p>}>
            <TransactionMonitoring session={session} />
          </Suspense>
        )}

        {activePage === "investigations" && (
          <Suspense fallback={<p className="empty-state">Loading fraud investigations...</p>}>
            <FraudInvestigation session={session} />
          </Suspense>
        )}

        <footer className="main-footer">
          <span>Sentinel · Banking Fraud Intelligence</span>
          <span>Machine learning evaluation workspace</span>
        </footer>
      </main>
    </div>
  );
}

function App() {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [loading, setLoading] = useState(false);
  const [session, setSession] = useState(null);

  const isRegistering = mode === "register";

  const updateForm = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const switchMode = (nextMode) => {
    setMode(nextMode);
    setMessage("");
    setMessageType("");
  };

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setMessage("");
    setMessageType("");

    try {
      if (isRegistering) {
        await axios.post(`${API_URL}/register`, {
          name: form.name,
          email: form.email,
          password: form.password,
        });
        setForm((current) => ({ ...current, name: "", password: "" }));
        setMode("login");
        setMessage("Account created. Sign in with your email and password.");
        setMessageType("success");
      } else {
        const { data } = await axios.post(`${API_URL}/login`, {
          email: form.email,
          password: form.password,
        });
        setSession({ token: data.accessToken, user: data.user });
        setForm((current) => ({ ...current, password: "" }));
      }
    } catch (error) {
      const detail = error.response?.data?.detail;
      setMessage(
        detail ||
          (error.response
            ? "The request could not be completed. Check the information and try again."
            : "Could not reach the backend. Check that Spring Boot is running on port 8080."),
      );
      setMessageType("error");
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    setSession(null);
    setForm({ name: "", email: "", password: "" });
    setMessage("You have been signed out.");
    setMessageType("success");
    setMode("login");
  };

  if (session) {
    return <DatasetWorkspace session={session} onLogout={logout} />;
  }

  return (
    <main className="app-shell">
      <section className="auth-card" aria-labelledby="auth-heading">
        <div className="brand-mark" aria-hidden="true">B</div>
        <p className="eyebrow">BANKING FRAUD DETECTION</p>
        <h1 id="auth-heading">{isRegistering ? "Create your account" : "Welcome back"}</h1>
        <p className="intro">
          {isRegistering
            ? "Register to access the fraud detection workspace."
            : "Sign in to continue to your fraud detection workspace."}
        </p>

        <div className="mode-switch" role="tablist" aria-label="Account action">
          <button
            type="button"
            role="tab"
            aria-selected={!isRegistering}
            className={!isRegistering ? "active" : ""}
            onClick={() => switchMode("login")}
          >
            Sign in
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={isRegistering}
            className={isRegistering ? "active" : ""}
            onClick={() => switchMode("register")}
          >
            Register
          </button>
        </div>

        <form onSubmit={submit}>
          {isRegistering && (
            <label>
              Full name
              <input
                autoComplete="name"
                maxLength={100}
                name="name"
                onChange={updateForm}
                placeholder="Your name"
                required
                type="text"
                value={form.name}
              />
            </label>
          )}
          <label>
            Email address
            <input
              autoComplete="email"
              maxLength={255}
              name="email"
              onChange={updateForm}
              placeholder="you@example.com"
              required
              type="email"
              value={form.email}
            />
          </label>
          <label>
            Password
            <input
              autoComplete={isRegistering ? "new-password" : "current-password"}
              maxLength={72}
              minLength={isRegistering ? 8 : undefined}
              name="password"
              onChange={updateForm}
              placeholder={isRegistering ? "At least 8 characters" : "Enter your password"}
              required
              type="password"
              value={form.password}
            />
          </label>

          {message && (
            <p className={`feedback ${messageType}`} role="status">
              {message}
            </p>
          )}

          <button className="primary-button" disabled={loading} type="submit">
            {loading
              ? "Please wait..."
              : isRegistering
                ? "Create account"
                : "Sign in"}
          </button>
        </form>

        <p className="security-note">
          Your password is sent securely to the backend for verification and is never stored in this browser.
        </p>
      </section>
    </main>
  );
}

export default App;
