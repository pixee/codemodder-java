package io.codemodder.remediation.pathtraversal;

import io.codemodder.RemediatorTestMixin;
import io.codemodder.remediation.Remediator;
import java.util.stream.Stream;

final class PathTraversalRemediatorTest implements RemediatorTestMixin<Object> {

  @Override
  public Remediator<Object> createRemediator() {
    return new PathTraversalRemediator<>();
  }

  @Override
  public Stream<FixableSample> createFixableSamples() {
    return Stream.of(
        new FixableSample(
            """
                        import org.springframework.web.multipart.MultipartFile;
                        public class Test {
                            public void test(MultipartFile file) {
                                String filename = file.getOriginalFilename();
                            }
                        }
                        """,
            """
                        import io.github.pixee.security.Filenames;
                        import org.springframework.web.multipart.MultipartFile;
                        public class Test {
                            public void test(MultipartFile file) {
                                String filename = Filenames.toSimpleFilename(file.getOriginalFilename());
                            }
                        }
                        """,
            4),
        new FixableSample(
            """
                        import org.springframework.web.multipart.MultipartFile;
                        public class Test {
                            public void test(MultipartFile file) {
                                String filename = new File(dir, file.getOriginalFilename());
                            }
                        }
                        """,
            """
                        import io.github.pixee.security.Filenames;
                        import org.springframework.web.multipart.MultipartFile;
                        public class Test {
                            public void test(MultipartFile file) {
                                String filename = new File(dir, Filenames.toSimpleFilename(file.getOriginalFilename()));
                            }
                        }
                        """,
            4));
  }

  @Override
  public Stream<UnfixableSample> createUnfixableSamples() {
    return Stream.of(
        // no getOriginalFilename() call
        new UnfixableSample(
            """
                                import org.springframework.web.multipart.MultipartFile;
                                public class Test {
                                    public void test(MultipartFile file) {
                                        String filename = file.filename();
                                    }
                                }
                                """,
            4));
  }
}
