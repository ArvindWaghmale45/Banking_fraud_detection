## User Input

> Implement the user's requested Sentinel banking fraud intelligence project in the existing workspace `C:\Banking-Fraud-Detection`, preserving the existing React/Vite frontend, Spring Boot Java backend, and Python FastAPI ML service (do not replace the stack). The requested product modules are: (1) Executive Dashboard: total processed, genuine vs fraud, fraud detection rate, amount analysis, trends over time; (2) Dataset Management: upload CSV, validate, process large datasets with Apache Spark, show processing history; (3) Machine Learning Center: train Random Forest and Logistic Regression, compare them, handle class imbalance, show precision/recall/F1/PR-AUC, persist and load trained models; (4) Transaction Monitoring: score individual/batch transactions with fraud probability and risk levels, filter suspicious transactions; (5) Fraud Investigation: list flagged transactions, transaction details, investigation status, export report.
>
> Repository facts observed: frontend is React in `frontend/src/App.jsx`; it uploads CSVs to `POST http://localhost:8080/api/datasets` and gets latest saved dashboard metrics from `/api/model-metrics/latest`; backend is Spring Boot under `backend`, has Dataset/Transaction/Prediction/FraudAlert/ModelMetric entities, a dataset upload endpoint that only stores the file and marks status UPLOADED, and prediction import endpoint that imports a pre-scored CSV with hard-coded model name Random Forest. The frontend Alerts page is a placeholder. The Python ML service under `ml-service/app/main.py` currently accepts only labeled `Time,V1..V28,Amount,Class` CSV up to 50k rows, trains a class-weighted LogisticRegression pipeline, saves a joblib model, and returns real held-out metrics; frontend upload does not call it. Thus the current UI's Random Forest metrics can be stale/unrelated.
>
> Deliver an end-to-end implementation matching the requested modules without fake/stub metrics or silent fallbacks. Use Spark for dataset processing in a suitable local development mode, and update dependency manifests/docs as required. Keep CSV compatibility explicit: implement the available credit-card schema as the initial supported format; don't pretend arbitrary CSV schemas work. Make training/model results actual and connected to the UI/DB; provide actual predictions and persist them so dashboard/alerts/investigation surfaces use data. Include input validation, clear errors, persistence, and focused tests. Preserve existing UX and avoid unrelated changes. Work through analyze/design/implementation, honor boundaries/quality gates, and run targeted builds/tests. If an entire module is too large for this turn, complete a coherent vertical slice and report remaining work explicitly rather than adding fake screens.

**Project started**: 2026-11-18T05:15:04Z

## Tasks

### Phase: Analysis
- 🔄 t1 [pm] Inventory existing Sentinel behaviors and define module acceptance criteria (dispatched 2026-11-18T05:27:03Z)
- 🔄 t2 [architect] Analyze the existing cross-service architecture and data flows (dispatched 2026-11-18T05:27:03Z)

### Phase: Design
- ⏳ t3 [dba] Design persistent dataset, model metric, prediction, and investigation data changes [deps: t1, t2]

### Phase: Plan and Test Strategy
- ⏳ t4 [teamlead] Create implementation plan, task breakdown, and focused test strategy [deps: t1, t2, t3]

### Phase: Plan Quality Gate
- ⏳ t5 [teamlead] Validate plan coverage, traceability, sequencing, and test readiness [deps: t4]

⏳ [Execute + Validate phases — pending deep planning completion]
