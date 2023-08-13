package io.codemodder.plugins.llm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Test;

final class LLMDiffsTest {
  private void verifyDiff(final String original, final String diff, final String patched) {
    assertThat(
        LLMDiffs.applyDiff(List.of(original.split("\n")), diff), is(List.of(patched.split("\n"))));
  }

  @Test
  void testApplyDiff() {
    String original = """
        hello
        """;

    String diff =
        """
        --- hello
        +++ hello
        @@ -1,1 +1,1 @@
        -hello
        +goodbye
        """;

    String patched = """
        goodbye
        """;

    verifyDiff(original, diff, patched);
  }

  @Test
  void testApplyDiff_IncorrectIndentation() {
    // This is poorly formatted, with the `else` at column 15 instead of 16.
    String original =
        """
                       }
                       else {
                           response.sendRedirect("LoginPage?error=Unauthorized Access");
                       }
                   }
        """;

    // The LLM puts the `else` at column 16.
    String diff =
        """
        --- LoginValidate.java
        +++ LoginValidate.java
        @@ -2,3 +2,4 @@ public class LoginValidate extends HttpServlet {
                         else {
        +                    System.out.println("Failed login attempt for user: " + user);
                             response.sendRedirect("LoginPage?error=Unauthorized Access");
                         }
        """;

    // The `else` should stay at column 15.
    String patched =
        """
                       }
                       else {
                            System.out.println("Failed login attempt for user: " + user);
                           response.sendRedirect("LoginPage?error=Unauthorized Access");
                       }
                   }
        """;

    verifyDiff(original, diff, patched);
  }

  @Test
  void testApplyDiff_MultipleHunks() {
    String original =
        """


              else{
                  HttpSession session=request.getSession();
      session.setAttribute("user",user);
               response.sendRedirect("MemberHome");}
             }
             else
             {
                 response.sendRedirect("LoginPage?error=Unauthorized Access");
             }
         }
         else{
              response.sendRedirect("LoginPage?error=Invalid User or Password");
         }

      }
      catch(Exception e){
         e.printStackTrace();
      """;

    // Wrong line numbers and indentation.
    String diff =
        """
        --- LoginValidate.java
        +++ LoginValidate.java
        @@ -6,4 +6,5 @@
                 else
                 {
        +            System.out.println("Failed login attempt for user: " + user);
                     response.sendRedirect("LoginPage?error=Unauthorized Access");
                 }
        @@ -16,2 +17,3 @@
         catch(Exception e){
             e.printStackTrace();
        +    System.out.println("Exception during login validation: " + e.getMessage());
        """;

    String patched =
        """


                else{
                    HttpSession session=request.getSession();
        session.setAttribute("user",user);
                 response.sendRedirect("MemberHome");}
               }
               else
               {
                    System.out.println("Failed login attempt for user: " + user);
                   response.sendRedirect("LoginPage?error=Unauthorized Access");
               }
           }
           else{
                response.sendRedirect("LoginPage?error=Invalid User or Password");
           }

        }
        catch(Exception e){
           e.printStackTrace();
            System.out.println("Exception during login validation: " + e.getMessage());
        """;

    verifyDiff(original, diff, patched);
  }
}
