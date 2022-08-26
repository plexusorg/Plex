For those who are wanting to contribute, we fully encourage doing so. There are a few rules we require following when
contributing.

## Steps

1. Make an issue and get feedback. It's important to know if your idea will be accepted before writing any code.

- If it is a feature request, describe the feature and be extremely specific.
- If it is a bug report, ensure you include how to reproduce the bug and the expected outcome
- If it is an enhancement, describe your proposed changes. Ensure you are extremely specific.

2. Fork this project
3. Create a new branch that describes the new feature, enhancement, or bug fix. For example, this is
   good: `feature/add-xyz`. This is bad: `fix-this-lol`.
4. Write the code that addresses your change.

- Keep in mind that it **must** be formatted correctly. If you are using IntelliJ, there is a `codeStyle.xml` file that
  tells IntelliJ how to format your code. Check this link for information on how to use the
  file: https://www.jetbrains.com/help/idea/configuring-code-style.html#import-export-schemes
- If you are not using IntelliJ, that is fine. We use the Plexus Code Style (which is almost the same as Allman) so
  please format your code accordingly.

6. Push your changes to your new branch and make a PR based off of that branch.

## Requirements for a PR

- The issue must be marked as approved
- It must only address each specific issue. Don't make one PR for multiple issues.
- Your PR must compile and work. If it does not compile or work, your PR will most likely be rejected.

## Code requirements

- Most importantly, your code must be efficient. Your pull request may be rejected if your code is deemed inefficient or
  sloppy.
- Do not repeat yourself. Create functions as needed if you're using large blocks of code over and over again.
- Do not use an excessive amount of commits when making your PR. It makes the master branch look messy.
- Your code must be consistent with Plex's codebase. If a function already exists, use it.
