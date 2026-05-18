"""
DeepInsight CLI — upload logs, serve locally, or convert formats.

Usage:
    deepinsight upload ./runs/ --host http://localhost:8080
    deepinsight serve ./runs/ --port 8040
"""

import os
import sys
import json
import glob
import click

import requests


@click.group()
def cli():
    """DeepInsight SDK — Deep Learning Visualization Toolkit"""
    pass


@cli.command()
@click.argument("logdir", type=click.Path(exists=True))
@click.option("--host", default="http://localhost:8080", help="Backend server URL")
@click.option("--api-key", default=None, help="API key for authentication")
@click.option("--name", default=None, help="Experiment run name (default: dir name)")
def upload(logdir, host, api_key, name):
    """Upload a log directory to the DeepInsight backend."""
    run_name = name or os.path.basename(os.path.abspath(logdir))

    # 1. Create a run
    headers = {"Content-Type": "application/json"}
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"

    try:
        resp = requests.post(
            f"{host}/api/v1/logs/runs",
            json={"name": run_name},
            headers=headers,
            timeout=10,
        )
        if resp.status_code != 200:
            click.echo(f"Failed to create run: {resp.status_code} {resp.text}", err=True)
            sys.exit(1)
        run_data = resp.json()
        if isinstance(run_data, dict) and "data" in run_data:
            run_data = run_data["data"]
        run_id = run_data.get("id") if isinstance(run_data, dict) else run_data
        click.echo(f"Created run: {run_name} (id={run_id})")
    except requests.exceptions.ConnectionError:
        click.echo(f"Cannot connect to {host}", err=True)
        sys.exit(1)

    # 2. Upload .vdlrecords files
    vdl_files = glob.glob(os.path.join(logdir, "*.vdlrecords"))
    if not vdl_files:
        click.echo("No .vdlrecords files found in logdir", err=True)
        sys.exit(1)

    for fpath in vdl_files:
        fname = os.path.basename(fpath)
        click.echo(f"Uploading {fname}...")
        with open(fpath, "rb") as f:
            resp = requests.post(
                f"{host}/api/v1/logs/runs/{run_id}/upload",
                files={"file": (fname, f, "application/octet-stream")},
                headers=headers,
                timeout=120,
            )
        if resp.status_code != 200:
            click.echo(f"  Failed: {resp.status_code} {resp.text}", err=True)
        else:
            click.echo(f"  OK: {fname}")

    click.echo(f"\nDone. View at {host}/viz?run={run_id}")


@cli.command()
@click.argument("logdir", type=click.Path(exists=True))
@click.option("--port", default=8040, help="Port to listen on")
def serve(logdir, port):
    """Start a local read-only visualization server (standalone mode).

    This uses Python's built-in HTTP server — no backend required.
    Useful for quick inspection of log files without a running backend.
    """
    import http.server
    import socketserver

    # Simple note: full standalone server requires frontend integration.
    # For now, print a summary of what's in the log.
    from deepinsight_sdk.log_reader import LogReader

    reader = LogReader(logdir)
    tags = reader.tags()
    click.echo(f"Log directory: {logdir}")
    click.echo(f"Tags found: {len(tags)}")
    for tag in tags:
        records = list(reader.read_tag(tag))
        click.echo(f"  {tag}: {len(records)} records, steps {records[0].step}..{records[-1].step}" if records else f"  {tag}: empty")

    # Start simple HTTP server for browsing raw files
    handler = http.server.SimpleHTTPRequestHandler
    os.chdir(logdir)
    click.echo(f"\nServing raw files at http://localhost:{port}")
    with socketserver.TCPServer(("", port), handler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            click.echo("\nShutting down.")


@cli.command()
@click.argument("logdir", type=click.Path(exists=True))
def info(logdir):
    """Print summary information about a log directory."""
    from deepinsight_sdk.log_reader import LogReader

    reader = LogReader(logdir)
    tags = reader.tags()
    click.echo(f"Log directory: {logdir}")
    click.echo(f"Number of tags: {len(tags)}")
    total_records = 0
    for tag in tags:
        records = list(reader.read_tag(tag))
        total_records += len(records)
        rtype = type(records[0]).__name__ if records else "empty"
        first_step = records[0].step if records else "-"
        last_step = records[-1].step if records else "-"
        click.echo(f"  [{rtype}] {tag}: {len(records)} records, steps {first_step}..{last_step}")
    click.echo(f"\nTotal records: {total_records}")


if __name__ == "__main__":
    cli()
