# Security Policy

Security reports are welcome and should be handled responsibly.

## Supported Versions

NexRhythm is currently in pre-release development.

Security fixes are focused on:

- the current `main` branch; and
- the latest published release once public releases begin.

Older development snapshots are not guaranteed to receive security updates.

## Reporting a Vulnerability

Please **do not open a public GitHub issue containing vulnerability details, exploit steps, credentials, private data, or other sensitive information**.

Preferred reporting method:

1. Use GitHub's private vulnerability reporting / **Report a vulnerability** option if it is available for this repository.
2. If private vulnerability reporting is unavailable, establish private contact with **FRIZDEV** using a private contact method listed on the maintainer's GitHub profile before sending sensitive technical details.

A useful report should include:

- affected version or commit;
- affected device or Android version when relevant;
- clear reproduction steps;
- expected and observed behavior;
- security impact;
- proof-of-concept information when safe to provide privately;
- any suggested mitigation, if known.

## Response

Security reports are reviewed on a best-effort basis.

There is currently no guaranteed response-time or remediation-time SLA.

The maintainer may request additional information to reproduce and evaluate the issue.

## Responsible Disclosure

Please allow reasonable time for investigation and remediation before public disclosure.

When a vulnerability is confirmed, the project may:

- prepare a fix;
- add regression coverage when practical;
- publish a security advisory;
- credit the reporter if attribution is desired.

## Scope

Relevant security issues may include:

- unsafe handling of imported or local media;
- unintended file or data exposure;
- insecure Android component behavior;
- dependency vulnerabilities that materially affect NexRhythm;
- build or release issues that could compromise distributed application artifacts.

General feature requests, crashes without security impact, and ordinary functional bugs should use the normal issue templates instead.
