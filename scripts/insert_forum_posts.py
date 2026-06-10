"""Legacy entrypoint for seeding official forum posts.

Official model/forum articles now come from the recommender-system knowledge
seed scripts. Keeping this file avoids reintroducing old generic AI articles.
"""

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

for script in ("seed-model-articles.cjs", "seed-platform-knowledge.cjs"):
    subprocess.run(["node", str(ROOT / "scripts" / script)], cwd=ROOT, check=True)
