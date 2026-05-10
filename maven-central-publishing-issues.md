# Maven Central Publishing — Issues and Resolutions

A chronological record of every problem encountered while setting up
`mvn release:prepare release:perform` to publish this library to Maven Central,
and the steps taken to resolve each one.

---

## 1. `release:prepare` fails — "You have local modifications"

**Error:** `Cannot prepare the release because you have local modifications`

**Cause:** `mvn release:prepare` requires a completely clean git working tree
before it will run.

**Fix:** Commit all pending changes before running the release command.

---

## 2. Spotless check fails during `release:prepare`

**Error:** `spotless:check` violations on every `mvn release:prepare` run.

**Cause:** The default `preparationGoals` for the release plugin is
`clean verify`. During `verify`, the Maven lifecycle runs through the
`initialize` phase first, where `license-maven-plugin` (bound to that phase)
rewrote all Java source file headers in raw template format. Then
`spotless:check` (bound to `verify`) failed because the reformatted headers no
longer matched Google Java Format output.

**Fix:** Set `preparationGoals` to `clean` in the `maven-release-plugin`
configuration, skipping the `verify` phase entirely during release preparation:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-release-plugin</artifactId>
  <configuration>
    <preparationGoals>clean</preparationGoals>
  </configuration>
</plugin>
```

---

## 3. `license-maven-plugin` and spotless perpetually conflict

**Cause:** `license-maven-plugin` and spotless fought each other on every build:

- `license:format` (in `initialize` phase) rewrote Java license headers using
  the raw template — no `<p>` tags, different indentation.
- `spotless:check` (in `verify` phase) expected Google Java Format output, which
  uses `<p>` tags and different line wrapping in Javadoc comments.

Running either plugin always undid what the other had done, making it
impossible for `spotless:check` to pass after `license:format` ran.

**Fix:** Removed `license-maven-plugin` entirely and switched to spotless's
built-in `licenseHeader` step. Because spotless owns both the header writing and
the check, `spotless:apply` and `spotless:check` always agree:

```xml
<java>
  <licenseHeader>
    <file>${project.basedir}/src/main/resources/license-header.txt</file>
  </licenseHeader>
  <googleJavaFormat>...</googleJavaFormat>
  ...
</java>
```

Also rewrote `license-header.txt` as a proper Java `/* ... */` block comment
(previously it was plain text with Maven property placeholders that only the
license plugin could expand).

---

## 4. Spotless check fails during `release:perform`

**Error:** `spotless:check` violations when `release:perform` runs the `deploy`
lifecycle.

**Cause:** `release:perform` checks out the tagged commit and runs
`mvn deploy`, which includes the `verify` phase and therefore `spotless:check`.
The release plugin modifies `pom.xml` during checkout (version, SCM tag), and
those modifications are not formatted to spotless standards.

**Fix:** Added `spotless:apply` before `deploy` in the release plugin's
`<goals>` so it runs in the checkout directory before verification:

```xml
<goals>spotless:apply deploy</goals>
```

---

## 5. `~/.m2/settings.xml` was malformed

**Error:** Maven warning on every build:
`Expected root element 'settings' but found 'server'`

**Cause:** The `settings.xml` file contained only a bare `<server>` element
with no enclosing `<settings>` root element or `<servers>` wrapper.

**Fix:** Rewrote the file with the correct structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" ...>
  <servers>
    <server>
      <id>central</id>
      <username>TOKEN_USERNAME</username>
      <password>TOKEN_PASSWORD</password>
    </server>
  </servers>
</settings>
```

---

## 6. Publishing configured for legacy OSSRH instead of Maven Central Portal

**Error:** `401 Unauthorized` connecting to `https://s01.oss.sonatype.org/`

**Cause:** The project was wired for the legacy Sonatype OSSRH
(`s01.oss.sonatype.org`) using `nexus-staging-maven-plugin`, but the account
was registered on the new Maven Central Portal (`central.sonatype.com`), which
uses a completely different plugin and token-based authentication.

**Fix:** Three changes:

1. Removed the old `<distributionManagement>` section (OSSRH URLs).
2. Replaced `nexus-staging-maven-plugin` with `central-publishing-maven-plugin`:

```xml
<plugin>
  <groupId>org.sonatype.central</groupId>
  <artifactId>central-publishing-maven-plugin</artifactId>
  <version>0.7.0</version>
  <extensions>true</extensions>
  <configuration>
    <publishingServerId>central</publishingServerId>
    <autoPublish>true</autoPublish>
    <waitUntil>published</waitUntil>
  </configuration>
</plugin>
```

3. Updated `settings.xml` server `<id>` to `central` (matching
   `publishingServerId`) and replaced credentials with a token generated from
   `central.sonatype.com` → Account → Generate User Token.

---

## 7. Namespace not allowed — wrong groupId

**Error:** `Namespace 'net.snytkine.springboot' is not allowed`

**Cause:** The groupId `net.snytkine.springboot` was not a verified namespace
on the Maven Central Portal. The only verified namespace on the account was
`io.github.snytkine`.

**Fix:** Changed the groupId and renamed all Java packages to align
(since the library had never been successfully published, this was the right
time to make a clean break):

- `pom.xml`: `<groupId>net.snytkine.springboot</groupId>` →
  `<groupId>io.github.snytkine</groupId>`
- All 8 Java source files: package declarations and imports updated from
  `net.snytkine.springboot.rest_clients.*` to `io.github.snytkine.rest_clients.*`
- Directory structure under `src/main/java` and `src/test/java` renamed to
  match the new package.
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:
  class reference updated to the new package.

---

## 8. Stale `release.properties` overrides `pom.xml` configuration

**Cause:** When a `release:prepare` run fails partway through, it leaves a
`release.properties` file on disk. On the next run, the release plugin reads
this file and uses the `preparationGoals` value cached there, ignoring the
current `pom.xml` configuration. This caused fixes to `preparationGoals` in
`pom.xml` to have no effect.

**Fix:** Always run `mvn release:clean` to remove stale `release.properties`
and `pom.xml.releaseBackup` files before retrying a failed release.

---

## 9. Stale git tags from failed release attempts cause `release:perform` to build wrong code

**Cause:** After each failed `release:prepare` run, a git tag was left behind
pointing to a commit that had old or incorrect `pom.xml` content (wrong
groupId, wrong plugin, etc.). When `release:perform` ran later, it checked out
that stale tag and built the wrong code.

**Fix:** After any failed release, delete the stale tag from both local and
remote before retrying:

```bash
mvn release:clean
git tag -d <tag-name>
git push origin :refs/tags/<tag-name>
```

---

## 10. Stale `<scm><tag>` causes release plugin to tag the wrong commit

**Error:** `release:perform` deployed a SNAPSHOT version instead of the release
version.

**Cause:** The `<scm><tag>` element in `pom.xml` had a leftover value from an
old release (`spring-boot-starter-easy-rest-clients-0.0.1`) instead of `HEAD`.
During development this element must always be `HEAD` — the release plugin
updates it to the actual tag name as part of `release:prepare`. With a stale
value, the plugin's SCM operations became confused: it tagged the SNAPSHOT
commit rather than the version-bumped release commit, so `release:perform`
checked out and deployed SNAPSHOT code.

**Fix:** Reset `<scm><tag>` to `HEAD` in `pom.xml` before running the release:

```xml
<scm>
  ...
  <tag>HEAD</tag>
</scm>
```

The release plugin will replace `HEAD` with the actual tag name during
`release:prepare` and restore it to `HEAD` after `release:perform` completes.

---

## Summary checklist before every release

1. `<scm><tag>` in `pom.xml` is `HEAD`.
2. Working tree is clean (`git status` shows nothing to commit).
3. No leftover `release.properties` or `pom.xml.releaseBackup` — run
   `mvn release:clean` if unsure.
4. No stale git tags from a previous failed attempt — check with `git tag -l`.
5. `~/.m2/settings.xml` has a valid `central` server entry with current token
   credentials from `central.sonatype.com`.
6. Then run: `mvn release:prepare release:perform`
