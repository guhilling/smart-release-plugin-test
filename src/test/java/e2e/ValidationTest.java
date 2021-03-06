package e2e;

import scaffolding.MavenExecutionException;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.CountMatcher.oneOf;
import static scaffolding.CountMatcher.twoOf;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ValidationTest {

    @Rule
    public TestProject singleProject              = new TestProject(ProjectType.SINGLE);
    @Rule
    public TestProject independentVersionsProject = new TestProject(ProjectType.INDEPENDENT_VERSIONS);

    @Test
    public void failsIfThereAreUntrackedFiles() throws IOException, InterruptedException {
        new File(singleProject.localDir, "untracked.txt").createNewFile();
        new File(singleProject.localDir, "someFolder").mkdir();
        new File(singleProject.localDir, "someFolder/anotherUntracked.txt").createNewFile();
        try {
            singleProject.checkClean = false;
            singleProject.mvnReleaseComplete();
            Assert.fail("Should not have worked the second time");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with uncommitted changes")));
            assertThat(mee.output, oneOf(containsString(" * untracked.txt")));
            assertThat(mee.output, oneOf(containsString(" * someFolder/anotherUntracked.txt")));
        }
    }

    @Test
    public void failsIfThereAreUncommittedFiles() throws IOException, InterruptedException, GitAPIException {
        new File(singleProject.localDir, "uncommitted.txt").createNewFile();
        singleProject.local.add().addFilepattern("uncommitted.txt").call();
        try {
            singleProject.checkClean = false;
            singleProject.mvnReleaseComplete();
            Assert.fail("Should not have worked as there are uncommitted files");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with uncommitted changes")));
            assertThat(mee.output, oneOf(containsString(" * uncommitted.txt")));
        }
    }

    @Test
    public void ifIOErrorOccursWhileUpdatingPomsThenThisIsReported() throws IOException, InterruptedException {
        independentVersionsProject.checkClean = false;
        independentVersionsProject.checkNoChanges = false;
        File pom = new File(independentVersionsProject.localDir, "console-app/pom.xml");
        pom.setWritable(false); // this should cause an IO exception when writing the pom
        try {
            independentVersionsProject.mvnReleaseComplete();
            Assert.fail("It was expected that this would fail due to a pom being readonly.");
        } catch (MavenExecutionException e) {
            assertThat(e.output,
                       twoOf(containsString("Unexpected exception while setting the release versions in the pom")));
            assertThat(e.output, oneOf(containsString("Going to revert changes because there was an error")));
        }
    }

    @Test
    public void failsIfThereAreDependenciesOnSnapshotVersionsThatAreNotPartOfTheReactor() throws Exception {
        TestProject badOne = TestProject.project(ProjectType.SNAPSHOT_DEPENDENCIES);
        badOne.checkClean = false;
        badOne.checkNoChanges = false;
        independentVersionsProject.mvn("install");


        badOne.mvn("install"); // this should work as the snapshot dependency is in the local repo

        try {
            badOne.mvnReleaseComplete();
            Assert.fail("Should not have worked as there are snapshot dependencies");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with references to snapshot dependencies")));
            assertThat(mee.output, oneOf(containsString("The following dependency errors were found:")));
            assertThat(mee.output,
                       oneOf(containsString(" * The parent of snapshot-dependencies is independent-versions")));
            assertThat(mee.output, oneOf(containsString(" * snapshot-dependencies references dependency core-utils")));
        }
    }

    @Test
    public void failsIfThereAreDependenciesOnSnapshotVersionsWithVersionPropertiesThatAreNotPartOfTheReactor() throws
                                                                                                               Exception {
        // Install the snapshot dependency so that it can be built
        TestProject badOne = TestProject.project(ProjectType.SNAPSHOT_DEPENDENCIES_VIA_PROPERTIES);
        badOne.checkClean = false;
        badOne.checkNoChanges = false;
        independentVersionsProject.mvn("install");



        badOne.mvn("install"); // this should work as the snapshot dependency is in the local repo

        try {
            badOne.mvnReleaseComplete();
            Assert.fail("Should not have worked as there are snapshot dependencies");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with references to snapshot dependencies")));
            assertThat(mee.output, oneOf(containsString("The following dependency errors were found:")));
            assertThat(mee.output, oneOf(
            containsString(" * snapshot-dependencies-with-version-properties references dependency core-utils")));
        }
    }
}
