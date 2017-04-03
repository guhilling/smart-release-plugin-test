package e2e;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTagWithModuleVersion;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

import java.io.IOException;
import java.util.List;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ParentAsSiblingTest {

    public static final String GROUP_ID = "com.github.danielflower.mavenplugins.testprojects.parentassibling";
    final String expectedAggregatorVersion = "1.0";
    final String expectedParentVersion = "1.0";
    final String expectedCoreVersion = "2.0";
    final String expectedAppVersion = "3.0";
    final TestProject testProject = TestProject.parentAsSibilngProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        buildsEachProjectOnceAndOnlyOnce(testProject.mvnRelease());
        installsAllModulesIntoTheRepoWithTheBuildNumber();
        theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion();
    }

    private void buildsEachProjectOnceAndOnlyOnce(List<String> commandOutput) throws Exception {
        assertThat(
            commandOutput,
            allOf(
                oneOf(containsString("Going to release parent-as-sibling " + expectedAggregatorVersion)),
                twoOf(containsString("Building parent-as-sibling")), // once for initial build; once for release build
                oneOf(containsString("Building parent-module")),
                oneOf(containsString("Building core-utils")),
                oneOf(containsString("Building console-app")),
                oneOf(containsString("The Calculator Test has run"))
            )
        );
    }

    private void installsAllModulesIntoTheRepoWithTheBuildNumber() throws Exception {
        assertArtifactInLocalRepo(GROUP_ID, "parent-as-sibling", expectedAggregatorVersion);
        assertArtifactInLocalRepo(GROUP_ID, "parent-module", expectedParentVersion);
        assertArtifactInLocalRepo(GROUP_ID, "core-utils", expectedCoreVersion);
        assertArtifactInLocalRepo(GROUP_ID, "console-app", expectedAppVersion);
    }

    private void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException, InterruptedException {
        assertTagExists("parent-as-sibling", expectedAggregatorVersion);
        assertTagExists("parent-module", expectedParentVersion);
        assertTagExists("core-utils", expectedCoreVersion);
        assertTagExists("console-app", expectedAppVersion);
    }

    private void assertTagExists(String module, String expectedVersion) {
        assertThat(testProject.local, hasTagWithModuleVersion(GROUP_ID, module, expectedVersion));
        assertThat(testProject.origin, hasTagWithModuleVersion(GROUP_ID, module, expectedVersion));
    }

    // TODO fix test
    @Ignore
    @Test
    public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
        ObjectId originHeadAtStart = head(testProject.origin);
        ObjectId localHeadAtStart = head(testProject.local);
        assertThat(originHeadAtStart, equalTo(localHeadAtStart));
        testProject.mvnRelease();
        assertThat(head(testProject.origin), equalTo(originHeadAtStart));
        assertThat(head(testProject.local), equalTo(localHeadAtStart));
        assertThat(testProject.local, hasCleanWorkingDirectory());
    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRef("HEAD").getObjectId();
    }
}
