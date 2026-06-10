"""Legacy entrypoint for seeding knowledge tutorials.

The project knowledge base is now recommender-system specific. This wrapper keeps
the old command name usable while delegating to the current seed scripts.
"""

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

for script in ("seed-model-articles.cjs", "seed-platform-knowledge.cjs"):
    subprocess.run(["node", str(ROOT / "scripts" / script)], cwd=ROOT, check=True)
