from __future__ import annotations

import os
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Annotated, Any

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, ConfigDict, Field, AliasChoices
from fastapi.responses import FileResponse
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    average_precision_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

MAX_FILE_SIZE_BYTES = 200 * 1024 * 1024
MAX_TRAINING_ROWS = 50_000
TEST_SIZE = 0.25
RANDOM_STATE = 42
FEATURE_COLUMNS = ["time", *(f"v{index}" for index in range(1, 29)), "amount"]
REQUIRED_COLUMNS = {*FEATURE_COLUMNS, "class"}
MODEL_DIR = Path(__file__).resolve().parents[1] / "models"
OUTPUT_DIR = Path(__file__).resolve().parents[1] / "outputs"
SAVED_MODEL_PATH = MODEL_DIR / "random_forest_fraud_model.joblib"
SAVED_METRICS_PATH = OUTPUT_DIR / "model_metrics.json"
SAVED_FEATURES_PATH = OUTPUT_DIR / "feature_importance.csv"
SAVED_FEATURE_COLUMNS = ["Time", *(f"V{index}" for index in range(1, 29)), "Amount"]
SAVED_MODEL = None
SAVED_METRICS: dict[str, Any] = {}

app = FastAPI(title="Fraud Detection ML Service", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)


class TrainingResponse(BaseModel):
    model_id: str
    model_name: str
    model_version: str
    trained_at: datetime
    total_records: int
    train_records: int
    test_records: int
    fraud_records: int
    genuine_records: int
    fraud_rate: float
    metrics: dict[str, float]
    confusion_matrix: dict[str, int]
    amount_by_class: list[dict[str, Any]]


class PredictionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    time: float = Field(validation_alias=AliasChoices("time", "Time"), ge=0)
    amount: float = Field(validation_alias=AliasChoices("amount", "Amount"), ge=0)
    v1: float
    v2: float
    v3: float
    v4: float
    v5: float
    v6: float
    v7: float
    v8: float
    v9: float
    v10: float
    v11: float
    v12: float
    v13: float
    v14: float
    v15: float
    v16: float
    v17: float
    v18: float
    v19: float
    v20: float
    v21: float
    v22: float
    v23: float
    v24: float
    v25: float
    v26: float
    v27: float
    v28: float

    def feature_frame(self) -> pd.DataFrame:
        values = {
            "Time": self.time,
            **{f"V{index}": getattr(self, f"v{index}") for index in range(1, 29)},
            "Amount": self.amount,
        }
        return pd.DataFrame([[values[column] for column in SAVED_FEATURE_COLUMNS]], columns=SAVED_FEATURE_COLUMNS)


class PredictionResponse(BaseModel):
    fraud_probability: float
    prediction: str
    threshold: float
    risk_level: str


def _validate_frame(frame: pd.DataFrame) -> pd.DataFrame:
    normalized_columns = [str(column).strip().lower() for column in frame.columns]
    if normalized_columns and normalized_columns[0].startswith("\ufeff"):
        normalized_columns[0] = normalized_columns[0].lstrip("\ufeff")
    if len(normalized_columns) != len(set(normalized_columns)):
        raise ValueError("CSV contains duplicate column names.")
    if set(normalized_columns) != REQUIRED_COLUMNS or len(normalized_columns) != len(REQUIRED_COLUMNS):
        raise ValueError("CSV must contain Time, V1-V28, Amount, and Class columns.")
    if len(frame) > MAX_TRAINING_ROWS:
        raise ValueError(
            f"Training samples are limited to {MAX_TRAINING_ROWS:,} rows. "
            "Create a smaller stratified sample first."
        )

    frame = frame.copy()
    frame.columns = normalized_columns
    frame = frame[FEATURE_COLUMNS + ["class"]]
    for column in FEATURE_COLUMNS + ["class"]:
        frame[column] = pd.to_numeric(frame[column], errors="coerce")

    if frame.empty:
        raise ValueError("CSV must contain transaction rows.")
    if frame.isna().any().any():
        raise ValueError("CSV contains empty or non-numeric feature values.")
    if not np.isfinite(frame.to_numpy(dtype=float)).all():
        raise ValueError("CSV contains non-finite numeric values.")
    if not frame["class"].isin([0, 1]).all():
        raise ValueError("Class values must be 0 (genuine) or 1 (fraud).")
    class_counts = frame["class"].value_counts()
    if any(int(class_counts.get(label, 0)) < 2 for label in (0, 1)):
        raise ValueError("The sample must contain at least two genuine and two fraud records.")

    return frame


def train_model(frame: pd.DataFrame, model_directory: Path) -> tuple[dict[str, Any], Path]:
    validated = _validate_frame(frame)
    labels = validated["class"].astype(int)
    features = validated[FEATURE_COLUMNS].astype(float)
    x_train, x_test, y_train, y_test = train_test_split(
        features,
        labels,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
        stratify=labels,
    )

    model = Pipeline(
        [
            ("scaler", StandardScaler()),
            (
                "classifier",
                LogisticRegression(
                    class_weight="balanced",
                    max_iter=1_000,
                    random_state=RANDOM_STATE,
                ),
            ),
        ]
    )
    model.fit(x_train, y_train)
    probabilities = model.predict_proba(x_test)[:, 1]
    predictions = (probabilities >= 0.5).astype(int)
    tn, fp, fn, tp = confusion_matrix(y_test, predictions, labels=[0, 1]).ravel()

    metrics = {
        "accuracy": float(accuracy_score(y_test, predictions)),
        "precision": float(precision_score(y_test, predictions, zero_division=0)),
        "recall": float(recall_score(y_test, predictions, zero_division=0)),
        "f1_score": float(f1_score(y_test, predictions, zero_division=0)),
        "roc_auc": float(roc_auc_score(y_test, probabilities)),
        "pr_auc": float(average_precision_score(y_test, probabilities)),
    }

    amount_frame = validated[["amount", "class"]].copy()
    amount_frame["label"] = amount_frame["class"].map({0: "Genuine", 1: "Fraud"})
    amount_by_class = (
        amount_frame.groupby("label", sort=False)["amount"]
        .agg(["count", "mean", "median"])
        .reset_index()
        .to_dict(orient="records")
    )
    model_id = str(uuid.uuid4())
    model_directory.mkdir(parents=True, exist_ok=True)
    artifact_path = model_directory / f"{model_id}.joblib"
    joblib.dump(model, artifact_path)
    test_predictions = x_test.rename(
        columns={
            "time": "Time",
            "amount": "Amount",
            **{f"v{index}": f"V{index}" for index in range(1, 29)},
        }
    ).copy()
    test_predictions["Actual_Class"] = y_test.astype(int)
    test_predictions["Fraud_Probability"] = probabilities
    test_predictions["Predicted_Class"] = predictions
    test_predictions["Prediction_Status"] = np.where(predictions == 1, "Suspicious", "Genuine")
    test_predictions_path = model_directory / f"{model_id}_test_predictions.csv"
    test_predictions.to_csv(test_predictions_path, index=False)

    result: dict[str, Any] = {
        "model_id": model_id,
        "model_name": "Logistic Regression (class-weight balanced)",
        "model_version": "baseline-1",
        "trained_at": datetime.now(timezone.utc),
        "total_records": int(len(validated)),
        "train_records": int(len(x_train)),
        "test_records": int(len(x_test)),
        "fraud_records": int(labels.sum()),
        "genuine_records": int(len(labels) - labels.sum()),
        "fraud_rate": float(labels.mean()),
        "metrics": metrics,
        "confusion_matrix": {
            "true_negative": int(tn),
            "false_positive": int(fp),
            "false_negative": int(fn),
            "true_positive": int(tp),
        },
        "amount_by_class": amount_by_class,
    }
    return result, artifact_path


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "ok",
        "service": "fraud-detection-ml",
        "saved_model_loaded": str(SAVED_MODEL is not None).lower(),
    }


@app.on_event("startup")
def load_saved_model() -> None:
    global SAVED_MODEL, SAVED_METRICS
    if not SAVED_MODEL_PATH.is_file():
        raise RuntimeError(f"Saved Random Forest model not found: {SAVED_MODEL_PATH}")
    if not SAVED_METRICS_PATH.is_file():
        raise RuntimeError(f"Saved model metrics not found: {SAVED_METRICS_PATH}")
    SAVED_MODEL = joblib.load(SAVED_MODEL_PATH)
    import json

    SAVED_METRICS = json.loads(SAVED_METRICS_PATH.read_text(encoding="utf-8"))


def score_saved_request(request: PredictionRequest) -> PredictionResponse:
    if SAVED_MODEL is None:
        raise HTTPException(status_code=503, detail="Saved Random Forest model is not loaded.")
    threshold = float(SAVED_METRICS.get("threshold", 0.49539234499074425))
    probability = float(SAVED_MODEL.predict_proba(request.feature_frame())[0, 1])
    prediction = "Suspicious" if probability >= threshold else "Genuine"
    risk_level = "HIGH" if probability >= 0.8 else "MEDIUM" if probability >= threshold else "LOW"
    return PredictionResponse(
        fraud_probability=probability,
        prediction=prediction,
        threshold=threshold,
        risk_level=risk_level,
    )


@app.get("/model-info")
def model_info() -> dict[str, Any]:
    return SAVED_METRICS


@app.get("/feature-importance")
def feature_importance() -> list[dict[str, Any]]:
    if not SAVED_FEATURES_PATH.is_file():
        raise HTTPException(status_code=404, detail="Feature importance file not found.")
    return pd.read_csv(SAVED_FEATURES_PATH).to_dict(orient="records")


@app.post("/predict", response_model=PredictionResponse)
def predict_saved_model(request: PredictionRequest) -> PredictionResponse:
    return score_saved_request(request)


@app.post("/predict-batch", response_model=list[PredictionResponse])
def predict_saved_model_batch(requests: list[PredictionRequest]) -> list[PredictionResponse]:
    if not requests:
        raise HTTPException(status_code=400, detail="Provide at least one transaction.")
    if len(requests) > 10_000:
        raise HTTPException(status_code=413, detail="Batch size is limited to 10,000 transactions.")
    return [score_saved_request(request) for request in requests]


@app.get("/api/models/{model_id}/test-predictions")
def download_test_predictions(model_id: str) -> FileResponse:
    try:
        normalized_model_id = str(uuid.UUID(model_id))
    except ValueError as exception:
        raise HTTPException(status_code=404, detail="Model not found.") from exception

    model_directory = Path(
        os.environ.get(
            "FRAUD_MODEL_DIR",
            str(Path.home() / ".banking-fraud-detection" / "models"),
        )
    )
    predictions_path = model_directory / f"{normalized_model_id}_test_predictions.csv"
    if not predictions_path.is_file():
        raise HTTPException(status_code=404, detail="Test predictions not found for this model.")

    return FileResponse(
        predictions_path,
        media_type="text/csv",
        filename=f"{normalized_model_id}_test_predictions.csv",
    )


@app.post("/api/train", response_model=TrainingResponse)
def train_sample(file: Annotated[UploadFile, File()]) -> TrainingResponse:
    filename = Path(file.filename or "").name
    if not filename.lower().endswith(".csv"):
        raise HTTPException(status_code=400, detail="Upload a .csv file.")
    if file.size is None or file.size == 0:
        raise HTTPException(status_code=400, detail="Choose a non-empty CSV file.")
    if file.size > MAX_FILE_SIZE_BYTES:
        raise HTTPException(status_code=413, detail="The maximum training sample size is 200 MB.")

    try:
        frame = pd.read_csv(file.file, nrows=MAX_TRAINING_ROWS + 1)
        model_directory = Path(
            os.environ.get(
                "FRAUD_MODEL_DIR",
                str(Path.home() / ".banking-fraud-detection" / "models"),
            )
        )
        result, _artifact_path = train_model(frame, model_directory)
        return TrainingResponse(**result)
    except (pd.errors.EmptyDataError, pd.errors.ParserError, UnicodeDecodeError) as exception:
        raise HTTPException(status_code=400, detail="The uploaded file is not a readable CSV.") from exception
    except ValueError as exception:
        raise HTTPException(status_code=400, detail=str(exception)) from exception
    except OSError as exception:
        raise HTTPException(status_code=500, detail="Could not save the trained model.") from exception
    finally:
        file.file.close()
