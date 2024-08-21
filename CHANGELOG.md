**[0.16.3] 2024-08-21**

- Fixed #120: Defined action update thread for actions
- New: Min IDEA version raised from PC-2022.1.4 to PC-2023.3.6
- Several dependency updates

**[0.16.2] 2023-10-16**

- Fix compatibility issues

**[0.16.1] 2023-10-09**

- Fix several compatibility issues

**[0.16.0] 2023-08-31**

- Fixed #63: Remote Interpreter
- Fixed #110: Support for mypy using WSL interpreter
- Respect mypy path field and don't force usage of project interpreter

**[0.15.0] 2023-04-24**

- Fixed #107: Icons not visible in new Jetbrains UI
- New: Min IDEA version raised from PC-2021.2 to PC-2022.1.4
- Several dependency updates

**[0.14.0] 2022-02-26**

- Change default inspection level to ERROR (thanks to @intgr)
- New: Make plugin hot-reloadable (thanks to @intgr)
- New: Show notification when Mypy exits abnormally (thanks to @intgr)
- New: Improved executable auto-detection on Windows
- Several dependency updates

**[0.12.1] 2021-12-06**

- New: Minimum compatibility version raised to 201.8743

**[0.12.0] 2021-12-05**

- Fixed #43: Major performance issues with background scanning (a huge thank you to @intgr for fixing this issue!)
- New: Ability to add suppress comments for Mypy violations via `IntentionAction` (again, thanks to @intgr for the
  contribution. See #81 for details)
- New: Min IDEA version raised from 2018 to PC-2021.2.3
- Several dependency updates

**[0.11.2] 2020-04-25**

- Fixed #61: Changed module/project icons to be compatible with EAPs of IDEA 2020.1

**[0.11.1] 2020-02-04**

- Fixed regression generating several errors in Event Log during inspection

**[0.11.0] 2020-02-01**

- New: Min IDEA version raised from 2016 to 2018
- New: Improved Mypy custom arguments handling
- New: Tidied up deprecations in the 2018 SDK
- New: Fixed possible deadlock during inspection

**[0.10.6] 2019-09-15**

- New: Improved error handling

**[0.10.5] 2019-09-13**

- New: Improved error handling

**[0.10.4] 2019-06-23**

- New: Implementing a better virtualenv detection

**[0.10.3] 2018-09-25**

- Fix #21: use `follow-imports=silent`

**[0.10.2] 2018-09-25**

- Fix #15: SyntaxError: Non-UTF-8 code starting with '\x90' when interpreter is set on Windows

**[0.10.1] 2018-09-21**

- Fix #12: Mypy absolute path not working on Windows
- New: Mypy auto-detections for Windows

**[0.10.0] 2018-09-17**

- New: Support scanning inside current Virtualenv
- New: Improved Mypy auto-detection
- New: Option to install Mypy if missing
- New: Settings button now opens File | Settings | Mypy
- New: Minimum compatibility version raised to 163.15529
- New: Added ability to optionally specify a mypy config file

**[0.8.1] 2018-09-08**

- Fix #7: Don't show the 'invalid syntax' message for real-time scan

**[0.8.0] 2018-09-05**

- Fix #2: Showing better info to the user if Mypy is missing

**[0.7.0] 2018-09-02**

- Initial release
