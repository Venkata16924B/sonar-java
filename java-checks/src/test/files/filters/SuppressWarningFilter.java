/**
 * Extra rules running:
 * - TodoTagPresenceCheck (S1135)
 * - UnusedPrivateFieldCheck (S1068)
 * - BadConstantNameCheck (S115)
 * - SuppressWarningCheck (S1309) - Raise an issue on all the @SuppressWarning annotation, can not be suppressed
 */
class Test {
  class UnusedPrivateFieldCheck {
    private String s; // WithIssue
  }

  @SuppressWarnings("all") // WithIssue
  class A {
    private static final int bad_constant_name = 42; // NoIssue

    private String s; // NoIssue

    int foo() {
      return bad_constant_name;
    }
  }

  // Syntax not supported - repository is required
  @SuppressWarnings("S1068") // WithIssue
  class B {
    private static final int bad_constant_name = 42; // WithIssue

    private String s; // WithIssue

    int foo() {
      return bad_constant_name;
    }
  }

  @SuppressWarnings({"repo:S1068", "repo:S115"}) // WithIssue
  class C {
    private static final int bad_constant_name = 42; // NoIssue

    private String s; // NoIssue

    int foo() {
      return bad_constant_name;
    }
  }

  @SuppressWarnings("repo:S115") // WithIssue
  class D {
    private static final int bad_constant_name = 42; // NoIssue

    @SuppressWarnings("unused") // WithIssue
    private String s; // WithIssue

    int foo() {
      return bad_constant_name;
    }
  }

  @SuppressWarnings // WithIssue
  @Deprecated
  class E {
  }

  class F {
    @SuppressWarnings("repo:S115") // WithIssue
    private static final int bad_constant_name = 42; // NoIssue

    @SuppressWarnings("repo:S1068") // WithIssue
    private String s; // NoIssue

    int foo() {
      return bad_constant_name;
    }

    @SuppressWarnings(org.sonar.java.filters.SuppressWarningFilterTest.CONSTANT_RULE_KEY) // WithIssue
    private static final int bad_constant_name2 = 42; // NoIssue

    @SuppressWarnings(someUnresolvedConstant) // WithIssue
    private static final int bad_constant_name3 = 42; // WithIssue
  }

  @SuppressWarnings("squid:S1068") // WithIssue
  class WithDeprecatedRuleKey {

    @SuppressWarnings("squid:S00115") // WithIssue
    private static final int bad_constant_name = 42; // NoIssue
    @SuppressWarnings("squid:S115") // WithIssue
    private static final int bad_constant_name2 = 42; // NoIssue
    @SuppressWarning("S115") // WithIssue
    private static final int bad_constant_name3 = 42; // WithIssue

    @SuppressWarnings("squid:ObjectFinalizeCheck") // WithIssue
    void a() {
      Object object = new Object();
      object.finalize(); // NoIssue
    }

    @SuppressWarnings("squid:S1111") // WithIssue
    void b() {
      Object object = new Object();
      object.finalize(); // NoIssue
    }

    @SuppressWarnings("repo:S1111") // WithIssue
    void c() {
      Object object = new Object();
      object.finalize(); // NoIssue
    }

    void d() {
      Object object = new Object();
      object.finalize(); // WithIssue
    }
  }
}

/**
 * This is trivia with issue
 * TODO  NoIssue
 */
// TODO another one NoIssue
@SuppressWarnings("repo:S1135") // WithIssue
class JavadocSuppressed {


}

/**
 * This is trivia with issue
 * TODO  WithIssue
 */
// TODO another one WithIssue
class Javadoc {

  /**
   * Works on method too
   * TODO  NoIssue
   */
  @SuppressWarnings("repo:S1135") // WithIssue
  void m() {

  }

}
