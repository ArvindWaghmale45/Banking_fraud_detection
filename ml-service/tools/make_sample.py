from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd

EXPECTED_COLUMNS = {"time", *(f"v{index}" for index in range(1, 29)), "amount", "class"}


def create_sample(source: Path, destination: Path, rows: int = 20_000, seed: int = 42) -> int:
    if not source.is_file():
        raise FileNotFoundError(f"Dataset file not found: {source}")
    if destination.exists():
        raise FileExistsError(f"Output already exists: {destination}. Remove it or choose another name.")
    if rows < 4:
        raise ValueError("Sample size must be at least four rows.")

    dataset = pd.read_csv(source)
    dataset.columns = [str(column).strip().lower() for column in dataset.columns]
    if len(dataset.columns) != len(EXPECTED_COLUMNS) or set(dataset.columns) != EXPECTED_COLUMNS:
        raise ValueError("Source CSV must contain Time, V1-V28, Amount, and Class columns.")
    if not dataset["class"].isin([0, 1]).all():
        raise ValueError("Class values must be 0 (genuine) or 1 (fraud).")

    fraud = dataset[dataset["class"] == 1]
    genuine = dataset[dataset["class"] == 0]
    if len(fraud) < 2 or len(genuine) < 2:
        raise ValueError("Source must contain at least two fraud and two genuine records.")
    if rows > len(dataset):
        raise ValueError(f"Requested {rows:,} rows, but source only has {len(dataset):,}.")

    fraud_rows = max(2, round(rows * len(fraud) / len(dataset)))
    fraud_rows = min(fraud_rows, rows - 2, len(fraud))
    genuine_rows = rows - fraud_rows
    if genuine_rows > len(genuine):
        genuine_rows = len(genuine)
        fraud_rows = rows - genuine_rows

    sample = pd.concat(
        [
            fraud.sample(n=fraud_rows, random_state=seed),
            genuine.sample(n=genuine_rows, random_state=seed),
        ],
        ignore_index=True,
    ).sample(frac=1, random_state=seed)

    destination.parent.mkdir(parents=True, exist_ok=True)
    sample.to_csv(destination, index=False)
    return len(sample)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Create a reproducible, fraud-stratified CSV sample for local model training."
    )
    parser.add_argument("source", type=Path, help="Path to the extracted Kaggle creditcard.csv")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("ml-service") / "data" / "creditcard_sample.csv",
        help="Output CSV path (default: ml-service/data/creditcard_sample.csv)",
    )
    parser.add_argument("--rows", type=int, default=20_000, help="Number of sample rows (default: 20000)")
    parser.add_argument("--seed", type=int, default=42, help="Sampling seed (default: 42)")
    arguments = parser.parse_args()

    count = create_sample(arguments.source, arguments.output, arguments.rows, arguments.seed)
    print(f"Created {count:,} rows at {arguments.output.resolve()}")
    print("The sample preserves the source dataset's fraud/genuine ratio approximately.")


if __name__ == "__main__":
    main()
