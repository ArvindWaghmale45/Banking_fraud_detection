# Local fraud-model baseline

This service trains a real, small scikit-learn baseline on an uploaded CSV. It is
for local project development, not a production model or a claim that fraud is
detected reliably. The baseline uses a stratified 75/25 train/test split,
standardized features, and class-weighted logistic regression. Metrics are
measured only on the held-out test split.

## Get and prepare the sample data

1. Download the **Credit Card Fraud Detection** dataset from
   [Kaggle (ULB)](https://www.kaggle.com/datasets/mlg-ulb/creditcardfraud).
   Kaggle requires an account.
2. Extract the ZIP. Locate the file named `creditcard.csv`.
3. From the repository root, create a 20,000-row stratified sample:

   ```powershell
   & "C:\Program Files\Python313\python.exe" .\ml-service\tools\make_sample.py "C:\path\to\creditcard.csv"
   ```

   The command creates `ml-service\data\creditcard_sample.csv`. To choose another output
   file or sample size, use `--output` or `--rows`.
4. Upload that sample in the web app. Expected headers are `Time`, `V1` through
   `V28`, `Amount`, and `Class`.

The sample is selected from the real Kaggle dataset and keeps both fraud and
genuine records. It is suitable for a local baseline, not a distributed big-data
benchmark. Training uploads are limited to 200 MB and 50,000 rows.

## Run the ML service

In a new PowerShell terminal:

```powershell
Set-Location C:\Banking-Fraud-Detection
& "C:\Program Files\Python313\python.exe" -m pip install -r .\ml-service\requirements.txt
& "C:\Program Files\Python313\python.exe" -m uvicorn app.main:app --app-dir .\ml-service --host 127.0.0.1 --port 8000
```

Keep this terminal open along with the Spring Boot and Vite terminals. The ML
service is intentionally bound to localhost for development. It saves the
trained model under `%USERPROFILE%\.banking-fraud-detection\models`.

`GET http://localhost:8000/health` reports whether the service is available.

After both the Spring Boot backend and ML service are running, upload a labeled
sample from the web app's **Datasets** page with **Upload & train**. The ML
service fits the class-weighted Logistic Regression model on the training split,
evaluates it on the held-out split, saves the model and held-out predictions,
and returns the evaluation metrics. The web app imports those held-out
predictions into the backend so predicted fraud cases appear as investigation
alerts. This is a local, labeled-dataset evaluation workflow; it does not yet
score new unlabeled transactions or process the full 284,807-row dataset in one
training run.
