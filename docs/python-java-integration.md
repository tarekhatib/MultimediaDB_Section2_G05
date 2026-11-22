# Java–Python Integration Contract

## Approach
Java launches Python using `ProcessBuilder`. No network service is required for the initial version.

## Commands (proposed)
- `python worker.py detect --input <path>`
- `python worker.py attack --input <path> --target <label> --reference <path> --epsilon <value> --seed <int>`

## JSON response (proposed)
```json
{
  "success": true,
  "input": "relative/path/input.png",
  "output": null,
  "detections": [
    {"label": "car", "confidence": 0.94, "bbox": [10, 20, 200, 180]}
  ],
  "metrics": {},
  "error": null
}
```

## Rules
- stdout contains JSON only; diagnostics go to stderr.
- Non-zero process exit code means failure.
- Pass executable and each argument separately to ProcessBuilder; do not concatenate a shell command.
- Use paths as arguments, not hard-coded machine-specific paths.
- Record model/version, random seed and all relevant parameters.
- Finalize field names with both owners before implementation; do not silently change the contract.
