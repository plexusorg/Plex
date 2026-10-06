# Contributing

Contributions are welcome. Follow these rules when you contribute.

## Steps

1. Open an issue and wait for feedback. This tells you if the change will be accepted before you write any code.
   - For a feature request, describe the feature in detail.
   - For a bug report, give the steps to reproduce the bug and the result that you expected.
   - For an enhancement, describe the change that you propose in detail.
2. Fork this repository.
3. Create a branch with a name that describes the change. For example, `feature/add-xyz` is good. `fix-this-lol` is
   bad.
4. Write the code for your change.
   - In IntelliJ IDEA, the project includes the Plexus Code Style in `.idea/codeStyles`. IntelliJ uses it
     automatically.
   - In other editors, follow the Plexus Code Style. It is close to the Allman style: put each opening brace on its own
     line.
5. Run `./gradlew build` (on Windows, `gradlew.bat build`). The build must pass, including Checkstyle.
6. Push your branch and open a pull request from it.

## Pull request requirements

- The issue must be approved.
- Each pull request addresses one issue only.
- Your code must compile and work. If it does not, we will most likely reject the pull request.

## Code requirements

- Your code must be efficient. We can reject a pull request if the code is inefficient or sloppy.
- Do not repeat code. If you use a large block of code more than once, move it into a method.
- Do not add many small commits to your pull request. They make the project history hard to read.
- Follow the existing code. If a method already does what you need, use it.

## Documentation

Read the documentation at [plex.us.org](https://plex.us.org) for setup, configuration, and module development.
