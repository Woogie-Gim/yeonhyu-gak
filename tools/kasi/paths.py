"""저장소 안 데이터 경로."""

from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS_DIR = REPO_ROOT / "core" / "data" / "src" / "main" / "assets" / "holidays"
LUNAR_TABLE_PATH = ASSETS_DIR / "lunar_table.json"
BUNDLE_PATH = ASSETS_DIR / "kr_holidays.json"
GOLDEN_PATH = REPO_ROOT / "core" / "engine" / "src" / "test" / "resources" / "golden" / "kasi_rest_days.json"

FIRST_YEAR = 2020
LAST_YEAR = 2035
YEARS = range(FIRST_YEAR, LAST_YEAR + 1)
