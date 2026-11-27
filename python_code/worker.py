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


def run_attack_command(args):
    from attack import run_attack      # imported here so `detect` stays fast
    with contextlib.redirect_stdout(sys.stderr):
        result = run_attack(args.input, args.target, args.out_dir, eps=args.epsilon,
                            steps=args.steps, seed=args.seed, threshold=args.threshold)
    return {"success": True, "input": args.input, "model": MODEL_NAME,
            **result, "error": None}


def main():
    parser = argparse.ArgumentParser(description="MDIP Python worker")
    sub = parser.add_subparsers(dest="command", required=True)

    p_detect = sub.add_parser("detect", help="run YOLO on one image")
    p_detect.add_argument("--input", required=True)
    p_detect.add_argument("--conf", type=float, default=0.25)

    p_attack = sub.add_parser("attack", help="targeted PGD attack on one image")
    p_attack.add_argument("--input", required=True)
    p_attack.add_argument("--target", required=True, help='e.g. "potted plant"')
    p_attack.add_argument("--out-dir", required=True)
    p_attack.add_argument("--epsilon", type=int, default=4, help="in /255 units")
    p_attack.add_argument("--steps", type=int, default=50)
    p_attack.add_argument("--seed", type=int, default=0)
    p_attack.add_argument("--threshold", type=float, default=0.5)

    args = parser.parse_args()

    try:
        if args.command == "detect":
            result = run_detect(args)
        else:
            result = run_attack_command(args)
        print(json.dumps(result))
        return 0
    except Exception as exc:
        print(f"worker error: {exc}", file=sys.stderr)
        print(json.dumps({"success": False, "input": getattr(args, "input", None),
                          "error": str(exc)}))
        return 1


if __name__ == "__main__":
    sys.exit(main())