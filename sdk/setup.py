from setuptools import setup, find_packages

setup(
    name="deepinsight-sdk",
    version="0.1.0",
    description="DeepInsight training visualization SDK",
    author="DeepInsight Team",
    packages=find_packages(),
    python_requires=">=3.7",
    install_requires=[
        "numpy>=1.21",
        "Pillow>=9.0",
        "click>=8.0",
        "requests>=2.28",
    ],
    extras_require={
        "audio": ["soundfile>=0.12"],
    },
    entry_points={
        "console_scripts": [
            "deepinsight=deepinsight_sdk.cli.main:cli",
        ],
    },
)
