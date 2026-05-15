# Repository Instructions

- Use Maven normally from the repository root. `.mvn/maven.config` sets `-Dmaven.repo.local=.mvn/repository`, so dependencies stay in the repo-local Maven cache.
- Never use a Maven local repository under `target`, including `target/m2-verify` or `target/.m2-verify`.
