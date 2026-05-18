"""
LogWriter — the primary API for logging training metrics, samples, and artifacts.

Usage:
    writer = LogWriter("./runs/experiment1")
    writer.add_scalar("train/loss", 0.35, step=100)
    writer.add_image("input/batch", image_array, step=0)   # numpy (H,W,C) or (H,W)
    writer.add_histogram("weights/conv1", weights, step=50)
    writer.add_embedding("features", embeddings, labels, step=0)
    writer.add_hparams({"lr": 0.001, "bs": 32}, {"val_acc": 0.92})
    writer.close()
"""

import os
import time
import json
import threading
import struct
import zlib
from typing import Optional, List, Dict, Union
import numpy as np

from .records import (
    Scalar, ImageRecord, AudioRecord, TextRecord, Histogram, Embedding,
    PRCurve, RocCurve, HParam, ProfilerStep, Figure, ImageMatrix,
    serialize_record, _crc32, _to_json,
)


def _compute_histogram_bins(
    values: np.ndarray, bins: int = 30
) -> tuple:
    """Compute histogram bins from a flat array of values. Returns (limits, counts)."""
    values = np.asarray(values).ravel()
    finite = values[np.isfinite(values)]
    if len(finite) == 0:
        limits = [0.0] * (bins + 1)
        for i in range(bins + 1):
            limits[i] = float(i - bins // 2)
        return limits, [0] * bins
    min_val = float(finite.min())
    max_val = float(finite.max())
    if min_val == max_val:
        min_val -= 0.5
        max_val += 0.5
    counts, edges = np.histogram(finite, bins=bins, range=(min_val, max_val))
    return edges.tolist(), counts.tolist()


class LogWriter:
    """Writes structured training logs to disk in .vdlrecords format.

    Each LogWriter instance corresponds to one experiment run. Data is written
    into per-tag files under a directory named by the `logdir` argument.

    Thread-safe: multiple threads may call add_* methods concurrently.
    """

    def __init__(self, logdir: str, max_queue: int = 100):
        if not os.path.exists(logdir):
            os.makedirs(logdir, exist_ok=True)
        self._logdir = logdir
        self._index_path = os.path.join(logdir, "_index.json")
        self._lock = threading.Lock()
        self._file_handles: Dict[str, object] = {}
        self._index: Dict[str, list] = {}
        self._start_time = time.time()
        self._closed = False

        if os.path.exists(self._index_path):
            with open(self._index_path, "r", encoding="utf-8") as f:
                self._index = json.load(f)

    def _wall_time(self) -> float:
        return time.time() - self._start_time

    def _get_file(self, tag: str) -> object:
        if tag not in self._file_handles:
            safe_name = tag.replace("/", "_").replace("\\", "_").replace(":", "_")
            fpath = os.path.join(self._logdir, f"{safe_name}.vdlrecords")
            f = open(fpath, "ab")
            self._file_handles[tag] = f
            if tag not in self._index:
                self._index[tag] = []
        return self._file_handles[tag]

    def _write_record(self, tag: str, record) -> None:
        if self._closed:
            raise RuntimeError("LogWriter is closed")
        with self._lock:
            f = self._get_file(tag)
            data = serialize_record(record)
            f.write(data)
            f.flush()
            self._index[tag].append(record.step)
            if len(self._index[tag]) > 1000:
                # Keep last 1000 step references in memory; file has everything
                self._index[tag] = self._index[tag][-1000:]

    # ---- Public API ----

    def add_scalar(self, tag: str, value: float, step: int) -> None:
        """Log a scalar value (loss, accuracy, learning_rate, etc.)."""
        rec = Scalar(tag=tag, value=float(value), step=step,
                     wall_time=self._wall_time())
        self._write_record(tag, rec)

    def add_scalars(self, tag: str, values: Dict[str, float], step: int) -> None:
        """Log multiple scalar values under the same tag prefix at one step.

        E.g., add_scalars("eval", {"accuracy": 0.92, "f1": 0.88}, step=100)
        This creates separate scalar records for each key under "eval/accuracy", "eval/f1".
        """
        for k, v in values.items():
            self.add_scalar(f"{tag}/{k}", v, step)

    def add_image(self, tag: str, image: np.ndarray, step: int) -> None:
        """Log an image. `image` can be a numpy array of shape (H,W), (H,W,C), or (C,H,W).
        Automatically normalizes to [0,255] uint8.
        """
        from PIL import Image
        import io

        img = np.asarray(image)
        if img.ndim == 2:
            img = img[:, :, None]
            img = np.repeat(img, 3, axis=2)
        if img.shape[0] == 3 and img.ndim == 3 and img.shape[-1] != 3:
            # (C, H, W) -> (H, W, C)
            img = img.transpose(1, 2, 0)
        if img.shape[0] == 1 and img.ndim == 3:
            img = img.squeeze(0)
        # Normalize to [0, 255]
        if img.dtype != np.uint8:
            if img.max() <= 1.0:
                img = (img * 255).astype(np.uint8)
            else:
                img = img.astype(np.uint8)
        pil_img = Image.fromarray(img)
        buf = io.BytesIO()
        pil_img.save(buf, format="PNG")
        encoded = buf.getvalue()
        rec = ImageRecord(
            tag=tag, encoded_image=encoded,
            height=img.shape[0], width=img.shape[1],
            step=step, wall_time=self._wall_time(),
        )
        self._write_record(tag, rec)

    def add_audio(
        self, tag: str, audio: np.ndarray, sample_rate: float, step: int
    ) -> None:
        """Log audio. `audio` is a 1D or 2D numpy array (samples, channels).
        Values should be in [-1, 1] or will be clipped.
        """
        import io
        import struct as _struct

        audio_data = np.asarray(audio)
        if audio_data.ndim == 1:
            audio_data = audio_data[:, None]
        if audio_data.ndim != 2:
            raise ValueError(f"Expected 1D or 2D audio array, got shape {audio_data.shape}")
        num_frames, num_channels = audio_data.shape

        # Clamp to [-1, 1] and convert to 16-bit PCM
        audio_data = np.clip(audio_data, -1.0, 1.0)
        int_data = (audio_data * 32767).astype("<i2")

        buf = io.BytesIO()
        # Write minimal WAV header
        data_size = int_data.nbytes
        buf.write(b"RIFF")
        buf.write(_struct.pack("<I", 36 + data_size))
        buf.write(b"WAVE")
        buf.write(b"fmt ")
        buf.write(_struct.pack("<IHHIIHH", 16, 1, num_channels,
                               int(sample_rate), int(sample_rate * num_channels * 2),
                               num_channels * 2, 16))
        buf.write(b"data")
        buf.write(_struct.pack("<I", data_size))
        buf.write(int_data.tobytes())
        encoded = buf.getvalue()

        rec = AudioRecord(
            tag=tag, encoded_audio=encoded,
            sample_rate=float(sample_rate), num_channels=num_channels,
            num_frames=num_frames,
            step=step, wall_time=self._wall_time(),
        )
        self._write_record(tag, rec)

    def add_text(self, tag: str, text: str, step: int) -> None:
        """Log a text string at a training step."""
        rec = TextRecord(tag=tag, text=text, step=step,
                         wall_time=self._wall_time())
        self._write_record(tag, rec)

    def add_histogram(
        self, tag: str, values: np.ndarray, step: int, bins: int = 30
    ) -> None:
        """Log a histogram of tensor values at a training step.

        Automatically computes bin limits and counts. Values can be any shape;
        they are flattened before binning.
        """
        limits, counts = _compute_histogram_bins(values, bins)
        flat = np.asarray(values).ravel()
        finite = flat[np.isfinite(flat)]
        rec = Histogram(
            tag=tag,
            limits=limits,
            counts=counts,
            sum=float(finite.sum()) if len(finite) > 0 else 0.0,
            sum_squares=float((finite ** 2).sum()) if len(finite) > 0 else 0.0,
            total_count=int(len(finite)),
            step=step,
            wall_time=self._wall_time(),
        )
        self._write_record(tag, rec)

    def add_embedding(
        self,
        tag: str,
        matrix: np.ndarray,
        labels: Optional[List[str]] = None,
        class_ids: Optional[List[int]] = None,
        sample_ids: Optional[List[int]] = None,
        step: int = 0,
    ) -> None:
        """Log high-dimensional embedding vectors.

        `matrix` — (N, D) array of N samples with D dimensions each.
        `labels` — optional string label per sample.
        `class_ids` — optional integer class id per sample.
        Writes one Embedding record per sample.
        """
        mat = np.asarray(matrix)
        if mat.ndim != 2:
            raise ValueError(f"Expected 2D matrix, got shape {mat.shape}")
        n = mat.shape[0]
        labels = labels or [None] * n
        class_ids = class_ids or [None] * n
        sample_ids = sample_ids or list(range(n))
        wt = self._wall_time()
        for i in range(n):
            rec = Embedding(
                tag=tag,
                values=mat[i].tolist(),
                label=labels[i],
                class_id=class_ids[i],
                sample_id=sample_ids[i],
                step=step,
                wall_time=wt,
            )
            self._write_record(tag, rec)

    def add_pr_curve(
        self,
        tag: str,
        labels: np.ndarray,
        predictions: np.ndarray,
        step: int,
        num_thresholds: int = 127,
    ) -> None:
        """Log a Precision-Recall curve.

        `labels` — ground-truth binary labels (0 or 1).
        `predictions` — predicted scores/probabilities in [0, 1].
        """
        labels = np.asarray(labels).ravel()
        predictions = np.asarray(predictions).ravel()

        # Sort by prediction descending
        order = np.argsort(predictions)[::-1]
        labels = labels[order]
        predictions = predictions[order]

        thresholds = np.linspace(0, 1, num_thresholds)
        precision_list = []
        recall_list = []
        tp = 0
        fp = 0
        fn = int(np.sum(labels))
        total_pos = fn
        if total_pos == 0:
            total_pos = 1  # avoid div-by-zero

        idx = 0
        for t in thresholds:
            while idx < len(predictions) and predictions[idx] >= t:
                if labels[idx] == 1:
                    tp += 1
                    fn -= 1
                else:
                    fp += 1
                idx += 1
            prec = tp / (tp + fp) if (tp + fp) > 0 else 1.0
            rec = tp / (tp + fn) if (tp + fn) > 0 else 0.0
            precision_list.append(prec)
            recall_list.append(rec)

        rec = PRCurve(
            tag=tag,
            precision=precision_list,
            recall=recall_list,
            thresholds=thresholds.tolist(),
            num_thresholds=num_thresholds,
            step=step,
            wall_time=self._wall_time(),
        )
        self._write_record(tag, rec)

    def add_roc_curve(
        self,
        tag: str,
        labels: np.ndarray,
        predictions: np.ndarray,
        step: int,
        num_thresholds: int = 127,
    ) -> None:
        """Log an ROC curve.

        `labels` — ground-truth binary labels (0 or 1).
        `predictions` — predicted scores/probabilities in [0, 1].
        """
        labels = np.asarray(labels).ravel()
        predictions = np.asarray(predictions).ravel()

        order = np.argsort(predictions)[::-1]
        labels = labels[order]
        predictions = predictions[order]

        thresholds = np.linspace(0, 1, num_thresholds)
        tpr_list, fpr_list = [], []
        tp, fp = 0, 0
        total_pos = int(np.sum(labels))
        total_neg = int(len(labels) - total_pos)
        if total_pos == 0:
            total_pos = 1
        if total_neg == 0:
            total_neg = 1

        idx = 0
        for t in thresholds:
            while idx < len(predictions) and predictions[idx] >= t:
                if labels[idx] == 1:
                    tp += 1
                else:
                    fp += 1
                idx += 1
            tpr_list.append(tp / total_pos)
            fpr_list.append(fp / total_neg)

        rec = RocCurve(
            tag=tag, tpr=tpr_list, fpr=fpr_list,
            thresholds=thresholds.tolist(),
            step=step, wall_time=self._wall_time(),
        )
        self._write_record(tag, rec)

    def add_hparams(
        self,
        hparams: Dict[str, Union[float, str]],
        metrics: Dict[str, float],
    ) -> None:
        """Log hyperparameters and their resulting metrics.

        `hparams` — dict of parameter name -> value (float or string).
        `metrics` — dict of metric name -> final value.
        """
        float_hparams = {}
        string_hparams = {}
        for k, v in hparams.items():
            if isinstance(v, (int, float)):
                float_hparams[k] = float(v)
            else:
                string_hparams[k] = str(v)

        rec = HParam(
            metric_values={k: float(v) for k, v in metrics.items()},
            string_values=string_hparams,
            step=0,
            wall_time=self._wall_time(),
        )
        # Also log each metric as a scalar so it shows up in scalar view
        for k, v in metrics.items():
            self.add_scalar(f"hparam/{k}", float(v), step=0)
        for k, v in float_hparams.items():
            rec.metric_values[k] = v
        self._write_record("hparams", rec)

    def add_figure(self, tag: str, fig, step: int) -> None:
        """Log a matplotlib figure as an image.

        `fig` — a matplotlib Figure object.
        """
        import io
        buf = io.BytesIO()
        fig.savefig(buf, format="png", dpi=100, bbox_inches="tight")
        buf.seek(0)
        encoded = buf.getvalue()
        rec = Figure(tag=tag, encoded_figure=encoded, step=step,
                     wall_time=self._wall_time())
        self._write_record(tag, rec)

    def add_image_matrix(
        self, tag: str, images: List[np.ndarray], names: List[str], step: int
    ) -> None:
        """Log a grid of related images with column labels.

        Each column is a group of related images (e.g., same class).
        """
        import io
        from PIL import Image

        columns = []
        for name, img_list in zip(names, images):
            encoded_imgs = []
            for img in img_list:
                img_arr = np.asarray(img)
                if img_arr.ndim == 2:
                    img_arr = np.stack([img_arr] * 3, axis=2)
                if img_arr.dtype != np.uint8:
                    img_arr = (img_arr * 255).astype(np.uint8) if img_arr.max() <= 1.0 else img_arr.astype(np.uint8)
                pil_img = Image.fromarray(img_arr)
                buf = io.BytesIO()
                pil_img.save(buf, format="PNG")
                encoded_imgs.append(buf.getvalue())
            columns.append({"name": name, "images": encoded_imgs})

        rec = ImageMatrix(tag=tag, columns=columns, step=step,
                          wall_time=self._wall_time())
        self._write_record(tag, rec)

    def add_profiler_step(
        self, tag: str, profiler_data: dict, step: int
    ) -> None:
        """Log profiler trace data as a JSON-compatible dict."""
        rec = ProfilerStep(tag=tag, profiler_data=profiler_data, step=step,
                           wall_time=self._wall_time())
        self._write_record(tag, rec)

    def close(self) -> None:
        """Flush and close all open log files. Call at end of training."""
        self._closed = True
        with self._lock:
            for f in self._file_handles.values():
                f.close()
            self._file_handles.clear()
            with open(self._index_path, "w", encoding="utf-8") as f:
                json.dump(self._index, f, ensure_ascii=False)

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()

    @property
    def logdir(self) -> str:
        return self._logdir
