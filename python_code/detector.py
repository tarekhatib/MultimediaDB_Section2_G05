from ultralytics import YOLO

MODEL_NAME = "yolov8n.pt"
_model = None


def get_model():
    """Load YOLO once and reuse it (loading is slow)."""
    global _model
    if _model is None:
        _model = YOLO(MODEL_NAME)
    return _model


def detect(image_path, conf_threshold=0.25):
    """Return a list of {"label", "confidence", "bbox"} dicts."""
    model = get_model()

    # TODO 1: run the model (verbose=False keeps logs out of stdout)
    results = model(image_path, conf=conf_threshold, verbose=False)

    # TODO 2: one image in, so take the first result and loop over its boxes
    r = results[0]
    detections = []
    for box in r.boxes:
        # TODO 3: convert tensors to plain Python values
        class_id = int(box.cls[0])
        confidence = float(box.conf[0])
        bbox = box.xyxy[0].tolist()

        detections.append({
            "label": r.names[class_id],
            "confidence": round(confidence, 4),
            "bbox": [round(v, 1) for v in bbox],
        })

    # TODO 4: hand the list back
    return detections