# Test Receipt Images

This directory contains sample receipt images for OCR accuracy testing.

## Structure

```
test_receipts/
├── ground_truth.json          # Expected OCR results for each receipt
├── receipt_001.jpg            # Sample receipt 1
├── receipt_002.jpg            # Sample receipt 2
├── receipt_003.jpg            # Sample receipt 3
└── ...
```

## Adding Test Receipts

### 1. Add Receipt Images

Place real receipt images (JPG or PNG) in this directory. Name them sequentially:
- `receipt_001.jpg`
- `receipt_002.jpg`
- etc.

### 2. Update Ground Truth

For each receipt image, add an entry to `ground_truth.json` with the expected extracted values:

```json
{
  "filename": "receipt_001.jpg",
  "payee": "Starbucks Coffee",
  "amount": 14.70,
  "date": "2024-01-15",
  "currency": "USD",
  "tax": 1.32,
  "lineItems": [
    {
      "description": "Latte",
      "quantity": 1,
      "unitPrice": 4.50,
      "totalPrice": 4.50
    },
    {
      "description": "Croissant",
      "quantity": 1,
      "unitPrice": 3.25,
      "totalPrice": 3.25
    }
  ]
}
```

## Guidelines

### Receipt Quality
- Use clear, well-lit images
- Avoid blurry or damaged receipts
- Include variety: different stores, formats, currencies

### Test Coverage
- **Common stores**: Grocery, coffee shops, restaurants
- **Edge cases**: Faded receipts, handwritten totals, foreign currencies
- **Formats**: Thermal paper, printed receipts, digital receipts

### Privacy
- Remove or redact personal information
- Use test/sample receipts when possible
- Never commit real payment card numbers

## Running Accuracy Tests

```bash
./gradlew app:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.ynab.receiptscanner.evaluation.AccuracyEvaluationTest
```

## Expected Metrics

Target accuracy:
- **Payee matching**: 85%+
- **Amount matching**: 90%+
- **Date matching**: 85%+
- **Overall confidence**: 0.80+

## Notes

- This directory is excluded from version control for large images
- Keep test set under 50 images for reasonable test time
- Update ground truth when parser logic changes
