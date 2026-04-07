#!/usr/bin/env python3
"""
create_release.py

Usage:
    python create_release.py <version> [--branches 2024.3 2025.1 ...] [--dry-run]

Example:
    python create_release.py 1.2.3
    python create_release.py 1.2.3 --branches 2024.3 2025.1 2025.2 2025.3
    python create_release.py 1.2.3 --dry-run
"""

from __future__ import annotations
import argparse
import subprocess
import sys
from typing import List, Tuple

BRANCHES=["2025.2", "2025.3", "2026.1"]

def check_file_contains_version(path: str, version: str) -> bool:
    try:
        with open(path, "r") as fh:
            data = fh.read()
    except Exception as e:
        print(f"Unable to read file {path}: {e}", file=sys.stderr)
        return False
    return version in data

def sanity_check_file_versions(version: str) -> bool:
    result = True
    for path in ["readme.md", "changelog.md", "gradle.properties"]:
        if not check_file_contains_version(path, version):
            print(f"File {path} does not contain the version number")
            result = False
    return result

def run_git(args: List[str], dry_run: bool = False, capture_output: bool = True) -> Tuple[int, str, str]:
    """Run git command and return (returncode, stdout, stderr)."""
    if dry_run:
        print(f"[dry_run] would: git {" ".join(args)}")
        return 0, "", ""

    proc = subprocess.run(
        ["git"] + args,
        stdout=subprocess.PIPE if capture_output else None,
        stderr=subprocess.PIPE if capture_output else None,
        text=True,
        shell=False,
    )
    stdout = proc.stdout or ""
    stderr = proc.stderr or ""
    return proc.returncode, stdout.strip(), stderr.strip()

def working_tree_clean() -> bool:
    rc, out, _ = run_git(["status", "--porcelain"])
    return out.strip() == ""

def ensure_branch_local_or_origin(branch: str, dry_run: bool):
    # if local branch exists, do nothing
    rc, _, _ = run_git(["show-ref", "--verify", f"refs/heads/{branch}"])
    if rc == 0:
        return
    # local branch not present, check origin/<branch>
    rc, _, _ = run_git(["ls-remote", "--exit-code", "--heads", "origin", branch])
    if rc == 0:
        print(f"Creating local branch '{branch}' from origin/{branch}...")
        rc2, out, err = run_git(["checkout", "-b", branch, f"origin/{branch}"], dry_run, capture_output=True)
        if dry_run:
            return
        if rc2 != 0:
            print(f"Failed to create local branch {branch} from origin/{branch}.", file=sys.stderr)
            print(out, err, sep="\n", file=sys.stderr)
            sys.exit(1)
        return
    # neither local nor origin branch exists
    print(f"Branch '{branch}' does not exist locally nor on origin. Aborting.", file=sys.stderr)
    sys.exit(1)

def checkout_branch(branch: str, dry_run: bool):
    rc, out, err = run_git(["checkout", branch], dry_run)
    if dry_run:
        return
    if rc != 0:
        print(f"Failed to checkout branch '{branch}'.", file=sys.stderr)
        print(out, err, sep="\n", file=sys.stderr)
        sys.exit(1)

def pull_ff_only(branch: str, dry_run: bool):
    # Try to fast-forward only; if it fails, continue (we don't forcibly rebase here).
    rc, out, err = run_git(["pull", "--ff-only", "origin", branch], dry_run)
    if dry_run:
        return
    # It's fine if pull reports nothing to do (rc==0) or fails (rc != 0); we'll continue,
    # but if rc != 0 we print a helpful message.
    if rc != 0:
        print(f"Warning: 'git pull --ff-only origin {branch}' returned non-zero. Continuing, but check branch state.")
        if out:
            print(out)
        if err:
            print(err)

def merge_source_into_target(source: str, target: str, dry_run: bool):
    print(f"Merging {source} -> {target}...")
    rc, out, err = run_git(["merge", "--no-edit", source], dry_run, capture_output=True)
    if dry_run:
        return True
    if rc == 0:
        print(out)
        return True
    # Merge failed. Check for conflicts
    print("Merge command returned non-zero. Checking for conflicts...")
    rc2, status_out, _ = run_git(["status", "--porcelain"])
    if any(line.startswith(("UU","AA","DU","UD","AU","UA","??")) for line in status_out.splitlines()):
        print("Merge appears to have conflicts. Attempting to abort the merge...", file=sys.stderr)
        rc3, abort_out, abort_err = run_git(["merge", "--abort"])
        if rc3 != 0:
            print("Failed to abort merge automatically. You must resolve the repository state manually.", file=sys.stderr)
            if abort_out:
                print(abort_out)
            if abort_err:
                print(abort_err)
        else:
            print("Merge aborted.")
        sys.exit(1)
    else:
        # Non-conflict failure (some other error)
        print("Merge failed for an unknown reason.", file=sys.stderr)
        if out:
            print(out)
        if err:
            print(err, file=sys.stderr)
        sys.exit(1)

def do_push(branch_name: str, dry_run: bool):
    print(f"Pushing branch {branch_name}")
    rc, out, err = run_git(["push"], dry_run)
    if rc != 0:
        print(f"Failed to push branch {branch_name}", file=sys.stderr)
        if out:
            print(out)
        if err:
            print(err)
        sys.exit(1)

def tag_and_push(branch: str, version: str, push: bool, dry_run: bool):
    tag = f"{version}-{branch}"
    # check tag does not already exist
    rc, _, _ = run_git(["rev-parse", "-q", "--verify", f"refs/tags/{tag}"])
    if rc == 0:
        print(f"Tag '{tag}' already exists. Aborting to avoid overwriting.", file=sys.stderr)
        sys.exit(1)
    print(f"Creating tag '{tag}'...")
    rc, out, err = run_git(["tag", tag], dry_run)
    if rc != 0:
        print(f"Failed to create tag {tag}.", file=sys.stderr)
        if out:
            print(out)
        if err:
            print(err)
        sys.exit(1)
    if push:
        print(f"Pushing tag '{tag}' to origin...")
        rc, out, err = run_git(["push", "origin", tag], dry_run)
        if rc != 0:
            print(f"Failed to push tag {tag} to origin.", file=sys.stderr)
            if out:
                print(out)
            if err:
                print(err)
            sys.exit(1)

def fetch_origin(dry_run: bool):
    print("Fetching origin ...")
    rc, out, err = run_git(["fetch", "origin"], dry_run)
    if rc != 0:
        print("Warning: 'git fetch origin' returned non-zero.", file=sys.stderr)
        if out:
            print(out)
        if err:
            print(err)

def parse_args():
    p = argparse.ArgumentParser(description="Merge release branches and create/push tags non-interactively.")
    p.add_argument("version", help="version string to append to tags (used as <version>-<branch>)")
    p.add_argument("--branches", nargs="+",
                   default=BRANCHES,
                   help="space-separated list of branches in order (default: %(default)s)")
    p.add_argument("--dry-run", action="store_true", help="show commands without executing them")
    p.add_argument("--no-push", dest="push", action="store_false", help="do not push tags to origin")
    return p.parse_args()

def main():
    args = parse_args()
    version = args.version
    branches = args.branches
    dry_run = args.dry_run
    push = args.push

    if not sanity_check_file_versions(version):
        sys.exit(1)

    if not working_tree_clean():
        print("Working tree is not clean. Please commit or stash changes before running this script.", file=sys.stderr)
        rc, status_out, _ = run_git(["status", "--porcelain"])
        if status_out:
            print(status_out)
        sys.exit(1)

    fetch_origin(dry_run=dry_run)

    prev = "dev"
    for br in branches:
        print("\n" + "="*60)
        print(f"Processing branch: {br}  (merge {prev} -> {br})")
        print("="*60)

        ensure_branch_local_or_origin(br, dry_run=dry_run)
        checkout_branch(br, dry_run=dry_run)
        pull_ff_only(br, dry_run=dry_run)

        merge_ok = merge_source_into_target(prev, br, dry_run=dry_run)
        if not merge_ok:
            print(f"Merge of {prev} into {br} failed. Aborting.", file=sys.stderr)
            sys.exit(1)

        if push:
            do_push(br, dry_run=dry_run)

        tag_and_push(br, version, push=push, dry_run=dry_run)

        prev = br

    print("\nSwitching back to dev branch...")
    checkout_branch("dev", dry_run=dry_run)

    print("\nAll done. Created tags for version", version, "on branches:", ", ".join(branches))

if __name__ == "__main__":
    main()
