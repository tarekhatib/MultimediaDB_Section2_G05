import os

import cv2
import numpy as np
import torch
from skimage.metrics import peak_signal_noise_ratio, structural_similarity
from ultralytics import YOLO

from detector import MODEL_NAME, detect

SIZE = 640
CAND_THRESHOLD = 0.25   # a candidate box "detects" a class above this score

_net = None
_names = None


def _get_net():
    """Load the raw YOLO network once, frozen (we need gradients for the image only)."""
    global _net, _names
    if _net is None:
        yolo = YOLO(MODEL_NAME)
        _names = yolo.names
        _net = yolo.model.eval()
        for p in _net.parameters():
            p.requires_grad_(False)
    return _net, _names


def class_id(label):
    _, names = _get_net()
    for k, v in names.items():
        if v == label:
            return k
    raise ValueError(f"'{label}' is not one of the detector's {len(names)} labels")


def _predict(net, x):
    out = net(x)
    return (out[0] if isinstance(out, (list, tuple)) else out)[0]   # (84, 8400)


def _to_uint8_bgr(x):
    a = (x.squeeze(0).permute(1, 2, 0).numpy() * 255).round().astype(np.uint8)
    return cv2.cvtColor(a, cv2.COLOR_RGB2BGR)


def pgd_targeted(image_path, target_label, eps=4, steps=50, seed=0):
    """Targeted L-infinity PGD. eps is in /255 units. Returns images and metrics."""
    net, names = _get_net()
    target = class_id(target_label)

    bgr = cv2.imread(image_path)
    if bgr is None:
        raise ValueError(f"cannot read image: {image_path}")
    rgb = cv2.cvtColor(cv2.resize(bgr, (SIZE, SIZE)), cv2.COLOR_BGR2RGB)
    x0 = torch.from_numpy(rgb).permute(2, 0, 1).float().unsqueeze(0) / 255.0

    eps_f = eps / 255
    alpha = eps_f / 8
    torch.manual_seed(seed)

    # which class does the model see, and which candidate boxes carry it?
    with torch.no_grad():
        scores = _predict(net, x0)[4:]                      # (80, 8400)
        orig = int(scores.max(dim=1).values.argmax())
        idx = (scores[orig] > CAND_THRESHOLD).nonzero().squeeze(1)
    if orig == target:
        raise ValueError("the image is already detected as the target label")

    def loss_fn(pred):
        s = pred[4:, idx]
        return -(s[target] + 1e-9).log().mean() - (1 - s[orig] + 1e-9).log().mean()

    x_adv = x0.clone()
    for _ in range(steps):
        x_adv.requires_grad_(True)
        loss_fn(_predict(net, x_adv)).backward()
        with torch.no_grad():
            x_adv = x_adv - alpha * x_adv.grad.sign()
            x_adv = (x0 + (x_adv - x0).clamp(-eps_f, eps_f)).clamp(0, 1)
    x_adv = x_adv.detach()

    clean_u8, adv_u8 = _to_uint8_bgr(x0), _to_uint8_bgr(x_adv)
    diff = np.abs(adv_u8.astype(int) - clean_u8.astype(int))
    return {
        "clean": clean_u8,
        "poisoned": adv_u8,
        "difference": diff,
        "original_label": names[orig],
        "target_label": target_label,
        "eps": eps, "steps": steps, "seed": seed,
        "candidates_attacked": int(len(idx)),
        "linf": int(diff.max()),
        "psnr": float(peak_signal_noise_ratio(clean_u8, adv_u8, data_range=255)),
        "ssim": float(structural_similarity(clean_u8, adv_u8, channel_axis=2, data_range=255)),
    }



SUCCESS_THRESHOLD = 0.5     # target must be the top detection with confidence >= this
DIFF_AMPLIFICATION = 20     # difference map is multiplied by this, for viewing only


def run_attack(image_path, target_label, out_dir, eps=4, steps=50, seed=0,
               threshold=SUCCESS_THRESHOLD):
    """Run the attack, save the three images, verify with the real detector."""
    r = pgd_targeted(image_path, target_label, eps=eps, steps=steps, seed=seed)

    os.makedirs(out_dir, exist_ok=True)
    stem = os.path.splitext(os.path.basename(image_path))[0]
    tag = target_label.replace(" ", "_")
    paths = {
        "clean": os.path.join(out_dir, f"{stem}_clean640.png"),
        "poisoned": os.path.join(out_dir, f"{stem}_poisoned_{tag}_eps{eps}.png"),
        "difference": os.path.join(out_dir, f"{stem}_diff_eps{eps}_x{DIFF_AMPLIFICATION}.png"),
    }
    cv2.imwrite(paths["clean"], r["clean"])
    cv2.imwrite(paths["poisoned"], r["poisoned"])
    cv2.imwrite(paths["difference"],
                np.clip(r["difference"] * DIFF_AMPLIFICATION, 0, 255).astype(np.uint8))

    # validation: read the saved PNG back through the real detection pipeline
    detections = detect(paths["poisoned"])
    top = max(detections, key=lambda d: d["confidence"]) if detections else None
    attack_success = bool(top and top["label"] == target_label
                          and top["confidence"] >= threshold)

    return {
        "attack": "pgd_targeted",
        "original_label": r["original_label"],
        "target_label": target_label,
        "eps": eps, "steps": steps, "seed": seed,
        "candidates_attacked": r["candidates_attacked"],
        "success_threshold": threshold,
        "attack_success": attack_success,
        "detections_after": detections,
        "linf": r["linf"], "psnr": round(r["psnr"], 3), "ssim": round(r["ssim"], 4),
        "images": paths,
    }