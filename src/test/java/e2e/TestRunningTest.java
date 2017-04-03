package e2e;

import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;

import java.io.IOException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.versioning.ReleaseDateSingleton;

public class TestRunningTest {
    final TestProject projectWithTestsThatFail = TestProject.moduleWithTestFailure();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void doesNotReleaseIfThereAreTestFailuresButTagsAreStillWritten() throws Exception {
        try {
            projectWithTestsThatFail.mvnRelease();
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
        }
        assertThat(projectWithTestsThatFail.local, hasCleanWorkingDirectory());
        String expectedTagName = expectedTagName();
        assertThat(projectWithTestsThatFail.local.tagList().call().get(0).getName(), startsWith(expectedTagName));
        assertThat(projectWithTestsThatFail.origin.tagList().call().get(0).getName(), startsWith(expectedTagName));
    }

    public String expectedTagName() {
        final String fullTag = "refs/tags/module-with-test-failure-" + ReleaseDateSingleton.getInstance()
                                                                                           .asFileSuffix();
        return fullTag.substring(0, fullTag.length() - 2);
    }

    @Test
    public void ifTestsAreSkippedYouCanReleaseWithoutRunningThem() throws IOException {
        projectWithTestsThatFail.mvn("-DbuildNumber=1", "-DskipTests", "releaser:release");
    }
}
