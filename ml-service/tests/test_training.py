from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from io import BytesIO
from unittest.mock import patch

import numpy as np
import pandas as pd
from fastapi import HTTPException, UploadFile
from fastapi.testclient import TestClient

from app.main import (
    FEATURE_COLUMNS,
    MAX_TRAINING_ROWS,
    download_test_predictions,
    train_model,
    train_sample,
)
from tools.make_sample import create_sample
from app.main import app


def make_dataset(rows: int = 80) -> pd.DataFrame:
    random = np.random.default_rng(42)
    labels = np.array([0] * (rows - 8) + [1] * 8)
    random.shuffle(labels)
    values = random.normal(size=(rows, len(FEATURE_COLUMNS)))
    values[:, 0] = np.arange(rows)
    values[:, -1] = random.uniform(1, 500, size=rows)
    values[labels == 1, 1] += 2.5
    frame = pd.DataFrame(values, columns=FEATURE_COLUMNS)
    frame["class"] = labels
    return frame


class TrainingTests(unittest.TestCase):
    def test_training_returns_held_out_metrics_and_persists_model(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            result, artifact_path = train_model(make_dataset(), Path(temporary_directory))

            self.assertEqual(80, result["total_records"])
            self.assertEqual(60, result["train_records"])
            self.assertEqual(20, result["test_records"])
            self.assertEqual(8, result["fraud_records"])
            self.assertTrue(artifact_path.is_file())
            self.assertEqual(6, len(result["metrics"]))
            self.assertEqual(20, sum(result["confusion_matrix"].values()))
            self.assertTrue(0 <= result["metrics"]["pr_auc"] <= 1)
            predictions_path = artifact_path.with_name(
                f"{artifact_path.stem}_test_predictions.csv"
            )
            self.assertTrue(predictions_path.is_file())
            predictions = pd.read_csv(predictions_path)
            self.assertEqual(20, len(predictions))
            expected_import_columns = {
                "Time",
                "Amount",
                "Actual_Class",
                "Fraud_Probability",
                "Predicted_Class",
                "Prediction_Status",
                *(f"V{index}" for index in range(1, 29)),
            }
            self.assertEqual(expected_import_columns, set(predictions.columns))
            self.assertTrue(predictions["Fraud_Probability"].between(0, 1).all())
            with patch.dict("os.environ", {"FRAUD_MODEL_DIR": temporary_directory}):
                self.assertEqual(
                    str(predictions_path),
                    str(download_test_predictions(artifact_path.stem).path),
                )

    def test_training_rejects_missing_fraud_or_genuine_class(self) -> None:
        dataset = make_dataset()
        dataset["class"] = 0

        with self.assertRaisesRegex(ValueError, "at least two genuine and two fraud"):
            train_model(dataset, Path(tempfile.gettempdir()))

    def test_training_rejects_samples_above_row_limit(self) -> None:
        dataset = make_dataset(MAX_TRAINING_ROWS + 1)

        with self.assertRaisesRegex(ValueError, "limited to"):
            train_model(dataset, Path(tempfile.gettempdir()))

    def test_sample_creator_preserves_both_classes_and_refuses_overwrite(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            source = Path(temporary_directory) / "source.csv"
            destination = Path(temporary_directory) / "sample.csv"
            make_dataset(100).to_csv(source, index=False)

            count = create_sample(source, destination, rows=40)

            self.assertEqual(40, count)
            sample = pd.read_csv(destination)
            self.assertEqual(40, len(sample))
            self.assertEqual({0, 1}, set(sample["class"]))
            with self.assertRaises(FileExistsError):
                create_sample(source, destination, rows=40)

    def test_training_endpoint_returns_measured_results_for_csv(self) -> None:
        csv_bytes = make_dataset().to_csv(index=False).encode("utf-8")
        upload = UploadFile(
            file=BytesIO(csv_bytes),
            size=len(csv_bytes),
            filename="creditcard_sample.csv",
        )

        with tempfile.TemporaryDirectory() as temporary_directory:
            with patch.dict("os.environ", {"FRAUD_MODEL_DIR": temporary_directory}):
                result = train_sample(upload)

            self.assertEqual(80, result.total_records)
            self.assertEqual(8, result.fraud_records)
            self.assertGreaterEqual(result.metrics["recall"], 0)
            self.assertEqual(20, sum(result.confusion_matrix.values()))

    def test_training_endpoint_rejects_non_csv_upload(self) -> None:
        upload = UploadFile(file=BytesIO(b"data"), size=4, filename="transactions.txt")

        with self.assertRaises(HTTPException) as raised:
            train_sample(upload)

        self.assertEqual(400, raised.exception.status_code)

    def test_saved_model_predict_matches_exported_probability(self) -> None:
        sample = pd.read_csv(
            Path(__file__).resolve().parents[1] / "outputs" / "test_predictions_sample.csv"
        ).iloc[0]
        payload = {"time": float(sample["Time"]), "amount": float(sample["Amount"])}
        payload.update({f"v{index}": float(sample[f"V{index}"]) for index in range(1, 29)})

        with TestClient(app) as client:
            response = client.post("/predict", json=payload)

        self.assertEqual(200, response.status_code)
        body = response.json()
        self.assertAlmostEqual(body["fraud_probability"], float(sample["Fraud_Probability"]), places=12)
        self.assertEqual(body["prediction"], "Genuine")
        self.assertEqual(body["threshold"], 0.49539234499074425)

    def test_saved_model_model_info_and_feature_importance(self) -> None:
        with TestClient(app) as client:
            model_info = client.get("/model-info")
            importance = client.get("/feature-importance")

        self.assertEqual(200, model_info.status_code)
        self.assertEqual("Random Forest", model_info.json()["model_name"])
        self.assertEqual(200, importance.status_code)
        self.assertEqual("V14", importance.json()[0]["Feature"])


if __name__ == "__main__":
    unittest.main()
