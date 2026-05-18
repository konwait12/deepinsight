"""
DeepInsight SDK — Deep Learning Visualization & Analysis Toolkit

Usage:
    from deepinsight_sdk import LogWriter

    writer = LogWriter("./runs/experiment1")
    writer.add_scalar("train/loss", 0.35, step=100)
    writer.add_image("input/batch", image_array, step=0)
    writer.close()
"""

from .log_writer import LogWriter
from .log_reader import LogReader

__version__ = "0.1.0"
__all__ = ["LogWriter", "LogReader"]
