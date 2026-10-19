"""테마 효과음 합성. 외부 음원 없이 numpy로 만들어 라이선스 문제가 없고 결과가 늘 같다.

4종(page · mark · unmark · success) × 5테마 → core/ui/src/main/res/raw/sfx_<테마>_<종류>.ogg
각 300ms 이하 · 피크 -3dBFS로 맞춘다 (인코딩 후에도 -1dBFS 이하).
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np
import soundfile as sf

REPO_ROOT = Path(__file__).resolve().parents[2]
RAW_DIR = REPO_ROOT / "core" / "ui" / "src" / "main" / "res" / "raw"

RATE = 22_050
MAX_SECONDS = 0.3
PEAK_DBFS = -3.0
THEMES = ("note", "season", "analog", "lofi", "baseball")
EFFECTS = ("page", "mark", "unmark", "success")


def _t(seconds: float) -> np.ndarray:
    return np.arange(int(RATE * seconds)) / RATE


def _env(n: int, attack: float = 0.004, decay: float = 18.0) -> np.ndarray:
    t = np.arange(n) / RATE
    a = np.clip(t / attack, 0, 1) if attack > 0 else np.ones(n)
    return a * np.exp(-decay * t)


def _tone(freq: float, seconds: float, decay: float = 18.0, sweep: float = 0.0) -> np.ndarray:
    t = _t(seconds)
    f = freq * (1 + sweep * t / seconds)
    phase = 2 * np.pi * np.cumsum(f) / RATE
    return np.sin(phase) * _env(len(t), decay=decay)


def _noise(rng: np.random.Generator, seconds: float, decay: float = 12.0, smooth: int = 1) -> np.ndarray:
    n = int(RATE * seconds)
    x = rng.standard_normal(n)
    if smooth > 1:
        x = np.convolve(x, np.ones(smooth) / smooth, mode="same")
    return x * _env(n, attack=0.01, decay=decay)


def _swish(rng: np.random.Generator, seconds: float, smooth: int) -> np.ndarray:
    n = int(RATE * seconds)
    x = np.convolve(rng.standard_normal(n), np.ones(smooth) / smooth, mode="same")
    # 가운데가 부푸는 쉭 소리
    return x * np.sin(np.pi * np.arange(n) / n) ** 2


def _seq(*parts: tuple[float, np.ndarray]) -> np.ndarray:
    """(시작 초, 신호) 목록을 겹쳐 하나로."""
    end = max(int(start * RATE) + len(sig) for start, sig in parts)
    out = np.zeros(end)
    for start, sig in parts:
        i = int(start * RATE)
        out[i : i + len(sig)] += sig
    return out


def _mix(*sigs: np.ndarray) -> np.ndarray:
    """길이가 다른 신호를 0초에 겹친다."""
    return _seq(*((0.0, s) for s in sigs))


def _arp(freqs: list[float], step: float, decay: float = 14.0) -> np.ndarray:
    return _seq(*((i * step, _tone(f, 0.3 - i * step, decay)) for i, f in enumerate(freqs)))


def synth(theme: str, effect: str) -> np.ndarray:
    # 고정 시드: 몇 번을 돌려도 같은 소리
    rng = np.random.default_rng(THEMES.index(theme) * 10 + EFFECTS.index(effect))
    if theme == "note":
        # 부드러운 틱
        sig = {
            "page": _swish(rng, 0.18, 24) * 0.6,
            "mark": _mix(_tone(1760, 0.06, 60), 0.3 * _tone(2640, 0.04, 80)),
            "unmark": _tone(1320, 0.06, 60),
            "success": _arp([1046, 1318, 1568], 0.06),
        }[effect]
    elif theme == "season":
        # 눈 밟기 · 바람 · 물방울
        sig = {
            "page": _swish(rng, 0.26, 60),
            "mark": _noise(rng, 0.09, 40, smooth=6),
            "unmark": _tone(1400, 0.08, 30, sweep=-0.5),
            "success": _seq((0, _tone(1200, 0.12, 25, sweep=-0.4)), (0.1, _tone(1600, 0.15, 25, sweep=-0.4))),
        }[effect]
    elif theme == "analog":
        # 종이 넘김 · 형광펜 긋기
        sig = {
            "page": _swish(rng, 0.22, 8) * 0.8 + _noise(rng, 0.22, 20, smooth=3) * 0.2,
            "mark": _swish(rng, 0.14, 3),
            "unmark": _noise(rng, 0.08, 45, smooth=4),
            "success": _seq((0, _swish(rng, 0.1, 3)), (0.12, _swish(rng, 0.12, 3))),
        }[effect]
    elif theme == "lofi":
        # 카세트 버튼 · 바이닐 잡음
        click = lambda f: _tone(f, 0.03, 120) + _noise(rng, 0.03, 150, smooth=2) * 0.5  # noqa: E731
        crackle = np.zeros(int(RATE * 0.25))
        crackle[rng.choice(len(crackle), 14, replace=False)] = rng.uniform(-1, 1, 14)
        sig = {
            "page": crackle + _swish(rng, 0.25, 30) * 0.3,
            "mark": _seq((0, click(420)), (0.05, click(220))),
            "unmark": _seq((0, click(220)), (0.05, click(420))),
            "success": _mix(_arp([523, 659, 784], 0.07, decay=10) * 0.7, crackle * 0.3),
        }[effect]
    elif theme == "baseball":
        # 공 받는 소리 · 짧은 함성
        thump = _mix(_tone(140, 0.12, 30), _noise(rng, 0.05, 80, smooth=2) * 0.6)
        crowd = _swish(rng, 0.28, 5) * (1 + 0.4 * np.sin(2 * np.pi * 9 * _t(0.28)))
        sig = {
            "page": _seq((0, _tone(900, 0.04, 90)), (0.05, _tone(700, 0.04, 90))),
            "mark": thump,
            "unmark": _tone(110, 0.1, 35),
            "success": _seq((0, thump), (0.04, crowd * 0.8)),
        }[effect]
    else:
        raise ValueError(theme)
    sig = sig[: int(RATE * MAX_SECONDS)]
    # 끝 5ms 페이드로 딸깍 방지
    fade = min(len(sig), int(RATE * 0.005))
    sig[-fade:] *= np.linspace(1, 0, fade)
    peak = np.max(np.abs(sig))
    return sig / peak * 10 ** (PEAK_DBFS / 20)


def write_all(out_dir: Path = RAW_DIR) -> list[Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    paths = []
    for theme in THEMES:
        for effect in EFFECTS:
            path = out_dir / f"sfx_{theme}_{effect}.ogg"
            sf.write(path, synth(theme, effect).astype(np.float32), RATE, format="OGG", subtype="VORBIS")
            paths.append(path)
    return paths


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="효과음 합성")
    parser.add_argument("--out", type=Path, default=RAW_DIR)
    args = parser.parse_args(argv)
    paths = write_all(args.out)
    print(f"{len(paths)}개 생성: {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
