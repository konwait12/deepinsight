"""
LogReader — reads .vdlrecords files back into structured records.

Usage:
    reader = LogReader("./runs/experiment1")
    print(reader.tags())          # ['train/loss', 'input/images', ...]
    for rec in reader.read_tag("train/loss"):
        print(rec.step, rec.value)
"""

import os
import json
from typing import Iterator, List, Optional
from .records import deserialize_records, Scalar


class LogReader:
    """Reads a log directory containing .vdlrecords files."""

    def __init__(self, logdir: str):
        if not os.path.isdir(logdir):
            raise ValueError(f"Not a directory: {logdir}")
        self._logdir = logdir
        self._index_path = os.path.join(logdir, "_index.json")
        self._index = {}
        if os.path.exists(self._index_path):
            with open(self._index_path, "r", encoding="utf-8") as f:
                self._index = json.load(f)

    def tags(self) -> List[str]:
        """Return all tag names found in this log directory."""
        tags = set()
        for fname in os.listdir(self._logdir):
            if fname.endswith(".vdlrecords"):
                tag = fname[:-len(".vdlrecords")].replace("_", "/", 1) if "_" in fname else fname[:-len(".vdlrecords")]
                # Try to match against index first
                tag_from_name = fname[:-len(".vdlrecords")]
                matched = False
                for idx_tag in self._index:
                    safe = idx_tag.replace("/", "_").replace("\\", "_").replace(":", "_")
                    if safe == tag_from_name:
                        tags.add(idx_tag)
                        matched = True
                        break
                if not matched:
                    tags.add(tag_from_name)
        return sorted(tags)

    def read_tag(self, tag: str) -> Iterator:
        """Iterate over all records for a given tag, in order written."""
        safe_name = tag.replace("/", "_").replace("\\", "_").replace(":", "_")
        fpath = os.path.join(self._logdir, f"{safe_name}.vdlrecords")
        if not os.path.exists(fpath):
            raise FileNotFoundError(f"No log file for tag '{tag}' at {fpath}")
        with open(fpath, "rb") as f:
            data = f.read()
        return iter(deserialize_records(data))

    def read_all(self) -> Iterator:
        """Iterate over all records from all tags."""
        for tag in self.tags():
            yield from self.read_tag(tag)

    def scalars(self, tag: str) -> List[Scalar]:
        """Convenience: return all scalar records for a tag as a list."""
        return [r for r in self.read_tag(tag) if isinstance(r, Scalar)]
