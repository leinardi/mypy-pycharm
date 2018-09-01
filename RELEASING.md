# Releasing

1. Bump the `version` property in `gradle.properties` based on Major.Minor.Patch naming scheme
2. Update `CHANGELOG.md` for the impending release.
3. Update the `README.md` with the new version.
4. `./gradlew clean buildPlugin` 
5. `git commit -am "Prepare for release X.Y.Z"` (where X.Y.Z is the version you set in step 1)
6. `git push`
7. `./gradlew publishPlugin`
8. Create a new release on Github
    1. Tag version `X.Y.Z` (`git tag -s X.Y.Z && git push --tags`)
    2. Release title `X.Y.Z`
    3. Paste the content from `CHANGELOG.md` as the description
    4. Upload the `build/distributions/Mypy-X.X.X.zip`
9. Create a PR from [master](../../tree/master) to [release](../../tree/release)
