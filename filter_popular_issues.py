#!/usr/bin/env python3
"""
Filter GitHub issues from FasterXML/jackson-databind with 5+ upvotes.

This script fetches all open issues from the jackson-databind repository
and filters them by the number of thumbs-up reactions (upvotes).

Usage:
    python filter_popular_issues.py [--min-upvotes N] [--state open|closed|all] [--token YOUR_TOKEN]

Environment:
    GITHUB_TOKEN: Optional GitHub personal access token for higher rate limits
"""

import argparse
import os
import sys
from typing import List, Dict
import urllib.request
import urllib.error
import json
import time


def fetch_issues(owner: str, repo: str, state: str = "open", token: str = None) -> List[Dict]:
    """
    Fetch all issues from a GitHub repository.

    Args:
        owner: Repository owner (e.g., 'FasterXML')
        repo: Repository name (e.g., 'jackson-databind')
        state: Issue state filter ('open', 'closed', 'all')
        token: Optional GitHub personal access token

    Returns:
        List of issue dictionaries
    """
    issues = []
    page = 1
    per_page = 100

    headers = {
        'Accept': 'application/vnd.github.v3+json',
        'User-Agent': 'Jackson-Issue-Filter/1.0'
    }

    if token:
        headers['Authorization'] = f'token {token}'

    while True:
        url = f'https://api.github.com/repos/{owner}/{repo}/issues?state={state}&page={page}&per_page={per_page}'

        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req) as response:
                page_issues = json.loads(response.read().decode('utf-8'))

                if not page_issues:
                    break

                # Filter out pull requests (they're returned by the issues API)
                page_issues = [issue for issue in page_issues if 'pull_request' not in issue]
                issues.extend(page_issues)

                print(f"Fetched page {page} ({len(page_issues)} issues)...", file=sys.stderr)
                page += 1

                # Be nice to the API
                time.sleep(0.5)

        except urllib.error.HTTPError as e:
            if e.code == 403:
                print(f"Rate limit exceeded. Consider using a GitHub token.", file=sys.stderr)
                print(f"Error: {e.read().decode('utf-8')}", file=sys.stderr)
            else:
                print(f"HTTP Error {e.code}: {e.read().decode('utf-8')}", file=sys.stderr)
            sys.exit(1)
        except Exception as e:
            print(f"Error fetching issues: {e}", file=sys.stderr)
            sys.exit(1)

    return issues


def get_upvotes(issue: Dict) -> int:
    """
    Get the number of thumbs-up reactions for an issue.

    Args:
        issue: Issue dictionary from GitHub API

    Returns:
        Number of thumbs-up reactions
    """
    reactions = issue.get('reactions', {})
    return reactions.get('+1', 0)


def filter_by_upvotes(issues: List[Dict], min_upvotes: int) -> List[Dict]:
    """
    Filter issues by minimum upvote count.

    Args:
        issues: List of issue dictionaries
        min_upvotes: Minimum number of upvotes required

    Returns:
        Filtered list of issues
    """
    filtered = [issue for issue in issues if get_upvotes(issue) >= min_upvotes]
    # Sort by upvotes (descending)
    filtered.sort(key=get_upvotes, reverse=True)
    return filtered


def print_issues(issues: List[Dict], min_upvotes: int):
    """
    Print filtered issues in a readable format.

    Args:
        issues: List of issue dictionaries to print
        min_upvotes: Minimum upvotes threshold (for display)
    """
    print(f"\nFound {len(issues)} issues with {min_upvotes}+ upvotes:\n")
    print("=" * 80)

    for issue in issues:
        upvotes = get_upvotes(issue)
        number = issue['number']
        title = issue['title']
        url = issue['html_url']
        state = issue['state']
        labels = [label['name'] for label in issue.get('labels', [])]

        print(f"\n#{number} - {title}")
        print(f"Upvotes: 👍 {upvotes}")
        print(f"State: {state}")
        if labels:
            print(f"Labels: {', '.join(labels)}")
        print(f"URL: {url}")
        print("-" * 80)


def export_to_json(issues: List[Dict], filename: str):
    """
    Export filtered issues to a JSON file.

    Args:
        issues: List of issue dictionaries
        filename: Output filename
    """
    simplified_issues = []
    for issue in issues:
        simplified_issues.append({
            'number': issue['number'],
            'title': issue['title'],
            'state': issue['state'],
            'upvotes': get_upvotes(issue),
            'url': issue['html_url'],
            'labels': [label['name'] for label in issue.get('labels', [])],
            'created_at': issue['created_at'],
            'updated_at': issue['updated_at']
        })

    with open(filename, 'w') as f:
        json.dump(simplified_issues, f, indent=2)

    print(f"\nExported {len(issues)} issues to {filename}")


def main():
    parser = argparse.ArgumentParser(
        description='Filter GitHub issues by upvote count',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
Examples:
  python filter_popular_issues.py
  python filter_popular_issues.py --min-upvotes 10
  python filter_popular_issues.py --state all --output popular_issues.json
  python filter_popular_issues.py --token ghp_xxxxx
        '''
    )

    parser.add_argument(
        '--min-upvotes',
        type=int,
        default=5,
        help='Minimum number of upvotes (default: 5)'
    )

    parser.add_argument(
        '--state',
        choices=['open', 'closed', 'all'],
        default='open',
        help='Issue state to filter (default: open)'
    )

    parser.add_argument(
        '--token',
        help='GitHub personal access token (or set GITHUB_TOKEN env var)'
    )

    parser.add_argument(
        '--output',
        '-o',
        help='Export results to JSON file'
    )

    args = parser.parse_args()

    # Get token from args or environment
    token = args.token or os.environ.get('GITHUB_TOKEN')

    if not token:
        print("Warning: No GitHub token provided. Rate limits will be lower.", file=sys.stderr)
        print("Consider setting GITHUB_TOKEN environment variable or using --token flag.\n", file=sys.stderr)

    print(f"Fetching {args.state} issues from FasterXML/jackson-databind...", file=sys.stderr)

    issues = fetch_issues('FasterXML', 'jackson-databind', state=args.state, token=token)
    print(f"Total issues fetched: {len(issues)}", file=sys.stderr)

    filtered_issues = filter_by_upvotes(issues, args.min_upvotes)

    print_issues(filtered_issues, args.min_upvotes)

    if args.output:
        export_to_json(filtered_issues, args.output)


if __name__ == '__main__':
    main()
