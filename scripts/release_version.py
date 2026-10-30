#!/usr/bin/env python3
"""Resolve Android release versions from Google Play and GitHub releases.

Google Play is the authoritative source for used versionCodes. GitHub stable
release tags and Play production release names determine the semantic version.
No release is uploaded or published by this script.
"""
import argparse
import json
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path

PACKAGE_NAME = "hu.merenyimiklos.meterreader"
PLAY_API = "https://androidpublisher.googleapis.com/androidpublisher/v3"
PLAY_SCOPE = "https://www.googleapis.com/auth/androidpublisher"
MAX_PLAY_VERSION_CODE = 2_100_000_000
STABLE_TAG = re.compile(r"^v?(\d+)\.(\d+)\.(\d+)$")
RELEASE_NAME = re.compile(r"(?:^|[^\d])(\d+)\.(\d+)\.(\d+)$")
CODE_IN_NOTES = re.compile(r"(?im)^-\s*Version code:\s*(\d+)\s*$")


@dataclass(frozen=True, order=True)
class Version:
    major: int
    minor: int
    patch: int

    @classmethod
    def parse(cls, value):
        matched = STABLE_TAG.fullmatch(value.strip())
        return cls(*(int(part) for part in matched.groups())) if matched else None

    def bump(self, change):
        if change == "major":
            return Version(self.major + 1, 0, 0)
        if change == "minor":
            return Version(self.major, self.minor + 1, 0)
        if change == "patch":
            return Version(self.major, self.minor, self.patch + 1)
        raise ValueError(f"Unknown release type: {change}")

    def __str__(self):
        return f"{self.major}.{self.minor}.{self.patch}"


def next_version(releases, tracks, baseline, change):
    stable = [baseline]
    for release in releases:
        if release.get("draft") or release.get("prerelease"):
            continue
        version = Version.parse(release.get("tag_name") or "")
        if version is not None:
            stable.append(version)
    # A valid published production release can be newer than a GitHub tag
    # (for example if an earlier workflow failed while creating the tag).
    for track in tracks:
        if track.get("track") != "production":
            continue
        for release in track.get("releases") or []:
            match = RELEASE_NAME.search(release.get("name") or "")
            if match:
                stable.append(Version(*(int(part) for part in match.groups())))
    return max(stable).bump(change)


def next_code(releases, play_state, baseline):
    used = [baseline]
    for release in releases:
        for match in CODE_IN_NOTES.finditer(release.get("body") or ""):
            used.append(int(match.group(1)))
    if play_state is not None:
        for bundle in play_state.get("bundles") or []:
            used.append(int(bundle["versionCode"]))
        for apk in play_state.get("apks") or []:
            used.append(int(apk["versionCode"]))
        for track in play_state.get("tracks") or []:
            for release in track.get("releases") or []:
                used.extend(int(code) for code in release.get("versionCodes") or [])
    code = max(used) + 1
    if code > MAX_PLAY_VERSION_CODE:
        raise ValueError("Google Play versionCode limit exceeded.")
    return code


def load_play_state():
    """Read-only inspection in a temporary Play edit; fail closed on API errors."""
    try:
        from google.oauth2 import service_account
        from google.auth.transport.requests import AuthorizedSession
    except ImportError as exc:
        raise RuntimeError("Install google-auth[requests] for Play releases.") from exc

    credentials_json = os.environ.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON")
    if not credentials_json:
        raise RuntimeError("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON secret is missing.")
    credentials = service_account.Credentials.from_service_account_info(
        json.loads(credentials_json), scopes=[PLAY_SCOPE]
    )
    session = AuthorizedSession(credentials)
    url = f"{PLAY_API}/applications/{PACKAGE_NAME}/edits"
    response = session.post(url, json={}, timeout=40)
    response.raise_for_status()
    edit_id = response.json()["id"]
    try:
        state = {}
        for endpoint in ("bundles", "apks", "tracks"):
            response = session.get(f"{url}/{edit_id}/{endpoint}", timeout=40)
            response.raise_for_status()
            state[endpoint] = response.json().get(endpoint) or []
        return state
    finally:
        # This edit has never been committed or used to modify the app.
        try:
            session.delete(f"{url}/{edit_id}", timeout=30).raise_for_status()
        except Exception as exc:
            print(f"::warning::Could not remove the read-only Play edit: {exc}", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bump", choices=("patch", "minor", "major"), required=True)
    parser.add_argument("--channel", choices=("stable", "beta"), required=True)
    parser.add_argument("--run-number", type=int, required=True)
    parser.add_argument("--github-releases", type=Path, required=True)
    parser.add_argument("--publish-to-play", choices=("true", "false"), required=True)
    parser.add_argument("--baseline-version", default="1.0.0")
    parser.add_argument("--baseline-code", type=int, default=20)
    parser.add_argument("--github-env", type=Path, required=True)
    parser.add_argument("--github-output", type=Path, required=True)
    args = parser.parse_args()

    releases = json.loads(args.github_releases.read_text(encoding="utf-8"))
    if not isinstance(releases, list):
        raise ValueError("GitHub releases response is not a list.")
    play_state = load_play_state() if args.publish_to_play == "true" else None

    # For an offline/artifact-only release, rely on GitHub metadata and the
    # checked-in baseline. Online releases always consult Play first.
    base = Version.parse(args.baseline_version)
    if base is None:
        raise ValueError("Invalid baseline version.")
    version = next_version(
        releases, (play_state or {}).get("tracks") or [], base, args.bump
    )
    code = next_code(releases, play_state, args.baseline_code)
    name = str(version)
    if args.channel == "beta":
        name += f"-beta.{args.run_number}"

    with args.github_env.open("a", encoding="utf-8") as file:
        file.write(f"APP_VERSION_CODE={code}\n")
        file.write(f"APP_VERSION_NAME={name}\n")
    with args.github_output.open("a", encoding="utf-8") as file:
        file.write(f"name={name}\n")
        file.write(f"code={code}\n")
    print(f"Resolved {args.channel} version: {name} ({code})")
    if play_state is None:
        print("::warning::Offline release: no Play versionCode check was performed.")


if __name__ == "__main__":
    main()
