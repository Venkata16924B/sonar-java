/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.Fail;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.assertj.core.api.Assertions.assertThat;

public class VisitorsBridgeTest {

  @Rule
  public LogTester logTester = new LogTester();

  private SonarComponents sonarComponents = null;

  private static final File FILE = new File("src/test/files/model/SimpleClass.java");
  private static final InputFile INPUT_FILE = TestUtils.inputFile(FILE);
  private static final CompilationUnitTree COMPILATION_UNIT_TREE = JParserTestUtils.parse(FILE);

  private static final NullPointerException NPE = new NullPointerException("BimBadaboum");
  private static final JavaFileScanner VISITOR_JAVA_FILE_SCANNER_NPE = c -> {
    throw NPE;
  };
  private static final JavaFileScanner VISITOR_SUBSCRIPTION_NPE = new IssuableSubscriptionVisitor() {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      throw NPE;
    }
  };

  @Test
  @Ignore
  public void test_semantic_exclusions() {
    VisitorsBridge visitorsBridgeWithoutSemantic = new VisitorsBridge(Collections.singletonList((JavaFileScanner) context -> {
      assertThat(context.getSemanticModel()).isNull();
      assertThat(context.fileParsed()).isTrue();
    }), new ArrayList<>(), null);
    checkFile(contstructFileName("java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("src", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("home", "user", "oracleSdk", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("java", "io", "Serializable.java"), "package java.io; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("java", "lang", "annotation", "Annotation.java"), "package java.lang.annotation; class Annotation {}", visitorsBridgeWithoutSemantic);

    VisitorsBridge visitorsBridgeWithParsingIssue = new VisitorsBridge(Collections.singletonList(new IssuableSubscriptionVisitor() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
        assertThat(context.fileParsed()).isFalse();
      }

      @Override
      public List<Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD);
      }
    }), new ArrayList<>(), null);
    checkFile(contstructFileName("org", "foo", "bar", "Foo.java"), "class Foo { arrrrrrgh", visitorsBridgeWithParsingIssue);
  }

  private static void checkFile(String filename, String code, VisitorsBridge visitorsBridge) {
    visitorsBridge.setCurrentFile(TestUtils.emptyInputFile(filename));
    visitorsBridge.visitFile(parse(code));
  }

  @Test
  @Ignore
  public void log_only_50_elements() throws Exception {
    DecimalFormat formatter = new DecimalFormat("00");
    IntFunction<String> classNotFoundName = i -> "NotFound" + formatter.format(i);
    VisitorsBridge visitorsBridge = new VisitorsBridge(Collections.singletonList((JavaFileScanner) context -> {
      assertThat(context.getSemanticModel()).isNotNull();
      // FIXME log missing classes?
      // ((SemanticModel) context.getSemanticModel()).classesNotFound().addAll(IntStream.range(0,
      // 60).mapToObj(classNotFoundName).collect(Collectors.toList()));
    }), new ArrayList<>(), null);
    checkFile("Foo.java", "class Foo {}", visitorsBridge);
    visitorsBridge.endOfAnalysis();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "Classes not found during the analysis : [" +
        IntStream.range(0, 50 /* only first 50 missing classes are displayed in the log */).mapToObj(classNotFoundName).sorted().collect(Collectors.joining(", ")) + ", ...]");
  }

  private static String contstructFileName(String... path) {
    String result = "";
    for (String s : path) {
      result += s + File.separator;
    }
    return result.substring(0, result.length() - 1);
  }

  private static CompilationUnitTree parse(String code) {
    return JParserTestUtils.parse(code);
  }

  @Test
  public void rethrow_exception_when_hidden_property_set_to_true_with_JavaFileScanner_visitors() {
    try {
      visitorsBridge(VISITOR_JAVA_FILE_SCANNER_NPE, true)
        .visitFile(COMPILATION_UNIT_TREE);
      Fail.fail("scanning of file should have raise an exception");
    } catch (Exception e) {
      assertThat(e).isSameAs(NPE);
    }
    assertThat(sonarComponents.analysisErrors).isEmpty();
  }

  @Test
  public void swallow_exception_when_hidden_property_set_to_false_with_JavaFileScanner_visitors() {
    try {
      visitorsBridge(VISITOR_JAVA_FILE_SCANNER_NPE, false)
        .visitFile(COMPILATION_UNIT_TREE);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exception should be swallowed when property is not set");
    }
    assertThat(sonarComponents.analysisErrors).hasSize(1);
  }

  @Test
  public void rethrow_exception_when_hidden_property_set_to_true_with_Subscription_visitors() {
    try {
      visitorsBridge(VISITOR_SUBSCRIPTION_NPE, true)
        .visitFile(COMPILATION_UNIT_TREE);
      Fail.fail("scanning of file should have raise an exception");
    } catch (Exception e) {
      assertThat(e).isSameAs(NPE);
    }
    assertThat(sonarComponents.analysisErrors).isEmpty();
  }

  @Test
  public void swallow_exception_when_hidden_property_set_to_false_with_Subscription_visitors() {
    try {
      visitorsBridge(VISITOR_SUBSCRIPTION_NPE, false)
        .visitFile(COMPILATION_UNIT_TREE);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exception should be swallowed when property is not set");
    }
    assertThat(sonarComponents.analysisErrors).hasSize(1);
  }

  private final VisitorsBridge visitorsBridge(JavaFileScanner visitor, boolean failOnException) {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    sensorContextTester.setSettings(new MapSettings().setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, failOnException));

    sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(sensorContextTester);

    VisitorsBridge visitorsBridge = new VisitorsBridge(Collections.singleton(visitor), new ArrayList<>(), sonarComponents);
    visitorsBridge.setCurrentFile(INPUT_FILE);

    return visitorsBridge;
  }
}
