import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import cv2
import numpy as np

ROOT = Path(__file__).resolve().parent.parent
WORKER = ROOT / "python_code" / "worker.py"
CAR = ROOT / "images" / "car.jpg"


def run_worker(*args):
    """Run the worker like Java will. stdout must be exactly one JSON object."""
    p = subprocess.run([sys.executable, str(WORKER), *args],
                       capture_output=True, text=True, cwd=ROOT)
    return p.returncode, json.loads(p.stdout)


def make_gray_image(folder):
    path = Path(folder) / "gray.png"
    cv2.imwrite(str(path), np.full((200, 300, 3), 128, dtype=np.uint8))
    return str(path)


class WorkerContract(unittest.TestCase):

    def test_detect_missing_file_fails_cleanly(self):
        code, out = run_worker("detect", "--input", "does_not_exist.jpg")
        self.assertEqual(code, 1)
        self.assertFalse(out["success"])
        self.assertTrue(out["error"])

    def test_detect_valid_image_returns_the_contract_fields(self):
        with tempfile.TemporaryDirectory() as d:
            code, out = run_worker("detect", "--input", make_gray_image(d))
        self.assertEqual(code, 0)
        self.assertTrue(out["success"])
        self.assertIsInstance(out["detections"], list)
        self.assertEqual(out["model"], "yolov8n.pt")

    def test_attack_rejects_a_label_the_model_does_not_have(self):
        with tempfile.TemporaryDirectory() as d:
            code, out = run_worker("attack", "--input", make_gray_image(d),
                                   "--target", "tree", "--out-dir", d)
        self.assertEqual(code, 1)
        self.assertFalse(out["success"])
        self.assertIn("labels", out["error"])

    @unittest.skipUnless(CAR.exists(), "images/car.jpg is not in the repo")
    def test_attack_on_the_car_image_succeeds_within_the_bound(self):
        with tempfile.TemporaryDirectory() as d:
            code, out = run_worker("attack", "--input", str(CAR),
                                   "--target", "potted plant",
                                   "--epsilon", "4", "--out-dir", d)
            self.assertEqual(code, 0)
            self.assertTrue(out["attack_success"])
            self.assertLessEqual(out["linf"], 4)
            self.assertGreater(out["psnr"], 36.0)   # floor for a +/-4 change
            for path in out["images"].values():
                self.assertTrue((ROOT / path).exists() or Path(path).exists())


if __name__ == "__main__":
    unittest.main(verbosity=2)