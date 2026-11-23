import argparse
import contextlib
import json
import sys

from detector import MODEL_NAME, detect


def run_detect(args):
    # Anything printed by libraries during detection goes to stderr,
    # so stdout stays clean for the JSON result.
    with contextlib.redirect_stdout(sys.stderr):
        detections = detect(args.input, conf_threshold=args.conf)
    return {
        "success": True,
        "input": args.input,
        "model": MODEL_NAME,
        "detections": detections,
        "error": None,
    }


def main():
    parser = argparse.ArgumentParser(description="MDIP Python worker")
    sub = parser.add_subparsers(dest="command", required=True)

    p_detect = sub.add_parser("detect", help="run YOLO on one image")
    p_detect.add_argument("--input", required=True)
    p_detect.add_argument("--conf", type=float, default=0.25)

    args = parser.parse_args()

    try:
        if args.command == "detect":
            result = run_detect(args)
        print(json.dumps(result))
        return 0
    except Exception as exc:
        print(f"worker error: {exc}", file=sys.stderr)
        print(json.dumps({"success": False, "input": getattr(args, "input", None),
                          "detections": [], "error": str(exc)}))
        return 1


if __name__ == "__main__":
    sys.exit(main())