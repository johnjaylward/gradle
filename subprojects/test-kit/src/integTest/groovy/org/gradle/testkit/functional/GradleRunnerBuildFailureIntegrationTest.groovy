/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testkit.functional

import org.gradle.util.TextUtil

import static org.gradle.testkit.functional.TaskResult.*

class GradleRunnerBuildFailureIntegrationTest extends AbstractGradleRunnerIntegrationTest {

    def "execute build for expected failure"() {
        given:
        buildFile << """
            task helloWorld {
                doLast {
                    throw new GradleException('Expected exception')
                }
            }
        """

        when:
        GradleRunner gradleRunner = prepareGradleRunner('helloWorld')
        BuildResult result = gradleRunner.buildAndFail()

        then:
        noExceptionThrown()
        result.standardOutput.contains(':helloWorld FAILED')
        result.standardError.contains("Execution failed for task ':helloWorld'")
        result.standardError.contains('Expected exception')
        result.tasks.collect { it.path } == [':helloWorld']
        result.taskPaths(SUCCESS).empty
        result.taskPaths(SKIPPED).empty
        result.taskPaths(UPTODATE).empty
        result.taskPaths(FAILED) == [':helloWorld']
    }

    def "execute build for expected failure but succeeds"() {
        given:
        buildFile << helloWorldTask()

        when:
        GradleRunner gradleRunner = prepareGradleRunner('helloWorld')
        gradleRunner.buildAndFail()

        then:
        Throwable t = thrown(UnexpectedBuildSuccess)
        String expectedMessage = """Unexpected build execution success in ${TextUtil.escapeString(gradleRunner.workingDir.canonicalPath)} with arguments \\u005BhelloWorld\\u005D

Output:
:helloWorld
Hello world!

BUILD SUCCESSFUL

Total time: .+ secs

-----
Error:

-----"""
        TextUtil.normaliseLineSeparators(t.message) ==~ expectedMessage
    }

    def "execute build for expected success but fails"() {
        given:
        buildFile << """
            task helloWorld {
                doLast {
                    throw new GradleException('Unexpected exception')
                }
            }
        """

        when:
        GradleRunner gradleRunner = prepareGradleRunner('helloWorld')
        gradleRunner.build()

        then:
        Throwable t = thrown(UnexpectedBuildFailure)
        String expectedMessage = """Unexpected build execution failure in ${TextUtil.escapeString(gradleRunner.workingDir.canonicalPath)} with arguments \\u005BhelloWorld\\u005D

Output:
:helloWorld FAILED

BUILD FAILED

Total time: .+ secs

-----
Error:

FAILURE: Build failed with an exception.

\\u002A Where:
Build file '${TextUtil.escapeString(new File(gradleRunner.workingDir, "build.gradle").canonicalPath)}' line: 4

\\u002A What went wrong:
Execution failed for task ':helloWorld'.
> Unexpected exception

\\u002A Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

-----
Reason:
Unexpected exception
-----"""
        TextUtil.normaliseLineSeparators(t.message) ==~ expectedMessage
    }

}