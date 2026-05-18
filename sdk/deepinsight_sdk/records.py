"""
Record type definitions for DeepInsight log data.

Each record type corresponds to one kind of logged data.
The binary file format is:
    [CRC32(4 bytes, big-endian)][payload_length(4 bytes, big-endian)][JSON payload][CRC32(4 bytes)]
"""

import struct
import json
import zlib
from dataclasses import dataclass, field, asdict
from typing import Optional, List, Dict
import numpy as np


def _crc32(data: bytes) -> int:
    return zlib.crc32(data) & 0xFFFFFFFF


def _to_json(obj) -> bytes:
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def _ndarray_to_list(arr) -> list:
    if isinstance(arr, np.ndarray):
        return arr.tolist()
    if isinstance(arr, list):
        return [_ndarray_to_list(x) for x in arr]
    return arr


@dataclass
class Scalar:
    tag: str
    value: float
    step: int
    wall_time: float

    def to_dict(self) -> dict:
        return {"type": "scalar", "tag": self.tag, "value": self.value,
                "step": self.step, "wall_time": self.wall_time}


@dataclass
class ImageRecord:
    tag: str
    encoded_image: bytes
    height: int
    width: int
    step: int
    wall_time: float

    def to_dict(self) -> dict:
        import base64
        return {
            "type": "image",
            "tag": self.tag,
            "encoded_image": base64.b64encode(self.encoded_image).decode("ascii"),
            "height": self.height,
            "width": self.width,
            "step": self.step,
            "wall_time": self.wall_time,
        }


@dataclass
class AudioRecord:
    tag: str
    encoded_audio: bytes
    sample_rate: float
    num_channels: int
    num_frames: int
    step: int
    wall_time: float

    def to_dict(self) -> dict:
        import base64
        return {
            "type": "audio",
            "tag": self.tag,
            "encoded_audio": base64.b64encode(self.encoded_audio).decode("ascii"),
            "sample_rate": self.sample_rate,
            "num_channels": self.num_channels,
            "num_frames": self.num_frames,
            "step": self.step,
            "wall_time": self.wall_time,
        }


@dataclass
class TextRecord:
    tag: str
    text: str
    step: int
    wall_time: float

    def to_dict(self) -> dict:
        return {"type": "text", "tag": self.tag, "text": self.text,
                "step": self.step, "wall_time": self.wall_time}


@dataclass
class Histogram:
    tag: str
    limits: List[float]
    counts: List[int]
    sum: float
    sum_squares: float
    total_count: int
    step: int
    wall_time: float

    def to_dict(self) -> dict:
        return {
            "type": "histogram",
            "tag": self.tag,
            "limits": self.limits,
            "counts": self.counts,
            "sum": self.sum,
            "sum_squares": self.sum_squares,
            "total_count": self.total_count,
            "step": self.step,
            "wall_time": self.wall_time,
        }


@dataclass
class Embedding:
    tag: str
    values: List[float]
    label: Optional[str] = None
    class_id: Optional[int] = None
    sample_id: Optional[int] = None
    step: int = 0
    wall_time: float = 0.0

    def to_dict(self) -> dict:
        d = {
            "type": "embedding",
            "tag": self.tag,
            "values": _ndarray_to_list(self.values),
            "step": self.step,
            "wall_time": self.wall_time,
        }
        if self.label is not None:
            d["label"] = self.label
        if self.class_id is not None:
            d["class_id"] = self.class_id
        if self.sample_id is not None:
            d["sample_id"] = self.sample_id
        return d


@dataclass
class PRCurve:
    tag: str
    precision: List[float]
    recall: List[float]
    thresholds: List[float]
    num_thresholds: int
    step: int
    wall_time: float

    def to_dict(self) -> dict:
        return {
            "type": "pr_curve",
            "tag": self.tag,
            "precision": _ndarray_to_list(self.precision),
            "recall": _ndarray_to_list(self.recall),
            "thresholds": _ndarray_to_list(self.thresholds),
            "num_thresholds": self.num_thresholds,
            "step": self.step,
            "wall_time": self.wall_time,
        }


@dataclass
class RocCurve:
    tag: str
    tpr: List[float]
    fpr: List[float]
    thresholds: List[float]
    step: int
    wall_time: float

    def to_dict(self) -> dict:
        return {
            "type": "roc_curve",
            "tag": self.tag,
            "tpr": _ndarray_to_list(self.tpr),
            "fpr": _ndarray_to_list(self.fpr),
            "thresholds": _ndarray_to_list(self.thresholds),
            "step": self.step,
            "wall_time": self.wall_time,
        }


@dataclass
class HParam:
    tag: str = "hparams"
    metric_values: Dict[str, float] = field(default_factory=dict)
    string_values: Dict[str, str] = field(default_factory=dict)
    step: int = 0
    wall_time: float = 0.0

    def to_dict(self) -> dict:
        return {
            "type": "hparam",
            "tag": self.tag,
            "metric_values": self.metric_values,
            "string_values": self.string_values,
            "step": self.step,
            "wall_time": self.wall_time,
        }


@dataclass
class ProfilerStep:
    tag: str
    profiler_data: dict
    step: int
    wall_time: float

    def to_dict(self) -> dict:
        return {
            "type": "profiler_step",
            "tag": self.tag,
            "profiler_data": self.profiler_data,
            "step": self.step,
            "wall_time": self.wall_time,
        }


@dataclass
class Figure:
    tag: str
    encoded_figure: bytes
    step: int
    wall_time: float

    def to_dict(self) -> dict:
        import base64
        return {
            "type": "figure",
            "tag": self.tag,
            "encoded_figure": base64.b64encode(self.encoded_figure).decode("ascii"),
            "step": self.step,
            "wall_time": self.wall_time,
        }


@dataclass
class ImageMatrix:
    tag: str
    columns: List[dict]
    step: int
    wall_time: float

    def to_dict(self) -> dict:
        import base64
        cols = []
        for col in self.columns:
            imgs = [base64.b64encode(img).decode("ascii") if isinstance(img, bytes) else img for img in col["images"]]
            cols.append({"name": col["name"], "images": imgs})
        return {
            "type": "image_matrix",
            "tag": self.tag,
            "columns": cols,
            "step": self.step,
            "wall_time": self.wall_time,
        }


# Map type string to class for deserialization
_TYPE_MAP = {
    "scalar": Scalar,
    "image": ImageRecord,
    "audio": AudioRecord,
    "text": TextRecord,
    "histogram": Histogram,
    "embedding": Embedding,
    "pr_curve": PRCurve,
    "roc_curve": RocCurve,
    "hparam": HParam,
    "profiler_step": ProfilerStep,
    "figure": Figure,
    "image_matrix": ImageMatrix,
}


def record_from_dict(d: dict):
    """Deserialize a dict back into the appropriate record class."""
    type_name = d.pop("type", None)
    if type_name is None:
        raise ValueError("Record dict missing 'type' field")
    cls = _TYPE_MAP.get(type_name)
    if cls is None:
        raise ValueError(f"Unknown record type: {type_name}")
    # Handle base64 encoded binary fields
    if type_name in ("image", "audio", "figure"):
        import base64
        if type_name == "image":
            d["encoded_image"] = base64.b64decode(d["encoded_image"])
        elif type_name == "audio":
            d["encoded_audio"] = base64.b64decode(d["encoded_audio"])
        elif type_name == "figure":
            d["encoded_figure"] = base64.b64decode(d["encoded_figure"])
    if type_name == "image_matrix":
        import base64
        for col in d.get("columns", []):
            col["images"] = [base64.b64decode(img) if isinstance(img, str) else img for img in col["images"]]
    return cls(**d)


def serialize_record(record) -> bytes:
    """Serialize a record to the binary file format."""
    payload = _to_json(record.to_dict())
    length = len(payload)
    header = struct.pack(">II", _crc32(payload), length)
    footer = struct.pack(">I", _crc32(payload))
    return header + payload + footer


def deserialize_records(data: bytes) -> List:
    """Deserialize a binary blob back into a list of records."""
    records = []
    offset = 0
    while offset < len(data):
        if offset + 12 > len(data):
            break
        crc_header = struct.unpack(">I", data[offset:offset + 4])[0]
        length = struct.unpack(">I", data[offset + 4:offset + 8])[0]
        if offset + 8 + length + 4 > len(data):
            break
        payload = data[offset + 8:offset + 8 + length]
        crc_footer = struct.unpack(">I", data[offset + 8 + length:offset + 12 + length])[0]
        if _crc32(payload) != crc_header or _crc32(payload) != crc_footer:
            raise ValueError(f"CRC mismatch at offset {offset}")
        d = json.loads(payload.decode("utf-8"))
        records.append(record_from_dict(d))
        offset += 12 + length
    return records
