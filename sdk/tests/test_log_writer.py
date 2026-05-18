"""Tests for LogWriter and LogReader round-trip."""

import os
import sys
import tempfile
import pytest
import numpy as np

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from deepinsight_sdk import LogWriter, LogReader
from deepinsight_sdk.records import Scalar, ImageRecord, Histogram, Embedding


class TestLogWriter:
    def test_scalar_roundtrip(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            writer.add_scalar("train/loss", 0.5, step=0)
            writer.add_scalar("train/loss", 0.3, step=10)
            writer.add_scalar("train/loss", 0.1, step=20)
            writer.close()

            reader = LogReader(tmpdir)
            assert "train/loss" in reader.tags()
            records = list(reader.read_tag("train/loss"))
            assert len(records) == 3
            assert all(isinstance(r, Scalar) for r in records)
            assert records[0].value == 0.5
            assert records[2].value == 0.1
            assert records[0].step == 0
            assert records[2].step == 20

    def test_multiple_tags(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            writer.add_scalar("train/loss", 0.5, step=0)
            writer.add_scalar("train/accuracy", 0.8, step=0)
            writer.add_scalar("val/loss", 0.6, step=0)
            writer.close()

            reader = LogReader(tmpdir)
            tags = reader.tags()
            assert len(tags) == 3
            for tag in ["train/loss", "train/accuracy", "val/loss"]:
                assert tag in tags

    def test_image_roundtrip(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            img = np.random.randint(0, 255, (64, 64, 3), dtype=np.uint8)
            writer.add_image("input/images", img, step=0)
            writer.close()

            reader = LogReader(tmpdir)
            records = list(reader.read_tag("input/images"))
            assert len(records) == 1
            assert isinstance(records[0], ImageRecord)
            assert records[0].height == 64
            assert records[0].width == 64
            assert len(records[0].encoded_image) > 0

    def test_histogram_roundtrip(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            values = np.random.randn(10000) * 0.1 + 0.5
            writer.add_histogram("weights/conv1", values, step=0, bins=30)
            writer.close()

            reader = LogReader(tmpdir)
            records = list(reader.read_tag("weights/conv1"))
            assert len(records) == 1
            rec = records[0]
            assert isinstance(rec, Histogram)
            assert len(rec.limits) == 31
            assert len(rec.counts) == 30
            assert sum(rec.counts) <= 10000
            assert rec.total_count > 0

    def test_pr_curve(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            labels = np.array([0, 0, 1, 1])
            preds = np.array([0.1, 0.4, 0.35, 0.8])
            writer.add_pr_curve("pr_test", labels, preds, step=0, num_thresholds=10)
            writer.close()

            reader = LogReader(tmpdir)
            records = list(reader.read_tag("pr_test"))
            assert len(records) == 1
            assert len(records[0].precision) == 10
            assert len(records[0].recall) == 10

    def test_roc_curve(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            labels = np.array([0, 0, 1, 1])
            preds = np.array([0.1, 0.4, 0.35, 0.8])
            writer.add_roc_curve("roc_test", labels, preds, step=0)
            writer.close()

            reader = LogReader(tmpdir)
            records = list(reader.read_tag("roc_test"))
            assert len(records) == 1

    def test_hparams(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            writer.add_hparams(
                {"lr": 0.001, "batch_size": 32, "optimizer": "adam"},
                {"val_acc": 0.92, "val_loss": 0.31},
            )
            writer.close()

            reader = LogReader(tmpdir)
            records = list(reader.read_tag("hparams"))
            assert len(records) == 1

    def test_embedding(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            emb = np.random.randn(10, 128).astype(np.float32)
            labels = [f"class_{i % 3}" for i in range(10)]
            writer.add_embedding("features", emb, labels=labels, step=0)
            writer.close()

            reader = LogReader(tmpdir)
            records = list(reader.read_tag("features"))
            assert len(records) == 10
            assert isinstance(records[0], Embedding)
            assert len(records[0].values) == 128

    def test_text(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            writer.add_text("output/summary", "Epoch 1: loss=0.5, acc=0.8", step=0)
            writer.add_text("output/summary", "Epoch 2: loss=0.3, acc=0.9", step=1)
            writer.close()

            reader = LogReader(tmpdir)
            records = list(reader.read_tag("output/summary"))
            assert len(records) == 2
            assert "Epoch 1" in records[0].text
            assert "Epoch 2" in records[1].text

    def test_context_manager(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            with LogWriter(tmpdir) as writer:
                writer.add_scalar("loss", 1.0, step=0)
            # Should be auto-closed
            reader = LogReader(tmpdir)
            assert "loss" in reader.tags()

    def test_different_image_shapes(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            # Grayscale
            gray = np.random.randint(0, 255, (32, 32), dtype=np.uint8)
            writer.add_image("gray", gray, step=0)
            # CHW format
            chw = np.random.randn(3, 48, 48)
            writer.add_image("chw", chw, step=1)
            writer.close()

            reader = LogReader(tmpdir)
            records = list(reader.read_tag("gray"))
            assert len(records) == 1
            records = list(reader.read_tag("chw"))
            assert len(records) == 1

    def test_scalars_batch(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            writer = LogWriter(tmpdir)
            writer.add_scalars("eval", {"accuracy": 0.92, "f1": 0.88, "precision": 0.90}, step=100)
            writer.close()

            reader = LogReader(tmpdir)
            assert "eval/accuracy" in reader.tags()
            assert "eval/f1" in reader.tags()
            assert "eval/precision" in reader.tags()
