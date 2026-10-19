import numpy as np
import soundfile as sf

from generate import EFFECTS, MAX_SECONDS, RAW_DIR, THEMES, synth, write_all

LIMIT_DBFS = -1.0


def _peak_dbfs(data: np.ndarray) -> float:
    return 20 * np.log10(np.max(np.abs(data)))


def test_bundled_pack_complete():
    # 4종 × 5테마 누락 없음
    missing = [
        f"sfx_{t}_{e}.ogg" for t in THEMES for e in EFFECTS if not (RAW_DIR / f"sfx_{t}_{e}.ogg").exists()
    ]
    assert missing == []


def test_bundled_length_and_peak():
    for path in sorted(RAW_DIR.glob("sfx_*.ogg")):
        data, rate = sf.read(path)
        assert len(data) / rate <= MAX_SECONDS, path.name
        assert _peak_dbfs(data) <= LIMIT_DBFS, path.name


def test_synth_is_deterministic():
    for t in THEMES:
        for e in EFFECTS:
            assert np.array_equal(synth(t, e), synth(t, e))


def test_write_all_to_tmp(tmp_path):
    paths = write_all(tmp_path)
    assert len(paths) == len(THEMES) * len(EFFECTS)
    for p in paths:
        data, rate = sf.read(p)
        assert 0 < len(data) / rate <= MAX_SECONDS
        assert _peak_dbfs(data) <= LIMIT_DBFS
